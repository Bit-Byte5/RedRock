#include <android/log.h>
#include <android/native_activity.h>
#include <dirent.h>
#include <dlfcn.h>
#include <jni.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "mcpelauncher_mod.h"
#include "mod_loader.h"

#define LOG_TAG "RedRockMc"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

typedef void (*native_entry_fn)(void *activity, void *saved_state, size_t saved_state_size);
typedef jint (*jni_onload_fn)(JavaVM *vm, void *reserved);

void ANativeActivity_onCreate(ANativeActivity *activity, void *saved_state, size_t saved_state_size);
void GameActivity_onCreate(void *activity, void *saved_state, size_t saved_state_size);

static JavaVM *g_vm;
static void *g_real;

static void load_dir(const char *dir, int skip_minecraftpe) {
  DIR *d = opendir(dir);
  if (d == NULL) {
    return;
  }
  struct dirent *ent;
  while ((ent = readdir(d)) != NULL) {
    const char *name = ent->d_name;
    size_t n = strlen(name);
    if (n < 4 || strcmp(name + n - 3, ".so") != 0) {
      continue;
    }
    int is_mc = strcmp(name, "libminecraftpe.so") == 0;
    if (skip_minecraftpe ? is_mc : !is_mc) {
      continue;
    }
    /* PairIP-protected. Loading these before VMRunner decrypts PLT SIGSEGVs. */
    if (strcmp(name, "libmaesdk.so") == 0 ||
        strcmp(name, "libpairipcore.so") == 0 ||
        strcmp(name, "libPlayFabMultiplayer.so") == 0) {
      continue;
    }
    char path[PATH_MAX];
    snprintf(path, sizeof(path), "%s/%s", dir, name);
    void *handle = dlopen(path, RTLD_NOW | RTLD_GLOBAL);
    if (handle == NULL) {
      LOGI("dlopen skip %s: %s", path, dlerror());
    } else {
      LOGI("dlopen %s", path);
    }
  }
  closedir(d);
}

static int is_self_library(const char *path) {
  return path != NULL && strstr(path, "/com.saltmarshdigital.redrock") != NULL &&
         strstr(path, "/lib/arm64/libminecraftpe.so") != NULL;
}

static const char *mods_dir(const char *internal_data_path) {
  const char *explicit_path = getenv("REDROCK_MODS_DIR");
  if (explicit_path != NULL && explicit_path[0] != '\0') {
    return explicit_path;
  }
  static char fallback[PATH_MAX];
  if (internal_data_path != NULL && internal_data_path[0] != '\0') {
    snprintf(fallback, sizeof(fallback), "%s/mods", internal_data_path);
    return fallback;
  }
  return NULL;
}

static void *load_real_library(const char *internal_data_path) {
  if (g_real != NULL) {
    return g_real;
  }

  const char *explicit_path = getenv("REDROCK_MINECRAFTPE");
  if (explicit_path == NULL || explicit_path[0] == '\0') {
    explicit_path = NULL;
  }

  char fallback[PATH_MAX];
  fallback[0] = '\0';
  if (internal_data_path != NULL && internal_data_path[0] != '\0') {
    snprintf(
        fallback,
        sizeof(fallback),
        "%s/game/com.mojang.minecraftpe/lib/arm64-v8a/libminecraftpe.so",
        internal_data_path);
  }

  const char *path = explicit_path != NULL ? explicit_path : fallback;
  if (path[0] == '\0' || is_self_library(path)) {
    LOGE("no extracted libminecraftpe.so path");
    return NULL;
  }

  char dir[PATH_MAX];
  snprintf(dir, sizeof(dir), "%s", path);
  char *slash = strrchr(dir, '/');
  if (slash != NULL) {
    *slash = '\0';
    load_dir(dir, 1);
  }

  const char *mods = mods_dir(internal_data_path);
  redrock_load_mods(mods, 1);

  void *handle = dlopen(path, RTLD_NOW | RTLD_GLOBAL);
  if (handle == NULL) {
    LOGE("dlopen real minecraftpe failed: %s", dlerror());
    return NULL;
  }
  LOGI("dlopen real %s", path);
  g_real = handle;
  redrock_apply_preinit_hooks();
  redrock_load_mods(mods, 0);
  return handle;
}

static void forward_entry(const char *symbol, void *activity, void *saved_state, size_t saved_state_size) {
  const char *internal = NULL;
  if (activity != NULL) {
    ANativeActivity *native = (ANativeActivity *) activity;
    internal = native->internalDataPath;
  }
  void *handle = load_real_library(internal);
  if (handle == NULL) {
    return;
  }
  native_entry_fn entry = (native_entry_fn) dlsym(handle, symbol);
  if (entry == NULL) {
    LOGE("could not resolve %s", symbol);
    return;
  }
  LOGI("forwarding %s", symbol);
  entry(activity, saved_state, saved_state_size);
}

__attribute__((visibility("default"))) void ANativeActivity_onCreate(
    ANativeActivity *activity,
    void *saved_state,
    size_t saved_state_size) {
  forward_entry("ANativeActivity_onCreate", activity, saved_state, saved_state_size);
}

__attribute__((visibility("default"))) void GameActivity_onCreate(
    void *activity,
    void *saved_state,
    size_t saved_state_size) {
  void *handle = load_real_library(NULL);
  if (handle == NULL) {
    return;
  }
  native_entry_fn entry = (native_entry_fn) dlsym(handle, "GameActivity_onCreate");
  if (entry == NULL || entry == (native_entry_fn) GameActivity_onCreate) {
    LOGE("could not resolve real GameActivity_onCreate");
    return;
  }
  LOGI("forwarding GameActivity_onCreate");
  redrock_load_mods(getenv("REDROCK_MODS_DIR"), 1);
  entry(activity, saved_state, saved_state_size);
  redrock_load_mods(getenv("REDROCK_MODS_DIR"), 0);
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
  g_vm = vm;
  void *handle = load_real_library(NULL);
  if (handle != NULL) {
    jni_onload_fn onload = (jni_onload_fn) dlsym(handle, "JNI_OnLoad");
    if (onload != NULL && (void *) onload != (void *) JNI_OnLoad) {
      jint version = onload(vm, reserved);
      if (version != 0) {
        return version;
      }
    }
  }
  return JNI_VERSION_1_6;
}

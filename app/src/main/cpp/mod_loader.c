#include "mod_loader.h"

#include "mcpelauncher_mod.h"

#include <android/log.h>
#include <dirent.h>
#include <dlfcn.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define LOG_TAG "ModLoader"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define MAX_MODS 64
#define CORE_MOD "libredrock.so"

struct LoadedMod {
  char path[PATH_MAX];
  void *handle;
  int preinit;
  int init;
};

static struct LoadedMod g_mods[MAX_MODS];
static int g_mod_count = 0;
static int g_loaded_total = 0;

static int ends_with(const char *name, const char *suffix) {
  size_t n = strlen(name);
  size_t s = strlen(suffix);
  return n >= s && strcmp(name + n - s, suffix) == 0;
}

static int is_disabled(const char *dir, const char *file) {
  char list[PATH_MAX];
  snprintf(list, sizeof(list), "%s/disabled.txt", dir);
  FILE *fp = fopen(list, "r");
  if (fp == NULL) {
    return 0;
  }
  char line[256];
  int hit = 0;
  while (fgets(line, sizeof(line), fp) != NULL) {
    size_t n = strlen(line);
    while (n > 0 && (line[n - 1] == '\n' || line[n - 1] == '\r' || line[n - 1] == ' ')) {
      line[--n] = '\0';
    }
    if (n == 0 || line[0] == '#') {
      continue;
    }
    if (strcmp(line, file) == 0) {
      hit = 1;
      break;
    }
  }
  fclose(fp);
  return hit;
}

static int needed_minecraftpe(const char *path) {
  FILE *file = fopen(path, "rb");
  if (file == NULL) {
    return 0;
  }
  char buf[1 << 16];
  size_t n = fread(buf, 1, sizeof(buf), file);
  fclose(file);
  const char needle[] = "libminecraftpe.so";
  size_t needle_len = sizeof(needle) - 1;
  if (n < needle_len) {
    return 0;
  }
  for (size_t i = 0; i + needle_len <= n; i++) {
    if (memcmp(buf + i, needle, needle_len) == 0) {
      return 1;
    }
  }
  return 0;
}

static struct LoadedMod *find_mod(void *handle) {
  for (int i = 0; i < g_mod_count; i++) {
    if (g_mods[i].handle == handle) {
      return &g_mods[i];
    }
  }
  return NULL;
}

static void load_one(const char *path, const char *file, int preinit) {
  if (preinit && strcmp(file, CORE_MOD) != 0 && needed_minecraftpe(path)) {
    return;
  }
  LOGI("Loading mod: %s", file);
  void *handle = dlopen(path, RTLD_NOW | RTLD_GLOBAL);
  if (handle == NULL) {
    LOGE("Failed to load mod %s: %s", path, dlerror());
    return;
  }
  struct LoadedMod *existing = find_mod(handle);
  if (existing != NULL) {
    if (preinit ? existing->preinit : existing->init) {
      return;
    }
  } else {
    if (g_mod_count >= MAX_MODS) {
      LOGW("mod cap reached, skip %s", file);
      return;
    }
    existing = &g_mods[g_mod_count++];
    memset(existing, 0, sizeof(*existing));
    snprintf(existing->path, sizeof(existing->path), "%s", path);
    existing->handle = handle;
  }
  if (preinit) {
    existing->preinit = 1;
  } else {
    existing->init = 1;
  }
  const char *entry = preinit ? "mod_preinit" : "mod_init";
  void (*init)(void) = (void (*)(void)) dlsym(handle, entry);
  if (init == NULL) {
    LOGW("Mod %s does not have a %s function", path, entry);
    return;
  }
  init();
  g_loaded_total++;
}

void redrock_load_mods(const char *dir, int preinit) {
  load_one(CORE_MOD, CORE_MOD, preinit);

  if (dir == NULL || dir[0] == '\0') {
    redrock_apply_preinit_hooks();
    return;
  }
  DIR *d = opendir(dir);
  if (d == NULL) {
    redrock_apply_preinit_hooks();
    return;
  }
  LOGI("Loading mods from %s", dir);
  char names[MAX_MODS][256];
  int n = 0;
  struct dirent *ent;
  while ((ent = readdir(d)) != NULL && n < MAX_MODS) {
    if (ent->d_name[0] == '.') {
      continue;
    }
    if (!ends_with(ent->d_name, ".so")) {
      continue;
    }
    if (strcmp(ent->d_name, CORE_MOD) == 0) {
      continue;
    }
    if (is_disabled(dir, ent->d_name)) {
      LOGI("skip disabled %s", ent->d_name);
      continue;
    }
    snprintf(names[n++], sizeof(names[0]), "%s", ent->d_name);
  }
  closedir(d);

  int before = g_loaded_total;
  for (int i = 0; i < n; i++) {
    char path[PATH_MAX];
    snprintf(path, sizeof(path), "%s/%s", dir, names[i]);
    load_one(path, names[i], preinit);
  }
  LOGI("Loaded %d mods", g_loaded_total);
  (void) before;
  redrock_apply_preinit_hooks();
}

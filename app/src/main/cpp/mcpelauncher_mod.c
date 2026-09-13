#include "mcpelauncher_mod.h"

#include "hook_aarch64.h"

#include <android/log.h>
#include <dlfcn.h>
#include <stdarg.h>
#include <stddef.h>
#include <stdio.h>
#include <string.h>

#define LOG_TAG "RedRockMods"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define MAX_PREINIT 64

struct PreinitHook {
  char name[192];
  void *hook;
  void **orig;
  void *user;
  void (*callback)(void *, void *);
  int applied;
};

static struct PreinitHook g_preinit[MAX_PREINIT];
static int g_preinit_count = 0;

static int android_prio(int level) {
  if (level <= 1) {
    return ANDROID_LOG_ERROR;
  }
  if (level == 2) {
    return ANDROID_LOG_WARN;
  }
  if (level >= 5) {
    return ANDROID_LOG_VERBOSE;
  }
  if (level == 4) {
    return ANDROID_LOG_DEBUG;
  }
  return ANDROID_LOG_INFO;
}

__attribute__((visibility("default"))) void mcpelauncher_log(
    int level,
    const char *tag,
    const char *text) {
  __android_log_print(android_prio(level), tag != NULL ? tag : "Mod", "%s", text != NULL ? text : "");
}

__attribute__((visibility("default"))) void mcpelauncher_vlog(
    int level,
    const char *tag,
    const char *fmt,
    va_list ap) {
  char buf[1024];
  vsnprintf(buf, sizeof(buf), fmt != NULL ? fmt : "", ap);
  mcpelauncher_log(level, tag, buf);
}

static void store_preinit(
    const char *name,
    void *hook,
    void **orig,
    void *user,
    void (*callback)(void *, void *)) {
  if (name == NULL || hook == NULL || g_preinit_count >= MAX_PREINIT) {
    return;
  }
  struct PreinitHook *slot = &g_preinit[g_preinit_count++];
  memset(slot, 0, sizeof(*slot));
  snprintf(slot->name, sizeof(slot->name), "%s", name);
  slot->hook = hook;
  slot->orig = orig;
  slot->user = user;
  slot->callback = callback;
}

static void apply_one(struct PreinitHook *slot) {
  if (slot->applied) {
    return;
  }
  void *sym = dlsym(RTLD_DEFAULT, slot->name);
  if (sym == NULL) {
    return;
  }
  void *orig = NULL;
  if (redrock_hook(sym, slot->hook, slot->orig != NULL ? &orig : NULL) != 0) {
    LOGE("hook failed %s", slot->name);
    return;
  }
  if (slot->orig != NULL && orig != NULL) {
    *slot->orig = orig;
  }
  if (slot->callback != NULL) {
    slot->callback(slot->user, orig);
  }
  slot->applied = 1;
  LOGI("hooked %s", slot->name);
}

__attribute__((visibility("default"))) void mcpelauncher_preinithook(
    const char *name,
    void *hook,
    void **orig) {
  int before = g_preinit_count;
  store_preinit(name, hook, orig, orig, NULL);
  if (g_preinit_count > before) {
    apply_one(&g_preinit[g_preinit_count - 1]);
  }
}

__attribute__((visibility("default"))) void mcpelauncher_preinithook2(
    const char *name,
    void *hook,
    void *user,
    void (*callback)(void *, void *)) {
  int before = g_preinit_count;
  store_preinit(name, hook, NULL, user, callback);
  if (g_preinit_count > before) {
    apply_one(&g_preinit[g_preinit_count - 1]);
  }
}

__attribute__((visibility("default"))) void *mcpelauncher_hook(void *sym, void *hook, void **orig) {
  if (redrock_hook(sym, hook, orig) != 0) {
    return NULL;
  }
  return orig != NULL ? *orig : hook;
}

__attribute__((visibility("default"))) void *mcpelauncher_hook2(
    void *lib,
    const char *name,
    void *hook,
    void **orig) {
  void *sym = dlsym(lib != NULL ? lib : RTLD_DEFAULT, name);
  if (sym == NULL) {
    LOGE("hook2 missing %s", name != NULL ? name : "?");
    return NULL;
  }
  return mcpelauncher_hook(sym, hook, orig);
}

__attribute__((visibility("default"))) void mcpelauncher_hook2_add_library(void *lib) {
  (void) lib;
}

__attribute__((visibility("default"))) void mcpelauncher_hook2_remove_library(void *lib) {
  (void) lib;
}

__attribute__((visibility("default"))) void mcpelauncher_hook2_delete(void *hook) {
  (void) hook;
}

__attribute__((visibility("default"))) void mcpelauncher_hook2_apply(void) {
  redrock_apply_preinit_hooks();
}

__attribute__((visibility("default"))) void *mcpelauncher_patch(void *address, void *data, size_t size) {
  return redrock_patch(address, data, size);
}

__attribute__((visibility("default"))) void *mcpelauncher_host_dlopen(const char *path, int flags) {
  return dlopen(path, flags);
}

__attribute__((visibility("default"))) void *mcpelauncher_host_dlsym(void *handle, const char *name) {
  return dlsym(handle, name);
}

__attribute__((visibility("default"))) int mcpelauncher_host_dlclose(void *handle) {
  return dlclose(handle);
}

void redrock_apply_preinit_hooks(void) {
  for (int i = 0; i < g_preinit_count; i++) {
    apply_one(&g_preinit[i]);
  }
}

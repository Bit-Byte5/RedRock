#include <android/log.h>

#define LOG_TAG "RedRockCore"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

/*
 * Bundled host mod. Always loaded into the hosted Minecraft process.
 * Scaffold for now: prove the loader attached us before and after libminecraftpe.
 */

__attribute__((visibility("default"))) void mod_preinit(void) {
  LOGI("loaded into Minecraft (preinit)");
}

__attribute__((visibility("default"))) void mod_init(void) {
  LOGI("loaded into Minecraft (init, libminecraftpe ready)");
}

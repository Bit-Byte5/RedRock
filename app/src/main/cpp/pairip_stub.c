#include <android/log.h>
#include <jni.h>
#include <stdint.h>

#define LOG_TAG "RedRockMc"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

/* Play PairIP VM. Hosted Minecraft cannot run the real interpreter. */
__attribute__((visibility("default"))) uint64_t ExecuteProgram(
    uint64_t a,
    uint64_t b,
    uint64_t c,
    uint64_t d,
    uint64_t e,
    uint64_t f,
    uint64_t g,
    uint64_t h) {
  (void) a;
  (void) b;
  (void) c;
  (void) d;
  (void) e;
  (void) f;
  (void) g;
  (void) h;
  return 0;
}

JNIEXPORT jobject JNICALL Java_com_pairip_VMRunner_executeVM(
    JNIEnv *env,
    jclass cls,
    jbyteArray bytecode,
    jobjectArray args) {
  (void) env;
  (void) cls;
  (void) bytecode;
  (void) args;
  return NULL;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
  (void) vm;
  (void) reserved;
  LOGI("stub libpairipcore JNI_OnLoad");
  return JNI_VERSION_1_6;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
  (void) vm;
  (void) reserved;
}

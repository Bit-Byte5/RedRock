#include "hook_aarch64.h"

#include <android/log.h>
#include <errno.h>
#include <stdint.h>
#include <string.h>
#include <sys/mman.h>
#include <unistd.h>

#define LOG_TAG "RedRockMods"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define HOOK_BYTES 16

#if defined(__aarch64__)

static int make_writable(void *addr, size_t length, int prot) {
  long page = sysconf(_SC_PAGESIZE);
  uintptr_t start = (uintptr_t) addr & ~((uintptr_t) page - 1);
  uintptr_t end = ((uintptr_t) addr + length + (uintptr_t) page - 1) & ~((uintptr_t) page - 1);
  if (mprotect((void *) start, (size_t) (end - start), prot) != 0) {
    LOGE("mprotect %p: %s", addr, strerror(errno));
    return -1;
  }
  return 0;
}

static void write_abs_jump(uint8_t *dst, void *target) {
  /* LDR X16, #8 ; BR X16 ; .quad target */
  dst[0] = 0x50;
  dst[1] = 0x00;
  dst[2] = 0x00;
  dst[3] = 0x58;
  dst[4] = 0x00;
  dst[5] = 0x02;
  dst[6] = 0x1f;
  dst[7] = 0xd6;
  memcpy(dst + 8, &target, sizeof(target));
}

int redrock_hook(void *target, void *replacement, void **original) {
  if (target == NULL || replacement == NULL) {
    return -1;
  }

  uint8_t *tramp = NULL;
  if (original != NULL) {
    tramp = mmap(NULL, 64, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
    if (tramp == MAP_FAILED) {
      LOGE("mmap trampoline: %s", strerror(errno));
      return -1;
    }
    memcpy(tramp, target, HOOK_BYTES);
    write_abs_jump(tramp + HOOK_BYTES, (uint8_t *) target + HOOK_BYTES);
    if (mprotect(tramp, 64, PROT_READ | PROT_EXEC) != 0) {
      LOGE("mprotect trampoline: %s", strerror(errno));
      munmap(tramp, 64);
      return -1;
    }
    __builtin___clear_cache((char *) tramp, (char *) tramp + 64);
    *original = tramp;
  }

  if (make_writable(target, HOOK_BYTES, PROT_READ | PROT_WRITE | PROT_EXEC) != 0) {
    if (make_writable(target, HOOK_BYTES, PROT_READ | PROT_WRITE) != 0) {
      if (tramp != NULL) {
        munmap(tramp, 64);
      }
      return -1;
    }
  }
  write_abs_jump(target, replacement);
  __builtin___clear_cache((char *) target, (char *) target + HOOK_BYTES);
  make_writable(target, HOOK_BYTES, PROT_READ | PROT_EXEC);
  return 0;
}

void *redrock_patch(void *address, const void *data, size_t size) {
  if (address == NULL || data == NULL || size == 0) {
    return NULL;
  }
  if (make_writable(address, size, PROT_READ | PROT_WRITE | PROT_EXEC) != 0) {
    if (make_writable(address, size, PROT_READ | PROT_WRITE) != 0) {
      return NULL;
    }
  }
  memcpy(address, data, size);
  __builtin___clear_cache((char *) address, (char *) address + size);
  make_writable(address, size, PROT_READ | PROT_EXEC);
  return address;
}

#else

int redrock_hook(void *target, void *replacement, void **original) {
  (void) target;
  (void) replacement;
  (void) original;
  LOGE("inline hook only implemented for arm64");
  return -1;
}

void *redrock_patch(void *address, const void *data, size_t size) {
  if (address == NULL || data == NULL) {
    return NULL;
  }
  memcpy(address, data, size);
  return address;
}

#endif

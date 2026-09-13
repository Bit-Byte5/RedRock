#pragma once

#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

int redrock_hook(void *target, void *replacement, void **original);
void *redrock_patch(void *address, const void *data, size_t size);

#ifdef __cplusplus
}
#endif

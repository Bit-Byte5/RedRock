cmake_minimum_required(VERSION 3.22.1)
project(redrock-minecraftpe)

add_library(
  mcpelauncher_mod
  SHARED
  mcpelauncher_mod.c
  hook_aarch64.c
)
target_link_libraries(mcpelauncher_mod android log dl)

add_library(
  minecraftpe
  SHARED
  minecraftpe_forwarder.c
  mod_loader.c
)
target_link_libraries(minecraftpe android log dl mcpelauncher_mod)

add_library(redrock SHARED redrock_core.c)
target_link_libraries(redrock android log)

# PairIP-protected Play libs crash when loaded from a host package.
# Replace them at attach time with these no-op SONAMEs.
add_library(pairipcore SHARED pairip_stub.c)
target_link_libraries(pairipcore android log)

add_library(maesdk SHARED maesdk_stub.cpp)
target_link_libraries(maesdk android log)

add_library(PlayFabMultiplayer SHARED playfab_stub.c)
target_link_libraries(PlayFabMultiplayer android log)

# Mods

The toolbox tab is a **native** loader. Extra mods are mcpelauncher-style **arm64-v8a `.so` files**.

Not Java Fabric/Forge jars. Not marketplace packs. Not x86 / x86_64 `.so` files built for a Mac mcpelauncher.

## Built-in

| Mod | Behavior |
|---|---|
| **RedRock Core** | Always on. Cannot remove. Loads `mod_preinit`, then `libminecraftpe`, then `mod_init`. |
| **VRed** | Stereo VR scaffold. Cannot remove; can disable. Stereo is **not implemented** — the toggle only loads or skips an empty stub. |

## Extra mods on the headset

1. Put an `arm64-v8a` `.so` on the headset (Files / Downloads, or adb).
2. Toolbox tab → **Add** → pick that file.
3. **Off** skips it on the next Play. **Remove** deletes it from RedRock’s `files/mods`.
4. Press **Play**.

Mods that call `mcpelauncher_hook` / `mcpelauncher_preinithook` resolve against RedRock’s `libmcpelauncher_mod.so`.

## From a computer

```bash
./redrock mods list
./redrock mods push libThatMod.so
./redrock mods rm libThatMod.so
```

Push uses `run-as com.saltmarshdigital.redrock` into `files/mods/`. Toggle on the toolbox tab, then Play.

Do not commit `.so` binaries. `mods/*.so` is gitignored. `mods/.gitkeep` only holds the folder.

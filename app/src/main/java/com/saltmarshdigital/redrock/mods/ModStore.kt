package com.saltmarshdigital.redrock.mods

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.InputStream

data class InstalledMod(
  val file: File,
  val name: String,
  val enabled: Boolean,
  val sizeBytes: Long,
  val required: Boolean = false,
)

sealed class ModInstall {
  data class Ok(val mod: InstalledMod) : ModInstall()

  object BadFile : ModInstall()

  object Failed : ModInstall()
}

/**
 * mcpelauncher-style native mods: arm64 `.so` files in private storage.
 * Play starts the hosted game, which dlopens enabled mods from here.
 */
class ModStore(context: Context) {
  private val app: Context = context.applicationContext ?: context
  val root: File = File(app.filesDir, "mods").apply { mkdirs() }

  fun list(): List<InstalledMod> {
    val disabled = disabledNames()
    val user =
      root.listFiles { _, name -> name.endsWith(".so") && name != CORE_NAME }
        ?.sortedBy { it.name.lowercase() }
        ?.map { file ->
          InstalledMod(
            file = file,
            name = file.name,
            enabled = file.name !in disabled,
            sizeBytes = file.length(),
          )
        } ?: emptyList()
    return listOfNotNull(core()) + user
  }

  fun setEnabled(name: String, enabled: Boolean) {
    if (name == CORE_NAME) return
    val file = File(root, name)
    if (!file.isFile || !name.endsWith(".so")) return
    val next = disabledNames().toMutableSet()
    if (enabled) next.remove(name) else next.add(name)
    writeDisabled(next)
  }

  fun remove(name: String) {
    if (name == CORE_NAME) return
    val file = File(root, name)
    if (file.isFile && file.parentFile == root) {
      file.delete()
    }
    val next = disabledNames().toMutableSet()
    if (next.remove(name)) writeDisabled(next)
  }

  fun install(source: File): InstalledMod? {
    if (!source.isFile) return null
    return writeMod(source.name, source.inputStream())
  }

  fun install(uri: Uri): ModInstall {
    val name = fileName(uri) ?: return ModInstall.BadFile
    return try {
      val stream = app.contentResolver.openInputStream(uri) ?: return ModInstall.Failed
      val mod = stream.use { writeMod(name, it) }
      if (mod == null) ModInstall.BadFile else ModInstall.Ok(mod)
    } catch (error: Exception) {
      Log.w(TAG, "install failed ${error.message}")
      ModInstall.Failed
    }
  }

  private fun writeMod(rawName: String, input: InputStream): InstalledMod? {
    val name = File(rawName).name
    if (!name.endsWith(".so", ignoreCase = true) || name == CORE_NAME) return null
    val dest = File(root, name)
    dest.outputStream().use { output -> input.copyTo(output) }
    dest.setReadable(true, false)
    dest.setExecutable(true, false)
    Log.i(TAG, "install ${dest.name}")
    return list().firstOrNull { it.name == dest.name }
  }

  private fun fileName(uri: Uri): String? {
    app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
      ?.use { cursor ->
        if (cursor.moveToFirst()) {
          val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (index >= 0) {
            val name = cursor.getString(index)?.trim().orEmpty()
            if (name.isNotEmpty()) return name
          }
        }
      }
    return uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
  }

  private fun core(): InstalledMod? {
    val so = File(app.applicationInfo.nativeLibraryDir, CORE_NAME)
    if (!so.isFile) return null
    return InstalledMod(
      file = so,
      name = CORE_NAME,
      enabled = true,
      sizeBytes = so.length(),
      required = true,
    )
  }

  private fun disabledNames(): Set<String> {
    val file = File(root, DISABLED)
    if (!file.isFile) return emptySet()
    return file.readLines()
      .map { it.trim() }
      .filter { it.isNotEmpty() && !it.startsWith("#") }
      .toSet()
  }

  private fun writeDisabled(names: Set<String>) {
    val file = File(root, DISABLED)
    if (names.isEmpty()) {
      file.delete()
      return
    }
    file.writeText(names.sorted().joinToString("\n", postfix = "\n"))
  }

  companion object {
    const val DISABLED = "disabled.txt"
    const val CORE_NAME = "libredrock.so"
    private const val TAG = "RedRockMods"
  }
}

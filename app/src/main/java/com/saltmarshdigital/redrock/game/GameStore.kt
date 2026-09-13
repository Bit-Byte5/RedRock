package com.saltmarshdigital.redrock.game

import android.content.Context
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import com.saltmarshdigital.redrock.play.FetchError
import com.saltmarshdigital.redrock.play.PlayException
import java.io.File
import java.util.zip.ZipException
import java.util.zip.ZipFile

/**
 * mcpelauncher-style payload: APKs stay in RedRock private storage.
 * Do not PackageInstaller them — that registers a second Quest app.
 */
class GameStore(private val root: File, private val cacheDir: File? = null) {
  constructor(context: Context) : this(
    File((context.applicationContext ?: context).filesDir, "game"),
    (context.applicationContext ?: context).cacheDir,
  )

  fun extractedVersion(packageName: String = DEFAULT_PACKAGE): String? {
    val file = File(File(root, packageName), "version.txt")
    return file.takeIf { it.isFile }?.readText()?.trim()?.takeIf { it.isNotEmpty() }
  }

  fun payload(packageName: String = DEFAULT_PACKAGE): GamePayload? {
    val dir = File(root, packageName)
    val lib = File(dir, "lib/arm64-v8a/libminecraftpe.so")
    val apks = File(dir, "apks").listFiles { _, name -> name.endsWith(".apk") }?.sortedBy { it.name }
    if (!lib.isFile || apks.isNullOrEmpty()) return null
    return GamePayload(packageName, dir, File(lib.parentFile!!.absolutePath), apks)
  }

  fun cachedApks(packageName: String = DEFAULT_PACKAGE): List<File> {
    val cache = cacheDir ?: return emptyList()
    return File(cache, "play/$packageName")
      .listFiles { _, name -> name.endsWith(".apk") }
      ?.sortedBy { it.name } ?: emptyList()
  }

  fun extract(packageName: String, apks: List<File>, versionName: String = ""): GamePayload {
    val pkg = packageName.trim()
    if (pkg.isEmpty()) throw PlayException(FetchError.UNKNOWN, "package name is empty")
    val usable = apks.filter { it.isFile }
    if (usable.isEmpty()) throw PlayException(FetchError.UNKNOWN, "apk missing")

    payload(pkg)?.let { existing ->
      val have = extractedVersion(pkg)
      if (versionName.isBlank() || have == versionName) return existing
    }

    val dir = File(root, pkg)
    val tmp = File(root, "$pkg.extracting")
    val apkDir = File(tmp, "apks").apply { mkdirs() }
    val libDir = File(tmp, "lib/arm64-v8a").apply { mkdirs() }

    for (apk in usable.sortedBy { nativeSplitRank(it.name) }) {
      unzipNativeLibs(apk, libDir)
    }

    val so = File(libDir, "libminecraftpe.so")
    if (!so.isFile) {
      tmp.deleteRecursively()
      throw PlayException(FetchError.ABI, "no arm64-v8a libminecraftpe.so")
    }

    for (apk in usable) {
      placeApk(apk, File(apkDir, apk.name))
    }
    if (versionName.isNotBlank()) File(tmp, "version.txt").writeText(versionName)

    if (dir.exists()) dir.deleteRecursively()
    if (!tmp.renameTo(dir)) {
      tmp.copyRecursively(dir, overwrite = true)
      tmp.deleteRecursively()
    }
    val readyLibs = File(dir, "lib/arm64-v8a")
    val readyApks =
      File(dir, "apks").listFiles { _, name -> name.endsWith(".apk") }?.sortedBy { it.name } ?: usable
    Log.i(TAG, "extracted $pkg lib=${readyLibs.list()?.size} apk=${readyApks.size}")
    return GamePayload(pkg, dir, readyLibs, readyApks)
  }

  fun adoptPlayCache(packageName: String): GamePayload? {
    val apks = cachedApks(packageName)
    if (apks.isEmpty()) return null
    return extract(packageName, apks)
  }

  private fun unzipNativeLibs(apk: File, libDir: File) {
    if (!mightContainNativeLibs(apk)) {
      Log.i(TAG, "skip unzip ${apk.name}")
      return
    }
    try {
      ZipFile(apk).use { zip ->
        val prefix = "lib/arm64-v8a/"
        val entries = zip.entries()
        while (entries.hasMoreElements()) {
          val entry = entries.nextElement()
          if (entry.isDirectory || !entry.name.startsWith(prefix)) continue
          val name = entry.name.removePrefix(prefix)
          if (name.isEmpty() || name.contains('/')) continue
          val out = File(libDir, name)
          if (out.isFile && entry.size > 0L && out.length() == entry.size) continue
          zip.getInputStream(entry).use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
          }
          out.setReadable(true, false)
          out.setExecutable(true, false)
          Log.i(TAG, "lib ${out.name} ${out.length()}")
        }
      }
    } catch (error: ZipException) {
      Log.w(TAG, "skip unzip ${apk.name}: ${error.message}")
    }
  }

  private fun placeApk(src: File, dest: File) {
    val dex = isDexApk(src)
    if (dex) {
      if (!(dest.isFile && dest.length() == src.length() && !dest.canWrite())) {
        dest.delete()
        src.copyTo(dest, overwrite = true)
      }
      dest.setWritable(false, false)
      dest.setReadable(true, false)
      return
    }
    if (dest.exists() && dest.length() == src.length()) return
    dest.delete()
    try {
      Os.link(src.absolutePath, dest.absolutePath)
    } catch (_: ErrnoException) {
      src.copyTo(dest, overwrite = true)
    }
  }

  private fun mightContainNativeLibs(apk: File): Boolean {
    val name = apk.name.lowercase()
    if (name.contains("arm64") || name.contains("native")) return true
    if (name.contains("install_pack") || name.contains("obb") || name.contains("asset")) return false
    return apk.length() < NATIVE_UNZIP_LIMIT
  }

  private fun nativeSplitRank(name: String): Int {
    val lower = name.lowercase()
    return when {
      lower.contains("arm64") -> 0
      lower.startsWith("base") -> 1
      else -> 2
    }
  }

  companion object {
    const val DEFAULT_PACKAGE = "com.mojang.minecraftpe"
    const val MAIN_ACTIVITY = "com.mojang.minecraftpe.MainActivity"
    private const val TAG = "RedRockGame"
    private const val NATIVE_UNZIP_LIMIT = 200L * 1024 * 1024

    fun fromDataDir(dataDir: File): GameStore {
      return GameStore(File(dataDir, "files/game"), File(dataDir, "cache"))
    }
  }
}

data class GamePayload(
  val packageName: String,
  val dir: File,
  val libDir: File,
  val apks: List<File>,
) {
  fun dexPath(): String {
    return apks
      .filter { isDexApk(it) }
      .onEach { lockDex(it) }
      .joinToString(File.pathSeparator) { it.absolutePath }
  }

  val minecraftpe: File
    get() = File(libDir, "libminecraftpe.so")

  fun baseApk(): File? {
    return apks.firstOrNull { it.name.equals("base.apk", ignoreCase = true) }
      ?: apks.firstOrNull { isDexApk(it) }
  }
}

internal fun isDexApk(apk: File): Boolean {
  val name = apk.name.lowercase()
  if (name.contains("install_pack") || name.contains("asset") || name.contains("obb")) {
    return false
  }
  if (name.contains("arm64") || name.contains("native")) return false
  return name.endsWith(".apk")
}

internal fun lockDex(apk: File) {
  if (!apk.isFile) return
  apk.setWritable(false, false)
  apk.setReadable(true, false)
}

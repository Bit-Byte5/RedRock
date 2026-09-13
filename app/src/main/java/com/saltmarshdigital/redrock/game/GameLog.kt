package com.saltmarshdigital.redrock.game

import android.content.Context
import java.io.File

/**
 * Live Minecraft log for the library CRT. Reads the `:minecraft` process
 * logcat (same UID) and any on-disk game log files. Never returns secrets.
 */
object GameLog {
  private val Secret = Regex("token|oauth|aas|password|cookie", RegexOption.IGNORE_CASE)
  private val Brief = Regex("""^([VDIWEF])/(.+?)\(\s*(\d+)\):\s*(.*)$""")
  private val GameTags =
    setOf(
      "Minecraft",
      "MinecraftPE",
      "MCPE",
      "Bedrock",
      "RedRockGame",
      "AppPlatform",
      "RenderDragon",
    )

  fun dump(context: Context): List<String> {
    val pids = minecraftPids()
    val cat = logcat(pids)
    if (cat.isNotEmpty()) return cat.takeLast(120)
    return fileTail(context).takeLast(120)
  }

  private fun minecraftPids(): Set<Int> {
    val fromPidof =
      runCatching {
          ProcessBuilder("pidof", "com.saltmarshdigital.redrock:minecraft")
            .redirectErrorStream(true)
            .start()
            .inputStream
            .bufferedReader()
            .readText()
            .trim()
            .split(Regex("\\s+"))
            .mapNotNull { it.toIntOrNull() }
        }
        .getOrDefault(emptyList())
    val fromProc =
      File("/proc").listFiles { f -> f.name.all { it.isDigit() } }?.mapNotNull { dir ->
        val cmd =
          runCatching {
              File(dir, "cmdline")
                .readBytes()
                .takeWhile { it != 0.toByte() }
                .toByteArray()
                .toString(Charsets.UTF_8)
            }
            .getOrNull()
            .orEmpty()
        if (cmd.contains(":minecraft")) dir.name.toIntOrNull() else null
      } ?: emptyList()
    return (fromPidof + fromProc).toSet()
  }

  private fun logcat(pids: Set<Int>): List<String> {
    val proc =
      runCatching {
          ProcessBuilder("logcat", "-d", "-t", "250", "-v", "brief")
            .redirectErrorStream(true)
            .start()
        }
        .getOrNull() ?: return emptyList()
    return proc.inputStream.bufferedReader().use { reader ->
      reader
        .lineSequence()
        .mapNotNull { line -> keep(line.trim(), pids) }
        .toList()
    }
  }

  private fun keep(line: String, pids: Set<Int>): String? {
    if (line.isEmpty() || Secret.containsMatchIn(line)) return null
    val m = Brief.matchEntire(line)
    if (m != null) {
      val tag = m.groupValues[2].trim()
      val pid = m.groupValues[3].toIntOrNull() ?: -1
      val msg = m.groupValues[4]
      val game = pid in pids || tag in GameTags || tag.startsWith("Minecraft")
      if (!game) return null
      return "$tag  $msg"
    }
    return if (pids.isEmpty() && GameTags.any { line.contains(it) }) line else null
  }

  private fun fileTail(context: Context): List<String> {
    val roots =
      listOfNotNull(
        context.filesDir,
        context.getExternalFilesDir(null),
        GameStore(context).payload()?.dir,
      )
    val logs =
      roots.flatMap { root ->
        runCatching {
            root.walkTopDown().maxDepth(5).filter { it.isFile && isGameLog(it) }.toList()
          }
          .getOrDefault(emptyList())
      }
    val newest = logs.maxByOrNull { it.lastModified() } ?: return emptyList()
    return runCatching { newest.readLines().takeLast(80).map { it.trim() }.filter { it.isNotEmpty() } }
      .getOrDefault(emptyList())
  }

  private fun isGameLog(file: File): Boolean {
    val name = file.name.lowercase()
    val parent = file.parentFile?.name?.lowercase().orEmpty()
    if (Secret.containsMatchIn(file.path)) return false
    return parent == "logs" ||
      name.endsWith(".log") ||
      name == "log.txt" ||
      (parent.contains("mojang") && name.endsWith(".txt"))
  }
}

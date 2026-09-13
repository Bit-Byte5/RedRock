package com.saltmarshdigital.redrock.play

/**
 * Known Minecraft Bedrock Android versionCodes.
 * Play encodes 1.{minor}.{patch}.{rev} as 95{minor:02d}{patch:03d}{rev:02d}.
 */
object MinecraftReleases {
  private val NAMES =
    listOf(
      "1.21.132.3",
      "1.21.130.4",
      "1.21.124.2",
      "1.21.123.2",
      "1.21.122.2",
      "1.21.120.4",
      "1.21.114.1",
      "1.21.113.1",
      "1.21.111.1",
      "1.21.102.1",
      "1.21.101.1",
      "1.21.100.7",
      "1.21.95.1",
      "1.21.93.1",
      "1.21.92.1",
      "1.21.90.4",
      "1.21.84.1",
      "1.21.72.1",
      "1.21.70.4",
      "1.21.60.10",
      "1.21.50.10",
      "1.21.44.1",
      "1.21.31.4",
      "1.21.20.3",
      "1.21.0.3",
    )

  val all: List<GameVersion.Release> =
    NAMES.map { GameVersion.Release(name = it, versionCode = androidCode(it)) }

  fun androidCode(name: String): Long {
    val parts = name.split('.').mapNotNull { it.toIntOrNull() }
    val minor = parts.getOrElse(1) { 21 }
    val patch = parts.getOrElse(2) { 0 }
    val rev = parts.getOrElse(3) { 0 }
    return 95L * 10_000_000 + minor * 100_000L + patch * 100L + rev
  }
}

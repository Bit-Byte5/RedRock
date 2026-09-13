package com.saltmarshdigital.redrock.play

import java.io.File
import kotlinx.coroutines.flow.StateFlow

data class PlayAccount(val email: String)

sealed class AccountState {
  data object SignedOut : AccountState()

  data object SigningIn : AccountState()

  data class SignedIn(val account: PlayAccount) : AccountState()
}

sealed class FetchState {
  data object Idle : FetchState()

  data class Fetching(
    val packageName: String,
    val receivedBytes: Long = 0L,
    val totalBytes: Long = 0L,
  ) : FetchState() {
    val fraction: Float?
      get() =
        if (totalBytes > 0L) {
          (receivedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
          null
        }
  }

  data class Installing(val packageName: String) : FetchState()

  data class Ready(val packageName: String, val apk: File) : FetchState()

  data class Error(val kind: FetchError, val detail: String) : FetchState()
}

enum class FetchError {
  OWNERSHIP,
  TOS,
  ABI,
  NETWORK,
  UNKNOWN,
}

data class PlayCatalogState(
  val account: AccountState = AccountState.SignedOut,
  val fetch: FetchState = FetchState.Idle,
  val latestName: String = "",
)

/**
 * Google Play catalog used by the 2D launcher. UI binds to [state] only.
 * Real client: [GooglePlayCatalog]. [FakePlayCatalog] remains for UI dry-runs.
 */
interface PlayCatalog {
  val state: StateFlow<PlayCatalogState>

  fun account(): PlayAccount?

  suspend fun signIn()

  suspend fun signInWithOAuth(email: String, oauthToken: String)

  suspend fun signOut()

  suspend fun refreshLatest()

  suspend fun fetch(packageName: String, version: GameVersion = GameVersion.Latest): File

  suspend fun install(file: File)
}

sealed class GameVersion {
  data object Latest : GameVersion()

  data class Release(val name: String, val versionCode: Long) : GameVersion()
}

open class PlayException(val kind: FetchError, detail: String) : Exception(detail)

class NeedGoogleLogin : PlayException(FetchError.UNKNOWN, "google login required")

const val MINECRAFT_PACKAGE = "com.mojang.minecraftpe"

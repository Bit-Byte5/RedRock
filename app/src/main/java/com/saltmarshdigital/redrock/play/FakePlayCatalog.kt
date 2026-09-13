package com.saltmarshdigital.redrock.play

import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory catalog for Quest 2D UI work. No Play protocol, no tokens.
 *
 * Package ids `com.denied.ownership`, `com.denied.tos`, and `com.denied.abi`
 * force the matching [FetchError] so error chrome can be exercised.
 */
class FakePlayCatalog(
  private val stagingDir: File = File(System.getProperty("java.io.tmpdir"), "redrock-fake-play"),
) : PlayCatalog {
  private val mutex = Mutex()
  private val _state = MutableStateFlow(PlayCatalogState())

  override val state: StateFlow<PlayCatalogState> = _state.asStateFlow()

  override fun account(): PlayAccount? =
    (_state.value.account as? AccountState.SignedIn)?.account

  override suspend fun signInWithOAuth(email: String, oauthToken: String) {
    mutex.withLock {
      _state.update { it.copy(account = AccountState.SigningIn) }
      delay(400)
      _state.update {
        it.copy(account = AccountState.SignedIn(PlayAccount(email.ifBlank { FAKE_EMAIL })))
      }
    }
  }

  override suspend fun signOut() {
    mutex.withLock {
      _state.update { it.copy(account = AccountState.SignedOut) }
    }
  }

  override suspend fun signIn() {
    mutex.withLock {
      _state.update { it.copy(account = AccountState.SigningIn) }
      delay(400)
      _state.update {
        it.copy(account = AccountState.SignedIn(PlayAccount(FAKE_EMAIL)))
      }
    }
  }

  override suspend fun refreshLatest() {
    _state.update { it.copy(latestName = "1.21.132.3") }
  }

  override suspend fun fetch(packageName: String, version: GameVersion): File {
    mutex.withLock {
      val pkg = packageName.trim()
      if (pkg.isEmpty()) {
        fail(FetchError.UNKNOWN, "package name is empty")
      }
      if (account() == null) {
        fail(FetchError.OWNERSHIP, "sign in with an account that owns $pkg")
      }
      _state.update { it.copy(fetch = FetchState.Fetching(pkg)) }
      when (pkg) {
        "com.denied.ownership" ->
          fail(FetchError.OWNERSHIP, "this account does not own $pkg")
        "com.denied.tos" ->
          fail(FetchError.TOS, "Play refused $pkg")
        "com.denied.abi" ->
          fail(FetchError.ABI, "no arm64-v8a build for $pkg")
      }
      val total = 8L * 1024L * 1024L
      repeat(8) { step ->
        _state.update {
          it.copy(fetch = FetchState.Fetching(pkg, (step + 1) * (total / 8), total))
        }
        delay(80)
      }
      val apk = File(stagingDir, "$pkg.apk")
      stagingDir.mkdirs()
      apk.writeText("redrock-fake\n")
      _state.update { it.copy(fetch = FetchState.Ready(pkg, apk)) }
      return apk
    }
  }

  override suspend fun install(file: File) {
    mutex.withLock {
      val pkg =
        (_state.value.fetch as? FetchState.Ready)?.packageName
          ?: file.name.removeSuffix(".apk")
      if (account() == null) {
        fail(FetchError.OWNERSHIP, "sign in before installing")
      }
      if (!file.isFile) {
        fail(FetchError.UNKNOWN, "apk missing: ${file.path}")
      }
      _state.update { it.copy(fetch = FetchState.Installing(pkg)) }
      delay(400)
      _state.update { it.copy(fetch = FetchState.Ready(pkg, file)) }
    }
  }

  private fun fail(kind: FetchError, detail: String): Nothing {
    _state.update { it.copy(fetch = FetchState.Error(kind, detail)) }
    throw FakePlayException(kind, detail)
  }

  companion object {
    const val FAKE_EMAIL = "dev@local"
  }
}

class FakePlayException(kind: FetchError, detail: String) : PlayException(kind, detail)

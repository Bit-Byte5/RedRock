package com.saltmarshdigital.redrock.play

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.data.models.PlayFile
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.AuthHelper
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.saltmarshdigital.redrock.game.GameStore
import java.io.File
import java.util.Properties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Real Google Play catalog. Sign-in uses the EmbeddedSetup WebView, then
 * AAS + gplayapi. Tokens stay in [PlayCredentialStore], never in git.
 */
class GooglePlayCatalog(context: Context) : PlayCatalog {
  private val appContext = context.applicationContext
  private val store = PlayCredentialStore(appContext)
  private val games = GameStore(appContext)
  private val http = OkHttpClient()
  private val mutex = Mutex()
  private val _state = MutableStateFlow(PlayCatalogState())
  private var authData: AuthData? = null
  private var lastProgressMs = 0L
  private var pendingVersionName = ""

  override val state: StateFlow<PlayCatalogState> = _state.asStateFlow()

  override fun account(): PlayAccount? =
    (_state.value.account as? AccountState.SignedIn)?.account

  override suspend fun signIn() {
    mutex.withLock {
      val saved = store.load() ?: throw NeedGoogleLogin()
      _state.update { it.copy(account = AccountState.SigningIn) }
      try {
        authData = withContext(Dispatchers.IO) { buildAuth(saved) }
        val latest = withContext(Dispatchers.IO) { peekLatest(authData!!) }
        _state.update {
          it.copy(account = AccountState.SignedIn(PlayAccount(saved.email)), latestName = latest)
        }
      } catch (e: NeedGoogleLogin) {
        _state.update { it.copy(account = AccountState.SignedOut) }
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "sign-in failed: ${e::class.java.simpleName}")
        _state.update { it.copy(account = AccountState.SignedOut) }
        fail(FetchError.TOS, detailOf(e))
      }
    }
  }

  override suspend fun signOut() {
    mutex.withLock {
      authData = null
      store.clear()
      _state.update { it.copy(account = AccountState.SignedOut) }
      Log.i(TAG, "signed out")
    }
  }

  override suspend fun signInWithOAuth(email: String, oauthToken: String) {
    mutex.withLock {
      _state.update { it.copy(account = AccountState.SigningIn) }
      try {
        val androidId = store.androidId()
        val credentials =
          withContext(Dispatchers.IO) { Ac2dm.exchange(email, oauthToken, androidId) }
        store.save(credentials)
        authData = withContext(Dispatchers.IO) { buildAuth(credentials) }
        val latest = withContext(Dispatchers.IO) { peekLatest(authData!!) }
        _state.update {
          it.copy(
            account = AccountState.SignedIn(PlayAccount(credentials.email)),
            latestName = latest,
          )
        }
        Log.i(TAG, "signed in")
      } catch (e: PlayException) {
        _state.update {
          it.copy(
            account = AccountState.SignedOut,
            fetch = FetchState.Error(e.kind, e.message ?: ""),
          )
        }
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "sign-in failed: ${e::class.java.simpleName}")
        _state.update { it.copy(account = AccountState.SignedOut) }
        fail(FetchError.TOS, detailOf(e))
      }
    }
  }

  override suspend fun refreshLatest() {
    mutex.withLock {
      val auth = authData ?: return
      val latest = withContext(Dispatchers.IO) { peekLatest(auth) }
      if (latest.isNotBlank()) _state.update { it.copy(latestName = latest) }
    }
  }

  override suspend fun fetch(packageName: String, version: GameVersion): File {
    mutex.withLock {
      val pkg = packageName.trim()
      if (pkg.isEmpty()) fail(FetchError.UNKNOWN, "package name is empty")
      val auth = authData ?: fail(FetchError.OWNERSHIP, "sign in with an account that owns $pkg")
      _state.update { it.copy(fetch = FetchState.Fetching(pkg)) }
      Log.i(TAG, "fetch $pkg")
      return try {
        withContext(Dispatchers.IO) {
          val app = AppDetailsHelper(auth).getAppByPackageName(pkg)
          val latestName = app.versionName.orEmpty()
          _state.update { it.copy(latestName = latestName.ifBlank { it.latestName }) }
          val versionCode =
            when (version) {
              GameVersion.Latest -> app.versionCode
              is GameVersion.Release -> version.versionCode
            }
          pendingVersionName =
            when (version) {
              GameVersion.Latest -> latestName.ifBlank { "Latest" }
              is GameVersion.Release -> version.name
            }
          val files = PurchaseHelper(auth).purchase(app.packageName, versionCode, app.offerType)
          val dir = File(appContext.cacheDir, "play/$pkg").apply { mkdirs() }
          dir.listFiles()?.forEach { it.delete() }
          val catalogTotal = files.sumOf { it.size.coerceAtLeast(0L) }
          emitFetching(pkg, received = 0L, total = catalogTotal, force = true)
          var completed = 0L
          var base: File? = null
          for (playFile in files) {
            val dest = File(dir, playFile.name.ifBlank { "base.apk" })
            val written =
              download(playFile, dest) { fileReceived, fileLength ->
                val total =
                  when {
                    catalogTotal > 0L -> catalogTotal
                    fileLength > 0L -> completed + fileLength
                    else -> 0L
                  }
                emitFetching(pkg, completed + fileReceived, total)
              }
            completed += written
            emitFetching(pkg, completed, if (catalogTotal > 0L) catalogTotal else completed, force = true)
            if (playFile.type == PlayFile.Type.BASE || dest.name == "base.apk") {
              base = dest
            }
          }
          val apk = base ?: dir.listFiles { _, name -> name.endsWith(".apk") }?.firstOrNull()
          if (apk == null) fail(FetchError.UNKNOWN, "Play sent no APK for $pkg")
          _state.update { it.copy(fetch = FetchState.Ready(pkg, apk)) }
          apk
        }
      } catch (e: PlayException) {
        throw e
      } catch (e: Exception) {
        fail(mapThrowable(e), e.message ?: "fetch failed")
      }
    }
  }

  override suspend fun install(file: File) {
    mutex.withLock {
      val pkg =
        (_state.value.fetch as? FetchState.Ready)?.packageName
          ?: file.parentFile?.name
          ?: file.name.removeSuffix(".apk")
      val apks =
        (file.parentFile?.listFiles { _, name -> name.endsWith(".apk") }?.toList()
            ?: listOf(file))
          .filter { it.isFile }
      if (apks.isEmpty()) fail(FetchError.UNKNOWN, "apk missing: ${file.path}")
      _state.update { it.copy(fetch = FetchState.Installing(pkg)) }
      Log.i(TAG, "extract $pkg (${apks.size} apk)")
      try {
        withContext(Dispatchers.IO) { games.extract(pkg, apks, pendingVersionName) }
        _state.update { it.copy(fetch = FetchState.Ready(pkg, file)) }
      } catch (e: PlayException) {
        throw e
      } catch (e: Exception) {
        fail(FetchError.UNKNOWN, e.message ?: "extract failed")
      }
    }
  }

  private fun peekLatest(auth: AuthData): String {
    return try {
      AppDetailsHelper(auth).getAppByPackageName(MINECRAFT_PACKAGE).versionName.orEmpty()
    } catch (_: Exception) {
      ""
    }
  }

  private fun buildAuth(credentials: PlayCredentials): AuthData {
    return AuthHelper.build(
      credentials.email,
      credentials.aasToken,
      AuthHelper.Token.AAS,
      isAnonymous = false,
      properties = playDevice(),
    )
  }

  private fun playDevice(): Properties {
    val props = Properties()
    appContext.resources.openRawResource(com.aurora.gplayapi.R.raw.gplayapi_px_9a).use {
      props.load(it)
    }
    if (props.isEmpty) fail(FetchError.UNKNOWN, "Play device profile missing")
    return props
  }

  private fun detailOf(e: Exception): String {
    val msg =
      e.message?.takeIf { it.isNotBlank() }
        ?: e.cause?.message?.takeIf { it.isNotBlank() }
        ?: e::class.java.simpleName
    return msg.take(160)
  }

  private fun emitFetching(pkg: String, received: Long, total: Long, force: Boolean = false) {
    val now = SystemClock.elapsedRealtime()
    if (!force && now - lastProgressMs < 120L) return
    lastProgressMs = now
    _state.update {
      it.copy(fetch = FetchState.Fetching(pkg, received.coerceAtLeast(0L), total.coerceAtLeast(0L)))
    }
  }

  private fun download(
    playFile: PlayFile,
    dest: File,
    onProgress: (received: Long, fileLength: Long) -> Unit,
  ): Long {
    val url = playFile.url
    if (url.isBlank()) throw PlayException(FetchError.NETWORK, "empty download url")
    val request = Request.Builder().url(url).build()
    http.newCall(request).execute().use { response ->
      if (!response.isSuccessful) {
        throw PlayException(FetchError.NETWORK, "download ${response.code}")
      }
      val body = response.body ?: throw PlayException(FetchError.NETWORK, "empty body")
      val headerLength = body.contentLength()
      val fileLength =
        when {
          playFile.size > 0L -> playFile.size
          headerLength > 0L -> headerLength
          else -> 0L
        }
      val input = body.byteStream()
      var written = 0L
      dest.outputStream().use { out ->
        val buf = ByteArray(64 * 1024)
        while (true) {
          val n = input.read(buf)
          if (n < 0) break
          out.write(buf, 0, n)
          written += n
          onProgress(written, fileLength)
        }
      }
      onProgress(written, if (fileLength > 0L) fileLength else written)
      return written
    }
  }

  private fun mapThrowable(e: Exception): FetchError {
    val name = e::class.simpleName.orEmpty() + (e.message ?: "")
    return when {
      name.contains("NotPurchased") -> FetchError.OWNERSHIP
      name.contains("NotSupported") -> FetchError.ABI
      e is java.io.IOException -> FetchError.NETWORK
      else -> FetchError.UNKNOWN
    }
  }

  private fun fail(kind: FetchError, detail: String): Nothing {
    _state.update { it.copy(fetch = FetchState.Error(kind, detail)) }
    throw PlayException(kind, detail)
  }

  companion object {
    private const val TAG = "RedRockPlay"
  }
}

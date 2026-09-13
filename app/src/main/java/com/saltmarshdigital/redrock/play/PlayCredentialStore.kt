package com.saltmarshdigital.redrock.play

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlin.random.Random

internal data class PlayCredentials(
  val email: String,
  val aasToken: String,
  val androidId: String,
)

internal class PlayCredentialStore(context: Context) {
  private val prefs: SharedPreferences =
    try {
      val alias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
      EncryptedSharedPreferences.create(
        PREFS_NAME,
        alias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
      )
    } catch (_: Exception) {
      context.getSharedPreferences(PREFS_NAME_FALLBACK, Context.MODE_PRIVATE)
    }

  fun load(): PlayCredentials? {
    val email = prefs.getString(KEY_EMAIL, null) ?: return null
    val aas = prefs.getString(KEY_AAS, null) ?: return null
    val androidId = prefs.getString(KEY_ANDROID_ID, null) ?: return null
    if (email.isBlank() || aas.isBlank()) return null
    return PlayCredentials(email, aas, androidId)
  }

  fun androidId(): String {
    val existing = prefs.getString(KEY_ANDROID_ID, null)
    if (!existing.isNullOrBlank()) return existing
    val generated = Random.nextLong().toULong().toString(16).padStart(16, '0').take(16)
    prefs.edit().putString(KEY_ANDROID_ID, generated).apply()
    return generated
  }

  fun save(credentials: PlayCredentials) {
    prefs
      .edit()
      .putString(KEY_EMAIL, credentials.email)
      .putString(KEY_AAS, credentials.aasToken)
      .putString(KEY_ANDROID_ID, credentials.androidId)
      .apply()
  }

  fun clear() {
    prefs.edit().remove(KEY_EMAIL).remove(KEY_AAS).apply()
  }

  companion object {
    private const val PREFS_NAME = "play_credentials"
    private const val PREFS_NAME_FALLBACK = "play_credentials_plain"
    private const val KEY_EMAIL = "email"
    private const val KEY_AAS = "aas"
    private const val KEY_ANDROID_ID = "android_id"
  }
}

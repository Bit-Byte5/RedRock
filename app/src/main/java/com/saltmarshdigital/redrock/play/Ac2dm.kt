package com.saltmarshdigital.redrock.play

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Exchange a one-time EmbeddedSetup `oauth_token` for an AAS master token.
 * Form matches Aurora Store / gplayapi `getAASTokenParams`.
 */
internal object Ac2dm {
  private const val GMS_SIG = "38918a453d07199354f8b19af05ec6562ced5788"

  private val http =
    OkHttpClient.Builder().followRedirects(true).followSslRedirects(true).build()

  fun exchange(email: String, oauthToken: String, androidId: String): PlayCredentials {
    val token = oauthToken.trim()
    if (!token.startsWith("oauth2_4/")) {
      throw PlayException(FetchError.TOS, "Google auth incomplete token")
    }
    val form = FormBody.Builder()
        .add("accountType", "HOSTED_OR_GOOGLE")
        .add("has_permission", "1")
        .add("add_account", "1")
        .add("get_accountid", "1")
        .add("ACCESS_TOKEN", "1")
        .add("Token", token)
        .add("service", "ac2dm")
        .add("source", "android")
        .add("androidId", androidId)
        .add("device_country", "us")
        .add("operatorCountry", "us")
        .add("lang", "en")
        .add("sdk_version", "34")
        .add("google_play_services_version", "240913000")
        .add("client_sig", GMS_SIG)
        .add("callerSig", GMS_SIG)
        .add("callerPkg", "com.google.android.gms")
        .add("droidguard_results", "null")
    if (email.isNotBlank()) form.add("Email", email)
    val body = form.build()

    val endpoints =
      listOf(
        "https://android.clients.google.com/auth",
        "https://android.googleapis.com/auth",
      )
    var lastCode = 0
    var lastBody = ""
    for (url in endpoints) {
      val request =
        Request.Builder()
          .url(url)
          .header("User-Agent", "GoogleAuth/1.4")
          .header("app", "com.android.vending")
          .header("Accept-Encoding", "identity")
          .post(body)
          .build()
      http.newCall(request).execute().use { response ->
        val text = response.body?.string().orEmpty()
        lastCode = response.code
        lastBody = text
        val fields = parse(text)
        val err = fields["Error"]
        if (response.isSuccessful && err.isNullOrBlank()) {
          val aas = fields["Token"] ?: throw PlayException(FetchError.TOS, "no AAS token")
          val resolvedEmail = fields["Email"] ?: email.ifBlank { "google-account" }
          return PlayCredentials(email = resolvedEmail, aasToken = aas, androidId = androidId)
        }
        if (!err.isNullOrBlank()) {
          lastBody = text
        }
      }
    }
    val err =
      lastBody
        .lineSequence()
        .firstOrNull { it.startsWith("Error=") }
        ?.substringAfter("=")
        ?: lastCode.toString()
    throw PlayException(FetchError.TOS, "Google auth $err")
  }

  fun parseCookies(raw: String): Map<String, String> {
    return raw
      .split(";")
      .map { it.trim() }
      .mapNotNull { part ->
        val eq = part.indexOf('=')
        if (eq <= 0) null
        else part.substring(0, eq) to decodeCookie(part.substring(eq + 1))
      }
      .toMap()
  }

  private fun decodeCookie(value: String): String {
    return try {
      URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8)
    } catch (_: Exception) {
      value
    }
  }

  private fun parse(body: String): Map<String, String> {
    return body
      .lineSequence()
      .map { it.trim() }
      .filter { it.contains('=') }
      .associate { line ->
        val eq = line.indexOf('=')
        line.substring(0, eq) to line.substring(eq + 1)
      }
  }
}

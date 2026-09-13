package com.saltmarshdigital.redrock.play

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.saltmarshdigital.redrock.R
import com.saltmarshdigital.redrock.ui.theme.RR
import java.util.concurrent.atomic.AtomicBoolean

private const val EMBEDDED_SETUP = "https://accounts.google.com/EmbeddedSetup"
private const val OAUTH_COOKIE = "oauth_token"
private const val TAG = "RedRockPlay"
private val COOKIE_URLS =
  listOf(
    "https://accounts.google.com",
    "https://accounts.google.com/EmbeddedSetup",
  )
private const val PROFILE_JS =
  """
  (function(){
    var e = document.getElementById('profileIdentifier');
    if (e && e.innerText) return e.innerText;
    var a = document.querySelector('[data-email]');
    if (a) return a.getAttribute('data-email') || a.innerText || '';
    return '';
  })();
  """

@Composable
fun GoogleLoginPane(onGrant: (email: String, oauthToken: String) -> Unit, onCancel: () -> Unit) {
  val done = remember { AtomicBoolean(false) }
  val grant = rememberUpdatedState(onGrant)
  var status by remember { mutableStateOf("Tap a field to open the Quest keyboard, then sign in.") }
  val view = LocalView.current

  DisposableEffect(Unit) {
    val window = (view.context as? Activity)?.window
    val previous = window?.attributes?.softInputMode
    window?.setSoftInputMode(
      WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
        WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE,
    )
    onDispose {
      if (previous != null) window?.setSoftInputMode(previous)
    }
  }

  Column(modifier = Modifier.fillMaxSize().background(RR.Night)) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(RR.Bar)
          .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
      Text(
        text = stringResource(R.string.google_login_title),
        color = RR.Ink,
        fontSize = 16.sp,
        modifier = Modifier.align(Alignment.CenterStart),
      )
      Text(
        text = stringResource(R.string.google_login_cancel),
        color = RR.Ember,
        fontSize = 16.sp,
        modifier = Modifier.align(Alignment.CenterEnd).clickable(onClick = onCancel),
      )
    }
    AndroidView(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      factory = { context ->
        WebView(context).apply {
          @SuppressLint("SetJavaScriptEnabled")
          settings.javaScriptEnabled = true
          settings.domStorageEnabled = true
          settings.cacheMode = WebSettings.LOAD_DEFAULT
          settings.allowContentAccess = true
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.safeBrowsingEnabled = false
          }
          enableQuestKeyboard()
          val cookies = CookieManager.getInstance()
          cookies.removeAllCookies(null)
          cookies.setAcceptCookie(true)
          cookies.setAcceptThirdPartyCookies(this, true)

          fun finish(email: String, oauth: String) {
            if (!done.compareAndSet(false, true)) return
            Log.i(TAG, "signed in")
            status = email.ifBlank { "Google" }
            grant.value(email, oauth)
          }

          fun readOauth(): Pair<String, String>? {
            CookieManager.getInstance().flush()
            val urls = COOKIE_URLS + listOfNotNull(url)
            for (candidate in urls.distinct()) {
              val raw = CookieManager.getInstance().getCookie(candidate) ?: continue
              val map = Ac2dm.parseCookies(raw)
              val oauth = map[OAUTH_COOKIE]?.trim() ?: continue
              if (!oauth.startsWith("oauth2_4/")) continue
              val email = map["Email"] ?: map["email"].orEmpty()
              return email to oauth
            }
            return null
          }

          val handler = Handler(Looper.getMainLooper())
          val poll =
            object : Runnable {
              override fun run() {
                if (done.get()) return
                val hit = readOauth()
                if (hit != null) {
                  val (cookieEmail, oauth) = hit
                  evaluateJavascript(PROFILE_JS) { js ->
                    val fromJs =
                      js.trim().trim('"').takeIf { it.isNotBlank() && it != "null" }.orEmpty()
                    finish(fromJs.ifBlank { cookieEmail }, oauth)
                  }
                  return
                }
                handler.postDelayed(this, 400)
              }
            }
          setTag(R.id.google_login_poll, handler to poll)

          webViewClient =
            object : WebViewClient() {
              override fun onPageFinished(view: WebView, finishedUrl: String) {
                if (finishedUrl.contains("404") || view.title?.contains("404") == true) {
                  if (finishedUrl != EMBEDDED_SETUP) view.loadUrl(EMBEDDED_SETUP)
                  return
                }
                view.enableQuestKeyboard()
                view.showQuestKeyboard()
                handler.removeCallbacks(poll)
                handler.post(poll)
              }
            }
          handler.postDelayed(poll, 800)
          loadUrl(EMBEDDED_SETUP)
        }
      },
      onRelease = { web ->
        @Suppress("UNCHECKED_CAST")
        val tagged = web.getTag(R.id.google_login_poll) as? Pair<Handler, Runnable>
        tagged?.first?.removeCallbacks(tagged.second)
        web.stopLoading()
        web.destroy()
      },
    )
    Text(
      text = status,
      color = RR.Mute,
      fontSize = 13.sp,
      modifier = Modifier.padding(12.dp),
    )
  }
}

private fun WebView.enableQuestKeyboard() {
  isFocusable = true
  isFocusableInTouchMode = true
  isClickable = true
  descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
  settings.setNeedInitialFocus(true)
  setOnTouchListener { v, event ->
    if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_UP) {
      v.performClick()
      v.requestFocus()
      (v as WebView).showQuestKeyboard()
    }
    false
  }
}

private fun WebView.showQuestKeyboard() {
  requestFocus()
  post {
    requestFocusFromTouch()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.restartInput(this)
    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
  }
}

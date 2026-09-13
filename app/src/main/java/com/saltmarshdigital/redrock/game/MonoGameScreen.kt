package com.saltmarshdigital.redrock.game

import android.graphics.PixelFormat
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.saltmarshdigital.redrock.R
import com.saltmarshdigital.redrock.ui.theme.MojanglesFamily
import com.saltmarshdigital.redrock.ui.theme.RR
import kotlinx.coroutines.flow.collectLatest

private enum class MenuItem {
  Play,
  Settings,
  Leave,
}

@Composable
fun MonoGameScreen(host: GameHost, onLeave: () -> Unit) {
  var focused by remember { mutableIntStateOf(0) }
  var flash by remember { mutableStateOf<String?>(null) }
  val items = MenuItem.entries

  fun activate(item: MenuItem) {
    when (item) {
      MenuItem.Play -> flash = "play"
      MenuItem.Settings -> flash = "settings"
      MenuItem.Leave -> onLeave()
    }
  }

  LaunchedEffect(host) {
    host.pad.collectLatest { action ->
      when (action) {
        PadAction.Confirm -> activate(items[focused])
        PadAction.Back -> onLeave()
        PadAction.Up -> focused = (focused - 1).mod(items.size)
        PadAction.Down -> focused = (focused + 1).mod(items.size)
      }
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(RR.Night)) {
    MonoGameSurface(host = host, modifier = Modifier.fillMaxSize())
    Column(
      modifier = Modifier.align(Alignment.Center),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = stringResource(R.string.app_name),
        color = Color.White,
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = MojanglesFamily,
        style =
          TextStyle(shadow = Shadow(Color.Black, Offset(3f, 3f), 0f)),
      )
      Spacer(modifier = Modifier.height(20.dp))
      MinecraftButton(
        label = stringResource(R.string.mc_button_play),
        focused = focused == 0,
        onFocused = { focused = 0 },
        onClick = { activate(MenuItem.Play) },
      )
      MinecraftButton(
        label = stringResource(R.string.mc_button_settings),
        focused = focused == 1,
        onFocused = { focused = 1 },
        onClick = { activate(MenuItem.Settings) },
      )
      MinecraftButton(
        label = stringResource(R.string.leave),
        focused = focused == 2,
        onFocused = { focused = 2 },
        onClick = onLeave,
      )
      flash?.let { key ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = stringResource(id = flashLabel(key)),
          color = Color.White,
          fontSize = 14.sp,
          fontFamily = MojanglesFamily,
        )
      }
    }
    Text(
      text = stringResource(R.string.mc_input_hint),
      color = RR.Ink,
      fontSize = 12.sp,
      fontFamily = MojanglesFamily,
      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp),
    )
  }
}

private fun flashLabel(key: String): Int {
  return when (key) {
    "settings" -> R.string.mc_pressed_settings
    else -> R.string.mc_pressed_play
  }
}

@Composable
private fun MonoGameSurface(host: GameHost, modifier: Modifier = Modifier) {
  var view by remember { mutableStateOf<SurfaceView?>(null) }
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner, view) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> host.resume()
        Lifecycle.Event.ON_PAUSE -> host.pause()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      host.pause()
    }
  }

  AndroidView(
    modifier = modifier,
    factory = { ctx ->
      SurfaceView(ctx).apply {
        keepScreenOn = true
        isClickable = true
        isFocusable = true
        isFocusableInTouchMode = true
        holder.setFormat(PixelFormat.RGBA_8888)
        holder.addCallback(
          object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
              val w = holder.surfaceFrame.width().coerceAtLeast(1)
              val h = holder.surfaceFrame.height().coerceAtLeast(1)
              host.onSurfaceAvailable(holder.surface, w, h)
            }

            override fun surfaceChanged(
              holder: SurfaceHolder,
              format: Int,
              width: Int,
              height: Int,
            ) {
              host.onSurfaceSizeChanged(width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
              host.onSurfaceDestroyed()
            }
          },
        )
        setOnTouchListener { _, event ->
          host.onMotion(event)
          false
        }
        setOnHoverListener { _, event ->
          host.onMotion(event)
          false
        }
        setOnGenericMotionListener { _, event ->
          host.onGenericMotion(event)
        }
        setOnKeyListener { _, _, event -> host.onKeyEvent(event) }
        view = this
      }
    },
  )
}

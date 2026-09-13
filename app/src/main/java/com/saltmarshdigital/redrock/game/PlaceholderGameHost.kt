package com.saltmarshdigital.redrock.game

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES30
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

private const val STICK_DEAD = 0.55f
private const val NAV_COOLDOWN_MS = 220L

class PlaceholderGameHost : GameHost {
  override val mode: RenderMode = RenderMode.MONO_FULL

  private val _pad =
    MutableSharedFlow<PadAction>(
      extraBufferCapacity = 16,
      onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
  override val pad: SharedFlow<PadAction> = _pad.asSharedFlow()

  @Volatile var pointerX = 0f
    private set
  @Volatile var pointerY = 0f
    private set
  @Volatile var pointerDown = false
    private set

  @Volatile private var running = false
  @Volatile private var paused = false
  @Volatile private var width = 1
  @Volatile private var height = 1

  private val lock = Object()
  private var thread: Thread? = null
  private var lastNavAt = 0L
  private var stickLatched = false

  override fun onMotion(event: MotionEvent) {
    pointerX = event.x
    pointerY = event.y
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN,
      MotionEvent.ACTION_POINTER_DOWN,
      MotionEvent.ACTION_BUTTON_PRESS,
      -> pointerDown = true
      MotionEvent.ACTION_UP,
      MotionEvent.ACTION_POINTER_UP,
      MotionEvent.ACTION_CANCEL,
      MotionEvent.ACTION_BUTTON_RELEASE,
      -> pointerDown = false
      MotionEvent.ACTION_HOVER_ENTER,
      MotionEvent.ACTION_HOVER_MOVE,
      MotionEvent.ACTION_HOVER_EXIT,
      -> Unit
    }
  }

  override fun onKeyEvent(event: KeyEvent): Boolean {
    if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount > 0) return false
    return when (event.keyCode) {
      KeyEvent.KEYCODE_BUTTON_A,
      KeyEvent.KEYCODE_BUTTON_1,
      KeyEvent.KEYCODE_ENTER,
      KeyEvent.KEYCODE_NUMPAD_ENTER,
      KeyEvent.KEYCODE_DPAD_CENTER,
      KeyEvent.KEYCODE_SPACE,
      -> emit(PadAction.Confirm)
      KeyEvent.KEYCODE_BUTTON_B,
      KeyEvent.KEYCODE_BUTTON_2,
      KeyEvent.KEYCODE_BACK,
      KeyEvent.KEYCODE_ESCAPE,
      -> emit(PadAction.Back)
      KeyEvent.KEYCODE_DPAD_UP,
      KeyEvent.KEYCODE_W,
      -> nav(PadAction.Up)
      KeyEvent.KEYCODE_DPAD_DOWN,
      KeyEvent.KEYCODE_S,
      -> nav(PadAction.Down)
      else -> false
    }
  }

  override fun onGenericMotion(event: MotionEvent): Boolean {
    val src = event.source
    val game =
      src and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK ||
        src and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
        src and InputDevice.SOURCE_DPAD == InputDevice.SOURCE_DPAD
    if (!game && event.action != MotionEvent.ACTION_MOVE) return false

    val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
    if (hatY <= -0.5f) return nav(PadAction.Up)
    if (hatY >= 0.5f) return nav(PadAction.Down)

    val y =
      axis(event, MotionEvent.AXIS_Y).takeIf { kotlin.math.abs(it) > STICK_DEAD }
        ?: axis(event, MotionEvent.AXIS_RZ).takeIf { kotlin.math.abs(it) > STICK_DEAD }
        ?: 0f
    if (kotlin.math.abs(y) > STICK_DEAD) {
      if (stickLatched) return true
      stickLatched = true
      return nav(if (y < 0f) PadAction.Up else PadAction.Down)
    }
    stickLatched = false
    return false
  }

  override fun onSurfaceAvailable(surface: Surface, width: Int, height: Int) {
    synchronized(lock) {
      stopLocked()
      this.width = width.coerceAtLeast(1)
      this.height = height.coerceAtLeast(1)
      running = true
      paused = false
      thread = Thread({ loop(surface) }, "redrock-mono").apply { start() }
    }
  }

  override fun onSurfaceSizeChanged(width: Int, height: Int) {
    this.width = width.coerceAtLeast(1)
    this.height = height.coerceAtLeast(1)
  }

  override fun onSurfaceDestroyed() {
    synchronized(lock) { stopLocked() }
  }

  override fun pause() {
    paused = true
  }

  override fun resume() {
    paused = false
  }

  override fun releaseGl() {
    synchronized(lock) { stopLocked() }
  }

  private fun emit(action: PadAction): Boolean = _pad.tryEmit(action)

  private fun nav(action: PadAction): Boolean {
    val now = SystemClock.uptimeMillis()
    if (now - lastNavAt < NAV_COOLDOWN_MS) return true
    lastNavAt = now
    return emit(action)
  }

  private fun axis(event: MotionEvent, axis: Int): Float {
    val device = event.device ?: return event.getAxisValue(axis)
    val range = device.getMotionRange(axis, event.source) ?: return event.getAxisValue(axis)
    val v = event.getAxisValue(axis)
    val flat = range.flat
    return if (kotlin.math.abs(v) < flat) 0f else v
  }

  private fun stopLocked() {
    running = false
    val t = thread
    thread = null
    if (t != null && t !== Thread.currentThread()) {
      t.join(1_000)
    }
  }

  private fun loop(surface: Surface) {
    val egl = EglSession.create(surface)
    if (egl == null) {
      Log.e(TAG, "GLES 3 window surface failed")
      return
    }
    try {
      var t = 0f
      while (running) {
        if (paused) {
          Thread.sleep(32)
          continue
        }
        if (!egl.makeCurrent()) break
        drawFrame(width, height, t)
        if (!egl.swap()) break
        t += 0.032f
        Thread.sleep(32)
      }
    } finally {
      egl.destroy()
    }
  }

  private fun drawFrame(w: Int, h: Int, t: Float) {
    GLES30.glViewport(0, 0, w, h)
    val horizon = (h * 0.58f).toInt().coerceIn(1, h - 1)
    val pulse = 0.03f * kotlin.math.sin(t.toDouble()).toFloat()
    GLES30.glEnable(GLES30.GL_SCISSOR_TEST)
    GLES30.glScissor(0, horizon, w, h - horizon)
    GLES30.glClearColor(0.47f, 0.66f + pulse, 0.98f, 1f)
    GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
    GLES30.glScissor(0, 0, w, horizon)
    GLES30.glClearColor(0.36f, 0.55f, 0.22f, 1f)
    GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
    GLES30.glDisable(GLES30.GL_SCISSOR_TEST)
  }

  companion object {
    private const val TAG = "RedRockMono"
  }
}

private class EglSession(
  private val display: EGLDisplay,
  private val context: EGLContext,
  private val window: EGLSurface,
) {
  fun makeCurrent(): Boolean =
    EGL14.eglMakeCurrent(display, window, window, context)

  fun swap(): Boolean = EGL14.eglSwapBuffers(display, window)

  fun destroy() {
    EGL14.eglMakeCurrent(
      display,
      EGL14.EGL_NO_SURFACE,
      EGL14.EGL_NO_SURFACE,
      EGL14.EGL_NO_CONTEXT,
    )
    EGL14.eglDestroySurface(display, window)
    EGL14.eglDestroyContext(display, context)
    EGL14.eglTerminate(display)
  }

  companion object {
    fun create(surface: Surface): EglSession? {
      val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
      if (display == EGL14.EGL_NO_DISPLAY) return null
      val version = IntArray(2)
      if (!EGL14.eglInitialize(display, version, 0, version, 1)) return null

      val config = chooseConfig(display) ?: run {
        EGL14.eglTerminate(display)
        return null
      }
      val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
      val context =
        EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
      if (context == EGL14.EGL_NO_CONTEXT) {
        EGL14.eglTerminate(display)
        return null
      }
      val window =
        EGL14.eglCreateWindowSurface(
          display,
          config,
          surface,
          intArrayOf(EGL14.EGL_NONE),
          0,
        )
      if (window == EGL14.EGL_NO_SURFACE) {
        EGL14.eglDestroyContext(display, context)
        EGL14.eglTerminate(display)
        return null
      }
      return EglSession(display, context, window)
    }

    private fun chooseConfig(display: EGLDisplay): EGLConfig? {
      val attribs =
        intArrayOf(
          EGL14.EGL_RENDERABLE_TYPE,
          EGLExt.EGL_OPENGL_ES3_BIT_KHR,
          EGL14.EGL_RED_SIZE,
          8,
          EGL14.EGL_GREEN_SIZE,
          8,
          EGL14.EGL_BLUE_SIZE,
          8,
          EGL14.EGL_ALPHA_SIZE,
          8,
          EGL14.EGL_DEPTH_SIZE,
          16,
          EGL14.EGL_NONE,
        )
      val configs = arrayOfNulls<EGLConfig>(1)
      val count = IntArray(1)
      if (!EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, count, 0)) {
        return null
      }
      return configs[0]
    }
  }
}

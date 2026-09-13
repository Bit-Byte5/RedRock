package com.saltmarshdigital.redrock.game

import android.view.Surface

/** First Minecraft path: one framebuffer filling the 2D window. Stereo / OpenXR is later. */
enum class RenderMode {
  MONO_FULL,
}

/**
 * Owns the window [Surface] Minecraft will draw into.
 *
 * The placeholder binds its own GLES 3 context. A later native host must call
 * [releaseGl] first so `libminecraftpe` can create EGL on the same surface.
 */
interface GameHost : GameInput {
  val mode: RenderMode

  fun onSurfaceAvailable(surface: Surface, width: Int, height: Int)

  fun onSurfaceSizeChanged(width: Int, height: Int)

  fun onSurfaceDestroyed()

  fun pause()

  fun resume()

  /** Drop the placeholder EGL context. Surface stays valid for native takeover. */
  fun releaseGl()
}

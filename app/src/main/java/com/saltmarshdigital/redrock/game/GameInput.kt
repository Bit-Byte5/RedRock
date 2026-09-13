package com.saltmarshdigital.redrock.game

import kotlinx.coroutines.flow.SharedFlow

/** Quest / gamepad actions Minecraft menus expect (A confirm, B back, stick/D-pad move). */
enum class PadAction {
  Confirm,
  Back,
  Up,
  Down,
}

/**
 * Controller + pointer sink for the mono host.
 *
 * Horizon 2D already turns the laser + trigger into hover/click on Compose.
 * Face buttons and sticks often land on the Activity — [onKeyEvent] / [onGenericMotion]
 * map those to [PadAction] so Minecraft buttons can be pressed without a mouse.
 * [onMotion] records pointer pose for later `libminecraftpe` injection.
 */
interface GameInput {
  val pad: SharedFlow<PadAction>

  fun onMotion(event: android.view.MotionEvent)

  fun onKeyEvent(event: android.view.KeyEvent): Boolean

  fun onGenericMotion(event: android.view.MotionEvent): Boolean
}

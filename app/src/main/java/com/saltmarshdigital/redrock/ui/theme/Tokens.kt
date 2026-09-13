package com.saltmarshdigital.redrock.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Squarish chrome with a little rounding. */
object RR {
  val Night = Color(0xFF000000)
  val Panel = Color(0xFF1A1A1A)
  val Bar = Color(0xFF161616)
  val Field = Color(0xFF1C1C1C)
  val Chip = Color(0xFF2A2A2A)
  val Mute = Color(0xFF8A8A8A)
  val Ink = Color(0xFFF2F2F2)
  val Paper = Color(0xFFFFFFFF)
  val Flame = Color(0xFFE22B14)
  val Ember = Color(0xFFFF8A3A)

  val PlayGradient = Brush.horizontalGradient(listOf(Ember, Flame))
  val Phosphor = Color(0xFF3DFF6A)

  val Radius = 4.dp
  val Shape = RoundedCornerShape(Radius)

  /** Left nav is the dominant chrome. Bottom bar holds version + Play. */
  val Side = 208.dp
  val BarHeight = 120.dp
}

val RedRockShapes =
  Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RR.Shape,
    medium = RR.Shape,
    large = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(8.dp),
  )

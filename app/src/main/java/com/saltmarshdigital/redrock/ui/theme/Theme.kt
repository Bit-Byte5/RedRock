package com.saltmarshdigital.redrock.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.saltmarshdigital.redrock.R

/** Recreated Minecraft UI typeface (Mojangles equivalent). SIL OFL — third_party/minecraft-font. */
val MojanglesFamily =
  FontFamily(
    Font(R.font.minecraft, FontWeight.Normal),
    Font(R.font.minecraft_bold, FontWeight.Bold),
  )

private val Base = TextStyle(fontFamily = MojanglesFamily)

val MojanglesTypography =
  Typography(
    displayLarge = Base.copy(fontSize = 57.sp, fontWeight = FontWeight.Bold),
    displayMedium = Base.copy(fontSize = 45.sp, fontWeight = FontWeight.Bold),
    displaySmall = Base.copy(fontSize = 36.sp, fontWeight = FontWeight.Bold),
    headlineLarge = Base.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
    headlineMedium = Base.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineSmall = Base.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    titleLarge = Base.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleMedium = Base.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
    titleSmall = Base.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
    bodyLarge = Base.copy(fontSize = 16.sp),
    bodyMedium = Base.copy(fontSize = 14.sp),
    bodySmall = Base.copy(fontSize = 12.sp),
    labelLarge = Base.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
    labelMedium = Base.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
    labelSmall = Base.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
  )

@Composable
fun RedRockTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme =
      darkColorScheme(
        primary = RR.Ember,
        onPrimary = RR.Paper,
        background = RR.Night,
        surface = RR.Bar,
      ),
    typography = MojanglesTypography,
    shapes = RedRockShapes,
  ) {
    CompositionLocalProvider(LocalTextStyle provides Base, content = content)
  }
}

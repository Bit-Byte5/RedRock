package com.saltmarshdigital.redrock.game

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.saltmarshdigital.redrock.ui.theme.MojanglesFamily

private val Face = Color(0xFF6F6F6F)
private val FaceHot = Color(0xFF548C2A)
private val FaceDown = Color(0xFF3C6618)
private val EdgeDark = Color(0xFF1A1A1A)
private val EdgeLight = Color(0xFFE0E0E0)
private val Label = Color(0xFFECECEC)

/** Classic Minecraft menu button. Hover/focus lights green; trigger or A clicks it. */
@Composable
fun MinecraftButton(
  label: String,
  focused: Boolean,
  onFocused: () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val interaction = remember { MutableInteractionSource() }
  val hovered by interaction.collectIsHoveredAsState()
  val pressed by interaction.collectIsPressedAsState()
  val focusRequester = remember { FocusRequester() }
  val hot = focused || hovered

  LaunchedEffect(hovered) {
    if (hovered) onFocused()
  }
  LaunchedEffect(focused) {
    if (focused) {
      runCatching { focusRequester.requestFocus() }
    }
  }

  val face =
    when {
      pressed -> FaceDown
      hot -> FaceHot
      else -> Face
    }

  Box(
    modifier =
      modifier
        .width(400.dp)
        .height(40.dp)
        .focusRequester(focusRequester)
        .hoverable(interaction)
        .focusable(interactionSource = interaction)
        .clickable(
          interactionSource = interaction,
          indication = null,
          role = Role.Button,
          onClick = onClick,
        )
        .border(2.dp, EdgeDark)
        .drawBehind {
          drawRect(face)
          val t = 2.dp.toPx()
          drawRect(EdgeLight, topLeft = Offset.Zero, size = Size(size.width, t))
          drawRect(EdgeLight, topLeft = Offset.Zero, size = Size(t, size.height))
          drawRect(
            Color(0xFF2A2A2A),
            topLeft = Offset(0f, size.height - t),
            size = Size(size.width, t),
          )
          drawRect(
            Color(0xFF2A2A2A),
            topLeft = Offset(size.width - t, 0f),
            size = Size(t, size.height),
          )
        },
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      color = Label,
      fontSize = 16.sp,
      fontWeight = FontWeight.Normal,
      fontFamily = MojanglesFamily,
      textAlign = TextAlign.Center,
      style =
        TextStyle(
          shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 0f),
        ),
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

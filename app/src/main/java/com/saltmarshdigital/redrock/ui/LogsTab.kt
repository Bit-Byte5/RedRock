package com.saltmarshdigital.redrock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saltmarshdigital.redrock.R
import com.saltmarshdigital.redrock.game.GameLog
import com.saltmarshdigital.redrock.ui.theme.MojanglesFamily
import com.saltmarshdigital.redrock.ui.theme.RR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@Composable
fun LogsTab() {
  val context = LocalContext.current.applicationContext
  val clipboard = LocalClipboardManager.current
  var lines by remember { mutableStateOf(listOf<String>()) }
  var copied by remember { mutableStateOf(false) }
  val scroll = rememberScrollState()
  val empty = stringResource(R.string.logs_empty)
  val body = if (lines.isEmpty()) empty else lines.joinToString("\n")

  LaunchedEffect(Unit) {
    while (isActive) {
      lines = withContext(Dispatchers.IO) { GameLog.dump(context) }
      delay(1000)
    }
  }
  LaunchedEffect(lines.size) {
    if (lines.isNotEmpty()) scroll.animateScrollTo(scroll.maxValue)
  }
  LaunchedEffect(copied) {
    if (!copied) return@LaunchedEffect
    delay(1500)
    copied = false
  }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .padding(16.dp)
        .clip(RR.Shape)
        .background(RR.Night)
        .padding(14.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.logs_title),
        color = RR.Phosphor,
        fontSize = 16.sp,
        fontFamily = MojanglesFamily,
      )
      Text(
        text = stringResource(if (copied) R.string.logs_copied else R.string.logs_copy),
        color = if (copied) RR.Night else RR.Phosphor,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = MojanglesFamily,
        modifier =
          Modifier.clip(RR.Shape)
            .background(if (copied) RR.Phosphor else RR.Chip)
            .clickable(role = Role.Button) {
              clipboard.setText(AnnotatedString(body))
              copied = true
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
      )
    }
    Text(
      text = body,
      color = RR.Phosphor,
      fontSize = 12.sp,
      lineHeight = 16.sp,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier.padding(top = 10.dp).verticalScroll(scroll),
    )
  }
}

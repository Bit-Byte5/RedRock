package com.saltmarshdigital.redrock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saltmarshdigital.redrock.R
import com.saltmarshdigital.redrock.play.AccountState
import com.saltmarshdigital.redrock.ui.theme.RR

private enum class SettingsPage {
  Google,
  Mixer,
}

@Composable
fun SettingsTab(
  account: AccountState,
  onSignIn: () -> Unit,
  onSignOut: () -> Unit,
  onSwitchAccount: () -> Unit,
) {
  var page by remember { mutableIntStateOf(0) }
  var look by remember { mutableStateOf(0.72f) }
  var volume by remember { mutableStateOf(0.55f) }
  val pages = SettingsPage.entries
  val current = pages[page]
  Row(
    modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 24.dp, top = 16.dp, bottom = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    Column(
      modifier = Modifier.width(200.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(
        text = stringResource(R.string.tab_settings),
        color = RR.Ink,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(8.dp))
      pages.forEachIndexed { index, item ->
        val selected = index == page
        Text(
          text =
            stringResource(
              when (item) {
                SettingsPage.Google -> R.string.settings_google
                SettingsPage.Mixer -> R.string.settings_mixer
              },
            ),
          color = if (selected) RR.Paper else RR.Ink,
          fontSize = 14.sp,
          fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
          modifier =
            Modifier.fillMaxWidth()
              .clip(RR.Shape)
              .background(if (selected) RR.Ember else RR.Chip)
              .clickable(role = Role.Tab, onClick = { page = index })
              .padding(horizontal = 12.dp, vertical = 10.dp),
        )
      }
    }
    Column(
      modifier = Modifier.fillMaxHeight().weight(1f).verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      when (current) {
        SettingsPage.Google ->
          GoogleAccountPage(
            account = account,
            onSignIn = onSignIn,
            onSignOut = onSignOut,
            onSwitchAccount = onSwitchAccount,
          )
        SettingsPage.Mixer -> MixerPage(look = look, volume = volume, onLook = { look = it }, onVolume = { volume = it })
      }
    }
  }
}

@Composable
private fun MixerPage(
  look: Float,
  volume: Float,
  onLook: (Float) -> Unit,
  onVolume: (Float) -> Unit,
) {
  Text(
    text = stringResource(R.string.settings_mixer),
    color = RR.Ink,
    fontSize = 22.sp,
    fontWeight = FontWeight.Bold,
  )
  SettingsSlider(label = stringResource(R.string.settings_look), value = look, onValue = onLook)
  SettingsSlider(label = stringResource(R.string.settings_volume), value = volume, onValue = onVolume)
}

@Composable
private fun GoogleAccountPage(
  account: AccountState,
  onSignIn: () -> Unit,
  onSignOut: () -> Unit,
  onSwitchAccount: () -> Unit,
) {
  Text(
    text = stringResource(R.string.settings_google),
    color = RR.Ink,
    fontSize = 22.sp,
    fontWeight = FontWeight.Bold,
  )
  Text(
    text = stringResource(R.string.google_profile_hint),
    color = RR.Mute,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  )
  val signedIn = account as? AccountState.SignedIn
  val signingIn = account is AccountState.SigningIn
  val email = signedIn?.account?.email.orEmpty()
  val clickable = !signingIn
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RR.Shape)
        .background(RR.Panel)
        .then(
          if (signedIn == null && clickable) {
            Modifier.clickable(role = Role.Button, onClick = onSignIn)
          } else {
            Modifier
          },
        )
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier =
          Modifier.size(56.dp)
            .clip(CircleShape)
            .background(avatarColor(email.ifBlank { "google" })),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = avatarLetter(email),
          color = RR.Paper,
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text =
            when {
              signedIn != null -> email
              else -> stringResource(R.string.settings_google)
            },
          color = RR.Ink,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text =
            when {
              signingIn -> stringResource(R.string.account_signing_in)
              signedIn != null -> stringResource(R.string.google_signed_in)
              else -> stringResource(R.string.google_tap_to_sign_in)
            },
          color = RR.Mute,
          fontSize = 13.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    if (signedIn != null) {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AccountAction(
          label = stringResource(R.string.google_sign_out),
          emphasis = false,
          enabled = clickable,
          onClick = onSignOut,
        )
        AccountAction(
          label = stringResource(R.string.google_switch),
          emphasis = true,
          enabled = clickable,
          onClick = onSwitchAccount,
        )
      }
    } else if (!signingIn) {
      AccountAction(
        label = stringResource(R.string.google_sign_in),
        emphasis = true,
        enabled = clickable,
        onClick = onSignIn,
      )
    }
  }
}

@Composable
private fun AccountAction(
  label: String,
  emphasis: Boolean,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  Text(
    text = label,
    color = if (emphasis) RR.Paper else RR.Ink,
    fontSize = 14.sp,
    fontWeight = FontWeight.Bold,
    modifier =
      Modifier.clip(RR.Shape)
        .background(if (emphasis) RR.Ember else RR.Chip)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 10.dp),
  )
}

@Composable
private fun SettingsSlider(label: String, value: Float, onValue: (Float) -> Unit) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(text = label, color = RR.Mute, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(28.dp)
          .clip(RR.Shape)
          .background(RR.Field)
          .pointerInput(Unit) {
            detectTapGestures { tap ->
              onValue((tap.x / size.width).coerceIn(0f, 1f))
            }
          }
          .pointerInput(Unit) {
            detectHorizontalDragGestures { change, _ ->
              change.consume()
              onValue((change.position.x / size.width).coerceIn(0f, 1f))
            }
          },
      contentAlignment = Alignment.CenterStart,
    ) {
      Box(
        modifier =
          Modifier.fillMaxWidth(value.coerceIn(0.08f, 1f))
            .height(28.dp)
            .background(RR.PlayGradient),
      )
    }
  }
}

private fun avatarLetter(email: String): String {
  val source = email.substringBefore('@').ifBlank { "G" }
  return source.first().uppercaseChar().toString()
}

private fun avatarColor(seed: String): Color {
  val palette =
    listOf(
      Color(0xFF4285F4),
      Color(0xFF34A853),
      Color(0xFFEA4335),
      Color(0xFF7B61FF),
      Color(0xFFE37400),
    )
  val index = seed.lowercase().sumOf { it.code }.mod(palette.size)
  return palette[index]
}

package com.saltmarshdigital.redrock.ui

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saltmarshdigital.redrock.R
import com.saltmarshdigital.redrock.mods.InstalledMod
import com.saltmarshdigital.redrock.mods.ModInstall
import com.saltmarshdigital.redrock.mods.ModStore
import com.saltmarshdigital.redrock.ui.theme.RR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ModsTab() {
  val context = LocalContext.current
  val store = remember { ModStore(context) }
  var mods by remember { mutableStateOf(store.list()) }
  val scope = rememberCoroutineScope()
  val picker =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri == null) return@rememberLauncherForActivityResult
      scope.launch {
        val result = withContext(Dispatchers.IO) { store.install(uri) }
        val message =
          when (result) {
            is ModInstall.Ok -> context.getString(R.string.mods_added, displayName(result.mod.name))
            ModInstall.BadFile -> context.getString(R.string.mods_add_bad_file)
            ModInstall.Failed -> context.getString(R.string.mods_add_failed)
          }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        mods = store.list()
      }
    }

  fun addMod() {
    try {
      picker.launch(arrayOf("*/*"))
    } catch (_: ActivityNotFoundException) {
      Toast.makeText(context, context.getString(R.string.mods_add_failed), Toast.LENGTH_SHORT).show()
    }
  }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .padding(start = 24.dp, end = 28.dp, top = 20.dp, bottom = 12.dp)
        .verticalScroll(rememberScrollState()),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = stringResource(R.string.mods_title),
        color = RR.Ink,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = stringResource(R.string.mods_add),
        color = RR.Paper,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier =
          Modifier.clip(RR.Shape)
            .background(RR.PlayGradient)
            .clickable(role = Role.Button, onClick = { addMod() })
            .padding(horizontal = 18.dp, vertical = 10.dp),
      )
    }
    Spacer(modifier = Modifier.height(18.dp))
    mods.forEach { mod ->
      ModRow(
        mod = mod,
        onToggle = {
          if (!mod.required) {
            store.setEnabled(mod.name, !mod.enabled)
            mods = store.list()
          }
        },
        onRemove = {
          if (!mod.required) {
            store.remove(mod.name)
            mods = store.list()
          }
        },
      )
      Spacer(modifier = Modifier.height(8.dp))
    }
    if (mods.none { !it.required }) {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = stringResource(R.string.mods_empty),
        color = RR.Mute,
        fontSize = 14.sp,
      )
    }
  }
}

@Composable
private fun ModRow(mod: InstalledMod, onToggle: () -> Unit, onRemove: () -> Unit) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RR.Shape)
        .background(if (mod.enabled) RR.Chip else RR.Field)
        .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(
        text = if (mod.required) stringResource(R.string.mods_core_name) else displayName(mod.name),
        color = RR.Ink,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text =
          if (mod.required) stringResource(R.string.mods_core_meta, formatBytes(mod.sizeBytes))
          else formatBytes(mod.sizeBytes),
        color = RR.Mute,
        fontSize = 12.sp,
      )
    }
    if (!mod.required) {
      Text(
        text = stringResource(R.string.mods_remove),
        color = RR.Mute,
        fontSize = 13.sp,
        modifier =
          Modifier.clip(RR.Shape)
            .clickable(role = Role.Button, onClick = onRemove)
            .padding(horizontal = 8.dp, vertical = 6.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
    }
    Text(
      text =
        stringResource(
          when {
            mod.required -> R.string.mods_required
            mod.enabled -> R.string.mods_on
            else -> R.string.mods_off
          },
        ),
      color = if (mod.enabled) RR.Paper else RR.Mute,
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      modifier =
        Modifier.clip(RR.Shape)
          .background(if (mod.enabled) RR.Ember else RR.Chip)
          .then(
            if (mod.required) Modifier
            else Modifier.clickable(role = Role.Switch, onClick = onToggle),
          )
          .padding(horizontal = 14.dp, vertical = 8.dp),
    )
  }
}

private fun displayName(fileName: String): String {
  return fileName.removePrefix("lib").removeSuffix(".so")
}

private fun formatBytes(n: Long): String {
  val kb = n / 1024.0
  return if (kb < 1024.0) "%.0f KB".format(kb) else "%.1f MB".format(kb / 1024.0)
}

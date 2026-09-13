package com.saltmarshdigital.redrock

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saltmarshdigital.redrock.game.GameRuntime
import com.saltmarshdigital.redrock.game.GameStore
import com.saltmarshdigital.redrock.play.AccountState
import com.saltmarshdigital.redrock.play.FetchError
import com.saltmarshdigital.redrock.play.FetchState
import com.saltmarshdigital.redrock.play.GameVersion
import com.saltmarshdigital.redrock.play.GoogleLoginPane
import com.saltmarshdigital.redrock.play.GooglePlayCatalog
import com.saltmarshdigital.redrock.play.MINECRAFT_PACKAGE
import com.saltmarshdigital.redrock.play.MinecraftReleases
import com.saltmarshdigital.redrock.play.NeedGoogleLogin
import com.saltmarshdigital.redrock.play.PlayCatalog
import com.saltmarshdigital.redrock.play.PlayCatalogState
import com.saltmarshdigital.redrock.play.PlayException
import com.saltmarshdigital.redrock.ui.DocsTab
import com.saltmarshdigital.redrock.ui.LogsTab
import com.saltmarshdigital.redrock.ui.ModsTab
import com.saltmarshdigital.redrock.ui.SettingsTab
import com.saltmarshdigital.redrock.ui.UnofficialPage
import com.saltmarshdigital.redrock.ui.theme.RR
import com.saltmarshdigital.redrock.ui.theme.RedRockTheme
import kotlinx.coroutines.launch

private val GrassTop = Color(0xFF5D9C3F)
private val GrassLit = Color(0xFF7CBD4A)
private val GrassDark = Color(0xFF3E6E28)
private val DirtLeft = Color(0xFFB07A4A)
private val DirtRight = Color(0xFF7A5230)

private val SlideTrackL = Color(0xFF3A3A3A)
private val SlideTrackR = Color(0xFF2A2A2A)
private val SlideFillL = Color(0xFFFF8A3A)
private val SlideFillR = Color(0xFFE22B14)
private val SlideKnobTop = Color(0xFFF2F2F2)
private val SlideKnobL = Color(0xFFD0D0D0)
private val SlideKnobR = Color(0xFF9A9A9A)

private val StackSpineA = Color(0xFF6E4A2C)
private val StackCoverA = Color(0xFFA07448)
private val StackSpineB = Color(0xFFE22B14)
private val StackCoverB = Color(0xFFFF8A3A)
private val StackSpineC = Color(0xFF8A3A1C)
private val StackCoverC = Color(0xFFB85A32)
private val BookPage = Color(0xFFF4E6D8)
private val CrtBeige = Color(0xFFB8B49A)
private val CrtBeigeDark = Color(0xFF7A7660)
private val CrtScreen = Color(0xFF031208)
private val CrtGreen = Color(0xFF3DFF6A)
private val CrtKey = Color(0xFF4A4A4A)
private val ToolLeft = Color(0xFFB85A22)
private val ToolFront = Color(0xFFE07030)
private val ToolRight = Color(0xFF8A3218)
private val ToolTop = Color(0xFFFFAA5A)
private val HandleL = Color(0xFF3A3A3A)
private val HandleF = Color(0xFF5A5A5A)
private val HandleR = Color(0xFF2A2A2A)
private val HandleTop = Color(0xFF9A9A9A)
private val LatchFront = Color(0xFFE8C056)
private val LatchTop = Color(0xFFF4D98A)

private val SideIcon = 80.dp

private enum class LauncherTab {
  Game,
  Mods,
  Docs,
  Logs,
  Settings,
  Unofficial,
}

private enum class GoogleLoginIntent {
  Download,
  Account,
}

class LauncherActivity : ComponentActivity() {
  private val catalog: PlayCatalog by lazy { GooglePlayCatalog(this) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setTheme(R.style.RedRockWindow)
    setContent {
      RedRockTheme {
        RedRockLauncher(catalog = catalog, onPlay = ::playMinecraft)
      }
    }
  }

  private fun playMinecraft() {
    val store = GameStore(this)
    if (store.payload() == null) {
      runCatching { store.adoptPlayCache(MINECRAFT_PACKAGE) }
    }
    if (store.payload() == null) {
      Toast.makeText(this, getString(R.string.play_missing_game), Toast.LENGTH_LONG).show()
      return
    }
    startActivity(
      Intent().setClassName(this, GameStore.MAIN_ACTIVITY).addFlags(
        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
      ),
    )
  }
}

@Composable
private fun RedRockLauncher(catalog: PlayCatalog, onPlay: () -> Unit) {
  var tab by remember { mutableStateOf(LauncherTab.Game) }
  val catalogState by catalog.state.collectAsState()
  val scope = rememberCoroutineScope()
  var googleLogin by remember { mutableStateOf<GoogleLoginIntent?>(null) }
  var pickingVersion by remember { mutableStateOf(false) }
  var selectedVersion by remember { mutableStateOf<GameVersion>(GameVersion.Latest) }
  val busy = catalogBusy(catalogState)
  val context = LocalContext.current
  val downloaded =
    remember(catalogState.fetch, selectedVersion) {
      gameIsReady(context, catalogState.fetch, selectedVersion)
    }

  LaunchedEffect(Unit) {
    try {
      catalog.signIn()
      catalog.refreshLatest()
    } catch (_: NeedGoogleLogin) {
    } catch (_: PlayException) {
    }
  }

  suspend fun runDownload() {
    try {
      val store = GameStore(context)
      val cached = store.cachedApks(MINECRAFT_PACKAGE)
      if (store.payload() == null && cached.isNotEmpty()) {
        catalog.install(cached.first())
        return
      }
      if (catalog.account() == null) catalog.signIn()
      val apk = catalog.fetch(MINECRAFT_PACKAGE, selectedVersion)
      catalog.install(apk)
    } catch (_: NeedGoogleLogin) {
      googleLogin = GoogleLoginIntent.Download
    } catch (_: PlayException) {
      // FetchState.Error is already on the catalog.
    }
  }

  fun openGoogleLogin(intent: GoogleLoginIntent) {
    pickingVersion = false
    googleLogin = intent
  }

  fun signInFromSettings() {
    if (busy || googleLogin != null) return
    scope.launch {
      try {
        catalog.signIn()
      } catch (_: NeedGoogleLogin) {
        openGoogleLogin(GoogleLoginIntent.Account)
      } catch (_: PlayException) {
        openGoogleLogin(GoogleLoginIntent.Account)
      }
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(RR.Night)) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .padding(start = RR.Side, bottom = RR.BarHeight),
      contentAlignment = Alignment.Center,
    ) {
      when (tab) {
        LauncherTab.Game -> GameTab()
        LauncherTab.Mods -> ModsTab()
        LauncherTab.Docs -> DocsTab()
        LauncherTab.Logs -> LogsTab()
        LauncherTab.Settings ->
          SettingsTab(
            account = catalogState.account,
            onSignIn = { signInFromSettings() },
            onSignOut = {
              if (!busy && googleLogin == null) {
                scope.launch { catalog.signOut() }
              }
            },
            onSwitchAccount = {
              if (!busy && googleLogin == null) {
                scope.launch {
                  catalog.signOut()
                  openGoogleLogin(GoogleLoginIntent.Account)
                }
              }
            },
          )
        LauncherTab.Unofficial -> UnofficialPage()
      }
    }
    BottomBar(
      catalogState = catalogState,
      versionLabel = versionChipLabel(selectedVersion),
      pickingVersion = pickingVersion,
      downloaded = downloaded,
      onVersionClick = {
        if (!busy && googleLogin == null) pickingVersion = !pickingVersion
      },
      onPrimary = {
        if (busy || googleLogin != null || pickingVersion) return@BottomBar
        if (downloaded) onPlay() else scope.launch { runDownload() }
      },
      modifier =
        Modifier.align(Alignment.BottomStart)
          .fillMaxWidth()
          .height(RR.BarHeight)
          .padding(start = RR.Side),
    )
    SideBar(
      selected = tab,
      onSelect = { tab = it },
      modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight().width(RR.Side),
    )
    if (pickingVersion && googleLogin == null) {
      VersionPicker(
        selected = selectedVersion,
        onSelect = {
          selectedVersion = it
          pickingVersion = false
        },
        onDismiss = { pickingVersion = false },
      )
    }
    if (googleLogin != null) {
      val resumeDownload = googleLogin == GoogleLoginIntent.Download
      GoogleLoginPane(
        onGrant = { email, oauth ->
          googleLogin = null
          scope.launch {
            try {
              catalog.signInWithOAuth(email, oauth)
              if (resumeDownload) runDownload()
            } catch (_: PlayException) {
            }
          }
        },
        onCancel = { googleLogin = null },
      )
    }
  }
}

@Composable
private fun SideBar(
  selected: LauncherTab,
  onSelect: (LauncherTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.background(RR.Panel).padding(top = 24.dp, bottom = 16.dp)) {
    Row(
      modifier = Modifier.align(Alignment.TopCenter),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Image(
        painter = painterResource(R.drawable.ic_redrock),
        contentDescription = null,
        modifier = Modifier.size(32.dp),
        contentScale = ContentScale.Fit,
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = stringResource(R.string.app_name),
        color = RR.Ink,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
      )
    }
    Column(
      modifier =
        Modifier.align(Alignment.Center)
          .fillMaxWidth()
          .fillMaxHeight()
          .padding(top = 56.dp, bottom = 52.dp),
      verticalArrangement = Arrangement.SpaceEvenly,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      SideTab(
        contentDescription = stringResource(R.string.tab_game),
        selected = selected == LauncherTab.Game,
        onClick = { onSelect(LauncherTab.Game) },
      ) {
        GrassBlock(modifier = Modifier.fillMaxSize())
      }
      SideTab(
        contentDescription = stringResource(R.string.tab_mods),
        selected = selected == LauncherTab.Mods,
        onClick = { onSelect(LauncherTab.Mods) },
      ) {
        ToolboxMark(modifier = Modifier.fillMaxSize())
      }
      SideTab(
        contentDescription = stringResource(R.string.tab_docs),
        selected = selected == LauncherTab.Docs,
        onClick = { onSelect(LauncherTab.Docs) },
      ) {
        BookMark(modifier = Modifier.fillMaxSize())
      }
      SideTab(
        contentDescription = stringResource(R.string.tab_logs),
        selected = selected == LauncherTab.Logs,
        onClick = { onSelect(LauncherTab.Logs) },
      ) {
        ComputerMark(modifier = Modifier.fillMaxSize())
      }
      SideTab(
        contentDescription = stringResource(R.string.tab_settings),
        selected = selected == LauncherTab.Settings,
        onClick = { onSelect(LauncherTab.Settings) },
      ) {
        SliderMark(modifier = Modifier.fillMaxSize())
      }
    }
    val unofficialOn = selected == LauncherTab.Unofficial
    Text(
      text = stringResource(R.string.unofficial),
      color = RR.Ink,
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center,
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .clip(RR.Shape)
          .background(if (unofficialOn) RR.Ember else RR.Chip)
          .clickable(role = Role.Tab, onClick = { onSelect(LauncherTab.Unofficial) })
          .padding(horizontal = 12.dp, vertical = 10.dp),
    )
  }
}

@Composable
private fun SideTab(
  contentDescription: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: @Composable () -> Unit,
) {
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val lift by animateFloatAsState(
    targetValue =
      when {
        pressed -> 1f
        selected -> 0.4f
        else -> 0f
      },
    animationSpec =
      spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    label = "tabLift",
  )

  Box(
    modifier =
      modifier
        .size(SideIcon)
        .semantics { this.contentDescription = contentDescription }
        .clickable(
          interactionSource = interaction,
          indication = null,
          role = Role.Tab,
          onClick = onClick,
        )
        .graphicsLayer {
          translationY = -22f * lift
          rotationX = 18f * lift
          rotationY = -10f * lift
          scaleX = 1f + 0.16f * lift
          scaleY = 1f + 0.16f * lift
          cameraDistance = 6f * density
          transformOrigin = TransformOrigin(0.5f, 0.88f)
        },
    contentAlignment = Alignment.Center,
  ) {
    icon()
  }
}

@Composable
private fun GameTab() {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    LiftedGrassBlock()
  }
}

@Composable
private fun BottomBar(
  catalogState: PlayCatalogState,
  versionLabel: String,
  pickingVersion: Boolean,
  downloaded: Boolean,
  onVersionClick: () -> Unit,
  onPrimary: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val busy = catalogBusy(catalogState)
  val status = fetchStatus(catalogState.fetch).ifBlank { accountLabel(catalogState.account) }
  Row(
    modifier = modifier.background(RR.Bar).padding(horizontal = 20.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      VersionChip(
        label = versionLabel,
        expanded = pickingVersion,
        enabled = !busy,
        onClick = onVersionClick,
      )
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FetchProgressBar(fetch = catalogState.fetch, modifier = Modifier.width(180.dp))
        Text(
          text = status,
          color = if (catalogState.fetch is FetchState.Error) RR.Flame else RR.Mute,
          fontSize = 12.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    PlayOrDownloadButton(
      downloaded = downloaded,
      fetch = catalogState.fetch,
      enabled = !busy && !pickingVersion,
      onClick = onPrimary,
    )
  }
}

@Composable
private fun VersionChip(
  label: String,
  expanded: Boolean,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier.widthIn(min = 148.dp)
        .height(56.dp)
        .clip(RR.Shape)
        .background(if (expanded) RR.Ember else RR.Chip)
        .clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      color = RR.Ink,
      fontSize = 16.sp,
      fontWeight = FontWeight.Medium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(end = 8.dp),
    )
    Text(
      text = if (expanded) "▴" else "▾",
      color = RR.Ink,
      fontSize = 14.sp,
    )
  }
}

@Composable
private fun VersionPicker(
  selected: GameVersion,
  onSelect: (GameVersion) -> Unit,
  onDismiss: () -> Unit,
) {
  val choices = remember { listOf(GameVersion.Latest) + MinecraftReleases.all }
  Box(
    modifier =
      Modifier.fillMaxSize()
        .padding(start = RR.Side, bottom = RR.BarHeight)
        .background(Color(0xCC000000))
        .clickable(onClick = onDismiss),
  ) {
    Column(
      modifier =
        Modifier.align(Alignment.BottomStart)
          .padding(start = 20.dp, bottom = 12.dp)
          .width(280.dp)
          .clip(RR.Shape)
          .background(RR.Panel)
          .clickable(onClick = {})
          .verticalScroll(rememberScrollState())
          .padding(vertical = 8.dp),
    ) {
      Text(
        text = stringResource(R.string.version_title),
        color = RR.Mute,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
      )
      choices.forEach { version ->
        val active = version == selected
        val title =
          when (version) {
            GameVersion.Latest -> stringResource(R.string.version_latest)
            is GameVersion.Release -> version.name
          }
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .background(if (active) RR.Chip else Color.Transparent)
              .clickable { onSelect(version) }
              .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = title,
            color = if (active) RR.Ember else RR.Ink,
            fontSize = 16.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
          )
        }
      }
    }
  }
}

@Composable
private fun PlayOrDownloadButton(
  downloaded: Boolean,
  fetch: FetchState,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  val transferring = fetch is FetchState.Fetching || fetch is FetchState.Installing
  val fraction = (fetch as? FetchState.Fetching)?.fraction ?: if (fetch is FetchState.Installing) 1f else 0f
  val fill by animateFloatAsState(targetValue = fraction, label = "play-download")
  val label =
    when {
      transferring -> downloadLabel(fetch)
      downloaded -> stringResource(R.string.play)
      else -> stringResource(R.string.download)
    }
  Box(
    modifier =
      Modifier.width(180.dp)
        .height(56.dp)
        .clip(RR.Shape)
        .then(
          if (transferring) Modifier.background(RR.Field)
          else Modifier.background(RR.PlayGradient)
        )
        .clickable(enabled = enabled, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    if (transferring) {
      Box(
        modifier =
          Modifier.align(Alignment.CenterStart)
            .fillMaxHeight()
            .fillMaxWidth(fill.coerceIn(0.04f, 1f))
            .background(RR.PlayGradient),
      )
    }
    Text(
      text = label,
      color = RR.Paper,
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.1.sp,
    )
  }
}

@Composable
private fun FetchProgressBar(fetch: FetchState, modifier: Modifier = Modifier) {
  val fetching = fetch as? FetchState.Fetching
  val installing = fetch is FetchState.Installing
  if (fetching == null && !installing) return
  val known = fetching?.fraction
  val pulse = rememberInfiniteTransition(label = "download-indeterminate")
  val sweep by pulse.animateFloat(
    initialValue = 0.12f,
    targetValue = 0.55f,
    animationSpec =
      infiniteRepeatable(animation = tween(900, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
    label = "download-sweep",
  )
  val target = known ?: if (installing) 1f else sweep
  val fill by animateFloatAsState(targetValue = target, label = "download-bar")
  Box(
    modifier = modifier.height(8.dp).clip(RR.Shape).background(RR.Chip),
  ) {
    Box(
      modifier =
        Modifier.fillMaxHeight()
          .fillMaxWidth(fill.coerceIn(0.04f, 1f))
          .background(RR.PlayGradient),
    )
  }
}

@Composable
private fun accountLabel(account: AccountState): String {
  return when (account) {
    AccountState.SignedOut -> stringResource(R.string.account_signed_out)
    AccountState.SigningIn -> stringResource(R.string.account_signing_in)
    is AccountState.SignedIn -> account.account.email
  }
}

@Composable
private fun downloadLabel(fetch: FetchState): String {
  return when (fetch) {
    is FetchState.Fetching -> {
      val pct = fetch.fraction
      if (pct != null) "${(pct * 100).toInt()}%" else stringResource(R.string.download_fetching)
    }
    is FetchState.Installing -> stringResource(R.string.download_installing)
    else -> stringResource(R.string.download)
  }
}

@Composable
private fun fetchStatus(fetch: FetchState): String {
  return when (fetch) {
    FetchState.Idle -> ""
    is FetchState.Fetching ->
      when {
        fetch.totalBytes > 0L ->
          "${formatBytes(fetch.receivedBytes)} / ${formatBytes(fetch.totalBytes)}"
        fetch.receivedBytes > 0L -> formatBytes(fetch.receivedBytes)
        else -> ""
      }
    is FetchState.Installing -> ""
    is FetchState.Ready -> ""
    is FetchState.Error ->
      when (fetch.kind) {
        FetchError.OWNERSHIP -> stringResource(R.string.error_ownership)
        FetchError.TOS -> fetch.detail.ifBlank { stringResource(R.string.error_tos) }
        FetchError.ABI -> stringResource(R.string.error_abi)
        FetchError.NETWORK -> fetch.detail.ifBlank { stringResource(R.string.error_network) }
        FetchError.UNKNOWN -> fetch.detail.ifBlank { stringResource(R.string.error_unknown) }
      }
  }
}

private fun catalogBusy(state: PlayCatalogState): Boolean {
  return state.account is AccountState.SigningIn ||
    state.fetch is FetchState.Fetching ||
    state.fetch is FetchState.Installing
}

private fun gameIsReady(
  context: android.content.Context,
  fetch: FetchState,
  selected: GameVersion,
): Boolean {
  val onDisk = GameRuntime.available(context, MINECRAFT_PACKAGE)
  if (!onDisk) return false
  return when (selected) {
    GameVersion.Latest -> true
    is GameVersion.Release -> GameStore(context).extractedVersion() == selected.name
  }
}

@Composable
private fun versionChipLabel(selected: GameVersion): String {
  return when (selected) {
    GameVersion.Latest -> stringResource(R.string.version_latest)
    is GameVersion.Release -> selected.name
  }
}

private fun formatBytes(n: Long): String {
  val mb = n / (1024.0 * 1024.0)
  return if (mb < 10.0) "%.1f MB".format(mb) else "%.0f MB".format(mb)
}

@Composable
private fun LiftedGrassBlock() {
  val motion = rememberInfiniteTransition(label = "lift")
  val lift by motion.animateFloat(
    initialValue = -8f,
    targetValue = 12f,
    animationSpec =
      infiniteRepeatable(
        animation = tween(2600, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse,
      ),
    label = "liftY",
  )
  val tilt by motion.animateFloat(
    initialValue = -10f,
    targetValue = -4f,
    animationSpec =
      infiniteRepeatable(
        animation = tween(2600, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse,
      ),
    label = "tilt",
  )

  Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
    GrassBlock(
      modifier =
        Modifier.size(220.dp).graphicsLayer {
          translationY = lift
          rotationX = 8f
          rotationY = tilt
          cameraDistance = 16f * density
          transformOrigin = TransformOrigin(0.5f, 0.6f)
        },
    )
  }
}

@Composable
private fun GrassBlock(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val cx = size.width * 0.5f
    val topY = size.height * 0.22f
    val s = size.width * 0.36f
    val h = size.height * 0.32f

    val top =
      isoTop(cx, topY, s)
    val left =
      isoLeft(cx, topY, s, h)
    val right =
      isoRight(cx, topY, s, h)

    drawPath(left, DirtLeft)
    drawPath(right, DirtRight)
    drawPath(isoLeft(cx, topY, s, h * 0.18f), GrassDark)
    drawPath(isoRight(cx, topY, s, h * 0.18f), GrassTop)
    drawPath(top, GrassLit)
  }
}

@Composable
private fun ToolboxMark(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val s = size.minDimension * 0.32f
    val o = Offset(size.width * 0.49f, size.height * 0.46f)

    fun p(x: Float, y: Float, z: Float): Offset {
      return Offset(o.x + (x - y) * s, o.y + (x + y) * s * 0.5f - z * s)
    }

    fun face(a: Offset, b: Offset, c: Offset, d: Offset, color: Color) {
      drawPath(
        Path().apply {
          moveTo(a.x, a.y)
          lineTo(b.x, b.y)
          lineTo(c.x, c.y)
          lineTo(d.x, d.y)
          close()
        },
        color,
      )
    }

    fun box(
      x0: Float,
      y0: Float,
      z0: Float,
      w: Float,
      d: Float,
      h: Float,
      left: Color,
      front: Color,
      right: Color,
      top: Color,
    ) {
      val x1 = x0 + w
      val y1 = y0 + d
      val z1 = z0 + h
      face(p(x0, y0, z1), p(x0, y1, z1), p(x0, y1, z0), p(x0, y0, z0), left)
      face(p(x0, y1, z1), p(x1, y1, z1), p(x1, y1, z0), p(x0, y1, z0), front)
      face(p(x1, y1, z1), p(x1, y0, z1), p(x1, y0, z0), p(x1, y1, z0), right)
      face(p(x0, y0, z1), p(x1, y0, z1), p(x1, y1, z1), p(x0, y1, z1), top)
    }

    box(0.00f, 0.18f, 0.00f, 1.18f, 0.72f, 0.50f, ToolLeft, ToolFront, ToolRight, ToolTop)
    box(0.00f, 0.18f, 0.50f, 1.18f, 0.72f, 0.12f, ToolLeft, ToolFront, ToolRight, ToolTop)
    box(0.50f, 0.82f, 0.22f, 0.18f, 0.10f, 0.18f, LatchFront, LatchFront, ToolRight, LatchTop)
    box(0.30f, 0.46f, 0.62f, 0.08f, 0.10f, 0.22f, HandleL, HandleF, HandleR, HandleTop)
    box(0.80f, 0.46f, 0.62f, 0.08f, 0.10f, 0.22f, HandleL, HandleF, HandleR, HandleTop)
    box(0.30f, 0.46f, 0.82f, 0.58f, 0.10f, 0.08f, HandleL, HandleF, HandleR, HandleTop)
  }
}

@Composable
private fun SliderMark(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val levels = floatArrayOf(0.40f, 0.88f, 0.62f)
    val s = size.width * 0.105f
    val fullH = size.height * 0.50f
    val bottom = size.height * 0.84f
    val pitch = size.width * 0.30f
    val startCx = size.width * 0.5f - pitch
    val trackTop = bottom - s - fullH
    levels.forEachIndexed { i, level ->
      val cx = startCx + i * pitch
      drawPath(isoLeft(cx, trackTop, s, fullH), SlideTrackL)
      drawPath(isoRight(cx, trackTop, s, fullH), SlideTrackR)
      val h = fullH * level
      val fillTop = bottom - s - h
      drawPath(isoLeft(cx, fillTop, s, h), SlideFillL)
      drawPath(isoRight(cx, fillTop, s, h), SlideFillR)
      val knob = s * 1.22f
      val knobH = s * 0.72f
      val ky = fillTop - knobH * 0.22f
      drawPath(isoLeft(cx, ky, knob, knobH), SlideKnobL)
      drawPath(isoRight(cx, ky, knob, knobH), SlideKnobR)
      drawPath(isoTop(cx, ky, knob), SlideKnobTop)
    }
  }
}

@Composable
private fun BookMark(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val s = size.minDimension * 0.32f
    val o = Offset(size.width * 0.46f, size.height * 0.44f)

    fun p(x: Float, y: Float, z: Float): Offset {
      return Offset(o.x + (x - y) * s, o.y + (x + y) * s * 0.5f - z * s)
    }

    fun face(a: Offset, b: Offset, c: Offset, d: Offset, color: Color) {
      drawPath(
        Path().apply {
          moveTo(a.x, a.y)
          lineTo(b.x, b.y)
          lineTo(c.x, c.y)
          lineTo(d.x, d.y)
          close()
        },
        color,
      )
    }

    fun slab(
      x0: Float,
      y0: Float,
      z0: Float,
      w: Float,
      d: Float,
      thick: Float,
      spine: Color,
      cover: Color,
    ) {
      val x1 = x0 + w
      val y1 = y0 + d
      val z1 = z0 + thick
      face(p(x0, y0, z1), p(x0, y1, z1), p(x0, y1, z0), p(x0, y0, z0), spine)
      face(p(x0, y1, z1), p(x1, y1, z1), p(x1, y1, z0), p(x0, y1, z0), cover)
      face(p(x1, y1, z1), p(x1, y0, z1), p(x1, y0, z0), p(x1, y1, z0), BookPage)
      face(p(x0, y0, z1), p(x1, y0, z1), p(x1, y1, z1), p(x0, y1, z1), cover)
    }

    val thick = 0.20f
    val w = 1.12f
    val d = 0.82f
    slab(-0.10f, 0.08f, 0f, w, d, thick, StackSpineA, StackCoverA)
    slab(0.08f, -0.06f, thick, w, d, thick, StackSpineB, StackCoverB)
    slab(-0.04f, 0.04f, thick * 2f, w, d, thick, StackSpineC, StackCoverC)
  }
}

@Composable
private fun ComputerMark(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val s = size.minDimension * 0.32f
    val o = Offset(size.width * 0.52f, size.height * 0.48f)

    fun p(x: Float, y: Float, z: Float): Offset {
      return Offset(o.x + (x - y) * s, o.y + (x + y) * s * 0.5f - z * s)
    }

    fun face(a: Offset, b: Offset, c: Offset, d: Offset, color: Color) {
      drawPath(
        Path().apply {
          moveTo(a.x, a.y)
          lineTo(b.x, b.y)
          lineTo(c.x, c.y)
          lineTo(d.x, d.y)
          close()
        },
        color,
      )
    }

    fun box(
      x0: Float,
      y0: Float,
      z0: Float,
      w: Float,
      d: Float,
      h: Float,
      left: Color,
      front: Color,
      right: Color,
      top: Color,
    ) {
      val x1 = x0 + w
      val y1 = y0 + d
      val z1 = z0 + h
      face(p(x0, y0, z1), p(x0, y1, z1), p(x0, y1, z0), p(x0, y0, z0), left)
      face(p(x0, y1, z1), p(x1, y1, z1), p(x1, y1, z0), p(x0, y1, z0), front)
      face(p(x1, y1, z1), p(x1, y0, z1), p(x1, y0, z0), p(x1, y1, z0), right)
      face(p(x0, y0, z1), p(x1, y0, z1), p(x1, y1, z1), p(x0, y1, z1), top)
    }

    box(0.00f, 0.52f, 0.00f, 1.10f, 0.50f, 0.12f, CrtKey, CrtBeigeDark, CrtKey, CrtBeige)
    box(0.08f, 0.18f, 0.12f, 0.94f, 0.40f, 0.92f, CrtBeigeDark, CrtBeige, CrtBeigeDark, CrtBeige)

    val sx0 = 0.20f
    val sx1 = 0.92f
    val sy = 0.58f
    val sz0 = 0.28f
    val sz1 = 0.92f
    face(p(sx0, sy, sz1), p(sx1, sy, sz1), p(sx1, sy, sz0), p(sx0, sy, sz0), CrtScreen)

    val bars = floatArrayOf(0.90f, 0.62f, 0.78f, 0.44f, 0.70f)
    bars.forEachIndexed { i, frac ->
      val top = sz1 - 0.08f - i * 0.11f
      val bot = top - 0.055f
      val right = sx0 + 0.06f + (sx1 - sx0 - 0.10f) * frac
      face(
        p(sx0 + 0.06f, sy, top),
        p(right, sy, top),
        p(right, sy, bot),
        p(sx0 + 0.06f, sy, bot),
        CrtGreen,
      )
    }
  }
}

private fun isoTop(cx: Float, topY: Float, s: Float): Path {
  return Path().apply {
    moveTo(cx, topY)
    lineTo(cx + s, topY + s * 0.5f)
    lineTo(cx, topY + s)
    lineTo(cx - s, topY + s * 0.5f)
    close()
  }
}

private fun isoLeft(cx: Float, topY: Float, s: Float, h: Float): Path {
  return Path().apply {
    moveTo(cx - s, topY + s * 0.5f)
    lineTo(cx, topY + s)
    lineTo(cx, topY + s + h)
    lineTo(cx - s, topY + s * 0.5f + h)
    close()
  }
}

private fun isoRight(cx: Float, topY: Float, s: Float, h: Float): Path {
  return Path().apply {
    moveTo(cx + s, topY + s * 0.5f)
    lineTo(cx, topY + s)
    lineTo(cx, topY + s + h)
    lineTo(cx + s, topY + s * 0.5f + h)
    close()
  }
}

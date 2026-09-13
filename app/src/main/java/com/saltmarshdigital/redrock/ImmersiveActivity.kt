package com.saltmarshdigital.redrock

import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.saltmarshdigital.redrock.ui.theme.MojanglesFamily
import com.saltmarshdigital.redrock.ui.theme.RR
import com.saltmarshdigital.redrock.ui.theme.RedRockTheme
import com.meta.spatial.compose.ComposeFeature
import com.meta.spatial.compose.ComposeViewPanelRegistration
import com.meta.spatial.core.Color4
import com.meta.spatial.core.Entity
import com.meta.spatial.core.Pose
import com.meta.spatial.core.SpatialFeature
import com.meta.spatial.core.Vector2
import com.meta.spatial.core.Vector3
import com.meta.spatial.runtime.AlphaMode
import com.meta.spatial.runtime.ReferenceSpace
import com.meta.spatial.toolkit.AppSystemActivity
import com.meta.spatial.toolkit.DpPerMeterDisplayOptions
import com.meta.spatial.toolkit.Material
import com.meta.spatial.toolkit.Mesh
import com.meta.spatial.toolkit.MeshCollision
import com.meta.spatial.toolkit.Panel
import com.meta.spatial.toolkit.PanelRegistration
import com.meta.spatial.toolkit.PanelStyleOptions
import com.meta.spatial.toolkit.Quad
import com.meta.spatial.toolkit.QuadShapeOptions
import com.meta.spatial.toolkit.Transform
import com.meta.spatial.toolkit.UIPanelSettings
import com.meta.spatial.vr.VRFeature

class ImmersiveActivity : AppSystemActivity() {
  override fun registerFeatures(): List<SpatialFeature> {
    return listOf(VRFeature(this), ComposeFeature())
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
  }

  override fun onSceneReady() {
    super.onSceneReady()

    scene.setReferenceSpace(ReferenceSpace.LOCAL_FLOOR)
    scene.setLightingEnvironment(
      ambientColor = Vector3(0f, 0f, 0f),
      sunColor = Vector3(0f, 0f, 0f),
      sunDirection = -Vector3(0f, 1f, 0f),
      environmentIntensity = 0f,
    )
    scene.setViewOrigin(0.0f, 0.0f, 0.0f, 0.0f)

    Entity.create(
      listOf(
        Mesh("mesh://skybox".toUri(), hittable = MeshCollision.NoCollision),
        Material().apply {
          baseColor = Color4(0f, 0f, 0f, 1f)
          unlit = true
        },
        Transform(Pose(Vector3(0f, 0f, 0f))),
      ),
    )

    // Contact glow under the cube so it reads as lifted off black.
    Entity.create(
      listOf(
        Mesh("mesh://quad".toUri(), hittable = MeshCollision.NoCollision),
        Quad(Vector2(-0.42f, -0.12f), Vector2(0.42f, 0.12f)),
        Material().apply {
          baseColor = Color4(0.72f, 0.22f, 0.10f, 0.55f)
          unlit = true
          alphaMode = AlphaMode.TRANSLUCENT.mode
        },
        Transform(Pose(Vector3(0f, 1.08f, -1.62f))),
      ),
    )

    Entity.create(
      listOf(
        Mesh("mesh://quad".toUri()),
        Quad(Vector2(-0.38f, -0.38f), Vector2(0.38f, 0.38f)),
        Material().apply {
          baseTextureAndroidResourceId = R.drawable.ic_redrock
          unlit = true
        },
        Transform(Pose(Vector3(0f, 1.46f, -1.48f))),
      ),
    )

    Entity.create(
      listOf(
        Panel(R.id.title_panel),
        Transform(Pose(Vector3(0f, 1.92f, -1.42f))),
      ),
    )
    Entity.create(
      listOf(
        Panel(R.id.exit_panel),
        Transform(Pose(Vector3(0f, 0.92f, -1.22f))),
      ),
    )
  }

  override fun registerPanels(): List<PanelRegistration> {
    return listOf(
      ComposeViewPanelRegistration(
        R.id.title_panel,
        composeViewCreator = { _, ctx ->
          ComposeView(ctx).apply { setContent { RedRockTheme { TitleMark() } } }
        },
        settingsCreator = { _ ->
          UIPanelSettings(
            shape = QuadShapeOptions(width = 1.1f, height = 0.28f),
            style = PanelStyleOptions(themeResourceId = R.style.RedRockWindow),
            display = DpPerMeterDisplayOptions(),
          )
        },
      ),
      ComposeViewPanelRegistration(
        R.id.exit_panel,
        composeViewCreator = { _, ctx ->
          ComposeView(ctx).apply {
            setContent { RedRockTheme { LeaveButton(onLeave = ::returnToWindow) } }
          }
        },
        settingsCreator = { _ ->
          UIPanelSettings(
            shape = QuadShapeOptions(width = 0.55f, height = 0.18f),
            style = PanelStyleOptions(themeResourceId = R.style.RedRockWindow),
            display = DpPerMeterDisplayOptions(),
          )
        },
      ),
    )
  }

  private fun returnToWindow() {
    startActivity(
      Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_HOME)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
    finish()
  }
}

@Composable
private fun TitleMark() {
  Box(
    modifier = Modifier.fillMaxSize().background(RR.Night),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = stringResource(R.string.app_name),
      color = RR.Ink,
      fontSize = 42.sp,
      fontFamily = MojanglesFamily,
      fontWeight = FontWeight.Bold,
      letterSpacing = 2.sp,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun LeaveButton(onLeave: () -> Unit) {
  Box(
    modifier =
      Modifier.fillMaxSize()
        .clip(RR.Shape)
        .background(RR.PlayGradient)
        .clickable(onClick = onLeave),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = stringResource(R.string.leave),
      color = RR.Paper,
      fontSize = 18.sp,
      fontFamily = MojanglesFamily,
      fontWeight = FontWeight.SemiBold,
      textAlign = TextAlign.Center,
    )
  }
}

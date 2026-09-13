package com.saltmarshdigital.redrock.game

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.content.res.AssetManager
import android.content.res.Resources

/**
 * Makes hosted Minecraft see [GameStore.DEFAULT_PACKAGE] and the extracted
 * native libs / APKs. Files stay in RedRock private storage.
 */
internal class MinecraftHostContext(
  base: Context,
  private val payload: GamePayload,
  private val loader: ClassLoader,
  private val assetsOverride: AssetManager,
  private val resourcesOverride: Resources,
) : ContextWrapper(base) {
  private val hostedAppInfo: ApplicationInfo =
    ApplicationInfo(base.applicationInfo).apply {
      packageName = payload.packageName
      nativeLibraryDir = payload.libDir.absolutePath
      sourceDir = payload.apks.first().absolutePath
      publicSourceDir = sourceDir
      if (payload.apks.size > 1) {
        splitSourceDirs = payload.apks.drop(1).map { it.absolutePath }.toTypedArray()
      }
    }

  override fun getPackageName(): String = payload.packageName

  override fun getOpPackageName(): String = payload.packageName

  override fun getApplicationInfo(): ApplicationInfo = hostedAppInfo

  override fun getClassLoader(): ClassLoader = loader

  override fun getAssets(): AssetManager = assetsOverride

  override fun getResources(): Resources = resourcesOverride

  override fun getApplicationContext(): Context = this
}

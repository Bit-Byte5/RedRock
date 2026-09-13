package com.saltmarshdigital.redrock

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.util.Log
import com.saltmarshdigital.redrock.game.GameRuntime
import com.saltmarshdigital.redrock.game.GameStore
import java.lang.reflect.Field

class RedRockApp : Application() {
  override fun attachBaseContext(base: Context) {
    super.attachBaseContext(base)
    val proc = processName()
    Log.i(TAG, "process=$proc")
    if (!isMinecraftProcess(proc)) return
    try {
      GameRuntime.attach(this)
    } catch (error: Throwable) {
      Log.e(TAG, "Minecraft host attach failed", error)
    }
  }

  override fun onCreate() {
    super.onCreate()
    wrapHostedActivity()
  }

  override fun getClassLoader(): ClassLoader {
    return if (isMinecraftProcess()) {
      runCatching { GameRuntime.minecraftClassLoader() }.getOrElse { super.getClassLoader() }
    } else {
      super.getClassLoader()
    }
  }

  private fun wrapHostedActivity() {
    registerActivityLifecycleCallbacks(
      object : ActivityLifecycleCallbacks {
        override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
          if (activity.javaClass.name != GameStore.MAIN_ACTIVITY) return
          runCatching { System.loadLibrary("mcpelauncher_mod") }
            .onFailure { Log.w(TAG, "mcpelauncher_mod: ${it.message}") }
          replaceBaseContext(activity)
          val resources = GameRuntime.resourceContext()?.resources ?: activity.resources
          val mcTheme =
            resources.getIdentifier("AppTheme", "style", GameStore.DEFAULT_PACKAGE)
          activity.setTheme(if (mcTheme != 0) mcTheme else R.style.RedRockGame)
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

        override fun onActivityStarted(activity: Activity) = Unit

        override fun onActivityResumed(activity: Activity) = Unit

        override fun onActivityPaused(activity: Activity) = Unit

        override fun onActivityStopped(activity: Activity) = Unit

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

        override fun onActivityDestroyed(activity: Activity) = Unit
      },
    )
  }

  private fun replaceBaseContext(activity: Activity) {
    try {
      val field: Field = ContextWrapper::class.java.getDeclaredField("mBase")
      field.isAccessible = true
      field.set(activity, GameRuntime.wrapContext(activity.baseContext))
    } catch (error: Throwable) {
      Log.w(TAG, "could not wrap Minecraft context", error)
    }
  }

  companion object {
    private const val TAG = "RedRockGame"

    fun isMinecraftProcess(): Boolean = isMinecraftProcess(processName())

    fun isMinecraftProcess(name: String): Boolean {
      return name.endsWith(":minecraft") || name.contains(":minecraft")
    }

    fun processName(): String {
      val api = runCatching { getProcessName() }.getOrNull().orEmpty()
      if (isMinecraftProcess(api)) return api
      val cmd =
        runCatching {
          java.io.File("/proc/self/cmdline")
            .readBytes()
            .takeWhile { it != 0.toByte() }
            .toByteArray()
            .toString(Charsets.UTF_8)
        }
          .getOrNull()
          .orEmpty()
      return cmd.ifBlank { api }
    }
  }
}

package com.saltmarshdigital.redrock.game

import android.app.Activity
import android.app.AppComponentFactory
import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.Log
import com.saltmarshdigital.redrock.RedRockApp
import java.io.File

class MinecraftComponentFactory : AppComponentFactory() {
  override fun instantiateClassLoader(cl: ClassLoader, aInfo: ApplicationInfo): ClassLoader {
    val proc = RedRockApp.processName()
    Log.i(TAG, "classLoader process=$proc dataDir=${aInfo.dataDir}")
    if (!RedRockApp.isMinecraftProcess(proc)) return cl
    return GameRuntime.attachFromDataDir(File(aInfo.dataDir), cl) ?: cl
  }

  override fun instantiateActivity(
    cl: ClassLoader,
    className: String,
    intent: Intent?,
  ): Activity {
    if (className != GameStore.MAIN_ACTIVITY) {
      return super.instantiateActivity(cl, className, intent)
    }
    val app = currentApplication()
    if (app != null) {
      GameRuntime.attach(app)
    } else {
      GameRuntime.attachFromDataDir(
        dataDir() ?: File("/data/user/0/com.saltmarshdigital.redrock"),
        cl,
      )
    }
    val loader = GameRuntime.minecraftClassLoader()
    Thread.currentThread().contextClassLoader = loader
    try {
      GameRuntime.startPairIp()
    } catch (error: Throwable) {
      Log.e(TAG, "PairIP startup failed", error)
      throw error
    }
    val type = Class.forName(className, true, loader)
    val ctor = type.getDeclaredConstructor()
    ctor.isAccessible = true
    Log.i(TAG, "instantiate $className")
    return ctor.newInstance() as Activity
  }

  private fun currentApplication(): Application? {
    return try {
      val thread = Class.forName("android.app.ActivityThread")
      thread.getMethod("currentApplication").invoke(null) as? Application
    } catch (_: Throwable) {
      null
    }
  }

  private fun dataDir(): File? {
    return try {
      val at = Class.forName("android.app.ActivityThread")
      val thread = at.getMethod("currentActivityThread").invoke(null) ?: return null
      val bound =
        at.getDeclaredField("mBoundApplication").apply { isAccessible = true }.get(thread)
          ?: return null
      val info =
        bound.javaClass.getDeclaredField("appInfo").apply { isAccessible = true }.get(bound)
          as? ApplicationInfo
      info?.dataDir?.let { File(it) }
    } catch (_: Throwable) {
      null
    }
  }

  private companion object {
    const val TAG = "RedRockGame"
  }
}

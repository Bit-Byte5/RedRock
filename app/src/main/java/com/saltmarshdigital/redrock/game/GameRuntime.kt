package com.saltmarshdigital.redrock.game

import android.app.Application
import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.content.res.loader.ResourcesLoader
import android.content.res.loader.ResourcesProvider
import android.os.ParcelFileDescriptor
import android.system.Os
import android.util.Log
import dalvik.system.PathClassLoader
import java.io.File
import java.util.zip.ZipFile

/**
 * Hosts Minecraft inside RedRock's `:minecraft` process from an extracted
 * Play payload. Do not attach the Horizon-installed Minecraft package.
 */
object GameRuntime {
  @Volatile private var classLoader: ClassLoader? = null
  @Volatile private var resourceContext: Context? = null
  @Volatile private var payload: GamePayload? = null
  private val resourceFds = mutableListOf<ParcelFileDescriptor>()

  fun available(context: Context, packageName: String = GameStore.DEFAULT_PACKAGE): Boolean {
    return GameStore(context).payload(packageName) != null
  }

  fun attach(app: Application) {
    attach(GameStore(app), app, app.classLoader)
  }

  fun attachFromDataDir(dataDir: File, parent: ClassLoader): ClassLoader? {
    val store = GameStore.fromDataDir(dataDir)
    return try {
      attach(store, host = null, parent = parent)
      classLoader
    } catch (error: Throwable) {
      Log.e(TAG, "attach from ${dataDir.path} failed", error)
      null
    }
  }

  @Synchronized
  fun attach(store: GameStore, host: Context?, parent: ClassLoader) {
    val next = store.payload() ?: throw IllegalStateException("Minecraft payload missing")
    if (classLoader == null) {
      attachExtracted(parent, next, host)
    }
    if (host != null && resourceContext == null) {
      bindResources(host, next)
    }
  }

  fun minecraftClassLoader(): ClassLoader {
    return classLoader ?: error("GameRuntime not attached")
  }

  fun resourceContext(): Context? = resourceContext

  fun wrapContext(base: Context): Context {
    val extracted = payload ?: return resourceContext ?: base
    val loader = classLoader ?: return base
    val hosted = resourceContext
    val assets = hosted?.assets ?: base.assets
    val resources = hosted?.resources ?: base.resources
    return MinecraftHostContext(base, extracted, loader, assets, resources)
  }

  /**
   * Play-delivered Bedrock 1.26+ wraps the real Application in PairIP.
   * That class's `<clinit>` runs `StartupLauncher` / `VMRunner`, which
   * decrypts scrambled native PLT entries. Skipping it lets
   * `libmaesdk.so` constructors jump to unmapped memory.
   *
   * Point VMRunner at the extracted `base.apk` so it does not read
   * RedRock's own manifest. Do not call `SignatureCheck.verifyIntegrity`
   * — that compares the running process cert to Mojang's.
   */
  fun startPairIp() {
    val loader = classLoader ?: return
    val extracted = payload ?: return
    val baseApk =
      extracted.baseApk() ?: error("PairIP: extracted base.apk missing")
    Thread.currentThread().contextClassLoader = loader
    val vm = Class.forName(PAIRIP_VM, true, loader)
    val apkPath = vm.getDeclaredField("apkPath")
    apkPath.isAccessible = true
    apkPath.set(null, baseApk.absolutePath)
    Log.i(TAG, "PairIP apkPath=${baseApk.absolutePath}")
    Class.forName(PAIRIP_APPLICATION, true, loader)
    Log.i(TAG, "PairIP StartupLauncher finished")
  }

  private fun attachExtracted(parent: ClassLoader, extracted: GamePayload, host: Context?) {
    Log.i(TAG, "host extracted ${extracted.packageName} ${extracted.libDir}")
    payload = extracted
    System.setProperty("redrock.minecraftpe", extracted.minecraftpe.absolutePath)
    System.setProperty("redrock.minecraft.libdir", extracted.libDir.absolutePath)
    runCatching {
      Os.setenv("REDROCK_MINECRAFTPE", extracted.minecraftpe.absolutePath, true)
      Os.setenv("REDROCK_MINECRAFT_LIBDIR", extracted.libDir.absolutePath, true)
      val filesDir = extracted.dir.parentFile?.parentFile
      if (filesDir != null) {
        Os.setenv("REDROCK_MODS_DIR", File(filesDir, "mods").absolutePath, true)
      }
    }
    installPlayStubs(extracted.libDir, host)
    val dexPath = extracted.dexPath()
    Log.i(TAG, "dex $dexPath")
    val loader = PathClassLoader(dexPath, extracted.libDir.absolutePath, parent)
    classLoader = loader
    Thread.currentThread().contextClassLoader = loader
  }

  private fun nativeLibraryDir(host: Context?): File? {
    host?.applicationInfo?.nativeLibraryDir?.let { return File(it) }
    return runCatching {
      val app =
        Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null)
          as? Context
      app?.applicationInfo?.nativeLibraryDir?.let { File(it) }
    }
      .getOrNull()
  }

  private fun hostApk(host: Context?): File? {
    host?.applicationInfo?.sourceDir?.let { return File(it) }
    return runCatching {
      val app =
        Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null)
          as? Context
      app?.applicationInfo?.sourceDir?.let { File(it) }
    }
      .getOrNull()
  }

  private fun installPlayStubs(libDir: File, host: Context?) {
    val srcDir = nativeLibraryDir(host)
    val apk = hostApk(host)
    for (name in STUB_LIBS) {
      val dest = File(libDir, name)
      val src = srcDir?.let { File(it, name) }?.takeIf { it.isFile }
      if (src != null) {
        src.copyTo(dest, overwrite = true)
      } else if (apk != null && copyStubFromApk(apk, name, dest)) {
        Unit
      } else {
        Log.w(TAG, "stub missing $name")
        continue
      }
      dest.setReadable(true, false)
      dest.setExecutable(true, false)
      Log.i(TAG, "stub $name ${dest.length()}")
    }
  }

  private fun copyStubFromApk(apk: File, name: String, dest: File): Boolean {
    return try {
      ZipFile(apk).use { zip ->
        val entry = zip.getEntry("lib/arm64-v8a/$name") ?: return false
        dest.outputStream().use { output ->
          zip.getInputStream(entry).use { input -> input.copyTo(output) }
        }
      }
      true
    } catch (error: Throwable) {
      Log.w(TAG, "stub unzip $name failed", error)
      false
    }
  }

  private fun bindResources(host: Context, extracted: GamePayload) {
    val loader = classLoader ?: return
    val wrapped = ExtractedResourceContext(host, extracted, loader)
    resourceContext =
      MinecraftHostContext(
        host,
        extracted,
        loader,
        wrapped.assets,
        wrapped.resources,
      )
    overlayResources(host.resources, extracted.apks)
  }

  private fun overlayResources(resources: Resources, apks: List<File>) {
    try {
      val loader = ResourcesLoader()
      for (apk in apks) {
        if (!isDexApk(apk)) continue
        val fd = ParcelFileDescriptor.open(apk, ParcelFileDescriptor.MODE_READ_ONLY)
        resourceFds += fd
        loader.addProvider(ResourcesProvider.loadFromApk(fd))
      }
      resources.addLoaders(loader)
    } catch (error: Throwable) {
      Log.w(TAG, "resource overlay failed", error)
    }
  }

  private const val TAG = "RedRockGame"
  private const val PAIRIP_VM = "com.pairip.VMRunner"
  private const val PAIRIP_APPLICATION = "com.pairip.application.Application"
  private val STUB_LIBS =
    listOf("libpairipcore.so", "libmaesdk.so", "libPlayFabMultiplayer.so")
}

internal class ExtractedResourceContext(
  base: Context,
  private val payload: GamePayload,
  private val loader: ClassLoader,
) : android.content.ContextWrapper(base) {
  val hostedAssets: AssetManager = run {
    try {
      val manager = AssetManager::class.java.getDeclaredConstructor().newInstance()
      val add = AssetManager::class.java.getMethod("addAssetPath", String::class.java)
      for (apk in payload.apks) {
        add.invoke(manager, apk.absolutePath)
      }
      manager
    } catch (_: Throwable) {
      base.assets
    }
  }
  val hostedResources =
    Resources(hostedAssets, base.resources.displayMetrics, base.resources.configuration)

  override fun getClassLoader(): ClassLoader = loader

  override fun getAssets(): AssetManager = hostedAssets

  override fun getResources(): Resources = hostedResources
}

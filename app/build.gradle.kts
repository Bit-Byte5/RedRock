plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.jetbrains.kotlin.android)
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "com.saltmarshdigital.redrock"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.saltmarshdigital.redrock"
    minSdk = 34
    targetSdk = 34
    versionCode = 1
    versionName = "0.1.0"
    ndk {
      abiFilters += "arm64-v8a"
    }
    externalNativeBuild {
      cmake {
        arguments += "-DANDROID_STL=c++_shared"
      }
    }
  }

  ndkVersion = "27.0.12077973"

  externalNativeBuild {
    cmake {
      path = file("src/main/cpp/CMakeLists.txt")
      version = "3.22.1"
    }
  }

  packaging {
    resources.excludes.add("META-INF/LICENSE")
    jniLibs {
      useLegacyPackaging = true
    }
  }

  lint {
    abortOnError = false
    checkReleaseBuilds = false
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions { jvmTarget = "17" }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.material3)
  implementation(libs.androidx.material.icons.extended)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.security.crypto)
  implementation(libs.okhttp)
  implementation(libs.gplayapi)
  debugImplementation(libs.androidx.ui.tooling)

  implementation(libs.meta.spatial.sdk.base)
  implementation(libs.meta.spatial.sdk.compose)
  implementation(libs.meta.spatial.sdk.toolkit)
  implementation(libs.meta.spatial.sdk.vr)
  implementation(libs.meta.spatial.sdk.uiset)
}

plugins {
    // AGP 9.4 内置 Kotlin 支持，无需再应用 org.jetbrains.kotlin.android
    alias(libs.plugins.android.application)
}

android {
    namespace = "org.fcitx.fcitx5.android.plugin.voice_input"

    // 遵循 fcitx5-android 插件包名规范：org.fcitx.fcitx5.android.plugin.<name>
    compileSdk = 36

    defaultConfig {
        applicationId = "org.fcitx.fcitx5.android.plugin.voice_input"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "语音输入 (Voice Input | Debug)")
            // 与插件规范一致：debug 变体对接 debug 版主程序
            buildConfigField("String", "MAIN_APPLICATION_ID", "\"org.fcitx.fcitx5.android.debug\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            resValue("string", "app_name", "语音输入 (Voice Input)")
            buildConfigField("String", "MAIN_APPLICATION_ID", "\"org.fcitx.fcitx5.android\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // AGP 9 内置 Kotlin：通过 android.kotlin 配置编译目标
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    lint {
        // 与上游约定一致，另避免 NewApi/UseKtx 类提示阻断 release 校验
        disable += setOf("UseKtx")
    }

    buildFeatures {
        buildConfig = true
        resValues = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.preference)
}

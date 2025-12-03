import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.central.uploader)
}

// 版本号从根项目的 ext.anifluxVersion 统一管理
project.ext.set("publishArtifactId", "aniflux")
project.ext.set("publishVersion", rootProject.ext.get("anifluxVersion") as String)


android {
    namespace = "com.kernelflux.aniflux"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        consumerProguardFiles("consumer-rules.pro")

        ndk {
            if (project.hasProperty("arm64-only")) {
                abiFilters.add("arm64-v8a")
            } else {
                abiFilters.add("armeabi-v7a")
                abiFilters.add("arm64-v8a")
            }
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    publishing {
        singleVariant("release") {}
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        // 跳过元数据版本检查，因为 OkHttp 可能使用较新的 Kotlin 版本编译
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

//noinspection NewerVersionAvailable,UseTomlInstead
dependencies {
    api(libs.androidx.core.ktx)
    api(libs.androidx.appcompat)
    api(libs.fragment)
    api(libs.androidx.exifinterface)
    api(libs.okhttp)

    // 使用本地源码依赖
    debugApi(project(path = ":animLibs:lottie"))
    debugApi(project(path = ":animLibs:libpag:android:libpag"))
    debugApi(project(path = ":animLibs:svga"))
    debugApi(project(path = ":animLibs:android-gif-drawable"))
    debugApi(project(path = ":animLibs:vap"))


    releaseApi("com.kernelflux.mobile:aniflux-gif-lib:0.0.3")
    releaseApi("com.kernelflux.mobile:aniflux-pag-lib:0.0.3")
    releaseApi("com.kernelflux.mobile:aniflux-svga-lib:0.0.3")
    releaseApi("com.kernelflux.mobile:aniflux-vap-lib:0.0.3")
    releaseApi("com.kernelflux.mobile:aniflux-lottie-lib:0.0.3")


}


// 应用通用发布配置
apply(from = rootProject.file("gradle/maven-publish.gradle"))

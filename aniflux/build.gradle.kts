import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.central.uploader)
}


// 发布配置
project.ext.set("publishArtifactId", "aniflux")
project.ext.set("publishVersion", "1.0.6")
project.ext.set("publishBundleName", "aniflux_bundle_v${project.ext.get("publishVersion")}")


android {
    namespace = "com.kernelflux.aniflux"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
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
    }
}

//noinspection NewerVersionAvailable,UseTomlInstead
dependencies {
    api(libs.androidx.core.ktx)
    api(libs.androidx.appcompat)
    api("androidx.fragment:fragment:1.8.9")

    api("androidx.exifinterface:exifinterface:1.4.1")
    api("com.squareup.okhttp3:okhttp:5.1.0")

    // 使用本地源码依赖
    debugApi(project(path = ":animLibs:lottie"))
    debugApi(project(path = ":animLibs:libpag:android:libpag"))
    debugApi(project(path = ":animLibs:svga"))
    debugApi(project(path = ":animLibs:android-gif-drawable"))
    debugApi(project(path = ":animLibs:vap"))


    releaseApi("com.kernelflux.mobile:aniflux-gif:1.0.6")
    releaseApi("com.kernelflux.mobile:aniflux-pag:1.0.6")
    releaseApi("com.kernelflux.mobile:aniflux-svga:1.0.6")
    releaseApi("com.kernelflux.mobile:aniflux-vap:1.0.6")
    releaseApi("com.kernelflux.mobile:aniflux-lottie:1.0.6")



}


// 应用通用发布配置
apply(from = rootProject.file("gradle/maven-publish.gradle"))

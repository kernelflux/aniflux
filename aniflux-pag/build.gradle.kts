import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.maven.central.uploader)
}

// 版本号从根项目的 ext.anifluxVersion 统一管理
project.ext.set("publishArtifactId", "aniflux-pag")
project.ext.set("publishVersion", rootProject.ext.get("anifluxVersion") as String)

android {
    namespace = "com.kernelflux.aniflux.pag"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
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

dependencies {
    debugApi(project(":aniflux-annotations"))
    debugApi(project(":aniflux-core"))
    debugApi(project(path = ":animLibs:libpag:android:libpag"))
    kspDebug(project(":aniflux-compiler-ksp"))


    releaseApi(libs.aniflux.pag.lib)
    releaseApi(libs.aniflux.core)
    releaseApi(libs.aniflux.annotations)
    kspRelease(libs.aniflux.compiler.ksp)

}
apply(from = rootProject.file("gradle/maven-publish.gradle"))


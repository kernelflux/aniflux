import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.maven.central.uploader)
}

// 发布配置
// 版本号从根项目的 ext.anifluxVersion 统一管理
project.ext.set("publishArtifactId", "aniflux-core")
project.ext.set("publishVersion", rootProject.ext.get("anifluxVersion") as String)
project.ext.set("publishBundleName", "aniflux_core_bundle_v${project.ext.get("publishVersion")}")

android {
    namespace = "com.kernelflux.aniflux"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "consumer-rules.pro"
            )
            // consumerProguardFiles 会将规则打包进 aar，供使用该库的应用使用
            consumerProguardFiles("consumer-rules.pro")
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
    api(libs.androidx.core.ktx)
    api(libs.androidx.appcompat)
    api(libs.fragment)
    api(libs.androidx.exifinterface)
    api(libs.okhttp)
}

// 应用通用发布配置
apply(from = rootProject.file("gradle/maven-publish.gradle"))


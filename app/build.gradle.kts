import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    //alias(libs.plugins.aniflux.register)
}

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties()
if (keystorePropsFile.exists()) {
    keystoreProps.load(keystorePropsFile.inputStream())
}
// helper to read env if property missing
fun propOrEnv(key: String): String? =
    (keystoreProps.getProperty(key) ?: System.getenv(key))?.takeIf { it.isNotBlank() }

android {
    namespace = "com.kernelflux.anifluxsample"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.kernelflux.anifluxsample"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

    }

    signingConfigs {
        create("release") {
            storeFile = file(propOrEnv("storeFile") ?: "")
            storePassword = propOrEnv("storePassword")
            keyAlias = propOrEnv("keyAlias")
            keyPassword = propOrEnv("keyPassword")
        }
    }



    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
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

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }
}


tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}


dependencies {
    implementation(fileTree("libs") {
        include("*.jar", "*.aar")
    })

    implementation(libs.material)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.glide)
    implementation(libs.androidx.lifecycle.runtime.ktx)


    debugImplementation(project(":aniflux"))

//    debugImplementation(project(":aniflux-core"))
//    debugImplementation(project(":aniflux-gif"))
//    debugImplementation(project(":aniflux-pag"))
//    debugImplementation(project(":aniflux-lottie"))
//    debugImplementation(project(":aniflux-svga"))
//    debugImplementation(project(":aniflux-vap"))


    releaseImplementation(libs.aniflux.core)
    releaseImplementation(libs.aniflux.gif)
    releaseImplementation(libs.aniflux.pag)
    releaseImplementation(libs.aniflux.lottie)
    releaseImplementation(libs.aniflux.svga)
    releaseImplementation(libs.aniflux.vap)


}
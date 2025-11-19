plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

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
    kotlinOptions {
        jvmTarget = "11"
    }
}

//noinspection NewerVersionAvailable,UseTomlInstead
dependencies {
    api(fileTree("libs") {
        include("*.jar", "*.aar")
    })

    api(libs.androidx.core.ktx)
    api(libs.androidx.appcompat)
    api("androidx.fragment:fragment:1.8.9")

    api("androidx.exifinterface:exifinterface:1.4.1")
    api("com.squareup.okhttp3:okhttp:5.1.0")
    api("com.airbnb.android:lottie:6.7.1")
    api(project(path = ":animLibs:libpag:android:libpag"))
    api(project(path = ":animLibs:svgaplayer"))
    api(project(path = ":animLibs:android-gif-drawable"))
    api(project(path = ":animLibs:vap"))

}
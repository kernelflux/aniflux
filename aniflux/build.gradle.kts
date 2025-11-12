plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.kernelflux.aniflux"
    compileSdk = 36

    defaultConfig {
        minSdk = 21

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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    api("androidx.exifinterface:exifinterface:1.4.1")
    api("com.squareup.okhttp3:okhttp:5.1.0")
    api("com.airbnb.android:lottie:6.7.1")

    //api("com.tencent.tav:libpag:4.5.2")
    //api("io.github.tencent:vap:2.0.28")
    //api("pl.droidsonroids.gif:android-gif-drawable:1.2.29")
    //api("com.github.yyued:svgaplayer-android:2.6.1")
    api(project(path = ":animLibs:svgaplayer"))
    api(project(path = ":animLibs:android-gif-drawable"))
    api(project(path = ":animLibs:vap"))

}
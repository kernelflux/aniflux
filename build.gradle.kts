// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    // Provide kotlin jvm plugin for pure Kotlin libraries (non-Android)
    //noinspection NewerVersionAvailable
    id("org.jetbrains.kotlin.jvm") version "2.0.21" apply false
}

// Globally load private.properties file (if exists)
val privatePropsFile = rootProject.file("private.properties")
if (privatePropsFile.exists()) {
    val privateProps = java.util.Properties()
    privateProps.load(java.io.FileInputStream(privatePropsFile))
    privateProps.forEach { (key, value) ->
        if (!rootProject.hasProperty(key.toString())) {
            rootProject.ext.set(key.toString(), value)
        }
    }
    
    // Set Gradle Plugin Portal credentials as system properties and project properties
    // This must be done before plugin-publish plugin is applied
    val gradlePublishKey = privateProps.getProperty("gradlePublishKey")
    val gradlePublishSecret = privateProps.getProperty("gradlePublishSecret")
    if (gradlePublishKey != null && gradlePublishSecret != null) {
        // Set as system properties (plugin-publish plugin reads these)
        if (System.getProperty("gradle.publish.key") == null) {
            System.setProperty("gradle.publish.key", gradlePublishKey)
        }
        if (System.getProperty("gradle.publish.secret") == null) {
            System.setProperty("gradle.publish.secret", gradlePublishSecret)
        }
        
        // Also set as root project properties (for subprojects to access)
        rootProject.extensions.extraProperties.set("gradle.publish.key", gradlePublishKey)
        rootProject.extensions.extraProperties.set("gradle.publish.secret", gradlePublishSecret)
    }
}

// Unified version management: read from version catalog, set to ext for all subprojects (including Groovy)
// Bottom layer library version (libraries under animLibs)
rootProject.ext.set("anifluxLibVersion", libs.versions.anifluxLib.get())
// Upper layer module version (aniflux-xxx modules)
rootProject.ext.set("anifluxVersion", libs.versions.aniflux.get())

// Global dependency resolution strategy: force using versions that support API 21
subprojects {
    configurations.all {
        resolutionStrategy {
            // Force all Kotlin-related dependencies to use version 2.0.21
            force("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.0.21")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.21")
            force("org.jetbrains.kotlin:kotlin-stdlib-common:2.0.21")
            force("org.jetbrains.kotlin:kotlin-reflect:2.0.21")

            // savedstate library: use version that supports API 21
            force("androidx.savedstate:savedstate-ktx:1.2.1")
            force("androidx.savedstate:savedstate:1.2.1")
            force("androidx.savedstate:savedstate-android:1.2.1")
            // lifecycle library: use version that supports API 21 (2.6.x supports API 21)
            force("androidx.lifecycle:lifecycle-livedata:2.6.2")
            force("androidx.lifecycle:lifecycle-livedata-core:2.6.2")
            force("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
            force("androidx.lifecycle:lifecycle-viewmodel:2.6.2")
            force("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
            force("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.6.2")
            force("androidx.lifecycle:lifecycle-viewmodel-savedstate-ktx:2.6.2")
            force("androidx.lifecycle:lifecycle-runtime:2.6.2")
            force("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
            force("androidx.lifecycle:lifecycle-common:2.6.2")
            force("androidx.lifecycle:lifecycle-common-java8:2.6.2")
        }
    }
}
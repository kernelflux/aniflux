// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    // 为纯 Kotlin 库（非 Android）提供 kotlin jvm 插件
    //noinspection NewerVersionAvailable
    id("org.jetbrains.kotlin.jvm") version "2.0.21" apply false
}

// 全局加载 private.properties 文件（如果存在）
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

// 统一管理版本号：从 version catalog 读取，设置到 ext 中，供所有子项目（包括 Groovy）使用
// 底层库版本（animLibs 下的库）
rootProject.ext.set("anifluxLibVersion", libs.versions.anifluxLib.get())
// 上层模块版本（aniflux-xxx 模块）
rootProject.ext.set("anifluxVersion", libs.versions.aniflux.get())

// 全局依赖解析策略：强制使用支持 API 21 的版本
subprojects {
    configurations.all {
        resolutionStrategy {
            // 强制所有 Kotlin 相关依赖使用 2.0.21 版本
            force("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.0.21")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.21")
            force("org.jetbrains.kotlin:kotlin-stdlib-common:2.0.21")
            force("org.jetbrains.kotlin:kotlin-reflect:2.0.21")

            // savedstate 库：使用支持 API 21 的版本
            force("androidx.savedstate:savedstate-ktx:1.2.1")
            force("androidx.savedstate:savedstate:1.2.1")
            force("androidx.savedstate:savedstate-android:1.2.1")
            // lifecycle 库：使用支持 API 21 的版本（2.6.x 支持 API 21）
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
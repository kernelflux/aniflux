// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false

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
}

// 全局依赖解析策略：强制使用支持 API 21 的版本
subprojects {
    configurations.all {
        resolutionStrategy {
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
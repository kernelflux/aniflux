import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
    id("java-library") // Required to expose API
    alias(libs.plugins.plugin.publish)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

group = "com.kernelflux.aniflux.plugin"
version = rootProject.ext.get("anifluxVersion") as String

gradlePlugin {
    website.set("https://github.com/kernelflux/aniflux")
    vcsUrl.set("https://github.com/kernelflux/aniflux.git")
    plugins {
        create("aniflux-plugin") {
            id = "com.kernelflux.aniflux.register"
            implementationClass = "com.kernelflux.aniflux.register.AniFluxRegisterPlugin"
            displayName = "AniFlux Gradle Plugin"
            description = "Auto-register AniFlux loaders via bytecode instrumentation"
            tags.set(listOf("android", "animation", "bytecode", "instrumentation", "gradle-plugin"))
        }
    }
}

dependencies {
    implementation(gradleApi())
    //noinspection UseTomlInstead,AndroidGradlePluginVersion
    compileOnly("com.android.tools.build:gradle:8.13.0")
    //noinspection UseTomlInstead
    implementation("org.ow2.asm:asm:9.9")
    //noinspection UseTomlInstead
    implementation("org.ow2.asm:asm-commons:9.9")
}

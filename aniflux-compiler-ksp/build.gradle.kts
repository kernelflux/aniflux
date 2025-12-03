import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.maven.central.uploader)
}


// 版本号从根项目的 ext.anifluxVersion 统一管理
project.ext.set("publishArtifactId", "aniflux-compiler-ksp")
project.ext.set("publishVersion", rootProject.ext.get("anifluxVersion") as String)


java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(libs.ksp.symbol.processing.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    implementation(project(":aniflux-annotations"))
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

// 确保 jar 任务包含所有编译输出
tasks.named<Jar>("jar") {
    archiveClassifier.set("")
    // 确保包含所有类文件
    from(sourceSets.main.get().output)
}


// 应用通用发布配置
apply(from = rootProject.file("gradle/maven-publish.gradle"))
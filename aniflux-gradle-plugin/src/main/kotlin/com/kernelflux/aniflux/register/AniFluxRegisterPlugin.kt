package com.kernelflux.aniflux.register

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * AniFlux Register Plugin
 * 
 * Implementation: ScopedArtifacts API + Custom Task
 * 
 * References:
 * - Booster framework implementation
 * - Router framework implementation
 * 
 * Core workflow:
 * 1. Register custom Task using ScopedArtifacts API
 * 2. Scan all classes (project + third-party libraries) in Task to collect registry classes
 * 3. Find AniFlux class and perform bytecode instrumentation using ASM
 * 4. Output processed JAR file
 */
class AniFluxRegisterPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        val androidComponents = project.extensions.findByType(AndroidComponentsExtension::class.java)
            ?: throw IllegalStateException("Android Gradle Plugin not found")
        
        // Register ScopedArtifacts API + Custom Task
        androidComponents.onVariants { variant ->
            val taskProvider = project.tasks.register(
                "transform${variant.name.replaceFirstChar { it.uppercaseChar() }}ClassesWithAniFlux",
                AniFluxRegisterTask::class.java
            )
            
            variant.artifacts.forScope(ScopedArtifacts.Scope.ALL)
                .use(taskProvider)
                .toTransform(
                    ScopedArtifact.CLASSES,
                    AniFluxRegisterTask::allJars,
                    AniFluxRegisterTask::allDirectories,
                    AniFluxRegisterTask::output
                )
        }
    }
}

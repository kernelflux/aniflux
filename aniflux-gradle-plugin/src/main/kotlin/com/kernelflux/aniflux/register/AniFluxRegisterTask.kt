package com.kernelflux.aniflux.register

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.*
import org.objectweb.asm.commons.AdviceAdapter
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

/**
 * AniFlux Register Task
 * 
 * Reference: Router framework implementation approach
 * 
 * Core functionality:
 * 1. Scan all classes (JARs and directories) to collect registry classes (XXXLoaderRegistry)
 * 2. Find AniFlux class and perform bytecode instrumentation using ASM
 * 3. Insert registration code into AniFlux.loadLoaderRegistries() method
 * 4. Output processed JAR file
 */
abstract class AniFluxRegisterTask : DefaultTask() {
    
    @get:InputFiles
    abstract val allJars: ListProperty<RegularFile>
    
    @get:InputFiles
    abstract val allDirectories: ListProperty<Directory>
    
    @get:OutputFile
    abstract val output: RegularFileProperty
    
    // Target class for instrumentation (Kotlin companion object compiles to inner class)
    private val ANIFLUX_COMPANION_CLASS = "com/kernelflux/aniflux/AniFlux\$Companion.class"
    
    // Blacklist: classes that don't need processing
    private val blackList = arrayOf(
        "androidx/",
        "android/",
        "kotlin/",
        "kotlinx/",
        "com/google/",
        "org/",
        "META-INF/"
    )
    
    @TaskAction
    fun taskAction() {
        val registryClasses = arrayListOf<String>()
        var waitInsertFile: File? = null
        var waitInsertJar: JarFile? = null
        var isFromJar = false
        
        // Collect all JAR files and directories for ClassWriter to load classes
        val allJarFiles = mutableListOf<JarFile>()
        val allDirectoriesList = mutableListOf<File>()
        
        val jarOutput = JarOutputStream(BufferedOutputStream(FileOutputStream(output.get().asFile)))
        
        // 1. Process directories
        allDirectories.get().onEach { directory ->
            allDirectoriesList.add(directory.asFile)
            val directoryUri = directory.asFile.toURI()
            directory.asFile
                .walk()
                .filter { it.isFile }
                .forEach { file ->
                    val filePath = directoryUri
                        .relativize(file.toURI())
                        .path
                        .replace(File.separatorChar, '/')
                    
                    // Check if it's AniFlux$Companion class
                    if (filePath == ANIFLUX_COMPANION_CLASS) {
                        waitInsertFile = file
                        isFromJar = false
                        return@forEach
                    }
                    
                    // Copy to output
                    jarOutput.putNextEntry(JarEntry(filePath))
                    file.inputStream().use { it.copyTo(jarOutput) }
                    jarOutput.closeEntry()
                    
                    // Check if it's a registry class
                    if (file.name.endsWith(".class")) {
                        val className = filePath.removeSuffix(".class")
                        if (isRegistryClass(className)) {
                            registryClasses.add(className)
                            println("AniFlux found registry class: $className")
                        }
                    }
                }
        }
        
        // 2. Process JAR files
        allJars.get().onEach { jarFile ->
            val jar = JarFile(jarFile.asFile)
            allJarFiles.add(jar)
            jar.entries().iterator().forEach { jarEntry ->
                try {
                    val entryName = jarEntry.name
                    
                    // Check if it's AniFlux$Companion class
                    if (entryName == ANIFLUX_COMPANION_CLASS) {
                        waitInsertJar = jar
                        waitInsertFile = jarFile.asFile
                        isFromJar = true
                        return@forEach
                    }
                    
                    // Copy to output
                    jarOutput.putNextEntry(JarEntry(entryName))
                    jar.getInputStream(jarEntry).use { it.copyTo(jarOutput) }
                    jarOutput.closeEntry()
                    
                    // Check if it's a registry class
                    val have = blackList.any { entryName.startsWith(it) }
                    if (!have && entryName.endsWith(".class")) {
                        val className = entryName.removeSuffix(".class")
                        if (isRegistryClass(className)) {
                            registryClasses.add(className)
                            println("AniFlux found registry class in JAR: $className")
                        }
                    }
                } catch (e: Exception) {
                    // Ignore errors and continue processing
                }
            }
            // Note: Do not close jar here, as JarClassWriter still needs access to them
        }
        
        // 3. Process AniFlux$Companion class and perform bytecode instrumentation
        val targetFile = waitInsertFile ?: throw RuntimeException("AniFlux\$Companion class not found, please check for references to aniflux-core")
        val targetJar = waitInsertJar
        
        val inputStream = if (isFromJar && targetJar != null) {
            targetJar.getInputStream(targetJar.getJarEntry(ANIFLUX_COMPANION_CLASS))
        } else {
            targetFile.inputStream()
        }
        
        // Create custom ClassWriter that can load classes from JAR files and directories
        // Note: Must complete all work before closing JAR files
        val writer = JarClassWriter(allJarFiles, allDirectoriesList)
        val insertVisitor = AniFluxInsertCodeVisitor(writer, registryClasses)
        
        // Read and process class file
        inputStream.use {
            ClassReader(it).accept(insertVisitor, ClassReader.SKIP_DEBUG)
        }
        
        // Complete ClassWriter work before closing JAR files
        // toByteArray() may trigger getCommonSuperClass calls that need access to JAR files
        val modifiedClassBytes = writer.toByteArray()
        
        // Write modified class file
        jarOutput.putNextEntry(JarEntry(ANIFLUX_COMPANION_CLASS))
        jarOutput.write(modifiedClassBytes)
        jarOutput.closeEntry()
        
        // Now it's safe to close all JAR files
        allJarFiles.forEach { it.close() }
        jarOutput.close()
        
        println("AniFlux Register Task completed. Found ${registryClasses.size} registry classes.")
    }
    
    /**
     * Check if it's a registry class
     */
    private fun isRegistryClass(className: String): Boolean {
        return className.startsWith("com/kernelflux/aniflux/generated/") &&
               className.endsWith("LoaderRegistry") &&
               !className.contains("$") // Exclude inner classes
    }
}

/**
 * Custom ClassWriter that loads classes from JAR files and directories instead of classpath
 * Solves the problem of classes not being found at runtime
 */
class JarClassWriter(
    private val jarFiles: List<JarFile>,
    private val directories: List<File>
) : ClassWriter(ClassWriter.COMPUTE_FRAMES) {
    
    override fun getCommonSuperClass(type1: String, type2: String): String {
        // If both types are the same, return directly
        if (type1 == type2) {
            return type1
        }
        
        // Try to load classes from JAR files and directories
        val class1 = loadClass(type1)
        val class2 = loadClass(type2)
        
        if (class1 == null || class2 == null) {
            // If classes cannot be loaded, return Object
            return "java/lang/Object"
        }
        
        // Check if class1 is a superclass of class2
        if (isAssignableFrom(class1, class2)) {
            return type1
        }
        
        // Check if class2 is a superclass of class1
        if (isAssignableFrom(class2, class1)) {
            return type2
        }
        
        // Check if it's an interface
        if (class1.access and Opcodes.ACC_INTERFACE != 0 || 
            class2.access and Opcodes.ACC_INTERFACE != 0) {
            return "java/lang/Object"
        }
        
        // Search upward for common superclass
        var current: ClassInfo? = class1
        val visited = mutableSetOf<String>() // Prevent circular references
        
        while (current != null && !visited.contains(current.name)) {
            visited.add(current.name)
            
            if (isAssignableFrom(current, class2)) {
                return current.name
            }
            
            // If superName is null or Object, stop searching
            val superName = current.superName
            if (superName == null || superName == "java/lang/Object") {
                break
            }
            
            current = loadClass(superName)
        }
        
        return "java/lang/Object"
    }
    
    /**
     * Load class from JAR file or directory
     */
    private fun loadClass(className: String): ClassInfo? {
        val classFileName = "$className.class"
        
        // 1. First search in directories
        for (directory in directories) {
            val classFile = File(directory, classFileName)
            if (classFile.exists()) {
                return readClassFile(classFile.inputStream())
            }
        }
        
        // 2. Search in JAR files
        for (jarFile in jarFiles) {
            val entry = jarFile.getJarEntry(classFileName)
            if (entry != null) {
                return readClassFile(jarFile.getInputStream(entry))
            }
        }
        
        return null
    }
    
    /**
     * Read class file and extract basic information
     */
    private fun readClassFile(inputStream: java.io.InputStream): ClassInfo? {
        return try {
            inputStream.use { stream ->
                val reader = ClassReader(stream)
                val info = ClassInfo()
                reader.accept(object : ClassVisitor(Opcodes.ASM9) {
                    override fun visit(
                        version: Int,
                        access: Int,
                        name: String?,
                        signature: String?,
                        superName: String?,
                        interfaces: Array<out String>?
                    ) {
                        info.name = name ?: ""
                        info.access = access
                        info.superName = superName
                        info.interfaces = interfaces?.toList() ?: emptyList()
                    }
                }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
                info
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Check if class1 can be assigned to class2 (whether class1 is a superclass of class2 or the same)
     */
    private fun isAssignableFrom(class1: ClassInfo, class2: ClassInfo): Boolean {
        if (class1.name == class2.name) {
            return true
        }
        
        var current: ClassInfo? = class2
        val visited = mutableSetOf<String>() // Prevent circular references
        
        while (current != null && !visited.contains(current.name)) {
            visited.add(current.name)
            
            if (current.name == class1.name) {
                return true
            }
            
            // Check interfaces
            for (interfaceName in current.interfaces) {
                val interfaceClass = loadClass(interfaceName)
                if (interfaceClass != null && isAssignableFrom(class1, interfaceClass)) {
                    return true
                }
            }
            
            // If superName is null or Object, stop searching
            val superName = current.superName
            if (superName == null || superName == "java/lang/Object") {
                break
            }
            
            current = loadClass(superName)
        }
        
        return false
    }
    
    /**
     * Class information data class
     */
    private class ClassInfo {
        var name: String = ""
        var access: Int = 0
        var superName: String? = null
        var interfaces: List<String> = emptyList()
    }
}

/**
 * ASM Visitor for inserting code into AniFlux class
 * Reference: Router framework's InsertCodeVisitor implementation
 */
class AniFluxInsertCodeVisitor(
    nextVisitor: ClassVisitor,
    private val registryClasses: List<String>
) : ClassVisitor(Opcodes.ASM9, nextVisitor) {
    
    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        
        // Only process loadLoaderRegistries method
        return if (name == "loadLoaderRegistries" && descriptor == "()V") {
            AniFluxMethodVisitor(Opcodes.ASM9, mv, access, name, descriptor, registryClasses)
        } else {
            mv
        }
    }
}

/**
 * ASM MethodVisitor for inserting code at the beginning of a method
 * Using AdviceAdapter allows safe insertion of code at method start and end
 */
class AniFluxMethodVisitor(
    api: Int,
    mv: MethodVisitor?,
    access: Int,
    name: String?,
    descriptor: String?,
    private val registryClassNames: List<String>
) : AdviceAdapter(api, mv, access, name, descriptor) {
    
    override fun onMethodEnter() {
        // Dynamically insert calls to all scanned registry classes at method start
        // Call each XXXLoaderRegistry.Companion.load() static method for registration
        registryClassNames.forEach { className ->
            insertRegistryClass(className)
        }
    }
    
    /**
     * Insert call to registry class, calling load() static method
     * 
     * Kotlin @JvmStatic generates static methods in the outer class, not in the Companion class
     * So we should call the outer class static method: GIFLoaderRegistry.load()
     * Instead of the Companion class instance method: GIFLoaderRegistry$Companion.load()
     */
    private fun insertRegistryClass(className: String) {
        // Directly call the outer class static method
        // @JvmStatic generates static method in outer class: public static void load()
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            className,
            "load",
            "()V",
            false
        )
    }
}

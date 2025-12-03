package com.kernelflux.aniflux.compiler.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*

/**
 * Loader注册处理器（KSP版本）
 *
 * 扫描所有标记了@AutoRegisterLoader的类，生成注册代码
 *
 * @author: kernelflux
 * @date: 2025/01/XX
 */
class LoaderRegistrationProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // 1. 查找所有标记了@AutoRegisterLoader的类
        val annotationName = "com.kernelflux.aniflux.annotation.AutoRegisterLoader"
        val symbols = try {
            resolver.getSymbolsWithAnnotation(annotationName)
                .filterIsInstance<KSClassDeclaration>()
                .toList()
        } catch (e: Exception) {
            logger.error("Failed to get symbols with annotation: ${e.message}")
            emptyList()
        }
        
        // 2. 收集Loader信息
        val loaders = symbols.mapNotNull { classDeclaration ->
            if (!classDeclaration.validate()) {
                return@mapNotNull null
            }

            val annotation = classDeclaration.annotations
                .find { it.shortName.asString() == "AutoRegisterLoader" }
                ?: return@mapNotNull null

            val animationType = annotation.arguments
                .find { it.name?.asString() == "animationType" }
                ?.value as? String
                ?: return@mapNotNull null

            LoaderInfo(
                className = classDeclaration.qualifiedName!!.asString(),
                simpleName = classDeclaration.simpleName.asString(),
                animationType = animationType
            )
        }
        
        // 3. 生成注册代码（如果没有找到任何Loader，直接返回）
        if (loaders.isNotEmpty()) {
            generateRegistrationCode(loaders)
        }
        return emptyList()
    }

    /**
     * 生成注册代码
     */
    private fun generateRegistrationCode(loaders: List<LoaderInfo>) {
        // 根据第一个 Loader 的格式类型生成唯一的类名，避免多个模块生成相同类名
        val formatName = loaders.firstOrNull()?.animationType?.uppercase() ?: "Default"
        val registryClassName = "${formatName}LoaderRegistry"
        val fileName = registryClassName
        
        val fileSpec = FileSpec.builder(
            packageName = "com.kernelflux.aniflux.generated",
            fileName = fileName
        ).apply {
            addFileComment("Auto-generated code. Do not edit manually.")

            // 导入必要的类
            addImport("com.kernelflux.aniflux.registry", "LoaderRegistry")
            addImport("com.kernelflux.aniflux.util", "AnimationTypeDetector")

            // 为每个Loader添加导入
            loaders.forEach { loader ->
                val packageName = loader.className.substringBeforeLast(".")
                val className = loader.simpleName
                addImport(packageName, className)
            }

            // 创建注册类
            // 参考大厂实践（ARouter、WMRouter等），使用 load() 方法名
            // 字节码插桩会调用此方法进行自动注册
            val companionObject = TypeSpec.companionObjectBuilder()
                .addFunction(
                    FunSpec.builder("load")
                        .addKdoc("加载并注册标记了@AutoRegisterLoader的Loader\n\n此方法由编译时字节码插桩自动调用，通常不需要手动调用。")
                        .addModifiers(KModifier.PUBLIC)
                        .addAnnotation(
                            AnnotationSpec.builder(ClassName("kotlin.jvm", "JvmStatic"))
                                .build()
                        )
                        .addCode(buildRegistrationCode(loaders))
                        .build()
                )
                .build()

            addType(
                TypeSpec.classBuilder(registryClassName)
                    .addKdoc("自动生成的Loader注册类\n\n此文件由KSP自动生成，请勿手动编辑。")
                    .addModifiers(KModifier.PUBLIC)
                    .addType(companionObject)
                    .build()
            )
        }.build()

        // 4. 写入文件
        val dependencies = Dependencies(false)
        val file = codeGenerator.createNewFile(
            dependencies = dependencies,
            packageName = "com.kernelflux.aniflux.generated",
            fileName = fileName
        )

        file.use { outputStream ->
            outputStream.bufferedWriter().use { writer ->
                fileSpec.writeTo(writer)
            }
        }
    }

    /**
     * 构建注册代码
     */
    private fun buildRegistrationCode(loaders: List<LoaderInfo>): CodeBlock {
        val builder = CodeBlock.builder()

        loaders.forEach { loader ->
            val animationTypeEnum =
                ClassName("com.kernelflux.aniflux.util", "AnimationTypeDetector", "AnimationType")
            val loaderClass = ClassName.bestGuess(loader.className)

            builder.addStatement(
                "%T.register(%T.%L, %T())",
                ClassName("com.kernelflux.aniflux.registry", "LoaderRegistry"),
                animationTypeEnum,
                loader.animationType.uppercase(),
                loaderClass
            )
        }

        return builder.build()
    }


    /**
     * Loader信息数据类
     */
    private data class LoaderInfo(
        val className: String,
        val simpleName: String,
        val animationType: String
    )
}

/**
 * KSP处理器提供者
 */
class LoaderRegistrationProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return LoaderRegistrationProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger
        )
    }
}


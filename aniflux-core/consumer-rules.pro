# 保持 aniflux-core 库所有类不被混淆和删除
-keep class com.kernelflux.aniflux.** { *; }
-dontwarn com.kernelflux.aniflux.**

# 保持注解
-keepattributes *Annotation*

# 保持行号信息用于调试
-keepattributes SourceFile,LineNumberTable

# 保持泛型签名
-keepattributes Signature

# 保持异常信息
-keepattributes Exceptions
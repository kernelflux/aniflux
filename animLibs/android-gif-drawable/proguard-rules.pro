# 保持 GIF 库所有类不被混淆和删除
-keep class com.kernelflux.gif.** { *; }
-dontwarn com.kernelflux.gif.**

# 保持 native 方法
-keepclasseswithmembernames class * {
    native <methods>;
}


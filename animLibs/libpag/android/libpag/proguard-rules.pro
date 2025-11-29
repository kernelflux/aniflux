# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/dom/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

-keep class **.R$* {
    *;
}

# ============================================
# PAG 库自混淆配置（包名：com.kernelflux.pag）
# 用户无需在应用层添加混淆规则
# ============================================

# 保持 PAG 库所有类不被混淆
-keep class com.kernelflux.pag.** {*;}

# 保持 androidx.exifinterface 不被混淆（PAG 依赖）
-keep class androidx.exifinterface.** {*;}

# 保持包名不被混淆
-keeppackagenames com.kernelflux.pag.**

# 保持所有 native 方法
-keepclasseswithmembers class ** {
    native <methods>;
}

# 保持 PAG 类的公共方法和字段
-keepclasseswithmembers public class com.kernelflux.pag.** {
    public <methods>;
}

-keepclasseswithmembers public class com.kernelflux.pag.** {
    public <fields>;
}

# 保持 nativeContext 字段（JNI 使用）
-keepclasseswithmembers class com.kernelflux.pag.** {
    long nativeContext;
}

# 保持 nativeSurface 字段（JNI 使用）
-keepclasseswithmembers class com.kernelflux.pag.** {
    long nativeSurface;
}

# 保持 PAGFile 的私有构造函数（JNI 使用）
-keepclasseswithmembers class com.kernelflux.pag.PAGFile {
   private <init>(long);
}

# 保持 PAGFont 的静态方法（JNI 使用）
-keepclasseswithmembers class com.kernelflux.pag.PAGFont {
    private static void RegisterFallbackFonts();
}

# 保持 PAGDiskCache 的静态方法（JNI 使用）
-keepclasseswithmembers class com.kernelflux.pag.PAGDiskCache {
    private static java.lang.String GetCacheDir();
}

# 保持 VideoSurface 的所有方法
-keepclasseswithmembers class com.kernelflux.pag.VideoSurface {
    <methods>;
}

# 保持 DisplayLink 的所有方法
-keepclasseswithmembers class com.kernelflux.pag.DisplayLink {
    <methods>;
}

# 保持 PAGAnimator 的回调方法（JNI 使用）
-keepclasseswithmembers class com.kernelflux.pag.PAGAnimator {
    private void onAnimationStart();
    private void onAnimationEnd();
    private void onAnimationCancel();
    private void onAnimationRepeat();
    private void onAnimationUpdate();
}

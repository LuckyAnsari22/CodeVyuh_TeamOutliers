# GreenIQ ProGuard Rules
-keep class com.greeniq.app.network.models.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn retrofit2.**

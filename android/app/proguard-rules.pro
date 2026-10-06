# Ravalt Proguard Rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn org.bouncycastle.**
-keep class org.bouncycastle.** { *; }

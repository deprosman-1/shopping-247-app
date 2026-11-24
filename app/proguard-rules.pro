# 24/7 Shopping R8 Hardening, Code Obfuscation & Reflection Keep Rules
-repackageclasses 'o'
-allowaccessmodification
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleTypeAnnotations,*Annotation*

# Preserve essential Android & Jetpack Compose / Room / Serialization entrypoints
-keep class androidx.annotation.Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <methods>;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <fields>;
}

# Preserve Domain & Room Models
-keep class com.example.data.model.** { *; }
-keep class com.example.data.local.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public ** *Dao();
}

# Moshi & Kotlin Metadata / Serialization
-keep class kotlin.Metadata { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
    @com.squareup.moshi.JsonClass *;
}
-keepclassmembers class **$Companion {
    public <fields>;
}

# OkHttp / Retrofit
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}

# Strip verbose debug logs in release builds to prevent data leakage & reverse-engineering hints
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

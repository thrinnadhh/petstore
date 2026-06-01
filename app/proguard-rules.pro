# PawsNearMe ProGuard Rules
# These rules ensure R8 minification doesn't break library-specific reflection patterns.
# Keep these rules up to date when adding new dependencies.

# ============================================================
# Room Database — preserve entity class names for DAO reflection
# ============================================================
-keep class com.example.data.*Entity { *; }
-keep class com.example.data.*Dao { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# ============================================================
# Kotlin Serialization — required by Supabase / Ktor
# ============================================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** { *; }

# ============================================================
# Supabase SDK
# ============================================================
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# ============================================================
# Ktor client
# ============================================================
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ============================================================
# Firebase
# ============================================================
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ============================================================
# Moshi (JSON serialization)
# ============================================================
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class ** { @com.squareup.moshi.FromJson *; @com.squareup.moshi.ToJson *; }

# ============================================================
# PostHog Analytics
# ============================================================
-keep class com.posthog.** { *; }
-dontwarn com.posthog.**

# ============================================================
# OkHttp / Retrofit
# ============================================================
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class retrofit2.** { *; }

# ============================================================
# Coil image loading
# ============================================================
-keep class coil.** { *; }
-dontwarn coil.**

# ============================================================
# Security: Preserve crash reporting stack trace info
# ============================================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================================
# Security: Strip all debug/verbose log calls from release builds
# ============================================================
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

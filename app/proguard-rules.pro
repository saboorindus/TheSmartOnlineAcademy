# ─── Android default rules ────────────────────────────────────────────────────
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep crash stack traces readable
-renamesourcefileattribute SourceFile

# ─── Kotlin ───────────────────────────────────────────────────────────────────
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { *; }
-keepclassmembers class kotlin.Lazy { *; }
-dontwarn kotlin.**

# Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Kotlin serialization (if added later)
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations

# ─── Firebase Auth ────────────────────────────────────────────────────────────
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-dontwarn com.google.firebase.auth.**

# ─── Firebase Firestore ───────────────────────────────────────────────────────
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# CRITICAL: Keep all data model classes used with toObject() / set()
# Firestore uses reflection to map documents to data classes.
# If these are obfuscated, toObject() will silently return null.
-keep class com.echologics.thesmartonlineacademy.data.model.** { *; }
-keepclassmembers class com.echologics.thesmartonlineacademy.data.model.** {
    public <init>();
    public <fields>;
    public <methods>;
}

# ─── Google Play Services ─────────────────────────────────────────────────────
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class com.google.android.gms.internal.** { *; }

# ─── Agora RTC SDK ────────────────────────────────────────────────────────────
-keep class io.agora.** { *; }
-dontwarn io.agora.**
-keep class io.agora.rtc2.** { *; }
-keep class io.agora.rtc2.video.** { *; }
-keep class io.agora.rtc2.internal.** { *; }
-keep class io.agora.base.** { *; }

# Agora native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# ─── Jetpack Compose ──────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Compose compiler generated classes
-keep class **ComposableSingletons** { *; }
-keep class **_PreviewProvider { *; }

# ─── Jetpack Navigation ───────────────────────────────────────────────────────
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# ─── ViewModel / Lifecycle ────────────────────────────────────────────────────
-keep class androidx.lifecycle.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**

# ─── Activity / Fragment ──────────────────────────────────────────────────────
-keep class * extends androidx.activity.ComponentActivity { *; }
-keep class * extends androidx.fragment.app.Fragment { *; }

# ─── Android core ─────────────────────────────────────────────────────────────
-keep class androidx.core.** { *; }
-dontwarn androidx.core.**
-keep class androidx.appcompat.** { *; }
-dontwarn androidx.appcompat.**

# ─── Notifications ────────────────────────────────────────────────────────────
-keep class androidx.core.app.NotificationCompat** { *; }
-keep class androidx.core.app.NotificationManagerCompat { *; }

# ─── App-specific ViewModels ──────────────────────────────────────────────────
# Keep all ViewModels so they can be instantiated by name
-keep class com.echologics.thesmartonlineacademy.ui.**.** extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class com.echologics.thesmartonlineacademy.ui.**.** extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# ─── App navigation and sealed classes ────────────────────────────────────────
-keep class com.echologics.thesmartonlineacademy.navigation.** { *; }
-keep class com.echologics.thesmartonlineacademy.navigation.Screen { *; }
-keep class com.echologics.thesmartonlineacademy.navigation.Screen$* { *; }
-keep class com.echologics.thesmartonlineacademy.navigation.NavArgs { *; }

# ─── Enums ────────────────────────────────────────────────────────────────────
# Enums used in Firestore string comparisons must keep their names
-keepclassmembers enum com.echologics.thesmartonlineacademy.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public java.lang.String name();
    public int ordinal();
}

# ─── Repositories ─────────────────────────────────────────────────────────────
-keep class com.echologics.thesmartonlineacademy.data.repository.** { *; }

# ─── Notifications helper ─────────────────────────────────────────────────────
-keep class com.echologics.thesmartonlineacademy.notifications.** { *; }

# ─── OkHttp / Retrofit (if added later) ──────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ─── Gson (used internally by Firebase) ──────────────────────────────────────
-keep class com.google.gson.** { *; }

# Keep only Gson interfaces (safe & required)
-keep interface com.google.gson.TypeAdapterFactory
-keep interface com.google.gson.JsonSerializer
-keep interface com.google.gson.JsonDeserializer

# Keep only SerializedName fields (LIMITED scope)
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}


-keep class io.agora.** { *; }
-dontwarn io.agora.**


# ─── Suppress common warnings ─────────────────────────────────────────────────
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn com.google.errorprone.**
-dontwarn sun.misc.Unsafe
-dontwarn java.lang.invoke.**
-dontwarn com.google.protobuf.**


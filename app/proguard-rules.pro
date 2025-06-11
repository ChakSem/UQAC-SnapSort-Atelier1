# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Hilt classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class **_HiltComponents { *; }
-keep class **_HiltModules { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep Compose classes
-keep class androidx.compose.** { *; }
-keep class androidx.compose.ui.** { *; }

# Keep data classes used with kotlinx.serialization
-keepclassmembers class com.example.snapsort.data.model.** {
    *;
}

# Keep domain models
-keepclassmembers class com.example.snapsort.domain.model.** {
    *;
}

# Camera and ML Kit
-keep class com.google.mlkit.** { *; }
-keep class androidx.camera.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Coil
-keep class coil.** { *; }
-keep interface coil.** { *; }

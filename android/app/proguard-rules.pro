# Lô tô Android — ProGuard / R8 rules
# Default mode (not fullMode) keeps reflection-heavy Kotlin/AndroidX intact.

# kotlinx.serialization — keep companion @Serializer references R8 can't infer
-keep,includedescriptorclasses class com.miti99.loto.**$$serializer { *; }
-keepclassmembers class com.miti99.loto.** {
    *** Companion;
}
-keepclasseswithmembers class com.miti99.loto.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Kotlin metadata for runtime reflection
-keep class kotlin.Metadata { *; }

# Media3 / ExoPlayer internals — prevent stripping of codec/extractor classes
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# DataStore
-keep class androidx.datastore.** { *; }

# Compose tooling only exists in debug; suppress release warnings
-dontwarn androidx.compose.ui.tooling.**

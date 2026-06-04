# Keep entities and models for Room and Gson
-keep class com.example.finance.data.entity.** { *; }
-keep class com.example.finance.data.repository.LocalBackupDocument { *; }
-keep class com.example.finance.data.repository.AppSettingsSnapshot { *; }
-keep class com.example.finance.domain.model.** { *; }
-keep class com.example.finance.core.common.** { *; }

# Keep generic signatures for Gson
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Keep coroutines for withTransaction safety
-keep class kotlinx.coroutines.** { *; }

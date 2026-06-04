# Proguard rules for personal-finance-tracker app

# Note: core:data provides its own rules via consumerProguardFiles

# Keep coroutines and kotlin intrinsics
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.HandlerContext {
    val handler;
}

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Additional app-specific rules can be added below

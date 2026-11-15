# Preserve file names and line numbers for BugLogManager crash reports
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature,InnerClasses,EnclosingMethod

# Moshi KSP codegen adapters & JSON annotations
-keep,allowobfuscation,allowshrinking @com.squareup.moshi.JsonClass class * {
    <init>(...);
}
-if @com.squareup.moshi.JsonClass class *
-keep,allowobfuscation class <1>JsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}

# Retrofit API interfaces
-keep interface com.example.data.api.WallhavenApiService { *; }

# Room
-keep class * extends androidx.room.RoomDatabase

# Strip debug/verbose/info Log calls in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# OkHttp / Retrofit optional warnings
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

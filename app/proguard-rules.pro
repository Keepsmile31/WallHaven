# Preserve file names and line numbers for BugLogManager crash reports
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature,InnerClasses,EnclosingMethod

# Moshi KSP codegen adapters & JSON annotations
-keep,allowobfuscation,allowshrinking @com.squareup.moshi.JsonClass class * {
    <init>(...);
}
-keep class *JsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}

# Retrofit API interfaces
-keep interface com.example.data.api.WallhavenApiService { *; }

# Room
-keep class * extends androidx.room.RoomDatabase

# OkHttp / Retrofit optional warnings
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# Preserve file names and line numbers for BugLogManager crash reports
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature,InnerClasses,EnclosingMethod

# Moshi KSP codegen adapters & JSON annotations
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class *JsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}

# Retrofit API interfaces & data models
-keep interface com.example.data.api.WallhavenApiService { *; }
-keep class com.example.data.model.** { *; }
-keep class com.example.data.backup.** { *; }
-keep class com.example.data.db.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase

# OkHttp / Retrofit optional warnings
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

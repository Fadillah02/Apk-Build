# Keep data classes for JSON
-keep class com.portscanpro.model.** { *; }
-keep class com.portscanpro.data.database.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

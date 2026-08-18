# Keep Live2D Cubism SDK (if integrated) - model classes
-keep class com.live2d.sdk.** { *; }
-keep class com.furry.live2dlauncher.live2d.** { *; }

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

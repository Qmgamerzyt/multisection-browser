# GeckoView
-keep class org.mozilla.geckoview.** { *; }
-dontwarn org.mozilla.geckoview.**

# Kotlin
-keep class kotlin.** { *; }

# Room
-keep class androidx.room.** { *; }

# Compose
-keep class androidx.compose.** { *; }

# Keep parcelable
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
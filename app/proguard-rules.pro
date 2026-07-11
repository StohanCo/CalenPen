# Default ProGuard rules for Android
-keepattributes *Annotation*
-keepclassmembers class ** {
    @org.jetbrains.annotations.NotNull *;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# CalenPen data classes (Gson serialisation)
-keep class com.calenpen.ui.editor.DrawingCanvas$SerialisedStroke { *; }
-keep class com.calenpen.utils.RecognitionStroke { *; }
-keep class com.calenpen.utils.StrokePoint { *; }

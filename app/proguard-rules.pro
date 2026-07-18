# youtubedl-android relies on reflection / bundled python — keep it intact
-keep class com.yausername.** { *; }
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

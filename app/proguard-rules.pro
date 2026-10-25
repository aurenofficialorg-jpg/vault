# Strip all logging from release builds so nothing sensitive can reach logcat.
-assumenosideeffects class android.util.Log { *; }

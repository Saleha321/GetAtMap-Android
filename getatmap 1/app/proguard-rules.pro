# ProGuard / R8 Configuration for GetAtMap

# Preserve WebView JavaScript Interfaces
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Explicitly keep Location JavaScript Interface and its public methods
-keep class com.example.webview.LocationJavaScriptInterface {
    public *;
}

# Keep LocationData data class fields
-keep class com.example.data.LocationData {
    public *;
}

# Preserve WebChromeClient file chooser methods
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void openFileChooser(...);
    public boolean onShowFileChooser(...);
}

# Google Play Services Location SDK
-keep class com.google.android.gms.location.** { *; }

# Keep line numbers for debugging release crash reports in Play Console
-keepattributes SourceFile,LineNumberTable

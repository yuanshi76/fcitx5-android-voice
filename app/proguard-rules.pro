# disable obfuscation
-dontobfuscate

# preserve the line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# remove kotlin null checks
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

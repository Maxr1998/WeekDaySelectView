-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose

-keep, allowobfuscation class *
-keepnames class *
-keepclassmembers class * {
    public <init>(...);
    public <methods>;
    void onDaySelected(...);
}
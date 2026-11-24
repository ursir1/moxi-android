# Server/native integration rules
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep the JNI entry point classes
-keep class com.stremio.core.** { *; }
-keep, includedescriptorclasses class org.rustls.platformverifier.** { *; }

# Keep protobuf runtime classes used by pbandk
-keep class pbandk.** { *; }
-dontwarn pbandk.**

# Keep Android components registered in Manifest (Moxi)
-keep class com.moxi.app.** { *; }
-keep class com.moxi.app.MainApplication { *; }
-keep class com.moxi.app.MainActivity { *; }
-keep class com.moxi.app.server.** { *; }
-keep class com.moxi.app.server.ServerService { *; }
-keep class com.moxi.app.server.BootReceiver { *; }
-keep class com.moxi.app.server.JniStreamingServerController { *; }

# Also keep old package just in case JNI looks for it
-keep class com.stremio.mobile.** { *; }

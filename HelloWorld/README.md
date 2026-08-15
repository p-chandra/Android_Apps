# Hello World Android App

A minimal native Android app that displays **Hello World!**. The shared JDK and
Android SDK are stored one level above this project and excluded from Git.

## Build

```bash
cd HelloWorld
./build.sh
```

The debug APK is created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Install on a connected Android device

Enable USB debugging on the device, connect it, and run:

```bash
./.android-sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

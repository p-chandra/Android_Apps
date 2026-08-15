# Android Auto Template

A minimal Java template for a projected Android Auto app using the Android for
Cars App Library. It includes:

- A phone launcher activity confirming installation
- A discoverable `CarAppService`
- A basic car `Session`, `Screen`, and `PaneTemplate`
- A development-only host validator
- Build and device-install scripts

The starter declares the `POI` category because Android Auto requires every
templated app to have a supported category. Change the category and implement
the corresponding behavior when adapting this project for navigation, IoT,
weather, media, messaging, or calling.

## Build and install

From this directory:

```bash
./build.sh
./install.sh
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Start a new app from this template

1. Copy the entire `AndroidAutoTemplate/` directory to a new sibling directory.
2. Change `rootProject.name` in `settings.gradle.kts`.
3. Change `namespace` and `applicationId` in `app/build.gradle.kts`.
4. Rename the Java package directory and its `package` declarations.
5. Update the app name, car category, and starter screen for your use case.

## Test with the Desktop Head Unit

1. Enable Android developer options and USB debugging on the phone.
2. Enable Android Auto developer mode by tapping its version information ten
   times in Android Auto settings.
3. Install the app with `./install.sh`.
4. In Android Auto developer settings, start the head unit server.
5. Connect the phone over USB and run:

   ```bash
   ../.android-sdk/platform-tools/adb forward tcp:5277 tcp:5277
   ../.android-sdk/extras/google/auto/desktop-head-unit
   ```

## Local-development limitation

This template is intended for the Desktop Head Unit and development testing.
Current Android Auto policy generally requires apps built with the Cars App
Library to be installed from a trusted source before they can run in a real
vehicle. The Android Auto “unknown sources” option does not exempt Cars App
Library apps. You do not need to publish publicly to build or use the DHU.

`HostValidator.ALLOW_ALL_HOSTS_VALIDATOR` is deliberately enabled for local
development. Replace it with a restrictive validator if the app is ever
distributed.

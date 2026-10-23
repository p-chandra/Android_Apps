# Pi Photos for Android

Native Android app for uploading and browsing photos on your Raspberry Pi over Tailscale. Separate package `com.pawan.piphotos`; existing Android projects/apps are unaffected. Requires Android 13 or newer.

1. Connect Tailscale on the Pi and phone.
2. Open **Pi Photos → Settings**. Server: `http://100.117.156.54:8090`. Enter the access key from `/home/p-c/pi-photos/.env` on the Pi, then **Save & connect**.
3. Tap **Upload photos**, select one or more images with Android's file picker, and confirm the selection. Allow notifications to see upload progress and Cancel.
4. Browse thumbnails, tap a photo to view it, and pinch to zoom. Tap **Refresh** after uploads, and **Load more** for older photos.

Original files stay on the phone and are copied at full quality to the Pi's primary USB drive. Duplicate files are skipped. Uploads run in a foreground service so navigating away from the app does not normally interrupt them. Cancelling, force-stopping the app, rebooting, or Android's long-running-service timeout can interrupt uploads; select remaining photos again to retry. Successful uploads are retained. There is no automatic background camera-roll sync or persistent retry queue.

The app uses Android's file picker without requesting broad storage access. The access key stays in app-private preferences; app backup and preference transfer are disabled. HTTP is supported for Tailscale addresses because Tailscale encrypts the network connection. Use HTTPS when configuring a server outside Tailscale. Image cache is in memory; full-photo viewing uses temporary app cache files that are deleted after decoding. Displayed full-photo previews are sampled to fit phone memory; originals on the Pi remain unchanged.

Build with the existing local JDK/Android SDK:

```sh
./build.sh :app:lintDebug --offline
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Server, USB configuration, existing-photo import, and backup instructions: [Pi Photos server](../../RaspPi_Projects/Pi_Photos/README.md).

The device integration runner in `app/src/androidTest` uses a generated sample and a disposable server on port 8091. It tests the actual foreground upload service, duplicate retries, gallery listing, exact original bytes and thumbnail decoding. It restores the configured server address afterward and never reads personal phone photos. Build it with `./build.sh :app:assembleDebugAndroidTest --offline` and run `adb shell am instrument -w -e testUrl http://100.117.156.54:8091 com.pawan.piphotos.test/com.pawan.piphotos.UploadSmokeTest` only against an empty test backend.

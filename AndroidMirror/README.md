# AndroidMirror

Experimental parked-only Android Auto phone-screen mirror, version 0.6/code 6. Renamed from ParkedPhotos/Phone Mirror. The Gradle project and app display name are AndroidMirror. Package and component names remain `com.pawan.parkedphotos` so installing this as an update preserves app data and existing Android Auto integration. Native Android MediaProjection renders the phone's built-in display into the car viewer's SurfaceView; no image files, network server, audio capture, touch injection, accessibility service, root, or head-unit software changes.

Open AndroidMirror in Android Auto while parked. On the phone, open AndroidMirror → Share phone screen → approve Android's screen-sharing dialog. Navigate on the phone; the car is a view-only display. Sharing consent is per session and expires after60seconds without an attached car viewer. Closing/pausing the projected viewer, AndroidAuto restriction, screen lock, system stop or notification Stop ends capture. Reopen and approve again to restart. Protected content may be blank; keep the phone unlocked. Notifications and private phone UI can appear in a full-screen share.

Separate phone setup and projected-viewer activities avoid showing a preview inside its own captured phone screen. Capture uses a single consent token and virtual display per session; resize reuses that display. Maximum capture dimension1280pixels, aspect fit. Foreground mediaProjection service displays a persistent notification. No INTERNET, storage, microphone, camera or input-injection permissions. The experimental game category retains the existing parked-launch path; this isn't a Play-approved game.

CarLinkRecorder1.2 companion lifecycle diagnostics remain signature protected; video_was_playing staysfalse because there is no video-file player. AndroidAuto still controls restrictions. No direct gear state is read.

The car viewer has a right-hand zoom sidebar suited to a 1920×720 display. Tap **+** or **−** to zoom in 25% steps from 100% (fit) to 400%. Drag the mirrored image to pan when enlarged; **Fit** resets zoom and position. The image keeps its aspect ratio and is clipped inside the viewing area. Zoom only changes the car-side view; it does not send gestures to the phone or change capture resolution. Zoom and pan survive activity configuration changes. There is no Stop sharing button in the car viewer; the existing phone and notification controls remain available.

Validation for 0.6: debug build and lint passed. `ZoomLayoutTest` passed on the Pixel 8 using a simulated 1920×720 secondary display, checking sidebar placement, zoom/fit controls, limits, pan bounds and SurfaceView clipping. The test creates its own display and never requests phone-screen capture. Physical-car rendering and live MediaProjection zoom still need an in-car check.

Build with JDK17/SDK35:

```sh
JAVA_HOME=/home/pawan/workspace/Android_Apps/.jdk ./gradlew :app:assembleDebug :app:lintDebug --offline
```

Pre-mirror photo-viewer0.3 APK and source rollback are preserved at `Kia/evidence/parked-photos-test/phone-mirror/`. Earlier selected media preferences and grants remain in app-private data but are not used by this build. Do not uninstall if you want to preserve rollback data.

Platform requirements: https://developer.android.com/media/grow/media-projection . Physical Kia mirroring is unverified until tested; refer to the consolidated Kia README for recorded validation.

The device layout test uses the standalone instrumentation runner (no extra test libraries):

```sh
JAVA_HOME=/home/pawan/workspace/Android_Apps/.jdk ./gradlew :app:assembleDebugAndroidTest --offline
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.pawan.parkedphotos.test/com.pawan.parkedphotos.ZoomLayoutTest
```

Run it while screen sharing is stopped. Its trusted virtual display uses instrumentation-only shell permissions; the app itself receives no additional permissions.

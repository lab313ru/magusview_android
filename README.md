# MagusView — preview and photo drawing (0.30)

## Version 0.30

**Draw** below the preview selects Line, Circle or Rectangle and one of seven colors.
Drag over the image, then enter an optional nearby text label. Undo removes the last
shape; Clear requires confirmation. Done drawing restores tap/hold focus gestures.
Drawing works while recording. Shapes appear on the preview (including PiP) and saved
full-resolution JPEGs, but are not added to recorded videos. PiP displays drawings but
editing requires the full app.

Coordinates are normalized to the displayed image, excluding letterboxing; stroke/text
size scales with the image. Photo requests snapshot the drawing list before asynchronous
capture, and render it on the independent full-resolution bitmap before JPEG encoding.
Drawings live with the camera service, not as permanently stored projects. They mark
image locations and do not track moving objects. Build and lint pass; phone validation
of gestures, label placement, photo alignment and concurrent recording is still needed.

## Version 0.29

The user's locked-screen log shows continued app execution but SDK EOF/frame loss about
48 seconds into recording. Repeated EOF messages exhausted the old log cap before the
final stop reason. EOF logging is now limited to once per second, and logs retain recent
entries by trimming older lines. Added screen/idle/battery-saver/exemption/wake-lock/
low-power-standby, blocked-network, and service lifecycle diagnostics.

More → Background recording offers a user-initiated Android battery-optimization
exemption request for continuous Wi-Fi recording. No setting is changed automatically.
This is not a confirmed fix; the camera/network and vendor power policies remain possible
causes. Build/lint pass. Test several minutes locked, then copy diagnostics if interrupted.

Versions 0.22–0.28 moved controls below preview, renamed Captures, added pause-aware
notification/PiP timers, removed duplicate full-screen timers and PiP badge background,
and changed startup status to Discovering cameras.

## Version 0.21

The expanded recording notification now offers Take photo, Pause/Continue, and Stop and
save, using the same service commands as PiP. Actions unavailable during an in-flight
command are omitted, and notification text distinguishes recording, paused, preparing,
and saving. Notification updates are keyed to state changes rather than preview frames.
The notification still exists only for recording/finalization; it disappears after saving.
Build and lint pass; notification interactions need device verification.

## Version 0.20

Tap the full-size preview to select an autofocus zone, then trigger one-shot focus.
The CHD50 reports a 7 × 7 focus grid (AFZONE maximum packs columns/rows as 458759).
Letterbox/pillarbox taps are ignored and right/bottom edges clamp to the final cell.
JNI validates the linear zone against the grid dimensions, not the packed maximum.
Press and hold the image for a bottom manual-focus panel with a slider and single-step
minus/plus buttons. Adjustments select manual mode; opening the panel changes nothing.
The old Autofocus once menu/button was removed. More → Manual focus also opens the
panel. PiP taps remain controlled by Android; expand the window to use focus gestures.
Host tests cover coordinate mapping, edge cells, invalid coordinates and native zone
bounds. Build and lint pass. Actual focus accuracy still needs phone testing.

## Version 0.19

**More → Autofocus once** requests AF mode 2. **More → Camera controls** loads camera
capabilities/current values and exposes supported exposure, gain, white balance, image
adjustments, focus mode and manual focus position. Unsupported controls are omitted.
Values are range-checked in both UI and JNI; native writes are restricted to image/focus
parameters, with calibration and white-balance ROI commands excluded. Manual exposure/
gain, WB gains, and focus position first select the corresponding manual mode.

Restore image defaults requires confirmation and restores only supported image values,
not focus/calibration, network, or codec settings. No settings are changed merely by
opening the panel. All SDK operations remain serialized on the camera executor.

Read-only CHD50 probe: autofocus mode supported (0–3), focus position 0–854, exposure
0–1,000,000, gain 0–35, automatic exposure and white balance supported. Temperature/tint
are marked unsupported. Calibration (mode 3) is not offered. Build/lint and native
bounds/capability/calibration rejection tests pass; phone setting writes need validation.

Version 0.18 replaced the Saved list with **Folder**, offering Photos and Videos folders
in the installed file manager, with a path explanation if direct folder opening fails.

## Version 0.17

PiP uses opaque white vector icons instead of legacy Android drawables. Idle preview
shows Photo and Start recording (circle); active recording shows Photo, Pause/Continue,
and Stop recording and save (square). No idle diskette or disabled pause action remains.
Start uses a foreground-service PendingIntent so recording can begin from the floating
window. Actions are disabled only while unavailable or processing a command. Android
controls the surrounding PiP menu styling; device visual verification is still needed.

## Version 0.16

The main screen now uses a compact header, a prominent recording/paused/saving indicator
with mm:ss timer, and one row for photo, record/stop and pause/continue. Connection actions
and Copy diagnostics moved to **More**. **Saved** lists this app's published photos and
videos, newest first, and opens them in an installed viewer. It queries only owned,
non-pending captures in MagusView folders without requesting broad media permissions.
PiP still hides the full UI and exposes its existing capture actions. Build and lint pass;
layout, saved-capture viewing, and long-duration recording need phone validation.

## Version 0.15

The 0.14 phone log confirms startup discovery and three saved recordings, followed by
a failed approximately 30-second clip with 67,108,912 bytes. This does not establish
whether finalization is delayed or the MP4 is invalid. Validation now retries for up to
10 seconds on the media worker while foreground protection remains active, logging file
size, duration, width, retry count, and retained filename. Multi-segment recordings are
validated before joining too. Invalid files remain private and are not published as
successful videos. The new behavior needs phone validation; it is not a confirmed fix
for the underlying SDK issue.

## Version 0.14

Attaching the UI to a fresh camera service automatically starts discovery on the current
Wi-Fi and shows the existing camera selection dialog. Reattaching to a running session
does not rescan or interrupt recording. Search cancellation, failure, and explicit
disconnect do not loop into another search; **Find camera** remains available to retry.

## Version 0.13

The complete 0.12 phone log shows a successful first recording and a second recording
with no playable frames, although a decoded preview frame arrived 3 ms before starting.
The Windows NAM SDK probe reproduced a related repeat failure: Record returned
0x8000FFFF on the second start using the same handle. Reopening the handle between
clips yielded three H.264 3840 × 2160 recordings, each with 122 frames and approximately
3.5–4 seconds duration, verified with ffprobe.

Before a subsequent recording segment (new video or Continue), the app now closes and
reopens the selected camera, starts the stream, and waits for a decoded frame before
starting recording. Preview can briefly freeze during this refresh. Camera-side settings
are not changed. Pending commands remain disabled during refresh, and a generation
change cancels the wait. Android validation is still needed.

Probe: `tools/test-stream-windows.ps1 -Bgr -RecordFile app/build/repeated.mp4 -Repeat 3`
and add `-ReopenBetween` to test the workaround.

## Version 0.12

User diagnostics show successful pause/resume and gallery publication, followed by a
second recording stopped 43 ms after starting due to SDK EOF (0x81), producing an empty
file. Its cause is not established. EOF now logs frame age and allows the existing
frame-loss timeout to confirm stream failure, rather than closing immediately. A frame
received after EOF is logged explicitly. Hard SDK errors (0x80) still stop immediately.
Recording starts log frame age, and initial pending-frame polling logs are rate-limited
to once per second. Device validation is needed to distinguish a real stream ending
from an EOF notification while frames are still available.

## Version 0.11

- Tap the floating PiP window to reveal **Take photo**, **Pause/Continue recording**,
  and **Stop and save**. Actions update their enabled state during capture/save commands.
- Pause/Continue is also available in the full app. Preview and photos continue while
  recording is paused, and the recording timer excludes paused time.
- The SDK only supports stream pause, so recording pause finalizes a private segment.
  Continue starts another segment; Stop joins compatible video tracks without re-encoding
  using MediaExtractor/MediaMuxer, removes paused time, then publishes one MP4.
  Leading non-keyframes are skipped on resumed segments. Very brief clips without a
  playable keyframe may be empty. Failed source files are retained rather than deleted.
- Host regression tests (`tools/test-video-segments.py`) run the actual join algorithm
  against fake Android media I/O: pause gaps, leading keyframes, duration, incompatible
  formats, invalid timestamps and source retention. This does not exercise Android's
  actual muxer. Build/lint pass; phone validation of pause/resume and PiP actions is pending.
- Phone check: record 5 seconds, pause 5 seconds, take a photo, continue 5 seconds,
  stop/save; expect one approximately 10-second clip and a full-resolution JPEG.

## Version 0.10

- Removed the 10 fps preview limit, with 2 ms frame polling. The preview follows camera
  output when the UI is ready; actual display rate depends on device performance.
- Live preview enters a floating picture-in-picture window when pressing Home on
  supported devices. Closing that window stops preview-only sessions; active recording
  continues until stopped in the app or notification. Full app controls return on expand.
- JNI converts directly from the held BGR frame to a preview bounded by 1280 × 720,
  avoiding full 4K RGBA conversion, bitmap allocation, and upload for each preview frame.
  The native scaling regression checks padded rows, channel order and buffer bounds.
- **Take photo** also works during recording. It requests the next full-resolution
  decoded frame and saves a JPEG to Pictures/MagusView independently of the preview.
  MP4 recording continues through the SDK; its resolution is unchanged.
- Device validation: compare live movement with 0.9, take several photos while recording,
  and check JPEG dimensions plus uninterrupted MP4 playback and duration.

## Version 0.9

- Camera and recording ownership moved from the Activity into a bound service. Starting
  recording promotes it to a `connectedDevice` foreground service, with a notification
  containing a **Stop and save** action. Notification permission is requested on first use.
- Home, switching apps, and rotation keep an active recording running. Reopening the app
  attaches to the existing session. Preview-only sessions still close when leaving the app.
- Hidden previews continue draining decoded SDK frames without allocating display bitmaps.
  A partial wake lock covers recording and gallery publication, including screen-off use.
- Stopping, disconnecting, or losing the stream finalizes the MP4 before closing the SDK.
  Foreground protection remains until gallery publication finishes. Force-stop or process
  termination cannot guarantee MP4 finalization; the service does not automatically restart.
- Build and lint pass (no lint errors). Background recording on OxygenOS still requires
  device validation: record, press Home, use another app for a minute, return, then stop.
  Also test the notification Stop action, rotation, screen off, and Wi-Fi loss; check saved
  clip duration/playback and that the ongoing notification disappears after saving.

Android service type reference: https://developer.android.com/develop/background-work/services/fgs/service-types

Target device: OnePlus 12, Android 16 / OxygenOS 16.0.10.

## Version 0.8

- **Take photo** saves the currently displayed full-resolution video frame as a quality-95
  JPEG in `Pictures/MagusView`. This is a video-frame snapshot, not a separate sensor-still
  capture request.
- **Record video / Stop recording** records the received camera stream through the NAM SDK
  to MP4, then validates and publishes it in `Movies/MagusView`. No microphone is used.
- In 0.8, recording was finalized on disconnect, stream error, app backgrounding, or
  rotation. Version 0.9 adds background recording as described above.
- MediaStore publishes completed items only; no broad photo-library/storage permission is
  needed. Failed gallery saves retain the app-private temporary video rather than deleting it.
- Decoded frames are continuously consumed while visual presentation is limited to about
  10 fps. This prevents recording from stalling behind a full decoded-frame queue. Native
  pixel conversion is optimized even in the debug APK.

The user confirmed that 0.7 previews correctly and that the washed-out image was caused
by camera color settings. Restoring defaults in the other software fixed it. MagusView
preserves camera exposure/color settings; it only chooses the local BGR decode format.

Validation: Android build/lint and native conversion tests pass. SDK recording was tested
against the actual CHD50 from Windows with the phone app closed: a roughly four-second
3840 × 2160 H.264 MP4 was validated with ffprobe. Android gallery saving and recording still
need on-device testing. Abrupt process death cannot guarantee finalization of an MP4.

## Version 0.7

The user's 0.6 tombstone identifies SIGSEGV/SEGV_ACCERR inside ARM64 `libtoupnam.so`
at relative PC 0x4e74ec, before any successful frame acquisition. Inspection of that exact
binary shows a four-channel NEON store in color conversion. This points to the SDK's
RGBA conversion path; the precise upstream allocation/stride defect is not confirmed.

Version 0.7 keeps the SDK's default BGR24 output and expands to RGBA in the app bridge,
checking source/destination dimensions and lengths and handling four-byte row padding.
This avoids requesting the SDK's four-channel conversion. A local Windows test against
the CHD50 confirmed a complete 3840 × 2160 BGR frame (24,883,200 bytes). Regression tests
cover BGR channel order and padded rows. Android runtime confirmation is still needed.
Crash reporting from 0.6 remains available if the workaround is insufficient.

## Collect the Android crash

Version 0.5 crashes after camera selection on the user's phone, despite the successful
Windows preview test. Version 0.6 preserves the same native stream path and adds diagnostics;
it does not claim to fix the crash.

Install over the current app, reproduce camera selection once, then reopen MagusView
without reconnecting and tap **Copy diagnostics**. The report includes the previous and
current session breadcrumbs plus Android's recent process-exit reasons. When Android
retains a native tombstone, it includes the signal, abort/cause messages, crashing thread,
library-relative program counters and build IDs. Java uncaught exceptions are also saved.
This may also recover a retained crash record from 0.5 immediately after installation.

Diagnostics stay in app-private storage and are copied only when requested. The native
tombstone summary omits memory dumps and other threads. Android may not retain a trace;
the report explicitly says when one is unavailable. No USB debugging is required.
Reference: https://developer.android.com/reference/android/app/ApplicationExitInfo#getTraceInputStream()

`tools/TestTombstoneSummary.java` tests signal/backtrace extraction, crashing-thread
selection, memory omission, unknown fields, and malformed/truncated input. Build, lint,
and parser regression tests pass; real-device crash-report collection needs confirmation.

## Automatic discovery

Tap **Find camera** while the phone and CHD50 are on the same reachable LAN. Select the
camera from the resulting list. No IP entry or Wi-Fi settings button is required.
The app uses the existing Wi-Fi connection, including networks without internet access.

Version 0.3 replaces the original Maguscam/GigE bridge with the dedicated ToupNam Wi-Fi
SDK. The installed desktop MAGUSView contains `magusnam.dll` exporting `Toupnam_Init`,
`Toupnam_Enum`, and other NAM APIs. The official ToupTek download page distinguishes
ToupCam (USB/GigE) from ToupNam (Wi-Fi/multi-interface cameras):
https://www.touptekphotonics.com/download/?category=SDK

The previous SDK's `GigeEnableV2(..., "wifi")` enables Wi-Fi adapter support for GigE
and was not evidence that it discovers this HDMI/Wi-Fi camera family. Versions 0.1 and
0.2 initialized that SDK but found no CHD50 on the user's phone. Version 0.3 uses
`Toupnam_PriFlag(BROADCAST, ...)` before `Toupnam_Init`, then `Toupnam_Enum` to list
ready cameras. The user confirmed discovery of MagusCHD50_2D1953 on the OnePlus 12.
Version 0.3 then rejected an initial 0 × 0 size before starting the stream. Version 0.4
starts streaming first and takes dimensions from the first decoded frame instead.
Version 0.4 started the stream but repeatedly returned 0x8000FFFF. A local Windows SDK
comparison against the actual CHD50 reproduced that failure twice with forced
RTP-over-RTSP, while default transport delivered 3840 × 2160 frames twice. Version 0.5
clears the forced-transport flag. Those decoded frames report length=0; the bridge now
uses the known RGBA format and validated dimensions when length is unspecified, while
still rejecting a nonzero length shorter than the required buffer. A complete 33,177,600
byte RGBA copy was verified locally. Android preview still needs phone confirmation.

## Current features and limits

- Automatic Wi-Fi broadcast discovery, bounded to 20 seconds.
- Camera names and automatically discovered addresses in the selection list.
- NAM live preview with default BGR24 output, app-side RGBA conversion, and default transport.
- Connection cleanup on backgrounding, manual reconnection, and error diagnostics.
- Copyable diagnostic text with SDK version, Wi-Fi interface, addresses, and routes.

Photo/video capture are available in 0.8. Visual preview is presented at up to 10
frames/second; recording uses the received stream rather than that UI presentation rate.
It preserves the camera's current stream resolution, with a 64 MiB frame
buffer ceiling. The app does not modify shared camera resolution or Wi-Fi settings.

## Test on the phone

1. Install `app/build/outputs/apk/debug/app-debug.apk` over the previous version.
2. Keep the phone on your current Wi-Fi; ensure the camera is online on the same LAN.
3. Open MagusView and tap **Find camera**. Choose the camera when it appears.
4. Check a moving image, colors/orientation, and increasing received-frame count.
5. Test disconnect/reconnect, background/foreground, rotation, and Wi-Fi loss.
6. Take a photo, record for several seconds, stop, and check both files in the phone gallery.
7. If discovery or preview fails, send the **Copy diagnostics** text. Include whether
   desktop MAGUSView can discover the camera at the same time.

Close another viewer if it holds an exclusive camera connection. No IP address is
required. If changing Wi-Fi networks after SDK initialization, restart the app: the
vendor documents one initialization per process and interface switching is not verified.

## Build

Open this folder in Android Studio, configure `sdk.dir` in `local.properties` or set
`ANDROID_HOME`, then run:

```powershell
./gradlew.bat :app:assembleDebug :app:lintDebug
```

Verified local toolchain: Gradle 9.6.0, AGP 9.5.1, compile SDK 37, target SDK 36, minimum
SDK 30, NDK 23.1.7779620, Android Studio JBR 25, Java source compatibility 17. ARM64 only.
The native bridge statically links the C++ runtime and requests 16 KB ELF alignment.
The downloaded Wi-Fi library has 64 KB LOAD alignment. Device testing is still needed
for runtime behavior on 16 KB systems.

The native build uses `vendor/toupnamsdk/inc/toupnam.h` and
`vendor/toupnamsdk/android/arm64/libtoupnam.so`. See `vendor/SOURCE.md` for provenance.
The original USB SDK sample has not been changed.

## Implementation and validation

All handle operations run on one process-wide executor, including across Activity
recreation. SDK callbacks only store error events. Generation tokens reject stale work.
The pointer-based image API allows validating frame dimensions and buffer length before
allocating/copying into Java memory. Each acquired frame is held while its size is read
and its pixels copied, then returned in a Java finally block. Resolution changes update
the preview allocation. Only one frame can await UI presentation. The first-frame timeout
is twenty seconds; subsequent frame loss times out after ten seconds.

Build and Android lint pass. The host regression test in `tools/test-preview-startup.cpp`
compiles the actual JNI bridge against a fake SDK and verifies default transport,
delayed first-frame arrival, zero/unspecified frame length, buffer bounds, pixel copy,
resolution changes, malformed frames, and exactly-once cleanup.
It passes with the local MinGW g++ and JDK JNI headers. APK alignment is checked separately. No physical Android
camera streaming test has been performed by the agent. The Windows discovery comparison
(`tools/discover-windows.ps1`) subsequently confirmed this CHD50 is found only by the
Wi-Fi SDK. `tools/test-stream-windows.ps1` opens a brief preview on the discovered camera
and verifies a decoded-frame copy without changing camera-side settings. Add `-Tcp` to
reproduce the forced-transport failure. The user also confirmed official ToupView Android
shows video on this phone and network.

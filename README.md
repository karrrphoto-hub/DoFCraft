# DoFCraft

**Depth of Field & Hyperfocal Calculator for Photographers**

Android · Kotlin · Photography

[Русский](README.ru.md)

DoFCraft is an Android photography tool for calculating depth of field and hyperfocal distance, with flexible sharpness criteria and support for digital, film and custom camera formats. It works entirely offline.

Choose your camera, focal length, aperture and focus distance to see the near and far limits of depth of field, total depth of field and a diagram. Hyperfocal mode finds the focus distance that extends the far limit to infinity.

## Features

- Depth of field and hyperfocal distance, with explicit optical infinity.
- Built-in camera formats and custom profiles with physical frame dimensions.
- Six sharpness criteria for everyday viewing, pixels, video, print, screens and a custom Circle of Confusion (CoC).
- Prime and zoom lens collections in custom camera profiles. Select a lens in Focal Length for its quick focal and aperture values.
- Per-lens distance scales in meters or feet, including a second hyperfocal reading in lens units when they differ from app units.
- Ignore Lens Profile to return to standard quick values and app units. Manual input remains available.
- An adaptive focus-distance slider, manual entry and a depth-of-field diagram.
- Independent, nonblocking diffraction and close-focus information.
- English and Russian, light and dark themes, local profiles and preferences.

## Supported camera formats

The catalogue includes Nikon 1, Four Thirds, APS-C, APS-H, full frame / 35 mm, digital medium format, medium-format film (645 through 6×17 cm) and large-format film (4×5, 5×7 and 8×10 inches). Custom profiles accept physical frame dimensions in millimeters.

Format labels describe the nominal format; calculations use its physical frame dimensions.

## Sharpness criteria

Choose an acceptable amount of blur for how you intend to view the image.

| Criterion | Intended use |
| --- | --- |
| Standard | Traditional photographic viewing, using the format's catalogue CoC or a frame-diagonal fallback. |
| Pixel-Based | Enlarged digital viewing based on sensor resolution. |
| Video-Based | Viewing based on recording resolution and active recording area. |
| Print-Based | A chosen print size and viewing conditions. |
| Screen-Based | A chosen display and viewing conditions. |
| Custom | A manually specified Circle of Confusion (CoC). |

Standard preserves explicit catalogue recommendations. Otherwise, it scales the traditional 36×24 mm / 0.030 mm reference by the physical frame diagonal. This is a photographic convention, not a universal physical sharpness limit.

Switching to a different camera selects Standard; editing the current camera preserves the selected criterion.

## Diffraction awareness

Informational messages describe the possible loss of fine detail as the aperture is stopped down, relative to the selected CoC. They leave all shooting parameters and results under your control. A greater depth of field can still be useful despite some loss of fine detail.

These are planning estimates, not measurements of a real lens's image quality. Close-focus information explains where simplified optical assumptions may become less accurate. There is no separate macro calculation mode in the current app.

## Technology

Kotlin, Jetpack Compose, Material 3, AndroidX ViewModel and local DataStore persistence. One Android app module; optical calculations are pure Kotlin and covered by JVM tests.

Android 8.0 (API 26) or newer. No internet permission, advertising, analytics, accounts or telemetry.

## Building

Open this existing project in Android Studio, use its configured Gradle JDK and install the SDK specified in the Gradle files. Sync, then run the **app** configuration on a device or emulator.

~~~bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
~~~

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

For production signing, follow [RELEASE_SIGNING.md](docs/RELEASE_SIGNING.md). Reuse the existing signing identity; never upload signing material or local configuration.

Development rules: [AGENTS.md](AGENTS.md).

## Project status and distribution

This is the private source repository for the existing application, now branded **DoFCraft**. Its previous user-facing name was DoF Master.

Download [DoFCraft 1.0.3](https://github.com/karrrphoto-hub/DoF-Master/releases/tag/v1.0.3) from the separate public distribution repository. Its historical repository name and older APKs are retained. Existing production-signed installations can update in place. No Google Play listing is claimed.

The rebrand preserves the Android application ID, namespace, packages, signing identity and stored user data. The experimental macro archive remains unchanged in the private `archive/macro-prototype` branch.

## License

No LICENSE file is included. This private repository is not presented as an open-source project.

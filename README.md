<div align="center">
  <img src="docs/images/app-icon.svg" width="112" height="112" alt="DoFCraft app icon">

  <h1>DoFCraft</h1>

  <p><strong>Depth of field and hyperfocal distance, wherever you shoot.</strong></p>
  <p>For photography and video. No internet required.</p>

  <p><strong>Android 8.0+</strong> &nbsp;·&nbsp; <strong>Fully offline</strong> &nbsp;·&nbsp; <strong>English / Русский</strong></p>

  <p>English &nbsp;|&nbsp; <a href="README.ru.md">Русский</a></p>

  <p><a href="https://github.com/karrrphoto-hub/DoF-Master/releases/download/v1.0.4/DoFCraft-1.0.4.apk"><strong>Download APK · DoFCraft 1.0.4</strong></a></p>
  <p>Android 8.0+ &nbsp;·&nbsp; Signed Release APK &nbsp;·&nbsp; No sign-in required</p>
  <p><a href="https://github.com/karrrphoto-hub/DoF-Master/releases/tag/v1.0.4">Release notes and SHA-256 checksum</a></p>
</div>

---

DoFCraft helps you estimate how much of a scene will appear sharp. Choose your camera, focal length, aperture and focus distance to see the near and far depth of field limits, total depth of field and a visual diagram.

For landscapes, switch to hyperfocal mode to find the focus distance that extends the far depth of field limit to infinity.

<table>
  <tr>
    <th>Depth of field</th>
    <th>Hyperfocal distance</th>
    <th>Sharpness criterion</th>
  </tr>
  <tr>
    <td><a href="docs/images/dof-en.svg"><img src="docs/images/dof-en.svg" width="240" alt="Depth of field calculator with camera, exposure inputs and results"></a></td>
    <td><a href="docs/images/hyperfocal-en.svg"><img src="docs/images/hyperfocal-en.svg" width="240" alt="Hyperfocal distance and near and far depth of field limits"></a></td>
    <td><a href="docs/images/sharpness-en.svg"><img src="docs/images/sharpness-en.svg" width="240" alt="Six sharpness criteria with an explanation of the selected criterion"></a></td>
  </tr>
</table>

*Actual app screenshots. Both English and Russian interfaces are available.*

## Features

- **Depth of field and hyperfocal distance.** Near and far limits, total depth of field and a diagram of the scene.
- **Flexible distance input.** An adaptive slider, manual entry and an explicit optical infinity setting.
- **Your cameras and lenses.** Custom camera profiles with physical frame dimensions and kits of prime and zoom lenses.
- **Quick values for your lens.** Select a lens in Focal Length to use its focal length and aperture presets and distance-scale units. Manual input is always available.
- **Information without restrictions.** Independent diffraction and close focus messages explain calculation limits without changing your shooting parameters.
- **Make it yours.** English and Russian, light, dark and system themes, measurement units and number formatting options.

## Sharpness for your output

Visible blur depends on how an image is viewed. DoFCraft offers six criteria:

| Criterion | Purpose |
| --- | --- |
| **Standard** | Normal photo viewing. Uses the format's catalogue value, or a calculation based on the physical frame diagonal when no value is defined. |
| **Pixel-Based** | Assessing an enlarged digital image using sensor resolution. |
| **Video-Based** | Depth of field based on recorded video resolution. |
| **Print-Based** | A chosen print size and viewing conditions. |
| **Screen-Based** | Viewing an image on a chosen display. |
| **Custom** | Setting the Circle of Confusion (CoC) manually. |

When Standard has no catalogue value, it uses the traditional photographic reference of a 36×24 mm frame with a 0.030 mm Circle of Confusion, scaled by the frame diagonal. Explicit built-in catalogue values take priority.

Calculations help plan a shot; they do not guarantee identical detail from every real lens. At close distances, simplified optical assumptions can differ more noticeably from a lens's actual behaviour. Informational messages explain these limits while keeping all results available.

## Cameras and quick values

The camera picker separates **User Camera Profiles** from **Default Camera Profiles**, with your profiles first. Empty sections are hidden, including when searching. Lens lists are sorted from shorter to longer focal lengths, using the minimum focal length for zooms, regardless of their distance-scale units.

Camera rows show up to three lenses with bullets and a count of the remaining lenses. Distance-scale units appear only when they differ from the app's unit family; the current focus distance is kept out of the camera picker.

Create a custom camera profile and add your lenses. In **Focal Length**, choose the lens you are using. Its focal range, aperture presets and distance-scale units become available. Lens selection is remembered for each camera. Selecting a lens brings out-of-range focal length and aperture inputs to its nearest supported limits; manual input remains available afterward.

Zooms contribute standard focal lengths within their range and their exact endpoints. A 58 mm prime or a 48–217 mm zoom keeps its own values. Set each lens's distance scale to meters or feet in Camera Profile.

**Ignore Lens Profile** is shared by the focal length and aperture dialogs. It restores the standard quick values and app distance units together, while keeping your selected lens and physical focus distance.

When lens and app distance units differ, hyperfocal distance appears in app units first, with lens-scale units in parentheses. An explanation helps you transfer that distance to the scale on your lens. Switching to a different camera resets the sharpness criterion to Standard; editing the current camera preserves it.

## Offline by design

The app runs locally, with no advertising, analytics, accounts or telemetry. It does not request internet permission. Profiles and preferences are stored on the device.

## Installation

1. [Download the signed Release APK](https://github.com/karrrphoto-hub/DoF-Master/releases/download/v1.0.4/DoFCraft-1.0.4.apk) on your Android device.
2. Open the downloaded file and, if prompted, allow your browser or file manager to install this app.
3. Complete installation. Android 8.0 or newer is required.

The [release page](https://github.com/karrrphoto-hub/DoF-Master/releases/tag/v1.0.4) includes a SHA-256 checksum for verifying the download.

If you previously installed a private Debug build, this Release uses a different signing certificate and cannot update it in place. Keep the old installation if you need its saved profiles; uninstalling it can erase app data.

## About this repository

This repository contains the app description, icon, screenshots and downloadable installation files under Releases. The app's source code is not included here.

---

<div align="center">
  <strong>DoFCraft</strong><br>
  Plan your depth of field. Keep your creative choices.
</div>

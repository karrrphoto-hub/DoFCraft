# DOF Calculator development guide

## Product scope

DOF Calculator is a native, fully offline Android application for calculating depth of field (DOF/ГРИП) and hyperfocal distance.

Keep these project constraints unless the user explicitly changes them:

- Kotlin.
- Jetpack Compose with Material 3.
- Gradle Kotlin DSL and the existing version catalog.
- Minimum Android version: API 26.
- A single app module and straightforward MVVM architecture.
- No network features or `INTERNET` permission.
- No advertising, analytics, payments, account systems, or telemetry.
- Do not change the existing `applicationId` or `namespace` without an explicit migration request.

## Architecture

Prefer the smallest design that keeps calculation logic independent from Android:

- Compose renders immutable UI state and sends user actions to a screen-level ViewModel.
- ViewModels validate user input, call calculation code, and expose state. Do not put formulas in Composables or Activities.
- Calculation code must be deterministic, side-effect free, and usable from local JVM tests.
- Persist only genuine user preferences. Keep local persistence behind a small boundary, but do not create repository interfaces until they improve testing or allow a real alternative implementation.
- `MainActivity` remains a thin application host.

Use unidirectional data flow for screens:

`user action -> ViewModel -> calculation/settings dependency -> immutable UI state -> Compose`

Avoid additional modules, use-case layers, dependency injection frameworks, and navigation abstractions until the app actually needs them.

## Package layout

Place future source files under `com.example.dofcalculator`:

- `calculation` — pure Kotlin DOF and hyperfocal formulas, validation helpers specific to calculations, and result computation. No Android or Compose dependencies.
- `model` — immutable domain inputs/results, focal length/aperture/distance types, unit definitions, and shared validation results.
- `ui` — Activities, Compose screens/components, screen UI state/actions, ViewModels, and the existing `ui.theme` package. Feature subpackages may be added when a screen grows.
- `data` — local-only data sources, built-in camera/sensor presets if required, and concrete persistence implementations. Never add remote data sources.
- `settings` — user preference models and settings behavior such as units, default circle of confusion, and last-used values. Keep settings UI under `ui` unless a dedicated settings feature becomes large enough to justify co-location.

Create packages when the first real class belongs in them; do not add placeholder files solely to make empty directories visible to Git.

## Camera profiles

- Camera profiles are independent of focal length, aperture, and focusing distance.
- CameraLensKit stores user Prime/Zoom attachments and independent Show All flags by stable camera ID in separate versioned DataStore records through PresetRepository. Built-in geometry stays read-only; both built-in and user cameras accept lens attachments. Do not reuse obsolete lens archives or add an active lens. Lens metadata generates input shortcuts only: exact primes, each zoom's separate range plus endpoints, and the aperture envelope of the entire kit with exact user f-numbers. Preserve existing focal standards and the chosen full/half/third aperture row; without supplied closure limits use that row's existing upper bound. Show All unions standards with generated values. Manual input and all optical/macro state remain independent. Deleting a custom camera removes its current kit, not legacy archives.
- Use the exact recommendations in `BuiltInCameraProfiles` / `FrameFormat`; built-in IDs are permanent and built-in profiles are read-only.
- Profiles contain camera identity, format and physical frame dimensions only. Custom formats require a name and finite positive dimensions when created or edited; migrated unknown dimensions remain unknown until supplied.
- Keep sharpness settings independent of cameras and DOF/Hyperfocal mode. StandardCoCResolver validates actual frame geometry, then uses the exact linked catalogue recommendation or, when absent, 0.030 × hypot(width, height) / hypot(36, 24). A present but invalid catalogue CoC is an error. Its typed CATALOG / DIAGONAL_FALLBACK result feeds the same effective CoC as the other criteria; the sheet shows a neutral fallback explanation only for DIAGONAL_FALLBACK. Never infer catalogue membership from dimensions or names, or persist the derived fallback value in a camera profile. Standard, Pixel-Based, Video-Based, Print-Based, Screen-Based and Custom resolve through the pure Kotlin SharpnessCriterionCalculator.
- VideoCoCCalculator owns aspect-matched central recording geometry in profile axes, additional linear crop, manual-area validation and output-pixel CoC. Defaults: 4K UHD 3840×2160, 3 px, Auto, 1×. Manual dimensions replace crop; permit at most 0.1% relative aspect mismatch for rounded measurements. Never crop focal length or infer camera recording resolution. Persist video parameters independently in named sharpness v2 fields, preserving existing mode names and enum ordinals.
- OutputSharpnessCalculator owns physical output geometry, orientation, Fit/Fill, exact angular blur and screen pixel pitch. Print uses viewing distance and angular tolerance; print PPI/DPI are optional preparation metadata and must never alter CoC. Screen Visual and Display Pixels are explicit alternative criteria, never automatically combined.
- New installs use Print 30×45 cm / 50 cm / 2 arcminutes / Fit and Screen Monitor 27-inch 4K / 60 cm / 1 arcminute / Visual / Fit. Viewing angles are photographic assumptions, not medical thresholds.
- Sharpness settings v2 persist separately from camera profiles. Retain the raw v1 record as an archive; never interpret its pixel blur tolerance as a viewing angle. Show the print-model migration notice once, until acknowledged.
- The ViewModel derives one effectiveCoCMm for both calculator modes, visualization and slider. CoC formatting never feeds back into calculations. Preserve actual focal length, aperture and focus distance when criteria change.
- Sharpness parameters persist separately in app settings. Standard is available for every format with valid physical dimensions, whether catalogued, custom, imported or legacy. Keep stored sharpness modes and Custom values when adding fallback support.
- Preserve camera IDs and old literal CoCs during migration. Camera v3 records store geometry; v1/v2 payloads and obsolete lens records remain archived in DataStore. Preserve the chosen criterion and the existing Custom CoC during migration. Archive any pre-criterion calculator CoC verbatim before overwriting its old preference; old calculator and camera CoCs can be imported into Custom only by an explicit action in the sharpness sheet.

## Domain rules

- The focus-distance slider always starts at 0.1 metres, independent of lens, camera, display units, and saved legacy preferences. Manual input retains its value even below this UI bound.

- Make units explicit at boundaries and convert to one documented internal unit before calculations.
- Use `Double` for optical calculations and format/round only for presentation.
- Diffraction is a one-way informational branch. MagnificationEstimator uses sensor-plane distance and the stable thin-lens branch 0–1:1; DiffractionCalculator uses 550 nm and pupil ratio P = 1. Unknown magnification stays explicitly unknown. Manual scale is saved independently of cameras as a confirmed pair with sensor-plane distance and nominal focal length. ManualMagnificationValidator compares canonical numbers with a relative tolerance of 1e-9. Distance/focal changes latch confirmation off; stale pairs fall back to explicitly labelled Auto. Infinity suspends the pair at m = 0; Hyperfocal uses its actual H and never reactivates a stale pair automatically. Never feed effective aperture or diffraction warnings back into DOF, CoC, hyperfocal or the slider.
- MacroWarningEvaluator classifies the existing resolved MagnificationEstimator output at the exact Double threshold m >= 0.2 (1:5). Ordinary Auto DOF shows one neutral card below Distance; D < 4f gets a typed unavailable state without an invented ratio. Infinity, invalid input, Hyperfocal and active manual Macro hide the new prompt. Preserve existing manual pair validation and diffraction behavior; while the new card is visible it replaces the duplicate automatic scale badge. Keep the transient memo key and result together in immutable UI state so settings emissions cannot invalidate a newer input snapshot. Aperture/CoC changes reuse that result. The independent macro_photography_warning preference defaults on and never changes optical or manual inputs. Macro settings currently refine magnification and diffraction only; DOF remains approximate.
- Keep input validation separate from numeric formulas.
- Represent special results such as an infinite far limit explicitly; do not leak sentinel numbers into the UI.
- Store optical constants and defaults in domain/settings code, never inside Composables.
- Add focused unit tests for formulas, unit conversion, boundary values, invalid input, and infinity behavior before relying on UI tests.

## UI rules

- Use Material 3 components and the existing theme.
- The main distance card has separate click targets: its distance summary opens the distance dialog; Hyperfocal and Macro buttons switch modes directly on the main screen. In Hyperfocal, the main distance summary shows “Hyperfocal mode” instead of repeating the numeric distance; the numeric H remains in the results card. The distance dialog contains only manual entry, the nonlinear slider and −/+ controls, with a Close action. Mode buttons are mutually exclusive and green when enabled in light and dark themes. Entering Hyperfocal sets magnification mode to Auto and persists that change; disabling Hyperfocal returns to ordinary DOF, never automatically re-enabling manual Macro. Retain saved manual values for explicit reuse via the macro editor. Normalize older saved Hyperfocal + Manual combinations to Auto on load. Keep them beside the summary where width permits; stack below it for narrow screens or large fonts without reducing touch targets. Macro entered from the main screen returns to the main screen after Apply or Cancel. There is no top-level DOF/Hyperfocal switch or H/Infinity shortcut button.
- Macro always opens the manual scale-and-distance sheet, including when entered from Settings or a macro indicator. Do not show an Auto/Manual selector or an Auto action inside that sheet. Auto estimation is the behavior when manual Macro is off; disabling Macro remains outside its editor. The unapplied form holds A:B and measured distance in metres. Each opening starts with an empty distance field; never prefill it from the calculator or a saved pair, including during optical/settings updates. The user must enter a measured distance or explicitly restore the saved pair. Apply commits one canonical calculator distance plus the bound manual pair; cancel discards edits. Distance remains editable in Manual; Hyperfocal alone locks its controls. Focal edits never recompute the measured distance. Macro uses the same focal-length input, step buttons and presets as the ordinary focal dialog, bound directly to the canonical ViewModel value; these edits apply immediately even when the scale/distance form is later cancelled. In DOF with manual Macro enabled (including a pair awaiting confirmation), the main focal tile opens the macro sheet. Auto and Hyperfocal use the ordinary focal dialog. Mark the main focal tile with “Macro mode” while manual Macro is enabled. Preserve saved pairs across DOF/Hyperfocal; show stale-pair notices and macro indicators only in DOF, never in Hyperfocal. Automatic macro estimation from ordinary distance remains continuous.
- ThinLensEquivalentCalculator computes informational f = D × m / (1+m)^2 using reciprocal scaling to avoid overflow. It never replaces nominal focal length in DOF or the adaptive slider. Ratios below 0.25 or above 1.5 require a separate confirmation, not a hard rejection; D < 4 nominal focal lengths is not forbidden for Manual. Diffraction uses actual confirmed magnification, P = 1, independently of equivalent focal length.
- Persist manual scale, bound distance, bound nominal focal length and confirmation together in diffraction settings. Retain old scale-driven distance and previous standalone scale as read-only migration archives. Unbound legacy magnifications require explicit confirmation.
- Persist aperture increments independently of optical inputs. Full stop is the default; half/third stop change the preset row, selection and step buttons without quantizing or replacing the current manual aperture.
- Keep strings in Android resources and provide accessible labels/content descriptions where needed.
- AppLocale explicitly provides LocalResources so dialogs, sheets and popups use the selected app language even when their platform context uses another locale. Format UI numbers with the locale from those resources; keep LocalConfiguration for window and font metrics.
- Preserve state across configuration changes through ViewModel state and, only where useful, saved-state or local settings.
- Do not build the calculator UI or mathematical core until the corresponding feature is requested.

## Dependencies and build

- Prefer AndroidX/Kotlin APIs already present in the project.
- Add a dependency only for a concrete requirement; declare versions and aliases in `gradle/libs.versions.toml`.
- Do not add networking, analytics, ads, billing, cloud services, or a DI framework.
- Keep `minSdk = 26`. Change compile/target SDK or toolchain versions only as a deliberate, verified migration.
- Before handing off code, run the narrowest relevant tests and `./gradlew :app:assembleDebug`.
- Prefer an emulator for instrumented tests. Android Studio may clear app data on test devices: before using a physical phone, back up its app data and restore it after the run. Never run instrumented tests on a personal phone without that backup.
- Never commit `local.properties`, build outputs, Gradle caches, IDE workspace state, credentials, signing material, or generated secrets.

## Change discipline

- Read existing files before editing and make the smallest coherent change.
- Preserve the current package/application identity.
- Keep public names and code understandable; prefer explicit code over clever abstraction.
- Update this guide when an architectural decision or product constraint changes.

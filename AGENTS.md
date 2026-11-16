# DoFCraft development guide

## Product scope

DoFCraft is a native, fully offline Android application for calculating depth of field (DOF/ГРИП) and hyperfocal distance.

Keep these project constraints unless the user explicitly changes them:

- Kotlin.
- Jetpack Compose with Material 3.
- Gradle Kotlin DSL and the existing version catalog.
- Minimum Android version: API 26.
- A single app module and straightforward MVVM architecture.
- No network features or `INTERNET` permission.
- No advertising, analytics, payments, account systems, or telemetry.
- Do not change the existing `applicationId` or `namespace` without an explicit migration request.
- DoFCraft is the user-facing brand. The private source repository is `karrrphoto-hub/DoFCraft`; keep the separate public `DoF-Master` distribution repository unchanged unless requested. Preserve existing package/theme names, DataStore identifiers and the permanent Release signing identity when rebranding.

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

- Explicitly selecting a camera activates its remembered lens and selects that lens's exact minimum focal length, unless Ignore Lens Profile is enabled. Ignored profiles do not set a focal length. Cameras without lenses keep the current focal length. Preserve aperture, saved distance/Infinity and all numeric sharpness parameters. Switching to a different camera ID selects Standard and persists that choice; reselecting the same ID, editing the current profile/lenses and restoring the app retain the selected criterion. Initial app restoration, lens edits and other preference emissions do not reset manual focal input.
- CameraLensKit stores user Prime/Zoom attachments, a remembered selected lens ID and the shared Ignore Lens Profile flag by stable camera ID in separate versioned DataStore records through PresetRepository. Built-in geometry stays read-only; only user cameras accept new lens attachments. Enforce this in UI, ViewModel and repositories; preserve existing built-in kits and allow their existing records to be viewed, edited or removed. Do not reuse obsolete pre-kit lens archives. Select a lens in Focal Length; use its shortcuts and focus-distance scale without passing the lens model to optical calculations. Use only the selected lens for shortcuts: exact prime focal length, a zoom's range plus exact endpoints, and its aperture envelope with exact f-numbers. Do not interpolate zoom apertures. Explicit lens selection clamps out-of-range focal/aperture values to its bounds; changing the kit or bypass flag alone never changes current optical inputs. Preserve existing focal standards and the chosen full/half/third aperture row; without supplied closure limits use that row's existing upper bound. Ignore Lens Profile bypasses focal/aperture presets and custom distance units together, showing only the app's standard rows. It preserves the remembered lens and all physical inputs. The old Show All Focal Lengths boolean persists this shared bypass; the old independent aperture flag is retained as legacy data. Manual input stays unrestricted; kit editing alone never changes optical inputs. Camera activation follows the focal-minimum and Standard-reset rules above. Deleting a custom camera removes its current kit, not legacy archives.
- Each lens has an optional persisted Meters/Feet distance-scale unit. New drafts default to the current app unit family (cm/m to Meters, inch/ft to Feet); explicit saved choices remain independent. Legacy null values follow the app until edited. Lens-kit v3 reads v1/v2 records, retaining optics, units, IDs and legacy preferences while adding selectedLensId. Show the current canonical distance for the selected camera's lenses in their list and editor preview, with the app-unit reference in parentheses when different; the selected lens overrides focus-distance display/input units, with the app units as reference. Hyperfocal results in both DOF and Hyperfocal modes keep app units first and append the selected lens's explicit scale units in parentheses when different; Ignore Lens Profile suppresses this secondary reading. Never rewrite global unit preferences or canonical physical distance. Clear unit-specific input drafts when lens/unit/bypass context changes. Compact readings use up to two decimals with localized separators and retain nonzero small values. Infinity remains explicit; invalid distance has no fabricated reading.
- The camera picker shows each kit as read-only lens descriptions, never a Lenses navigation button. Camera editing contains an inline lens list with Add/Edit/Delete below the camera fields. Lens subdialogs keep the camera form and scroll state mounted and return to it; closing/saving the camera clears that transient lens context. Lens operations retain their existing independent persistence behavior. New unsaved cameras show a disabled Add action and the save-first explanation.
- Use the exact recommendations in `BuiltInCameraProfiles` / `FrameFormat`; built-in IDs are permanent and built-in profiles are read-only.
- Profiles contain camera identity, format and physical frame dimensions only. Custom formats require a name and finite positive dimensions when created or edited; migrated unknown dimensions remain unknown until supplied.
- Store per-criterion sharpness parameters independently of camera profiles and DOF/Hyperfocal mode. A camera-ID change selects Standard without erasing these parameters; editing the same camera or restoring saved state must not reset the criterion. StandardCoCResolver validates actual frame geometry, then uses the exact linked catalogue recommendation or, when absent, 0.030 × hypot(width, height) / hypot(36, 24). A present but invalid catalogue CoC is an error. Its typed CATALOG / DIAGONAL_FALLBACK result feeds the same effective CoC as the other criteria; the sheet shows a neutral fallback explanation only for DIAGONAL_FALLBACK. Never infer catalogue membership from dimensions or names, or persist the derived fallback value in a camera profile. Standard, Pixel-Based, Video-Based, Print-Based, Screen-Based and Custom resolve through the pure Kotlin SharpnessCriterionCalculator.
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
- Diffraction is a one-way informational branch for conventional photography: Airy diameter = 2.44 × 0.00055 mm × nominal aperture. Compare with effective CoC using the 1.0 / 1.5 heuristics: below 1.0 NONE, from 1.0 POSSIBLE, from 1.5 NOTICEABLE. Warnings describe the gradual detail/DOF trade-off, never an absolute sharpness limit. Never derive magnification for a diffraction correction or feed diffraction back into DOF, CoC, hyperfocal or the slider. The experimental macro prototype is preserved only in archive/macro-prototype; legacy DataStore keys remain unused and unchanged.
- CloseFocusEvaluator is an independent informational thin-lens estimate from nominal focal length and canonical sensor-plane distance only. At m >= 0.2 (1:5), ordinary DOF shows a neutral card below Distance. D < 4f has a typed unavailable state with no fabricated ratio; allow a few ULPs at both boundaries. Infinity, invalid input and Hyperfocal hide the card. Cache its input/result together in immutable UI state; aperture/CoC/camera edits reuse the estimate. The separate close_focus_warning preference defaults on and does not reuse archived macro keys. The card only expands local help; no manual scale, macro navigation or effective-aperture correction is restored.
- Keep input validation separate from numeric formulas.
- Represent special results such as an infinite far limit explicitly; do not leak sentinel numbers into the UI.
- Store optical constants and defaults in domain/settings code, never inside Composables.
- Add focused unit tests for formulas, unit conversion, boundary values, invalid input, and infinity behavior before relying on UI tests.

## UI rules

- Use Material 3 components and the existing theme.
- The main distance card has separate click targets: its summary opens the distance dialog; the Hyperfocal button toggles between DOF and Hyperfocal and is green when enabled in both themes. In Hyperfocal the summary shows “Hyperfocal mode”; the numeric H remains in the results card. Stack the button below the summary on narrow screens or with large fonts. The dialog contains manual entry, the nonlinear slider and −/+ controls with a Close action. Tapping the summary in Hyperfocal asks whether to exit Hyperfocal and enter a distance manually: Yes returns to DOF and opens the editor, while No, Back and outside dismissal leave Hyperfocal unchanged. Preserve the saved manual distance/Infinity until the user edits it; never open a disabled distance editor from Hyperfocal. There is no Macro button or magnification sheet; close-focus information is an independent nonmodal card, and no top-level mode switch or H/Infinity shortcut button.
- Persist aperture increments independently of optical inputs. Full stop is the default; half/third stop change the preset row, selection and step buttons without quantizing or replacing the current manual aperture.
- Keep strings in Android resources and provide accessible labels/content descriptions where needed.
- Explanatory copy leads with the practical consequence or action, followed by the cause and optional technical detail. Use «глубина резкости» / depth of field. On first mention within a standalone explanation, expand the term as «кружок рассеяния (CoC, Circle of Confusion)» / Circle of Confusion (CoC); use plain wording or pronouns for later references.
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
- Production APKs use the permanent local Release identity documented in `docs/RELEASE_SIGNING.md`; `scripts/sign_release_macos.py` signs the unsigned Gradle output using this app's macOS Keychain item. Never replace the key or use Android Debug signing for public releases. Keep source/history and the Macro archive private; publish only documentation/images and signed APK/checksum assets in the separate public DoF-Master repository.

## Change discipline

- Read existing files before editing and make the smallest coherent change.
- Preserve the current package/application identity.
- Keep public names and code understandable; prefer explicit code over clever abstraction.
- Update this guide when an architectural decision or product constraint changes.

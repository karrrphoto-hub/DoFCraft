# Experimental Macro Research — DOF Calculator

## Status and preservation rule

This document describes the implemented macro prototype before its planned removal
from the ordinary DOF Calculator. It is intended to accompany the complete project
in an `archive/macro-prototype` branch (or a unique suffixed archive branch).

**A local copy of this document is not proof of a remote backup.** Removal is allowed
only after a private GitHub archive branch exists, its remote tip matches the local
archive commit, and this document and the implementation have been checked in that
remote commit. The GitHub URL, archive SHA and upload verification belong in the
operation report; none is assumed here.

The implementation is an informational magnification/diffraction extension. It
does **not** contain a separate, physically calibrated macro-DOF engine.

## Project and reproducibility

The snapshot is the existing single-module Kotlin Android project with Jetpack
Compose / Material 3, minimum API 26, the existing application identity, Gradle
wrapper and version catalog. It includes all six sharpness criteria, camera formats,
user camera profiles and camera lens kits. Lens kits generate input shortcuts; they
do not identify the lens actually mounted or supply macro calibration.

Restore the entire project, not isolated Kotlin files. Use the checked-in wrapper,
`gradle/libs.versions.toml`, Gradle build scripts and JVM criteria. Supply an Android
SDK locally through an ignored `local.properties`; signing keys, caches and APKs
are not part of the archive.

Relevant commands:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
./gradlew :app:assembleDebugAndroidTest :app:lintDebug
```

Instrumented tests should run on an explicitly selected emulator. The project guide
requires a data backup before any test run on a personal physical phone, because
Android Studio test deployment may clear application data.

At the last completed validation before the archival request, Gradle Sync, Debug
APK and lint passed, all **233 local unit tests** passed, and **9 selected UI tests**
passed on an emulator (macro warnings, diffraction, focus-distance dialogs in
English/Russian). The final macro-warning UI pair was rerun successfully after its
last component change. This records previous evidence; it does not claim that a
future checkout has already been rebuilt.

## Actually implemented behavior

### Auto and Manual

`CalculatorMode` has only `DepthOfField` and `Hyperfocal`. Macro is not a third
calculator-mode enum value. Its active UI state is DOF with
`DiffractionSettings.magnificationMode == MANUAL`.

When manual Macro is off, the existing estimator automatically derives an
approximate magnification from nominal focal length and recording-plane focus
distance. Estimates use the lower thin-lens branch, from zero to 1:1, and are
displayed with an approximation sign.

Manual Macro opens the existing magnification bottom sheet. In the current UI there
is no Auto/Manual selector inside that sheet; switching Macro off restores Auto
through the main-screen action. The form offers 1:10, 1:4, 1:2, 1:1, 2:1 and 5:1
presets, arbitrary positive A:B input, and a measured focus distance in metres.
Internally `m = A / B`, so 1:2 is 0.5 and 2:1 is 2.

### Measured distance and nominal focal length

Distance is measured **from the sensor/film plane to the subject**, not from the
front lens. The bottom-sheet distance is an unapplied draft of the calculator's
single canonical distance; Apply commits it to that shared distance. Opening the
sheet starts with an empty distance field. A dedicated action may explicitly
restore the saved pair. Cancel discards draft scale/distance edits.

The sheet uses the same focal-length entry, step buttons and kit-generated shortcuts
as the main focal dialog. Focal edits apply immediately to the canonical nominal
focal length, even if the sheet is subsequently cancelled. Such edits never derive
or replace the user's measured distance.

In active manual Macro, the main focal tile opens the macro sheet and shows a
Macro-mode label. Main-screen Hyperfocal and Macro buttons are mutually exclusive.
Entering Hyperfocal sets the active magnification mode to Auto while retaining the
last manual pair for explicit later reuse. Disabling Hyperfocal returns to ordinary
DOF, not to the previous manual Macro state.

### Thin-lens equivalent and consistency checks

The sheet previews nominal focal length, an informational thin-lens equivalent
focal length, effective aperture and the actual source of magnification. The
equivalent never replaces nominal focal length in geometric DOF or the focus slider.

The ratio `K = fEquivalent / fNominal` is checked against heuristic bounds:
`K < 0.25` or `K > 1.5` requires a separate confirmation on Apply. These bounds
detect possible input mistakes; they are not physical lens limits. Invalid,
non-positive or non-finite inputs cannot be applied. A manually entered distance
below four nominal focal lengths is not forbidden merely for being below 4f.

A confirmed manual configuration binds magnification to both its measured distance
and the nominal focal length used at confirmation. Distance/focal comparisons use
canonical numbers with a relative tolerance of `1e-9`, not formatted text.
Changing either input latches confirmation off. The saved values are retained,
while diffraction uses an explicitly labelled Auto fallback until confirmation.

Statuses are VALID, NEEDS_CONFIRMATION, INVALID and SUSPENDED_AT_INFINITY.
Optical Infinity suspends non-zero manual magnification and uses zero for the
current informational estimate. A return to an incompatible finite distance
cannot silently reactivate the old manual pair.

### Diffraction information

Magnification continuously adjusts the informational effective aperture, including
below the UI macro-indicator thresholds. It does not alter geometric DOF.

The diffraction module receives the effective CoC from the existing sharpness
system: Standard, Pixel-Based, Video-Based, Print-Based, Screen-Based or Custom.
It does not calculate a separate CoC. Warning levels are heuristic Airy/CoC bands:
NONE below 0.7, POSSIBLE from 0.7 to below 1.0, NOTICEABLE from 1.0.

The detail sheet includes nominal/effective aperture, magnification and its source,
Airy diameter, CoC, ratio and sharpness criterion. It explains the gradual loss of
fine-detail contrast, the DOF trade-off and the limits of comparing an Airy disk
with a geometric circle of confusion.

### Ordinary-DOF macro warning

`MacroWarningEvaluator` classifies the existing estimator output at the exact,
unrounded threshold `m >= 0.2` (1:5). It adds a neutral information card under
Distance and before DOF results. The card opens the same macro settings sheet,
without activating Manual or changing optical inputs.

The typed states distinguish None, MacroEstimated, unavailable at very close
distance, InvalidInput and ManualConfirmed, with explicit sources for scaled
results. D < 4f has its own unavailable message and no invented numerical ratio.
Infinity, Hyperfocal, invalid input and active manual Macro hide this new prompt;
existing manual-pair confirmation/status remains responsible for manual Macro.

The independent warning preference defaults on. While the new card is visible,
the older automatic scale badge is suppressed to avoid a duplicate prompt.
Diffraction warnings remain independent and can appear at the same time.
The derived detection input and result are memoized together in immutable UI state;
aperture/CoC changes reuse the detection when focal length, distance and scale
source have not changed.

## Mathematical models and units

All optical computations use Double. Frame/focal lengths and CoC are in millimetres;
canonical focus distance is in metres. Ratios are dimensionless. Formatting is the
only point where values are rounded or localized.

### Sensor-plane thin-lens magnification

For focal length f in mm, sensor-to-subject distance D in mm and magnification m:

```text
D = f * (2 + m + 1/m)
A = D/f - 2
m = 2 / (A + sqrt(A*A - 4))
```

Auto uses the 0 < m <= 1 branch when D >= 4f. At D = 4f, m = 1.
Explicit optical Infinity gives m = 0. Below 4f there is no real solution for this
idealized nominal-focal-length model; the estimator returns unavailable.

The implementation is an algebraically scaled version of the stable expression:

```text
x = f / D
inverseA = x / (1 - 2*x)
m = 2*inverseA / (1 + sqrt((1 - 2*inverseA)*(1 + 2*inverseA)))
```

It avoids subtracting nearly equal numbers and overflowing A squared. The actual
code divides focal millimetres by distance metres and then by 1000 to avoid
overflow in a metres-to-millimetres multiplication.

`MagnificationEstimator.distanceMeters` also retains the forward relation as a
pure utility. The current manual UI asks for measured distance; it does not use
this utility to invent a distance from the entered scale.

For f = 100 mm: D = 2 m gives m about 0.0557; 1 m about 0.1270;
0.72 m about 0.2; 0.6 m about 0.267949; 0.5 m about 0.381966; 0.4 m gives 1.

### Informational equivalent focal length

```text
fEquivalent = D / (2 + m + 1/m)
            = D * m / (1 + m)^2
```

D is in mm and the result is in mm. For large m, the implementation uses the
symmetry under m -> 1/m and a divided factor to avoid squaring large values.
A metres-based entry point also avoids an unnecessarily overflowing D*1000.

Examples: 300 mm at m=1 gives 75 mm; 600 mm at m=0.5 gives about 133.333 mm;
30 mm at m=1 gives 7.5 mm. The last is suspicious relative to a nominal 100 mm
lens and invokes confirmation; it is not rejected as universally impossible.

### Effective aperture and Airy disk

```text
P = exit pupil diameter / entrance pupil diameter
N_effective = N * (1 + m/P)
P = 1 in this prototype

wavelength = 550 nm = 0.00055 mm
airyDiameterMm = 2.44 * wavelength * N_effective
               = 0.001342 * N_effective
diffractionRatio = airyDiameterMm / effectiveCoCmm
```

At nominal f/8: m=0 gives effective f/8, m=0.5 gives f/12, m=1 gives f/16,
m=2 gives f/24 and m=5 gives f/48. The f/8, m=1 Airy diameter is 21.472 micrometres.

P is a constant, not editable in the UI. Aperture input is assumed nominal.
If a camera already displays a corrected effective f-number, applying this
correction again overestimates diffraction. The equivalent focal length is never
an input to this effective-aperture calculation.

An Airy disk describes diffraction in an ideal system, not a geometric defocus
circle. The 0.7/1.0 warning bands are application heuristics, not universal
scientific sharpness limits or a claim that the lens becomes unusable.

## Storage and compatibility

App preferences use the existing settings DataStore; camera profiles/lens kits
remain separate. No calculated equivalent focal length or auto estimate is persisted
as a new camera/lens property. The main calculator retains the actual canonical
focus distance in `last_focus_distance` and explicit Infinity in `focus_at_infinity`.

Macro-related keys in the current DataStore repository:

| Key | Meaning |
| --- | --- |
| magnification_mode | AUTO / MANUAL |
| manual_magnification | Last positive manual m |
| manual_pair_version | Version marker for the bound pair |
| manual_distance_m | Distance bound to manual scale, metres |
| manual_focal_mm | Nominal focal length at confirmation |
| manual_confirmed | Confirmation latch |
| macro_photography_warning | Independent ordinary-DOF information-card preference |
| legacy_manual_magnification | Read-only archive of the older standalone manual scale |
| focus_magnification | Older scale-driven-distance archive, read but not rewritten |

`diffraction_warnings` is a shared ordinary-diffraction setting, not disposable
macro data. `calculator_mode` remains DOF/Hyperfocal. The ordinary camera, sharpness,
lens-kit and optical-input keys must be preserved independently.

Old unbound manual values are kept but require confirmation when a bound distance
or focal length is unknown. A later removal should leave obsolete macro keys
unused rather than clearing DataStore, and preserve the actual saved distance.
Because active manual Macro is represented alongside DOF, ignoring that legacy
magnification state naturally restores ordinary DOF. Existing Hyperfocal remains
Hyperfocal. Unknown older calculator-mode names should safely fall back to DOF.

## Source inventory

Paths below are relative to
`app/src/main/java/com/example/dofcalculator/`.

### Predominantly or exclusively macro-specific

- `calculation/MagnificationEstimator.kt`
- `calculation/ThinLensEquivalentCalculator.kt`
- `calculation/ManualMagnificationValidator.kt`
- `calculation/MacroWarningEvaluator.kt`
- `model/MagnificationMode.kt`
- `model/ManualMagnificationState.kt`
- `model/MacroDetectionState.kt`
- `ui/calculator/MacroFormUiState.kt`
- `ui/calculator/MagnificationFormatter.kt`
- `ui/calculator/MacroWarningCard.kt`

### Shared files with macro integrations; do not remove wholesale

- `calculation/DiffractionCalculator.kt`: ordinary Airy/CoC math plus macro aperture correction.
- `model/DiffractionResult.kt`: ordinary results plus effective-aperture/magnification fields.
- `settings/DiffractionSettings.kt`: ordinary warning preference plus manual-pair preferences.
- `settings/AppSettings.kt`: calculator/preferences boundary, legacy scale field, macro warning flag.
- `settings/DataStoreAppSettingsRepository.kt`: current/legacy keys and safe persistence.
- `settings/InMemoryAppSettingsRepository.kt`: test/in-memory preferences.
- `ui/calculator/MagnificationSettingsSheet.kt`: macro editor **and ordinary diffraction detail UI**.
- `ui/calculator/DiffractionWarning.kt`: ordinary warning plus scale labels, manual notices and badge.
- `ui/calculator/DofCalculatorViewModel.kt`: shared optical inputs, macro draft/apply/confirm,
  manual assessment, diffraction, detection and persistence.
- `ui/calculator/DofCalculatorUiState.kt`: calculator data, macro-derived/form states and visibility.
- `ui/calculator/DofCalculatorScreen.kt`: route callbacks and dialog composition.
- `ui/calculator/CompactCalculatorContent.kt`: shared focal input, focal-tile macro routing,
  macro-mode label, warning placement and results.
- `ui/calculator/DistanceParameterTile.kt`: shared distance/Hyperfocal controls and Macro button.
- `ui/calculator/FocusDistanceDialogs.kt`: shared distance dialog visibility.
- `ui/calculator/AppDialogs.kt`: general settings, macro preference and settings entry.
- `app/src/main/res/values/strings.xml` and `values-ru/strings.xml`: shared and macro text.
- `AGENTS.md`: behavioral constraints and development rules.

The unchanged geometric foundation includes `DepthOfFieldCalculator`,
`DepthOfFieldInput`, `DepthOfFieldResult`, `CalculatedDistance`,
`FocusDistanceScale`, `DofVisualization` and all CoC calculation modules.
Their real dependencies should still be reviewed before any future extraction.

## Test inventory

Relative to `app/src/test/java/com/example/dofcalculator/`:

- `calculation/MagnificationEstimatorTest.kt`
- `calculation/ThinLensEquivalentCalculatorTest.kt`
- `calculation/MacroWarningEvaluatorTest.kt`
- `ui/calculator/MacroFocusDistanceTest.kt`
- `ui/calculator/MacroWarningViewModelTest.kt`
- `ui/calculator/MagnificationFormatterTest.kt`
- `settings/ManualMacroPersistenceTest.kt`
- `settings/MacroWarningPersistenceTest.kt`

Shared tests also exercise macro dependencies and must be adapted selectively:

- `calculation/DiffractionCalculatorTest.kt`
- `settings/DiffractionPersistenceTest.kt`
- `ui/calculator/DiffractionViewModelTest.kt`
- `ui/calculator/FocusDistanceControlTest.kt`
- `ui/calculator/CameraLensesViewModelTest.kt`
- `ui/calculator/StandardCoCViewModelTest.kt`
- `ui/calculator/VideoSharpnessViewModelTest.kt`

Instrumented UI tests under `app/src/androidTest/java/com/example/dofcalculator/`:

- `MacroWarningUiTest.kt`
- `DiffractionUiTest.kt`
- `FocusDistanceDialogsTest.kt`

Keep the rest of the complete test suite in the archive: it establishes that
camera/lens metadata, all six criteria, Infinity, adaptive distance and geometric
DOF were not altered by the informational macro additions.

## Precision limits and what was not implemented

- Real lenses can change effective focal length under internal focusing.
- Principal planes can be separated and can move while focusing.
- Entrance/exit pupil asymmetry makes P different from the assumed 1.
- The Auto model assumes an ideal thin lens and fixed nominal focal length.
- One sensor-plane distance has reciprocal thin-lens branches m and 1/m; Auto cannot
  identify magnification above 1:1 from distance alone.
- A single measured magnification/distance pair does not determine the real focal
  length or principal-plane positions of a general real lens. fEquivalent is only
  a model equivalent, not a measurement of a lens specification.
- Extension tubes, bellows and supplementary optics are not physically modeled.
- The ordinary DOF equations remain based on nominal focal length, nominal aperture
  and the selected CoC. Their accuracy can be limited at high magnification.
- No separate macro near/far-limit, pupil-aware macro-DOF, lens calibration or
  exposure-compensation engine was implemented.
- No lens-specific macro profiles, active-lens selection, variable pupil-ratio UI,
  payments, Macro Pro entitlement or additional app/module was implemented.

## Possible future work — not implemented

Potential directions are a standalone Macro DOF Calculator, an optional Macro Pro
feature, or a reusable independent mathematical module. None is part of the
current removal task.

Research before implementation should cover:

1. A precise and documented macro-DOF model and viewing/CoC assumptions.
2. Pupil magnification and the relationship between nominal and effective f-number.
3. Calibration against multiple measured magnification/distance pairs.
4. Real-lens internal focusing and principal-plane movement.
5. Extension-tube, bellows and supplementary-lens configurations.
6. Clear uncertainty, source and applicability reporting.
7. Separation of pure mathematics from Android, preference migration and UI.
8. Validation against measured optical systems, not only ideal thin-lens examples.

## Future restoration

After the archive has actually been pushed and verified, obtain a separate working
copy without modifying the main checkout:

```sh
git fetch origin
git worktree add ../DOF-Calculator-Macro archive/macro-prototype
```

Use the actual unique archive branch name from the final operation report if the
preferred name was already occupied. Do not delete the current checkout or reset
the main branch to recover this research.

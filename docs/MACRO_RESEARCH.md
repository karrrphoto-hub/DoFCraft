# Archived experimental macro prototype

The ordinary DOF Calculator no longer includes the experimental macro UI,
magnification models or macro diffraction correction.

The complete implementation, tests, resources, build configuration and detailed
research notes are preserved in the private repository:

- Repository: https://github.com/karrrphoto-hub/DOF-Calculator
- Branch: `archive/macro-prototype`
- Commit: `e92688b2ee250d12e19e3d48cd461b08a0ecc3ba`
- Full research document in that branch: `docs/MACRO_RESEARCH.md`

After the archive was uploaded through Android Studio, a fresh Fetch on
2026-11-08 returned the exact SHA above. The fetched commit includes the
magnification estimator, thin-lens equivalent calculator, manual pair validation,
macro detection/UI, persistence code and tests. Private visibility was confirmed
by the repository owner in the GitHub interface.

Restore a separate working copy without changing the main application's branch:

```sh
git fetch origin
git worktree add ../DOF-Calculator-Macro archive/macro-prototype
```

The archive documents the thin-lens magnification and equivalent-focal formulas,
effective aperture and Airy-disk comparison, implemented behavior, file/test
inventory, precision limitations and possible future work. It contains no separate
calibrated macro-DOF engine.

Main retains conventional diffraction using nominal aperture and the existing
CoC/thresholds. Legacy macro DataStore values remain unused and unmodified.

# AudioTool clone (v0.1, personal use)

Android app for SPL, Leq and fractional-octave spectrum, written from the public recon in `../recon.md`.
Working name only; `replica-brand` replaces it before anything else.

## Build the APK

1. Install Android Studio (it bundles the JDK 17 and the Android SDK).
2. Open the `android/` folder as a project. Accept the SDK prompts (Platform 34, Build Tools).
3. Android Studio creates the Gradle wrapper on first sync. If it asks, let it generate one.
4. Build > Build Bundle(s) / APK(s) > Build APK(s). The file is `app/build/outputs/apk/debug/app-debug.apk`.
5. Install on the phone (allow installs from this source), grant the microphone permission on first Start.

Command line, once the wrapper exists: `./gradlew assembleDebug`.

## Check the DSP core (no Android needed)

```bash
cd android/app
javac -d /tmp/dsp src/main/java/com/replica/audiotool/dsp/*.java src/test/java/com/replica/audiotool/dsp/DspSelfTest.java
java -cp /tmp/dsp com.replica.audiotool.dsp.DspSelfTest
```

Expected: 14 checks, 0 failures. Covers the FFT against a naive DFT, A and C weighting at known points, sine level with and without calibration offset, IEC 61260 band counts, band placement, Leq, and averaging with peak and valley.

## What is in v1

- Live SPL readout with calibration offset (stored on the phone).
- Weighting: Flat, A, C (cycle button).
- Band spectrum: 1/1, 1/3, 1/6, 1/12 octave (cycle button), with averaging, peak and valley ticks.
- Leq since last reset, Reset button.
- Microphone: UNPROCESSED source on Android 10 and later, else MIC.

## Not in v1 (listed in `../features.csv`)

Chart Recorder, cursors, store and load of spectra, WAV record and playback, signal generator, polarity check, RT60, sweep and Peak Store, target curves, per-band calibration, X weighting, sone, NC and NR overlays, file management.

## Known limits

- Accuracy depends on the phone's microphone. Calibrate against a reference meter.
- Not tested on a device. The DSP is tested; capture and UI are checked only for compile errors, which means the framework classes were not available here.
- Gradle wrapper is not in the repo; Android Studio generates it.

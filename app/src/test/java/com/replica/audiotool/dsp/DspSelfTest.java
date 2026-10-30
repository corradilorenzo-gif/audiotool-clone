package com.replica.audiotool.dsp;

import java.util.Random;

/**
 * Plain-Java checks for the DSP core. No JUnit, so it runs with javac and java alone.
 * Run: see replica/app/README.md. Exit code 0 = all passed.
 */
public final class DspSelfTest {
    private static int failures = 0;
    private static int checks = 0;

    public static void main(String[] args) {
        fftMatchesNaiveDft();
        weightingKnownValues();
        sineLevelFlat();
        bandCountThirdOctave();
        sineLandsInItsBand();
        leqOfConstantLevel();
        averagerPeakValley();
        System.out.println("checks: " + checks + ", failures: " + failures);
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void check(String name, boolean ok, String detail) {
        checks++;
        if (!ok) failures++;
        System.out.println((ok ? "PASS " : "FAIL ") + name + (detail.isEmpty() ? "" : "  " + detail));
    }

    private static void fftMatchesNaiveDft() {
        int n = 64;
        Random r = new Random(1);
        double[] re = new double[n], im = new double[n];
        for (int i = 0; i < n; i++) re[i] = r.nextGaussian();
        double[] refRe = new double[n], refIm = new double[n];
        for (int k = 0; k < n; k++) {
            for (int t = 0; t < n; t++) {
                double a = -2 * Math.PI * k * t / n;
                refRe[k] += re[t] * Math.cos(a);
                refIm[k] += re[t] * Math.sin(a);
            }
        }
        double[] fr = re.clone(), fi = im.clone();
        Fft.transform(fr, fi);
        double maxErr = 0;
        for (int k = 0; k < n; k++) {
            maxErr = Math.max(maxErr, Math.abs(fr[k] - refRe[k]));
            maxErr = Math.max(maxErr, Math.abs(fi[k] - refIm[k]));
        }
        check("FFT matches naive DFT (N=64)", maxErr < 1e-9, String.format("max error %.2e", maxErr));
    }

    private static void weightingKnownValues() {
        double a1k = Weighting.gainDb(Weighting.Mode.A, 1000);
        double a100 = Weighting.gainDb(Weighting.Mode.A, 100);
        double a10k = Weighting.gainDb(Weighting.Mode.A, 10000);
        double c1k = Weighting.gainDb(Weighting.Mode.C, 1000);
        check("A-weighting at 1 kHz is 0 dB", Math.abs(a1k) < 0.1, String.format("%.3f", a1k));
        check("A-weighting at 100 Hz is about -19.1 dB", Math.abs(a100 + 19.1) < 0.3, String.format("%.3f", a100));
        check("A-weighting at 10 kHz is about -2.5 dB", Math.abs(a10k + 2.5) < 0.3, String.format("%.3f", a10k));
        check("C-weighting at 1 kHz is 0 dB", Math.abs(c1k) < 0.1, String.format("%.3f", c1k));
    }

    private static void sineLevelFlat() {
        double fs = 48000;
        int n = 4096;
        int bin = 85; // 996.09 Hz, not on a band edge
        double amp = 0.5;
        double[] x = new double[n];
        for (int i = 0; i < n; i++) x[i] = amp * Math.sin(2 * Math.PI * bin * i / n);
        double expected = 10 * Math.log10(amp * amp / 2.0); // -9.03 dBFS
        double got = new SplMeter(Weighting.Mode.FLAT, 0.0).levelDb(x, fs);
        check("sine amplitude 0.5 reads -9.03 dB flat (within 0.3)", Math.abs(got - expected) < 0.3,
                String.format("got %.2f expected %.2f", got, expected));

        double withOffset = new SplMeter(Weighting.Mode.FLAT, 94.0).levelDb(x, fs);
        check("calibration offset adds directly", Math.abs(withOffset - got - 94.0) < 1e-9,
                String.format("%.4f", withOffset - got));
    }

    private static void bandCountThirdOctave() {
        Bands.Band[] b = Bands.fractional(3, 19.9, 20100); // exact grid centres 19.95 Hz to 19.95 kHz
        check("1/3 octave bands from 20 Hz to 20 kHz (IEC 61260 grid) is 31", b.length == 31,
                "count=" + b.length);
        Bands.Band[] oct = Bands.fractional(1, 19.9, 20100);
        check("1/1 octave bands from 20 Hz to 20 kHz is 10", oct.length == 10, "count=" + oct.length);
        check("1/3 band centred on 1 kHz exists", java.util.Arrays.stream(b)
                .anyMatch(x -> Math.abs(x.centre - 1000.0) < 0.5), "");
    }

    private static void sineLandsInItsBand() {
        double fs = 48000;
        int n = 4096;
        int bin = 85;
        double[] x = new double[n];
        for (int i = 0; i < n; i++) x[i] = 0.5 * Math.sin(2 * Math.PI * bin * i / n);
        Bands.Band[] bands = Bands.fractional(3, 19.9, 20100);
        double[] levels = Spectrum.bandLevelsDb(x, fs, bands, Weighting.Mode.FLAT, 0.0);
        int best = 0;
        for (int i = 1; i < levels.length; i++) if (levels[i] > levels[best]) best = i;
        double centre = bands[best].centre;
        check("996 Hz sine peaks in the 1 kHz 1/3-octave band", Math.abs(centre - 1000.0) < 1.0,
                String.format("centre %.1f Hz, %.2f dB", centre, levels[best]));
    }

    private static void leqOfConstantLevel() {
        Leq leq = new Leq();
        for (int i = 0; i < 100; i++) leq.add(80.0);
        check("Leq of constant 80 dB is 80 dB", Math.abs(leq.valueDb() - 80.0) < 1e-9,
                String.format("%.6f", leq.valueDb()));
    }

    private static void averagerPeakValley() {
        BandAverager a = new BandAverager(1);
        a.add(new double[]{60});
        a.add(new double[]{80});
        a.add(new double[]{70});
        check("averager mean is arithmetic dB mean", Math.abs(a.averageDb()[0] - 70.0) < 1e-9,
                String.format("%.3f", a.averageDb()[0]));
        check("averager peak is 80, valley is 60",
                a.peakDb()[0] == 80.0 && a.valleyDb()[0] == 60.0,
                "peak " + a.peakDb()[0] + " valley " + a.valleyDb()[0]);
    }
}

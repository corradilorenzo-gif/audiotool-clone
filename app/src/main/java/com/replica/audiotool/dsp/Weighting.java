package com.replica.audiotool.dsp;

/** Frequency weightings from the analytic form of IEC 61672 (A and C). Flat is 0 dB everywhere. */
public final class Weighting {
    public enum Mode { FLAT, A, C }

    private static final double F1 = 20.598997;
    private static final double F2 = 107.65265;
    private static final double F3 = 737.86223;
    private static final double F4 = 12194.217;

    private Weighting() {}

    /** Gain in dB for frequency f (Hz). */
    public static double gainDb(Mode mode, double f) {
        if (mode == Mode.FLAT || f <= 0) return 0.0;
        double f2 = f * f;
        double f4sq = f2 * f2;
        double f1s = F1 * F1, f4s = F4 * F4;
        if (mode == Mode.A) {
            double ra = (f4s * f4sq)
                    / ((f2 + f1s) * Math.sqrt((f2 + F2 * F2) * (f2 + F3 * F3)) * (f2 + f4s));
            return 20.0 * Math.log10(ra) + 2.00;
        }
        // C
        double rc = (f4s * f2) / ((f2 + f1s) * (f2 + f4s));
        return 20.0 * Math.log10(rc) + 0.06;
    }

    /** Linear power gain for frequency f. */
    public static double powerGain(Mode mode, double f) {
        return Math.pow(10.0, gainDb(mode, f) / 10.0);
    }
}

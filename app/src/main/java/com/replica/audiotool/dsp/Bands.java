package com.replica.audiotool.dsp;

import java.util.ArrayList;
import java.util.List;

/**
 * Fractional-octave bands on the IEC 61260 base-10 grid: one octave is a ratio of 10^0.3 (about 1.995),
 * so 1/3 octave centres are 1000 * 10^(n/10), which gives the 31 nominal bands from 20 Hz to 20 kHz.
 */
public final class Bands {

    public static final class Band {
        public final double centre, lo, hi;

        Band(double centre, double lo, double hi) {
            this.centre = centre;
            this.lo = lo;
            this.hi = hi;
        }
    }

    private Bands() {}

    /** Base-10 octave ratio, 10^0.3. */
    static final double OCTAVE = Math.pow(10.0, 0.3);

    /**
     * Bands with perOctave in {1, 3, 6, 12} whose centre lies in [fMin, fMax].
     * Centre = 1000 * OCTAVE^(n / perOctave); edges at centre * OCTAVE^(+/- 1/(2*perOctave)).
     */
    public static Band[] fractional(int perOctave, double fMin, double fMax) {
        List<Band> out = new ArrayList<>();
        double half = 1.0 / (2.0 * perOctave);
        for (int n = -200 * perOctave; n <= 200 * perOctave; n++) {
            double c = 1000.0 * Math.pow(OCTAVE, (double) n / perOctave);
            if (c < fMin || c > fMax) continue;
            out.add(new Band(c, c * Math.pow(OCTAVE, -half), c * Math.pow(OCTAVE, half)));
        }
        return out.toArray(new Band[0]);
    }

    /**
     * Sums single-sided bin powers into bands. Bin k is at k * sampleRate / frameSize.
     * Bins are assigned to the band whose [lo, hi) contains their frequency.
     */
    public static double[] bandPower(double[] binPower, double sampleRate, int frameSize, Band[] bands) {
        double[] out = new double[bands.length];
        double binHz = sampleRate / frameSize;
        for (int k = 0; k < binPower.length; k++) {
            double f = k * binHz;
            for (int b = 0; b < bands.length; b++) {
                if (f >= bands[b].lo && f < bands[b].hi) {
                    out[b] += binPower[k];
                    break;
                }
            }
        }
        return out;
    }
}

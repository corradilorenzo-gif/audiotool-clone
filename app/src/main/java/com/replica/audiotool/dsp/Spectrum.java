package com.replica.audiotool.dsp;

/** One analysis frame -> levels per band, with weighting and calibration offset applied. */
public final class Spectrum {
    private Spectrum() {}

    /**
     * Band levels in dB for one block of samples (full scale = 1.0).
     * Weighting gain is applied per bin before summing into bands. Bands with no bins report -200 dB.
     */
    public static double[] bandLevelsDb(double[] samples, double sampleRate,
                                        Bands.Band[] bands, Weighting.Mode mode, double offsetDb) {
        int n = samples.length;
        double[] p = Fft.powerSpectrum(samples);
        double binHz = sampleRate / n;
        for (int k = 0; k < p.length; k++) {
            if (k == 0 && mode != Weighting.Mode.FLAT) {
                p[k] = 0.0;
            } else {
                p[k] *= Weighting.powerGain(mode, k * binHz);
            }
        }
        double[] power = Bands.bandPower(p, sampleRate, n, bands);
        double[] out = new double[bands.length];
        for (int b = 0; b < bands.length; b++) {
            double mean = power[b] / SplMeter.HANN_ENBW;
            out[b] = mean <= 1e-20 ? -200.0 + offsetDb : 10.0 * Math.log10(mean) + offsetDb;
        }
        return out;
    }
}

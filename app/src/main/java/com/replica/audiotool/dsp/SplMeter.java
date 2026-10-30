package com.replica.audiotool.dsp;

/**
 * Sound pressure level from a block of samples.
 * Samples are full scale = 1.0. Level = 10*log10(weighted mean power) + calibration offset.
 * The offset is the user's calibration against a reference meter.
 */
public final class SplMeter {
    /** Equivalent noise bandwidth of a Hann window, in bins. Divides the summed bin powers back to mean power. */
    static final double HANN_ENBW = 1.5;

    private final Weighting.Mode weighting;
    private double offsetDb;

    public SplMeter(Weighting.Mode weighting, double offsetDb) {
        this.weighting = weighting;
        this.offsetDb = offsetDb;
    }

    public void setOffsetDb(double offsetDb) { this.offsetDb = offsetDb; }
    public double getOffsetDb() { return offsetDb; }
    public Weighting.Mode getWeighting() { return weighting; }

    /** SPL in dB for one block. Block length should be a power of two (for example 4096). */
    public double levelDb(double[] samples, double sampleRate) {
        double[] p = Fft.powerSpectrum(samples);
        int n = samples.length;
        double binHz = sampleRate / n;
        double sum = 0.0;
        for (int k = 0; k < p.length; k++) {
            if (k == 0 && weighting != Weighting.Mode.FLAT) continue; // no DC in A or C
            sum += p[k] * Weighting.powerGain(weighting, k * binHz);
        }
        double meanPower = sum / HANN_ENBW;
        if (meanPower <= 1e-20) return -200.0 + offsetDb; // silence floor
        return 10.0 * Math.log10(meanPower) + offsetDb;
    }
}

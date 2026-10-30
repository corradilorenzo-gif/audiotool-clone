package com.replica.audiotool.dsp;

/**
 * Per-band tracking for the analyser: arithmetic mean of dB over frames (averaging),
 * plus running peak and valley per band. Reset clears all of it.
 */
public final class BandAverager {
    private final int bands;
    private final double[] sumDb;
    private final double[] peakDb;
    private final double[] valleyDb;
    private int frames;

    public BandAverager(int bands) {
        this.bands = bands;
        this.sumDb = new double[bands];
        this.peakDb = new double[bands];
        this.valleyDb = new double[bands];
        reset();
    }

    public void reset() {
        frames = 0;
        for (int i = 0; i < bands; i++) {
            sumDb[i] = 0.0;
            peakDb[i] = Double.NEGATIVE_INFINITY;
            valleyDb[i] = Double.POSITIVE_INFINITY;
        }
    }

    /** Add one frame of band levels in dB. */
    public void add(double[] levelsDb) {
        frames++;
        for (int i = 0; i < bands; i++) {
            double v = levelsDb[i];
            sumDb[i] += v;
            if (v > peakDb[i]) peakDb[i] = v;
            if (v < valleyDb[i]) valleyDb[i] = v;
        }
    }

    public int frames() { return frames; }

    /** Arithmetic mean of dB per band over all frames added since reset. */
    public double[] averageDb() {
        double[] out = new double[bands];
        for (int i = 0; i < bands; i++) {
            out[i] = frames == 0 ? Double.NaN : sumDb[i] / frames;
        }
        return out;
    }

    public double[] peakDb() { return peakDb.clone(); }
    public double[] valleyDb() { return valleyDb.clone(); }
}

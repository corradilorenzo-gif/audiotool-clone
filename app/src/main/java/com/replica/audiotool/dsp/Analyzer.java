package com.replica.audiotool.dsp;

/**
 * Holds the analysis state for one session: weighting, band layout, calibration offset,
 * averaging, peak and valley, and Leq. Each block of samples produces a Frame for the UI.
 * All public methods are synchronized, so the capture thread and the UI thread can share it.
 */
public final class Analyzer {
    public static final double SAMPLE_RATE = 48000.0;
    public static final int BLOCK = 4096;

    /** One UI update: current levels plus the running statistics. */
    public static final class Frame {
        public final double splDb;
        public final double leqDb;
        public final double[] bandDb;
        public final double[] avgDb;
        public final double[] peakDb;
        public final double[] valleyDb;
        public final double[] centres;

        Frame(double spl, double leq, double[] bandDb, double[] avg, double[] peak, double[] valley,
              double[] centres) {
            this.splDb = spl;
            this.leqDb = leq;
            this.bandDb = bandDb;
            this.avgDb = avg;
            this.peakDb = peak;
            this.valleyDb = valley;
            this.centres = centres;
        }
    }

    private SplMeter meter;
    private int perOctave = 3;
    private Bands.Band[] bands;
    private BandAverager averager;
    private final Leq leq = new Leq();

    public Analyzer(Weighting.Mode mode, double offsetDb) {
        this.meter = new SplMeter(mode, offsetDb);
        rebuildBands();
    }

    private void rebuildBands() {
        bands = Bands.fractional(perOctave, 19.9, 20100);
        averager = new BandAverager(bands.length);
    }

    public synchronized void setWeighting(Weighting.Mode mode) {
        this.meter = new SplMeter(mode, meter.getOffsetDb());
        averager.reset();
        leq.reset();
    }

    public synchronized Weighting.Mode getWeighting() {
        return meter.getWeighting();
    }

    /** Sets bands per octave: 1, 3, 6 or 12. Clears averaging, peak, valley and Leq. */
    public synchronized void setPerOctave(int n) {
        this.perOctave = n;
        rebuildBands();
        leq.reset();
    }

    public synchronized int getPerOctave() { return perOctave; }

    public synchronized void setOffsetDb(double offsetDb) {
        meter.setOffsetDb(offsetDb);
    }

    public synchronized double getOffsetDb() { return meter.getOffsetDb(); }

    /** Clears averaging, peak, valley and Leq. */
    public synchronized void resetStatistics() {
        averager.reset();
        leq.reset();
    }

    /** Process one block of BLOCK samples (full scale = 1.0). */
    public synchronized Frame process(double[] samples) {
        double spl = meter.levelDb(samples, SAMPLE_RATE);
        double[] bandDb = Spectrum.bandLevelsDb(samples, SAMPLE_RATE, bands, meter.getWeighting(),
                meter.getOffsetDb());
        averager.add(bandDb);
        leq.add(spl);
        double[] centres = new double[bands.length];
        for (int i = 0; i < bands.length; i++) centres[i] = bands[i].centre;
        return new Frame(spl, leq.valueDb(), bandDb, averager.averageDb(), averager.peakDb(),
                averager.valleyDb(), centres);
    }
}

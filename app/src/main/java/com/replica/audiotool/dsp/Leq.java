package com.replica.audiotool.dsp;

/** Equivalent continuous level: energy average of the levels fed in. */
public final class Leq {
    private double sumLinear;
    private long count;

    /** Add one level reading in dB. */
    public void add(double levelDb) {
        sumLinear += Math.pow(10.0, levelDb / 10.0);
        count++;
    }

    public long count() { return count; }

    /** Leq in dB, or NaN if nothing has been added. */
    public double valueDb() {
        if (count == 0) return Double.NaN;
        return 10.0 * Math.log10(sumLinear / count);
    }

    public void reset() {
        sumLinear = 0.0;
        count = 0;
    }
}

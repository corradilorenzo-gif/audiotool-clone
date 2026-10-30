package com.replica.audiotool.dsp;

/** Radix-2 FFT and Hann-windowed single-sided power spectrum. Pure Java, no dependencies. */
public final class Fft {
    private Fft() {}

    /** In-place iterative complex FFT. Length must be a power of two. */
    public static void transform(double[] re, double[] im) {
        int n = re.length;
        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            for (; (j & bit) != 0; bit >>= 1) j ^= bit;
            j ^= bit;
            if (i < j) {
                double t = re[i]; re[i] = re[j]; re[j] = t;
                t = im[i]; im[i] = im[j]; im[j] = t;
            }
        }
        for (int len = 2; len <= n; len <<= 1) {
            double ang = -2.0 * Math.PI / len;
            double wRe = Math.cos(ang), wIm = Math.sin(ang);
            int half = len >> 1;
            for (int i = 0; i < n; i += len) {
                double curRe = 1.0, curIm = 0.0;
                for (int k = 0; k < half; k++) {
                    int a = i + k, b = a + half;
                    double tRe = re[b] * curRe - im[b] * curIm;
                    double tIm = re[b] * curIm + im[b] * curRe;
                    re[b] = re[a] - tRe;
                    im[b] = im[a] - tIm;
                    re[a] += tRe;
                    im[a] += tIm;
                    double nRe = curRe * wRe - curIm * wIm;
                    curIm = curRe * wIm + curIm * wRe;
                    curRe = nRe;
                }
            }
        }
    }

    /**
     * Hann-windowed single-sided power spectrum, bins 0..N/2.
     * For a sine of amplitude A centred on a bin, that bin's value is A^2 / 2.
     * Samples are full scale = 1.0.
     */
    public static double[] powerSpectrum(double[] samples) {
        int n = samples.length;
        double[] re = new double[n];
        double[] im = new double[n];
        for (int i = 0; i < n; i++) {
            double w = 0.5 - 0.5 * Math.cos(2.0 * Math.PI * i / n);
            re[i] = samples[i] * w;
        }
        transform(re, im);
        double coherentGain = 0.5; // Hann
        double[] p = new double[n / 2 + 1];
        for (int k = 0; k <= n / 2; k++) {
            double mag = Math.hypot(re[k], im[k]);
            boolean edge = (k == 0 || k == n / 2);
            double amp = (edge ? 1.0 : 2.0) * mag / (n * coherentGain);
            p[k] = edge ? amp * amp : amp * amp / 2.0;
        }
        return p;
    }
}

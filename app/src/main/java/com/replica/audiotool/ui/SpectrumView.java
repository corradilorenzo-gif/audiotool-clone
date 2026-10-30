package com.replica.audiotool.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Band bars for the current frame, with peak (blue) and valley (white) ticks and a
 * frequency label row. dB range is fixed for v1: MIN_DB to MAX_DB.
 */
public final class SpectrumView extends View {
    static final double MIN_DB = 0.0;
    static final double MAX_DB = 100.0;

    private double[] bandDb = new double[0];
    private double[] peakDb = new double[0];
    private double[] valleyDb = new double[0];
    private double[] centres = new double[0];

    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint peak = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valley = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);

    public SpectrumView(Context context, AttributeSet attrs) {
        super(context, attrs);
        bar.setColor(Color.rgb(76, 175, 80));
        peak.setColor(Color.rgb(33, 150, 243));
        valley.setColor(Color.WHITE);
        text.setColor(Color.LTGRAY);
        text.setTextSize(24f * getResources().getDisplayMetrics().density / 2f);
        grid.setColor(Color.DKGRAY);
        grid.setStrokeWidth(2f);
    }

    /** Replace the data and redraw. Call on the UI thread. */
    public void setFrame(double[] bandDb, double[] peakDb, double[] valleyDb, double[] centres) {
        this.bandDb = bandDb;
        this.peakDb = peakDb;
        this.valleyDb = valleyDb;
        this.centres = centres;
        invalidate();
    }

    private float yFor(double db, float h) {
        double clamped = Math.max(MIN_DB, Math.min(MAX_DB, db));
        return (float) (h - (clamped - MIN_DB) / (MAX_DB - MIN_DB) * h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (bandDb.length == 0 || w == 0 || h == 0) return;

        float labelH = text.getTextSize() * 1.5f;
        float plotH = h - labelH;
        float slot = w / (float) bandDb.length;
        float barW = Math.max(1f, slot * 0.7f);

        for (int i = 0; i < bandDb.length; i++) {
            float x = i * slot + (slot - barW) / 2f;
            float top = yFor(bandDb[i], plotH);
            canvas.drawRect(x, top, x + barW, plotH, bar);
            if (peakDb.length == bandDb.length && Double.isFinite(peakDb[i])) {
                float py = yFor(peakDb[i], plotH);
                canvas.drawRect(x, py - 2, x + barW, py + 2, peak);
            }
            if (valleyDb.length == bandDb.length && Double.isFinite(valleyDb[i])) {
                float vy = yFor(valleyDb[i], plotH);
                canvas.drawRect(x, vy - 2, x + barW, vy + 2, valley);
            }
        }

        // Frequency labels at a few anchors, so the axis is readable.
        double[] anchors = {63, 250, 1000, 4000, 16000};
        for (double a : anchors) {
            int idx = nearest(a);
            if (idx < 0) continue;
            String label = a >= 1000 ? (int) (a / 1000) + "k" : String.valueOf((int) a);
            float cx = idx * slot + slot / 2f;
            canvas.drawText(label, cx - text.measureText(label) / 2f, h - 6f, text);
        }
        canvas.drawLine(0, plotH, w, plotH, grid);
    }

    private int nearest(double f) {
        int best = -1;
        double bestDiff = Double.MAX_VALUE;
        for (int i = 0; i < centres.length; i++) {
            double d = Math.abs(Math.log(centres[i] / f));
            if (d < bestDiff) {
                bestDiff = d;
                best = i;
            }
        }
        return best;
    }
}

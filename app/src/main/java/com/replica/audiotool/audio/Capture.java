package com.replica.audiotool.audio;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;

import com.replica.audiotool.dsp.Analyzer;

/**
 * Microphone capture on its own thread. Delivers blocks of BLOCK samples as doubles
 * (full scale = 1.0) to the listener. Prefers the UNPROCESSED source on API 29+ so the
 * phone does not apply its own noise suppression or AGC; falls back to MIC.
 */
public final class Capture {

    public interface Listener {
        /** Called on the capture thread. The array is reused: copy it if you keep it. */
        void onBlock(double[] samples);

        void onError(String message);
    }

    private volatile boolean running;
    private Thread thread;
    private AudioRecord record;

    @SuppressLint("MissingPermission") // caller must have RECORD_AUDIO granted
    public synchronized boolean start(Listener listener) {
        if (running) return true;
        int minBytes = AudioRecord.getMinBufferSize((int) Analyzer.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int bufBytes = Math.max(minBytes, Analyzer.BLOCK * 2 * 2);

        AudioRecord rec = null;
        if (Build.VERSION.SDK_INT >= 29) {
            rec = build(MediaRecorder.AudioSource.UNPROCESSED, bufBytes);
        }
        if (rec == null) {
            rec = build(MediaRecorder.AudioSource.MIC, bufBytes);
        }
        if (rec == null) {
            listener.onError("Microphone could not be opened");
            return false;
        }
        record = rec;
        record.startRecording();
        running = true;
        thread = new Thread(() -> loop(listener), "capture");
        thread.start();
        return true;
    }

    @SuppressLint("MissingPermission")
    private static AudioRecord build(int source, int bufBytes) {
        AudioRecord r = new AudioRecord(source, (int) Analyzer.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufBytes);
        if (r.getState() != AudioRecord.STATE_INITIALIZED) {
            r.release();
            return null;
        }
        return r;
    }

    private void loop(Listener listener) {
        short[] pcm = new short[Analyzer.BLOCK];
        double[] block = new double[Analyzer.BLOCK];
        while (running) {
            int n = record.read(pcm, 0, Analyzer.BLOCK);
            if (n < 0) {
                running = false;
                listener.onError("Microphone read failed (" + n + ")");
                break;
            }
            if (n == Analyzer.BLOCK) {
                for (int i = 0; i < n; i++) block[i] = pcm[i] / 32768.0;
                listener.onBlock(block);
            }
        }
    }

    /** Stops the thread and releases the microphone. Safe to call twice. */
    public synchronized void stop() {
        running = false;
        if (thread != null) {
            try {
                thread.join(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            thread = null;
        }
        if (record != null) {
            try {
                record.stop();
            } catch (IllegalStateException ignored) {
                // already stopped
            }
            record.release();
            record = null;
        }
    }

    public boolean isRunning() { return running; }
}

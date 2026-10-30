package com.replica.audiotool.ui;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.replica.audiotool.audio.Capture;
import com.replica.audiotool.dsp.Analyzer;
import com.replica.audiotool.dsp.Weighting;

/** The single screen of v1: live SPL, Leq, band spectrum, weighting, octave width, calibration. */
public final class MainActivity extends AppCompatActivity {

    private static final String PREFS = "audiotool";
    private static final String KEY_OFFSET = "offset_db";
    private static final Weighting.Mode[] WEIGHTINGS = {Weighting.Mode.FLAT, Weighting.Mode.A, Weighting.Mode.C};
    private static final int[] OCTAVES = {1, 3, 6, 12};

    private Analyzer analyzer;
    private final Capture capture = new Capture();
    private SharedPreferences prefs;

    private TextView splText;
    private TextView leqText;
    private SpectrumView spectrum;
    private Button startStop;
    private Button weightingButton;
    private Button octaveButton;
    private int weightingIndex = 1; // A
    private int octaveIndex = 1;    // 1/3

    private final ActivityResultLauncher<String> micPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startCapture();
                else toast("Microphone permission is needed to measure");
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        double offset = Double.longBitsToDouble(prefs.getLong(KEY_OFFSET, Double.doubleToLongBits(0.0)));
        analyzer = new Analyzer(WEIGHTINGS[weightingIndex], offset);
        analyzer.setPerOctave(OCTAVES[octaveIndex]);
        setContentView(buildLayout());
        updateButtons();
    }

    private LinearLayout buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);

        splText = new TextView(this);
        splText.setTextSize(44f);
        splText.setGravity(Gravity.CENTER);
        splText.setText("-- dB SPL");
        root.addView(splText, wrap());

        leqText = new TextView(this);
        leqText.setTextSize(16f);
        leqText.setGravity(Gravity.CENTER);
        leqText.setText("Leq --");
        root.addView(leqText, wrap());

        spectrum = new SpectrumView(this, null);
        root.addView(spectrum, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout row1 = row();
        startStop = button("Start", v -> toggleCapture());
        weightingButton = button("", v -> cycleWeighting());
        octaveButton = button("", v -> cycleOctave());
        row1.addView(startStop, weightLp());
        row1.addView(weightingButton, weightLp());
        row1.addView(octaveButton, weightLp());
        root.addView(row1, wrap());

        LinearLayout row2 = row();
        row2.addView(button("Reset", v -> analyzer.resetStatistics()), weightLp());
        row2.addView(button("Calibrate", v -> showCalibrate()), weightLp());
        root.addView(row2, wrap());

        return root;
    }

    private void toggleCapture() {
        if (capture.isRunning()) {
            capture.stop();
            startStop.setText("Start");
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startCapture();
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startCapture() {
        boolean ok = capture.start(new Capture.Listener() {
            @Override
            public void onBlock(double[] samples) {
                Analyzer.Frame f = analyzer.process(samples);
                runOnUiThread(() -> show(f));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    toast(message);
                    startStop.setText("Start");
                });
            }
        });
        if (ok) startStop.setText("Stop");
    }

    private void show(Analyzer.Frame f) {
        splText.setText(Double.isFinite(f.splDb) && f.splDb > -150
                ? String.format("%.1f dB SPL", f.splDb) : "-- dB SPL");
        leqText.setText(Double.isFinite(f.leqDb)
                ? String.format("Leq %.1f dB", f.leqDb) : "Leq --");
        spectrum.setFrame(f.bandDb, f.peakDb, f.valleyDb, f.centres);
    }

    private void cycleWeighting() {
        weightingIndex = (weightingIndex + 1) % WEIGHTINGS.length;
        analyzer.setWeighting(WEIGHTINGS[weightingIndex]);
        updateButtons();
    }

    private void cycleOctave() {
        octaveIndex = (octaveIndex + 1) % OCTAVES.length;
        analyzer.setPerOctave(OCTAVES[octaveIndex]);
        updateButtons();
    }

    private void updateButtons() {
        weightingButton.setText("Weight " + WEIGHTINGS[weightingIndex].name());
        octaveButton.setText("1/" + OCTAVES[octaveIndex] + " oct");
    }

    private void showCalibrate() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED);
        input.setText(String.valueOf(analyzer.getOffsetDb()));
        new AlertDialog.Builder(this)
                .setTitle("Calibration offset (dB)")
                .setMessage("Set this so the reading matches a reference meter. Accuracy depends on the phone's microphone.")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    try {
                        double v = Double.parseDouble(input.getText().toString().trim());
                        analyzer.setOffsetDb(v);
                        prefs.edit().putLong(KEY_OFFSET, Double.doubleToLongBits(v)).apply();
                    } catch (NumberFormatException e) {
                        toast("Not a number");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        capture.stop();
        startStop.setText("Start");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        capture.stop();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private Button button(String text, android.view.View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setOnClickListener(l);
        return b;
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        return r;
    }

    private static LinearLayout.LayoutParams match() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static LinearLayout.LayoutParams weightLp() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }
}

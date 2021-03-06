package com.example.tfliteperformance;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ProgressBar;

import org.tensorflow.lite.examples.classification.tflite.Classifier;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Model;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Device;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        AdapterView.OnItemSelectedListener {

    private ImageView plusImageView, minusImageView;
    private Spinner modelSpinner;
    private Spinner deviceSpinner;
    private TextView threadsTextView;

    protected TextView progressBarValue,
            inferenceTimeTextView,
            totalTimeTextView,
            accuracyTextView,
            testDataTextView;
    protected ProgressBar progressBar;

    private Model model = Model.QUANTIZED_EFFICIENTNET;
    private Device device = Device.CPU;
    private int numThreads = -1;

    private Activity act;
    public Activity getCurrentActivity(){
        return act;
    }

    private static final List<String> INPUTS = new ArrayList<String>();
    private static final List<String> LABELS = new ArrayList<String>();

    boolean is_running = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        act = this;

        // Initialize the views and progress bar from the UI
        threadsTextView = findViewById(R.id.threads);
        plusImageView = findViewById(R.id.plus);
        minusImageView = findViewById(R.id.minus);
        modelSpinner = findViewById(R.id.model_spinner);
        deviceSpinner = findViewById(R.id.device_spinner);

        progressBar = findViewById(R.id.progress_bar);
        progressBarValue = findViewById(R.id.progress_value);
        inferenceTimeTextView = findViewById(R.id.inference_time_value);
        totalTimeTextView = findViewById(R.id.total_time_value);
        accuracyTextView = findViewById(R.id.accuracy_value);
        testDataTextView = findViewById(R.id.test_data_value);

        // Initialize Listeners
        modelSpinner.setOnItemSelectedListener(this);
        deviceSpinner.setOnItemSelectedListener(this);

        plusImageView.setOnClickListener(this);
        minusImageView.setOnClickListener(this);

        // Initialize the configuration
        model = Model.valueOf(modelSpinner.getSelectedItem().toString().toUpperCase());
        device = Device.valueOf(deviceSpinner.getSelectedItem().toString());
        numThreads = Integer.parseInt(threadsTextView.getText().toString().trim());

        final Button bmark_btn = findViewById(R.id.benchmark_btn);

        bmark_btn.setOnClickListener(v -> {
            if (!is_running) {
                new benchmarker().execute("");
                is_running = true;
            } else {
                Log.d("ButtonClick", "Benchmarker already running.");
            }
        });
    }

    @SuppressLint("StaticFieldLeak")
    private class benchmarker extends AsyncTask<String, Double, Double[]> {
        @Override
        protected Double[] doInBackground(String... acts) {
            File dataset_folder = new File("/data/local/tmp/DataSet/");
            if (dataset_folder.exists()) {
                Log.d("Data Load", "Dataset folder exists.");
                File[] label_folders = dataset_folder.listFiles();
                for (File label_folder: label_folders) {
                    String label = label_folder.getName();

                    File[] input_files = label_folder.listFiles();
                    for (File input_file: input_files) {
                        INPUTS.add(dataset_folder + "/" + label + "/" + input_file.getName());
                        LABELS.add(label);
                    }
                }
            } else {
                Log.d("Data Load", "Dataset folder exists.");
            }

            // Metrics
            int correct = 0;
            int n_samples = LABELS.size();
            int epochs =  1;
            // List<Long> inference_timings = new ArrayList<>();
            double accuracy = 0.0, avg_inference_time = 0.0, total_time = 0.0;

            Log.d("Benchmark", "Loaded test data");

            Classifier classifier = null;
            try {
                classifier = Classifier.create(getCurrentActivity(),
                        getModel(),
                        getDevice(),
                        getNumThreads());
            } catch (IOException e) {
                e.printStackTrace();
            }

            setProgressBarRange(n_samples);

            for (int r = 0; r < epochs; r++ ) {
                for (int i = 0; i < n_samples; i++) {
                    Log.d("Benchmark", "Iteration: " + i);

                    String imageFileName = INPUTS.get(i);
                    String label = LABELS.get(i).toLowerCase();

                    Log.d("Benchmark", "Image File Name: " + imageFileName);
                    Log.d("Benchmark", "Label: " + label);

                    Bitmap input = null;
                    try {
                        input = loadImage(imageFileName);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    Classifier.Result result = classifier.recognizeImage(input, 0);

                    String pred = result.recognitions.get(0).getTitle().toLowerCase();
                    Log.d("Benchmark", "Prediction: " + pred);
                    Log.d("Benchmark", "Confidence: " +
                                    result.recognitions.get(0).getConfidence().toString());

                    if (pred.contains(label) || label.contains(pred)) {
                        correct++;
                    }

                    accuracy = (double)correct * 100 / (i + 1);

                    total_time += result.runtime;
                    avg_inference_time = total_time / i + 1;

                    publishProgress((double) i+1,
                            (double) n_samples,
                            accuracy,
                            avg_inference_time,
                            total_time);
                }
            }

            Log.d("Results", "Accuracy = " + accuracy + "%");
            Log.d("Results", "Avg Inference Time = " + avg_inference_time + "%");

            return new Double[]{avg_inference_time, total_time, accuracy};
        }

        @SuppressLint("DefaultLocale")
        protected void onProgressUpdate(Double... progress) {
            setProgressBar(progress[0].intValue());
            setProgressBarValue(progress[0].intValue() + "/" + progress[1].intValue());
            setAccuracy(format("%.2f", progress[2]) + "%");
            setInferenceTime(formatTime(progress[3]));
            setTotalTime(formatTime(progress[4]));
        }

        protected void onPostExecute(Double[] results) {
            Log.d("Async", "Finished.");
            is_running = false;
            saveHistory(results[0], results[1], results[2]);
            INPUTS.clear();
            LABELS.clear();
        }
    }

    @SuppressLint("DefaultLocale")
    private String formatTime(double time_) {
        int time_mins = 0, time_seconds = 0, time_ms = 0;
        if (time_ > 1000) {
            time_seconds = ((int) Math.floor(time_ / 1000));
            time_ms = (int) (time_ % 1000);
            if (time_seconds > 60) {
                time_mins = ((int) Math.floor(time_seconds / 60));
                time_seconds = time_seconds % 60;
                return format("%dmin %2ds %3dms",
                        time_mins,
                        time_seconds,
                        time_ms);
            }
            else {
                return format("%ds %3dms", time_seconds, time_ms);
            }
        }
        else {
            return format("%.2fms", time_);
        }
    }

    @SuppressLint("ResourceAsColor")
    private TextView createTextView(String text, int layout_column){
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setBackground(ContextCompat.getDrawable(this, R.drawable.border));
        tv.setPadding(5,10,3,3);
        tv.setLayoutParams(new TableRow.LayoutParams(layout_column));
        tv.setTextColor(R.color.black);
        return tv;
    }

    @SuppressLint("DefaultLocale")
    private void saveHistory(double avg_inf_time, double total_time, double accuracy) {

        TableLayout tl = findViewById(R.id.history);
        TableRow tr = new TableRow(this);
        tr.setBackground(ContextCompat.getDrawable(this, R.drawable.border));

        String model_ = model.name();
        model_ = model_.substring(0, 1).toUpperCase() + model_.substring(1).toLowerCase();
        TextView tv_model = createTextView(model_, 0);
        TextView tv_device = createTextView(device.name(), 1);
        TextView tv_num_threads = createTextView(device == Device.CPU ? numThreads + "" : "N/A",
                2);
        TextView tv_avg_inf_time = createTextView(format("%.2f", avg_inf_time) + "ms",
                3);
        TextView tv_total_time = createTextView(formatTime(total_time), 4);
        TextView tv_accuracy = createTextView(format("%.2f", accuracy) + "%",
                5);

        tr.addView(tv_model);
        tr.addView(tv_device);
        tr.addView(tv_num_threads);
        tr.addView(tv_avg_inf_time);
        tr.addView(tv_total_time);
        tr.addView(tv_accuracy);

        tl.addView(tr);
    }

    private Bitmap loadImage(String imageFileName) throws FileNotFoundException {
        File file = new File(imageFileName);
        FileInputStream inputStream = new FileInputStream(file);
        return BitmapFactory.decodeStream(inputStream);
    }

    protected void setProgressBarRange(Integer max) {
        progressBar.setMin(0);
        progressBar.setMax(max);
    }

    protected void setProgressBar(Integer progress) {
        progressBar.setProgress(progress);
    }

    protected void setProgressBarValue(String value) {
        progressBarValue.setText(value);
    }

    protected void setInferenceTime(String inferenceTime) {
        inferenceTimeTextView.setText(inferenceTime);
    }

    protected void setTotalTime(String totalTime) {
        totalTimeTextView.setText(totalTime);
    }

    protected void setAccuracy(String accuracy) {
        accuracyTextView.setText(accuracy);
    }

    protected void setTestData(String testData) {
        testDataTextView.setText(testData);
    }

    protected String getTestData() {
        return testDataTextView.getText().toString();
    }

    protected Model getModel() {
        return model;
    }

    private void setModel(Model model) {
        if (this.model != model) {
            Log.d("setModel","Updating  model: " + model);
            this.model = model;
        }
    }

    protected Device getDevice() {
        return device;
    }

    private void setDevice(Device device) {
        if (this.device != device) {
            Log.d("setDevice","Updating  device: " + device);
            this.device = device;
            final boolean threadsEnabled = device == Device.CPU;
            plusImageView.setEnabled(threadsEnabled);
            minusImageView.setEnabled(threadsEnabled);
            threadsTextView.setText(threadsEnabled ? String.valueOf(numThreads) : "N/A");
        }
    }

    protected int getNumThreads() {
        return numThreads;
    }

    private void setNumThreads(int numThreads) {
        if (this.numThreads != numThreads) {
            Log.d("setNumThreads","Updating  numThreads: " + numThreads);
            this.numThreads = numThreads;
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.plus) {
            String threads = threadsTextView.getText().toString().trim();
            int numThreads = Integer.parseInt(threads);
            if (numThreads >= 9) return;
            setNumThreads(++numThreads);
            threadsTextView.setText(String.valueOf(numThreads));
        } else if (v.getId() == R.id.minus) {
            String threads = threadsTextView.getText().toString().trim();
            int numThreads = Integer.parseInt(threads);
            if (numThreads == 1) {
                return;
            }
            setNumThreads(--numThreads);
            threadsTextView.setText(String.valueOf(numThreads));
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        if (parent == modelSpinner) {
            setModel(Model.valueOf(parent.getItemAtPosition(pos).toString().toUpperCase()));
        } else if (parent == deviceSpinner) {
            setDevice(Device.valueOf(parent.getItemAtPosition(pos).toString()));
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing.
    }
}
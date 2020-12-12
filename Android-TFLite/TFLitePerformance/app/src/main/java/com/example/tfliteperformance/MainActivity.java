package com.example.tfliteperformance;

import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ProgressBar;

import org.tensorflow.lite.examples.classification.tflite.Classifier;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Model;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Device;
import org.w3c.dom.Text;

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

    private static List<String> INPUTS = new ArrayList<String>();
    private static List<String> LABELS = new ArrayList<String>();

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
                Log.i("ButtonClick", "Benchmarker already running.");
            }
        });
    }

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
            int correct = 0, incorrect = 0;
            int n_samples = LABELS.size();
            int epochs =  1;
            List<Long> inference_timings = new ArrayList<Long>();
            double accuracy = 0.0, avg_inference_time = 0.0, total_time = 0.0;

            Log.i("Benchmark", "Loaded test data");

            Classifier classifier = null;
            try {
                classifier = Classifier.create(getCurrentActivity(),
                        getModel(),
                        getDevice(),
                        getNumThreads());
            } catch (IOException e) {
                e.printStackTrace();
            }

            setProgressBarRange(0, n_samples);

            for (int r = 0; r < epochs; r++ ) {
                for (int i = 0; i < n_samples; i++) {
                    Log.i("Benchmark", "Iteration: " + i);

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
                    } else {
                        incorrect++;
                    }

                    accuracy = (double)correct * 100 / i + 1;

                    inference_timings.add(result.runtime);
                    total_time += result.runtime;
                    avg_inference_time = total_time / i + 1;

                    publishProgress((double) i+1,
                            (double) n_samples,
                            accuracy,
                            avg_inference_time,
                            total_time);
                }
            }

            Log.i("Results", "Accuracy = " + accuracy + "%");
            Log.i("Results", "Avg Inference Time = " + avg_inference_time + "%");

            Double results[] = {avg_inference_time, total_time, accuracy};
            return results;
        }

        @SuppressLint("DefaultLocale")
        protected void onProgressUpdate(Double... progress) {
            setProgressBar(progress[0].intValue());
            setProgressBarValue(progress[0].intValue() + "/" + progress[1].intValue());
            setAccuracy(format("%.2f", progress[2]) + "%");
            setInferenceTime(format("%.2f", progress[3]) + "ms");
            setTotalTime(formatTime(progress[4]));
        }

        protected void onPostExecute(Double[] results) {
            Log.i("Async", "Finished.");
            is_running = false;
            saveHistory(results[0], results[1], results[2]);
        }
    }

    @SuppressLint("DefaultLocale")
    private String formatTime(double time_) {
        int total_time_mins = 0, total_time_seconds = 0, total_time_ms = 0;
        if (time_ > 1000) {
            total_time_seconds = ((int) Math.floor(time_ / 1000));
            total_time_ms = (int) (time_ % 1000);
            if (total_time_seconds > 60) {
                total_time_mins = ((int) Math.floor(total_time_seconds / 60));
                total_time_seconds = total_time_seconds % 60;
                return format("%dmins %2ds %3dms",
                        total_time_mins,
                        total_time_seconds,
                        total_time_ms);
            }
            else {
                return format("%ds %3dms", total_time_seconds, total_time_ms);
            }
        }
        else {
            return format("%.2fms", time_);
        }
    }

    private TextView createTextView(String text, int layout_column){
        TextView tv = new TextView(this);
        tv.setText("Model");
        tv.setBackground(Drawable.createFromPath("@drawable/border"));
        tv.setPadding(3,3,3,3);
        tv.setLayoutParams(new TableRow.LayoutParams(layout_column));

        return tv;
    }

    private void saveHistory(double avg_inf_time, double total_time, double accuracy) {

        TableLayout tl = findViewById(R.id.history);
        TableRow tr = new TableRow(this);
        // tr.setLayoutParams(new TableRow.LayoutParams());
        tr.setBackground(Drawable.createFromPath("@drawable/border"));

        TextView tv_model = createTextView(model.name(), 0);
        TextView tv_device = createTextView(device.name(), 1);
        TextView tv_num_threads = createTextView(numThreads + "", 2);
        TextView tv_avg_inf_time = createTextView(avg_inf_time + "ms", 3);
        TextView tv_total_time = createTextView(formatTime(total_time), 4);
        @SuppressLint
                ("DefaultLocale") TextView tv_accuracy =
                createTextView(format("%.2f", accuracy) + "%", 5);

        tr.addView(tv_model);
        tr.addView(tv_device);
        tr.addView(tv_num_threads);
        tr.addView(tv_avg_inf_time);
        tr.addView(tv_total_time);
        tr.addView(tv_accuracy);

        tl.addView(tr);

        /* Add row to TableLayout. */
        //tr.setBackgroundResource(R.drawable.sf_gradient_03);
        // tl.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
    }

    private Bitmap loadImage(String imageFileName) throws FileNotFoundException {
        File file = new File(imageFileName);
        FileInputStream inputStream = new FileInputStream(file);
        return BitmapFactory.decodeStream(inputStream);
    }

    protected void setProgressBarRange(Integer min, Integer max) {
        progressBar.setMin(min);
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
# Low Latency Deep Learning on Smartphones
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

In this project, we empirically evaluate the performance of two mobile DL frameworks TensorFlow Lite and CoreML , on the inference performance on Android and iOS devices, respectively, using various Convolution Neural Network Architectures.

## Frameworks
1. [TensorFlow Lite](https://www.tensorflow.org/lite) (for Android)
2. [CoreML](https://developer.apple.com/documentation/coreml) (for iOS)

## On-board devices and API Support
1. CPU:
    - [XNNPack Optimized](https://blog.tensorflow.org/2020/07/accelerating-tensorflow-lite-xnnpack-integration.html) [TF Lite, Android]
2. GPU
3. Apple Neural Network (ANE) [CoreML, iOS]
4. [Neural Network API](https://www.tensorflow.org/lite/performance/nnapi) (NNAPI) [TF Lite, Android]

## Usage
### TFLite Performance
1. In Android Studio, use the *Open an Existing Project* option and select the `Android-TFLite\TFLitePerformance` folder.
2. If you wish to use a custom TF-Lite model, copy the `.tflite` file to the `app\assets` folder. Also, update the path in the model's classifier java file (like *ClassifierSqueezeNet.java*), in the lib_support library. The path is present in the `getModelPath` function.
3. Use the **Device File Explorer** to upload the test data onto the device. Default location of data is `/data/local/tmp/DataSet/`. This can be modified by updating the following line of code at *line 114* in the *MainActivity.java*.
   ```Java
   File dataset_folder = new File(<Dataset location on device>);
   ```
4. Build the project using `Build > Make Project`.
5. Run the application on device/emulator using `Run > Run 'app'`.
6. If you wish to start profiling with application launch, use `Run > Profile 'app'` instead.
7. On Device/Emulator, ensure the correct model and device is selected. 
8. Click the `Benchmark` button on the mobile application to initiate the inferencing.


## Benchmark Metrics
### CPU
- #### TensorFlow Lite
    These metrics were recorded using Android Studio CPU Profiler.
    ##### MobileNet V2 on CPU (no delegates)
    ###### 4 Threads
    ![MobileNet V2, CPU](/Evaluation_Results/TF_Lite_Metrics/CPU/MobileNetV2_CPU_4.png)
    
    ###### 8 Threads
    ![MobileNet V2, CPU](/Evaluation_Results/TF_Lite_Metrics/CPU/MobileNetV2_CPU_8.png)

    ##### MobileNet V2 using GPU delegate
    ![MobileNet V2, CPU](/Evaluation_Results/TF_Lite_Metrics/CPU/MobileNetV2_GPU.png)
    
    ##### MobileNet V2 using NNAPI delegate
    ![MobileNet V2, CPU](/Evaluation_Results/TF_Lite_Metrics/CPU/MobileNetV2_NNAPI.png)
    
### GPU
- #### TensorFlow Lite
    These metrics were recorded using Qualcomm Snapdragon Profiler.

    ##### MobileNet V2 on CPU (no delegates)
    ![MobileNet V2, CPU](/Evaluation_Results/TF_Lite_Metrics/GPU/MobileNetV2_CPU_4.png)
    
    ##### MobileNet V2 using GPU delegate
    ![MobileNet V2, CPU](/Evaluation_Results/TF_Lite_Metrics/GPU/MobileNetV2_GPU.png)
    
    ##### MobileNet V2 using NNAPI delegate
    ![MobileNet V2, CPU](/Evaluation_Results/TF_Lite_Metrics/GPU/MobileNetV2_NNAPI.png)


## Evaluation Results
| **Size vs Accuracy** | **Inference Time vs Accuracy** |
|------------------|----------------------------|
|![Size vs Accuracy](/Evaluation_Results/Framework_Evaluation/Size_vs_Accuracy.png)| ![Inference Time vs Accuracy](/Evaluation_Results/Framework_Evaluation/InferenceTime_vs_Accuracy.png)   |

## Environments
1. [Xcode](https://developer.apple.com/xcode/ide/)
2. [Android Studio](https://developer.android.com/studio)
3. [Qualcomm Snapdragon Profiler](https://developer.qualcomm.com/software/snapdragon-profiler)

# Low Latency Deep Learning on Smartphones
In this project, we empirically evaluate the performance of two mobile DL frameworks TensorFlow Lite and CoreML , on the inference performance on Android and iOS devices, respectively, using various Convolution Neural Network Architectures.

## Frameworks
1. [TensorFlow Lite](https://www.tensorflow.org/lite) (for Android)
2. [CoreML](https://developer.apple.com/documentation/coreml) (for iOS)

## On-board devices and API Support:
1. CPU:
    - [XNNPack Optimized](https://blog.tensorflow.org/2020/07/accelerating-tensorflow-lite-xnnpack-integration.html) [TF Lite, Android]
2. GPU
3. Apple Neural Network (ANE) [CoreML, iOS]
4. [Neural Network API](https://www.tensorflow.org/lite/performance/nnapi) (NNAPI) [TF Lite, Android]

## Benchmark Metrics
### CPU
- #### TensorFlow Lite
    These metrics were recorded using Android Studio CPU Profiler.
    ##### MobileNet V2 on CPU (no delegates)
    ###### 4 Threads
    ![MobileNet V2, CPU](/Evaluation_Results/TF_Lite_Metrics/CPU/MobileNetV2_CPU_4.png)
    
    ###### 8 Threads
    ![MobileNet V2, CPU](/Evaluation_Results/TF_Lite_Metrics/CPU/MobileNetV2_CPU_8.png)|

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

This Folder Contains All the code, required for running experiments related to CoreML.

# Coreml-Performance
CoreML performance, is used to measure the inference time, throughput and accuracy on the test data.
Models Used For Comparison
- MobileNetV2 (Size: 24.7MB)
- MobileNetV2 (Size: 12.4MB) Model weights using half-precision (16 bit) floating point numbers.
- MobileNetV2 (Size 6.3 MB) Model optimized using 8 bit quantization with KMeans.
- Resnet50 (Size 102.6 MB) - Model weights using full precision (32 bit) floating point numbers.
- Resnet50 (16) - Model weights using half-precision (16 bit) floating point numbers.
- Resnet50 (18) (Size 25.8 MB) Model optimized using 8 bit quantization with KMeans.
- SqueezeNet (5MB) - Model weights using full precision (32 bit) floating point numbers.
- SqueezeNet (16) (2.5MB) - Model weights using half-precision (16 bit) floating point numbers.
- SqueezeNet-8 (1.3MB) - - Model weights using half-precision (8 bit) floating point numbers.

## BenchMarking
- There is a warmup for 50 iterations, and then the model inference time is calculated and averaged for 1000 iterations.
- For comparing the accuracy, model is tested on 1000 images randomnly selected from 10 different classes from [Kaggle Animal 10](!https://www.kaggle.com/alessiocorrado99/animals10) Datatset.
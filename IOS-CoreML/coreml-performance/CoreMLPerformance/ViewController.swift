//
//  ViewController.swift
//  CoreMLPerformance
//
//  Created by Mohit Nihalani
//

import UIKit
import Vision


class ViewController: UIViewController {

    func url(forResource fileName: String, withExtension ext: String) -> URL {
        let bundle = Bundle(for: ViewController.self)
        return bundle.url(forResource: fileName, withExtension: ext)!
    }

    func testPerformace(modelName: String, device: String, numIter: Int = 100) throws -> Double {
        
        let config = MLModelConfiguration()
        if device.lowercased() == "ane" {
            config.computeUnits = .all
        } else if device.lowercased() == "gpu" {
            config.computeUnits = .cpuAndGPU
        } else {
            config.computeUnits = .cpuOnly
        }

        let model = try Predictor(contentsOf: modelName, configuration: config)
        
        let imageFeatureValue = try MLFeatureValue(
            imageAt: url(forResource: "test01", withExtension: "jpg"),
            constraint: model.model.modelDescription.inputDescriptionsByName["image"]!.imageConstraint!,
            options: [.cropAndScale: VNImageCropAndScaleOption.centerCrop.rawValue]
        )

        let input = try MLDictionaryFeatureProvider(
            dictionary: ["image": imageFeatureValue.imageBufferValue!]
        )

        let startTime = CACurrentMediaTime()
        for _ in 0..<numIter {
            _ = try! model.prediction(input: input)
        }
        let endTime = CACurrentMediaTime()

        return (endTime - startTime) / Double(numIter)
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        
        let fm = FileManager.default
        let path = Bundle.main.resourcePath!
        print(path)
        do {
            let items = try fm.contentsOfDirectory(atPath: path)

            for item in items {
                print("Found \(item)")
            }
        } catch {
            // failed to read directory – bad permissions, perhaps?
        }
        for modelName in [
             "Resnet50Int8LUT"
            ] {
            for device in ["ANE", "GPU", "CPU"] {
                // warmup
                _ = try! testPerformace(
                    modelName: modelName,
                    device: device,
                    numIter: 50
                )
                // real test
                let latency = try! testPerformace(
                    modelName: modelName,
                    device: device,
                    numIter: 1000
                )
                print(modelName)
                print("Latency \(device): \(latency)")
                print("RPS \(device): \(1 / latency)")
                print()
            }
        }
    }

}

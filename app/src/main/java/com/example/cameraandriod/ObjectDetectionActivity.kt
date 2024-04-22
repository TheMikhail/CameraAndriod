package com.example.cameraandriod

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

class ObjectDetectionActivity: AppCompatActivity(){
    fun useDefaultObjectDetector() {
        val localModel =
            LocalModel.Builder()
                .setAssetFilePath("asset_file_path_to_tflite_model")
                .build()
        val options =
            CustomObjectDetectorOptions.Builder(localModel)
                .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .setClassificationConfidenceThreshold(0.5f)
                .setMaxPerObjectLabelCount(3)
                .build()
        // [END create_custom_options]
         val image = InputImage.fromBitmap(
            Bitmap.createBitmap(IntArray(100 * 100), 100, 100, Bitmap.Config.ARGB_8888),
            0)
        val objectDetector = ObjectDetection.getClient(options)
        objectDetector.process(image)
            .addOnSuccessListener { results ->
                for (result in results) {
                    val bounds = result.boundingBox
                    val label = result.labels.firstOrNull()?.text ?: "Unknown"
                }
            }
            .addOnFailureListener { e ->
                // Task failed with an exception
                // ...
            }
        val results = listOf<DetectedObject>()
        // [START read_results_custom]
        for (detectedObject in results) {
            val boundingBox = detectedObject.boundingBox
            val trackingId = detectedObject.trackingId
            for (label in detectedObject.labels) {
                val text = label.text
                val index = label.index
                val confidence = label.confidence
            }
        }
    }
}




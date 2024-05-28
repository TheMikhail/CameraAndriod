package com.example.cameraandriod

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.TaskExecutors
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import java.nio.ByteBuffer

@ExperimentalGetImage
    class DetectedObjectProcessor {

        val localModel = LocalModel.Builder()
            .setAssetFilePath("V3.tflite")
            .build()
        private val detector: ObjectDetector

        private val executor = TaskExecutors.MAIN_THREAD

        init {
            val customObjectDetectorOptions = CustomObjectDetectorOptions.Builder(localModel)
                .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .setClassificationConfidenceThreshold(0.5f)
                .build()

            detector = ObjectDetection.getClient(customObjectDetectorOptions)
        }
            private var reuseBuffer: ByteBuffer? = null

        fun stop(){
            detector.close()
        }
        @SuppressLint("UnsafeExperimentalUsageError")
        fun processImageProxy(image: ImageProxy, onDetectionFinished: (List<DetectedObject>) -> Unit){
            image.image ?: return
            detector.process(InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees))
                .addOnSuccessListener(executor) { labels ->
                    onDetectionFinished(labels)
                    Log.e("CameraMisha", "Все окей, ObjectDetectorProcessor работает")
                    /*for (label in labels){
                        for (lab in label.labels ){
                        val text = lab.text
                        val confidence = lab.confidence
                        val index = lab.index
                            Log.d("DetectObj", text)
                            Log.d("DetectObj", confidence.toString())
                            Log.d("DetectObj", index.toString())
                        }
                    }*/

                }
                .addOnFailureListener(executor) { e: Exception ->
                    Log.e("Camera", "Error detecting face", e)
                }
                .addOnCompleteListener { image.close() }
        }
}

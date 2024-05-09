package com.example.cameraandriod

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.TaskExecutors
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
    @ExperimentalGetImage
    class DetectedObjectProcessor {

        private val detector: ObjectDetector

        private val executor = TaskExecutors.MAIN_THREAD

        init {
            val objectDetectorOptions = ObjectDetectorOptions.Builder()
                .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build()
            detector = ObjectDetection.getClient(objectDetectorOptions)
        }

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
                    //debugPrint(labels)
                }
                .addOnFailureListener(executor) { e: Exception ->
                    Log.e("Camera", "Error detecting face", e)
                }
                .addOnCompleteListener { image.close() }
        }
}

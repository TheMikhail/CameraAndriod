package com.example.cameraandriod

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.TaskExecutors
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

@ExperimentalGetImage class PoseDetectorProcessor {
    private val detector: PoseDetector

    private val executor = TaskExecutors.MAIN_THREAD

    init {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        detector = PoseDetection.getClient(options)
    }

    fun stop() {
        detector.close()
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    fun processImageProxy(image: ImageProxy, onDetectionFinished: (Pose) -> Unit) {
        detector.process(InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees))
            .addOnSuccessListener(executor) { results: Pose -> onDetectionFinished(results)
                Log.e("CameraMisha", "Все окей, PosedetectorClass работает")}
            .addOnFailureListener(executor) { e: Exception ->
                Log.e("CameraMisha", "Error detecting pose", e)
            }
            .addOnCompleteListener { image.close() }
    }
}
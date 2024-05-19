package com.example.cameraandriod

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.TaskExecutors
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceDetectorProcessor {
    private val detector: FaceDetector

    private val executor = TaskExecutors.MAIN_THREAD

    init {
        val faceDetectorOptions = FaceDetectorOptions.Builder()
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setMinFaceSize(0.2f)
            .enableTracking()
            .build()

        detector = FaceDetection.getClient(faceDetectorOptions)
    }

    fun stop() {
        detector.close()
    }

    @ExperimentalGetImage
    @SuppressLint("UnsafeExperimentalUsageError")
    fun processImageProxy(image: ImageProxy, onDetectionFinished: (List<Face>) -> Unit) {
        detector.process(InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees))
            .addOnSuccessListener(executor) { results: List<Face> -> onDetectionFinished(results) }
            .addOnFailureListener(executor) { e: Exception ->
                Log.e("Camera", "Error detecting face", e)
            }
            .addOnCompleteListener { image.close() }
    }
}
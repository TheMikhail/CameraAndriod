package com.example.cameraandriod

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.TaskExecutors
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.IOException

class LabelObjectProcessor {

    private val labeler : ImageLabeler
    private val executor = TaskExecutors.MAIN_THREAD
    init {
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.8f)
            .build()
        labeler = ImageLabeling.getClient(options)
    }
    fun stop() {
        try {
            labeler.close()
        } catch (e: IOException) {
            Log.e(
                TAG,
                "Exception thrown while trying to close ImageLabelerClient: $e"
            )
        }
    }
    @ExperimentalGetImage
    @SuppressLint("UnsafeExperimentalUsageError")
    fun processImageProxy(image: ImageProxy, onDetectionFinished: (List<ImageLabel>) -> Unit){
        image.image ?: return
        labeler.process(InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees))
            .addOnSuccessListener(executor) { labels ->
                onDetectionFinished(labels)
                Log.e("CameraMisha", "Все окей, LabelObjectProcessor работает")
                for (label in labels){
                    val text = label.text
                    val confidence = label.confidence
                    val index = label.index
                    Log.d("CameraMishaLabel", text)
                    Log.d("CameraMishaLabel", confidence.toString())
                    Log.d("CameraMishaLabel", index.toString())

                }
            }
            .addOnFailureListener(executor) { e: Exception ->
                Log.e("Camera", "Error detecting label", e)
            }
            .addOnCompleteListener { image.close() }
    }
}
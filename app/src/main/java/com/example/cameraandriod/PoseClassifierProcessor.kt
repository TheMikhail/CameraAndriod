package com.example.cameraandriod

import android.nfc.Tag
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.TaskExecutors
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.lang.Math.atan2

class PoseClassifierProcessor {
    private val detector: PoseDetector
    private val executor = TaskExecutors.MAIN_THREAD
    init {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        detector = PoseDetection.getClient(options)
    }
    @OptIn(ExperimentalGetImage::class)
    fun poseAnalyzer(image: ImageProxy, pose: (Pose) -> Unit){
        detector.process(InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees))
            .addOnSuccessListener(executor){result: Pose ->
                val angle = getAngle(result.getPoseLandmark(PoseLandmark.LEFT_HIP),
                    result.getPoseLandmark(PoseLandmark.LEFT_KNEE),
                    result.getPoseLandmark(PoseLandmark.LEFT_ANKLE))
                if (angle<90){
                    Log.d("CameraMishaPoseDetector", "Человевек сел!")
                }
                pose(result)
            }.addOnFailureListener(executor) { e: Exception ->
                Log.e("CameraMisha", "Ошибка, в функции poseAnalyzer!", e)
            }
            .addOnCompleteListener { image.close() }
    }
    private fun getAngle(firstPoint: PoseLandmark?, midPoint: PoseLandmark?, lastPoint: PoseLandmark?): Double {
        var result = Math.toDegrees(atan2(lastPoint!!.getPosition().y.toDouble() - midPoint!!.getPosition().y.toDouble(),
            lastPoint.getPosition().x.toDouble() - midPoint.getPosition().x.toDouble())
                - atan2(firstPoint!!.getPosition().y.toDouble() - midPoint.getPosition().y.toDouble(),
            firstPoint.getPosition().x.toDouble() - midPoint.getPosition().x.toDouble()))
        result = Math.abs(result) // Angle should never be negative
        if (result > 180) {
            result = 360.0 - result // Always get the acute representation of the angle
        }
        return result
    }
}
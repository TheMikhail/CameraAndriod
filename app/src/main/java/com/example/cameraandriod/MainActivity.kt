package com.example.cameraandriod;
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.cameraandriod.ui.theme.CameraAndriodTheme
import com.example.cameraandriod.PoseDetectorProcessor
import com.google.android.gms.tasks.TaskExecutors
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

@ExperimentalGetImage class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (permissionGranted()) {
            initView()
        } else {
            requestPermission()
        }
    }

    private fun requestPermission() {
        ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            android.Manifest.permission.CAMERA
        )
    }

    private fun permissionGranted() = ContextCompat.checkSelfPermission(
        this, android.Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    fun onRequestPermissionResult(
        requestCode: Int,
        permission: Array<String>,
        grantResult: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permission, grantResult)
        if (requestCode == 0) {
            if (grantResult[0] == PackageManager.PERMISSION_GRANTED) {
                initView()
            } else {
                Toast.makeText(
                    this, "camera permission denied",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun initView() {
        setContent {
            CameraAndriodTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    var lens by remember {
                        mutableStateOf(CameraSelector.LENS_FACING_BACK)
                    }
                    CameraPreview(cameraLens = lens)

                }
            }
        }
    }

    @Composable
    private fun CameraPreview(previewView: PreviewView) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                previewView.apply {
                    this.scaleType = PreviewView.ScaleType.FILL_CENTER
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                previewView
            })
    }

    @Composable
    fun CameraPreview(
        modifier: Modifier = Modifier,
        cameraLens: Int
    ) {


        val lifecycleOwner = LocalLifecycleOwner.current
        val context = LocalContext.current
        var sourceInfo by remember { mutableStateOf(SourceInfo(10, 10, false)) }
        var detectedObject by remember { mutableStateOf<List<DetectedObject>>(emptyList()) }
        var detectedPose by remember { mutableStateOf<Pose?>(null) }
        val previewView = remember { PreviewView(context) }
        val cameraProvider = remember(sourceInfo) {
            ProcessCameraProvider.getInstance(context)
                .configureCamera(
                    previewView, lifecycleOwner, cameraLens, context,
                    setSourceInfo = { sourceInfo = it },
                    onObjectDetected = { detectedObject = it },
                    onPoseDetected = { detectedPose = it }
                )
        }
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            with(LocalDensity.current) {
                Box(
                    modifier = Modifier
                        .size(
                            height = sourceInfo.height.toDp(),
                            width = sourceInfo.width.toDp()
                        )
                        .scale(
                            calculateScale(
                                constraints,
                                sourceInfo,
                                PreviewScaleType.CENTER_CROP
                            )
                        )
                ) {
                    CameraPreview(previewView)
                    DetectedObject(detectedObjects = detectedObject, sourceInfo = sourceInfo)
                    DetectedPose(pose = detectedPose, sourceInfo = sourceInfo)
                }
            }
        }
    }

    private fun ListenableFuture<ProcessCameraProvider>.configureCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        cameraLens: Int,
        context: Context,
        setSourceInfo: (SourceInfo) -> Unit,
        onObjectDetected: (List<DetectedObject>) -> Unit,
        onPoseDetected: (Pose) -> Unit
    ): ListenableFuture<ProcessCameraProvider> {
        addListener({
            val cameraSelector = CameraSelector.Builder().requireLensFacing(cameraLens).build()

            val preview = androidx.camera.core.Preview.Builder()
                .build()
                .apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }
            val analysis =
                bindAnalysisCase(cameraLens, setSourceInfo, onObjectDetected, onPoseDetected)
            try {
                get().apply {
                    unbindAll()
                    bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                    bindToLifecycle(lifecycleOwner, cameraSelector, analysis)
                }
            } catch (exc: Exception) {
                TODO("process errors")
            }
        }, ContextCompat.getMainExecutor(context))
        return this
    }

    private fun bindAnalysisCase(
        lens: Int,
        setSourceInfo: (SourceInfo) -> Unit,
        onObjectDetected: (List<DetectedObject>) -> Unit,
        onPoseDetected: (Pose) -> Unit
    ): ImageAnalysis? {

        val poseProcessor = try {
            Log.e("CameraMisha", "Все окей, Pose detector работает")
            PoseDetectorProcessor()
        } catch (e: Exception) {
            Log.e("CameraMisha", "Can not create pose processor", e)
            return null
        }
        val detectProcessor = try {
            Log.e("CameraMisha", "Все окей, Object detector работает")
            DetectedObjectProcessor()
        } catch (e: Exception) {
            Log.e("CameraMisha", "Can not create object detector processor", e)
            return null
        }
        val builder = ImageAnalysis.Builder()
        val analysisUseCase = builder.build()

        var sourceInfoUpdater = false

        analysisUseCase.setAnalyzer(
            TaskExecutors.MAIN_THREAD
        ) { imageProxy: ImageProxy ->
            if (!sourceInfoUpdater) {
                Log.e("CameraMisha", "Все окей, Pose detector работает")
                setSourceInfo(obtainSourceInfo(lens, imageProxy))
                sourceInfoUpdater = true
            }
            try {
                Log.e("CameraMisha", "Все окей, Posedetector.ProcessImageProxy работает")
                //detectProcessor.processImageProxy(imageProxy, onObjectDetected)
                poseProcessor.processImageProxy(imageProxy, onPoseDetected)
            } catch (e: MlKitException) {
                Log.e(
                    "CameraMisha",
                    "Failed to process image on Pose Detector. Error: " + e.localizedMessage
                )
            }
            try {
                Log.e("CameraMisha", "Все окей, ObjectDetector.ProcessImageProxy работает")
                detectProcessor.processImageProxy(imageProxy, onObjectDetected)

                // poseProcessor.processImageProxy(imageProxy, onPoseDetected)
            } catch (e: MlKitException) {
                Log.e(
                    "CameraMisha",
                    "Failed to process image on Detector Object. Error: " + e.localizedMessage
                )
            }
        }
        return analysisUseCase
    }

    @Composable
    fun DetectedPose(
        pose: Pose?,
        sourceInfo: SourceInfo
    ) {
        if (pose != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 1.dp.toPx()
                val whitePaint = SolidColor(Color.White)
                val leftPaint = SolidColor(Color.Green)
                val rightPaint = SolidColor(Color.Yellow)

                val needToMirror = sourceInfo.isImageFlipped
                val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
                val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
                val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
                val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
                val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
                val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
                val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
                val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
                val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
                val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
                val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
                val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

                val leftPinky = pose.getPoseLandmark(PoseLandmark.LEFT_PINKY)
                val rightPinky = pose.getPoseLandmark(PoseLandmark.RIGHT_PINKY)
                val leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX)
                val rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX)
                val leftThumb = pose.getPoseLandmark(PoseLandmark.LEFT_THUMB)
                val rightThumb = pose.getPoseLandmark(PoseLandmark.RIGHT_THUMB)
                val leftHeel = pose.getPoseLandmark(PoseLandmark.LEFT_HEEL)
                val rightHeel = pose.getPoseLandmark(PoseLandmark.RIGHT_HEEL)
                val leftFootIndex = pose.getPoseLandmark(PoseLandmark.LEFT_FOOT_INDEX)
                val rightFootIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_FOOT_INDEX)
                fun drawLine(
                    startLandmark: PoseLandmark?,
                    endLandmark: PoseLandmark?,
                    paint: Brush
                ) {
                    if (startLandmark != null && endLandmark != null) {
                        val startX =
                            if (needToMirror) size.width - startLandmark.position.x else startLandmark.position.x
                        val startY = startLandmark.position.y
                        val endX =
                            if (needToMirror) size.width - endLandmark.position.x else endLandmark.position.x
                        val endY = endLandmark.position.y
                        drawLine(
                            brush = paint,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = strokeWidth,
                        )
                    }
                }
                drawLine(leftShoulder, rightShoulder, whitePaint)
                drawLine(leftHip, rightHip, whitePaint)
                // Left body
                drawLine(leftShoulder, leftElbow, leftPaint)
                drawLine(leftElbow, leftWrist, leftPaint)
                drawLine(leftShoulder, leftHip, leftPaint)
                drawLine(leftHip, leftKnee, leftPaint)
                drawLine(leftKnee, leftAnkle, leftPaint)
                drawLine(leftWrist, leftThumb, leftPaint)
                drawLine(leftWrist, leftPinky, leftPaint)
                drawLine(leftWrist, leftIndex, leftPaint)
                drawLine(leftIndex, leftPinky, leftPaint)
                drawLine(leftAnkle, leftHeel, leftPaint)
                drawLine(leftHeel, leftFootIndex, leftPaint)
                // Right body
                drawLine(rightShoulder, rightElbow, rightPaint)
                drawLine(rightElbow, rightWrist, rightPaint)
                drawLine(rightShoulder, rightHip, rightPaint)
                drawLine(rightHip, rightKnee, rightPaint)
                drawLine(rightKnee, rightAnkle, rightPaint)
                drawLine(rightWrist, rightThumb, rightPaint)
                drawLine(rightWrist, rightPinky, rightPaint)
                drawLine(rightWrist, rightIndex, rightPaint)
                drawLine(rightIndex, rightPinky, rightPaint)
                drawLine(rightAnkle, rightHeel, rightPaint)
                drawLine(rightHeel, rightFootIndex, rightPaint)
            }
        }
    }

    @Composable
    fun DetectedObject(
        detectedObjects: List<DetectedObject>,
        sourceInfo: SourceInfo
    ) {
        Box() {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val needToMirror = sourceInfo.isImageFlipped

                for (detectedObject in detectedObjects) {
                    drawRect(
                        Color.Red, style = Stroke(1.dp.toPx()),
                        topLeft = Offset(
                            detectedObject.boundingBox.left.toFloat(),
                            detectedObject.boundingBox.top.toFloat()
                        ),
                        size = Size(
                            detectedObject.boundingBox.width().toFloat(),
                            detectedObject.boundingBox.height().toFloat()
                        )
                    )
                    val text = detectedObject.labels.joinToString {
                        it.text
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        text,
                        detectedObject.boundingBox.left.toFloat(),
                        detectedObject.boundingBox.top.toFloat() - 16F,  // Adjust the Y offset for the text placement
                        Paint().apply {
                            color = android.graphics.Color.RED
                            textSize = 24f // Set the text size as needed
                        }
                    )
                }
            }
        }
    }

    private fun obtainSourceInfo(lens: Int, imageProxy: ImageProxy): SourceInfo {
        val isImageFlipped = lens == CameraSelector.LENS_FACING_FRONT
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        return if (rotationDegrees == 0 || rotationDegrees == 180) {
            SourceInfo(
                height = imageProxy.height,
                width = imageProxy.width,
                isImageFlipped = isImageFlipped
            )
        } else {
            SourceInfo(
                height = imageProxy.width,
                width = imageProxy.height,
                isImageFlipped = isImageFlipped
            )
        }
    }

    private fun calculateScale(
        constraints: Constraints,
        sourceInfo: SourceInfo,
        scaleType: PreviewScaleType
    ): Float {
        val heightRatio = constraints.maxHeight.toFloat() / sourceInfo.height
        val widthRatio = constraints.maxWidth.toFloat() / sourceInfo.width
        return when (scaleType) {
            PreviewScaleType.FIT_CENTER -> kotlin.math.min(heightRatio, widthRatio)
            PreviewScaleType.CENTER_CROP -> kotlin.math.max(heightRatio, widthRatio)
        }
    }
}
data class SourceInfo(
    val width: Int,
    val height: Int,
    val isImageFlipped: Boolean,
)

private enum class PreviewScaleType {
    FIT_CENTER,
    CENTER_CROP
}


package com.example.cameraandriod;
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.cameraandriod.ui.theme.CameraAndriodTheme
import com.example.cameraandriod.ui.theme.notification.PushService
import com.google.android.gms.tasks.TaskExecutors
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
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
                    val sharedPref : SharedPreferences
                    sharedPref = getSharedPreferences("Phone number", Context.MODE_PRIVATE)
                    val phoneNumber = remember { mutableStateOf("") }
                    PhoneNumberScreen(phoneNumber)
                    /*Button(onClick = { savePhoneNumber(phoneNumber.value) }) {
                        Text("Save phone number")
                    }*/
                }
            }
        }
    }

    fun saveNumber(phoneNumber: String){

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
        var detectedFaces by remember { mutableStateOf<List<Face>>(emptyList()) }
        var detectedObject by remember { mutableStateOf<List<DetectedObject>>(emptyList()) }
        var detectedPose by remember { mutableStateOf<Pose?>(null) }
        var labelDetected by remember { mutableStateOf<List<ImageLabel>>(emptyList()) }
        val previewView = remember { PreviewView(context) }
        val cameraProvider = remember(sourceInfo) {
            ProcessCameraProvider.getInstance(context)
                .configureCamera(
                    previewView, lifecycleOwner, cameraLens, context,
                    setSourceInfo = { sourceInfo = it },
                    onObjectDetected = { detectedObject = it },
                    onLabelDetector = {labelDetected = it},
                    onPoseDetected = { detectedPose = it },
                    onFacesDetected = { detectedFaces = it },
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
                   // DetectedObjectLabels(labels = labelDetected, sourceInfo = sourceInfo)
                    DetectedPose(pose = detectedPose, sourceInfo = sourceInfo)
                    DetectedFaces(faces = detectedFaces, sourceInfo = sourceInfo)
                    DetectedObject(detectedObjects = detectedObject, sourceInfo = sourceInfo)
                    poseSitDownAnalyzer(pose = detectedPose)
                    poseLeaningForward(pose = detectedPose)
                    isHuman(labels = labelDetected)
                    isCar(labels = labelDetected)
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
        onFacesDetected: (List<Face>) -> Unit,
        onObjectDetected: (List<DetectedObject>) -> Unit,
        onLabelDetector: (List<ImageLabel>) -> Unit,
        onPoseDetected: (Pose) -> Unit,
    ): ListenableFuture<ProcessCameraProvider> {
        addListener({
            val cameraSelector = CameraSelector.Builder().requireLensFacing(cameraLens).build()

            val preview = androidx.camera.core.Preview.Builder()
                .build()
                .apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }
            val analysis =
                bindAnalysisCase(cameraLens, setSourceInfo,onObjectDetected/*,onLabelDetector*/,  onPoseDetected, onFacesDetected)
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
       // onLabelDetector: (List<ImageLabel>) -> Unit,
        onPoseDetected: (Pose) -> Unit,
        onFacesDetected: (List<Face>) -> Unit
    ): ImageAnalysis? {

        val poseProcessor = try {
            Log.d("CameraMisha", "Все окей, Pose detector работает")
            PoseDetectorProcessor()
        } catch (e: Exception) {
            Log.d("CameraMisha", "Can not create pose processor", e)
            return null
        }
        val faceProcessor = try {
            Log.d("CameraMisha", "Все окей, Object detector работает")
            FaceDetectorProcessor()
        } catch (e: Exception) {
            Log.d("CameraMisha", "Can not create object detector processor", e)
            return null
        }
        val labelProcessor = try {
            Log.d("CameraMisha", "Все окей, Object detector работает")
            LabelObjectProcessor()
        } catch (e: Exception) {
            Log.d("CameraMisha", "Can not create object detector processor", e)
            return null
        }
        val detectProcessor = try {
            Log.d("CameraMisha", "Все окей, Object detector работает")
            DetectedObjectProcessor()
        } catch (e: Exception) {
            Log.d("CameraMisha", "Can not create object detector processor", e)
            return null
        }
        val builder = ImageAnalysis.Builder()
        val analysisUseCase = builder.build()

        var sourceInfoUpdater = false

        analysisUseCase.setAnalyzer(
            TaskExecutors.MAIN_THREAD
        ) { imageProxy: ImageProxy ->
            if (!sourceInfoUpdater) {
                Log.d("CameraMisha", "SourceInfo работает")
                setSourceInfo(obtainSourceInfo(lens, imageProxy))
                sourceInfoUpdater = true
            }
            try {
                Log.d("CameraMisha", "Все окей, Posedetector.ProcessImageProxy работает")
                poseProcessor.processImageProxy(imageProxy, onPoseDetected)
            } catch (e: MlKitException) {
                Log.d(
                    "CameraMisha",
                    "Failed to process image on Pose Detector. Error: " + e.localizedMessage
                )
            }
            try {
                Log.d("CameraMisha", "Все окей, ObjectDetector.ProcessImageProxy работает")
                detectProcessor.processImageProxy(imageProxy, onObjectDetected)
            } catch (e: MlKitException) {
                Log.d(
                    "CameraMisha",
                    "Failed to process image on Detector Object. Error: " + e.localizedMessage
                )
            }
            try {
                Log.d("CameraMisha", "Все окей, Posedetector.ProcessImageProxy работает")
               // labelProcessor.processImageProxy(imageProxy, onLabelDetector)
            } catch (e: MlKitException) {
                Log.d(
                    "CameraMisha",
                    "Failed to process image on sitDownProcessor. Error: " + e.localizedMessage
                )
            }
            try {
                Log.d("CameraMisha", "Все окей, Posedetector.ProcessImageProxy работает")
                faceProcessor.processImageProxy(imageProxy, onFacesDetected)
            } catch (e: MlKitException) {
                Log.d(
                    "CameraMisha",
                    "Failed to process image on Pose Detector. Error: " + e.localizedMessage
                )
            }
        }
        return analysisUseCase
    }
    fun getAngle(firstPoint: PoseLandmark?, midPoint: PoseLandmark?, lastPoint: PoseLandmark?): Double{
        if (lastPoint != null && midPoint != null && firstPoint != null){
        var result = Math.toDegrees(
            Math.atan2(
                lastPoint.position.y.toDouble() - midPoint.position.y.toDouble(),
                lastPoint.position.x.toDouble() - midPoint.position.x.toDouble()
            )
                    - Math.atan2(
                firstPoint.position.y.toDouble() - midPoint.position.y.toDouble(),
                firstPoint.position.x.toDouble() - midPoint.position.x.toDouble()
            )
        )
        result = Math.abs(result) // Angle should never be negative
        if (result > 180) {
            result = 360.0 - result // Always get the acute representation of the angle
            PushService().sendNotification(this)
        }
        return result
        }
        return 0.0
    }
    fun poseSitDownAnalyzer(pose: Pose?){
        if (pose != null){
            val angle = getAngle(pose.getPoseLandmark(PoseLandmark.LEFT_HIP),
                pose.getPoseLandmark(PoseLandmark.LEFT_KNEE),
                pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE))
            if (angle>20 && angle<130){
                Log.d("CameraMishaPoseDetector", "Человевек сел!")
            }
        }
    }
    fun poseLeaningForward(pose: Pose?){
        if (pose != null){
            val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
            val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
            val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
            val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
            val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
            val angle = getAngle(leftShoulder, nose, leftHip) + getAngle(leftShoulder, nose, rightHip)
            if (angle < 160 && angle > 60){
                Log.d("CameraMishaPoseDetector", "Человевек наклонился!")
            }
        }
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
    fun DetectedFaces(
        faces: List<Face>,
        sourceInfo: SourceInfo
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val needToMirror = sourceInfo.isImageFlipped

            for (face in faces) {
                Log.d("CameraMishaFace", "человек: ${face.trackingId}")
                val left =
                    if (needToMirror) size.width - face.boundingBox.right.toFloat() else face.boundingBox.left.toFloat()
                drawRect(
                    Color.Gray, style = Stroke(2.dp.toPx()),
                    topLeft = Offset(left, face.boundingBox.top.toFloat()),
                    size = Size(face.boundingBox.width().toFloat(), face.boundingBox.height().toFloat())
                )
            }
        }
    }
    /*@Composable
    fun DetectedObject(
        detectedObjects: List<DetectedObject>,
        sourceInfo: SourceInfo
    ) {
        Box() {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(Color.Transparent, style = Fill)
                val needToMirror = sourceInfo.isImageFlipped
                // Предположим, что у нас есть переменные для координат объекта
                for (detectedObject in detectedObjects){
                    val objectX = detectedObject.boundingBox.left.toFloat()
                    val objectY = detectedObject.boundingBox.top.toFloat()
                    val objectWidth = detectedObject.boundingBox.width().toFloat()
                    val objectHeight = detectedObject.boundingBox.height().toFloat()
                    // Отрисовка рамки вокруг объекта
                    drawRect(color = Color.Blue, topLeft = Offset(objectX, objectY), size = Size(objectWidth, objectHeight), style = Stroke(width = 2f))

                    // Подпись названия объекта
                    val objectName = detectedObject.labels.joinToString {
                        it.text
                    }
                    val textOffsetX = objectX + objectWidth / 2
                    val textOffsetY = objectY + objectHeight + 20f
                    drawIntoCanvas {
                        it.nativeCanvas.drawText(objectName, textOffsetX, textOffsetY, Paint().apply {
                            color = Color.Black.toArgb()
                            textSize = 24f
                        })
                    }
                }



            }
        }
    }*/
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
    /*@Composable
    fun DetectedObjectLabels(
        labels: List<ImageLabel>,
        sourceInfo: SourceInfo
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 32.dp)) {
            val needToMirror = sourceInfo.isImageFlipped
            for (label in labels) {
                val text = label.text
                val confidence = label.confidence
                val index = label.index
                Text("Найден объект: ${text} / ${(confidence*100).roundToInt()}% $index"
                    , modifier = Modifier.fillMaxWidth(0.8f), fontSize = 5.sp, maxLines = 2)
            }
        }
    }*/
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneNumberScreen(phoneNumber: MutableState<String>){
    val context = LocalContext.current
    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            phoneNumber.value,
            { phoneNumber.value = it },
            textStyle = TextStyle(fontSize = 28.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            label = { Text("Введите номер телефона") }
        )
        Button(
            onClick = { savePhoneNumber(context, phoneNumber.value) },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Сохранить номер")
        }
    }
}
private const val PHONE_PREFS_KEY = "phone_number"
fun savePhoneNumber(context: Context, phoneNumber: String){
    val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putString(PHONE_PREFS_KEY, phoneNumber)
    editor.apply()
    if (sharedPreferences.getString(PHONE_PREFS_KEY,"")!!.isNotEmpty()){
        Log.d("SaveNumber","Телефон сохранен")
    }
}
private fun getSavedPhoneNumber(context: Context): String? {
    val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    return sharedPreferences.getString(PHONE_PREFS_KEY, null)
}

fun distanceBetweenObjects(detectedObjects: List<DetectedObject>):Boolean{

    for (detectedObject in detectedObjects) {
        val boundingBox = detectedObject.boundingBox
        val trackingId = detectedObject.trackingId
        for (label in detectedObject.labels) {
            val text = label.text
            val index = label.index
            val confidence = label.confidence
        }
    }

    return false
}
fun isCar(labels: List<ImageLabel>):Boolean{
    for (label in labels){
        if(label.text == "Car"||label.text == "Vehicle"){
            Log.d("CameraMishaLabelIs", "Обнаружена машина")
            return true
        }
    }
    return false
}
fun isHuman(labels: List<ImageLabel>):Boolean {
    for (label in labels){
        if (label.text == "Hand" || label.text == "Skin"){
            Log.d("CameraMishaLabelIs", "Обнаружен человек!")
            return true
        }
    }
    return false
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


data class SourceInfo(
    val width: Int,
    val height: Int,
    val isImageFlipped: Boolean,
)

private enum class PreviewScaleType {
    FIT_CENTER,
    CENTER_CROP
}


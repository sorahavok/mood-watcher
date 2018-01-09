package ai.havok.sora.faceai

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import com.wonderkiln.camerakit.*
import kotlinx.android.synthetic.main.activity_raw_camera.*


class RawCamera : AppCompatActivity() {

    private val timeBetweenPicMs = 2000L //milliseconds
    private val cameraHandler = Handler()

    private var currentImage = ByteArray(0)
    private var faceDetector : FaceDetector? = null

    private var userFaceState = FaceState.NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_raw_camera)
        addSwitchListener()
        addToggleCamButtonListener()
        addMoodRadioListener()
        addCameraListener()
        initFaceDetector()

        cameraView.facing = CameraKit.Constants.FACING_FRONT
    }

    override fun onResume() {
        super.onResume()
        cameraView.start()
    }

    override fun onPause() {
        cameraView.stop()
        super.onPause()
    }
    
    private fun initFaceDetector() {
        faceDetector = FaceDetector.Builder(applicationContext)
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setMode(FaceDetector.ACCURATE_MODE)
                .build()
        if (!faceDetector!!.isOperational) {
            AlertDialog.Builder(applicationContext)
                    .setMessage("Could not set up the face detector!")
                    .show()
            return
        }
    }

    private fun addMoodRadioListener() {
        moodRadio.setOnCheckedChangeListener({ _, checkedRadioButtonId ->
            userFaceState = when (checkedRadioButtonId) {
                sadRadio.id -> FaceState.SAD
                happyRadio.id -> FaceState.HAPPY
                madRadio.id -> FaceState.MAD
                scaredRadio.id -> FaceState.SCARED
                disgustRadio.id -> FaceState.DISGUST
                noneRadio.id -> FaceState.NONE
                else -> FaceState.UNRECOGNIZED
            }
            Log.e("setOnCheckedChangeListener", "User Face State is now $userFaceState")
        })
    }

    private fun addToggleCamButtonListener() {
        toggleCamButton.setOnCheckedChangeListener({ _, _ ->
            Log.i("toggleCamButton", "toggleCamButton Hit!")
            if (helpingSwitch.isChecked) {
                helpingSwitch.toggle()
                // Sleep to let the last picture finish being taken
                Thread.sleep(timeBetweenPicMs)
                helpingSwitch.toggle()
            }
            cameraView.toggleFacing()
        })
    }

    private fun addSwitchListener() {
        helpingSwitch.setOnCheckedChangeListener({ _, isChecked ->
            cameraView.captureImage()
            if(isChecked) {
                cameraHandler.postDelayed(object : Runnable {
                    override fun run() {
                        cameraView.captureImage()
                        cameraHandler.postDelayed(this, timeBetweenPicMs)
                    }
                }, timeBetweenPicMs)
            } else {
                cameraHandler.removeCallbacksAndMessages(null)
            }
        })
    }

    private fun addCameraListener() {
        val myRectPaint = Paint()
        myRectPaint.strokeWidth = 5f
        myRectPaint.color = Color.RED
        myRectPaint.style = Paint.Style.STROKE

        val landmarkDotsPaint = Paint()
        landmarkDotsPaint.strokeWidth = 2f
        landmarkDotsPaint.color = Color.GREEN
        landmarkDotsPaint.style = Paint.Style.STROKE

        cameraView.addCameraKitListener(object : CameraKitEventListener {
            override fun onEvent(cameraKitEvent: CameraKitEvent) {
                val jpeg = cameraKitEvent.message
                Log.i("Camera", "In onEvent " + jpeg)
            }

            override fun onError(cameraKitError: CameraKitError) {
                print("looks like we got an error " + cameraKitError.message)
                Log.e("Camera", "looks like we got an error ", cameraKitError.exception)
            }

            override fun onImage(cameraKitImage: CameraKitImage) {
                Log.i("Camera", "In onImage")
                currentImage = cameraKitImage.jpeg // will return byte[]
                val options = BitmapFactory.Options()
                options.inMutable = true
                val img = BitmapFactory.decodeByteArray(currentImage, 0, currentImage.size, options )
                val tempCanvas = Canvas(img)
                tempCanvas.drawBitmap(img, 0f, 0f, null)
                val frame = Frame.Builder().setBitmap(img).build()
                val faces = faceDetector!!.detect(frame)
                Log.i("On Image", "Got ${faces.size()} faces.")
                for (i in 0 until faces.size()) {
                    val thisFace = faces.valueAt(i)
                    drawRedBoundingBox(thisFace, tempCanvas)
                    drawFaceLandmarks(thisFace, tempCanvas)
                    val faceProto = FaceProtoHelper.convertFaceToProto(thisFace, userFaceState)
                    Log.w("onImage", "Face Proto: $faceProto")
                }
                imageView.setImageDrawable(BitmapDrawable(resources, img))
            }

            private fun drawFaceLandmarks(face: Face, canvas: Canvas) {
                for (landmark in face.landmarks) {
                    Log.i("drawFaceLandmarks", "Got ${face.landmarks.size} landmarks.")
                    val landmarkX = landmark.position.x
                    val landmarkY = landmark.position.y
                    canvas.drawCircle(landmarkX, landmarkY, 20f, landmarkDotsPaint)
                }
            }

            private fun drawRedBoundingBox(thisFace: Face, tempCanvas: Canvas) {
                val x1 = thisFace.position.x
                val y1 = thisFace.position.y
                val x2 = x1 + thisFace.width
                val y2 = y1 + thisFace.height
                tempCanvas.drawRect(x1, y1, x2, y2, myRectPaint)
            }

            override fun onVideo(video: CameraKitVideo?) {}
        })
    }
}

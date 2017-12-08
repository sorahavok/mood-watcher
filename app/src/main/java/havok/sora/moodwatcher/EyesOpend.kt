package havok.sora.moodwatcher

import android.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import kotlinx.android.synthetic.main.activity_eyes_opend.*
import android.util.Log
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.FaceDetector
import com.wonderkiln.camerakit.CameraKitEventListener
import com.wonderkiln.camerakit.CameraKitVideo
import com.wonderkiln.camerakit.CameraKitImage
import com.wonderkiln.camerakit.CameraKitError
import com.wonderkiln.camerakit.CameraKitEvent
import android.graphics.drawable.BitmapDrawable
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import android.R.attr.y
import android.R.attr.x
import android.graphics.*
import java.nio.file.Files.size



/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class EyesOpened : AppCompatActivity() {

    val timeBetweenPicMs = 2000L //milliseconds
    val cameraHandler = Handler()

    var currentImage = ByteArray(0)
    var faceDetector : FaceDetector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_eyes_opend)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        addSwitchListener()

        if (cameraView.isFacingBack) {
            cameraView.toggleFacing()
        }
        addCameraListener()

        faceDetector = FaceDetector.Builder(applicationContext)
                .setTrackingEnabled(false)
                .setClassificationType(FaceDetector.ACCURATE_MODE)
                .build()
        if (!faceDetector!!.isOperational) {
            AlertDialog.Builder(applicationContext)
                    .setMessage("Could not set up the face detector!")
                    .show()
            return
        }
    }

    private fun addSwitchListener() {
        helpingSwitch.setOnCheckedChangeListener({ _, isChecked ->
            cameraView.captureImage()
            print("The button is Checked: " + isChecked)
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
            true
        })
    }

    private fun addCameraListener() {
        val myRectPaint = Paint()
        myRectPaint.strokeWidth = 5f
        myRectPaint.color = Color.RED
        myRectPaint.style = Paint.Style.STROKE

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
                print(cameraKitImage.toString())
                Log.i("Camera", "In onImage")
                currentImage = cameraKitImage.jpeg // will return byte[]
                Log.i("Camera", currentImage.contentToString())
                val options = BitmapFactory.Options()
                options.inMutable = true
                val img = BitmapFactory.decodeByteArray(currentImage, 0, currentImage.size, options )
                val tempCanvas = Canvas(img)
                tempCanvas.drawBitmap(img, 0f, 0f, null)
                val frame = Frame.Builder().setBitmap(img).build()
                val faces = faceDetector!!.detect(frame)
                for (i in 0 until faces.size()) {
                    val thisFace = faces.valueAt(i)
                    val x1 = thisFace.position.x
                    val y1 = thisFace.position.y
                    val x2 = x1 + thisFace.width
                    val y2 = y1 + thisFace.height

                    tempCanvas.drawRoundRect(RectF(x1, y1, x2, y2), 2f, 2f, myRectPaint)
                }
                imageView.setImageDrawable(BitmapDrawable(resources, img))
            }

            override fun onVideo(video: CameraKitVideo?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        cameraView.start()
    }

    override fun onPause() {
        cameraView.stop()
        super.onPause()
    }
}
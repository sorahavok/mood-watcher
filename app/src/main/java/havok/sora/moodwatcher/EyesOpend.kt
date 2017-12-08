package havok.sora.moodwatcher

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import kotlinx.android.synthetic.main.activity_eyes_opend.*
import android.graphics.BitmapFactory
import android.util.Log
import com.wonderkiln.camerakit.CameraKitEventListener
import com.wonderkiln.camerakit.CameraKitVideo
import com.wonderkiln.camerakit.CameraKitImage
import com.wonderkiln.camerakit.CameraKitError
import com.wonderkiln.camerakit.CameraKitEvent

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class EyesOpened : AppCompatActivity() {

    val timeBetweenPicMs = 1000L //milliseconds
    val cameraHandler = Handler()

    var currentImage = ByteArray(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_eyes_opend)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        addSwitchListener()

        if (cameraView.isFacingBack) {
            cameraView.toggleFacing()
        }
        addCameraListener()


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
                val img = BitmapFactory.decodeByteArray(currentImage, 0, currentImage.size)
                imageView.setImageBitmap(img)
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
package com.cspoet.ktflite

import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.wonderkiln.camerakit.*
import java.util.concurrent.Executors

class StartupActivity : AppCompatActivity() {

    companion object {
        private val MODEL_PATH = "mobilenet_quant_v1_224.tflite"
        private val LABEL_PATH = "labels.txt"
        private val INPUT_SIZE = 224
    }

    lateinit var classifier: Classifier

    private val executor = Executors.newSingleThreadExecutor()
    lateinit var textViewResult: TextView
    lateinit var btnDetectObject: Button
    lateinit var btnToggleCamera:Button
    lateinit var imageViewResult: ImageView
    lateinit var cameraView: CameraView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cameraView = findViewById(R.id.cameraView)
        imageViewResult = findViewById(R.id.imageViewResult)
        textViewResult = findViewById(R.id.textViewResult)
        textViewResult.movementMethod = ScrollingMovementMethod()

        btnToggleCamera = findViewById(R.id.btnToggleCamera)
        btnDetectObject = findViewById(R.id.btnDetectObject)

        cameraView.addCameraKitListener(object : CameraKitEventListener {
            override fun onEvent(cameraKitEvent: CameraKitEvent) {

            }

            override fun onError(cameraKitError: CameraKitError) {

            }

            override fun onImage(cameraKitImage: CameraKitImage) {

                var bitmap = cameraKitImage.bitmap

                bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false)

                imageViewResult.setImageBitmap(bitmap)

                val results = classifier.recognizeImage(bitmap)

                textViewResult.text = results.toString()

            }

            override fun onVideo(cameraKitVideo: CameraKitVideo) {

            }
        })

        btnToggleCamera.setOnClickListener { cameraView.toggleFacing() }

        btnDetectObject.setOnClickListener { cameraView.captureImage() }

        initTensorFlowAndLoadModel()
    }

    override fun onResume() {
        super.onResume()
        cameraView.start()
    }

    override fun onPause() {
        cameraView.stop()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.execute { classifier.close() }
    }

    private fun initTensorFlowAndLoadModel() {
        executor.execute {
            try {
                classifier = Classifier.create(
                        assets,
                        MODEL_PATH,
                        LABEL_PATH,
                        INPUT_SIZE)
                makeButtonVisible()
            } catch (e: Exception) {
                throw RuntimeException("Error initializing TensorFlow!", e)
            }
        }
    }

    private fun makeButtonVisible() {
        runOnUiThread { btnDetectObject.visibility = View.VISIBLE }
    }
}

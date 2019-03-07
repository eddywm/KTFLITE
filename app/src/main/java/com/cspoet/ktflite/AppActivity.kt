package com.cspoet.ktflite

import android.app.Dialog
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import com.wonderkiln.camerakit.*
import java.util.concurrent.Executors


class AppActivity : AppCompatActivity() {

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
        AppCenter.start(
            application, "92e13fbb-77e2-444e-a4ea-56a7dc805de6",
            Analytics::class.java, Crashes::class.java
        )
        setContentView(R.layout.activity_main)
        cameraView = findViewById(R.id.cameraView)
        imageViewResult = findViewById<ImageView>(R.id.imageViewResult)
        textViewResult = findViewById(R.id.textViewResult)
        textViewResult.movementMethod = ScrollingMovementMethod()

        btnToggleCamera = findViewById(R.id.btnToggleCamera)
        btnDetectObject = findViewById(R.id.btnDetectObject)

        val resultDialog = Dialog(this)
        val customProgressView = LayoutInflater.from(this).inflate(R.layout.result_dialog_layout, null)
        resultDialog.setCancelable(false)
        resultDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        resultDialog.setContentView(customProgressView)

        val ivImageResult = customProgressView.findViewById<ImageView>(R.id.iViewResult)

        val tvLoadingText = customProgressView.findViewById<TextView>(R.id.tvLoadingRecognition)

        val tvTextResults = customProgressView.findViewById<TextView>(R.id.tvResult)


        // The Loader Holder is used due to a bug in the Avi Loader library
        val aviLoaderHolder = customProgressView.findViewById<View>(R.id.aviLoaderHolderView)


        cameraView.addCameraKitListener(object : CameraKitEventListener {
            override fun onEvent(cameraKitEvent: CameraKitEvent) {

            }

            override fun onError(cameraKitError: CameraKitError) {

            }

            override fun onImage(cameraKitImage: CameraKitImage) {

                var bitmap = cameraKitImage.bitmap

                bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false)

                aviLoaderHolder.visibility = View.GONE
                tvLoadingText.visibility = View.GONE

                val results = classifier.recognizeImage(bitmap)

                ivImageResult.setImageBitmap(bitmap)
                tvTextResults.text = results.toString()

                tvTextResults.visibility = View.VISIBLE
                ivImageResult.visibility = View.VISIBLE

                resultDialog.setCancelable(true)



            }

            override fun onVideo(cameraKitVideo: CameraKitVideo) {

            }
        })

        btnToggleCamera.setOnClickListener { cameraView.toggleFacing() }

        btnDetectObject.setOnClickListener {

            cameraView.captureImage()

            resultDialog.show()
            tvTextResults.visibility = View.GONE
            ivImageResult.visibility = View.GONE



        }

        resultDialog.setOnDismissListener {
            tvLoadingText.visibility = View.VISIBLE
            aviLoaderHolder.visibility = View.VISIBLE

        }

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

package com.example.francesco.ingredientsscanner

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_camera.*
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.CameraListener
import android.R.attr.name
import android.content.Intent
import java.io.File


private const val TAG = "CameraActivity"
const val EXTRA_PICTUREPATH = "com.example.ingredientsscanner.PICTUREPATH"

class CameraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        camera.setLifecycleOwner(this)
        camera.addCameraListener(object : CameraListener() {
            override fun onPictureTaken(result: PictureResult) { onPicture(result) }
        })

        takePicture.setOnClickListener { camera.takePicture() }
    }

    private fun onPicture(result: PictureResult) {
        val outputDir = cacheDir
        val imageFile = File(outputDir, "$name.jpg")
        result.toFile(imageFile) {
            Log.d(TAG, "imagesaved")
            val path = it?.path
            if(path != null) launchResultActivity(path) else Log.e(TAG, "Error creating image file")
        }
    }

    private fun launchResultActivity(picturePath: String) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(EXTRA_PICTUREPATH, picturePath)
        }
        startActivity(intent)
        finish()
    }
}

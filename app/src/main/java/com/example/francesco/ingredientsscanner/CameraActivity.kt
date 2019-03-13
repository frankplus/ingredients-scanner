package com.example.francesco.ingredientsscanner

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_camera.*
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.CameraListener
import android.content.Intent
import java.io.File

const val EXTRA_PICTUREPATH = "PICTURE_PATH"
const val CAMERA_SHAREDPREFS = "CAMERA_SHARED_PREFERENCES"
const val KEY_LASTPICPATH = "LAST_PICTURE_PATH"

private const val TAG = "CameraActivity"
private const val PICTURE_FILENAME = "ingredients_picture.jpg"

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
        val imageFile = File(outputDir, PICTURE_FILENAME)
        result.toFile(imageFile) {
            val picturePath = it?.path
            if(picturePath != null) {
                savePicture(picturePath)
                launchResultActivity(picturePath)
            } else Log.e(TAG, "Error creating image file")
        }
    }

    private fun launchResultActivity(picturePath: String) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(EXTRA_PICTUREPATH, picturePath)
        }
        startActivity(intent)
        finish()
    }

    private fun savePicture(picturePath: String) {
        val sharedPref = getSharedPreferences(CAMERA_SHAREDPREFS, Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putString(KEY_LASTPICPATH, picturePath)
            apply()
        }
    }
}

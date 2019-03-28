package com.example.francesco.ingredientsscanner

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_camera.*
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.CameraListener
import android.content.Intent
import java.io.File
import android.net.Uri


const val EXTRA_PICTUREURI = "PICTURE_PATH"
const val CAMERA_SHAREDPREFS = "CAMERA_SHARED_PREFERENCES"
const val KEY_LASTPICURI = "LAST_PICTURE_PATH"

private const val TAG = "CameraActivity"
private const val PICTURE_FILENAME = "ingredients_picture.jpg"
private const val GALLERY_REQUEST_CODE = 100

class CameraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        camera.setLifecycleOwner(this)
        camera.addCameraListener(object : CameraListener() {
            override fun onPictureTaken(result: PictureResult) { onPicture(result) }
        })

        takePicture.setOnClickListener { camera.takePicture() }

        galleryButton.setOnClickListener { pickFromGallery() }
    }

    private fun onPicture(result: PictureResult) {
        val outputDir = cacheDir
        val imageFile = File(outputDir, PICTURE_FILENAME)
        result.toFile(imageFile) {
            val pictureUri = Uri.fromFile(it)
            if(pictureUri != null) {
                savePicture(pictureUri)
                launchResultActivity(pictureUri)
            } else Log.e(TAG, "Error creating image file")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == Activity.RESULT_OK) {
            when(requestCode) {
                GALLERY_REQUEST_CODE -> {
                    //data.getData returns the content URI for the selected Image
                    val selectedImage: Uri? = data?.data
                    if(selectedImage != null)
                        launchResultActivity(selectedImage)
                }
            }
        }
    }

    private fun launchResultActivity(picture: Uri) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(EXTRA_PICTUREURI, picture.toString())
        }
        startActivity(intent)
        finish()
    }

    private fun savePicture(picture: Uri) {
        val sharedPref = getSharedPreferences(CAMERA_SHAREDPREFS, Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putString(KEY_LASTPICURI, picture.toString())
            apply()
        }
    }

    private fun pickFromGallery() {
        //Create an Intent with action as ACTION_PICK
        val intent = Intent(Intent.ACTION_PICK)
        // Sets the type as image/*. This ensures only components of type image are selected
        intent.type = "image/*"
        //We pass an extra array with the accepted mime types. This will ensure only components with these MIME types as targeted.
        val mimeTypes = arrayOf("image/jpeg", "image/png")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        // Launching the Intent
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }
}

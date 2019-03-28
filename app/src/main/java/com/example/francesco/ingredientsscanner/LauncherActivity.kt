package com.example.francesco.ingredientsscanner

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

/**
 * This activity is the first started activity that chooses which activity launch next
 * @author Francesco Pham
 */
class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // load inci db, ingredients extractor, text corrector and allergens manager
        // this can continue after this activity finishes and will end when loading is finished.
        thread(start = true) {
            AppSingleton.getInstance(applicationContext)
        }

        //Get image path of last image
        val prefs = getSharedPreferences(CAMERA_SHAREDPREFS, MODE_PRIVATE)
        val lastPicturePath = prefs.getString(KEY_LASTPICURI, null)

        //Launch result activity if exists a picture
        val intent = if (lastPicturePath != null) {
            Intent(this@LauncherActivity, ResultActivity::class.java).apply {
                putExtra(EXTRA_PICTUREURI, lastPicturePath)
            }
        } else {
            Intent(this@LauncherActivity, CameraActivity::class.java)
        }

        startActivity(intent)
        finish()
    }
}

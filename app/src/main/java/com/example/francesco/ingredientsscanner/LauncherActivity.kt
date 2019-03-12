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
            InciSingleton.load(applicationContext)
        }

        //Get image path of last image
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val pathImage = prefs.getString("imagePath", null)


        val intent: Intent

        /**
         * If already exists a photo, launch result activity to show it
         * with text attached.
         * @author Luca Moroldo modified by Francesco Pham
         */
        intent = if (pathImage != null) {
            Intent(this@LauncherActivity, ResultActivity::class.java)
        } else {
            Intent(this@LauncherActivity, CameraActivity::class.java)
        }

        startActivity(intent)
        finish()
    }
}

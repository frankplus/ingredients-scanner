package com.example.francesco.ingredientsscanner

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import android.text.style.BackgroundColorSpan
import com.example.francesco.ingredientsscanner.inci.Ingredient
import android.text.SpannableString
import android.widget.ProgressBar
import android.os.AsyncTask
import android.widget.TextView
import java.lang.ref.WeakReference


private const val TAG = "ResultActivity"

class ResultActivity : AppCompatActivity() {

    private lateinit var emptyTextView: TextView
    private lateinit var analyzedTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        //on floating action button click launch camera
        fab.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        //set on empty list view
        emptyTextView= findViewById(R.id.emptyList)
        ingredientsListView.emptyView = emptyTextView

        //show analyzed text view
        analyzedTextView = TextView(this)
        ingredientsListView.addHeaderView(analyzedTextView)

        val picturePath = intent.getStringExtra(EXTRA_PICTUREPATH)
        if(picturePath != null) {
            val bitmapPicture = loadBitmapFromFile(picturePath)

            //set image view
            takenPictureView.setImageBitmap(
                Bitmap.createScaledBitmap(
                    bitmapPicture,
                    bitmapPicture.width,
                    bitmapPicture.height,
                false
            ))

            launchOCRAndIngredientsExtraction(bitmapPicture)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun launchOCRAndIngredientsExtraction(picture: Bitmap) {
        val image = FirebaseVisionImage.fromBitmap(picture)
        val detector = FirebaseVision.getInstance().onDeviceTextRecognizer

        val result = detector.processImage(image)
            .addOnSuccessListener { firebaseVisionText ->
                val ocrText = firebaseVisionText.text
                AsyncIngredientsExtraction(this).execute(ocrText)
            }
            .addOnFailureListener {
                Log.e(TAG, "text extraction failed")
            }
    }


    /**
     * Asynctask for INCI db loading, ingredients extraction from ocr text, list ingredients display
     * and progress bar update
     * @author Francesco Pham
     */
    private class AsyncIngredientsExtraction (context: ResultActivity) :
        AsyncTask<String, Void, List<Ingredient>>() {

        private val activityReference: WeakReference<ResultActivity> = WeakReference(context)
        private var correctedText: String? = null


        override fun onPreExecute() {
            //show progress bar
            val activity = activityReference.get()
            if (activity != null && !activity.isFinishing()) {
                activity.progressBar.setVisibility(ProgressBar.VISIBLE)
                activity.emptyTextView.setText(R.string.finding_ingredients)
            }
        }

        /**
         *
         * @param strings ocr text scanned for ingredients
         * @return a list of ingredients, null if the list is empty or the param is null or empty
         */
        override fun doInBackground(vararg strings: String): List<Ingredient>? {

            val ocrText = strings[0]

            val activity = activityReference.get()
            if (activity != null && !activity.isFinishing()) {

                //get extractor and corrector from singleton
                InciSingleton.load(activity.applicationContext)
                val extractor = InciSingleton.ingredientsExtractor
                val corrector = InciSingleton.textCorrector

                if (ocrText == "")
                    return null

                val startTime = System.currentTimeMillis()

                //correct text
                correctedText = corrector.correctText(ocrText)

                val endCorrectionTime = System.currentTimeMillis()

                //extract ingredients
                val ingredientList = extractor.findListIngredients(correctedText!!)

                val endExtractionTime = System.currentTimeMillis()

                //log execution time
                Log.d("IngredientsExtraction", "correction time: " + (endCorrectionTime - startTime) + " ms")
                Log.d(
                    "IngredientsExtraction",
                    "ingredients extraction time: " + (endExtractionTime - endCorrectionTime) + " ms"
                )

                return ingredientList
            }

            return null
        }

        override fun onPostExecute(ingredients: List<Ingredient>?) {

            val activity = activityReference.get()
            if (activity != null && !activity.isFinishing()) {

                activity.progressBar.setVisibility(ProgressBar.INVISIBLE)

                //if something has been found then set the list of recognized ingredients
                if (ingredients != null && !ingredients.isEmpty()) {
                    val adapter = AdapterIngredient(
                        activity,
                        ingredients
                    )
                    activity.ingredientsListView.setAdapter(adapter)

                    //print analyzed text highlighting the recognized ingredients
                    val analyzedText = SpannableString(this.correctedText)
                    for (ingred in ingredients) {
                        analyzedText.setSpan(
                            BackgroundColorSpan(Color.YELLOW),
                            ingred.startPositionFound!!, ingred.endPositionFound!! + 1, 0
                        )
                    }
                    activity.analyzedTextView.setText(analyzedText)
                } else {
                    activity.ingredientsListView.setAdapter(null)
                    activity.emptyTextView.setText(R.string.no_ingredient_found)
                }
            }
        }
    }
}

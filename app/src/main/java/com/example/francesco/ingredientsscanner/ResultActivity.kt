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
import com.yalantis.ucrop.UCrop
import android.net.Uri
import java.io.File
import android.app.Activity
import android.graphics.ImageDecoder
import android.provider.MediaStore
import java.io.IOException


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

        //get picture from extra
        val pictureUri = Uri.parse(intent.getStringExtra(EXTRA_PICTUREURI))
        if(pictureUri != null) {
            takenPictureView.setOnClickListener { launchImageEditor(pictureUri) }
            analyzeImageUpdateUI(pictureUri)
        }

        //set on click on ingredient launching IngredientDetailsFragment
        ingredientsListView.setOnItemClickListener { parent, _, position, _ ->
            val selectedIngredient = parent.getItemAtPosition(position) as Ingredient

            val inciName = selectedIngredient.inciName
            val description = selectedIngredient.description
            val function = selectedIngredient.function
            val fm = supportFragmentManager
            val detailsFragment = IngredientDetailsFragment.newInstance(inciName, description, function)
            detailsFragment.show(fm, "fragment_ingredient_details")
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

    private fun launchImageEditor(picture: Uri) {
        val resultImageUri = Uri.fromFile(File(cacheDir, "croppedImg.jpg"))

        //Create a new result file and take his Uri
        val options = UCrop.Options()
        options.setHideBottomControls(false)
        options.setFreeStyleCropEnabled(true)
        options.setToolbarTitle(resources.getString(R.string.crop_image_title))
        UCrop.of(picture, resultImageUri)
            .withOptions(options)
            .start(this@ResultActivity)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP && data != null) {
            //get cropped image and update UI
            val resultUri = UCrop.getOutput(data)
            if(resultUri != null) analyzeImageUpdateUI(resultUri)
        }
    }

    /**
     * Show image, extract text from the image, extract ingredients and update UI showing results.
     * @param pictureUri Path of the picture which has to be analyzed
     */
    private fun analyzeImageUpdateUI(pictureUri: Uri) {

        val source = ImageDecoder.createSource(contentResolver, pictureUri)
        val bitmapPicture = ImageDecoder.decodeBitmap(source)

        //set image view
        takenPictureView.setImageBitmap(
            Bitmap.createScaledBitmap(
                bitmapPicture,
                bitmapPicture.width,
                bitmapPicture.height,
                false
            ))

        //set message
        emptyTextView.text = getString(R.string.finding_text)

        //launch text recognizer
        try {
            val image = FirebaseVisionImage.fromFilePath(this, pictureUri)

            val detector = FirebaseVision.getInstance().onDeviceTextRecognizer
            detector.processImage(image)
                .addOnSuccessListener { firebaseVisionText ->
                    val ocrText = firebaseVisionText.text

                    //launch ingredients extraction
                    AsyncIngredientsExtraction(this).execute(ocrText)
                }
                .addOnFailureListener {
                    Log.e(TAG, "text extraction failed")
                }
        } catch (e: IOException) {
            Log.e(TAG, "Failed loading image")
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
            if (activity != null && !activity.isFinishing) {
                activity.progressBar.visibility = ProgressBar.VISIBLE
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
            if (activity != null && !activity.isFinishing) {

                //get extractor and corrector from singleton
                val extractor = AppSingleton.getInstance(activity.applicationContext).ingredientsExtractor
                val corrector = AppSingleton.getInstance(activity.applicationContext).textCorrector

                if (ocrText == "")
                    return null

                val startTime = System.currentTimeMillis()

                //correct text
                val corrected = corrector?.correctText(ocrText) ?: ocrText
                val endCorrectionTime = System.currentTimeMillis()

                //extract ingredients
                val ingredientList = extractor.findListIngredients(corrected)
                val endExtractionTime = System.currentTimeMillis()

                //log execution time
                Log.d("IngredientsExtraction", "correction time: " + (endCorrectionTime - startTime) + " ms")
                Log.d(
                    "IngredientsExtraction",
                    "ingredients extraction time: " + (endExtractionTime - endCorrectionTime) + " ms"
                )

                correctedText = corrected
                return ingredientList
            }

            return null
        }

        override fun onPostExecute(ingredients: List<Ingredient>?) {

            val activity = activityReference.get()
            if (activity != null && !activity.isFinishing) {

                activity.progressBar.visibility = ProgressBar.INVISIBLE

                //if something has been found then set the list of recognized ingredients
                if (ingredients != null && !ingredients.isEmpty()) {
                    val adapter = AdapterIngredient(
                        activity,
                        ingredients
                    )
                    activity.ingredientsListView.adapter = adapter

                    //print analyzed text highlighting the recognized ingredients
                    val analyzedText = SpannableString(this.correctedText)
                    for (ingred in ingredients) {
                        analyzedText.setSpan(
                            BackgroundColorSpan(Color.YELLOW),
                            ingred.startPositionFound!!, ingred.endPositionFound!! + 1, 0
                        )
                    }
                    activity.analyzedTextView.text = analyzedText
                } else {
                    activity.ingredientsListView.adapter = null
                    activity.emptyTextView.setText(R.string.no_ingredient_found)
                }
            }
        }
    }
}

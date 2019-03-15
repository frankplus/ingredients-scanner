package com.example.francesco.ingredientsscanner

import android.content.Context
import android.util.Log
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.example.francesco.ingredientsscanner.inci.*
import java.io.IOException

private const val TAG = "AppSingleton"

class AppSingleton constructor(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: AppSingleton? = null
        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppSingleton(context).also {
                    INSTANCE = it
                }
            }
    }

    val requestQueue: RequestQueue by lazy {
        // applicationContext is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        Volley.newRequestQueue(context.applicationContext)
    }

    private val listInciIngredients: List<Ingredient> by lazy {
        //Load list of ingredients from INCI DB
        val inciDbStream = context.resources.openRawResource(R.raw.incidb)
        try {
            getListIngredients(inciDbStream)
        } catch (e: IOException) {
            Log.e(TAG, "Error reading csv")
            //TODO maybe should print error to user here
            ArrayList<Ingredient>()
        }
    }

    val ingredientsExtractor: IngredientsExtractor by lazy {
        //initialize ingredients extractor
        NameMatchIngredientsExtractor(listInciIngredients)
    }

    val textCorrector: TextAutoCorrection? by lazy {
        //Load wordlist and initialize text corrector
        val wordListStream = context.resources.openRawResource(R.raw.inciwordlist)
        TextAutoCorrection(wordListStream)
    }
}
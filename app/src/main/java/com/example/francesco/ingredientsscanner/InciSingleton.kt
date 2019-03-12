package com.example.francesco.ingredientsscanner

import com.example.francesco.ingredientsscanner.inci.Ingredient
import com.example.francesco.ingredientsscanner.inci.TextAutoCorrection
import com.example.francesco.ingredientsscanner.inci.IngredientsExtractor
import android.content.Context
import android.util.Log
import com.example.francesco.ingredientsscanner.inci.NameMatchIngredientsExtractor
import com.example.francesco.ingredientsscanner.inci.Inci
import java.io.IOException

private const val TAG = "InciSingleton"

object InciSingleton{
    lateinit var ingredientsExtractor: IngredientsExtractor
    lateinit var textCorrector: TextAutoCorrection
    lateinit var listInciIngredients: List<Ingredient>

    fun load(context: Context){
        //Load list of ingredients from INCI DB
        val inciDbStream = context.getResources().openRawResource(R.raw.incidb)
        try {
            listInciIngredients = Inci.getListIngredients(inciDbStream)
        } catch (e: IOException) {
            Log.e(TAG, "Error reading csv")
        }


        //initialize ingredients extractor
        ingredientsExtractor = NameMatchIngredientsExtractor(listInciIngredients)

        //Load wordlist and initialize text corrector
        val wordListStream = context.getResources().openRawResource(R.raw.inciwordlist)
        try {
            textCorrector = TextAutoCorrection(wordListStream)
        } catch (e: IOException) {
            Log.e(TAG, "Error reading word list")
        }
    }
}
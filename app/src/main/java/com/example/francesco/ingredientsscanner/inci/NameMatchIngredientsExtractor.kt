package com.example.francesco.ingredientsscanner.inci

import org.apache.commons.lang3.StringUtils
import java.util.ArrayList

private const val TAG = "IngredientsExtractor"

/**
 * IngredientsExtractor implementation that for each INCI ingredient check if it is contained
 * inside the text. Non alphanumeric characters are ignored.
 * @param listIngredients Total list of ingredients from the INCI DB
 * @author Francesco Pham
 */
class NameMatchIngredientsExtractor(listIngredients: List<Ingredient>) : IngredientsExtractor {

    //list where all ingredients from inci db are stored
    private val listIngredients: List<Ingredient>

    init {
        //copying list so that sorting doesn't affect original list
        this.listIngredients = ArrayList(listIngredients)

        //sort by name length so when we search for ingredients in text we match longer names first
        this.listIngredients.sortedBy { it.inciName.length }
    }


    /**
     * This method extracts ingredients from the ocr text and returns the list of ingredients.
     * @param text The entire OCR text
     * @return List of extracted ingredients, empty list if no ingredients are found
     * @author Francesco Pham
     */
    override fun findListIngredients(text: String): List<Ingredient> {

        val foundIngredients = ArrayList<Ingredient>()

        //remove non alphanumeric characters from text
        //in mapIndexes we store for each character in the stripped text, the original position
        val mapIndexes = IntArray(text.length)
        val strippedTextBuilder = StringBuilder()
        for (i in 0 until text.length) {
            val currentChar = text[i]
            if (currentChar.isLetterOrDigit()) {
                mapIndexes[strippedTextBuilder.length] = i
                strippedTextBuilder.append(currentChar)
            }
        }
        var strippedText = strippedTextBuilder.toString()

        //for each inci ingredient check if it is contained in the text
        for (ingredient in listIngredients) {
            val strippedName = ingredient.strippedInciName

            //search the ingredient inside the text
            val foundAtIndex = strippedText.indexOf(strippedName)
            val foundEndIndex = foundAtIndex + strippedName.length - 1

            if (foundAtIndex >= 0) {
                val foundAtOriginalIndex = mapIndexes[foundAtIndex]
                val foundEndOriginalIndex = mapIndexes[foundEndIndex]

                var found = false

                // for names with nCharThreshold characters or less, check if before and after the name there is
                // a non alphanumeric character (e.g. prevent match of EGG inside PROTEGGE)
                val nCharThreshold = 4
                if (strippedName.length > nCharThreshold) {
                    found = true
                } else if ((foundAtOriginalIndex == 0 || !text[foundAtOriginalIndex - 1].isLetterOrDigit()) &&
                            (foundEndOriginalIndex + 1 >= text.length || !text[foundEndOriginalIndex + 1].isLetterOrDigit() )
                ) {
                    found = true
                }

                if (found) {
                    //found the ingredient
                    ingredient.startPositionFound = foundAtOriginalIndex
                    ingredient.endPositionFound = foundEndOriginalIndex
                    foundIngredients.add(ingredient)

                    //remove the ingredient from text replacing it with whitespaces
                    val replacement = StringUtils.repeat(' ', strippedName.length)
                    strippedText = strippedText.replace(strippedName, replacement)
                }
            }
        }

        //sort by index where the ingredients are found (reconstruct original order)
        foundIngredients.sortedBy { it.startPositionFound }

        return foundIngredients
    }
}

package com.example.francesco.ingredientsscanner.inci

/**
 * Classes that implement this interface should provide a method to extract a list of ingredients from a text
 * @author Francesco Pham
 */
interface IngredientsExtractor {

    /**
     * This method extracts ingredients from the ocr text and returns the list of ingredients.
     * @param text The entire OCR text
     * @return List of extracted ingredients, empty list if no ingredients are found
     * @author Francesco Pham
     */
    fun findListIngredients(text: String): List<Ingredient>
}

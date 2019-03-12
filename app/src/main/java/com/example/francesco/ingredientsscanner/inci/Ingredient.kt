package com.example.francesco.ingredientsscanner.inci

/**
 * Ingredient class which contains the information of the ingredient taken from inci db.
 * @author Francesco Pham
 */
class Ingredient (
    val cosingRefNo: String,
    val inciName: String,
    val description: String,
    val function: String ): Comparable<String> {


    var startPositionFound: Int? = null
    var endPositionFound: Int? = null
    val strippedInciName: String

    init {
        strippedInciName = stripString(inciName)
    }

    override fun compareTo(other: String): Int {
        return inciName.compareTo(other, ignoreCase = true)
    }

    //this method removes all non alphanumeric characters
    private fun stripString(name: String): String {
        return name.replace("[^A-Za-z0-9]".toRegex(), "")
    }
}

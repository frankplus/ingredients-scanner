package com.example.francesco.ingredientsscanner.inci

import android.util.Log
import com.opencsv.CSVIterator

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.ArrayList

import com.opencsv.CSVReader

/**
 * Class for Inci database management
 * The database is in CSV format, using openCSV to parse the file.
 * For more informations about openCSV:
 * http://opencsv.sourceforge.net/
 * @author Francesco Pham
 */

private const val TAG = "Inci"

private const val COL_COSING_REF_NO = 0
private const val COL_INCI_NAME = 1
private const val COL_DESCRIPTION = 6
private const val COL_FUNCTION = 8


fun getListIngredients(inciDbStream: InputStream): ArrayList<Ingredient> {
    val reader = BufferedReader(InputStreamReader(inciDbStream))

    val listIngredients = ArrayList<Ingredient>() //initializing list of ingredients

    //initialize openCSV reader
    val csvReader = CSVReader(reader)

    //skip first line containing field names
    try {
        csvReader.skip(1)
    } catch (e: IOException) {
        Log.e(TAG, "Error skipping first line")
    }

    //for each line in the csv add an Ingredient object to the list
    val iterator = CSVIterator(csvReader)
    iterator.forEach { line ->
        if (line.size > COL_FUNCTION) {
            val element = Ingredient(
                cosingRefNo = line[COL_COSING_REF_NO],
                inciName = line[COL_INCI_NAME],
                description = line[COL_DESCRIPTION],
                function = line[COL_FUNCTION] )

            listIngredients.add(element)
        } else
            Log.d(TAG, "There is an empty line in the database file, line " + csvReader.getLinesRead())
    }

    //closing
    try {
        reader.close()
        csvReader.close()
    } catch (e: IOException) {
        Log.e(TAG, "Error closing csv reader")
    }

    return listIngredients
}
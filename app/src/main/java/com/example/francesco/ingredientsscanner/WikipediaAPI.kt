package com.example.francesco.ingredientsscanner

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONException
import org.json.JSONObject

private const val TAG = "WikipediaAPI"

enum class ResultStatus{RESULT_OK, NOT_FOUND, REQUEST_FAILED}

fun getWikipediaExtract(context: Context, searchQuery: String, listener: (ResultStatus, String?) -> Unit) {

    // Get a RequestQueue
    AppSingleton.getInstance(context.applicationContext).requestQueue

    // Make url
    val url = buildWikiUrlRequest(searchQuery)

    // Make request to wikipedia, searching for ingredient informations.
    val jsonObjectRequest =
        JsonObjectRequest(Request.Method.GET, url, null,
            Response.Listener<JSONObject> { response ->
                try {
                    val query = response.get("query") as JSONObject
                    val pages = query.get("pages") as JSONObject
                    val keys = pages.keys()

                    while (keys.hasNext()) {
                        val key = keys.next()
                        if (key != "-1" && pages.get(key) is JSONObject) {
                            //show wikipedia extract
                            val page = pages.get(key) as JSONObject
                            val wikipediaExtract = page.get("extract") as String
                            listener(ResultStatus.RESULT_OK, wikipediaExtract)
                        } else {
                            //wikipedia page not found
                            listener(ResultStatus.NOT_FOUND, null)
                        }
                    }

                } catch (e: JSONException) {
                    Log.e(TAG, "invalid response from wikipedia")
                    listener(ResultStatus.REQUEST_FAILED, null)
                }
            }, Response.ErrorListener {
                Log.e(TAG, "Could not send request to wikipedia")
                listener(ResultStatus.REQUEST_FAILED, null)
            })

    // Add the request to the RequestQueue.
    AppSingleton.getInstance(context).requestQueue.add(jsonObjectRequest)
}

/**
 * Build URL for the request to wikipedia of an extract of the given ingredient in json format
 * @param inciName Inci name of the ingredient to search on wikipedia
 * @return Url string for the request
 */
private fun buildWikiUrlRequest(inciName: String): String {
    return "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&" +
            "exintro&explaintext&redirects=1&titles=" + inciName.toLowerCase()
}


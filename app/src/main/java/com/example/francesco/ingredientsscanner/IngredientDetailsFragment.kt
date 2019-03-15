package com.example.francesco.ingredientsscanner

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment

import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import kotlinx.android.synthetic.main.fragment_ingredient_details.view.*

import org.json.JSONException
import org.json.JSONObject


private const val TAG = "IngredDetailsFragment"

const val ARG_NAME = "name"
const val ARG_DESCRIPTION = "description"
const val ARG_FUNCTION = "function"


/**
 * Dialog Fragment that shows details about the ingredient selected such as name, description,
 * function and an extract from wikipedia.
 * @author Francesco Pham
 */
class IngredientDetailsFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_ingredient_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inciName = arguments?.run{getString(ARG_NAME)}
        val description = arguments?.run{getString(ARG_DESCRIPTION)}
        val function = arguments?.run{getString(ARG_FUNCTION)}

        //set on click listener on search button
        val searchButton = view.findViewById<Button>(R.id.searchButton)
        searchButton.setOnClickListener {
            val webSearchIntent = Intent(Intent.ACTION_WEB_SEARCH)
            webSearchIntent.putExtra(SearchManager.QUERY, inciName)
            startActivity(webSearchIntent)
        }

        //set close button listener
        val closeButton = view.closeButton
        closeButton.setOnClickListener {dismiss()}

        // show ingredient information
        val nameView = view.inciNameView
        nameView.text = inciName
        val descriptionView = view.descriptionView
        descriptionView.text = description
        val functionView = view.functionView
        functionView.text = function

        val wikipediaView = view.wikipediaView


        val context = activity

        if(context != null && inciName != null) {

            // Get a RequestQueue
            RequestQueueSingleton.getInstance(context.applicationContext).requestQueue

            // Make url
            val url = buildWikiUrlRequest(inciName)

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
                                    wikipediaView.text = wikipediaExtract
                                } else {
                                    //wikipedia page not found
                                    wikipediaView.text = getString(R.string.wikipedia_not_found)
                                }
                            }

                        } catch (e: JSONException) {
                            Log.e(TAG, "invalid response from wikipedia")
                            wikipediaView.text = getString(R.string.wikipedia_failed_request)
                        }
                    }, Response.ErrorListener {
                        Log.e(TAG, "Could not send request to wikipedia")
                        wikipediaView.text = getString(R.string.wikipedia_failed_request)
                    })

            // Add the request to the RequestQueue.
            RequestQueueSingleton.getInstance(context).addToRequestQueue(jsonObjectRequest)

        }
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

    override fun onResume() {
        super.onResume()

        //set dialog fragment size (width and height values in fragment_ingredients_details.xml do not work)
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    companion object {

        /**
         * Method for the fragment creation
         * @param inciName Title to show in the fragment
         * @param description Description of the ingredient
         * @param function Functions of the ingredient
         * @return Returns the fragment created
         */
        fun newInstance(inciName: String, description: String, function: String): IngredientDetailsFragment {
            val fragment = IngredientDetailsFragment()
            val args = Bundle()
            args.putString(ARG_NAME, inciName)
            args.putString(ARG_DESCRIPTION, description)
            args.putString(ARG_FUNCTION, function)
            fragment.arguments = args
            return fragment
        }
    }


}
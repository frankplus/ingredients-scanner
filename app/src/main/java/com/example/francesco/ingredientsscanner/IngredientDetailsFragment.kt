package com.example.francesco.ingredientsscanner

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment

import kotlinx.android.synthetic.main.fragment_ingredient_details.view.*


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

    private lateinit var wikipediaView: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_ingredient_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wikipediaView = view.wikipediaView

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

        val context = activity
        if(context != null && inciName != null) getWikipediaExtract(context, inciName, ::onWikipediaResult)
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

    /**
     * This is called when the request to wikipedia has returned
     * a response and is passed as a listener to getWikipediaExtract
     * @param result The result status of the request
     * @param extract Wikipedia extract returned by the request to wikipedia (null if request failed)
     */
    fun onWikipediaResult(result: ResultStatus, extract: String?){
        when(result) {
            ResultStatus.RESULT_OK -> wikipediaView.text = extract
            ResultStatus.NOT_FOUND -> wikipediaView.text = getString(R.string.wikipedia_not_found)
            ResultStatus.REQUEST_FAILED -> wikipediaView.text = getString(R.string.wikipedia_failed_request)
        }

    }
}
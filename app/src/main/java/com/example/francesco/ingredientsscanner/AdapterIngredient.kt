package com.example.francesco.ingredientsscanner

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter

import com.amulyakhare.textdrawable.TextDrawable
import com.example.francesco.ingredientsscanner.inci.Ingredient
import kotlinx.android.synthetic.main.ingredient_element.view.*

/**
 * Adapter for list view of ingredients after extraction from ocr text
 * @author Francesco Pham
 */
class AdapterIngredient(
    private val context: Context,
    private val ingredients: List<Ingredient> //ingredients to be displayed
) : BaseAdapter() {


    override fun getCount(): Int {
        return ingredients.size
    }

    override fun getItem(position: Int): Any {
        return ingredients[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val rowView = convertView ?: LayoutInflater.from(context).inflate(R.layout.ingredient_element, parent, false)


        //get ingredient name and functions capitalizing first letter
        val ingredient = ingredients[position]
        val inciName = ingredient.inciName
        val function = ingredient.function
        val capitalizedInciName = inciName.substring(0, 1).toUpperCase() + inciName.substring(1).toLowerCase()
        val capitalizedFunction = function.substring(0, 1).toUpperCase() + function.substring(1).toLowerCase()

        //set drawable text, a gmail like letter icon showing first letter of the ingredient
        val firstLetter = Character.toString(inciName[0])
        val drawableLetter = TextDrawable.builder().buildRound(firstLetter, Color.CYAN)
        val image = rowView.letter_icon_view
        image.setImageDrawable(drawableLetter)

        // Set the inci name
        val nameText = rowView.inci_name_view
        nameText.text = capitalizedInciName

        // Set function
        val functionView = rowView.function_view
        functionView.text = capitalizedFunction

        return rowView
    }


}

package com.appsbysha.funfinwin

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class MidWordAdapter(private val items : MutableList<String>, private val numOfLetters: Int,
                     private val dbHelper: DictionaryDbHelper, private val context: Context,
                     mAddWordListener: AddWordListener, mRemoveWordListener: RemoveWordListener ) : RecyclerView.Adapter<MidWordAdapter.WordViewHolder>() {

    var addWordListener = mAddWordListener
    var removeWordListener = mRemoveWordListener


    var hintPos = -1
    var isWin: Boolean = false
    var editWordPosition = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {

        return WordViewHolder(LayoutInflater.from(context).inflate(R.layout.word_row, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {

       /* if(position == 0 || position == itemCount-1) {
            holder.rowLayout.visibility = View.GONE
            return
        }*/
        holder.rowLayout.visibility = View.VISIBLE
        holder.linearLayout.removeAllViews()
        holder.removeWord.setOnClickListener(holder)
        holder.removeWord.visibility = View.INVISIBLE
        holder.swapHint.visibility = View.INVISIBLE
        holder.swapHint.setOnClickListener(holder)


        var prevWord: CharArray? = null
        if(position != 0 && position != itemCount-1)
        {
            if(!isWin)
            holder.removeWord.visibility = View.VISIBLE
        }
        val params = LinearLayout.LayoutParams(UiUtils.dpToPixels(40,context), UiUtils.dpToPixels(40,context))
        params.setMargins(1,0,1,0)

        val word = items[position]
        if(word.isEmpty()){
            holder.swapHint.visibility = View.VISIBLE
            editWordPosition = position
            prevWord = items[position + hintPos].toCharArray()
        }
        val wordArray = word.toCharArray()
        for(i in 0 until numOfLetters) {
            val newLetter = EditText(context)
            newLetter.id =  i
            newLetter.layoutParams = params
            newLetter.gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.CENTER_HORIZONTAL
            newLetter.hint = prevWord?.get(i).toString()
            newLetter.setBackgroundResource(R.drawable.letter_background)
            newLetter.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
            newLetter.setTextColor(ContextCompat.getColor(context,R.color.invalidWord))
            newLetter.filters = arrayOf<InputFilter>(LengthFilter(2))

            if(wordArray.isNotEmpty()) {
                newLetter.setText(wordArray[i].toString())
                newLetter.isEnabled = false
                if(position != 0 && position != itemCount-1) {
                    newLetter.setTextColor(ContextCompat.getColor(context, R.color.validWord))
                    newLetter.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
                }
                else {
                    newLetter.setTextColor(ContextCompat.getColor(context, R.color.gameWord))
                    newLetter.setTypeface(Typeface.SANS_SERIF,Typeface.BOLD)
                }

            }
            else {
                newLetter.isEnabled = true
                newLetter.addTextChangedListener(holder)
            }
            holder.linearLayout.addView(newLetter)

        }





    }

    fun notifyAdapterOfWin() { //... your custom logic

        isWin = true
        notifyDataSetChanged()

    }

    inner class WordViewHolder (view: View) : RecyclerView.ViewHolder(view), TextWatcher, View.OnClickListener {

        var rowLayout: View = view.findViewById<View>(R.id.row_layout)
        var linearLayout: LinearLayout = view.findViewById(R.id.editTextLayout)
        var removeWord: TextView = view.findViewById(R.id.removeWord)
        var swapHint: TextView = view.findViewById(R.id.swapHint)

        private var letterTemp = ""

        override fun afterTextChanged(s: Editable?) {
            var newWord = ""

            s?.let{


                for (index in 0 until linearLayout.childCount) {
                    val letter = linearLayout.getChildAt(index)
                    if (letter is EditText) {
                        letter.removeTextChangedListener(this)
                        if (s !== letter.editableText) {
                            if (s.isBlank()) { //hit backspace
                             letter.setText("")
                            }
                            else {
                                letter.setText(letter.hint)
                            }
                        }
                        else
                        {
                            if (s.length >= 2) {//if more than 1 char
                                val newLetterTemp = s.toString().substring(s.length - 1, s.length)
                                if (newLetterTemp != letterTemp) {
                                    letter.setText(newLetterTemp)
                                } else {
                                    letter.setText(s.toString().substring(0, s.length - 1))
                                }}
                        }
                        newWord += letter.text
                        letter.addTextChangedListener(this)

                    }

                }
                addWordListener.onAddWord(newWord, adapterPosition, hintPos)


            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            letterTemp = s.toString()
        }//store the previous letter

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun onClick(v: View?) {
            when (v?.id) {
                R.id.removeWord -> {
                    removeWordListener.onRemoveWord(adapterPosition, editWordPosition)
                }
                R.id.swapHint -> {
                    hintPos *= (-1)
                    notifyDataSetChanged()
                }
            }
        }


    }


    interface AddWordListener{
        fun onAddWord(word: String, position: Int, hintPos: Int)
    }


    interface RemoveWordListener{
        fun onRemoveWord(position: Int, editWordPosition: Int)
    }

}
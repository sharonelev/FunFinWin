package com.appsbysha.funfinwin

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.word_row.view.*

class MidWordAdapter(
    private val items: MutableList<String>, private val numOfLetters: Int, private val context: Context,
    mAddWordListener: AddWordListener, mRemoveWordListener: RemoveWordListener
) : RecyclerView.Adapter<MidWordAdapter.WordViewHolder>() {

    var addWordListener = mAddWordListener
    var removeWordListener = mRemoveWordListener


    enum class gameStat{
      WIN,
        SHORTER,
        NOT_WIN
    }
    var isWin: gameStat = gameStat.NOT_WIN
    var editWordPosition = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {

        return WordViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.word_row,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {

        holder.rowLayout.visibility = View.VISIBLE
        holder.linearLayout.removeAllViews()
        holder.removeWord.setOnClickListener(holder)
        holder.stepNum.text = position.toString()
        if(position == 0) {
            holder.stepNum.visibility = View.INVISIBLE
        }
        else
            holder.stepNum.visibility = View.VISIBLE



        if (position == 0 || position == itemCount - 1 || isWin == gameStat.WIN) {
                holder.removeWord.visibility = View.INVISIBLE
        }
        else
            holder.removeWord.visibility = View.VISIBLE



        val params = LinearLayout.LayoutParams(
            UiUtils.dpToPixels(LinearLayout.LayoutParams.WRAP_CONTENT, context),
            UiUtils.dpToPixels(LinearLayout.LayoutParams.WRAP_CONTENT, context)
        )
        params.setMargins(1, 0, 1, 0)

        val word = items[position]

        val wordArray = word.toCharArray()
        for (i in 0 until numOfLetters) {
            val newLetter = EditText(context)
            val padding = UiUtils.dpToPixels(1,context)
            newLetter.setPadding(padding,padding,padding,padding)
            newLetter.id = i
            newLetter.layoutParams = params
            newLetter.gravity =
                android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.CENTER_HORIZONTAL
            newLetter.textSize = UiUtils.pixelsToSp(context, 8f)
            if (word.isEmpty()) {
                editWordPosition = position
                newLetter.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
                newLetter.setTextColor(ContextCompat.getColor(context, R.color.invalidWord))
                newLetter.filters = arrayOf<InputFilter>(LengthFilter(2))
                newLetter.isEnabled = true
                newLetter.addTextChangedListener(holder)
                newLetter.isCursorVisible = false

                newLetter.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                newLetter.setBackgroundResource(R.drawable.editable_letter_background)
                newLetter.setOnFocusChangeListener{_, hasFocus ->
                    if(hasFocus){
                        newLetter.setBackgroundResource(R.drawable.focused_letter_background) //use white circle and make orange
                    }
                    else{
                        newLetter.setBackgroundResource(R.drawable.editable_letter_background)
                    }
                    newLetter.setOnKeyListener { _, keyCode, event ->
                        if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                            //backspace
                            if(newLetter.text.isEmpty() && i != 0) { //Don't implement for first digit
                                val prevChar = holder.linearLayout.getChildAt(i - 1) as EditText
                                prevChar.text.clear()
                                prevChar.requestFocus()
                                prevChar.setSelection(prevChar.length())
                            }
                        }
                        false
                    }
                }


            }

           else {
                newLetter.setText(wordArray[i].toString())
                newLetter.setBackgroundResource(R.drawable.valid_letter_background)

                newLetter.isEnabled = false
                if (position != 0 && position != itemCount - 1) {
                    newLetter.setTextColor(ContextCompat.getColor(context, R.color.validWord))
                    newLetter.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
                } else {
                    newLetter.setTextColor(ContextCompat.getColor(context, R.color.gameWord))
                    newLetter.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD)
                }

            }
            holder.linearLayout.addView(newLetter)

        }
        if(position == editWordPosition) {
            holder.linearLayout.getChildAt(0).requestFocus()
        }


    }

    fun notifyAdapterOfWin(win: gameStat) {

        isWin = win
        notifyDataSetChanged()

    }

    inner class WordViewHolder(view: View) : RecyclerView.ViewHolder(view), TextWatcher,
        View.OnClickListener {

        var rowLayout: View = view.row_layout
        var linearLayout: LinearLayout = view.editTextLayout
        var removeWord: TextView = view.removeWord
        var stepNum: TextView = view.stepNum

        private var letterTemp = ""

        override fun afterTextChanged(s: Editable?) {
            var newWord = ""

            s?.let {
                if (s.isBlank()) {
                    return
                }

                for (index in 0 until linearLayout.childCount) {
                    val letter = linearLayout.getChildAt(index)
                    if (letter is EditText) {
                        letter.removeTextChangedListener(this)
                        if (s === letter.editableText) {
                            if (s.length >= 2) {//if more than 1 char
                                val newLetterTemp = s.toString().substring(s.length - 1, s.length)
                                if (newLetterTemp != letterTemp) {
                                    letter.setText(newLetterTemp)
                                } else {
                                    letter.setText(s.toString().substring(0, s.length - 1))
                                }
                            }
                            if(index != numOfLetters - 1) //last char
                            {
                                val nextChar = linearLayout.getChildAt(index + 1) as EditText
                                nextChar.requestFocus()
                                nextChar.setSelection(nextChar.length())
                            }
                        }
                        newWord += letter.text

                        letter.addTextChangedListener(this)

                    }

                }
                if (newWord.length == numOfLetters) {
                    addWordListener.onAddWord(newWord, adapterPosition)
                }

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
                    removeWordListener.onRemoveWord(adapterPosition, editWordPosition, isWin)
                }
            }
        }

    }


    interface AddWordListener {
        fun onAddWord(word: String, position: Int)
    }


    interface RemoveWordListener {
        fun onRemoveWord(position: Int, editWordPosition: Int, win: gameStat)
    }

}
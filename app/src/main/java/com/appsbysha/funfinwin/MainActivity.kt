package com.appsbysha.funfinwin

import android.database.SQLException
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity(), MidWordAdapter.AddWordListener,
    MidWordAdapter.RemoveWordListener {

    private var numOfLetters = 0
    private var maxWords = 10

    lateinit var addWordsRV: RecyclerView
    var midWordAdapter: MidWordAdapter? = null
    private var solutionList: MutableList<String>? = null


    lateinit var addWords: MutableList<String>

    var dbHelper: DictionaryDbHelper = DictionaryDbHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        attachDB()

        addWordsRV = findViewById(R.id.listOfAddWords)



        numOfLetters = 4 //todo add number picker
        maxWords = 10 //todo add number picker

        createGame()


    }

    private fun attachDB() {
        try {
            dbHelper.createDataBase()

        } catch (ioe: IOException) {

            throw Error("Unable to create database")
        }

        try {
            dbHelper.openDataBase()
            // dbHelper.get_table();

        } catch (sqle: SQLException) {

            throw sqle
        }

    }

    private fun createGame() {


        do {
            println(solutionList)

            var firstWord: String
            var lastWord: String
            do {
                firstWord = dbHelper.get_word(numOfLetters)
                Log.i("firstWord", firstWord)
                lastWord = dbHelper.get_word(numOfLetters)
                Log.i("lastWord", lastWord)
            } while (isNeighbors(firstWord, lastWord) && firstWord != lastWord)

            addWords = mutableListOf(firstWord, "", lastWord)

            //todo if computing more than 3 seconds, random new words
            //todo add progress bar and disable new game
            solutionList = solve(mutableListOf(firstWord), lastWord, mutableListOf(firstWord))
            //testList = solve(mutableListOf("dame"), "onyx", mutableListOf("dame"))

        } while (solutionList == null || solutionList?.size ?: 0 > maxWords)

        solutionList?.let {
            Log.i("solution size", it.size.toString())
            for (index in 0 until it.size) {
                //          Log.i(index.toString(), it[index])

            }
        }
        midWordAdapter = MidWordAdapter(addWords, numOfLetters, dbHelper, this, this, this)
        addWordsRV.layoutManager = LinearLayoutManager(this)

        addWordsRV.adapter = midWordAdapter

    }


    private fun solve(
        list: MutableList<String>,
        secondWord: String,
        usedWordsList: MutableList<String>
    ): MutableList<String>? {


        val firstWordArray: CharArray = list.last().toCharArray()
        val secondWordArray: CharArray = secondWord.toCharArray()

        for (index in 0 until numOfLetters) {
            if (firstWordArray[index] == secondWordArray[index])
                continue

            val newWordArray: CharArray = firstWordArray.clone()
            newWordArray[index] = secondWordArray[index]
            val newWord = String(newWordArray)

            var usedWord = false
            for (j in 0 until usedWordsList.size) {
                if (newWord == usedWordsList[j])
                    usedWord = true
            }

            if (usedWord)
                continue

            if (!isWord(newWord))
                continue

            list.add(newWord)
            usedWordsList.add(newWord)

            when {
                newWord == secondWord -> {
                    return list
                }
                solve(list, secondWord, usedWordsList) == null -> {
                    list.remove(newWord)
                }
                else -> return list
            }

        }


        for (index in 0 until numOfLetters) {
            if (firstWordArray[index] == secondWordArray[index])
                continue

            for (i in 'a'..'z') {
                if (i == secondWordArray[index] || i == firstWordArray[index])
                    continue

                val newWordArray: CharArray = firstWordArray.clone()
                newWordArray[index] = i
                val newWord = String(newWordArray)


                var usedWord = false
                for (j in 0 until usedWordsList.size) {
                    if (newWord == usedWordsList[j])
                        usedWord = true
                }
                if (usedWord)
                    continue

                if (!isWord(newWord))
                    continue

                list.add(newWord)
                usedWordsList.add(newWord)

                when {
                    newWord == secondWord -> {
                        return list
                    }
                    solve(list, secondWord, usedWordsList) == null -> { //can't continue from here
                        list.remove(newWord)
                    }
                    else -> return list
                }

            }
        }
        return null

    }

    private fun isWord(word: String): Boolean {

        return dbHelper.check_word(word)

    }

    private fun isNeighbors(
        firstWord: String,
        secondWord: String
    ): Boolean { //only 1 different letter

        if (firstWord == secondWord)
            return false

        val firstWordArray = firstWord.toCharArray()
        val secondWordArray = secondWord.toCharArray()
        var countDiff = 0

        for (index in 0 until numOfLetters) {
            if (firstWordArray[index] != (secondWordArray[index])) {
                countDiff++
                if (countDiff > 1)
                    return false
            }
        }
        return true
    }


    fun newGameClick(view: View) {

        solutionList = null
        createGame()

    }


    override fun onAddWord(word: String, position: Int) {


        var hintPos: Int = when {
            isNeighbors(word, addWords[position + 1]) -> 1
            isNeighbors(word, addWords[position - 1]) -> -1
            else -> {
                Toast.makeText(
                    this,
                    "More than one letter difference than adjacent word",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        if (!dbHelper.check_word(word)) {
            Toast.makeText(this, "Word not in the Scrabble dictionary", Toast.LENGTH_SHORT).show()
            return
        }


        Log.i("onValidWord", word)
        var newWordPosition = position

        if (hintPos == 1) {
            newWordPosition += hintPos
        }
        addWords.add(newWordPosition, word)


        val testWord = addWords[newWordPosition - hintPos * 2]

        if (isNeighbors(word, testWord)) {
            addWords.removeAt(newWordPosition - hintPos) //remove the next blank word
            midWordAdapter?.notifyAdapterOfWin()
            if (solutionList?.size ?: 0 < addWords.size)
                Log.i("done", "there is a shorter solution")
            else
                Log.i("done", "way to go!")
        } else {
            midWordAdapter?.notifyDataSetChanged()
        }


    }


    override fun onRemoveWord(position: Int, editWordPosition: Int) {
        if (position == 0 || position == addWords.size - 1) //should not be able to reach this statement
            return

        addWords[position] = ""

        if (position != editWordPosition) {
            var hintPos = 1
            if (position < editWordPosition)
                hintPos = -1
            val from = min(position, editWordPosition + hintPos)
            val to = max(position, editWordPosition + hintPos)

            for (i in to downTo from)
                addWords.removeAt(i)
        }
        midWordAdapter?.notifyDataSetChanged()
    }


}

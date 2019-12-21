package com.appsbysha.funfinwin

import android.database.SQLException
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.florescu.android.rangeseekbar.RangeSeekBar
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity(), MidWordAdapter.AddWordListener,
    MidWordAdapter.RemoveWordListener {

    private var numOfLetters = 0
    private var minWords = 3
    private var maxWords = 10

    lateinit var addWordsRV: RecyclerView
    var midWordAdapter: MidWordAdapter? = null
    private var solutionList: MutableList<String> = mutableListOf()
    private lateinit var stepRangeBar: RangeSeekBar<Int>
    private lateinit var numOfLettersSpinner: Spinner
    private lateinit var progressBar: ProgressBar
    var firstWord: String = ""
    var lastWord: String = ""

    lateinit var addWords: MutableList<String>

    var dbHelper: DictionaryDbHelper = DictionaryDbHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        attachDB()

        progressBar = findViewById(R.id.createGameProgressBar)
        addWordsRV = findViewById(R.id.listOfAddWords)
        stepRangeBar = findViewById(R.id.stepsRangePicker)
        stepRangeBar.setRangeValues(3, 20)
        numOfLettersSpinner = findViewById(R.id.numOfLettersSpinner)
        numOfLettersSpinner.setSelection(1) // default 4

        numOfLetters = (numOfLettersSpinner.selectedItem as String).toInt()

        minWords = stepRangeBar.absoluteMinValue as Int
        maxWords = stepRangeBar.absoluteMaxValue as Int

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


        progressBar.visibility = View.VISIBLE
        progressBar.bringToFront()

        do {
            println(solutionList)


            do {
                firstWord = dbHelper.get_word(numOfLetters)
                Log.i("firstWord", firstWord)
                lastWord =  dbHelper.get_word(numOfLetters)
                Log.i("lastWord", lastWord)
            } while (isNeighbors(firstWord, lastWord) && firstWord != lastWord)

            addWords = mutableListOf(firstWord, "", lastWord)

            //todo if computing more than 10 seconds, random new words
            //todo add progress bar and disable new game
            val firstWordArray: CharArray = firstWord.toCharArray()
            val secondWordArray: CharArray = lastWord.toCharArray()



            solutionList.clear()
         //   solutionList =
                solve(mutableListOf(firstWord), lastWord, mutableListOf(firstWord), mutableListOf())
            //testList = solve(mutableListOf("dame"), "onyx", mutableListOf("dame"))

        } while (solutionList.isEmpty() || solutionList.size > maxWords || solutionList.size < minWords)


        findViewById<TextView>(R.id.solvedNumOfSteps).text =
            "min num of steps : ${solutionList.size}"

        midWordAdapter = MidWordAdapter(addWords, numOfLetters, this, this, this)
        addWordsRV.layoutManager = LinearLayoutManager(this)

        addWordsRV.adapter = midWordAdapter


        progressBar.visibility = View.GONE

    }


    private fun solve(
        list: MutableList<String>,
        secondWord: String,
        usedWordsList: MutableList<String>,
        shortestList: MutableList<String>
    ): MutableList<String> {


        val firstWord = list.last()
        val firstWordArray: CharArray = firstWord.toCharArray()
        val secondWordArray: CharArray = secondWord.toCharArray()



        for (index in 0 until numOfLetters) {

            if (firstWordArray[index] == secondWordArray[index])
                continue


            val newWordArray: CharArray = firstWordArray.clone()
            newWordArray[index] = secondWordArray[index]
            val newWord = String(newWordArray)


            if (list.size > 1) {
                //don't swap a letter that was just swapped
                var twoWordsBackArray = list[list.size - 2]
                if (firstWordArray[index] != twoWordsBackArray[index])
                    continue
            }

            if (newWord == secondWord) { //full solution
                list.add(newWord)
                usedWordsList.add(newWord)
                if (solutionList.isEmpty() || list.size < solutionList.size) {
                    solutionList.clear()
                    solutionList.addAll(list)
                }
                return list

            } else {

                var usedWord = false
                for (j in 0 until usedWordsList.size) {
                    if (newWord == usedWordsList[j])
                        usedWord = true
                }

                if (usedWord)
                    continue

                if (!isWord(newWord))
                    continue

                if(solutionList.isNotEmpty() && list.size + calcOptimalMin(newWordArray, secondWordArray) >= solutionList.size )
                    continue

                list.add(newWord)
                usedWordsList.add(newWord)

                val solve = solve(list, secondWord, usedWordsList, solutionList).toMutableList()
                if (solve.isEmpty())
                    list.remove(newWord)
                else {
                    usedWordsList.remove(newWord)
                    var p = solve.indexOf(firstWord)
                    if (solve.size - p == calcOptimalMin(firstWordArray, secondWordArray))
                        return solve
                    else {
                        list.clear()
                        list.addAll(0, solve.subList(0,p+1))

                    }
                }
                continue
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

                if (list.size > 1) {
                    //don't swap a letter that was just swapped
                    var twoWordsBackArray = list[list.size - 2]
                    if (firstWordArray[index] != twoWordsBackArray[index])
                        continue
                }

                if (newWord == secondWord) { //full solution
                    list.add(newWord)
                    usedWordsList.add(newWord)
                    if (solutionList.isEmpty() || list.size < solutionList.size) {
                        solutionList.clear()
                        solutionList.addAll(list)
                    }
                    return list

                } else {

                    var usedWord = false
                    for (j in 0 until usedWordsList.size) {
                        if (newWord == usedWordsList[j])
                            usedWord = true
                    }
                    if (usedWord)
                        continue

                    if (!isWord(newWord))
                        continue

                    if(solutionList.isNotEmpty() && list.size + calcOptimalMin(newWordArray, secondWordArray) >= solutionList.size )
                        continue

                    list.add(newWord)
                    usedWordsList.add(newWord)

                    val solve = solve(list, secondWord, usedWordsList, solutionList).toMutableList()
                    println(solve)
                    if (solve.isEmpty()) {
                        list.remove(newWord)

                    }
                    else {
                        usedWordsList.remove(newWord) //the new word leads to solution even if not optimal

                        var p = solve.indexOf(firstWord)
                        if (solve.size - p == calcOptimalMin(firstWordArray, secondWordArray))
                            return solve
                        else {
                            list.clear()
                            list.addAll(0, solve.subList(0,p+1))

                        }
                    }
                    continue
                }

            }

        }

        if(list.size <3)
        {
            println(list)
        }
        return mutableListOf()
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


    fun calcOptimalMin(firstWordArray: CharArray, secondWordArray: CharArray): Int{
        var counter = 1
        for (index in 0 until numOfLetters) {
            if (firstWordArray[index] != secondWordArray[index])
                counter++
        }
        return counter
    }

    fun newGameClick(view: View) {

        numOfLetters = (numOfLettersSpinner.selectedItem as String).toInt()
        minWords = stepRangeBar.selectedMinValue as Int
        maxWords = stepRangeBar.selectedMaxValue as Int
        solutionList = mutableListOf()
        //    addWordsRV.requestFocus()
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


            if (solutionList.size  < addWords.size)
                Toast.makeText(this, "way to go! But... there is a shorter solution", Toast.LENGTH_SHORT).show()
            else
                Toast.makeText(this, "way to go!", Toast.LENGTH_SHORT).show()
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

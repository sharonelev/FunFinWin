package com.appsbysha.funfinwin

import android.content.Context
import android.database.SQLException
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import org.florescu.android.rangeseekbar.RangeSeekBar
import java.io.IOException
import kotlin.math.max
import kotlin.math.min


class MainActivity : AppCompatActivity(), MidWordAdapter.AddWordListener,
    MidWordAdapter.RemoveWordListener {
    private var drawerToggle: ActionBarDrawerToggle? = null
    private lateinit var drawerLeft: LinearLayout
    var drawerLayout: DrawerLayout? = null
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private var numOfLetters = 0
    private var minWords = 3
    private var maxWords = 10
    private var shouldStop = false

    lateinit var addWordsRV: RecyclerView
    var midWordAdapter: MidWordAdapter? = null
    private var solutionList: MutableList<String> = mutableListOf()
    private lateinit var stepRangeBar: RangeSeekBar<Int>
    private lateinit var progressBar: ProgressBar
    var firstWord: String = ""
    var lastWord: String = ""

    lateinit var addWords: MutableList<String>

    private var previousNumView: TextView? = null

    var dbHelper: DictionaryDbHelper = DictionaryDbHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolBar)
        drawerLayout = findViewById(R.id.drawerLayout)
        drawerLeft = findViewById(R.id.leftDrawer)
        setupToolbar()
        setupDrawerToggle()

        attachDB()

        progressBar = findViewById(R.id.createGameProgressBar)
        addWordsRV = findViewById(R.id.wordsRecyclerView)
        stepRangeBar = findViewById(R.id.stepsRangePicker)
        stepRangeBar.setRangeValues(3, 20)
        stepRangeBar.selectedMinValue = 4
        stepRangeBar.selectedMaxValue = 10
        //numOfLettersSpinner = findViewById(R.id.numOfLettersSpinner)
        // numOfLettersSpinner.setSelection(1) // default 4


        var threeTextview = findViewById<TextView>(R.id.threeLetters)
        threeTextview.setOnClickListener { numOfLettersClick(threeTextview) }
        var fourTextview = findViewById<TextView>(R.id.fourLetters)
        fourTextview.setOnClickListener { numOfLettersClick(fourTextview) }
        var fiveTextview = findViewById<TextView>(R.id.fiveLetters)
        fiveTextview.setOnClickListener { numOfLettersClick(fiveTextview) }

        numOfLetters = 4
        numOfLettersClick(fourTextview)
        minWords = stepRangeBar.absoluteMinValue as Int
        maxWords = stepRangeBar.absoluteMaxValue as Int


        drawerLeft.findViewById<TextView>(R.id.newGameDrawerButton)
            .setOnClickListener { newGameClick() }


    }

    private fun numOfLettersClick(view: TextView) {

        previousNumView?.background = null

        view.background = getDrawable(R.drawable.editable_letter_background)
        numOfLetters = view.text.toString().toInt()
        previousNumView = view


    }


    override fun onResume() {
        super.onResume()
        createGame()

    }

    private fun setupToolbar() {

        toolbar = findViewById(R.id.toolBar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        toolbar.visibility = View.VISIBLE


    }

    private fun setupDrawerToggle() {

        drawerToggle = object : ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.app_name,
            R.string.app_name
        ) {
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                val imm =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
            }
        }
        drawerToggle?.let {
            drawerLayout!!.addDrawerListener(it)
        }
        drawerToggle!!.syncState()


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

        var listCreated = false

        progressBar.visibility = View.VISIBLE
        progressBar.bringToFront()

        do {
            println(solutionList)


            do {
                firstWord = dbHelper.get_word(numOfLetters)
                Log.i("firstWord", firstWord)
                lastWord = dbHelper.get_word(numOfLetters)
                Log.i("lastWord", lastWord)
            } while (isNeighbors(firstWord, lastWord) && firstWord != lastWord)

            addWords = mutableListOf(firstWord, "", lastWord)

            //todo if computing more than 10 seconds, random new words
            val timer = object : CountDownTimer(10000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    Log.i("timer", millisUntilFinished.toString())
                }

                override fun onFinish() {
                    if (!listCreated) {
                        shouldStop = true
                    }
                }
            }
            timer.start()
            solutionList.clear()
            //   solutionList =
            solve(mutableListOf(firstWord), lastWord, mutableListOf(firstWord), mutableListOf())
            //testList = solve(mutableListOf("dame"), "onyx", mutableListOf("dame"))

        } while (solutionList.isEmpty() || solutionList.size - 1 > maxWords || solutionList.size - 1 < minWords)

        listCreated = true
        shouldStop = false


        findViewById<TextView>(R.id.showNumStepsDrawerButton).text =
            "min num of steps : ${solutionList.size - 1}"

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

            if (shouldStop)
                return mutableListOf()

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

                if (solutionList.isNotEmpty() && list.size + calcOptimalMin(
                        newWordArray,
                        secondWordArray
                    ) >= solutionList.size
                )
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
                        list.addAll(0, solve.subList(0, p + 1))

                    }
                }
                continue
            }

        }


        for (index in 0 until numOfLetters) {
            if (firstWordArray[index] == secondWordArray[index])
                continue

            if (shouldStop)
                return mutableListOf()

            for (i in 'a'..'z') {

                if (shouldStop)
                    return mutableListOf()

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

                    if (solutionList.isNotEmpty() && list.size + calcOptimalMin(
                            newWordArray,
                            secondWordArray
                        ) >= solutionList.size
                    )
                        continue

                    list.add(newWord)
                    usedWordsList.add(newWord)

                    val solve = solve(list, secondWord, usedWordsList, solutionList).toMutableList()
                    println(solve)
                    if (solve.isEmpty()) {
                        list.remove(newWord)

                    } else {
                        usedWordsList.remove(newWord) //the new word leads to solution even if not optimal

                        var p = solve.indexOf(firstWord)
                        if (solve.size - p == calcOptimalMin(firstWordArray, secondWordArray))
                            return solve
                        else {
                            list.clear()
                            list.addAll(0, solve.subList(0, p + 1))

                        }
                    }
                    continue
                }

            }

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


    private fun calcOptimalMin(firstWordArray: CharArray, secondWordArray: CharArray): Int {
        var counter = 1
        for (index in 0 until numOfLetters) {
            if (firstWordArray[index] != secondWordArray[index])
                counter++
        }
        return counter
    }

    private fun newGameClick() {

        drawerLayout?.closeDrawer(drawerLeft)
        minWords = stepRangeBar.selectedMinValue as Int
        maxWords = stepRangeBar.selectedMaxValue as Int
        solutionList = mutableListOf()
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


            if (solutionList.size < addWords.size)
                Toast.makeText(
                    this,
                    "way to go! But... there is a shorter solution",
                    Toast.LENGTH_SHORT
                ).show()
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

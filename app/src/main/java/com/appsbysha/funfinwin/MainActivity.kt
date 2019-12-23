package com.appsbysha.funfinwin

import android.content.Context
import android.database.SQLException
import android.os.Bundle
import android.os.CountDownTimer
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

    lateinit var gameWordsRecyclerView: RecyclerView
    var midWordAdapter: MidWordAdapter? = null
    private var solutionList: MutableList<String> = mutableListOf()
    private lateinit var stepRangeBar: RangeSeekBar<Int>
    private lateinit var progressBar: ProgressBar
    var firstWord: String = ""
    var lastWord: String = ""

    lateinit var gameWordsList: MutableList<String>

    private var showSolutionSteps = true
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
        showHideClick(findViewById(R.id.showNumStepsDrawerButton))

        attachDB()

        progressBar = findViewById(R.id.createGameProgressBar)
        gameWordsRecyclerView = findViewById(R.id.wordsRecyclerView)
        stepRangeBar = findViewById(R.id.stepsRangePicker)
        stepRangeBar.setRangeValues(3, 20)
        stepRangeBar.selectedMinValue = 3
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


    }

    private fun numOfLettersClick(view: TextView) {

        previousNumView?.background = null

        view.background = getDrawable(R.drawable.settings_number_background)
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
                firstWord = "drip" //dbHelper.get_word(numOfLetters)
                Log.i("firstWord", firstWord)
                lastWord = "crab"//dbHelper.get_word(numOfLetters)
                Log.i("lastWord", lastWord)
            } while (isNeighbors(firstWord, lastWord) && firstWord != lastWord)

            gameWordsList = mutableListOf(firstWord, "", lastWord)

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
            solve(mutableListOf(firstWord), lastWord, mutableListOf(firstWord))
            //testList = solve(mutableListOf("dame"), "onyx", mutableListOf("dame"))

        } while (solutionList.isEmpty() || solutionList.size - 1 > maxWords || solutionList.size - 1 < minWords)

        listCreated = true
        shouldStop = false


        midWordAdapter = MidWordAdapter(gameWordsList, numOfLetters, this, this, this)
        gameWordsRecyclerView.layoutManager = LinearLayoutManager(this)

        gameWordsRecyclerView.adapter = midWordAdapter


        progressBar.visibility = View.GONE

    }


    private fun solve(
        list: MutableList<String>,
        secondWord: String,
        usedWordsList: MutableList<String>
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

                val solve = solve(list, secondWord, usedWordsList).toMutableList()
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

                    val solve = solve(list, secondWord, usedWordsList).toMutableList()
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

    fun newGameClick(view: View) {

        drawerLayout?.closeDrawer(drawerLeft)
        minWords = stepRangeBar.selectedMinValue as Int
        maxWords = stepRangeBar.selectedMaxValue as Int
        solutionList = mutableListOf()
        createGame()

    }

    fun startOverClick(view: View) {

        for(i in gameWordsList.size - 2 downTo 1){
            gameWordsList.removeAt(i)
        }
        gameWordsList.add(1,"")
        midWordAdapter?.notifyAdapterOfWin(false)

    }


    fun solveClick(view: View) {


        gameWordsList.clear()
        gameWordsList.addAll(solutionList)
        midWordAdapter?.notifyAdapterOfWin(true)

    }

    private fun hintClick(view: View) {


    }

    fun showHideClick(view: View) {

        var showTextView = findViewById<TextView>(R.id.showNumStepsDrawerButton)
        showSolutionSteps = !showSolutionSteps
        //get show hide from shared pref
        if (showSolutionSteps) {
            val text = SpannableString(
                getString(
                    R.string.show_number_of_steps,
                    (solutionList.size - 1).toString()
                )
            )
            text.setSpan(ForegroundColorSpan(ContextCompat.getColor(this,R.color.colorPrimary)), 0, 4, 0)
            showTextView.text = text
        } else {

            val text = SpannableString(
                getString(R.string.hide_number_of_steps)
            )
            text.setSpan(ForegroundColorSpan(ContextCompat.getColor(this,R.color.colorPrimary)), 0, 4, 0)
            showTextView.text = text
        }

    }


    override fun onAddWord(word: String, position: Int) {


        var hintPos: Int = when {
            isNeighbors(word, gameWordsList[position + 1]) -> 1
            isNeighbors(word, gameWordsList[position - 1]) -> -1
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
        gameWordsList.add(newWordPosition, word)


        val testWord = gameWordsList[newWordPosition - hintPos * 2]

        if (isNeighbors(word, testWord)) {
            gameWordsList.removeAt(newWordPosition - hintPos) //remove the next blank word
            midWordAdapter?.notifyAdapterOfWin(true)


            if (solutionList.size < gameWordsList.size)
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
        if (position == 0 || position == gameWordsList.size - 1) //should not be able to reach this statement
            return

        gameWordsList[position] = ""

        if (position != editWordPosition) {
            var hintPos = 1
            if (position < editWordPosition)
                hintPos = -1
            val from = min(position, editWordPosition + hintPos)
            val to = max(position, editWordPosition + hintPos)

            for (i in to downTo from)
                gameWordsList.removeAt(i)
        }
        midWordAdapter?.notifyDataSetChanged()
    }


/*    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.newGameDrawerButton -> newGameClick()
            R.id.solveDrawerButton -> solveClick()
            R.id.hintDrawerButton -> hintClick()
            R.id.showNumStepsDrawerButton -> showHideClick()

        }
    }*/


}

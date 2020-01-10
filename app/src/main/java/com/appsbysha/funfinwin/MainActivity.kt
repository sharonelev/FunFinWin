package com.appsbysha.funfinwin

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.database.SQLException
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
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
    private var MIN_LIMIT = 6
    private var timeout = false

    lateinit var gameWordsRecyclerView: RecyclerView
    var midWordAdapter: MidWordAdapter? = null
    private var solutionList: MutableList<String> = mutableListOf()
    private lateinit var stepRangeBar: RangeSeekBar<Int>
    private var progressBar: Dialog? = null
    var firstWord: String = ""
    var lastWord: String = ""
    lateinit var showTextView: TextView

    lateinit var gameWordsList: MutableList<String>

    private var showSolutionSteps = true
    private var previousNumView: TextView? = null

    private lateinit var sharedPrefs: SharedPreferences
    private val NUM_OF_LETTERS_SETTING = "num_of_letters"
    private val MIN_SETTING = "min_steps"
    private val MAX_SETTING = "max_steps"
    private val SHOW_SETTING = "show_solution_min_steps"


    var dbHelper: DictionaryDbHelper = DictionaryDbHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)

        toolbar = findViewById(R.id.toolBar)
        drawerLayout = findViewById(R.id.drawerLayout)
        drawerLeft = findViewById(R.id.leftDrawer)
        setupToolbar()
        setupDrawerToggle()
        showTextView = findViewById(R.id.showNumStepsDrawerButton)
        attachDB()

        progressBar = UiUtils.createProgressDialog(this)
        gameWordsRecyclerView = findViewById(R.id.wordsRecyclerView)
        stepRangeBar = findViewById(R.id.stepsRangePicker)
        stepRangeBar.setRangeValues(3, 20)
        stepRangeBar.setOnRangeSeekBarChangeListener { bar, minValue, maxValue ->
            if (minValue > MIN_LIMIT) {
                stepRangeBar.selectedMinValue = MIN_LIMIT
            }
            var editor = sharedPrefs.edit()
            editor.putInt(MIN_SETTING, minValue)
            editor.putInt(MAX_SETTING, maxValue)
            editor.apply()
        }


        var threeTextview = findViewById<TextView>(R.id.threeLetters)
        threeTextview.setOnClickListener { numOfLettersClick(threeTextview) }
        var fourTextview = findViewById<TextView>(R.id.fourLetters)
        fourTextview.setOnClickListener { numOfLettersClick(fourTextview) }
        var fiveTextview = findViewById<TextView>(R.id.fiveLetters)
        fiveTextview.setOnClickListener { numOfLettersClick(fiveTextview) }

        setSettings()

    }

    private fun setSettings() {
        //Shared preferences

        numOfLetters = sharedPrefs.getInt(NUM_OF_LETTERS_SETTING, 4)
        when (numOfLetters) {
            3 -> numOfLettersClick(findViewById(R.id.threeLetters))
            4 -> numOfLettersClick(findViewById(R.id.fourLetters))
            5 -> numOfLettersClick(findViewById(R.id.fiveLetters))
        }
        showSolutionSteps = sharedPrefs.getBoolean(SHOW_SETTING, false)
        applyShowHide()
        minWords = sharedPrefs.getInt(MIN_SETTING, 3)
        stepRangeBar.selectedMinValue = minWords
        maxWords = sharedPrefs.getInt(MAX_SETTING, 10)
        stepRangeBar.selectedMaxValue = maxWords

    }

    private fun numOfLettersClick(view: TextView) {

        previousNumView?.background = null

        view.background = getDrawable(R.drawable.settings_number_background)
        numOfLetters = view.text.toString().toInt()
        val editor = sharedPrefs.edit()
        editor.putInt(NUM_OF_LETTERS_SETTING, numOfLetters)
        editor.apply()
        previousNumView = view


    }


    override fun onResume() {
        super.onResume()
        if (solutionList.isEmpty())
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


        var solveAsync = SolveAsync()
        solveAsync.execute()


        var handler = Handler()
        handler.postDelayed(
            {
                if (solveAsync.status == AsyncTask.Status.RUNNING) {
                    timeout = true
                    Log.i("createGame", "solve timeout")
                }
            }, 10000


        )


    }

    inner class SolveAsync : AsyncTask<Void, Void, Boolean>() {


        override fun doInBackground(vararg params: Void?): Boolean {
            Log.i("createGame", "new async task")

            do {

                do {
                    firstWord = dbHelper.get_word(numOfLetters)
                    Log.i("firstWord", firstWord)
                    lastWord = dbHelper.get_word(numOfLetters)
                    Log.i("lastWord", lastWord)
                } while (haveMutualLetter(firstWord, lastWord))

                Log.i("createGame", "newWords")

                solutionList.clear()
                gameWordsList = mutableListOf(firstWord, "", lastWord)
                timeout = false
                solve(mutableListOf(firstWord), lastWord, mutableListOf(firstWord))


                println(solutionList)
                if (timeout) {
                    Log.i("createGame", "timeout")
                    return false
                }
            } while (solutionList.isEmpty() || solutionList.size - 1 > maxWords || solutionList.size - 1 < minWords)

            return true
        }


        override fun onCancelled() {
            super.onCancelled()
            Log.i("createGame", "onCancelled")

        }

        override fun onPreExecute() {
            Log.i("createGame", "onPreExecute")

            super.onPreExecute()
            progressBar?.show()
        }

        override fun onPostExecute(solved: Boolean) {
            super.onPostExecute(solved)
            Log.i("createGame", "onPostExecute")

            if (solved) {
                midWordAdapter = MidWordAdapter(
                    gameWordsList,
                    numOfLetters,
                    baseContext,
                    this@MainActivity,
                    this@MainActivity
                )
                gameWordsRecyclerView.layoutManager = LinearLayoutManager(baseContext)

                gameWordsRecyclerView.adapter = midWordAdapter
                progressBar?.hide()
                if (showSolutionSteps)
                    setShowStepsText()

            } else
                createGame()

        }
    }


    fun solve(
        list: MutableList<String>,
        secondWord: String,
        usedWordsList: MutableList<String>
    ): MutableList<String> {

        val firstWord = list.last()
        val firstWordArray: CharArray = firstWord.toCharArray()
        val secondWordArray: CharArray = secondWord.toCharArray()

        for (index in 0 until numOfLetters) {

            if (timeout)
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
                    break
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
                    if (newWord == "pean") {
                        println()
                    }
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

            if (timeout)
                return mutableListOf()

            for (i in 'a'..'z') {

                if (timeout)
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

                    if (!isWord(newWord))
                        continue


                    var usedWord = false
                    for (j in 0 until usedWordsList.size) {
                        if (newWord == usedWordsList[j]) {
                            usedWord = true
                            break
                        }
                    }
                    if (usedWord)
                        continue

                    var neighbour = false
                    for (k in 0 until list.size - 2) {
                        if (isNeighbors(
                                newWord,
                                list[k]
                            )
                        ) //if the new word is a neighbour of an earlier word in the list, it musn't calculate and will reach it later
                        {
                            neighbour = true
                            break
                        }
                    }
                    if (neighbour)
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

    private fun haveMutualLetter(
        firstWord: String,
        secondWord: String
    ): Boolean {
        val firstWordArray = firstWord.toCharArray()
        val secondWordArray = secondWord.toCharArray()
        for (index in 0 until numOfLetters) {
            if (firstWordArray[index] == (secondWordArray[index])) {
                return true
            }
        }
        return false
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

        for (i in gameWordsList.size - 2 downTo 1) {
            gameWordsList.removeAt(i)
        }
        gameWordsList.add(1, "")
        midWordAdapter?.notifyAdapterOfWin(false)
        drawerLayout?.closeDrawer(drawerLeft)

    }


    fun solveClick(view: View) {


        gameWordsList.clear()
        gameWordsList.addAll(solutionList)
        midWordAdapter?.notifyAdapterOfWin(true)
        drawerLayout?.closeDrawer(drawerLeft)

    }

    private fun hintClick(view: View) {


    }

    fun showHideClick(view: View) {

        showSolutionSteps = !showSolutionSteps

        applyShowHide()

    }

    fun applyShowHide() {

        var editor = sharedPrefs.edit()
        editor.putBoolean(SHOW_SETTING, showSolutionSteps)
        editor.apply()
        //get show hide from shared pref
        if (showSolutionSteps) {
            setShowStepsText()
        } else {

            val text = SpannableString(
                getString(R.string.hide_number_of_steps)
            )
            text.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(this, R.color.colorPrimary)),
                0,
                4,
                0
            )
            showTextView.text = text
        }
    }

    private fun setShowStepsText() {
        val text = SpannableString(
            getString(
                R.string.show_number_of_steps,
                (solutionList.size - 1).toString()
            )
        )
        text.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.colorPrimary)),
            0,
            4,
            0
        )
        showTextView.text = text
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

            hideKeyboard(this, currentFocus)

            if (solutionList.size < gameWordsList.size) {
                Toast.makeText(
                    this,
                    "way to go! But... there is a shorter solution",
                    Toast.LENGTH_SHORT
                ).show()
                midWordAdapter?.notifyAdapterOfWin(false)
            } else {
                Toast.makeText(this, "way to go!", Toast.LENGTH_SHORT).show()
                midWordAdapter?.notifyAdapterOfWin(true)

            }
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


    /**
     * Hide keyboard
     */
    private fun hideKeyboard(
        context: Context,
        view: View?
    ) {
        val imm =
            context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

}
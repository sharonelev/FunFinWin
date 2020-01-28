package com.appsbysha.funfinwin

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.database.SQLException
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
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
    private var numOfLetters = 0
    private var minWords = 4
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


        setupToolbar()
        setupDrawerToggle()
        attachDB()

        progressBar = UiUtils.createProgressDialog(this)
        gameWordsRecyclerView = findViewById(R.id.wordsRecyclerView)
        stepRangeBar = findViewById(R.id.stepsRangePicker)
        stepRangeBar.setRangeValues(4, 20)
        stepRangeBar.setOnRangeSeekBarChangeListener { bar, minValue, maxValue ->
            if (minValue > MIN_LIMIT) {
                stepRangeBar.selectedMinValue = MIN_LIMIT
            }
            val editor = sharedPrefs.edit()
            editor.putInt(MIN_SETTING, minValue)
            editor.putInt(MAX_SETTING, maxValue)
            editor.apply()
        }


        threeLetters.setOnClickListener { numOfLettersClick(threeLetters) }
        fourLetters.setOnClickListener { numOfLettersClick(fourLetters) }
        fiveLetters.setOnClickListener { numOfLettersClick(fiveLetters) }

        setSettings()

    }

    private fun setSettings() {
        //Shared preferences

        numOfLetters = sharedPrefs.getInt(NUM_OF_LETTERS_SETTING, 4)
        when (numOfLetters) {
            3 -> numOfLettersClick(threeLetters)
            4 -> numOfLettersClick(fourLetters)
            5 -> numOfLettersClick(fiveLetters)
        }
        showSolutionSteps = sharedPrefs.getBoolean(SHOW_SETTING, false)
        applyShowHide()
        minWords = sharedPrefs.getInt(MIN_SETTING, 4)
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


        setSupportActionBar(toolBar)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        toolBar.visibility = View.VISIBLE


    }

    private fun setupDrawerToggle() {

        drawerToggle = object : ActionBarDrawerToggle(
            this, drawerLayout, toolBar, R.string.app_name,
            R.string.app_name
        ) {
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                val imm =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
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


        val solveAsync = SolveAsync()
        solveAsync.execute()


        val handler = Handler()
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
                instructionsTextView.text = HtmlCompat.fromHtml("Can you get from<br><b>${firstWord.toUpperCase()}</b> to <b>${lastWord.toUpperCase()}</b><br>by changing one letter at a time?", 0)

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
                val twoWordsBackArray = list[list.size - 2]
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
                    usedWordsList.remove(newWord)
                    val p = solve.indexOf(firstWord)
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
                    val twoWordsBackArray = list[list.size - 2]
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

                        val p = solve.indexOf(firstWord)
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

        drawerLayout?.closeDrawer(leftDrawer)
        newGame()

    }

    private fun newGame() {
        minWords = stepRangeBar.selectedMinValue as Int
        maxWords = stepRangeBar.selectedMaxValue as Int
        solutionList = mutableListOf()
        createGame()
    }

    fun startOverClick(view: View) {

        startOver()
        drawerLayout?.closeDrawer(leftDrawer)

    }

    private fun startOver() {
        for (i in gameWordsList.size - 2 downTo 1) {
            gameWordsList.removeAt(i)
        }
        gameWordsList.add(1, "")
        midWordAdapter?.notifyAdapterOfWin(MidWordAdapter.gameStat.NOT_WIN)
    }


    fun solveClick(view: View) {
        gameWordsList.clear()
        gameWordsList.addAll(solutionList)
        midWordAdapter?.notifyAdapterOfWin(MidWordAdapter.gameStat.WIN)
        drawerLayout?.closeDrawer(leftDrawer)

    }

    fun hintClick(view: View) {
        drawerLayout?.closeDrawer(leftDrawer)
        hint()
    }

    private fun hint() {
        if (gameWordsList[1] != solutionList[1]) {
            buildAlertDialog(getString(R.string.pssst) + solutionList[1] + getString(R.string.is_a_valid_word))
            return
        }
        if (gameWordsList[gameWordsList.size - 2] != solutionList[solutionList.size - 2]) {
            buildAlertDialog(
                getString(R.string.pssst) + solutionList[solutionList.size - 2] + getString(
                    R.string.is_a_valid_word
                )
            )
            return
        }
        buildAlertDialog(getString(R.string.right_track))
    }

    fun showHideClick(view: View) {

        showSolutionSteps = !showSolutionSteps

        applyShowHide()

    }

    private fun applyShowHide() {

        val editor = sharedPrefs.edit()
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
            showNumStepsDrawerButton.text = text
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
        showNumStepsDrawerButton.text = text
    }

    private fun makeToast(message: String, position: Int?, yOffset: Int?) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        position?.let { toast.setGravity(it, 0, yOffset ?: 0) }
        toast.show()

    }

    override fun onAddWord(word: String, position: Int) {


        val hintPos: Int = when {
            isNeighbors(word, gameWordsList[position + 1]) -> 1
            isNeighbors(word, gameWordsList[position - 1]) -> -1
            else -> {
                makeToast(getString(R.string.not_adjacent), Gravity.TOP, 50)
                return
            }
        }

        if (!dbHelper.check_word(word)) {

            makeToast(getString(R.string.not_in_dict), Gravity.TOP, 50)
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
                buildEndGameAlertDialog(getString(R.string.done_but_shorter))
                midWordAdapter?.notifyAdapterOfWin(MidWordAdapter.gameStat.SHORTER)
            } else {
                buildEndGameAlertDialog(getString(R.string.way_to_go))
                midWordAdapter?.notifyAdapterOfWin(MidWordAdapter.gameStat.WIN)

            }
        } else {
            midWordAdapter?.notifyDataSetChanged()
        }


    }


    private fun buildEndGameAlertDialog(message: String) {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setMessage(message)
        when (message) {
            getString(R.string.way_to_go) ->
                alertDialog.setButton(
                    DialogInterface.BUTTON_NEGATIVE,
                    getString(R.string.start_over)
                ) { _, _ ->
                    startOver()
                }
            getString(R.string.done_but_shorter) ->
                alertDialog.setButton(
                    DialogInterface.BUTTON_NEGATIVE,
                    getString(R.string.keep_trying)
                ) { _, _ -> }
        }
        alertDialog.setButton(
            DialogInterface.BUTTON_POSITIVE,
            getString(R.string.new_game)
        ) { _, _ ->
            newGame()
        }
        alertDialog.show()


    }


    private fun buildAlertDialog(message: String) {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setMessage(message)
        alertDialog.show()


    }

    override fun onRemoveWord(position: Int, editWordPosition: Int, win: MidWordAdapter.gameStat) {
        if (position == 0 || position == gameWordsList.size - 1) //should not be able to reach this statement
            return

        gameWordsList[position] = ""

        if (win == MidWordAdapter.gameStat.NOT_WIN) {
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
        else {
            midWordAdapter?.notifyAdapterOfWin(MidWordAdapter.gameStat.NOT_WIN)
        }


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
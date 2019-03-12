package com.example.francesco.ingredientsscanner.inci

import android.util.Log

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.ArrayList
import java.util.concurrent.CountDownLatch
import java.util.regex.Pattern

import edu.gatech.gtri.bktree.BkTreeSearcher
import edu.gatech.gtri.bktree.Metric
import edu.gatech.gtri.bktree.MutableBkTree

/**
 * This class corrects automatically the ocr text using a
 * dictionary of different words taken from INCI DB.
 *
 * To see how I generated the dictionary see :
 * https://github.com/frankplus/incidb/tree/master/src/main/java
 *
 * For the search of best matching words in the dictionary I used an implementation of bk-trees,
 * this way the search complexity is logarithmic instead of quadratic.
 * For more information: https://github.com/gtri/bk-tree
 *
 * @author Francesco Pham
 */

private const val TAG = "TextAutoCorrection"

class TextAutoCorrection
/**
 * Constructor which loads the word list into a bk-tree and initialize the searcher
 * @param wordList InputStream from the word list
 * @author Francesco Pham
 */
@Throws(IOException::class)
constructor(wordList: InputStream) {

    //searcher for words in the word list with minimum distance from a given query
    private val searcher: BkTreeSearcher<String>

    init {

        //open word list
        val reader = BufferedReader(InputStreamReader(wordList))

        //declaring metric used for string distance
        val levenshtein = LevenshteinStringDistance()
        val levenshteinDistance = object : Metric<String> {
            override fun distance(x: String, y: String): Int {
                return levenshtein.distance(x, y).toInt()
            }
        }

        //inizialize bk-tree
        val bkTree = MutableBkTree(levenshteinDistance)

        //add each element to the tree
        reader.forEachLine { bkTree.add(it) }

        //initialize searcher
        searcher = BkTreeSearcher(bkTree)
    }


    /**
     * Each word of the text is searched for a best match in the dictionary and
     * if there is a good match the word is corrected
     * @param text The text (taken from ocr) which has to be corrected
     * @return Corrected text
     */
    fun correctText(text: String): String {
        //not correcting words with less than minChars characters
        val minChars = 3

        val formattedText = formatText(text)

        //search all words composed by alphanumeric or hyphen characters
        val pattern = Pattern.compile("[a-zA-Z0-9-]+")
        val matcher = pattern.matcher(formattedText)

        //generate list of words to correct and store their starting position in the text
        val wordsToCorrect = ArrayList<String>()
        val wordsStartPos = ArrayList<Int>()
        while (matcher.find()) {
            val word = matcher.group()
            if (word.length >= minChars) {
                wordsToCorrect.add(word)
                wordsStartPos.add(matcher.start())
            }
        }

        if (wordsToCorrect.size == 0)
            return formattedText //no words to correct

        //correct words
        val correctedWords = correctMultipleWords(wordsToCorrect)

        //substitute the words
        return substituteWords(formattedText, wordsToCorrect, wordsStartPos, correctedWords)
    }


    /**
     * format the text in order to increase the probability to match ingredients in the INCI DB
     * @param text Text to be formatted
     * @return Text formatted
     */
    private fun formatText(text: String): String {
        var newText = text
        //merge the word before and after hyphen + new line (e.g. "ceta- \n ril" into "cetaryl")
        newText = newText.replace(" *- *[\\n\\r]+ *".toRegex(), "")

        //ignoring case by converting all into upper case
        newText = newText.toUpperCase()
        return newText
    }

    /**
     * Each word in the list given is corrected
     * @param words Words to be corrected in a list
     * @return List of corrected words in the same position of the original word in the given list.
     */
    private fun correctMultipleWords(words: List<String>): List<String> {

        //minimum number of words per task (if total number of words is less than this value all words are corrected in one task)
        val minWordsPerTask = 10

        //calculate number of concurrent tasks and number of words per task
        var concurrentTasks: Int
        if (words.size < minWordsPerTask)
            concurrentTasks = 1
        else {
            concurrentTasks = Runtime.getRuntime().availableProcessors()
            if (concurrentTasks > 1)
                concurrentTasks-- //leave a processor for the OS
            if (words.size / concurrentTasks < minWordsPerTask)
                concurrentTasks = words.size / minWordsPerTask
        }
        val wordsPerTask = words.size / concurrentTasks
        Log.d(TAG, "$concurrentTasks tasks. $wordsPerTask words per task.")

        //generate threads
        val latch = CountDownLatch(concurrentTasks)
        val threads = arrayOfNulls<WordsCorrectionThread>(concurrentTasks)
        for (i in 0 until concurrentTasks) {
            //split the word list to be corrected
            val from = i * wordsPerTask
            val to = if (i == concurrentTasks - 1) words.size else (i + 1) * wordsPerTask
            val wordList = words.subList(from, to)

            //start thread
            threads[i] = WordsCorrectionThread(wordList, latch)
            threads[i]?.start() ?: latch.countDown()
        }

        //wait until all threads are finished
        try {
            latch.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
            return words
        }

        //join the result lists
        val correctedWords = ArrayList<String>(words.size)
        for (i in 0 until concurrentTasks) {
            val threadCorrectedWords = threads[i]?.correctedWords
            if(threadCorrectedWords != null)
                correctedWords.addAll(threadCorrectedWords)
        }
        return correctedWords
    }

    /**
     * Thread for words correction
     */
    private inner class WordsCorrectionThread
    /**
     * Constructor
     * @param wordsToCorrect Words to be corrected
     * @param doneSignal CountDownLatch for signalling when the thread has ended
     */
    internal constructor(private val wordsToCorrect: List<String>, private val doneSignal: CountDownLatch) : Thread() {

        val correctedWords: MutableList<String> = ArrayList(wordsToCorrect.size)

        override fun run() {
            //correct each word
            wordsToCorrect.forEach { correctedWords.add(correctWord(it)) }

            doneSignal.countDown()
        }

    }

    /**
     * Correct a single word by searching for the most similar in word list
     * @param word The word to be corrected
     * @return Best candidate word from word list. If no words within maxDistance is found, the same word is returned.
     */
    private fun correctWord(word: String): String {

        //percentage distance below which we substitute the word with the term found in dictionary
        // (during testing i found out that above 30% the confidence does not improve by much and
        // also increases chance of correcting words not related to ingredients)
        val maxNormalizedDistance = 0.30

        //Searches the tree for elements whose distance satisfy max distance
        // for the demostration of the distance upper bound see:
        // https://github.com/frankplus/incidb/blob/master/maxNormalizedDistanceFormulaDim.jpg
        val distanceUpperBound = (word.length * maxNormalizedDistance / (1 - maxNormalizedDistance)).toInt()
        val matches = searcher.search(word, distanceUpperBound)

        //find the word with minimum distance
        var minDistance = java.lang.Double.MAX_VALUE
        var closest = ""
        for (match in matches) {

            //if same word is found, no need to continue
            if (match.distance == 0) {
                closest = word
                break
            }

            //calculate normalized distance
            val wordLength = word.length
            val matchLength = match.match.length
            val normalizedDistance = match.distance.toDouble() / Math.max(wordLength, matchLength)

            //only if normalized distance satisfy max distance put it into closest match
            if (normalizedDistance <= maxNormalizedDistance && normalizedDistance < minDistance) {
                minDistance = normalizedDistance
                closest = match.match
            }
        }

        //If no words within maxNormalizedDistance is found, the same word is returned.
        return if (closest == "") word else closest
    }

    /**
     * Method for substituting some words in a string
     * @param text The original string from which we want to substitute the words
     * @param wordsToSubstitute Original words which have to be substituted
     * @param wordsPositions Positions in the original string of the words
     * @param newWords The replacing words
     * @return The resulting string after words substitution
     */
    private fun substituteWords(
        text: String,
        wordsToSubstitute: List<String>,
        wordsPositions: List<Int>,
        newWords: List<String>
    ): String {

        //in this array we store the mapping between indexes of original text and the corrected text.
        val mapIndexes = IntArray(text.length)
        for (i in 0 until text.length) mapIndexes[i] = i

        var correctedText = text

        //construct corrected text by substituting the corrected words
        for (i in wordsToSubstitute.indices) {
            val oldWord = wordsToSubstitute[i]
            val correctedWord = newWords[i]
            if (correctedWord != oldWord) {

                Log.d(TAG, "word $oldWord corrected with $correctedWord")

                //get start-end positions of the word to replace
                val startPos = wordsPositions[i]
                val endPos = startPos + oldWord.length

                //substitute with the corrected word
                var newText = ""
                if (startPos > 0) newText = correctedText.substring(0, mapIndexes[startPos])
                newText += correctedWord
                if (endPos < text.length) newText += correctedText.substring(mapIndexes[endPos])

                correctedText = newText

                //shift map indexes by the difference of length between the old word and corrected word
                val shift = correctedWord.length - oldWord.length
                val from = startPos + Math.min(correctedWord.length, oldWord.length)
                for (j in from until text.length)
                    mapIndexes[j] += shift
            }
        }

        return correctedText
    }
}

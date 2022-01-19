package com.perrochon.wolver

import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

const val dirPath = "C:\\AndroidStudioProjects\\Wolver\\app\\src\\main\\java\\com\\perrochon\\wolver\\"

const val GRAY = 0
const val YELLOW = 1
const val GREEN = 2

val iToACache = Array<IntArray>(243){ IntArray(5){-1} }

val G_SIZE = 12972
val A_SIZE = 2315

val allGuesses = Array<String>(G_SIZE){""}
val allAnswers = Array<String>(A_SIZE){""}
val guessMap = HashMap<String,Int>()
val answerMap = HashMap<String, Int>()

val scoresCacheSize = (G_SIZE * A_SIZE)
val scoresCache = IntArray(scoresCacheSize){-1}

val gAnswers = Array(5) {Array<ArrayList<Int>?>(243){null}}
val gSorter = Array(5) {Array(243){arrayOf(0,0)}}

//val gGuesses = Array(5) {Array<ArrayList<Int>?>(243){null}}

//val minMaxes = IntArray(5){ 0}
val words = IntArray(5){-1}

var tStart = 0L

var cTemp1 = 0L
var tTemp1 = 0L
var cTemp2 = 0L
var tTemp2 = 0L


// Job Options
const val S1 = 0
const val W1 = 1
const val W2 = 2
const val W3 = 3
const val PX = 4 // Process all, choose minimal Max Bucket
const val PA = 5 // Process all, choose minimal mean.

// Job Configuration
val job = S1 // See job options above

var startLevel = 1 // 1: Just one level deep, 2 takes an hour, 3 takes days, 4 a lot longer.
val printLevels = 1 // console output and log. 1, unless debugging
val loopCutOff = G_SIZE // in loops, stop output after that many

val guess1 = "TOILE" // used for S1 and W1, W2, w3
val result1 = intArrayOf(GRAY, GRAY, YELLOW, GRAY, GRAY) //W1, W2, W2
val guess2 = "TONIC"
val result2 = intArrayOf(YELLOW, GREEN, YELLOW, YELLOW, GRAY)
val guess3 = "CRUDE"
val result3 = intArrayOf(GRAY, GREEN, GRAY, GRAY, GRAY)

var criteria = 0 // How to choose guess: (0: minimal max bucket, 1: minimal  mean). PA/PX will override this value

fun processAllJob() {
    l_(startLevel,f_("ProcessAll - startlevel %d criteria %d (0: max, 1: mean)", startLevel, criteria))

    val r = processAll(IntArray(G_SIZE){it}, IntArray(A_SIZE){it}, startLevel, printLevels)

    l_(startLevel+1,f_("Processed %d guesses to level %d, use %s with max %d and mean %d ", allAnswers.size, startLevel, allGuesses[r[0]], r[1], r[2]))
    l_(startLevel,f_("Choose %s with max %d mean %.2f",
        allGuesses[r[0]], r[1], r[2].toFloat()/100))

}

fun filterGuesses(_guess: String, _r: IntArray, _a: IntArray) : IntArray {
    val r = _r[0]*81+_r[1]*27+_r[2]*9+_r[3]*3+_r[4]
    val answers = _a.filter { scoresCache[guessMap[_guess]!! * A_SIZE + it] == r }.toIntArray()
    l_(startLevel, f_(" %s", _guess))
    l_(startLevel, f_("%s %d %s", r_(r), answers.size, iToS_(answers.toCollection(ArrayList()))))
    println()
    return answers
}

fun main() {

    tStart = System.currentTimeMillis()

    l_(startLevel+1, "    Wolver - a Wordle Solver\n")
    l_(startLevel+1, "  " + Calendar.getInstance().time.toString())

    loadData() // Performance: 30 ms
    scoreAll() // performance: 2100/330 First Run (fill cache)/ Second Run (from cache)
    scoreAll() // run twice to measure time
    loadCache()

    tStart = System.currentTimeMillis() // exclude startup time

    when (job) {
        S1 -> {
            val (max, mean, max2) = processGuess( guessMap[guess1]!!, IntArray(G_SIZE){it}, IntArray(A_SIZE){it}, startLevel, printLevels)
            l_(startLevel+1,f_("Processed guess %s to level %d, max bucket is %d", guess1, startLevel, max))

        }
        W1 -> {
            var answers = filterGuesses(guess1, result1, IntArray(A_SIZE){it})
            val (max, mean, max2) = processGuess( guessMap[guess1]!!, IntArray(G_SIZE){it}, answers, startLevel, printLevels)
            l_(startLevel+1,f_("Processed guess %s to level %d, max bucket is %d", guess1, startLevel, max))

        }
        W2 -> {
            var answers = filterGuesses(guess1, result1, IntArray(A_SIZE){it})
            answers = filterGuesses(guess2, result2, answers)
            val (max, mean, max2) = processGuess( guessMap[guess2]!!, IntArray(G_SIZE){it}, answers, startLevel, printLevels)

        }
        W3 -> {
            var answers = filterGuesses(guess1, result1, IntArray(A_SIZE){it})
            answers = filterGuesses(guess2, result2, answers)
            answers = filterGuesses(guess3, result3, answers)
            val (max, mean, max2) = processGuess( guessMap[guess3]!!, IntArray(G_SIZE){it}, answers, startLevel, printLevels)

        }
        PX -> {
            criteria = 0
            processAllJob()
        }
        PA -> {
            criteria = 1
            processAllJob()
        }

    }

/*


    val r2 = result2[0]*81+result2[1]*27+result2[2]*9+result2[3]*3+result2[4] // TODO use loop and trit..
    answers = answers.filter { scoresCache[guessMap[guess2]!! * A_SIZE + it] == r2 }.toIntArray()
    println(guess2)
    print(r_(r2))
    println(iToS_(answers.toCollection(ArrayList())))

    val r3 = result3[0]*81+result3[1]*27+result3[2]*9+result3[3]*3+result3[4] // TODO use loop and trit..
    //answers = answers.filter { scoresCache[guessMap[guess3]!! * A_SIZE + it] == r3 }.toIntArray()
    println(guess3)
    print(r_(r3))
    println(iToS_(answers.toCollection(ArrayList())))

    println("\n\n"+answers.size+"\n")
*/

    //val (max, mean, max2) = processGuess( guessMap[guess2]!!, answers, startLevel, printLevels)


            // Performance:
            // 169s start
            // 264? Create BucketAnswers only on demand
            // 105 Memoize and Create BucketAnswers only on demand
        // Total process time: 112,321 Score Calls: 30,030,180 (1,679) / 3,063,080,675 (34,187)

    // 21691 | Processed 2315 guesses to level 2, minMax bucket is 168
    // 21691 | Total: 21,691,703ms | Score: 386,146M/30M 9,374s/1s (24ns) | 12,972 (356ms)
    // 22ks for 13k guesses for 1672ms/guess.

    // processAll()

    p_()
}

fun processAll(guesses: IntArray, answers: IntArray, level : Int, pl_: Int ) : IntArray {
    // Change return order so that max/mean are [0][1] for consistenc. next word can be 3, it is not really needed at lower levels.


    if (level < 0) { println("*** Went too deep"); return intArrayOf(-1,-1) }

    var bestResult = intArrayOf(answers.size, answers.size)
    var bestWord = -2

    for (i  in 0 .. guesses.size-1) {

        // Skip some to manually continue after aborts
        if (level == startLevel && i < 0) continue

        // Progress logging
        val start = if (pl_ > 0  && (guesses[i] < loopCutOff || level == startLevel)) System.currentTimeMillis() else 0
        if (pl_ > 0  && (guesses[i] < loopCutOff || level == startLevel)) { print(f_("  %4d %s...",guesses[i], allGuesses[guesses[i]]))}

        val result  = processGuess(guesses[i], guesses, answers, level, pl_-1)

        if ((result[criteria] < bestResult[criteria]) ||
            ((result[criteria] == bestResult[criteria]) && (result[1-criteria] < bestResult[1-criteria]))) {
            bestResult = result
            bestWord = guesses[i]
        }

        if (pl_ > 0  && (i < loopCutOff || level == startLevel)) {
            val d = System.currentTimeMillis()- start
            l_(level,f_("%4d %4d %s %d %d %d %d (%dms)", i, guesses[i], allGuesses[guesses[i]], result[2], result[3], result[0], result[1], d))
        }

    }
    if (pl_ > 0) { // Production runs
        l_(level,f_("Choose %s with max %d mean %.2f max2 %d mean2 %.2f",
            allGuesses[bestWord], bestResult[2], bestResult[3].toFloat()/100, bestResult[0], bestResult[1].toFloat()/100))
    }
    return intArrayOf(bestWord)+bestResult
}

fun processGuess(g: Int, guesses: IntArray, answers: IntArray, level : Int, pl_: Int ) : IntArray {

    if (level < 0 || answers.size == 0) { println("*** Went too deep"); return intArrayOf(A_SIZE,A_SIZE,A_SIZE,A_SIZE) }

    val bAnswers = gAnswers[level]
    bAnswers.fill(null)

    var maxf = 0
    var bCount = 0

    // compute all buckets
    for (i in 0 .. answers.size-1) {

        val a=answers[i]
        val c = scoresCache[g * A_SIZE + a]

        var l = bAnswers[c]?.size ?: 0
        if (l == 0) {
            bAnswers[c] = arrayListOf(a)
            bCount++
        } else {
            bAnswers[c]!!.add(a)
        }
        l++

        if (l > maxf) { maxf = l; }
    }

    if (pl_ > 0) {
        //l_(level,f_("  %s with %d answers max bucket size is %d (level %d)", allGuesses[g], answers.size, maxf, level))
    }

    var meanf = 0 // TODO rename? Weighted Average of Bucket Sizes, Expected Bucket Size at that level
    for (i in 0..242) {
        val l = bAnswers[i]?.size ?: 0
        meanf += l * l
    }
    meanf = (meanf + G_SIZE/2) * 100 / answers.size

    if(pl_ > 0) {
        var c = 0
        for (i in 0..242) {
            val l = bAnswers[i]?.size ?: 0
            if (l > 0 && (c++ < loopCutOff || level == startLevel)) {
                l_(level,f_("   %3d %s %3d %s",i,r_(i),l, iToS_(bAnswers[i])))
            }
        }
    }

    if (level == 1) { return intArrayOf(maxf, meanf, maxf, meanf) } // Reached Max Recursion, let's stop here

    // go deeper

    //val bGuesses = gGuesses[level]
    //bGuesses.fill(null)

    // We are sorting the arrays, because in some cases we can prune the search space // TODO: Encode c into bAnswers, and sort directly
    // Total time for sorting is 1% of 1% of total run time for level 2 full search.

    // We do this complicated, because if we just sort bAnswers, we lose the c. We only need c for logging, though.
    val bSorter = gSorter[level]
    for (i in 0 .. 242) {bSorter[i][0] = i; bSorter[i][1]= bAnswers[i]?.size ?: 0}
    bSorter.sortWith(Comparator{o1,o2 -> o1[1].compareTo(o2[1]) })

    var maxResult = intArrayOf(-1,0,0)
    var mean = 0
    for (c in 242 downTo 0) {
        val i = bSorter[c][0]
        val l = bSorter[c][1] // bAnswers[i]?.size ?: 0
        //if (l <= maxNext) { if (pl_ > 0) l_(level,"   pruning, next list has "+l); break} // With pruning, mean will become inaccurate


        when (l) {
            0 -> {
                //if (pl_ > 0 && (c < listCutOff || level == startLevel)) {
                    //l_(level,f_("   %3d %s %3d-> 0",i,r_(i),l))
                //}
            }
            else -> {

                //l_(level,f_("%d %d", c, bSorter[c][0]))

                val foo = guesses//.filter{ hardFilter(g, bSorter[c][0], it)}.toIntArray() // TODO Niceify and parameterize
                //l_(level,f_("Guesses %d Foo %d", guesses.size, foo.size))


                if (pl_ > 0 && (c < loopCutOff || level == startLevel)) {
                    //println(f_("................%3d %s %3d/%d->...%s",i,r_(i),l, foo.size, iToS_(bAnswers[i])))
                }
                if (level > 1) {
                    val result = processAll(foo, bAnswers[i]!!.toIntArray(), level-1, pl_-1)
                    mean = mean + l * result[1] // sizeof bucket * mean for that bucket
                    if (result[1] >= maxResult[1]) {
                        maxResult = result
                    }
                    if (pl_ > 0 && (c < loopCutOff || level == startLevel)) {
                        l_(level,f_("   %3d %s %3d->%2d %d %d  [%s]",i,r_(i),l, maxResult[1], maxResult[2], mean, allGuesses[maxResult[0]]))
                    }
                }
            }
        } // when
    }

    mean = mean * 100 / answers.size

    if (pl_ > 0) {
        l_(level,f_("  %s with %d answers max bucket size is %d mean is %d (level %d)", allGuesses[g], answers.size, maxResult[1], maxResult[2], level+1))
    }
    return intArrayOf(maxResult[1], mean, maxf, meanf)
}

// Arrays (re-)used by score. Moved out for performance
val aUsed = BooleanArray(5) { false }
val gUsed = BooleanArray(5) { false }
val trit = intArrayOf(81, 27, 9, 3, 1)

fun score(_g: Int, _a: Int) {
    // Scores guess _g against answer _a and puts it in scoresCache
    // Score is a base 3 encoding of GREEN(2)/YELLOW(1)/GRAY(0), stored as an Int.

    // PERF: computing the key is a very frequent operation. shl 12 didn't make much difference compared to *
    val key = _g * A_SIZE + _a
    var score = scoresCache[key]

    if (score == -1) {
        // PERF: this branch is only used warming the cache, once per run, so not worth optimizing.

        score = 0
        val guess = allGuesses[_g]
        val answer = allAnswers[_a]
        aUsed.fill(false)
        gUsed.fill(false)

        for (i in 0..4) {
            if (guess[i] == answer[i]) {
                score += GREEN * trit[i]
                aUsed[i] = true
                gUsed[i] = true
            }
        }

        for (g in 0..4) {
            if (!gUsed[g]) {
                for (a in 0..4) {
                    if (!aUsed[a] && (guess[g] == answer[a])) {
                        score += YELLOW * trit[g]
                        aUsed[a] = true
                        gUsed[g] = true
                        break
                    }
                }
            }
        }
        scoresCache[key] = score
    }
}

fun hardFilter(_g: Int, _r: Int, _c: Int): Boolean{

    val g = allGuesses[_g] // Last guess
    val r = iToACache[_r] // result as an IntArray
    val c = allGuesses[_c] // candidate guess

    // Greens
    for (i in 0..4) {
        if (r[i] == GREEN && (g[i] != c[i])) {
            return false
        }
    }

    // Yellows
    for (i in 0..4) {
        if (r[i] == YELLOW) {
            var found = false
            for (j in 0..4) {
                if (c[j] == g[i]) found = true
            }
            if (!found) return false
        }
    }
    return true
}

fun loadCache() {
    for (i in 0 .. 242) {
        var r = i
        for (j in 4 downTo 0 ) {
            val v = r % 3
            r = r / 3
            iToACache[i][j] = when (v) {
                0 -> GRAY
                1 -> YELLOW
                2 -> GREEN
                else -> -1
            }
        }
        //l_(startLevel, " " + i + r_(i)+iToACache[i].contentToString())
    }
}

fun scoreAll() {
    // Loads scoresCache with all scores
    // Can be used for performance testing of score() cold vs warm (run it twice)
    print("      scoreAll: ...")
    val start = System.currentTimeMillis()
        for (g in 0 .. G_SIZE-1) {
            for (a in 0.. A_SIZE-1) {
                score(g, a)
            }
        }
    val d = System.currentTimeMillis()-start
    l_(startLevel+1,f_("  Scores: %,d (%dms) %.1fns per call", G_SIZE * A_SIZE, d, d.toFloat()*1000000/(G_SIZE * A_SIZE)))
}

fun loadData() {
    // Load guesses and answers from files. Called just once
    // TODO: Make more robust, read file to list, then convert to array. Runs only once and is fast already.
    print("      loadData: ...")
    val start = System.currentTimeMillis()
    var g = 0
    var a = 0

    File(dirPath + "guesses.txt").forEachLine {
        guessMap[it] = g
        allGuesses[g++] = it
    }

    File(dirPath + "answers.txt").forEachLine {
        answerMap[it] = a
        allAnswers[a++] = it
    }

    if (g != G_SIZE || a != A_SIZE) {
        l_(startLevel+1,f_("Arrays are too big: Set to Guesses: %,d Answers: %,d", g, a))
        System.exit(-27)
    }
    val d = System.currentTimeMillis()-start
    l_(startLevel+1,f_("  Guesses: %,d Answers: %,d (%dms)", g, a, d))
}


//
// Helper functions for output, profiling, and debugging
//
fun r_(_r: Int) : String {
    // Convert int answer pattern to string
    val output = CharArray(5){'+'}
    var r = _r
    for (i in 4 downTo 0 ) {
        val v = r % 3
        r = r / 3
        output[i] = when (v) {
            0 -> "\u1b1c"[0]
            1 -> "\u2bc0"[0]
            2 -> "\u25a3"[0]
            else -> '-'
        }
    }
    return String(output)
}

fun iToS_(_a: ArrayList<Int>?) : String {
    // Convert list of up to 20 answer indices to concatenated answer strings.
    var result = ""
    if (_a != null){
        for (i in _a.indices) {
            result = result + allAnswers[_a[i]] + " "
            if (i==7) {result = result + "...";  break}
        }
    }
    return result
}

fun l_(_l: Int, _s: String) {
    // Log s to output and file
    var indent = "";
    for (i in 0 .. (startLevel - _l)) indent = indent + "    "
    val ds = f_("%4d |%d|%s %s", ((System.currentTimeMillis()- tStart)/1000),_l, indent, _s)
    println("\r"  + ds)
    File(dirPath + "output3.txt").appendText(ds + "\n")
}

fun p_() {
    // Display Profile Information
    val k = 1000
    val M = 1000000
    val s = f_("Total: %,dms | %,d (%,dms)|  %,d (%,dms)",
        (System.currentTimeMillis()- tStart), cTemp1, tTemp1/M, cTemp2, tTemp2/M)
    val ds = "\n" + ((System.currentTimeMillis()- tStart)/k) + " | " + s
    l_(startLevel, ds)
    //println("\r"+ ds)
    //File(dirPath + "output3.txt").appendText(ds + "\n")
}

fun f_(_s: String, vararg _v: Any?):String {return String.format(_s, *_v)}
    // Shorten String.format()
        //  Stringbuilder??
        //  Kotlin String templates "Guess ${g}" when no parameter formatting is required


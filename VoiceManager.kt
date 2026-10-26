package com.example.calculator

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import java.math.BigDecimal

fun MainActivity.startVoiceRecognition() {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "he-IL")
        putExtra(RecognizerIntent.EXTRA_PROMPT, "אמור תרגיל (לדוגמה: חמש מאות אלף כפול שתיים)")
    }
    try {
        startActivityForResult(intent, VOICE_REQUEST_CODE)
    } catch (e: Exception) {
        Toast.makeText(this, "זיהוי קולי לא נתמך במכשיר זה", Toast.LENGTH_SHORT).show()
    }
}

// מנוע פענוח דיבור מתקדם התומך בכל המספרים בעברית (כולל מאות, אלפים, מיליונים, מיליארדים, שברים ופעולות מדעיות)
fun MainActivity.parseHebrewVoiceToMath(rawSpoken: String): String {
    var s = rawSpoken.lowercase()
        .replace(",", "")
        .replace("״", "")
        .replace("\"", "")
        .replace("'", "")

    val phraseOps = listOf(
        "שלושה רבעים" to "0.75", "שלוש רבעים" to "0.75",
        "שני שליש" to "0.666666", "שתי שליש" to "0.666666",
        "חצי מיליון" to "500000", "רבע מיליון" to "250000",
        "חצי אלף" to "500", "מאה אלף" to "100000", "מאתיים אלף" to "200000",
        "שלוש מאות אלף" to "300000", "ארבע מאות אלף" to "400000", "חמש מאות אלף" to "500000",
        "שש מאות אלף" to "600000", "שבע מאות אלף" to "700000", "שמונה מאות אלף" to "800000", "תשע מאות אלף" to "900000",
        "עשרת אלפים" to "10000", "עשרה אלף" to "10000", "עשר אלף" to "10000",
        "עשרים אלף" to "20000", "שלושים אלף" to "30000", "ארבעים אלף" to "40000",
        "חמישים אלף" to "50000", "שישים אלף" to "60000", "שבעים אלף" to "70000",
        "שמונים אלף" to "80000", "תשעים אלף" to "90000",
        "שלושת אלפים" to "3000", "ארבעת אלפים" to "4000", "חמשת אלפים" to "5000",
        "ששת אלפים" to "6000", "שבעת אלפים" to "7000", "שמונת אלפים" to "8000", "תשעת אלפים" to "9000",
        "שלוש מאות" to "300", "ארבע מאות" to "400", "חמש מאות" to "500",
        "שש מאות" to "600", "שבע מאות" to "700", "שמונה מאות" to "800", "תשע מאות" to "900",
        "אחת עשרה" to "11", "אחד עשר" to "11", "שתים עשרה" to "12", "שנים עשר" to "12",
        "שלוש עשרה" to "13", "שלושה עשר" to "13", "ארבע עשרה" to "14", "ארבעה עשר" to "14",
        "חמש עשרה" to "15", "חמישה עשר" to "15", "שש עשרה" to "16", "שישה עשר" to "16",
        "שבע עשרה" to "17", "שבעה עשר" to "17", "שמונה עשרה" to "18", "שמונה עשר" to "18",
        "תשע עשרה" to "19", "תשעה עשר" to "19",
        "פתח סוגריים" to " ( ", "סגור סוגריים" to " ) ",
        "שורש ריבועי של" to " √( ", "שורש של" to " √( ", "שורש" to " √( ",
        "בריבוע" to " ^2 ", "בשלישית" to " ^3 ", "ברביעית" to " ^4 ",
        "בחזקת" to " ^ ", "בחזקה" to " ^ ",
        "לחלק ל" to " ÷ ", "חלקי" to " ÷ ", "מחולק ב" to " ÷ ", "לחלק" to " ÷ ",
        "כפול" to " × ", "פעמים" to " × ", "מוכפל ב" to " × ",
        "פלוס" to " + ", "ועוד" to " + ", "חיבור" to " + ",
        "פחות" to " − ", "מינוס" to " − ", "חיסור" to " − ",
        "אחוזים" to " % ", "אחוז" to " % ",
        "סינוס" to " sin( ", "קוסינוס" to " cos( ", "טנגנס" to " tan( ",
        "לוגריתם" to " log( ", "לוג" to " log( ", "לן" to " ln( ",
        "פאי" to " π ", "עצרת" to " ! ", "נקודה" to " . ", "שווה" to ""
    )

    for ((phrase, rep) in phraseOps) {
        s = s.replace(phrase, rep)
    }

    val unitWords = mapOf(
        "אפס" to 0L, "אחת" to 1L, "אחד" to 1L, "שתיים" to 2L, "שניים" to 2L, "שתי" to 2L, "שני" to 2L,
        "שלוש" to 3L, "שלושה" to 3L, "ארבע" to 4L, "ארבעה" to 4L, "חמש" to 5L, "חמישה" to 5L,
        "שש" to 6L, "שישה" to 6L, "שבע" to 7L, "שבעה" to 7L, "שמונה" to 8L, "תשע" to 9L, "תשעה" to 9L,
        "עשר" to 10L, "עשרה" to 10L, "עשרים" to 20L, "שלושים" to 30L, "ארבעים" to 40L,
        "חמישים" to 50L, "שישים" to 60L, "שבעים" to 70L, "שמונים" to 80L, "תשעים" to 90L,
        "מאה" to 100L, "מאתיים" to 200L, "אלפיים" to 2000L
    )

    val scaleMultipliers = mapOf(
        "מאות" to 100L,
        "אלף" to 1000L, "אלפים" to 1000L,
        "מיליון" to 1000000L, "מליון" to 1000000L, "מיליונים" to 1000000L,
        "מיליארד" to 1000000000L, "מליארד" to 1000000000L, "ביליון" to 1000000000L
    )

    val fractionWords = mapOf(
        "חצי" to 0.5, "וחצי" to 0.5, "רבע" to 0.25, "ורבע" to 0.25,
        "שליש" to 0.333333, "ושליש" to 0.333333, "עשירית" to 0.1, "ועשירית" to 0.1
    )

    val tokens = s.split(Regex("\\s+")).filter { it.isNotEmpty() }
    val resultBuilder = StringBuilder()

    var totalNumber = 0.0
    var currentChunk = 0.0
    var buildingNumber = false

    fun flushNumber() {
        if (buildingNumber) {
            val finalNum = totalNumber + currentChunk
            val formatted = if (finalNum % 1.0 == 0.0) {
                finalNum.toLong().toString()
            } else {
                BigDecimal.valueOf(finalNum).stripTrailingZeros().toPlainString()
            }
            resultBuilder.append(formatted)
            totalNumber = 0.0
            currentChunk = 0.0
            buildingNumber = false
        }
    }

    for (rawToken in tokens) {
        val cleanTok = if (rawToken.startsWith("ו") && rawToken.length > 1 &&
            (unitWords.containsKey(rawToken.substring(1)) ||
                    scaleMultipliers.containsKey(rawToken.substring(1)) ||
                    fractionWords.containsKey(rawToken.substring(1)))
        ) {
            rawToken.substring(1)
        } else {
            rawToken
        }

        val numericVal = cleanTok.toDoubleOrNull()
        when {
            numericVal != null -> {
                currentChunk += numericVal
                buildingNumber = true
            }
            unitWords.containsKey(cleanTok) -> {
                currentChunk += unitWords[cleanTok]!!.toDouble()
                buildingNumber = true
            }
            scaleMultipliers.containsKey(cleanTok) -> {
                val mult = scaleMultipliers[cleanTok]!!.toDouble()
                if (mult == 100.0) {
                    currentChunk = (if (currentChunk == 0.0) 1.0 else currentChunk) * 100.0
                } else {
                    val base = if (currentChunk == 0.0) 1.0 else currentChunk
                    totalNumber += base * mult
                    currentChunk = 0.0
                }
                buildingNumber = true
            }
            fractionWords.containsKey(cleanTok) -> {
                currentChunk += fractionWords[cleanTok]!!
                buildingNumber = true
            }
            cleanTok == "." -> {
                if (buildingNumber) {
                    val intPart = (totalNumber + currentChunk).toLong()
                    resultBuilder.append(intPart).append(".")
                    totalNumber = 0.0
                    currentChunk = 0.0
                    buildingNumber = false
                } else {
                    resultBuilder.append(".")
                }
            }
            else -> {
                flushNumber()
                resultBuilder.append(cleanTok)
            }
        }
    }
    flushNumber()

    var finalExpr = resultBuilder.toString()
        .replace("-", "−")
        .replace("*", "×")
        .replace("/", "÷")
        .replace(" ", "")
        .replace(Regex("[^0-9.−+×÷^√%!()πsincostanloglne]"), "")

    val openParens = finalExpr.count { it == '(' }
    val closeParens = finalExpr.count { it == ')' }
    if (openParens > closeParens) {
        finalExpr += ")".repeat(openParens - closeParens)
    }
    return finalExpr
}

fun MainActivity.handleVoiceActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == VOICE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
        val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        if (!matches.isNullOrEmpty()) {
            if (justCalculated) { currentInput = ""; justCalculated = false }
            val cleanMath = parseHebrewVoiceToMath(matches[0])
            currentInput += cleanMath
            currentView?.text = if (currentInput.isEmpty()) "0" else currentInput
            val hasOperator = cleanMath.any { it in listOf('+', '−', '×', '÷', '^', '√', '%', '!') } ||
                    cleanMath.contains("sin(") || cleanMath.contains("cos(") ||
                    cleanMath.contains("tan(") || cleanMath.contains("log(") || cleanMath.contains("ln(")
            val endsValid = cleanMath.lastOrNull()?.let { it.isDigit() || it == ')' || it == '!' || it == '%' || it == 'π' } == true
            if (hasOperator && endsValid) {
                onEquationButtonClick("=")
            }
        }
    }
}
 

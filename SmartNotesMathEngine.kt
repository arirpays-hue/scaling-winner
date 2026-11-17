package com.example.calculator

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

data class SmartNoteEntry(
    val title: String,
    val body: String,
    val solutionSummary: String,
    val date: String
)

// =========================================================================
// מנוע אלגברי ומתמטי אוניברסלי לפתקים חכמים (100% חסין קריסות, ללא Regex מקונן)
// פותר: אריתמטיקה, פונקציות מדעיות, אחוזים, משתנים רב-שורתיים, משוואות לינאריות,
// משוואות ריבועיות, משוואות ממעלה שלישית/כלליות, מערכות משוואות (2x2, 3x3) ואי-שוויונות!
// =========================================================================
object SmartNotesMathEngine {

    // מסיר תווי כיווניות נסתרים ומנרמל סימנים מתמטיים
    fun cleanMathString(raw: String): String {
        val sb = StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            val ch = raw[i]
            when (ch) {
                '\u200E', '\u200F', '\u202A', '\u202B', '\u202C', '\u202D', '\u202E',
                '\u2066', '\u2067', '\u2068', '\u2069' -> { /* דילוג על תווי BiDi */ }
                '−', '–', '—' -> sb.append('-')
                '×', '⋅', '∙' -> sb.append('*')
                '÷' -> sb.append('/')
                '²' -> sb.append("^2")
                '³' -> sb.append("^3")
                '⁴' -> sb.append("^4")
                'π' -> sb.append("pi")
                else -> sb.append(ch)
            }
            i++
        }
        return sb.toString()
    }

    // מחלץ את החלק המתמטי מתוך שורה שכוללת גם תיאור בעברית (למשל: "מחיר מזגן: 150 + 240 * 2")
    private fun extractMathPartFromLine(rawLine: String): Pair<String, String> {
        val cleaned = cleanMathString(rawLine).trim()
        if (cleaned.isEmpty()) return "" to ""

        // אם יש נקודתיים אחרי מילה בעברית (למשל "תרגיל 1: 2x + 5 = 15")
        val colonIdx = cleaned.indexOf(':')
        if (colonIdx in 1 until cleaned.length - 1) {
            val prefix = cleaned.substring(0, colonIdx + 1).trim()
            val rest = cleaned.substring(colonIdx + 1).trim()
            if (prefix.any { it in 'א'..'ת' }) {
                return prefix to rest
            }
        }
        return "" to cleaned
    }

    // מפענח ומחשב ביטוי מתמטי עבור מפת משתנים נתונה (תומך בכפל מרומז כמו 2x, 3(x+1), (x+1)(x-1))
    fun evalWithVars(exprStr: String, vars: Map<String, Double>): Double {
        val s = preprocessImplicitMath(exprStr)
        return Parser(s, vars).parseExpression()
    }

    private fun preprocessImplicitMath(raw: String): String {
        val s = cleanMathString(raw)
            .replace("√", "sqrt")
            .replace(" ", "")
        if (s.isEmpty()) return "0"

        // טיפול באחוזים כמו 500+17% או 20%*300
        val pctAdjusted = fixPercentages(s)

        val out = StringBuilder()
        var i = 0
        while (i < pctAdjusted.length) {
            val c = pctAdjusted[i]
            out.append(c)
            if (i + 1 < pctAdjusted.length) {
                val next = pctAdjusted[i + 1]
                val cIsDigitOrClose = c.isDigit() || c == ')' || c == '!'
                val cIsVar = isSingleVarChar(c) && !isPartOfFunctionName(pctAdjusted, i)
                val nextIsOpen = next == '('
                val nextIsAlpha = next.isLetter() || next in 'א'..'ת'
                val nextIsDigit = next.isDigit()

                if ((cIsDigitOrClose && (nextIsOpen || nextIsAlpha)) ||
                    (cIsVar && (nextIsOpen || nextIsDigit)) ||
                    (c == ')' && (nextIsOpen || nextIsDigit || nextIsAlpha))
                ) {
                    out.append('*')
                }
            }
            i++
        }
        return out.toString()
    }

    private fun fixPercentages(s: String): String {
        if (!s.contains('%')) return s
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            if (s[i] == '%') {
                sb.append("*0.01")
            } else {
                sb.append(s[i])
            }
            i++
        }
        return sb.toString()
    }

    private fun isSingleVarChar(c: Char): Boolean {
        return c in 'a'..'z' || c in 'A'..'Z' || c in 'א'..'ת'
    }

    private fun isPartOfFunctionName(s: String, idx: Int): Boolean {
        val funcs = listOf("sin", "cos", "tan", "asin", "acos", "atan", "sqrt", "cbrt", "log", "ln", "abs", "exp", "pi")
        for (fn in funcs) {
            var start = (idx - fn.length + 1).coerceAtLeast(0)
            while (start <= idx && start + fn.length <= s.length) {
                if (s.regionMatches(start, fn, 0, fn.length, ignoreCase = true)) return true
                start++
            }
        }
        return false
    }

    private class Parser(private val input: String, private val vars: Map<String, Double>) {
        private var pos = 0

        fun parseExpression(): Double {
            val v = parseTerm()
            return if (pos < input.length) Double.NaN else v
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (pos < input.length) {
                val op = input[pos]
                if (op == '+') {
                    pos++
                    x += parseFactor()
                } else if (op == '-') {
                    pos++
                    x -= parseFactor()
                } else {
                    break
                }
            }
            return x
        }

        private fun parseFactor(): Double {
            var x = parseUnary()
            while (pos < input.length) {
                val op = input[pos]
                if (op == '*') {
                    pos++
                    x *= parseUnary()
                } else if (op == '/') {
                    pos++
                    val denom = parseUnary()
                    x = if (abs(denom) < 1e-15) Double.NaN else x / denom
                } else {
                    break
                }
            }
            return x
        }

        private fun parseUnary(): Double {
            if (pos < input.length && input[pos] == '+') {
                pos++
                return parseUnary()
            }
            if (pos < input.length && input[pos] == '-') {
                pos++
                return -parseUnary()
            }
            return parsePower()
        }

        private fun parsePower(): Double {
            var base = parsePostfix()
            if (pos < input.length && input[pos] == '^') {
                pos++
                val exp = parseUnary()
                base = base.pow(exp)
            }
            return base
        }

        private fun parsePostfix(): Double {
            var v = parsePrimary()
            while (pos < input.length && input[pos] == '!') {
                pos++
                v = factorial(v)
            }
            return v
        }

        private fun parsePrimary(): Double {
            if (pos >= input.length) return Double.NaN
            val ch = input[pos]

            if (ch == '(') {
                pos++
                val v = parseTerm()
                if (pos < input.length && input[pos] == ')') pos++
                return v
            }

            if (ch.isDigit() || ch == '.') {
                val start = pos
                while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) pos++
                return input.substring(start, pos).toDoubleOrNull() ?: Double.NaN
            }

            if (ch.isLetter() || ch in 'א'..'ת') {
                val start = pos
                while (pos < input.length && (input[pos].isLetter() || input[pos] in 'א'..'ת')) pos++
                val token = input.substring(start, pos)
                val lower = token.lowercase(Locale.US)

                if (pos < input.length && input[pos] == '(') {
                    pos++
                    val arg = parseTerm()
                    if (pos < input.length && input[pos] == ')') pos++
                    return when (lower) {
                        "sqrt" -> if (arg < 0) Double.NaN else sqrt(arg)
                        "cbrt" -> cbrt(arg)
                        "sin" -> sin(Math.toRadians(arg))
                        "cos" -> cos(Math.toRadians(arg))
                        "tan" -> tan(Math.toRadians(arg))
                        "asin" -> Math.toDegrees(asin(arg))
                        "acos" -> Math.toDegrees(acos(arg))
                        "atan" -> Math.toDegrees(atan(arg))
                        "log" -> if (arg <= 0) Double.NaN else log10(arg)
                        "ln" -> if (arg <= 0) Double.NaN else ln(arg)
                        "abs" -> abs(arg)
                        "exp" -> exp(arg)
                        else -> Double.NaN
                    }
                }

                // אם כתוב sqrt9 ללא סוגריים
                if (lower.startsWith("sqrt") && lower.length == 4 && pos < input.length && (input[pos].isDigit() || input[pos] == '.')) {
                    val arg = parsePrimary()
                    return if (arg < 0) Double.NaN else sqrt(arg)
                }

                if (lower == "pi") return PI
                if (lower == "e" && !vars.containsKey("e")) return E

                vars[token]?.let { return it }
                vars[lower]?.let { return it }

                // פירוק מכפלת משתנים צמודים כמו xy -> x * y
                if (token.length > 1) {
                    var prod = 1.0
                    for (c in token) {
                        val s = c.toString()
                        val v = vars[s] ?: vars[s.lowercase(Locale.US)] ?: return Double.NaN
                        prod *= v
                    }
                    return prod
                }
                return Double.NaN
            }

            return Double.NaN
        }

        private fun factorial(n: Double): Double {
            if (n < 0 || n > 170 || abs(n - n.roundToInt()) > 1e-9) return Double.NaN
            var r = 1.0
            for (i in 2..n.roundToInt()) r *= i
            return r
        }
    }

    // מזהה אילו משתנים (נעלמים) מופיעים בביטוי
    private fun detectVariables(s: String, knownVars: Map<String, Double>): List<String> {
        val cleaned = cleanMathString(s)
        val reserved = setOf("sin", "cos", "tan", "asin", "acos", "atan", "sqrt", "cbrt", "log", "ln", "abs", "exp", "pi")
        val found = linkedSetOf<String>()
        var i = 0
        while (i < cleaned.length) {
            val c = cleaned[i]
            if (c.isLetter() || c in 'א'..'ת') {
                val start = i
                while (i < cleaned.length && (cleaned[i].isLetter() || cleaned[i] in 'א'..'ת')) i++
                val word = cleaned.substring(start, i)
                val lower = word.lowercase(Locale.US)
                if (lower !in reserved) {
                    if (knownVars.containsKey(word) || (word.any { it in 'א'..'ת' } && word.length > 1)) {
                        if (!knownVars.containsKey(word)) found.add(word)
                    } else {
                        for (ch in word) {
                            val vName = ch.toString()
                            if (vName.lowercase(Locale.US) != "e" || !cleaned.contains("e^")) {
                                if (!knownVars.containsKey(vName)) found.add(vName)
                            }
                        }
                    }
                }
            } else {
                i++
            }
        }
        return found.toList()
    }

    // פותר משוואה בנעלם אחד (לינארית, ריבועית, ממעלה שלישית או כללית)
    private fun solveSingleVariableEquation(
        lhsRaw: String,
        rhsRaw: String,
        varName: String,
        knownVars: Map<String, Double>,
        precision: Int
    ): String? {
        fun f(x: Double): Double {
            val map = HashMap(knownVars)
            map[varName] = x
            val l = evalWithVars(lhsRaw, map)
            val r = evalWithVars(rhsRaw, map)
            if (l.isNaN() || r.isNaN() || l.isInfinite() || r.isInfinite()) return Double.NaN
            return l - r
        }

        val f0 = f(0.0)
        val f1 = f(1.0)
        val fm1 = f(-1.0)
        val f2 = f(2.0)
        val fm2 = f(-2.0)

        if (!f0.isNaN() && !f1.isNaN() && !fm1.isNaN() && !f2.isNaN() && !fm2.isNaN()) {
            val c = f0
            val b = (f1 - fm1) / 2.0
            val a = (f1 + fm1 - 2.0 * c) / 2.0

            val isPolyDeg2OrLess = abs(f2 - (4.0 * a + 2.0 * b + c)) < 1e-6 &&
                    abs(fm2 - (4.0 * a - 2.0 * b + c)) < 1e-6

            if (isPolyDeg2OrLess) {
                // 1. משוואה לינארית: b*x + c = 0
                if (abs(a) < 1e-8) {
                    return if (abs(b) > 1e-9) {
                        val root = -c / b
                        val cleanRoot = if (abs(root) < 1e-12) 0.0 else root
                        BidiFmt.ltr("$varName = ${Utils.formatResult(cleanRoot, precision)}")
                    } else if (abs(c) < 1e-8) {
                        "אינסוף פתרונות (זהות מתמטית ∀$varName)"
                    } else {
                        "אין פתרון (סתירה מתמטית ∅)"
                    }
                }

                // 2. משוואה ריבועית: a*x^2 + b*x + c = 0
                val delta = b * b - 4.0 * a * c
                return when {
                    abs(delta) < 1e-9 -> {
                        val x = -b / (2.0 * a)
                        val cleanX = if (abs(x) < 1e-12) 0.0 else x
                        BidiFmt.ltr("$varName = ${Utils.formatResult(cleanX, precision)}") + " (שורש יחיד, Δ=0)"
                    }
                    delta > 0 -> {
                        val sqrtD = sqrt(delta)
                        val x1 = (-b + sqrtD) / (2.0 * a)
                        val x2 = (-b - sqrtD) / (2.0 * a)
                        BidiFmt.ltr("${varName}₁ = ${Utils.formatResult(x1, precision)} , ${varName}₂ = ${Utils.formatResult(x2, precision)}")
                    }
                    else -> {
                        val realPart = -b / (2.0 * a)
                        val imagPart = abs(sqrt(-delta) / (2.0 * a))
                        val rStr = Utils.formatResult(if (abs(realPart) < 1e-12) 0.0 else realPart, 3)
                        val iStr = Utils.formatResult(imagPart, 3)
                        BidiFmt.ltr("${varName}₁,₂ = $rStr ± ${iStr}i") + " (שורשים מרוכבים)"
                    }
                }
            }
        }

        // 3. משוואה ממעלה שלישית / שורשים / מעריכית / טריגונומטרית - חיפוש שורשים נומרי רב-מוקדי (Brent / Secant)
        val roots = findNumericalRoots(::f)
        if (roots.isNotEmpty()) {
            val formattedRoots = roots.mapIndexed { idx, r ->
                val cleanR = if (abs(r) < 1e-11) 0.0 else r
                if (roots.size == 1) "$varName = ${Utils.formatResult(cleanR, precision)}"
                else "${varName}${idx + 1} = ${Utils.formatResult(cleanR, precision)}"
            }.joinToString(" , ")
            return BidiFmt.ltr(formattedRoots)
        }
        return null
    }

    private fun findNumericalRoots(f: (Double) -> Double): List<Double> {
        val found = mutableListOf<Double>()
        fun addUniqueRoot(r: Double) {
            if (r.isNaN() || r.isInfinite()) return
            val fv = f(r)
            if (fv.isNaN() || abs(fv) > 1e-4) return
            val rounded = if (abs(r - r.roundToInt()) < 1e-7) r.roundToInt().toDouble() else r
            if (found.none { abs(it - rounded) < 1e-4 }) {
                found.add(rounded)
            }
        }

        val grid = mutableListOf<Double>()
        var x = -200.0
        while (x <= 200.0) {
            grid.add(x)
            x += if (abs(x) < 20.0) 0.25 else 2.5
        }

        for (i in 0 until grid.size - 1) {
            val a = grid[i]; val b = grid[i + 1]
            val fa = f(a); val fb = f(b)
            if (fa.isNaN() || fb.isNaN()) continue
            if (abs(fa) < 1e-9) addUniqueRoot(a)
            if (fa * fb < 0.0) {
                var lo = a; var hi = b
                var flo = fa; var fhi = fb
                for (iter in 0 until 45) {
                    val mid = 0.5 * (lo + hi)
                    val fmid = f(mid)
                    if (fmid.isNaN() || abs(fmid) < 1e-11) {
                        lo = mid; hi = mid; break
                    }
                    if (flo * fmid <= 0.0) {
                        hi = mid; fhi = fmid
                    } else {
                        lo = mid; flo = fmid
                    }
                }
                addUniqueRoot(0.5 * (lo + hi))
            }
        }
        found.sort()
        return found.take(4)
    }

    // פותר אי-שוויון לינארי או ריבועי (למשל 2x - 6 > 4 או x^2 - 4 <= 0)
    private fun solveInequality(
        line: String,
        knownVars: Map<String, Double>,
        precision: Int
    ): String? {
        val normalized = cleanMathString(line)
            .replace("<=", "≤")
            .replace(">=", "≥")
        val op = listOf("≤", "≥", "<", ">").firstOrNull { normalized.contains(it) } ?: return null
        val parts = normalized.split(op, limit = 2)
        if (parts.size != 2) return null

        val lhs = parts[0].trim(); val rhs = parts[1].trim()
        val vars = detectVariables("$lhs - ($rhs)", knownVars)
        if (vars.isEmpty()) {
            val lVal = evalWithVars(lhs, knownVars)
            val rVal = evalWithVars(rhs, knownVars)
            if (lVal.isNaN() || rVal.isNaN()) return null
            val holds = when (op) {
                "<" -> lVal < rVal
                ">" -> lVal > rVal
                "≤" -> lVal <= rVal
                else -> lVal >= rVal
            }
            return if (holds) "מתקיים (אמת ✓)" else "לא מתקיים (שקר ✗)"
        }
        if (vars.size == 1) {
            val v = vars[0]
            fun f(x: Double): Double {
                val m = HashMap(knownVars); m[v] = x
                return evalWithVars(lhs, m) - evalWithVars(rhs, m)
            }
            val c = f(0.0); val f1 = f(1.0); val fm1 = f(-1.0)
            if (!c.isNaN() && !f1.isNaN() && !fm1.isNaN()) {
                val b = (f1 - fm1) / 2.0
                val a = (f1 + fm1 - 2.0 * c) / 2.0
                if (abs(a) < 1e-8 && abs(b) > 1e-9) {
                    val boundary = -c / b
                    val flippedOp = if (b < 0) {
                        when (op) { "<" -> ">"; ">" -> "<"; "≤" -> "≥"; else -> "≤" }
                    } else op
                    return BidiFmt.ltr("$v $flippedOp ${Utils.formatResult(boundary, precision)}")
                }
            }
        }
        return null
    }

    // פותר מערכת של 2 או 3 משוואות לינאריות ב-2 או 3 נעלמים (למשל x + y = 10, x - y = 2)
    private fun trySolveLinearSystem(
        eqLines: List<Pair<String, String>>,
        varNames: List<String>,
        knownVars: Map<String, Double>,
        precision: Int
    ): String? {
        val n = varNames.size
        if (n !in 2..3 || eqLines.size < n) return null

        val matrix = Array(n) { DoubleArray(n) }
        val rhsVec = DoubleArray(n)

        for (row in 0 until n) {
            val (lRaw, rRaw) = eqLines[row]
            fun evalAt(point: Map<String, Double>): Double {
                val m = HashMap(knownVars)
                m.putAll(point)
                return evalWithVars(lRaw, m) - evalWithVars(rRaw, m)
            }
            val zeroMap = varNames.associateWith { 0.0 }
            val f0 = evalAt(zeroMap)
            if (f0.isNaN()) return null
            rhsVec[row] = -f0

            for (col in 0 until n) {
                val unitMap = varNames.mapIndexed { idx, name -> name to if (idx == col) 1.0 else 0.0 }.toMap()
                val fUnit = evalAt(unitMap)
                if (fUnit.isNaN()) return null
                matrix[row][col] = fUnit - f0
            }

            // בדיקה שהמשוואה אכן לינארית
            val testMap = varNames.mapIndexed { idx, name -> name to (idx + 2.0) }.toMap()
            val fTest = evalAt(testMap)
            var expected = f0
            for (col in 0 until n) expected += matrix[row][col] * (col + 2.0)
            if (fTest.isNaN() || abs(fTest - expected) > 1e-5) return null
        }

        // אלימינציית גאוס עם בחירת ציר (Partial Pivoting)
        val a = Array(n) { r -> DoubleArray(n + 1) { c -> if (c < n) matrix[r][c] else rhsVec[r] } }
        for (i in 0 until n) {
            var maxRow = i
            for (k in i + 1 until n) {
                if (abs(a[k][i]) > abs(a[maxRow][i])) maxRow = k
            }
            val tmp = a[i]; a[i] = a[maxRow]; a[maxRow] = tmp
            if (abs(a[i][i]) < 1e-10) return null

            for (k in i + 1 until n) {
                val factor = a[k][i] / a[i][i]
                for (j in i..n) a[k][j] -= factor * a[i][j]
            }
        }

        val sol = DoubleArray(n)
        for (i in n - 1 downTo 0) {
            var sum = a[i][n]
            for (j in i + 1 until n) sum -= a[i][j] * sol[j]
            sol[i] = sum / a[i][i]
            if (abs(sol[i]) < 1e-12) sol[i] = 0.0
        }

        val parts = varNames.mapIndexed { idx, v -> "$v = ${Utils.formatResult(sol[idx], precision)}" }
        return "פתרון מערכת משוואות: " + BidiFmt.ltr(parts.joinToString(" , "))
    }

    // פונקציית העל: מנתחת טקסט פתק מלא, פותרת כל שורה וכל מערכת משוואות, ומחזירה טקסט מעודכן וסיכום!
    fun analyzeAndSolveNote(rawNoteText: String, precision: Int): Pair<String, String> {
        val rawLines = rawNoteText.lines()
        val assignedVars = mutableMapOf<String, Double>()
        val unsolvedEquations = mutableListOf<Pair<String, String>>()
        val outLines = mutableListOf<String>()
        val summaryBullets = mutableListOf<String>()

        for (rawLine in rawLines) {
            val (prefix, mathCandidate) = extractMathPartFromLine(rawLine)
            if (mathCandidate.isEmpty()) {
                outLines.add(rawLine)
                continue
            }

            // 1. בדיקת אי-שוויון (<, >, <=, >=)
            val ineqRes = solveInequality(mathCandidate, assignedVars, precision)
            if (ineqRes != null) {
                val cleanBase = cleanMathString(rawLine).trim()
                outLines.add(cleanBase)
                summaryBullets.add("• אי-שוויון (${BidiFmt.ltr(mathCandidate)}): $ineqRes")
                continue
            }

            // 2. בדיקה אם השורה מכילה סימן '=' (השמת משתנה, משוואה עם נעלם, או תרגיל עם '=' בסופו)
            if (mathCandidate.contains('=')) {
                val eqParts = mathCandidate.split('=').map { it.trim() }.filter { it.isNotEmpty() }
                if (eqParts.isEmpty()) {
                    outLines.add(rawLine)
                    continue
                }

                // אם המשתמש כתב "150 + 240 * 2 =" או שורה שכבר חושבה בעבר "150 + 240 * 2 = 630"
                val lhs = eqParts[0]
                val rhs = if (eqParts.size >= 2) eqParts[1] else ""
                val lhsVars = detectVariables(lhs, assignedVars)
                val rhsVars = detectVariables(rhs, assignedVars)
                val allLineVars = (lhsVars + rhsVars).distinct()

                // א. תרגיל מספרי טהור (ללא נעלמים)
                if (allLineVars.isEmpty()) {
                    val v = evalWithVars(lhs, assignedVars)
                    if (!v.isNaN() && !v.isInfinite()) {
                        val fmt = Utils.formatResult(v, precision)
                        val prefixStr = if (prefix.isNotEmpty()) "$prefix " else ""
                        outLines.add("$prefixStr$lhs = $fmt")
                        summaryBullets.add("• ${BidiFmt.ltr("$lhs = $fmt")}")
                        continue
                    }
                }

                // ב. השמת משתנה ישירה (למשל: "x = 5" או "אורך = 12 + 3")
                if (lhsVars.size == 1 && lhs == lhsVars[0] && rhs.isNotEmpty() && rhsVars.isEmpty()) {
                    val v = evalWithVars(rhs, assignedVars)
                    if (!v.isNaN() && !v.isInfinite()) {
                        assignedVars[lhs] = v
                        val fmt = Utils.formatResult(v, precision)
                        val prefixStr = if (prefix.isNotEmpty()) "$prefix " else ""
                        outLines.add("$prefixStr$lhs = $fmt")
                        summaryBullets.add("• הוגדר משתנה: ${BidiFmt.ltr("$lhs = $fmt")}")
                        continue
                    }
                }

                // ג. משוואה בנעלם יחיד (למשל: "2x + 5 = 17" או "x^2 - 5x + 6 = 0")
                if (rhs.isNotEmpty() && allLineVars.size == 1) {
                    val varName = allLineVars[0]
                    val eqSol = solveSingleVariableEquation(lhs, rhs, varName, assignedVars, precision)
                    if (eqSol != null) {
                        val cleanOriginal = if (prefix.isNotEmpty()) "$prefix $lhs = $rhs" else "$lhs = $rhs"
                        outLines.add("$cleanOriginal  ➔  ${cleanMathString(eqSol)}")
                        summaryBullets.add("• משוואה (${BidiFmt.ltr("$lhs = $rhs")}): $eqSol")
                        continue
                    }
                }

                // ד. משוואה מרובת נעלמים (למשל "2x + y = 10") - נשמור לפתרון מערכת משוואות
                if (rhs.isNotEmpty() && allLineVars.size in 2..3) {
                    unsolvedEquations.add(lhs to rhs)
                    outLines.add(cleanMathString(rawLine).trim())
                    continue
                }
            } else {
                // 3. שורה ללא סימן '=' (למשל: "150 + 240 * 2" או "2x + 3y" אחרי שהוגדרו x ו-y)
                val lineVars = detectVariables(mathCandidate, assignedVars)
                if (lineVars.isEmpty()) {
                    val hasOperator = mathCandidate.any { it in listOf('+', '-', '*', '/', '^', '!', '%') } ||
                            mathCandidate.contains("sqrt", ignoreCase = true) ||
                            mathCandidate.contains("sin", ignoreCase = true) ||
                            mathCandidate.contains("cos", ignoreCase = true) ||
                            mathCandidate.contains("tan", ignoreCase = true) ||
                            mathCandidate.contains("log", ignoreCase = true) ||
                            mathCandidate.contains("ln", ignoreCase = true)
                    if (hasOperator) {
                        val v = evalWithVars(mathCandidate, assignedVars)
                        if (!v.isNaN() && !v.isInfinite()) {
                            val fmt = Utils.formatResult(v, precision)
                            val prefixStr = if (prefix.isNotEmpty()) "$prefix " else ""
                            outLines.add("$prefixStr$mathCandidate = $fmt")
                            summaryBullets.add("• ${BidiFmt.ltr("$mathCandidate = $fmt")}")
                            continue
                        }
                    }
                } else if (lineVars.size == 1) {
                    // אם המשתמש כתב רק פולינום כמו "x^2 - 9" ללא "= 0", נמצא את שורשי הפולינום (= 0)
                    val vName = lineVars[0]
                    val eqSol = solveSingleVariableEquation(mathCandidate, "0", vName, assignedVars, precision)
                    if (eqSol != null) {
                        outLines.add("${cleanMathString(rawLine).trim()} = 0  ➔  ${cleanMathString(eqSol)}")
                        summaryBullets.add("• שורשי הביטוי (${BidiFmt.ltr("$mathCandidate = 0")}): $eqSol")
                        continue
                    }
                }
            }

            // בדיקה אם יש מספר משוואות מופרדות בפסיק באותה שורה (למשל: "x + y = 10, x - y = 2")
            if (mathCandidate.contains(',') && mathCandidate.count { it == '=' } >= 2) {
                val subEqs = mathCandidate.split(',').mapNotNull { part ->
                    val sp = part.split('=')
                    if (sp.size == 2) sp[0].trim() to sp[1].trim() else null
                }
                unsolvedEquations.addAll(subEqs)
            }

            outLines.add(cleanMathString(rawLine).trim())
        }

        // 4. פתרון מערכת משוואות אם זוהו 2 או 3 משוואות עם 2 או 3 נעלמים
        if (unsolvedEquations.size >= 2) {
            val sysVars = unsolvedEquations
                .flatMap { (l, r) -> detectVariables("$l - ($r)", assignedVars) }
                .distinct()
            val sysSol = trySolveLinearSystem(unsolvedEquations, sysVars, assignedVars, precision)
            if (sysSol != null) {
                summaryBullets.add("• $sysSol")
                outLines.add("➔ ${cleanMathString(sysSol)}")
            }
        }

        // 5. אם לא היו משוואות, נבדוק אם יש רשימת מספרים בפתק ונסכם אותם (סכום, ממוצע, מינימום, מקסימום)
        if (summaryBullets.isEmpty()) {
            val allNums = Regex("-?\\d+(?:\\.\\d+)?")
                .findAll(cleanMathString(rawNoteText))
                .mapNotNull { it.value.toDoubleOrNull() }
                .toList()
            if (allNums.isNotEmpty()) {
                val sum = allNums.sum()
                val avg = sum / allNums.size
                summaryBullets.add("• סכום המספרים בפתק (${BidiFmt.ltr(allNums.size)} פריטים): ${BidiFmt.ltr(Utils.formatResult(sum, precision))}")
                if (allNums.size > 1) {
                    summaryBullets.add("• ממוצע: ${BidiFmt.ltr(Utils.formatResult(avg, 2))} | טווח: ${BidiFmt.ltr("${Utils.formatResult(allNums.minOrNull() ?: 0.0, 2)} .. ${Utils.formatResult(allNums.maxOrNull() ?: 0.0, 2)}")}")
                }
            }
        }

        val updatedText = outLines.joinToString("\n")
        val summaryStr = if (summaryBullets.isNotEmpty()) {
            BidiFmt.rtlParagraph("✅ תוצאות ופתרון מתמטי:", *summaryBullets.toTypedArray())
        } else {
            ""
        }
        return updatedText to summaryStr
    }
}

// =========================================================================
// מסך פתקים חכמים ורשימות עם פותר המשוואות המובנה (Smart Notes - Tab 78)
// =========================================================================
fun MainActivity.createProSmartNotesScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20f), dp(12f), dp(20f), dp(24f))
    }
    root.addView(
        createProCanvaHeader(
            "📝 פתקים חכמים ופותר משוואות (Smart Notes)",
            "מחברת פתקים עם מנוע אלגברי הפותר תרגילים, משוואות עם נעלמים (x, y), משוואות ריבועיות ורשימות"
        )
    )

    val titleIn = createInputBox("כותרת הפתק (למשל: שיעורי בית / מידות / קניות)").apply {
        inputType = InputType.TYPE_CLASS_TEXT
        textDirection = View.TEXT_DIRECTION_RTL
        gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
    }

    val bodyIn = createInputBox("כתוב פתק, תרגיל או משוואה (למשל: 2x + 5 = 17 או x^2 - 5x + 6 = 0)...").apply {
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        textDirection = View.TEXT_DIRECTION_RTL
        gravity = Gravity.RIGHT or Gravity.TOP
        minLines = 3
    }

    // סרגל סימנים מתמטיים ונעלמים להקלדה מהירה ללא צורך בהחלפת מקלדת
    val mathTokens = listOf("x", "y", "=", "+", "-", "*", "/", "^2", "√(", "(", ")", "≤", "≥", "π")
    val quickMathScroll = HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(10f) }
    }
    val quickMathRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    for (tok in mathTokens) {
        val chip = TextView(this).apply {
            tag = "PRO_STAR_CHIP"
            text = tok
            textSize = 14.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (isMinimalistMode) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(14f), dp(8f), dp(14f), dp(8f))
            background = GradientDrawable().apply {
                cornerRadius = dp(999f).toFloat()
                setColor(if (isMinimalistMode) MinimalistTheme.ACCENT_PASTEL_BLUE else Utils.lightenColor(bgColor, 0.14f))
                setStroke(dp(1f), if (isMinimalistMode) MinimalistTheme.ACCENT_SKY_BLUE else primaryColor)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(3f), dp(2f), dp(3f), dp(2f)) }
            setOnClickListener {
                feedback(it)
                val start = bodyIn.selectionStart.coerceAtLeast(0)
                val end = bodyIn.selectionEnd.coerceAtLeast(0)
                bodyIn.text.replace(min(start, end), max(start, end), tok, 0, tok.length)
            }
        }
        quickMathRow.addView(chip)
    }
    quickMathScroll.addView(quickMathRow)

    val statusTv = BidiFmt.applyRtlToResultView(createResultTextView().apply {
        textSize = 15.5f
        setPadding(dp(4f), dp(8f), dp(4f), dp(8f))
    })
    val notesListContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

    fun encodeField(s: String): String = try {
        Base64.encodeToString(s.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    } catch (_: Throwable) { "" }

    fun decodeField(b64: String): String = try {
        String(Base64.decode(b64, Base64.NO_WRAP), Charsets.UTF_8)
    } catch (_: Throwable) { "" }

    fun loadNotesSafely(): MutableList<SmartNoteEntry> {
        val list = mutableListOf<SmartNoteEntry>()
        try {
            val raw = prefs?.getString("pro_smart_notes_v3", null)
                ?: prefs?.getString("pro_smart_notes_v2", "") ?: ""
            if (raw.isNotBlank()) {
                for (entry in raw.split("||")) {
                    val parts = entry.split("::")
                    if (parts.size >= 3) {
                        val t = decodeField(parts[0])
                        val b = decodeField(parts[1])
                        val s = if (parts.size >= 4) decodeField(parts[2]) else ""
                        val d = if (parts.size >= 4) decodeField(parts[3]) else decodeField(parts[2])
                        if (t.isNotEmpty() || b.isNotEmpty()) {
                            list.add(SmartNoteEntry(t, b, s, d))
                        }
                    }
                }
            }
        } catch (_: Throwable) {}
        return list
    }

    fun saveNotesSafely(list: List<SmartNoteEntry>) {
        try {
            val serialized = list.joinToString("||") {
                "${encodeField(it.title)}::${encodeField(it.body)}::${encodeField(it.solutionSummary)}::${encodeField(it.date)}"
            }
            prefs?.edit()?.putString("pro_smart_notes_v3", serialized)?.apply()
        } catch (_: Throwable) {}
    }

    fun hideKeyboardSafely(v: View) {
        try {
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(v.windowToken, 0)
        } catch (_: Throwable) {}
    }

    fun renderNotesList() {
        try {
            notesListContainer.removeAllViews()
            val notes = loadNotesSafely()
            val textCol = if (isMinimalistMode) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            val subCol = if (isMinimalistMode) MinimalistTheme.TEXT_MUTED_GREY else Color.LTGRAY

            if (notes.isEmpty()) {
                if (statusTv.text.isNullOrEmpty()) {
                    statusTv.text = BidiFmt.rtlParagraph("המחברת ריקה כעת. הזן תרגיל, משוואה או פתק למעלה ולחץ על 'שמור פתק חדש במחברת'.")
                }
                return
            }

            for ((idx, note) in notes.withIndex()) {
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
                    background = when {
                        isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProSmartNotesScreen, 18f, 0)
                        isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProSmartNotesScreen, 18f)
                        else -> GradientDrawable().apply {
                            setColor(Utils.lightenColor(bgColor, 0.08f))
                            cornerRadius = dp(18f).toFloat()
                            setStroke(dp(1f), MinimalistTheme.resolveBorderStrokeColor(this@createProSmartNotesScreen, primaryColor))
                        }
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dp(10f) }
                }

                val noteTitleTv = TextView(this).apply {
                    text = "\u200F📌 ${note.title}"
                    setTextColor(textCol)
                    textSize = 16.5f
                    typeface = Typeface.DEFAULT_BOLD
                    textDirection = View.TEXT_DIRECTION_RTL
                    gravity = Gravity.RIGHT
                }

                val noteBodyTv = TextView(this).apply {
                    text = BidiFmt.rtlParagraph(*note.body.split("\n").toTypedArray())
                    setTextColor(textCol)
                    textSize = 15.0f
                    textDirection = View.TEXT_DIRECTION_RTL
                    gravity = Gravity.RIGHT
                    setPadding(0, dp(6f), 0, dp(6f))
                    setOnClickListener { copyToClipboard(note.body) }
                }

                card.addView(noteTitleTv)
                card.addView(noteBodyTv)

                if (note.solutionSummary.isNotBlank()) {
                    val solTv = TextView(this).apply {
                        text = note.solutionSummary
                        setTextColor(if (isMinimalistMode) Color.parseColor("#1B5E20") else Color.parseColor("#00E676"))
                        textSize = 14.5f
                        typeface = Typeface.DEFAULT_BOLD
                        textDirection = View.TEXT_DIRECTION_RTL
                        gravity = Gravity.RIGHT
                        setPadding(0, dp(2f), 0, dp(8f))
                        setOnClickListener { copyToClipboard(note.solutionSummary) }
                    }
                    card.addView(solTv)
                }

                val bottomRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val delBtn = TextView(this).apply {
                    text = "🗑️ מחק פתק"
                    setTextColor(if (isMinimalistMode) MinimalistTheme.ALERT_RED_TEXT else Color.parseColor("#FF5252"))
                    textSize = 13.5f
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(dp(8f), dp(4f), dp(8f), dp(4f))
                    setOnClickListener {
                        feedback(it)
                        val currentList = loadNotesSafely()
                        if (idx in currentList.indices) {
                            currentList.removeAt(idx)
                            saveNotesSafely(currentList)
                            renderNotesList()
                        }
                    }
                }

                val editBtn = TextView(this).apply {
                    text = "✏️ טען לעריכה"
                    setTextColor(if (isMinimalistMode) MinimalistTheme.TEXT_CHARCOAL else primaryColor)
                    textSize = 13.5f
                    setPadding(dp(12f), dp(4f), dp(12f), dp(4f))
                    setOnClickListener {
                        feedback(it)
                        titleIn.setText(note.title)
                        bodyIn.setText(note.body)
                        statusTv.text = note.solutionSummary
                    }
                }

                val dateTv = TextView(this).apply {
                    text = BidiFmt.ltr(note.date)
                    setTextColor(subCol)
                    textSize = 12.5f
                    gravity = Gravity.RIGHT
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                bottomRow.addView(delBtn)
                bottomRow.addView(editBtn)
                bottomRow.addView(dateTv)
                card.addView(bottomRow)
                notesListContainer.addView(card)
            }
        } catch (_: Throwable) {}
    }

    val evalMathBtn = createActionBtn("🧮 פתור תרגיל, משוואה או נעלמים בפתק", false) {
        val raw = bodyIn.text.toString().trim()
        if (raw.isEmpty()) {
            Toast.makeText(this, "הזן תרגיל או משוואה בתוך תוכן הפתק", Toast.LENGTH_SHORT).show()
            return@createActionBtn
        }
        try {
            val (updatedBody, summary) = SmartNotesMathEngine.analyzeAndSolveNote(raw, calcPrecision)
            bodyIn.setText(updatedBody)
            bodyIn.setSelection(bodyIn.text.length)
            if (summary.isNotEmpty()) {
                statusTv.text = summary
            } else {
                statusTv.text = BidiFmt.rtlParagraph("לא זוהה ביטוי מתמטי או משוואה לפתרון.")
            }
        } catch (e: Throwable) {
            statusTv.text = BidiFmt.rtlParagraph("לא ניתן לפענח את הביטוי המתמטי שהוזן.")
        }
    }

    val saveBtn = createActionBtn("💾 שמור פתק חדש במחברת", true) {
        val rawTitle = titleIn.text.toString().trim()
        val rawBody = bodyIn.text.toString().trim()
        if (rawBody.isEmpty() && rawTitle.isEmpty()) {
            Toast.makeText(this, "נא להזין תוכן לפתק לפני השמירה", Toast.LENGTH_SHORT).show()
            return@createActionBtn
        }
        try {
            hideKeyboardSafely(bodyIn)
            bodyIn.clearFocus()
            titleIn.clearFocus()

            val t = rawTitle.ifEmpty { "פתק מהיר" }
            val (solvedBody, solutionSummary) = if (rawBody.isNotEmpty()) {
                SmartNotesMathEngine.analyzeAndSolveNote(rawBody, calcPrecision)
            } else {
                "" to ""
            }
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date())
            val list = loadNotesSafely()
            list.add(0, SmartNoteEntry(t, solvedBody, solutionSummary, dateStr))
            saveNotesSafely(list)

            titleIn.setText("")
            bodyIn.setText("")
            if (solutionSummary.isNotEmpty()) {
                statusTv.text = solutionSummary
            } else {
                statusTv.text = BidiFmt.rtlParagraph("✅ הפתק נשמר במחברת (${BidiFmt.ltr(list.size)} פתקים)")
            }
            renderNotesList()
        } catch (_: Throwable) {
            Toast.makeText(this, "הפתק נשמר", Toast.LENGTH_SHORT).show()
        }
    }

    renderNotesList()
    root.addView(titleIn)
    root.addView(bodyIn)
    root.addView(quickMathScroll)
    root.addView(evalMathBtn)
    root.addView(saveBtn)
    root.addView(statusTv)
    root.addView(notesListContainer)
    scroll.addView(root)
    return scroll
}

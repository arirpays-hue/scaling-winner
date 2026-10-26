package com.example.calculator

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import kotlin.math.pow

fun MainActivity.fetchAndCacheCurrencies(onComplete: ((Boolean) -> Unit)? = null) {
    val savedJson = prefs?.getString("cachedRatesJson", null)
    if (savedJson != null) {
        try {
            val rates = JSONObject(savedJson)
            val keys = rates.keys()
            val fetchedLocal = mutableListOf<String>()
            while (keys.hasNext()) {
                val k = keys.next()
                cachedRates[k] = rates.getDouble(k)
                fetchedLocal.add(CurrencyData.getFullName(k))
            }
            if (fetchedLocal.isNotEmpty()) {
                fetchedLocal.sort()
                allCurrencies.clear()
                allCurrencies.addAll(fetchedLocal)
            }
        } catch (e: Exception) {}
    }
    thread {
        try {
            val conn = URL("https://open.er-api.com/v6/latest/USD").openConnection() as HttpURLConnection
            conn.connectTimeout = 7000; conn.readTimeout = 7000
            val res = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(res); val rates = json.getJSONObject("rates"); val fetched = mutableListOf<String>()
            val keys = rates.keys()
            while (keys.hasNext()) {
                val code = keys.next()
                cachedRates[code] = rates.getDouble(code)
                fetched.add(CurrencyData.getFullName(code))
            }
            prefs?.edit()?.putString("cachedRatesJson", rates.toString())?.apply()
            fetched.sort()
            Handler(Looper.getMainLooper()).post {
                allCurrencies.clear(); allCurrencies.addAll(fetched)
                onComplete?.invoke(true)
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post { onComplete?.invoke(cachedRates.isNotEmpty()) }
        }
    }
}

fun MainActivity.extractCurrencyCode(raw: String): String {
    return Regex("\\b[A-Z]{3}\\b").find(raw.uppercase())?.value ?: "USD"
}

fun MainActivity.createUniversalConverter(
    pickerTitle: String,
    itemsProvider: () -> List<String>,
    getFrom: () -> String,
    getTo: () -> String,
    setUnits: (String, String) -> Unit,
    displayLabel: (String) -> String = { it },
    convertLogic: (Double, String, String, (String, String) -> Unit) -> Unit
): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(10f), dp(20f), dp(24f)) }

    var fromBtn: TextView? = null
    var toBtn: TextView? = null
    val amountInput = createInputBox("הזן כמות להמרה...")
    val resCard = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(20f), dp(22f), dp(20f), dp(22f))
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createUniversalConverter, IosGlassTheme.glassCornerPt) else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.08f)); cornerRadius = dp(22f).toFloat(); setStroke(dp(1.5f), primaryColor) }
        visibility = View.GONE
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16f) }
    }
    val mainResTv = TextView(this).apply { setTextColor(Color.WHITE); textSize = 26f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER }
    val subResTv = TextView(this).apply { setTextColor(if (isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else primaryColor); textSize = 14f; gravity = Gravity.CENTER; setPadding(0, dp(8f), 0, 0) }
    resCard.addView(mainResTv); resCard.addView(subResTv)
    resCard.setOnClickListener { if (mainResTv.text.isNotEmpty()) copyToClipboard(mainResTv.text.toString()) }

    fun runConversion() {
        val v = amountInput.text.toString().toDoubleOrNull()
        if (v == null) { resCard.visibility = View.GONE; return }
        convertLogic(v, getFrom(), getTo()) { mainText, subText ->
            resCard.visibility = View.VISIBLE
            mainResTv.text = mainText; subResTv.text = subText
            if (useAnimations) {
                resCard.scaleX = 0.94f; resCard.scaleY = 0.94f
                resCard.animate().scaleX(1f).scaleY(1f).setDuration(220).setInterpolator(OvershootInterpolator(1.4f)).start()
            }
        }
    }

    fromBtn = createActionBtn("מ: ${displayLabel(getFrom())}", false) {
        showItemPicker("$pickerTitle (מקור)", itemsProvider()) { selected ->
            setUnits(selected, getTo())
            fromBtn?.text = "מ: ${displayLabel(getFrom())}"
            runConversion()
        }
    }

    val swapContainer = FrameLayout(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(10f) } }
    val swapBtn = TextView(this).apply {
        text = "⇅ החלף כיוון"; textSize = 15f; typeface = Typeface.DEFAULT_BOLD
        setTextColor(when {
            isAuroraMode -> AuroraBorealisTheme.EMERALD_NIGHT
            isIosGlassMode -> Color.WHITE
            else -> Color.parseColor("#08090C")
        })
        gravity = Gravity.CENTER
        setPadding(dp(24f), dp(12f), dp(24f), dp(12f))
        background = if (isCleanGlassActive) IosGlassTheme.createGlassButtonDrawable(this@createUniversalConverter, "=", false) else GradientDrawable().apply { setColor(primaryColor); cornerRadius = dp(50f).toFloat() }
        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        setOnClickListener { v ->
            feedback(v)
            if (useAnimations) v.animate().rotationBy(360f).setDuration(350).setInterpolator(OvershootInterpolator()).start()
            val oldFrom = getFrom(); val oldTo = getTo()
            setUnits(oldTo, oldFrom)
            fromBtn?.text = "מ: ${displayLabel(getFrom())}"
            toBtn?.text = "ל: ${displayLabel(getTo())}"
            runConversion()
        }
    }
    swapContainer.addView(swapBtn)

    toBtn = createActionBtn("ל: ${displayLabel(getTo())}", false) {
        showItemPicker("$pickerTitle (יעד)", itemsProvider()) { selected ->
            setUnits(getFrom(), selected)
            toBtn?.text = "ל: ${displayLabel(getTo())}"
            runConversion()
        }
    }

    amountInput.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { runConversion() }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    val calcBtn = createActionBtn("חשב והמר 🔄", true) { runConversion() }

    root.addView(fromBtn); root.addView(swapContainer); root.addView(toBtn); root.addView(amountInput); root.addView(calcBtn); root.addView(resCard)
    scroll.addView(root); return scroll
}

fun MainActivity.createCurrencyCalculator(): View = createUniversalConverter(
    pickerTitle = "בחר מטבע",
    itemsProvider = { allCurrencies },
    getFrom = { fromCurrency },
    getTo = { toCurrency },
    setUnits = { f, t -> fromCurrency = extractCurrencyCode(f); toCurrency = extractCurrencyCode(t); savePreferences() },
    displayLabel = { code -> CurrencyData.getFullName(extractCurrencyCode(code)) },
    convertLogic = { amount, fRaw, tRaw, onResult ->
        val fCode = extractCurrencyCode(fRaw); val tCode = extractCurrencyCode(tRaw)
        fun computeWithCache() {
            val rFrom = cachedRates[fCode] ?: if (fCode == "USD") 1.0 else null
            val rTo = cachedRates[tCode] ?: if (tCode == "USD") 1.0 else null
            if (rFrom != null && rTo != null && rFrom > 0) {
                val unitRate = rTo / rFrom
                val converted = amount * unitRate
                onResult("${Utils.formatResult(converted, 4)} $tCode", "שער יציג: 1 $fCode = ${Utils.formatResult(unitRate, 4)} $tCode (לחץ להעתקה)")
            } else {
                onResult("טוען שערים...", "מתחבר לשרת המטבעות...")
                fetchAndCacheCurrencies { ok ->
                    if (ok && cachedRates.containsKey(fCode) && cachedRates.containsKey(tCode)) {
                        val uRate = cachedRates[tCode]!! / cachedRates[fCode]!!
                        onResult("${Utils.formatResult(amount * uRate, 4)} $tCode", "שער יציג: 1 $fCode = ${Utils.formatResult(uRate, 4)} $tCode")
                    } else {
                        onResult("שגיאת חיבור", "בדוק חיבור אינטרנט ונסה שוב")
                    }
                }
            }
        }
        computeWithCache()
    }
)

fun MainActivity.createDistanceCalculator(): View {
    val units = mapOf(
        "מילימטר (mm)" to 0.001, "סנטימטר (cm)" to 0.01, "דצימטר (dm)" to 0.1, "מטר (m)" to 1.0,
        "קילומטר (km)" to 1000.0, "אינץ' (in)" to 0.0254, "רגל - פיט (ft)" to 0.3048,
        "יארד (yd)" to 0.9144, "מייל (mi)" to 1609.344, "מייל ימי (nmi)" to 1852.0,
        "מיקרומטר (µm)" to 1e-6, "ננומטר (nm)" to 1e-9, "אמה (cubit)" to 0.48, "שנת אור (ly)" to 9.4607e15
    )
    return createUniversalConverter("בחר יחידת אורך", { units.keys.toList() }, { fromDist }, { toDist }, { f, t -> fromDist = f; toDist = t; savePreferences() }) { v, f, t, cb ->
        val fFactor = units[f] ?: 1.0; val tFactor = units[t] ?: 1.0
        val res = v * fFactor / tFactor; val unitRate = fFactor / tFactor
        cb("${Utils.formatResult(res, calcPrecision)} ${t.substringBefore(" ")}", "יחס: 1 ${f.substringBefore(" ")} = ${Utils.formatResult(unitRate, 6)} ${t.substringBefore(" ")}")
    }
}

fun MainActivity.createWeightCalculator(): View {
    val units = mapOf(
        "מיליגרם (mg)" to 1e-6, "גרם (g)" to 0.001, "קילוגרם (kg)" to 1.0, "טון מטרי (t)" to 1000.0,
        "אונקיה (oz)" to 0.02834952, "פאונד (lbs)" to 0.45359237, "סטון (st)" to 6.350293,
        "קראט (ct)" to 0.0002, "גריין (gr)" to 0.0000647989, "טון אמריקאי (US ton)" to 907.18474, "טון בריטי (UK ton)" to 1016.0469
    )
    return createUniversalConverter("בחר יחידת משקל", { units.keys.toList() }, { fromWeight }, { toWeight }, { f, t -> fromWeight = f; toWeight = t; savePreferences() }) { v, f, t, cb ->
        val fFactor = units[f] ?: 1.0; val tFactor = units[t] ?: 1.0
        val res = v * fFactor / tFactor; val unitRate = fFactor / tFactor
        cb("${Utils.formatResult(res, calcPrecision)} ${t.substringBefore(" ")}", "יחס: 1 ${f.substringBefore(" ")} = ${Utils.formatResult(unitRate, 6)} ${t.substringBefore(" ")}")
    }
}

fun MainActivity.createTempCalculator(): View {
    val units = listOf("צלזיוס (°C)", "פרנהייט (°F)", "קלווין (K)", "רנקין (°R)", "דליל (°De)", "ניוטון (°N)", "ראומיר (°Ré)", "רומר (°Rø)")
    fun toCelsius(v: Double, u: String): Double = when (u) {
        "פרנהייט (°F)" -> (v - 32.0) * 5.0 / 9.0
        "קלווין (K)" -> v - 273.15
        "רנקין (°R)" -> (v - 491.67) * 5.0 / 9.0
        "דליל (°De)" -> 100.0 - v * 2.0 / 3.0
        "ניוטון (°N)" -> v * 100.0 / 33.0
        "ראומיר (°Ré)" -> v * 5.0 / 4.0
        "רומר (°Rø)" -> (v - 7.5) * 40.0 / 21.0
        else -> v
    }
    fun fromCelsius(c: Double, u: String): Double = when (u) {
        "פרנהייט (°F)" -> c * 9.0 / 5.0 + 32.0
        "קלווין (K)" -> c + 273.15
        "רנקין (°R)" -> (c + 273.15) * 9.0 / 5.0
        "דליל (°De)" -> (100.0 - c) * 1.5
        "ניוטון (°N)" -> c * 33.0 / 100.0
        "ראומיר (°Ré)" -> c * 4.0 / 5.0
        "רומר (°Rø)" -> c * 21.0 / 40.0 + 7.5
        else -> c
    }
    return createUniversalConverter("בחר סולם טמפרטורה", { units }, { fromTemp }, { toTemp }, { f, t -> fromTemp = f; toTemp = t; savePreferences() }) { v, f, t, cb ->
        val c = toCelsius(v, f); val res = fromCelsius(c, t)
        cb("${Utils.formatResult(res, 4)} ${t.substringAfter("(").removeSuffix(")")}", "שוות ערך ל-${Utils.formatResult(c, 2)}°C (נקודת קיפאון: 0°C)")
    }
}

fun MainActivity.createSpeedCalculator(): View {
    val units = mapOf(
        "קמ״ש (km/h)" to (1.0 / 3.6), "מטר לשנייה (m/s)" to 1.0, "מייל לשעה (mph)" to 0.44704,
        "קשר ימי (Knot)" to 0.514444, "רגל לשנייה (ft/s)" to 0.3048, "מאך - מהירות הקול (Mach)" to 343.0, "מהירות האור (c)" to 299792458.0
    )
    return createUniversalConverter("בחר יחידת מהירות", { units.keys.toList() }, { fromSpeed }, { toSpeed }, { f, t -> fromSpeed = f; toSpeed = t; savePreferences() }) { v, f, t, cb ->
        val fFactor = units[f] ?: 1.0; val tFactor = units[t] ?: 1.0
        val res = v * fFactor / tFactor; val unitRate = fFactor / tFactor
        cb("${Utils.formatResult(res, calcPrecision)} ${t.substringBefore(" ")}", "יחס: 1 ${f.substringBefore(" ")} = ${Utils.formatResult(unitRate, 5)} ${t.substringBefore(" ")}")
    }
}

fun MainActivity.createCompoundInterestCalculator(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    val initialIn = createInputBox("סכום הפקדה התחלתי (₪)")
    val monthlyIn = createInputBox("הפקדה חודשית קבועה (₪)")
    val rateIn = createInputBox("ריבית שנתית משוערת (%)")
    val yearsIn = createInputBox("תקופת חיסכון (שנים)")
    val resTv = createResultTextView()
    val btn = createActionBtn("חשב צמיחת השקעה 📈") {
        val p = initialIn.text.toString().toDoubleOrNull() ?: 0.0
        val pmt = monthlyIn.text.toString().toDoubleOrNull() ?: 0.0
        val r = (rateIn.text.toString().toDoubleOrNull() ?: 0.0) / 100.0 / 12.0
        val months = ((yearsIn.text.toString().toDoubleOrNull() ?: 0.0) * 12).toInt()
        if (months <= 0) { resTv.text = "נא להזין מספר שנים תקין"; return@createActionBtn }
        val futureValue = if (r == 0.0) p + pmt * months else p * (1 + r).pow(months) + pmt * (((1 + r).pow(months) - 1) / r)
        val totalDeposited = p + pmt * months
        val profit = futureValue - totalDeposited
        resTv.text = if (isCleanGlassActive) {
            "סך הכל חיסכון עתידי:\n₪${Utils.formatResult(futureValue, 2)}\n\nסך הפקדות (קרן): ₪${Utils.formatResult(totalDeposited, 2)}\nרווח נקי מריבית: ₪${Utils.formatResult(profit, 2)}"
        } else {
            "💰 סך הכל חיסכון עתידי:\n₪${Utils.formatResult(futureValue, 2)}\n\n📥 סך הפקדות (קרן): ₪${Utils.formatResult(totalDeposited, 2)}\n✨ רווח נקי מריבית: ₪${Utils.formatResult(profit, 2)}"
        }
    }
    root.addView(initialIn); root.addView(monthlyIn); root.addView(rateIn); root.addView(yearsIn); root.addView(btn); root.addView(resTv)
    scroll.addView(root); return scroll
}
 

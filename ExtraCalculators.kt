package com.example.calculator

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// 20. מחשבון עלות דלק של רכב
fun MainActivity.createFuelCostCalculator(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(16f), dp(20f), dp(24f)) }
    var isKmPerLiter = true
    val modeBtn = createActionBtn("שיטת חישוב: ק״מ לליטר (km/L)", false) {}
    val distInput = createInputBox("מרחק נסיעה (ק״מ)")
    val effInput = createInputBox("יעילות דלק (ק״מ לליטר)")
    val priceInput = createInputBox("מחיר דלק לליטר (₪)").apply { setText("7.50") }
    val resText = createResultTextView()

    modeBtn.setOnClickListener {
        feedback(it)
        isKmPerLiter = !isKmPerLiter
        modeBtn.text = if (isKmPerLiter) "שיטת חישוב: ק״מ לליטר (km/L)" else "שיטת חישוב: ליטר ל-100 ק״מ (L/100km)"
        effInput.hint = if (isKmPerLiter) "יעילות דלק (ק״מ לליטר)" else "צריכת דלק (ליטר ל-100 ק״מ)"
    }

    val calcBtn = createActionBtn("חשב עלות דלק לנסיעה ⛽") {
        val d = distInput.text.toString().toDoubleOrNull() ?: 0.0
        val e = effInput.text.toString().toDoubleOrNull() ?: 0.0
        val p = priceInput.text.toString().toDoubleOrNull() ?: 0.0
        if (d > 0 && e > 0) {
            val litersNeeded = if (isKmPerLiter) d / e else (d / 100.0) * e
            val totalCost = litersNeeded * p
            resText.text = "דלק נדרש לנסיעה: ${Utils.formatResult(litersNeeded, 2)} ליטר\nעלות כוללת: ₪${Utils.formatResult(totalCost, 2)}\nעלות לקילומטר: ₪${Utils.formatResult(totalCost / d, 2)}"
        } else {
            resText.text = "נא להזין מרחק ויעילות דלק תקינים"
        }
    }

    root.addView(modeBtn); root.addView(distInput); root.addView(effInput); root.addView(priceInput); root.addView(calcBtn); root.addView(resText)
    scroll.addView(root); return scroll
}

// 21. מחשבון יעילות שריפת דלק
fun MainActivity.createFuelEfficiencyCalculator(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(16f), dp(20f), dp(24f)) }
    val distInput = createInputBox("מרחק שנצבר (ק״מ)")
    val fuelInput = createInputBox("כמות דלק שבשימוש (ליטרים)")
    val resText = createResultTextView()

    val calcBtn = createActionBtn("חשב יעילות שריפת דלק 🚘") {
        val d = distInput.text.toString().toDoubleOrNull() ?: 0.0
        val f = fuelInput.text.toString().toDoubleOrNull() ?: 0.0
        if (d > 0 && f > 0) {
            val kmPerL = d / f
            val lPer100Km = (f / d) * 100.0
            val mpgUs = kmPerL * 2.35215
            resText.text = "יעילות שריפת הדלק:\n${Utils.formatResult(kmPerL, 2)} ק״מ לליטר (km/L)\n\nצריכה ל-100 ק״מ: ${Utils.formatResult(lPer100Km, 2)} ליטר\nמייל לגלון (MPG US): ${Utils.formatResult(mpgUs, 1)}"
        } else {
            resText.text = "נא להזין מרחק וכמות דלק תקינים"
        }
    }

    root.addView(distInput); root.addView(fuelInput); root.addView(calcBtn); root.addView(resText)
    scroll.addView(root); return scroll
}

// 22. מחשבון ממוצע ציונים (GPA)
fun MainActivity.createGpaCalculator(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(16f), dp(20f), dp(24f)) }
    val gradesList = mutableListOf<Triple<String, Double, Double>>()

    val nameIn = EditText(this).apply {
        hint = "שם מקצוע / קורס (אופציונלי)"
        setHintTextColor(if (isCleanGlassActive) Color.parseColor("#B0BEC5") else Color.parseColor("#5C657A"))
        setTextColor(Color.WHITE); textSize = 16f; setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createGpaCalculator, 16f) else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(16f).toFloat(); setStroke(dp(1.5f), Utils.lightenColor(bgColor, 0.15f)) }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(10f) }
    }
    val gradeIn = createInputBox("ציון (0 - 100)")
    val creditIn = createInputBox("נקודות זכות / משקל (ברירת מחדל 1)").apply { setText("1") }
    val listLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(8f), 0, dp(8f)) }
    val resText = createResultTextView()

    fun updateGpaDisplay() {
        listLayout.removeAllViews()
        if (gradesList.isEmpty()) {
            resText.text = "טרם נוספו ציונים לרשימה"
            return
        }
        var totalWeighted = 0.0
        var totalCredits = 0.0
        for ((idx, item) in gradesList.withIndex()) {
            totalWeighted += item.second * item.third
            totalCredits += item.third
            val row = TextView(this).apply {
                text = "${idx + 1}. ${item.first}: ציון ${Utils.formatResult(item.second, 1)} (משקל: ${Utils.formatResult(item.third, 1)})"
                setTextColor(Color.WHITE); textSize = 15f; gravity = Gravity.RIGHT
                setPadding(dp(10f), dp(6f), dp(10f), dp(6f))
            }
            listLayout.addView(row)
        }
        val avg = if (totalCredits > 0) totalWeighted / totalCredits else 0.0
        resText.text = "ממוצע ציונים משוקלל: ${Utils.formatResult(avg, 2)}\nסה״כ נקודות זכות: ${Utils.formatResult(totalCredits, 1)} | קורסים: ${gradesList.size}"
    }

    val addBtn = createActionBtn("הוסף ציון לחישוב ➕") {
        val g = gradeIn.text.toString().toDoubleOrNull()
        val c = creditIn.text.toString().toDoubleOrNull() ?: 1.0
        if (g != null && c > 0) {
            val courseName = nameIn.text.toString().trim().ifEmpty { "קורס ${gradesList.size + 1}" }
            gradesList.add(Triple(courseName, g, c))
            nameIn.setText(""); gradeIn.setText(""); creditIn.setText("1")
            updateGpaDisplay()
        } else {
            Toast.makeText(this, "נא להזין ציון תקין", Toast.LENGTH_SHORT).show()
        }
    }

    val clearBtn = createActionBtn("אפס רשימת ציונים", false) {
        gradesList.clear()
        updateGpaDisplay()
    }

    root.addView(nameIn); root.addView(gradeIn); root.addView(creditIn); root.addView(addBtn); root.addView(clearBtn); root.addView(resText); root.addView(listLayout)
    scroll.addView(root); return scroll
}

// 23. מחשבון תשר מתקדם (אוטומטי + אי חישוב תשרים במס)
fun MainActivity.createAdvancedTipCalculator(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(16f), dp(20f), dp(24f)) }
    val billInput = createInputBox("סכום החיוב הכולל (₪)")
    val tipInput = createInputBox("אחוז התשר (%)").apply { setText("12") }
    val taxInput = createInputBox("אחוז מס כלול בחשבון (%)").apply { setText("17") }
    val dinersInput = createInputBox("מספר סועדים").apply { setText("1") }
    var excludeTaxFromTip = true

    val excludeTaxBtn = createActionBtn("אי חישוב תשר במס: פעיל (תשר לפני מס) ✔️", false) {}
    val resText = createResultTextView()

    fun recalc() {
        val bill = billInput.text.toString().toDoubleOrNull() ?: 0.0
        val tipPct = tipInput.text.toString().toDoubleOrNull() ?: 0.0
        val taxPct = taxInput.text.toString().toDoubleOrNull() ?: 0.0
        val diners = (dinersInput.text.toString().toIntOrNull() ?: 1).coerceAtLeast(1)
        if (bill <= 0) { resText.text = ""; return }

        val preTaxAmount = if (taxPct > 0) bill / (1.0 + taxPct / 100.0) else bill
        val baseForTip = if (excludeTaxFromTip) preTaxAmount else bill
        val tipAmount = baseForTip * (tipPct / 100.0)
        val finalTotal = bill + tipAmount
        val perPerson = finalTotal / diners

        resText.text = "סכום התשר להוספה: ₪${Utils.formatResult(tipAmount, 2)}\n" +
                "סכום כולל לתשלום: ₪${Utils.formatResult(finalTotal, 2)}\n" +
                "סכום לפני מס: ₪${Utils.formatResult(preTaxAmount, 2)}\n" +
                "לתשלום לכל סועד ($diners): ₪${Utils.formatResult(perPerson, 2)}"
    }

    excludeTaxBtn.setOnClickListener {
        feedback(it)
        excludeTaxFromTip = !excludeTaxFromTip
        excludeTaxBtn.text = if (excludeTaxFromTip) "אי חישוב תשר במס: פעיל (תשר לפני מס) ✔️" else "אי חישוב תשר במס: כבוי (תשר על הסכום הכולל)"
        recalc()
    }

    val watcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { recalc() }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }
    billInput.addTextChangedListener(watcher); tipInput.addTextChangedListener(watcher)
    taxInput.addTextChangedListener(watcher); dinersInput.addTextChangedListener(watcher)

    root.addView(billInput); root.addView(tipInput); root.addView(taxInput); root.addView(dinersInput); root.addView(excludeTaxBtn); root.addView(resText)
    scroll.addView(root); return scroll
}

// 24. מחשבון מס מכירות (Sales Tax)
fun MainActivity.createSalesTaxCalculator(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(16f), dp(20f), dp(24f)) }
    val origPriceInput = createInputBox("מחיר מקורי לפני מס (₪)")
    val taxRateInput = createInputBox("שיעור מס מכירות (%)").apply { setText("17") }
    val resText = createResultTextView()

    fun calcTax() {
        val price = origPriceInput.text.toString().toDoubleOrNull()
        val rate = taxRateInput.text.toString().toDoubleOrNull() ?: 0.0
        if (price == null) { resText.text = ""; return }
        val taxVal = price * (rate / 100.0)
        val total = price + taxVal
        resText.text = "מחיר כולל מס:\n₪${Utils.formatResult(total, 2)}\n\nסכום המס לתשלום: ₪${Utils.formatResult(taxVal, 2)}"
    }

    val watcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { calcTax() }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }
    origPriceInput.addTextChangedListener(watcher); taxRateInput.addTextChangedListener(watcher)
    val btn = createActionBtn("חשב מחיר כולל מס 🛍️") { calcTax() }

    root.addView(origPriceInput); root.addView(taxRateInput); root.addView(btn); root.addView(resText)
    scroll.addView(root); return scroll
}

// 25. מחשבון של מחיר יחידתי והשוואת מוצרים
fun MainActivity.createUnitPriceCalculator(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(16f), dp(20f), dp(24f)) }

    val labelA = TextView(this).apply { text = "מוצר א'"; setTextColor(Color.WHITE); textSize = 16f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.RIGHT; setPadding(0, 0, 0, dp(6f)) }
    val priceA = createInputBox("מחיר מוצר א' (₪)")
    val qtyA = createInputBox("כמות / משקל מוצר א'")

    val labelB = TextView(this).apply { text = "מוצר ב' (להשוואה - אופציונלי)"; setTextColor(Color.WHITE); textSize = 16f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.RIGHT; setPadding(0, dp(8f), 0, dp(6f)) }
    val priceB = createInputBox("מחיר מוצר ב' (₪)")
    val qtyB = createInputBox("כמות / משקל מוצר ב'")

    val resText = createResultTextView()

    val btn = createActionBtn("חשב והשווה מחיר יחידתי ⚖️") {
        val pa = priceA.text.toString().toDoubleOrNull()
        val qa = qtyA.text.toString().toDoubleOrNull()
        val pb = priceB.text.toString().toDoubleOrNull()
        val qb = qtyB.text.toString().toDoubleOrNull()

        if (pa == null || qa == null || qa <= 0) {
            resText.text = "נא להזין מחיר וכמות תקינים למוצר א'"
            return@createActionBtn
        }
        val unitA = pa / qa
        var out = "מחיר ליחידה (מוצר א'): ₪${Utils.formatResult(unitA, 4)}"

        if (pb != null && qb != null && qb > 0) {
            val unitB = pb / qb
            out += "\nמחיר ליחידה (מוצר ב'): ₪${Utils.formatResult(unitB, 4)}\n\n"
            out += when {
                unitA < unitB -> {
                    val savePct = ((unitB - unitA) / unitB) * 100.0
                    "🏆 מוצר א' משתלם יותר ב-${Utils.formatResult(savePct, 1)}%!"
                }
                unitB < unitA -> {
                    val savePct = ((unitA - unitB) / unitA) * 100.0
                    "🏆 מוצר ב' משתלם יותר ב-${Utils.formatResult(savePct, 1)}%!"
                }
                else -> "שני המוצרים בעלי אותו מחיר יחידתי בדיוק!"
            }
        }
        resText.text = out
    }

    root.addView(labelA); root.addView(priceA); root.addView(qtyA)
    root.addView(labelB); root.addView(priceB); root.addView(qtyB)
    root.addView(btn); root.addView(resText)
    scroll.addView(root); return scroll
}

// 26. ממיר שעון עולמי (400+ ערים ואזורי זמן כולל חישוב שעון קיץ)
fun MainActivity.createWorldClockConverter(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(16f), dp(20f), dp(24f)) }

    val priorityCities = mapOf(
        "Asia/Jerusalem" to "ירושלים / תל אביב (ישראל)",
        "America/New_York" to "ניו יורק (ארה״ב)",
        "Europe/London" to "לונדון (בריטניה)",
        "Europe/Paris" to "פריז (צרפת)",
        "Europe/Berlin" to "ברלין (גרמניה)",
        "Asia/Tokyo" to "טוקיו (יפן)",
        "Australia/Sydney" to "סידני (אוסטרליה)",
        "America/Los_Angeles" to "לוס אנג׳לס (ארה״ב)",
        "Asia/Dubai" to "דובאי (איחוד האמירויות)",
        "Asia/Bangkok" to "בנגקוק (תאילנד)",
        "America/Toronto" to "טורונטו (קנדה)",
        "Europe/Rome" to "רומא (איטליה)",
        "Europe/Madrid" to "מדריד (ספרד)",
        "Europe/Moscow" to "מוסקבה (רוסיה)",
        "Asia/Shanghai" to "שנגחאי (סין)",
        "Asia/Singapore" to "סינגפור",
        "Asia/Hong_Kong" to "הונג קונג",
        "America/Sao_Paulo" to "סאו פאולו (ברזיל)",
        "America/Buenos_Aires" to "בואנוס איירס (ארגנטינה)",
        "Europe/Zurich" to "ציריך (שווייץ)",
        "Europe/Amsterdam" to "אמסטרדם (הולנד)"
    )

    val allZoneItems = rememberWorldZones(priorityCities)
    var selectedZoneId = "America/New_York"
    var selectedDisplay = "${priorityCities[selectedZoneId]} - $selectedZoneId"

    val cityBtn = createActionBtn("עיר יעד: $selectedDisplay", false) {}
    val resCard = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(20f), dp(22f), dp(20f), dp(22f))
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createWorldClockConverter, 24f) else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.08f)); cornerRadius = dp(22f).toFloat(); setStroke(dp(1.5f), primaryColor) }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12f) }
    }
    val mainClockTv = TextView(this).apply { setTextColor(Color.WHITE); textSize = 26f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER }
    val subClockTv = TextView(this).apply { setTextColor(if (isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else primaryColor); textSize = 15f; gravity = Gravity.CENTER; setPadding(0, dp(10f), 0, 0) }
    resCard.addView(mainClockTv); resCard.addView(subClockTv)

    fun updateWorldTime() {
        val now = Date()
        val targetTz = TimeZone.getTimeZone(selectedZoneId)
        val localTz = TimeZone.getDefault()

        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply { timeZone = targetTz }
        val dateFmt = SimpleDateFormat("dd/MM/yyyy (EEEE)", Locale("he", "IL")).apply { timeZone = targetTz }
        val localFmt = SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = localTz }

        val isDstTarget = targetTz.inDaylightTime(now)
        val dstStatus = if (isDstTarget) "שעון קיץ פעיל ☀️ (DST)" else "שעון חורף תקני ❄️"
        val diffHours = (targetTz.getOffset(now.time) - localTz.getOffset(now.time)) / (1000.0 * 3600.0)
        val diffStr = if (diffHours >= 0) "+${Utils.formatResult(diffHours, 1)}" else Utils.formatResult(diffHours, 1)

        mainClockTv.text = "🕒 ${timeFmt.format(now)}\n${dateFmt.format(now)}"
        subClockTv.text = "$dstStatus\nהפרש מהשעון המקומי (${localFmt.format(now)}): $diffStr שעות"
    }

    cityBtn.setOnClickListener {
        showItemPicker("בחר עיר (${allZoneItems.size} ערים בעולם)", allZoneItems) { chosen ->
            selectedDisplay = chosen
            selectedZoneId = chosen.substringAfterLast(" - ").trim()
            cityBtn.text = "עיר יעד: $selectedDisplay"
            updateWorldTime()
        }
    }

    val refreshBtn = createActionBtn("רענן שעה נוכחית 🔄") { updateWorldTime() }
    updateWorldTime()

    root.addView(cityBtn); root.addView(refreshBtn); root.addView(resCard)
    scroll.addView(root); return scroll
}

private fun rememberWorldZones(priorityCities: Map<String, String>): List<String> {
    val list = mutableListOf<String>()
    for ((id, heb) in priorityCities) {
        list.add("$heb - $id")
    }
    val allIds = TimeZone.getAvailableIDs().filter { it.contains("/") && !it.startsWith("Etc/") && !priorityCities.containsKey(it) }.sorted()
    for (id in allIds) {
        val city = id.substringAfterLast("/").replace("_", " ")
        val region = id.substringBefore("/")
        list.add("$city ($region) - $id")
    }
    return list
}

// 27. מחשבון ביוץ וחלון פוריות (כולל שמירת הערות לתאריכים)
fun MainActivity.createOvulationCalculator(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(16f), dp(20f), dp(24f)) }
    val selectedCal = Calendar.getInstance()
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val dateBtn = createActionBtn("תאריך תחילת מחזור אחרון: ${sdf.format(selectedCal.time)}", false) {}
    val cycleLenInput = createInputBox("אורך מחזור ממוצע בימים (למשל 28)").apply { setText("28") }

    val noteInput = EditText(this).apply {
        hint = "רשום הערה לתאריך זה..."
        setHintTextColor(if (isCleanGlassActive) Color.parseColor("#B0BEC5") else Color.parseColor("#5C657A"))
        setTextColor(Color.WHITE); textSize = 16f; setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createOvulationCalculator, 16f) else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(16f).toFloat(); setStroke(dp(1.5f), Utils.lightenColor(bgColor, 0.15f)) }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12f) }
    }

    val resText = createResultTextView()
    val savedNotesTv = TextView(this).apply { setTextColor(if (isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else primaryColor); textSize = 15f; gravity = Gravity.RIGHT; setPadding(0, dp(14f), 0, 0) }

    fun loadSavedNotes() {
        val rawNotes = prefs?.getString("ovulation_notes", "") ?: ""
        savedNotesTv.text = if (rawNotes.isNotEmpty()) "📝 הערות שמורות:\n$rawNotes" else ""
    }

    dateBtn.setOnClickListener {
        feedback(it)
        DatePickerDialog(this, { _, y, m, d ->
            selectedCal.set(y, m, d)
            dateBtn.text = "תאריך תחילת מחזור אחרון: ${sdf.format(selectedCal.time)}"
        }, selectedCal.get(Calendar.YEAR), selectedCal.get(Calendar.MONTH), selectedCal.get(Calendar.DAY_OF_MONTH)).show()
    }

    val calcBtn = createActionBtn("חשב מועד ביוץ וחלון פוריות 🌸") {
        val cycleLen = (cycleLenInput.text.toString().toIntOrNull() ?: 28).coerceIn(20, 45)
        val nextPeriod = (selectedCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, cycleLen) }
        val ovulationDay = (nextPeriod.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -14) }
        val fertileStart = (ovulationDay.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -5) }
        val fertileEnd = (ovulationDay.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }

        resText.text = "יום הביוץ המשוער:\n${sdf.format(ovulationDay.time)}\n\n" +
                "חלון הפוריות המיטבי:\n${sdf.format(fertileStart.time)} עד ${sdf.format(fertileEnd.time)}\n\n" +
                "המחזור הבא המשוער:\n${sdf.format(nextPeriod.time)}"

        val noteStr = noteInput.text.toString().trim()
        if (noteStr.isNotEmpty()) {
            val prev = prefs?.getString("ovulation_notes", "") ?: ""
            val entry = "• ${sdf.format(selectedCal.time)}: $noteStr"
            val updated = if (prev.isEmpty()) entry else "$entry\n$prev"
            prefs?.edit()?.putString("ovulation_notes", updated)?.apply()
            noteInput.setText("")
            loadSavedNotes()
        }
    }

    val clearNotesBtn = createActionBtn("מחק הערות שמורות", false) {
        prefs?.edit()?.remove("ovulation_notes")?.apply()
        loadSavedNotes()
    }

    loadSavedNotes()
    root.addView(dateBtn); root.addView(cycleLenInput); root.addView(noteInput); root.addView(calcBtn); root.addView(clearNotesBtn); root.addView(resText); root.addView(savedNotesTv)
    scroll.addView(root); return scroll
}

// 28. ממיר הקסדצימלי דו-כיווני (DEC <-> HEX)
fun MainActivity.createHexConverter(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(16f), dp(20f), dp(24f)) }
    val decInput = createInputBox("מספר עשרוני (Decimal - בסיס 10)")
    val hexInput = EditText(this).apply {
        hint = "מספר הקסדצימלי (Hex - בסיס 16, למשל FF)"
        setHintTextColor(if (isCleanGlassActive) Color.parseColor("#B0BEC5") else Color.parseColor("#5C657A"))
        setTextColor(Color.WHITE); textSize = 17f; setPadding(dp(18f), dp(18f), dp(18f), dp(18f))
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createHexConverter, 16f) else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(16f).toFloat(); setStroke(dp(1.5f), Utils.lightenColor(bgColor, 0.15f)) }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(14f) }
    }
    val resText = createResultTextView()
    var isUpdating = false

    decInput.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            if (isUpdating) return
            isUpdating = true
            val v = s.toString().trim().toLongOrNull()
            if (v != null) {
                val h = java.lang.Long.toHexString(v).uppercase()
                hexInput.setText(h)
                resText.text = "DEC: $v\nHEX: 0x$h\nBIN: ${java.lang.Long.toBinaryString(v)}"
            } else {
                hexInput.setText("")
                resText.text = ""
            }
            isUpdating = false
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    hexInput.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            if (isUpdating) return
            isUpdating = true
            val clean = s.toString().trim().removePrefix("0x").removePrefix("0X")
            val v = clean.toLongOrNull(16)
            if (v != null) {
                decInput.setText(v.toString())
                resText.text = "HEX: 0x${clean.uppercase()}\nDEC: $v\nBIN: ${java.lang.Long.toBinaryString(v)}"
            } else {
                decInput.setText("")
                resText.text = ""
            }
            isUpdating = false
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    root.addView(decInput); root.addView(hexInput); root.addView(resText)
    scroll.addView(root); return scroll
}

// 29. מחשבון חסכונות (כולל חישוב ריבית לאחר מס ויתרה סופית)
fun MainActivity.createSavingsCalculator(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(16f), dp(20f), dp(24f)) }
    val depositInput = createInputBox("סכום הפקדה (₪)")
    val rateInput = createInputBox("שיעור ריבית שנתית (%)")
    val yearsInput = createInputBox("תקופת החיסכון (שנים)")
    val taxRateInput = createInputBox("שיעור מס רווחי הון על הריבית (%)").apply { setText("15") }
    val resText = createResultTextView()

    val btn = createActionBtn("חשב ריבית לאחר מס ויתרה סופית 🏦") {
        val p = depositInput.text.toString().toDoubleOrNull() ?: 0.0
        val r = (rateInput.text.toString().toDoubleOrNull() ?: 0.0) / 100.0
        val t = yearsInput.text.toString().toDoubleOrNull() ?: 0.0
        val taxPct = (taxRateInput.text.toString().toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)

        if (p > 0 && t > 0) {
            val grossFinal = p * Math.pow(1.0 + r, t)
            val grossInterest = grossFinal - p
            val taxAmount = grossInterest * (taxPct / 100.0)
            val netInterest = grossInterest - taxAmount
            val netFinalBalance = p + netInterest

            resText.text = "יתרת החסכונות הסופית (לאחר מס):\n₪${Utils.formatResult(netFinalBalance, 2)}\n\n" +
                    "ריבית נטו לאחר מס: ₪${Utils.formatResult(netInterest, 2)}\n" +
                    "ריבית ברוטו (לפני מס): ₪${Utils.formatResult(grossInterest, 2)}\n" +
                    "ניכוי מס (${Utils.formatResult(taxPct, 1)}%): ₪${Utils.formatResult(taxAmount, 2)}"
        } else {
            resText.text = "נא להזין סכום הפקדה ותקופה תקינים"
        }
    }

    root.addView(depositInput); root.addView(rateInput); root.addView(yearsInput); root.addView(taxRateInput); root.addView(btn); root.addView(resText)
    scroll.addView(root); return scroll
}

// 30. מחשבון תאריך ויום שנה
fun MainActivity.createDateAnniversaryCalculator(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(16f), dp(20f), dp(24f)) }
    val calStart = Calendar.getInstance()
    val calTarget = Calendar.getInstance()
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val startBtn = createActionBtn("תאריך התחלה / אירוע: ${sdf.format(calStart.time)}", false) {}
    val targetBtn = createActionBtn("תאריך יעד להשוואה: ${sdf.format(calTarget.time)}", false) {}
    val daysOffsetInput = createInputBox("או הזן מספר ימים להוספה/החסרה מתאריך ההתחלה")
    val resText = createResultTextView()

    fun calcDiff() {
        val diffMillis = calTarget.timeInMillis - calStart.timeInMillis
        val diffDays = abs(diffMillis / (1000 * 60 * 60 * 24))
        val weeks = diffDays / 7
        val remDays = diffDays % 7

        val nextAnniversary = (calStart.clone() as Calendar).apply {
            set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR))
            if (before(Calendar.getInstance())) add(Calendar.YEAR, 1)
        }
        val daysToAnniv = (nextAnniversary.timeInMillis - Calendar.getInstance().timeInMillis) / (1000 * 60 * 60 * 24)

        resText.text = "הפרש בין התאריכים: $diffDays ימים\n" +
                "($weeks שבועות ו-$remDays ימים)\n\n" +
                "יום השנה הבא יחול ב-${sdf.format(nextAnniversary.time)}\n" +
                "(בעוד ${daysToAnniv.coerceAtLeast(0)} ימים)"
    }

    startBtn.setOnClickListener {
        feedback(it)
        DatePickerDialog(this, { _, y, m, d ->
            calStart.set(y, m, d)
            startBtn.text = "תאריך התחלה / אירוע: ${sdf.format(calStart.time)}"
            calcDiff()
        }, calStart.get(Calendar.YEAR), calStart.get(Calendar.MONTH), calStart.get(Calendar.DAY_OF_MONTH)).show()
    }

    targetBtn.setOnClickListener {
        feedback(it)
        DatePickerDialog(this, { _, y, m, d ->
            calTarget.set(y, m, d)
            targetBtn.text = "תאריך יעד להשוואה: ${sdf.format(calTarget.time)}"
            calcDiff()
        }, calTarget.get(Calendar.YEAR), calTarget.get(Calendar.MONTH), calTarget.get(Calendar.DAY_OF_MONTH)).show()
    }

    val addDaysBtn = createActionBtn("חשב תאריך יעד לפי ימים 📅") {
        val offset = daysOffsetInput.text.toString().toIntOrNull()
        if (offset != null) {
            val resCal = (calStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            resText.text = "התאריך לאחר $offset ימים:\n${sdf.format(resCal.time)}"
        } else {
            calcDiff()
        }
    }

    calcDiff()
    root.addView(startBtn); root.addView(targetBtn); root.addView(daysOffsetInput); root.addView(addDaysBtn); root.addView(resText)
    scroll.addView(root); return scroll
}
 

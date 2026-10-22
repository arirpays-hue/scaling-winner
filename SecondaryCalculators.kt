package com.example.calculator

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.net.URL
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.pow
import kotlin.math.sqrt

// --- מחשבונים משניים ---
fun MainActivity.createVatCalculator(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    val amountInput = createInputBox("הכנס סכום (₪)")
    val vatInput = createInputBox("אחוז מע״מ (ברירת מחדל 17%)").apply { setText("17") }
    val resText = createResultTextView()
    val btnAdd = createActionBtn("הוסף מע״מ לסכום") { val a = amountInput.text.toString().toDoubleOrNull() ?: 0.0; val v = vatInput.text.toString().toDoubleOrNull() ?: 17.0; val res = a * (1 + v / 100); resText.text = "סך הכל כולל מע״מ:\n${Utils.formatResult(res, 2)} ₪" }
    val btnSub = createActionBtn("חלץ מע״מ מהסכום", false) { val a = amountInput.text.toString().toDoubleOrNull() ?: 0.0; val v = vatInput.text.toString().toDoubleOrNull() ?: 17.0; val res = a / (1 + v / 100); val vatAmount = a - res; resText.text = "סכום לפני מע״מ: ${Utils.formatResult(res, 2)} ₪\nסכום המע״מ: ${Utils.formatResult(vatAmount, 2)} ₪" }
    root.addView(amountInput); root.addView(vatInput); root.addView(btnAdd); root.addView(btnSub); root.addView(resText); return root
}

fun MainActivity.createLoanCalculator(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    val amountInput = createInputBox("סכום ההלוואה (₪)")
    val monthsInput = createInputBox("תקופה (בחודשים)")
    val interestInput = createInputBox("ריבית שנתית (%)")
    val resText = createResultTextView()
    val btn = createActionBtn("חשב החזר חודשי") {
        val p = amountInput.text.toString().toDoubleOrNull() ?: 0.0; val n = monthsInput.text.toString().toDoubleOrNull() ?: 0.0; val r = (interestInput.text.toString().toDoubleOrNull() ?: 0.0) / 100 / 12
        if (p > 0 && n > 0) { val res = if (r == 0.0) p / n else p * (r * (1 + r).pow(n)) / ((1 + r).pow(n) - 1); val total = res * n; resText.text = "החזר חודשי: ${Utils.formatResult(res, 2)} ₪\nסך הכל החזר: ${Utils.formatResult(total, 2)} ₪" }
    }
    root.addView(amountInput); root.addView(monthsInput); root.addView(interestInput); root.addView(btn); root.addView(resText); return root
}

fun MainActivity.createConverterLayout(title: String, options: List<String>, currentFrom: String, currentTo: String, saveFrom: (String) -> Unit, saveTo: (String) -> Unit, calc: (Double, String, String) -> Double): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    var locFrom = currentFrom; var locTo = currentTo
    val btnFrom = createActionBtn("מ: $locFrom", false) {}
    val btnTo = createActionBtn("ל: $locTo", false) {}
    val input = createInputBox("הכנס ערך")
    val resText = createResultTextView()
    fun update() { savePreferences(); btnFrom.text = "מ: $locFrom"; btnTo.text = "ל: $locTo"; val v = input.text.toString().toDoubleOrNull(); if (v != null) resText.text = Utils.formatResult(calc(v, locFrom, locTo), 4) else resText.text = "" }
    btnFrom.setOnClickListener { feedback(it); showItemPicker("בחר מקור", options) { sel -> locFrom = sel; saveFrom(sel); update() } }
    btnTo.setOnClickListener { feedback(it); showItemPicker("בחר יעד", options) { sel -> locTo = sel; saveTo(sel); update() } }
    input.addTextChangedListener(object : TextWatcher { override fun afterTextChanged(s: Editable?) { update() }; override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}; override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {} })
    root.addView(btnFrom); root.addView(btnTo); root.addView(input); root.addView(resText); return root
}

fun MainActivity.createProgrammerCalculator(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    val input = createInputBox("הכנס מספר עשרוני (Base 10)")
    val resText = TextView(this).apply { setTextColor(Color.WHITE); textSize = 18f; setPadding(0, dp(20f), 0, 0); setLineSpacing(dp(8f).toFloat(), 1.2f) }
    input.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            val v = s.toString().toLongOrNull()
            if (v != null) resText.text = "HEX (בסיס 16):\n${v.toString(16).uppercase()}\n\nOCT (בסיס 8):\n${v.toString(8)}\n\nBIN (בסיס 2):\n${v.toString(2)}" else resText.text = ""
        }; override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}; override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
    root.addView(input); root.addView(resText); return root
}
fun MainActivity.createPercentageCalculator(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    val val1 = createInputBox("חלק / אחוז"); val val2 = createInputBox("שלם / סכום"); val resText = createResultTextView()
    val btn1 = createActionBtn("כמה זה X% מתוך Y?") { val x = val1.text.toString().toDoubleOrNull() ?: 0.0; val y = val2.text.toString().toDoubleOrNull() ?: 0.0; resText.text = "תוצאה: ${Utils.formatResult(x * y / 100, 4)}" }
    val btn2 = createActionBtn("איזה אחוז זה X מתוך Y?", false) { val x = val1.text.toString().toDoubleOrNull() ?: 0.0; val y = val2.text.toString().toDoubleOrNull() ?: 0.0; if (y != 0.0) resText.text = "תוצאה: ${Utils.formatResult(x / y * 100, 4)}%" }
    root.addView(val1); root.addView(val2); root.addView(btn1); root.addView(btn2); root.addView(resText); return root
}

fun MainActivity.createDiscountCalculator(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    val price = createInputBox("מחיר מקורי"); val discount = createInputBox("אחוז הנחה (%)"); val resText = createResultTextView()
    fun calc() { val p = price.text.toString().toDoubleOrNull() ?: 0.0; val d = discount.text.toString().toDoubleOrNull() ?: 0.0; val saved = p * (d / 100); resText.text = "מחיר סופי: ${Utils.formatResult(p - saved, 2)}\nנחסך: ${Utils.formatResult(saved, 2)}" }
    val tw = object : TextWatcher { override fun afterTextChanged(s: Editable?) { calc() }; override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}; override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {} }
    price.addTextChangedListener(tw); discount.addTextChangedListener(tw); root.addView(price); root.addView(discount); root.addView(resText); return root
}

fun MainActivity.createTipCalculator(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    val bill = createInputBox("סכום החשבון"); val tip = createInputBox("אחוז טיפ (%)").apply { setText("10") }; val people = createInputBox("מספר סועדים").apply { setText("1") }; val resText = createResultTextView()
    fun calc() { val b = bill.text.toString().toDoubleOrNull() ?: 0.0; val t = tip.text.toString().toDoubleOrNull() ?: 10.0; val p = people.text.toString().toDoubleOrNull() ?: 1.0; if (p > 0) { val tipAmt = b * (t / 100); val total = b + tipAmt; resText.text = "טיפ סך הכל: ${Utils.formatResult(tipAmt, 2)}\nחשבון סופי: ${Utils.formatResult(total, 2)}\n\nלתשלום לכל סועד: ${Utils.formatResult(total / p, 2)}" } }
    val tw = object : TextWatcher { override fun afterTextChanged(s: Editable?) { calc() }; override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}; override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {} }
    bill.addTextChangedListener(tw); tip.addTextChangedListener(tw); people.addTextChangedListener(tw); root.addView(bill); root.addView(tip); root.addView(people); root.addView(resText); return root
}

fun MainActivity.createBMICalculator(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    val weight = createInputBox("משקל (ק״ג)"); val height = createInputBox("גובה (ס״מ)"); val resText = createResultTextView()
    val btn = createActionBtn("חשב BMI") { val w = weight.text.toString().toDoubleOrNull() ?: 0.0; val h = height.text.toString().toDoubleOrNull() ?: 0.0; if (h > 0) { val hMeters = h / 100; val bmi = w / (hMeters * hMeters); val status = when { bmi < 18.5 -> "תת משקל"; bmi < 25 -> "משקל תקין"; bmi < 30 -> "עודף משקל"; else -> "השמנת יתר" }; resText.text = "ה-BMI שלך: ${Utils.formatResult(bmi, 1)}\nמצב: $status" } }
    root.addView(weight); root.addView(height); root.addView(btn); root.addView(resText); return root
}

fun MainActivity.createAgeCalculator(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    val btn = createActionBtn("בחר תאריך לידה") {}
    val resText = createResultTextView()
    btn.setOnClickListener {
        feedback(it); val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val dob = Calendar.getInstance().apply { set(y, m, d) }; val today = Calendar.getInstance()
            var ageYears = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) ageYears--
            val diff = today.timeInMillis - dob.timeInMillis; val days = diff / (1000 * 60 * 60 * 24)
            resText.text = "גיל בשנים: $ageYears\nגיל בימים: $days ימים"
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }
    root.addView(btn); root.addView(resText); return root
}

fun MainActivity.createBmrCalculator(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    var isMale = true
    val maleLabel = if (isCleanGlassActive) "מין: גבר" else "מין: גבר 👨"
    val femaleLabel = if (isCleanGlassActive) "מין: אישה" else "מין: אישה 👩"
    val genderBtn = createActionBtn(maleLabel, false) {}
    genderBtn.setOnClickListener { feedback(it); isMale = !isMale; genderBtn.text = if (isMale) maleLabel else femaleLabel }
    val weight = createInputBox("משקל (ק״ג)"); val height = createInputBox("גובה (ס״מ)"); val age = createInputBox("גיל")
    val resText = createResultTextView()
    val btn = createActionBtn("חשב BMR") {
        val w = weight.text.toString().toDoubleOrNull() ?: 0.0; val h = height.text.toString().toDoubleOrNull() ?: 0.0; val a = age.text.toString().toDoubleOrNull() ?: 0.0
        if (w > 0 && h > 0 && a > 0) { val bmr = if (isMale) 88.362 + (13.397 * w) + (4.799 * h) - (5.677 * a) else 447.593 + (9.247 * w) + (3.098 * h) - (4.330 * a); resText.text = "שריפת קלוריות במנוחה (BMR):\n${Utils.formatResult(bmr, 0)} קלוריות ליום" }
    }
    root.addView(genderBtn); root.addView(weight); root.addView(height); root.addView(age); root.addView(btn); root.addView(resText); return root
}
fun MainActivity.createQuadraticCalculator(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    val aIn = createInputBox("מקדם a"); val bIn = createInputBox("מקדם b"); val cIn = createInputBox("מקדם c")
    val resText = createResultTextView()
    val btn = createActionBtn("חשב שורשים") {
        val a = aIn.text.toString().toDoubleOrNull() ?: 0.0; val b = bIn.text.toString().toDoubleOrNull() ?: 0.0; val c = cIn.text.toString().toDoubleOrNull() ?: 0.0
        if (a == 0.0) { resText.text = "לא משוואה ריבועית (a=0)" } else {
            val delta = b * b - 4 * a * c
            when { delta > 0 -> { val x1 = (-b + sqrt(delta)) / (2 * a); val x2 = (-b - sqrt(delta)) / (2 * a); resText.text = "X₁ = ${Utils.formatResult(x1, 4)}\nX₂ = ${Utils.formatResult(x2, 4)}" }; delta == 0.0 -> resText.text = "X = ${Utils.formatResult(-b / (2 * a), 4)}"; else -> resText.text = "אין פתרון ממשי (Δ < 0)" }
        }
    }
    root.addView(aIn); root.addView(bIn); root.addView(cIn); root.addView(btn); root.addView(resText); return root
}

fun MainActivity.createFractionCalculator(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    val n1 = createInputBox("מונה 1"); val d1 = createInputBox("מכנה 1")
    val opBtn = createActionBtn("פעולה: חיבור (+)", false) {}
    var op = "+"
    opBtn.setOnClickListener { feedback(it); val ops = arrayOf("+", "-", "×", "÷"); val next = (ops.indexOf(op) + 1) % ops.size; op = ops[next]; opBtn.text = "פעולה: $op" }
    val n2 = createInputBox("מונה 2"); val d2 = createInputBox("מכנה 2"); val resText = createResultTextView()
    val btn = createActionBtn("חשב שבר") {
        val a = n1.text.toString().toDoubleOrNull() ?: 0.0; val b = d1.text.toString().toDoubleOrNull() ?: 1.0
        val c = n2.text.toString().toDoubleOrNull() ?: 0.0; val d = d2.text.toString().toDoubleOrNull() ?: 1.0
        if (b == 0.0 || d == 0.0) { resText.text = "חלוקה באפס!" } else {
            var rn = 0.0; var rd = 1.0
            when (op) { "+" -> { rn = a * d + c * b; rd = b * d }; "-" -> { rn = a * d - c * b; rd = b * d }; "×" -> { rn = a * c; rd = b * d }; "÷" -> { rn = a * d; rd = b * c } }
            resText.text = "${Utils.formatResult(rn, 0)} / ${Utils.formatResult(rd, 0)}\n= ${Utils.formatResult(rn / rd, 4)}"
        }
    }
    root.addView(n1); root.addView(d1); root.addView(opBtn); root.addView(n2); root.addView(d2); root.addView(btn); root.addView(resText); return root
}

fun MainActivity.createSettingsPage(): View {
    val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(0f), dp(20f), dp(24f)) }
   
    val titleThemes = TextView(this).apply { text = if (isCleanGlassActive) "18 ערכות נושא ופלטות צבעים מוכנות" else "🎨 18 ערכות נושא ופלטות צבעים מוכנות"; setTextColor(Color.WHITE); textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(12f), 0, dp(12f)) }
    val themeScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(16f) } }
    val themeContainer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    val presetThemes = listOf(
        Triple("OLED", "#64B5F6", "#08090C"), Triple("iOS Glass", "#FF9F0A", "#1C1C1E"),
        Triple("Matrix", "#00E676", "#0A190F"), Triple("Barbie", "#FF4081", "#2A0A18"),
        Triple("Ocean", "#00B4D8", "#0A1128"), Triple("Sunset", "#FFB703", "#1A0F0A"),
        Triple("Dracula", "#BD93F9", "#282A36"), Triple("Cyber", "#FF007F", "#120458"),
        Triple("Gold VIP", "#FFD700", "#141109"), Triple("Emerald", "#10B981", "#061E16"),
        Triple("Cherry", "#FF4D6D", "#220910"), Triple("Lavender", "#C084FC", "#191024"),
        Triple("Nordic", "#88C0D0", "#2E3440"), Triple("Coffee", "#D4A373", "#1D1611"),
        Triple("Neon Lime", "#AACC00", "#111805"), Triple("Crimson", "#EF233C", "#1B0609"),
        Triple("Midnight", "#38BDF8", "#020617"), Triple("Synthwave", "#F72585", "#10002B")
    )
    for (theme in presetThemes) { val tName = theme.first; val tPri = Color.parseColor(theme.second); val tBg = Color.parseColor(theme.third); val btn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(12f), dp(12f), dp(12f), dp(12f)); background = GradientDrawable().apply { setColor(tBg); cornerRadius = dp(16f).toFloat(); setStroke(dp(2f), tPri) }; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(6f), dp(6f), dp(6f), dp(6f)) }; addView(TextView(this@createSettingsPage).apply { text = tName; setTextColor(tPri); textSize = 14f; typeface = Typeface.DEFAULT_BOLD }); setOnClickListener { feedback(it); primaryColor = tPri; bgColor = tBg; savePreferences(); applyTheme() } }; themeContainer.addView(btn) }
    themeScroll.addView(themeContainer)

    val currentModeLabel = when {
        isAuroraMode -> IosGlassTheme.AURORA_THEME_NAME
        isIosGlassMode -> "עיצוב הזכוכית (${IosGlassTheme.LIQUID_GLASS_THEME_NAME})"
        else -> "עיצוב רגיל"
    }
    val iosStyleBtn = createActionBtn(
        if (isCleanGlassActive) "מצב תצוגה: $currentModeLabel" else "✨ מצב תצוגה: $currentModeLabel",
        true
    ) {
        showGlassDisplayModePickerDialog()
    }
    root.addView(titleThemes); root.addView(themeScroll); root.addView(iosStyleBtn)

    // --- פאנל התאמה אישית מיוחד שמופיע רק כאשר עיצוב הזכוכית מופעל ---
    if (isIosGlassMode && !isAuroraMode) {
        val iosPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), dp(16f), dp(16f), dp(16f))
            background = IosGlassTheme.createGlassCardDrawable(this@createSettingsPage, 22f)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8f); bottomMargin = dp(12f)
            }
        }
        val iosHeader = TextView(this).apply {
            text = "התאמה אישית מלאה - iOS 26 Liquid Glass"
            setTextColor(primaryColor); textSize = 16f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.RIGHT
            setPadding(0, 0, 0, dp(12f))
        }
        iosPanel.addView(iosHeader)

        fun addIosSlider(labelPrefix: String, minVal: Int, maxVal: Int, currentVal: Int, unit: String, onValueChange: (Int) -> Unit) {
            val lbl = TextView(this).apply {
                text = "$labelPrefix: $currentVal$unit"
                setTextColor(Color.WHITE); textSize = 14f; gravity = Gravity.RIGHT; setPadding(0, dp(6f), 0, dp(4f))
            }
            val seek = SeekBar(this).apply {
                max = maxVal - minVal
                progress = (currentVal - minVal).coerceIn(0, maxVal - minVal)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                        val v = minVal + p
                        lbl.text = "$labelPrefix: $v$unit"
                        if (fromUser) onValueChange(v)
                    }
                    override fun onStartTrackingTouch(s: SeekBar?) {}
                    override fun onStopTrackingTouch(s: SeekBar?) {
                        savePreferences()
                        iosPanel.background = IosGlassTheme.createGlassCardDrawable(this@createSettingsPage, 22f)
                    }
                })
            }
            iosPanel.addView(lbl); iosPanel.addView(seek)
        }

        addIosSlider("שקיפות ועומק זכוכית (Liquid Opacity)", 75, 245, IosGlassTheme.glassOpacity, "") { IosGlassTheme.glassOpacity = it }
        addIosSlider("עוצמת טשטוש והשתקפות (Dynamic Blur)", 0, 50, IosGlassTheme.glassBlurRadius.toInt(), "px") { IosGlassTheme.glassBlurRadius = it.toFloat() }
        addIosSlider("הברקת קצה 1px לבנה (Specular Edge)", 40, 255, IosGlassTheme.specularIntensity, "") { IosGlassTheme.specularIntensity = it }
        addIosSlider("רדיוס קמירות מקשי הזכוכית (Corner Radius)", 10, 38, IosGlassTheme.glassCornerPt.toInt(), "pt") { IosGlassTheme.glassCornerPt = it.toFloat() }
        addIosSlider("עוצמת הארת פולימר בלחיצה (Press Glow)", 60, 255, IosGlassTheme.pressGlowIntensity, "") { IosGlassTheme.pressGlowIntensity = it }

        val depthModeBtn = createActionBtn(
            if (IosGlassTheme.symbolDepthMode == 0) "סגנון סמלים (SF Symbols 6): חרוטים בזכוכית (Etched)" else "סגנון סמלים (SF Symbols 6): מרחפים 1 מ״מ מעל הזכוכית",
            false
        ) {
            IosGlassTheme.symbolDepthMode = if (IosGlassTheme.symbolDepthMode == 0) 1 else 0
            savePreferences(); switchTab(19)
        }

        val orangeOpsBtn = createActionBtn(
            if (IosGlassTheme.useClassicOrangeOps) "צבע מקשי פעולות (+, −, ×, ÷, =): כתום זכוכית מקורי של אפל" else "צבע מקשי פעולות (+, −, ×, ÷, =): מותאם לצבע הנושא",
            false
        ) {
            IosGlassTheme.useClassicOrangeOps = !IosGlassTheme.useClassicOrangeOps
            savePreferences(); switchTab(19)
        }

        iosPanel.addView(depthModeBtn); iosPanel.addView(orangeOpsBtn)
        root.addView(iosPanel)
    }

    // --- פאנל התאמה אישית מיוחד שמופיע כאשר הזוהר הצפוני (Aurora Borealis) מופעל ---
    if (isAuroraMode) {
        val auroraPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), dp(16f), dp(16f), dp(16f))
            background = IosGlassTheme.createGlassCardDrawable(this@createSettingsPage, 22f)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8f); bottomMargin = dp(12f)
            }
        }
        val auroraHeader = TextView(this).apply {
            text = "התאמה אישית מלאה - ${IosGlassTheme.AURORA_THEME_NAME}"
            setTextColor(AuroraBorealisTheme.AURORA_PRIMARY); textSize = 16f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.RIGHT
            setPadding(0, 0, 0, dp(12f))
        }
        auroraPanel.addView(auroraHeader)

        fun addAuroraSlider(labelPrefix: String, minVal: Int, maxVal: Int, currentVal: Int, unit: String, onValueChange: (Int) -> Unit) {
            val lbl = TextView(this).apply {
                text = "$labelPrefix: $currentVal$unit"
                setTextColor(Color.WHITE); textSize = 14f; gravity = Gravity.RIGHT; setPadding(0, dp(6f), 0, dp(4f))
            }
            val seek = SeekBar(this).apply {
                max = maxVal - minVal
                progress = (currentVal - minVal).coerceIn(0, maxVal - minVal)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                        val v = minVal + p
                        lbl.text = "$labelPrefix: $v$unit"
                        if (fromUser) onValueChange(v)
                    }
                    override fun onStartTrackingTouch(s: SeekBar?) {}
                    override fun onStopTrackingTouch(s: SeekBar?) {
                        savePreferences()
                        auroraPanel.background = IosGlassTheme.createGlassCardDrawable(this@createSettingsPage, 22f)
                    }
                })
            }
            auroraPanel.addView(lbl); auroraPanel.addView(seek)
        }

        addAuroraSlider("שקיפות זכוכית ארקטית (Aurora Opacity)", 80, 245, AuroraBorealisTheme.auroraOpacity, "") { AuroraBorealisTheme.auroraOpacity = it }
        addAuroraSlider("עוצמת טשטוש עומק (Arctic Blur)", 0, 50, AuroraBorealisTheme.auroraBlurRadius.toInt(), "px") { AuroraBorealisTheme.auroraBlurRadius = it.toFloat() }
        addAuroraSlider("עוצמת הארת ניאון ציאן (Luminescence)", 50, 255, AuroraBorealisTheme.auroraGlowIntensity, "") { AuroraBorealisTheme.auroraGlowIntensity = it }
        addAuroraSlider("רדיוס קמירות מקשים (Corner Radius)", 10, 38, AuroraBorealisTheme.auroraCornerPt.toInt(), "pt") { AuroraBorealisTheme.auroraCornerPt = it.toFloat() }

        root.addView(auroraPanel)
    }

    var editingTarget = 0; val targetRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16f); bottomMargin = dp(14f) } }
    lateinit var btnEditPrimary: TextView; lateinit var btnEditBg: TextView; lateinit var pickerContainer: FrameLayout
   
    fun refreshTargetButtons() {
        btnEditPrimary.background = GradientDrawable().apply { setColor(if (editingTarget == 0) primaryColor else Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(14f).toFloat(); setStroke(dp(2f), if (editingTarget == 0) Color.WHITE else Utils.lightenColor(bgColor, 0.15f)) }
        btnEditPrimary.setTextColor(if (editingTarget == 0) Color.parseColor("#08090C") else Color.WHITE)
        btnEditBg.background = GradientDrawable().apply { setColor(if (editingTarget == 1) primaryColor else Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(14f).toFloat(); setStroke(dp(2f), if (editingTarget == 1) Color.WHITE else Utils.lightenColor(bgColor, 0.15f)) }
        btnEditBg.setTextColor(if (editingTarget == 1) Color.parseColor("#08090C") else Color.WHITE)
    }
   
    fun loadColorPicker() {
        refreshTargetButtons(); pickerContainer.removeAllViews()
        pickerContainer.addView(createColorWheelPicker(this, if (editingTarget == 0) primaryColor else bgColor) { newColor ->
            if (editingTarget == 0) primaryColor = newColor else {
                bgColor = newColor; window.statusBarColor = bgColor; window.navigationBarColor = bgColor
                if (isAuroraMode) mainContent?.background = AuroraBorealisTheme.createAuroraAtmosphericBackground()
                else if (isIosGlassMode) mainContent?.background = IosGlassTheme.createLiquidAmbientBackground(this)
                else mainContent?.setBackgroundColor(bgColor)
                drawerMenu?.setBackgroundColor(if (isCleanGlassActive) Color.argb(225, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor)) else Utils.lightenColor(bgColor, 0.06f))
            }
            savePreferences(); refreshTargetButtons()
        })
    }
   
    btnEditPrimary = TextView(this).apply { text = if (isCleanGlassActive) "צבע נושא מותאם" else "🎨 צבע נושא מותאם"; gravity = Gravity.CENTER; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(12f), 0, dp(12f)); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(6f) }; setOnClickListener { feedback(it); editingTarget = 0; loadColorPicker() } }
    btnEditBg = TextView(this).apply { text = if (isCleanGlassActive) "צבע רקע מותאם" else "🌌 צבע רקע מותאם"; gravity = Gravity.CENTER; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(12f), 0, dp(12f)); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(6f) }; setOnClickListener { feedback(it); editingTarget = 1; loadColorPicker() } }
    targetRow.addView(btnEditPrimary); targetRow.addView(btnEditBg); pickerContainer = FrameLayout(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(18f) } }; loadColorPicker()
   
    val titleSettings = TextView(this).apply { text = "הגדרות מערכת נוספות"; setTextColor(Color.WHITE); textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(8f), 0, dp(10f)) }
    val vibBtn = createActionBtn(if (useHaptic) "רטט בלחיצה: מופעל 🟢" else "רטט בלחיצה: כבוי 🔴", false) { useHaptic = !useHaptic; savePreferences(); applyTheme() }
    val sndBtn = createActionBtn(if (useSound) "צלילי הקשה: מופעל 🔊" else "צלילי הקשה: כבוי 🔇", false) { useSound = !useSound; savePreferences(); applyTheme() }
    val soundLabel = if (soundType == 0) "מערכת 📱" else "צליל $soundType 🎵"
    val sndTypeBtn = createActionBtn("סוג צליל: $soundLabel", false) { showGlassSoundPickerDialog { switchTab(19) } }
    val scrBtn = createActionBtn(if (keepScreenOn) "מסך תמיד דולק: מופעל 🔆" else "מסך תמיד דולק: כבוי 🌙", false) { keepScreenOn = !keepScreenOn; updateKeepScreenOn(); savePreferences(); applyTheme() }
    val shapeName = when(buttonCornerStyle) { 8f -> "מרובעים ⏹️"; 24f -> "מודרניים 🔲"; else -> "עגולים ⚪" }
    val shapeBtn = createActionBtn("צורת כפתורים: $shapeName", false) { buttonCornerStyle = when(buttonCornerStyle) { 8f -> 24f; 24f -> 42f; else -> 8f }; savePreferences(); applyTheme() }
    val precBtn = createActionBtn("דיוק עשרוני: $calcPrecision ספרות ⚙️", false) { calcPrecision = if (calcPrecision >= 10) 2 else calcPrecision + 2; savePreferences(); applyTheme() }
   
    root.addView(targetRow); root.addView(pickerContainer); root.addView(titleSettings)
    root.addView(vibBtn); root.addView(sndBtn); root.addView(sndTypeBtn); root.addView(scrBtn); root.addView(shapeBtn); root.addView(precBtn); scroll.addView(root); return scroll
}
 

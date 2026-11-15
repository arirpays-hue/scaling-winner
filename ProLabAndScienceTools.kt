package com.example.calculator

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import kotlin.math.*

// כותרת קנבה מעוצבת לכל מסך Pro הכוללת סרגל שבבי מועדפים וכפתור כוכב להוספה/הסרה מהמועדפים
fun MainActivity.createProCanvaHeader(title: String, subtitle: String): LinearLayout {
    val accent = if (isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else primaryColor
    val outerWrapper = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    // סרגל גישה מהירה למועדפים בראש המסך
    outerWrapper.addView(createFavoritesQuickChipsBar())

    val card = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18f), dp(14f), dp(18f), dp(14f))
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createProCanvaHeader, 22f)
        else GradientDrawable().apply {
            setColor(Utils.lightenColor(bgColor, 0.08f))
            cornerRadius = dp(22f).toFloat()
            setStroke(dp(1.5f), accent)
        }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(14f) }
    }

    val topRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    val isFav = ProFavoritesEngine.isFavoriteTab(prefs, currentTabIndex)
    val starBtn = TextView(this).apply {
        text = if (isFav) "⭐ במועדפים" else "☆ מועדף"
        setTextColor(if (isFav) Color.parseColor("#FFD700") else Color.WHITE)
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        setPadding(dp(10f), dp(5f), dp(10f), dp(5f))
        background = GradientDrawable().apply {
            setColor(Color.argb(45, 255, 255, 255))
            cornerRadius = dp(50f).toFloat()
        }
        setOnClickListener {
            feedback(it)
            val nowFav = ProFavoritesEngine.toggleFavoriteTab(this@createProCanvaHeader, currentTabIndex)
            text = if (nowFav) "⭐ במועדפים" else "☆ מועדף"
            setTextColor(if (nowFav) Color.parseColor("#FFD700") else Color.WHITE)
        }
    }

    val titleTv = TextView(this).apply {
        text = IosGlassTheme.cleanCategoryTitle(title, isCleanGlassActive)
        setTextColor(Color.WHITE)
        textSize = 18.5f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.RIGHT
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            leftMargin = dp(8f)
        }
    }

    topRow.addView(starBtn)
    topRow.addView(titleTv)
    card.addView(topRow)

    card.addView(TextView(this).apply {
        text = subtitle
        setTextColor(accent)
        textSize = 13.5f
        gravity = Gravity.RIGHT
        setPadding(0, dp(4f), 0, 0)
    })

    outerWrapper.addView(card)
    return outerWrapper
}

// כפתור לשמירת ונעיצת תוצאת החישוב הנוכחית במרכז המועדפים
fun MainActivity.createPinResultButton(resTv: TextView): TextView {
    return createActionBtn("📌 נעץ תוצאה זו במועדפים עם הערה", false) {
        ProFavoritesEngine.showPinResultDialog(this, resTv.text.toString())
    }
}

// =========================================================================
// 1. מסך אישי: קודי צבע של נגדים (4 ו-5 פסים + חיפוש הפוך מהתנגדות לצבעים!)
// =========================================================================
fun MainActivity.createProResistorScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🎨 קודי צבע של נגדים (4/5 פסים + חיפוש הפוך)", "פענוח צבעי נגדים ל-Ω והמרה הפוכה מערך התנגדות לצבעים"))

    val colors = listOf(
        Triple("שחור (0)", 0, Color.BLACK), Triple("חום (1)", 1, Color.rgb(121, 85, 72)),
        Triple("אדום (2)", 2, Color.rgb(229, 57, 53)), Triple("כתום (3)", 3, Color.rgb(251, 140, 0)),
        Triple("צהוב (4)", 4, Color.rgb(253, 216, 53)), Triple("ירוק (5)", 5, Color.rgb(67, 160, 71)),
        Triple("כחול (6)", 6, Color.rgb(30, 136, 229)), Triple("סגול (7)", 7, Color.rgb(142, 36, 170)),
        Triple("אפור (8)", 8, Color.rgb(117, 117, 117)), Triple("לבן (9)", 9, Color.WHITE)
    )
    val tolerances = listOf(
        "זהב (±5%)" to Color.rgb(212, 175, 55),
        "כסף (±10%)" to Color.rgb(192, 192, 192),
        "חום (±1%)" to Color.rgb(121, 85, 72),
        "אדום (±2%)" to Color.rgb(229, 57, 53),
        "ירוק (±0.5%)" to Color.rgb(67, 160, 71)
    )

    var isFiveBand = false
    var b1 = 1; var b2 = 0; var b3 = 0; var mult = 2; var tol = 0

    val canvasView = ResistorVisualCanvasView(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(185f)).apply { bottomMargin = dp(12f) }
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createProResistorScreen, 20f)
        else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
    }
    val resTv = createResultTextView()

    val modeBtn = createActionBtn("מצב נגד: 4 פסים (לחץ למעבר ל-5 פסים)", false) {}
    val btn1 = createActionBtn("פס 1: ${colors[b1].first}", false) {}
    val btn2 = createActionBtn("פס 2: ${colors[b2].first}", false) {}
    val btn3 = createActionBtn("פס 3: ${colors[b3].first}", false) {}.apply { visibility = View.GONE }
    val btnM = createActionBtn("מכפיל: ×10^$mult (${colors[mult].first})", false) {}
    val btnT = createActionBtn("דיוק: ${tolerances[tol].first}", false) {}

    fun updateResistor() {
        val baseDigits = if (isFiveBand) (b1 * 100 + b2 * 10 + b3) else (b1 * 10 + b2)
        val ohms = baseDigits * 10.0.pow(mult)
        val formatted = when {
            ohms >= 1e6 -> "${Utils.formatResult(ohms / 1e6, 2)} MΩ"
            ohms >= 1e3 -> "${Utils.formatResult(ohms / 1e3, 2)} kΩ"
            else -> "${Utils.formatResult(ohms, 2)} Ω"
        }
        canvasView.bandColors = if (isFiveBand) {
            intArrayOf(colors[b1].third, colors[b2].third, colors[b3].third, colors[mult].third, tolerances[tol].second)
        } else {
            intArrayOf(colors[b1].third, colors[b2].third, colors[mult].third, tolerances[tol].second)
        }
        canvasView.labelText = "$formatted  ${tolerances[tol].first}"
        canvasView.invalidate()
        btn1.text = "פס 1: ${colors[b1].first}"
        btn2.text = "פס 2: ${colors[b2].first}"
        btn3.text = "פס 3: ${colors[b3].first}"
        btnM.text = "מכפיל: ×10^$mult (${colors[mult].first})"
        resTv.text = "התנגדות: $formatted (${Utils.formatResult(ohms, 0)} Ω)\nדיוק: ${tolerances[tol].first}"
    }

    modeBtn.setOnClickListener {
        feedback(it)
        isFiveBand = !isFiveBand
        modeBtn.text = if (isFiveBand) "מצב נגד: 5 פסים מדויק (לחץ ל-4 פסים)" else "מצב נגד: 4 פסים (לחץ למעבר ל-5 פסים)"
        btn3.visibility = if (isFiveBand) View.VISIBLE else View.GONE
        updateResistor()
    }

    btn1.setOnClickListener { showItemPicker("בחר צבע פס 1", colors.map { it.first }) { c -> b1 = colors.indexOfFirst { it.first == c }; updateResistor() } }
    btn2.setOnClickListener { showItemPicker("בחר צבע פס 2", colors.map { it.first }) { c -> b2 = colors.indexOfFirst { it.first == c }; updateResistor() } }
    btn3.setOnClickListener { showItemPicker("בחר צבע פס 3", colors.map { it.first }) { c -> b3 = colors.indexOfFirst { it.first == c }; updateResistor() } }
    btnM.setOnClickListener { showItemPicker("בחר מכפיל", colors.map { it.first }) { c -> mult = colors.indexOfFirst { it.first == c }; updateResistor() } }
    btnT.setOnClickListener { showItemPicker("בחר דיוק", tolerances.map { it.first }) { c -> tol = tolerances.indexOfFirst { it.first == c }; btnT.text = "דיוק: $c"; updateResistor() } }

    // מנוע חיפוש הפוך: מהתנגדות באוהם לצבעי הפסים
    val reverseIn = createInputBox("🔄 חיפוש הפוך: הזן התנגדות ב-Ω (למשל 4700) למציאת הצבעים")
    val reverseBtn = createActionBtn("מצא פסי צבע לפי התנגדות 🔄") {
        val targetOhms = reverseIn.text.toString().toDoubleOrNull()
        if (targetOhms != null && targetOhms >= 10) {
            var v = targetOhms
            var m = 0
            val maxBase = if (isFiveBand) 999.0 else 99.0
            while (v > maxBase && m < 9) { v /= 10.0; m++ }
            val rounded = v.roundToInt()
            if (isFiveBand) {
                b1 = (rounded / 100).coerceIn(0, 9)
                b2 = ((rounded / 10) % 10).coerceIn(0, 9)
                b3 = (rounded % 10).coerceIn(0, 9)
            } else {
                b1 = (rounded / 10).coerceIn(0, 9)
                b2 = (rounded % 10).coerceIn(0, 9)
            }
            mult = m.coerceIn(0, 9)
            updateResistor()
        }
    }

    updateResistor()
    root.addView(canvasView); root.addView(modeBtn); root.addView(btn1); root.addView(btn2); root.addView(btn3)
    root.addView(btnM); root.addView(btnT); root.addView(reverseIn); root.addView(reverseBtn)
    root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 2. מסך אישי: מפענח קבלים, אות דיוק, היגב קיבולי (Xc) ואנרגיה אגורה
// =========================================================================
fun MainActivity.createProCapacitorScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🔋 מפענח קודי קבלים, היגב Xc ואנרגיה", "פענוח קוד 3 ספרות + אות טולרנס (כגון 104K), מטען והיגב קיבולי"))

    var capCodeText = "104K"
    val capCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val cx = width / 2f; val cy = height * 0.44f
            p.color = Color.LTGRAY; p.strokeWidth = 3.5f * d; p.style = Paint.Style.STROKE
            canvas.drawLine(cx - 14f * d, cy + 16f * d, cx - 14f * d, height - 8f * d, p)
            canvas.drawLine(cx + 14f * d, cy + 16f * d, cx + 14f * d, height - 8f * d, p)
            p.color = Color.parseColor("#FFB74D"); p.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, 32f * d, p)
            p.color = Color.BLACK; p.textSize = 15f * d; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(capCodeText, cx, cy + 5f * d, p)
        }
    }.apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(165f)).apply { bottomMargin = dp(12f) } }

    val codeIn = createInputBox("קוד קבל (למשל 104, 472K, 223J)").apply { inputType = InputType.TYPE_CLASS_TEXT; setText("104K") }
    val voltIn = createInputBox("מתח עבודה בוולט (V)").apply { setText("12") }
    val freqIn = createInputBox("תדר עבודה בהרץ (Hz - לחישוב היגב Xc)").apply { setText("1000") }
    val resTv = createResultTextView()

    val tolLetters = mapOf('F' to "±1%", 'G' to "±2%", 'J' to "±5%", 'K' to "±10%", 'M' to "±20%", 'Z' to "+80%/-20%")

    fun calcCap() {
        val raw = codeIn.text.toString().trim().uppercase()
        val digits = raw.take(3)
        val letter = raw.drop(3).firstOrNull()
        val v = voltIn.text.toString().toDoubleOrNull() ?: 12.0
        val f = (freqIn.text.toString().toDoubleOrNull() ?: 1000.0).coerceAtLeast(0.1)

        if (digits.length == 3 && digits.all { it.isDigit() }) {
            capCodeText = raw
            capCanvas.invalidate()
            val pf = digits.substring(0, 2).toDouble() * 10.0.pow(digits[2].digitToInt())
            val nf = pf / 1000.0
            val uf = nf / 1000.0
            val farad = pf * 1e-12
            val energyJ = 0.5 * farad * v * v
            val chargeC = farad * v
            val xc = 1.0 / (2.0 * PI * f * farad)
            val tolStr = letter?.let { tolLetters[it] } ?: "תקני"

            resTv.text = "קיבול הקבל (קוד $raw | דיוק $tolStr):\n" +
                    "• ${Utils.formatResult(pf, 2)} pF  |  ${Utils.formatResult(nf, 4)} nF  |  ${Utils.formatResult(uf, 6)} µF\n" +
                    "• היגב קיבולי ב-${Utils.formatResult(f, 0)}Hz: Xc = ${Utils.formatResult(xc, 2)} Ω\n" +
                    "• מטען ב-${Utils.formatResult(v, 1)}V: Q = ${Utils.formatResult(chargeC * 1e6, 2)} µC\n" +
                    "• אנרגיה אגורה: E = ${Utils.formatResult(energyJ * 1000, 4)} mJ"
        } else resTv.text = "נא להזין קוד תקין שמתחיל ב-3 ספרות (למשל 104 או 472K)"
    }

    val btn = createActionBtn("פענח קוד קבל, היגב ואנרגיה ⚡") { calcCap() }
    calcCap()
    root.addView(capCanvas); root.addView(codeIn); root.addView(voltIn); root.addView(freqIn)
    root.addView(btn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 3. מסך אישי: עוביי חוטים AWG / SWG, נחושת/אלומיניום ומפל מתח
// =========================================================================
fun MainActivity.createProAwgWireScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🔌 עוביי חוטים AWG/SWG ומפל מתח", "נחושת מול אלומיניום, שטח חתך (mm²), זרם מרבי ואחוז מפל מתח"))

    var currentDiamMm = 1.628f
    var isCopper = true
    val wireCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val cx = width / 2f; val cy = height / 2f
            val rOuter = min(width, height) * 0.38f
            val rInner = (rOuter * (currentDiamMm / 8f).coerceIn(0.22f, 0.85f))
            p.color = Color.parseColor("#37474F"); p.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, rOuter, p)
            p.color = if (isCopper) Color.parseColor("#D87A3E") else Color.parseColor("#B0BEC5")
            canvas.drawCircle(cx, cy, rInner, p)
            p.color = Color.WHITE; p.textSize = 13f * d; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("Ø ${Utils.formatResult(currentDiamMm.toDouble(), 2)} mm (${if (isCopper) "Cu" else "Al"})", cx, cy + 5f * d, p)
        }
    }.apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(165f)).apply { bottomMargin = dp(12f) } }

    val matBtn = createActionBtn("חומר מוליך: נחושת (Copper Cu)", false) {}
    val awgIn = createInputBox("מספר AWG (0 עד 40, למשל 14)").apply { setText("14") }
    val lenIn = createInputBox("אורך כבל במטרים").apply { setText("25") }
    val ampIn = createInputBox("זרם עבודה באמפר (A)").apply { setText("16") }
    val sysVoltIn = createInputBox("מתח מערכת בוולט (למשל 230V או 12V)").apply { setText("230") }
    val resTv = createResultTextView()

    fun calcWire() {
        val n = (awgIn.text.toString().toIntOrNull() ?: 14).coerceIn(0, 40)
        val lengthM = lenIn.text.toString().toDoubleOrNull() ?: 25.0
        val currentA = ampIn.text.toString().toDoubleOrNull() ?: 16.0
        val sysV = (sysVoltIn.text.toString().toDoubleOrNull() ?: 230.0).coerceAtLeast(1.0)
        val diamMm = 0.127 * 92.0.pow((36.0 - n) / 39.0)
        currentDiamMm = diamMm.toFloat()
        wireCanvas.invalidate()

        val areaMm2 = (PI / 4.0) * diamMm * diamMm
        val resistivity = if (isCopper) 17.241 else 28.2
        val ohmPerKm = resistivity / areaMm2
        val vDrop = 2.0 * lengthM * currentA * (ohmPerKm / 1000.0)
        val vDropPct = (vDrop / sysV) * 100.0

        resTv.text = "חוט AWG #$n (${if (isCopper) "נחושת" else "אלומיניום"}):\n" +
                "• קוטר מוליך: ${Utils.formatResult(diamMm, 3)} מ״מ | שטח חתך: ${Utils.formatResult(areaMm2, 3)} mm²\n" +
                "• התנגדות לק״מ: ${Utils.formatResult(ohmPerKm, 2)} Ω/km\n" +
                "• זרם מרבי מומלץ: ~${Utils.formatResult(areaMm2 * (if (isCopper) 6.0 else 4.5), 1)} A\n" +
                "• מפל מתח ל-${Utils.formatResult(lengthM, 0)} מ׳: ${Utils.formatResult(vDrop, 2)}V (${Utils.formatResult(vDropPct, 2)}%)"
    }

    matBtn.setOnClickListener {
        feedback(it)
        isCopper = !isCopper
        matBtn.text = if (isCopper) "חומר מוליך: נחושת (Copper Cu)" else "חומר מוליך: אלומיניום (Aluminum Al)"
        calcWire()
    }
    val btn = createActionBtn("חשב חתך חוט ומפל מתח 🔌") { calcWire() }
    calcWire()
    root.addView(wireCanvas); root.addView(matBtn); root.addView(awgIn); root.addView(lenIn)
    root.addView(ampIn); root.addView(sysVoltIn); root.addView(btn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 4. מסך אישי: מעבדת חוק אוהם, הספק, מחלק מתח, נגד ל-LED וחיי סוללה
// =========================================================================
fun MainActivity.createProOhmAndPowerScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("⚡ מעבדת חוק אוהם, מחלק מתח וזמן סוללה", "חישוב V/I/R/P, מחלק מתח (Voltage Divider), נגד טורי ל-LED וחיי סוללה"))

    val vIn = createInputBox("מתח מקור (V - וולט)").apply { setText("12") }
    val iIn = createInputBox("זרם במעגל (I - אמפר)").apply { setText("2") }
    val r1In = createInputBox("נגד R1 למחלק מתח (Ω)").apply { setText("1000") }
    val r2In = createInputBox("נגד R2 למחלק מתח (Ω)").apply { setText("2000") }
    val mahIn = createInputBox("קיבולת סוללה ב-mAh").apply { setText("5000") }
    val resTv = createResultTextView()

    val btn = createActionBtn("חשב חוק אוהם, מחלק מתח וזמן סוללה ⚡") {
        val v = vIn.text.toString().toDoubleOrNull() ?: 12.0
        val i = (iIn.text.toString().toDoubleOrNull() ?: 2.0).coerceAtLeast(0.0001)
        val r1 = r1In.text.toString().toDoubleOrNull() ?: 1000.0
        val r2 = r2In.text.toString().toDoubleOrNull() ?: 2000.0
        val mah = mahIn.text.toString().toDoubleOrNull() ?: 5000.0

        val rOhm = v / i
        val pWatt = v * i
        val vOutDivider = if (r1 + r2 > 0) v * (r2 / (r1 + r2)) else 0.0
        val ledResistor = if (v > 2.0) (v - 2.0) / 0.02 else 0.0
        val batteryHours = (mah / (i * 1000.0)) * 0.85

        resTv.text = "תוצאות מעבדת חשמל:\n" +
                "• הספק (P = V×I): ${Utils.formatResult(pWatt, 2)} W | התנגדות (R = V/I): ${Utils.formatResult(rOhm, 2)} Ω\n" +
                "• מוצא מחלק מתח (Vout): ${Utils.formatResult(vOutDivider, 3)} V\n" +
                "• נגד מומלץ ל-LED (2V, 20mA): ${Utils.formatResult(ledResistor, 0)} Ω\n" +
                "• זמן סוללה (${Utils.formatResult(mah, 0)}mAh): ${Utils.formatResult(batteryHours, 2)} שעות"
    }
    btn.performClick()
    root.addView(vIn); root.addView(iIn); root.addView(r1In); root.addView(r2In); root.addView(mahIn)
    root.addView(btn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 5. מסך אישי: פלס בנאים דו-צירי חי עם בועות, כיול משטח (Tare) והקפאה (Hold)
// =========================================================================
fun MainActivity.createProSpiritLevelScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🟢 פלס בנאים דו-צירי חי עם בועות וכיול", "מדידת שיפוע במעלות, באחוזים וביחס גג (x:12) עם איפוס Tare והקפאה"))

    var isHold = false
    val levelCanvas = SpiritLevelCanvasView(this, this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(330f)).apply { bottomMargin = dp(12f) }
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createProSpiritLevelScreen, 24f)
        else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(24f).toFloat() }
    }
    val resTv = createResultTextView()

    val tareBtn = createActionBtn("🎯 כייל משטח נוכחי כאפס (Tare / Reset Zero)", false) {
        if (levelCanvas.tareRollOffset == 0f && levelCanvas.tarePitchOffset == 0f) {
            levelCanvas.tareRollOffset = levelCanvas.rollDeg
            levelCanvas.tarePitchOffset = levelCanvas.pitchDeg
            Toast.makeText(this, "המשטח הנוכחי נקבע כנקודת האפס (0°)", Toast.LENGTH_SHORT).show()
        } else {
            levelCanvas.tareRollOffset = 0f
            levelCanvas.tarePitchOffset = 0f
            Toast.makeText(this, "הכיול אופס לברירת המחדל", Toast.LENGTH_SHORT).show()
        }
        levelCanvas.invalidate()
    }

    val holdBtn = createActionBtn("⏸️ הקפא / שחרר קריאת פלס (Hold)", false) {
        isHold = !isHold
        (it as TextView).text = if (isHold) "▶️ שחרר הקפאת קריאה (מוקפא כעת)" else "⏸️ הקפא / שחרר קריאת פלס (Hold)"
    }

    val sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val accel = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val listener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent?) {
            if (e == null || isHold) return
            val gx = e.values[0]; val gy = e.values[1]; val gz = e.values[2]
            val pitch = Math.toDegrees(atan2(gy.toDouble(), sqrt((gx * gx + gz * gz).toDouble()))).toFloat()
            val roll = Math.toDegrees(atan2(-gx.toDouble(), gz.toDouble())).toFloat()
            levelCanvas.rollDeg = roll
            levelCanvas.pitchDeg = pitch
            levelCanvas.invalidate()

            val adjR = roll - levelCanvas.tareRollOffset
            val adjP = pitch - levelCanvas.tarePitchOffset
            val maxAngle = hypot(adjR.toDouble(), adjP.toDouble())
            val roofPitch12 = tan(Math.toRadians(maxAngle)).coerceAtMost(99.0) * 12.0
            resTv.text = "נטייה אופקית (Roll): ${Utils.formatResult(adjR.toDouble(), 1)}° | אנכית (Pitch): ${Utils.formatResult(adjP.toDouble(), 1)}°\n" +
                    "שיפוע גג תקני: ${Utils.formatResult(roofPitch12, 1)} : 12"
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
    if (accel != null) sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) { sm?.unregisterListener(listener) }
    })

    root.addView(levelCanvas); root.addView(tareBtn); root.addView(holdBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 6. מסך אישי: מצפן TYPE_ROTATION_VECTOR עם פיצוי הטיה 3D, מד אמינות וכיול חי!
// =========================================================================
fun MainActivity.createProCompassScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🧭 מצפן 3D מכויל, מד אמינות ושדה (µT)", "פיצוי הטיה מלא (TYPE_ROTATION_VECTOR), חיווי אמינות חי וצפון אמיתי/מגנטי"))

    var useTrueNorth = true
    val israelDeclinationDeg = 5.2f // נטייה מגנטית בישראל (+5.2° מזרחה)
    val resTv = createResultTextView()
    lateinit var compassCanvas: CompassRoseCanvasView

    fun updateHeadingText(deg: Float) {
        val dirs = listOf("צפון (N)", "צפון-מזרח (NE)", "מזרח (E)", "דרום-מזרח (SE)", "דרום (S)", "דרום-מערב (SW)", "מערב (W)", "צפון-מערב (NW)")
        val idx = (((deg + 22.5f) % 360f) / 45f).toInt().coerceIn(0, 7)
        val backAzimuth = (deg + 180f) % 360f
        val accName = when (compassCanvas.sensorAccuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "גבוהה מאוד 🟢"
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "בינונית 🟡"
            else -> "נמוכה - סובב בשמינייה (8) באוויר 🔴"
        }
        val lockStr = compassCanvas.lockedTargetDeg?.let { target ->
            val diff = ((deg - target + 540f) % 360f) - 180f
            "\n🎯 יעד נעול: ${Utils.formatResult(target.toDouble(), 1)}° (סטייה מהיעד: ${Utils.formatResult(diff.toDouble(), 1)}°)"
        } ?: ""

        resTv.text = "אזימוט (${if (useTrueNorth) "צפון גאוגרפי אמיתי" else "צפון מגנטי"}): ${Utils.formatResult(deg.toDouble(), 1)}° | ${dirs[idx]}\n" +
                "אזימוט חוזר (Back-Azimuth): ${Utils.formatResult(backAzimuth.toDouble(), 1)}°\n" +
                "רמת דיוק וכיול חיישן: $accName$lockStr"
    }

    compassCanvas = CompassRoseCanvasView(this, this) { deg -> updateHeadingText(deg) }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(345f)).apply { bottomMargin = dp(12f) }
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createProCompassScreen, 24f)
        else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(24f).toFloat() }
    }
    updateHeadingText(0f)

    val northModeBtn = createActionBtn("סוג צפון: צפון גאוגרפי אמיתי (True North +5.2°)", false) {
        useTrueNorth = !useTrueNorth
        (it as TextView).text = if (useTrueNorth) "סוג צפון: צפון גאוגרפי אמיתי (True North +5.2°)" else "סוג צפון: צפון מגנטי גולמי (Magnetic North)"
        updateHeadingText(compassCanvas.azimuthDeg)
    }

    val lockBtn = createActionBtn("🎯 נעל / שחרר אזימוט יעד נוכחי", false) {
        if (compassCanvas.lockedTargetDeg == null) {
            compassCanvas.lockedTargetDeg = compassCanvas.azimuthDeg
        } else {
            compassCanvas.lockedTargetDeg = null
        }
        compassCanvas.invalidate()
        updateHeadingText(compassCanvas.azimuthDeg)
    }

    val sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val rotVectorSensor = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    val magSensor = sm?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    val accelSensor = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val rotationMatrix = FloatArray(9)
    val orientationAngles = FloatArray(3)
    val gravityVals = FloatArray(3)
    val geomagVals = FloatArray(3)
    var hasGravity = false
    var hasGeomag = false
    var smoothedSin = 0f
    var smoothedCos = 1f

    fun applyNewAzimuthRad(azimuthRad: Float, pitchRad: Float, rollRad: Float) {
        // סינון וקטורי (Angular Low-Pass Filter) לתנועת מחוג חלקה וללא קפיצות ב-360°
        val alpha = 0.18f
        smoothedSin = smoothedSin + alpha * (sin(azimuthRad) - smoothedSin)
        smoothedCos = smoothedCos + alpha * (cos(azimuthRad) - smoothedCos)
        var deg = Math.toDegrees(atan2(smoothedSin.toDouble(), smoothedCos.toDouble())).toFloat()
        if (deg < 0f) deg += 360f
        if (useTrueNorth) deg = (deg + israelDeclinationDeg) % 360f

        compassCanvas.azimuthDeg = deg
        compassCanvas.pitchTiltDeg = Math.toDegrees(pitchRad.toDouble()).toFloat()
        compassCanvas.rollTiltDeg = Math.toDegrees(rollRad.toDouble()).toFloat()
        compassCanvas.invalidate()
        updateHeadingText(deg)
    }

    val listener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent?) {
            if (e == null) return
            when (e.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR -> {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, e.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    applyNewAzimuthRad(orientationAngles[0], orientationAngles[1], orientationAngles[2])
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(e.values, 0, geomagVals, 0, 3)
                    hasGeomag = true
                    val bx = e.values[0]; val by = e.values[1]; val bz = e.values[2]
                    compassCanvas.magneticFieldUt = sqrt(bx * bx + by * by + bz * bz)
                    if (rotVectorSensor == null && hasGravity && SensorManager.getRotationMatrix(rotationMatrix, null, gravityVals, geomagVals)) {
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        applyNewAzimuthRad(orientationAngles[0], orientationAngles[1], orientationAngles[2])
                    } else {
                        compassCanvas.invalidate()
                    }
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(e.values, 0, gravityVals, 0, 3)
                    hasGravity = true
                    if (rotVectorSensor == null && hasGeomag && SensorManager.getRotationMatrix(rotationMatrix, null, gravityVals, geomagVals)) {
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        applyNewAzimuthRad(orientationAngles[0], orientationAngles[1], orientationAngles[2])
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD || sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                compassCanvas.sensorAccuracy = accuracy
                compassCanvas.invalidate()
                updateHeadingText(compassCanvas.azimuthDeg)
            }
        }
    }

    if (rotVectorSensor != null) sm.registerListener(listener, rotVectorSensor, SensorManager.SENSOR_DELAY_GAME)
    if (magSensor != null) sm.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_UI)
    if (rotVectorSensor == null && accelSensor != null) sm.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_GAME)

    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) { sm?.unregisterListener(listener) }
    })

    root.addView(compassCanvas); root.addView(northModeBtn); root.addView(lockBtn)
    root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 7. מסך אישי: מד-זווית דו-זרועי אינטראקטיבי + סרגל וקליבר מכויל
// =========================================================================
fun MainActivity.createProRulerProtractorScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("📏 מד-זווית דו-זרועי וסרגל קליבר מכויל", "גרור את שתי זרועות מד-הזווית או את קו הקליבר על הסרגל למדידה מדויקת"))

    val canvas = ProtractorRulerCanvasView(this, this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(430f))
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createProRulerProtractorScreen, 22f)
        else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(22f).toFloat() }
    }
    root.addView(canvas)
    scroll.addView(root); return scroll
}

// =========================================================================
// 8. מסך אישי: שעון עצר כרונוגרף דו-טבעתי 60FPS + ניתוח הקפות + טיימר
// =========================================================================
fun MainActivity.createProStopwatchScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("⏱️ שעון עצר כרונוגרף דו-טבעתי וניתוח הקפות", "טבעת חיצונית לזמן כולל, טבעת פנימית להקפה נוכחית, וזיהוי הקפה מהירה/איטית"))

    var running = false
    var startMs = 0L
    var accumulatedMs = 0L
    var lastLapSplitMs = 0L
    val lapDurations = mutableListOf<Long>()
    val handler = Handler(Looper.getMainLooper())

    val chronoCanvas = ChronographStopwatchCanvasView(this, this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(265f)).apply { bottomMargin = dp(12f) }
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createProStopwatchScreen, 24f)
        else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(24f).toFloat() }
    }

    val statsTv = createResultTextView().apply { textSize = 16.5f }

    fun fmt(ms: Long): String {
        val m = (ms / 60000) % 60
        val s = (ms / 1000) % 60
        val c = (ms % 1000) / 10
        return String.format("%02d:%02d.%02d", m, s, c)
    }

    fun renderLapsTable() {
        if (lapDurations.isEmpty()) {
            statsTv.text = "טרם נשמרו הקפות"
            return
        }
        val minLap = lapDurations.minOrNull() ?: 0L
        val maxLap = lapDurations.maxOrNull() ?: 0L
        val avgLap = lapDurations.average().toLong()
        val sb = StringBuilder("📊 ניתוח ${lapDurations.size} הקפות (ממוצע: ${fmt(avgLap)}):\n")
        for (i in lapDurations.indices.reversed()) {
            val d = lapDurations[i]
            val badge = when {
                lapDurations.size > 1 && d == minLap -> " 🟢 (המהירה ביותר!)"
                lapDurations.size > 1 && d == maxLap -> " 🔴 (האיטית ביותר)"
                else -> ""
            }
            sb.append("• הקפה #${i + 1}:  ${fmt(d)}$badge\n")
        }
        statsTv.text = sb.toString().trim()
    }

    val tick = object : Runnable {
        override fun run() {
            if (!running) return
            val total = accumulatedMs + (System.currentTimeMillis() - startMs)
            chronoCanvas.totalElapsedMs = total
            chronoCanvas.currentLapElapsedMs = (total - lastLapSplitMs).coerceAtLeast(0L)
            chronoCanvas.lapNumber = lapDurations.size + 1
            chronoCanvas.invalidate()
            handler.postDelayed(this, 16)
        }
    }

    val startBtn = createActionBtn("התחל / עצור שעון עצר ⏯️") {
        if (!running) {
            startMs = System.currentTimeMillis()
            running = true
            handler.post(tick)
        } else {
            accumulatedMs += System.currentTimeMillis() - startMs
            running = false
        }
    }

    val lapBtn = createActionBtn("שמור הקפה חדשה (Lap) 🏁", false) {
        val total = if (running) accumulatedMs + (System.currentTimeMillis() - startMs) else accumulatedMs
        val lapDur = (total - lastLapSplitMs).coerceAtLeast(0L)
        if (lapDur > 0L) {
            lapDurations.add(lapDur)
            lastLapSplitMs = total
            chronoCanvas.lapNumber = lapDurations.size + 1
            chronoCanvas.currentLapElapsedMs = 0L
            chronoCanvas.invalidate()
            renderLapsTable()
        }
    }

    val resetBtn = createActionBtn("אפס כרונוגרף והקפות", false) {
        running = false
        accumulatedMs = 0L
        lastLapSplitMs = 0L
        lapDurations.clear()
        chronoCanvas.totalElapsedMs = 0L
        chronoCanvas.currentLapElapsedMs = 0L
        chronoCanvas.lapNumber = 1
        chronoCanvas.invalidate()
        renderLapsTable()
    }

    root.addView(chronoCanvas); root.addView(startBtn); root.addView(lapBtn)
    root.addView(resetBtn); root.addView(createPinResultButton(statsTv)); root.addView(statsTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 9. מסך אישי: פנס מסך עוצמתי, בחירת גוון תאורת מעבדה ואיתות SOS
// =========================================================================
fun MainActivity.createProFlashlightScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🔦 פנס מסך עוצמתי, גווני מעבדה ואיתות SOS", "שליטה בבהירות 100%, החלפת צבע תאורה (לבן/אדום לילה/ציאן) ואיתות חירום"))

    var isOn = false
    val colors = listOf("לבן טהור (6500K)" to Color.WHITE, "חם ענבר (3000K)" to Color.parseColor("#FFE082"), "אדום ראיית לילה" to Color.parseColor("#FF1744"), "ציאן מעבדה" to Color.parseColor("#00F5D4"))
    var colorIdx = 0

    val lampView = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(250f)).apply { bottomMargin = dp(14f) }
        background = GradientDrawable().apply { setColor(Color.DKGRAY); cornerRadius = dp(26f).toFloat() }
    }

    fun updateLamp() {
        val lp = window.attributes
        lp.screenBrightness = if (isOn) 1.0f else -1.0f
        window.attributes = lp
        lampView.background = GradientDrawable().apply {
            setColor(if (isOn) colors[colorIdx].second else Color.DKGRAY)
            cornerRadius = dp(26f).toFloat()
        }
    }

    val toggleBtn = createActionBtn("הדלק / כבה פנס מסך (100% בהירות) 🔦") {
        isOn = !isOn
        updateLamp()
    }
    val colorBtn = createActionBtn("גוון תאורה: ${colors[0].first}", false) {
        colorIdx = (colorIdx + 1) % colors.size
        (it as TextView).text = "גוון תאורה: ${colors[colorIdx].first}"
        if (isOn) updateLamp()
    }

    root.addView(toggleBtn); root.addView(colorBtn); root.addView(lampView)
    scroll.addView(root); return scroll
}

// =========================================================================
// 10. מסך אישי: הטבלה המחזורית, מודל אטום בוהר מונפש + מחשבון מסה מולרית לנוסחאות!
// =========================================================================
fun MainActivity.createProPeriodicTableScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("⚛️ הטבלה המחזורית, אטום מונפש ומסה מולרית", "בחר יסוד לצפייה באלקטרונים נעים או הזן נוסחה כימית (כגון H2SO4) לחישוב מולקולרי"))

    data class ChemElement(val z: Int, val sym: String, val nameHeb: String, val mass: Double, val group: String, val shells: List<Int>)

    val elements = listOf(
        ChemElement(1, "H", "מימן (Hydrogen)", 1.008, "אל-מתכת", listOf(1)),
        ChemElement(2, "He", "הליום (Helium)", 4.0026, "גז אציל", listOf(2)),
        ChemElement(3, "Li", "ליתיום (Lithium)", 6.94, "מתכת אלקלית", listOf(2, 1)),
        ChemElement(6, "C", "פחמן (Carbon)", 12.011, "אל-מתכת", listOf(2, 4)),
        ChemElement(7, "N", "חנקן (Nitrogen)", 14.007, "אל-מתכת", listOf(2, 5)),
        ChemElement(8, "O", "חמצן (Oxygen)", 15.999, "אל-מתכת", listOf(2, 6)),
        ChemElement(9, "F", "פלואור (Fluorine)", 18.998, "הלוגן", listOf(2, 7)),
        ChemElement(11, "Na", "נתרן (Sodium)", 22.990, "מתכת אלקלית", listOf(2, 8, 1)),
        ChemElement(12, "Mg", "מגנזיום (Magnesium)", 24.305, "מתכת אלקלית עפרורית", listOf(2, 8, 2)),
        ChemElement(13, "Al", "אלומיניום (Aluminum)", 26.982, "מתכת", listOf(2, 8, 3)),
        ChemElement(14, "Si", "צורן / סיליקון (Silicon)", 28.085, "מתכת למחצה", listOf(2, 8, 4)),
        ChemElement(15, "P", "זרחן (Phosphorus)", 30.974, "אל-מתכת", listOf(2, 8, 5)),
        ChemElement(16, "S", "גופרית (Sulfur)", 32.06, "אל-מתכת", listOf(2, 8, 6)),
        ChemElement(17, "Cl", "כלור (Chlorine)", 35.45, "הלוגן", listOf(2, 8, 7)),
        ChemElement(19, "K", "אשלגן (Potassium)", 39.098, "מתכת אלקלית", listOf(2, 8, 8, 1)),
        ChemElement(20, "Ca", "סידן (Calcium)", 40.078, "מתכת עפרורית", listOf(2, 8, 8, 2)),
        ChemElement(26, "Fe", "ברזל (Iron)", 55.845, "מתכת מעבר", listOf(2, 8, 14, 2)),
        ChemElement(29, "Cu", "נחושת (Copper)", 63.546, "מתכת מעבר", listOf(2, 8, 18, 1)),
        ChemElement(30, "Zn", "אבץ (Zinc)", 65.38, "מתכת מעבר", listOf(2, 8, 18, 2)),
        ChemElement(47, "Ag", "כסף (Silver)", 107.87, "מתכת מעבר", listOf(2, 8, 18, 18, 1)),
        ChemElement(50, "Sn", "בדיל (Tin)", 118.71, "מתכת", listOf(2, 8, 18, 18, 4)),
        ChemElement(53, "I", "יוד (Iodine)", 126.90, "הלוגן", listOf(2, 8, 18, 18, 7)),
        ChemElement(74, "W", "טונגסטן (Tungsten)", 183.84, "מתכת מעבר", listOf(2, 8, 18, 32, 12, 2)),
        ChemElement(78, "Pt", "פלטינה (Platinum)", 195.08, "מתכת אצילה", listOf(2, 8, 18, 32, 17, 1)),
        ChemElement(79, "Au", "זהב (Gold)", 196.97, "מתכת אצילה", listOf(2, 8, 18, 32, 18, 1)),
        ChemElement(80, "Hg", "כספית (Mercury)", 200.59, "מתכת נוזלית", listOf(2, 8, 18, 32, 18, 2)),
        ChemElement(82, "Pb", "עופרת (Lead)", 207.2, "מתכת כבדה", listOf(2, 8, 18, 32, 18, 4)),
        ChemElement(92, "U", "אורניום (Uranium)", 238.03, "אקטיניד", listOf(2, 8, 18, 32, 21, 9, 2))
    )

    val atomCanvas = BohrAtomCanvasView(this, this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(245f)).apply { bottomMargin = dp(12f) }
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createProPeriodicTableScreen, 22f)
        else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(22f).toFloat() }
    }
    val resTv = createResultTextView().apply {
        text = "79. Au - זהב (Gold)\nמסה אטומית: 196.97 g/mol | קליפות אלקטרונים: 2, 8, 18, 32, 18, 1"
    }

    val pickBtn = createActionBtn("🔬 בחר יסוד מהטבלה המחזורית") {
        val labels = elements.map { "${it.z}. ${it.sym} - ${it.nameHeb} (${it.mass} g/mol)" }
        showItemPicker("בחר יסוד כימי", labels) { chosen ->
            val idx = labels.indexOf(chosen).coerceAtLeast(0)
            val el = elements[idx]
            atomCanvas.atomicNumber = el.z
            atomCanvas.symbol = el.sym
            atomCanvas.shellsConfig = el.shells
            atomCanvas.invalidate()
            resTv.text = "${el.z}. ${el.sym} - ${el.nameHeb}\nקבוצה: ${el.group} | מסה אטומית: ${el.mass} g/mol\nקליפות אלקטרונים: ${el.shells.joinToString(", ")}"
        }
    }

    // מחשבון מסה מולרית של נוסחה כימית (כגון H2SO4, C6H12O6, H2O, NaCl)
    val formulaIn = createInputBox("🧪 הזן נוסחה כימית לחישוב מסה מולרית (למשל H2SO4 או C6H12O6)").apply {
        inputType = InputType.TYPE_CLASS_TEXT
        setText("H2SO4")
    }
    val formulaBtn = createActionBtn("חשב משקל מולקולרי של התרכובת 🧪", false) {
        val f = formulaIn.text.toString().trim()
        val massMap = elements.associate { it.sym to it.mass }
        val regex = Regex("([A-Z][a-z]?)(\\d*)")
        var totalMolar = 0.0
        val breakdown = mutableListOf<String>()
        for (match in regex.findAll(f)) {
            val sym = match.groupValues[1]
            val count = match.groupValues[2].toIntOrNull() ?: 1
            val m = massMap[sym] ?: 0.0
            if (m > 0) {
                totalMolar += m * count
                breakdown.add("$sym×$count = ${Utils.formatResult(m * count, 3)} g/mol")
            }
        }
        if (totalMolar > 0) {
            resTv.text = "נוסחה כימית: $f\nמסה מולרית כוללת: ${Utils.formatResult(totalMolar, 3)} g/mol\nפירוט: ${breakdown.joinToString(" | ")}"
        } else {
            resTv.text = "נא להזין נוסחה באותיות גדולות תקניות (למשל H2O, NaCl, C6H12O6)"
        }
    }

    root.addView(atomCanvas); root.addView(pickBtn); root.addView(formulaIn)
    root.addView(formulaBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 11. מסך אישי: מתרגם קוד מורס דו-כיווני (טקסט ↔ מורס) + משדר אור מהבהב
// =========================================================================
fun MainActivity.createProMorseScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("📡 מתרגם ומשדר קוד מורס דו-כיווני", "תרגום מטקסט למורס וממורס חזרה לטקסט + משדר אור ויזואלי"))

    val morseMap = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.",
        'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..",
        'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
        'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
        'Y' to "-.--", 'Z' to "--..", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
        '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.", '0' to "-----",
        'א' to ".-", 'ב' to "-...", 'ג' to "--.", 'ד' to "-..", 'ה' to "---", 'ו' to ".",
        'ז' to "--..", 'ח' to "....", 'ט' to "..-", 'י' to "..", 'כ' to "-.-", 'ל' to ".-..",
        'מ' to "--", 'נ' to "-.", 'ס' to "-.-.", 'ע' to ".---", 'פ' to ".--.", 'צ' to ".--",
        'ק' to "--.-", 'ר' to ".-.", 'ש' to "...", 'ת' to "-"
    )
    val reverseMorseEng = morseMap.entries.take(36).associate { it.value to it.key }

    val beaconView = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(75f)).apply { bottomMargin = dp(12f) }
        background = GradientDrawable().apply { setColor(Color.DKGRAY); cornerRadius = dp(18f).toFloat() }
    }

    val input = createInputBox("הקלד טקסט או קוד מורס (נקודות וקווים מופרדים ברווח)").apply {
        inputType = InputType.TYPE_CLASS_TEXT
        setText("SOS")
    }
    val resTv = createResultTextView().apply { text = "... --- ..." }

    input.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            val txt = s.toString().trim()
            if (txt.isNotEmpty() && txt.all { it == '.' || it == '-' || it == ' ' || it == '/' }) {
                val decoded = txt.split(" ").map { token ->
                    if (token == "/") " " else reverseMorseEng[token] ?: '?'
                }.joinToString("")
                resTv.text = "פענוח מורס לטקסט:\n$decoded"
            } else {
                resTv.text = txt.uppercase().map { ch -> if (ch == ' ') " / " else morseMap[ch] ?: "" }.joinToString(" ")
            }
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    val flashBtn = createActionBtn("שדר הודעת מורס באור מהבהב 💡") {
        val code = resTv.text.toString()
        val handler = Handler(Looper.getMainLooper())
        var delay = 0L
        val accent = if (isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else primaryColor
        for (ch in code.take(40)) {
            if (ch == '.' || ch == '-') {
                val dur = if (ch == '.') 140L else 380L
                handler.postDelayed({
                    beaconView.background = GradientDrawable().apply { setColor(accent); cornerRadius = dp(18f).toFloat() }
                }, delay)
                delay += dur
                handler.postDelayed({
                    beaconView.background = GradientDrawable().apply { setColor(Color.DKGRAY); cornerRadius = dp(18f).toFloat() }
                }, delay)
                delay += 120L
            } else {
                delay += 180L
            }
        }
    }

    root.addView(beaconView); root.addView(input); root.addView(flashBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 12. מסך אישי: מחולל סיסמאות מאובטחות, מד אנטרופיה וזמן פריצה משוער
// =========================================================================
fun MainActivity.createProPasswordGenScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🔐 מחולל סיסמאות, PIN ומד אנטרופיה", "ייצור סיסמאות קריפטוגרפיות, התאמת תווים וחישוב זמן עמידות לפריצה"))

    var entropyBits = 104f
    var includeSymbols = true
    val meterCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            p.color = Color.argb(80, 255, 255, 255)
            canvas.drawRoundRect(RectF(16f * d, 16f * d, w - 16f * d, 38f * d), 12f * d, 12f * d, p)
            val ratio = (entropyBits / 160f).coerceIn(0.1f, 1f)
            p.color = if (entropyBits >= 80f) Color.parseColor("#00E676") else Color.parseColor("#FFB74D")
            canvas.drawRoundRect(RectF(16f * d, 16f * d, 16f * d + (w - 32f * d) * ratio, 38f * d), 12f * d, 12f * d, p)
            p.color = Color.WHITE; p.textSize = 13.5f * d; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("אנטרופיה: ${entropyBits.toInt()} bits (${if (entropyBits >= 90f) "הצפנה צבאית חזקה" else "חוזק בינוני"})", w / 2f, h - 10f * d, p)
        }
    }.apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(95f)).apply { bottomMargin = dp(10f) } }

    val lenIn = createInputBox("אורך סיסמה (6 עד 64 תווים)").apply { setText("16") }
    val symBtn = createActionBtn("סימנים מיוחדים (!@#$): מופעל ✔️", false) {}
    val resTv = createResultTextView()

    symBtn.setOnClickListener {
        feedback(it)
        includeSymbols = !includeSymbols
        symBtn.text = if (includeSymbols) "סימנים מיוחדים (!@#$): מופעל ✔️" else "סימנים מיוחדים: כבוי (אותיות ומספרים בלבד)"
    }

    val genBtn = createActionBtn("צור סיסמה חדשה 🔐") {
        val len = (lenIn.text.toString().toIntOrNull() ?: 16).coerceIn(4, 64)
        val chars = if (includeSymbols) {
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#\$%^&*()-_=+"
        } else {
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
        }
        val pass = (1..len).map { chars.random() }.joinToString("")
        entropyBits = (len * (ln(chars.length.toDouble()) / ln(2.0))).toFloat()
        meterCanvas.invalidate()
        resTv.text = pass
    }
    genBtn.performClick()
    root.addView(meterCanvas); root.addView(lenIn); root.addView(symBtn); root.addView(genBtn)
    root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 13. מסך אישי: גימטריה רגילה וקטנה, צופן אתב״ש, ספרות רומיות דו-כיווני וטקסט
// =========================================================================
fun MainActivity.createProGematriaAndTextScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🔠 גימטריה, אתב״ש, ספרות רומיות וטקסט", "גימטריה רגילה וקטנה, צופן אתב״ש, ספרות רומיות דו-כיווני וסטטיסטיקת טקסט"))

    val gemMap = mapOf(
        'א' to 1, 'ב' to 2, 'ג' to 3, 'ד' to 4, 'ה' to 5, 'ו' to 6, 'ז' to 7, 'ח' to 8, 'ט' to 9,
        'י' to 10, 'כ' to 20, 'ך' to 20, 'ל' to 30, 'מ' to 40, 'ם' to 40, 'נ' to 50, 'ן' to 50,
        'ס' to 60, 'ע' to 70, 'פ' to 80, 'ף' to 80, 'צ' to 90, 'ץ' to 90, 'ק' to 100, 'ר' to 200,
        'ש' to 300, 'ת' to 400
    )
    val alefBet = "אבגדהוזחטיכלמנסעפצקרשת"
    val atbashMap = alefBet.mapIndexed { idx, c -> c to alefBet[alefBet.length - 1 - idx] }.toMap()

    val textIn = createInputBox("הקלד מילה/משפט בעברית או מספר/ספרות רומיות").apply { inputType = InputType.TYPE_CLASS_TEXT; setText("חכמה") }
    val resTv = createResultTextView()

    fun toRoman(num: Int): String {
        if (num <= 0 || num > 3999) return "לא בטווח (1-3999)"
        val vals = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
        val syms = arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
        var n = num
        val sb = StringBuilder()
        for (i in vals.indices) {
            while (n >= vals[i]) { n -= vals[i]; sb.append(syms[i]) }
        }
        return sb.toString()
    }

    val calcBtn = createActionBtn("חשב גימטריה, אתב״ש ורומית 📜") {
        val raw = textIn.text.toString().trim()
        val gVal = raw.sumOf { gemMap[it] ?: 0 }
        var smallGem = gVal
        while (smallGem >= 10) {
            smallGem = smallGem.toString().sumOf { it.digitToInt() }
        }
        val atbash = raw.map { ch -> atbashMap[ch] ?: ch }.joinToString("")
        val asNum = raw.toIntOrNull() ?: gVal
        val words = raw.split(Regex("\\s+")).filter { it.isNotEmpty() }.size

        resTv.text = "• גימטריה רגילה: $gVal (גימטריה קטנה: $smallGem)\n" +
                "• צופן אתב״ש: $atbash\n" +
                "• ספרות רומיות ($asNum): ${toRoman(asNum)}\n" +
                "• מילים: $words | תווים: ${raw.length}"
    }
    calcBtn.performClick()
    root.addView(textIn); root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}
 

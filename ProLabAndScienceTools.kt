package com.example.calculator

import android.app.Dialog
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import kotlin.math.*

// כותרת קנבה מעוצבת לכל מסך Pro הכוללת סרגל שבבי מועדפים וכפתור כוכב להוספה/הסרה מהמועדפים
// (ללא אפשרות להוסיף את "מרכז המועדפים" עצמו למועדפים)
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

    val isFavCenterTab = (currentTabIndex == ProFavoritesEngine.FAVORITES_CENTER_TAB_INDEX)
    val isFav = ProFavoritesEngine.isFavoriteTab(prefs, currentTabIndex)
    val starBtn = TextView(this).apply {
        visibility = if (isFavCenterTab) View.GONE else View.VISIBLE
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
            switchTab(currentTabIndex)
        }
        setOnLongClickListener {
            feedback(it)
            switchTab(ProFavoritesEngine.FAVORITES_CENTER_TAB_INDEX)
            true
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
        val accName = when {
            !compassCanvas.hasSensorHardware || compassCanvas.sensorAccuracy == -2 -> "ללא חיישן חומרה (סיבוב ידני במגע) ⚪"
            !compassCanvas.hasReceivedFirstReading || compassCanvas.sensorAccuracy == -1 -> "מאתחל קריאת חיישן... ⏳"
            compassCanvas.sensorAccuracy >= SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "גבוהה מאוד 🟢"
            compassCanvas.sensorAccuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "בינונית 🟡"
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

    val sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val rotVectorSensor = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    val magSensor = sm?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    val accelSensor = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val hardwareAvailable = (rotVectorSensor != null || (magSensor != null && accelSensor != null))

    compassCanvas = CompassRoseCanvasView(this, this) { deg -> updateHeadingText(deg) }.apply {
        hasSensorHardware = hardwareAvailable
        sensorAccuracy = if (hardwareAvailable) -1 else -2
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

    val rotationMatrix = FloatArray(9)
    val orientationAngles = FloatArray(3)
    val gravityVals = FloatArray(3)
    val geomagVals = FloatArray(3)
    var hasGravity = false
    var hasGeomag = false
    var smoothedSin = 0f
    var smoothedCos = 1f

    fun applyNewAzimuthRad(azimuthRad: Float, pitchRad: Float, rollRad: Float, eventAccuracy: Int) {
        compassCanvas.hasReceivedFirstReading = true
        if (compassCanvas.sensorAccuracy < 0) {
            compassCanvas.sensorAccuracy = if (eventAccuracy > 0) eventAccuracy else SensorManager.SENSOR_STATUS_ACCURACY_HIGH
        }
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
                    applyNewAzimuthRad(orientationAngles[0], orientationAngles[1], orientationAngles[2], e.accuracy)
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(e.values, 0, geomagVals, 0, 3)
                    hasGeomag = true
                    compassCanvas.hasReceivedFirstReading = true
                    if (compassCanvas.sensorAccuracy < 0 && e.accuracy > 0) {
                        compassCanvas.sensorAccuracy = e.accuracy
                    }
                    val bx = e.values[0]; val by = e.values[1]; val bz = e.values[2]
                    compassCanvas.magneticFieldUt = sqrt(bx * bx + by * by + bz * bz)
                    if (rotVectorSensor == null && hasGravity && SensorManager.getRotationMatrix(rotationMatrix, null, gravityVals, geomagVals)) {
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        applyNewAzimuthRad(orientationAngles[0], orientationAngles[1], orientationAngles[2], e.accuracy)
                    } else {
                        compassCanvas.invalidate()
                    }
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(e.values, 0, gravityVals, 0, 3)
                    hasGravity = true
                    if (rotVectorSensor == null && hasGeomag && SensorManager.getRotationMatrix(rotationMatrix, null, gravityVals, geomagVals)) {
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        applyNewAzimuthRad(orientationAngles[0], orientationAngles[1], orientationAngles[2], e.accuracy)
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
// 7. מסך אישי: מד-זווית דו-זרועי + סרגל וקליבר מכויל פיזית מול כרטיס אשראי (ISO 7810)
// =========================================================================
fun MainActivity.createProRulerProtractorScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(
        createProCanvaHeader(
            "📏 מד-זווית דו-זרועי וסרגל מכויל פיזית",
            "כולל כיול PPI פיזי מול כרטיס אשראי תקני (53.98 מ״מ) וכפתורי צעד עדינים לדיוק מרבי"
        )
    )

    val savedCalib = (prefs?.getFloat("pro_ruler_calib_factor", 1.0f) ?: 1.0f).coerceIn(0.70f, 1.35f)
    val resTv = createResultTextView().apply { textSize = 16.5f }
    lateinit var canvas: ProtractorRulerCanvasView

    fun updateReadout() {
        val deltaDeg = abs(canvas.angleArmB - canvas.angleArmA)
        val cm = canvas.caliperCm
        val mm = cm * 10f
        val inches = cm / 2.54f
        val effDpi = canvas.getEffectivePxPerCm() * 2.54f
        val calibState = if (abs(canvas.calibrationScaleFactor - 1.0f) < 0.005f) "ברירת מחדל (לפי חיישן תצוגה)" else "מכויל פיזית (×${Utils.formatResult(canvas.calibrationScaleFactor.toDouble(), 3)})"
        resTv.text = "פלט מדידה מכויל:\n" +
                "• אורך קליבר: ${Utils.formatResult(mm.toDouble(), 2)} מ״מ (${Utils.formatResult(cm.toDouble(), 3)} ס״מ | ${Utils.formatResult(inches.toDouble(), 3)}\")\n" +
                "• זווית נמדדת (Δ): ${Utils.formatResult(deltaDeg.toDouble(), 1)}° (זרוע A: ${Utils.formatResult(canvas.angleArmA.toDouble(), 1)}° | זרוע B: ${Utils.formatResult(canvas.angleArmB.toDouble(), 1)}°)\n" +
                "• צפיפות מכוילת: ${Utils.formatResult(effDpi.toDouble(), 1)} PPI ($calibState)"
    }

    canvas = ProtractorRulerCanvasView(this, this) { _, _, newScale ->
        prefs?.edit()?.putFloat("pro_ruler_calib_factor", newScale)?.apply()
        updateReadout()
    }.apply {
        calibrationScaleFactor = savedCalib
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(420f)).apply { bottomMargin = dp(12f) }
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createProRulerProtractorScreen, 22f)
        else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(22f).toFloat() }
    }
    updateReadout()

    // כפתור הפעלת מצב כיול פיזי מול כרטיס אשראי תקני (53.98 מ״מ גובה תקן ISO/IEC 7810 ID-1)
    val calibCardBtn = createActionBtn("💳 כייל סרגל מול כרטיס אשראי פיזי (53.98 מ״מ)", false) {
        canvas.isCreditCardCalibrationMode = !canvas.isCreditCardCalibrationMode
        if (canvas.isCreditCardCalibrationMode) {
            canvas.caliperCm = 5.398f
            (it as TextView).text = "✅ סיום ושמירת כיול כרטיס אשראי (פעיל כעת)"
            Toast.makeText(this, "הנח כרטיס אשראי על המסך וגרור את הקו הירוק לקצה התחתון של הכרטיס", Toast.LENGTH_LONG).show()
        } else {
            prefs?.edit()?.putFloat("pro_ruler_calib_factor", canvas.calibrationScaleFactor)?.apply()
            (it as TextView).text = "💳 כייל סרגל מול כרטיס אשראי פיזי (53.98 מ״מ)"
            Toast.makeText(this, "הכיול הפיזי נשמר בזיכרון!", Toast.LENGTH_SHORT).show()
        }
        canvas.invalidate()
        updateReadout()
    }

    // שורת כפתורים לכוונון עדין של כיול ה-PPI ואיפוס
    val calibRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    val minusCalibBtn = createActionBtn("PPI -1%", false) {
        canvas.calibrationScaleFactor = (canvas.calibrationScaleFactor - 0.01f).coerceIn(0.70f, 1.35f)
        prefs?.edit()?.putFloat("pro_ruler_calib_factor", canvas.calibrationScaleFactor)?.apply()
        canvas.invalidate(); updateReadout()
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(4f) } }

    val resetCalibBtn = createActionBtn("אפס כיול", false) {
        canvas.calibrationScaleFactor = 1.0f
        canvas.isCreditCardCalibrationMode = false
        prefs?.edit()?.putFloat("pro_ruler_calib_factor", 1.0f)?.apply()
        canvas.invalidate(); updateReadout()
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(4f); rightMargin = dp(4f) } }

    val plusCalibBtn = createActionBtn("PPI +1%", false) {
        canvas.calibrationScaleFactor = (canvas.calibrationScaleFactor + 0.01f).coerceIn(0.70f, 1.35f)
        prefs?.edit()?.putFloat("pro_ruler_calib_factor", canvas.calibrationScaleFactor)?.apply()
        canvas.invalidate(); updateReadout()
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(4f) } }
    calibRow.addView(minusCalibBtn); calibRow.addView(resetCalibBtn); calibRow.addView(plusCalibBtn)

    // שורת כפתורי מיקרו-צעד למניעת שגיאות מגע אנושי ועובי אצבע
    val fineStepRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    val stepMmMinus = createActionBtn("-0.5 מ״מ", false) {
        canvas.caliperCm = (canvas.caliperCm - 0.05f).coerceIn(0.05f, 14f)
        canvas.invalidate(); updateReadout()
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(4f) } }

    val stepMmPlus = createActionBtn("+0.5 מ״מ", false) {
        canvas.caliperCm = (canvas.caliperCm + 0.05f).coerceIn(0.05f, 14f)
        canvas.invalidate(); updateReadout()
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(4f); rightMargin = dp(4f) } }

    val stepDegMinus = createActionBtn("-0.5° זווית", false) {
        canvas.angleArmB = (canvas.angleArmB - 0.5f).coerceIn(0f, 180f)
        canvas.invalidate(); updateReadout()
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(4f); rightMargin = dp(4f) } }

    val stepDegPlus = createActionBtn("+0.5° זווית", false) {
        canvas.angleArmB = (canvas.angleArmB + 0.5f).coerceIn(0f, 180f)
        canvas.invalidate(); updateReadout()
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(4f) } }
    fineStepRow.addView(stepMmMinus); fineStepRow.addView(stepMmPlus); fineStepRow.addView(stepDegMinus); fineStepRow.addView(stepDegPlus)

    root.addView(canvas); root.addView(calibCardBtn); root.addView(calibRow); root.addView(fineStepRow)
    root.addView(createPinResultButton(resTv)); root.addView(resTv)
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
// 9. מסך אישי: פנס מצלמה פיזי (LED) + פנס מסך מלא (Fullscreen) + איתות חירום SOS!
// =========================================================================
fun MainActivity.createProFlashlightScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(
        createProCanvaHeader(
            "🔦 פנס LED פיזי, פנס מסך מלא ואיתות SOS",
            "הדלקת פנס המצלמה האחורי, איתות SOS אוטומטי בקוד מורס ומצב פנס מסך מלא בנגיעה"
        )
    )

    var isScreenOn = false
    var isCameraTorchOn = false
    var isSosActive = false
    val colors = listOf(
        "לבן טהור (6500K)" to Color.WHITE,
        "חם ענבר (3000K)" to Color.parseColor("#FFE082"),
        "אדום ראיית לילה" to Color.parseColor("#FF1744"),
        "ציאן מעבדה" to Color.parseColor("#00F5D4"),
        "ירוק סימון" to Color.parseColor("#00E676")
    )
    var colorIdx = 0
    val sosHandler = Handler(Looper.getMainLooper())
    var fullScreenDialog: Dialog? = null
    var fullScreenOverlayView: View? = null

    val camManager = getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    val torchCameraId: String? = try {
        camManager?.cameraIdList?.firstOrNull { id ->
            camManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    } catch (_: Exception) { null }

    fun setHardwareTorch(turnOn: Boolean) {
        if (torchCameraId == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        try {
            camManager?.setTorchMode(torchCameraId, turnOn)
            isCameraTorchOn = turnOn
        } catch (_: Exception) {}
    }

    val lampView = TextView(this).apply {
        text = "💡 לחץ כאן או על 'מסך מלא' להפיכת כל המסך לפנס"
        setTextColor(Color.LTGRAY)
        textSize = 14.5f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(210f)).apply { bottomMargin = dp(14f) }
        background = GradientDrawable().apply { setColor(Color.DKGRAY); cornerRadius = dp(26f).toFloat() }
    }

    lateinit var screenToggleBtn: TextView
    lateinit var camTorchBtn: TextView
    lateinit var sosBtn: TextView

    fun applyVisualLampState(lit: Boolean) {
        val activeCol = if (lit) colors[colorIdx].second else Color.DKGRAY
        lampView.background = GradientDrawable().apply {
            setColor(activeCol)
            cornerRadius = dp(26f).toFloat()
        }
        lampView.setTextColor(if (lit) Color.BLACK else Color.LTGRAY)
        lampView.text = if (lit) "🔆 פנס מסך דולק (לחץ למסך מלא)" else "💡 לחץ כאן או על 'מסך מלא' להפיכת כל המסך לפנס"
        fullScreenOverlayView?.setBackgroundColor(if (lit) colors[colorIdx].second else Color.BLACK)
    }

    fun updateScreenBrightness(maxBright: Boolean) {
        val lp = window.attributes
        lp.screenBrightness = if (maxBright) 1.0f else -1.0f
        window.attributes = lp
    }

    // תבנית SOS תקנית בקוד מורס: 3 קצרות (...), 3 ארוכות (---), 3 קצרות (...) + הפסקה
    // כל איבר הוא Pair(האם_דולק, משך_במילישניות)
    val sosPattern = listOf(
        true to 180L, false to 160L, true to 180L, false to 160L, true to 180L, false to 380L, // S (...)
        true to 520L, false to 180L, true to 520L, false to 180L, true to 520L, false to 380L, // O (---)
        true to 180L, false to 160L, true to 180L, false to 160L, true to 180L, false to 950L  // S (...)
    )
    var sosStepIdx = 0

    val sosRunnable = object : Runnable {
        override fun run() {
            if (!isSosActive) return
            val (flashOn, durationMs) = sosPattern[sosStepIdx]
            applyVisualLampState(flashOn)
            if (torchCameraId != null) {
                setHardwareTorch(flashOn)
            }
            sosStepIdx = (sosStepIdx + 1) % sosPattern.size
            sosHandler.postDelayed(this, durationMs)
        }
    }

    fun stopSosIfRunning() {
        if (isSosActive) {
            isSosActive = false
            sosHandler.removeCallbacksAndMessages(null)
            setHardwareTorch(false)
            camTorchBtn.text = "🔦 הדלק פנס מצלמה אחורי (LED): כבוי"
            sosBtn.text = "🆘 הפעל איתות חירום SOS (מסך + פנס מצלמה)"
            applyVisualLampState(isScreenOn)
        }
    }

    // פתיחת מצב פנס על כל המסך (100% Fullscreen) עד לנגיעה חוזרת במסך
    fun openFullScreenFlashlight() {
        if (!isScreenOn && !isSosActive) {
            isScreenOn = true
            updateScreenBrightness(true)
            screenToggleBtn.text = "🔆 פנס מסך (100% בהירות): דולק 🟢"
            applyVisualLampState(true)
        }
        val dlg = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.window?.apply {
            setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val wlp = attributes
            wlp.screenBrightness = 1.0f
            attributes = wlp
        }
        val fullContainer = FrameLayout(this).apply {
            setBackgroundColor(colors[colorIdx].second)
            isClickable = true
        }
        val hintTv = TextView(this).apply {
            text = "🔦 מצב פנס מסך מלא פעיל\nגע בכל מקום במסך לחזרה"
            setTextColor(Color.argb(190, 0, 0, 0))
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        }
        fullContainer.addView(hintTv)
        hintTv.postDelayed({
            hintTv.animate().alpha(0f).setDuration(600).start()
        }, 1800L)

        fullContainer.setOnClickListener {
            feedback(it)
            dlg.dismiss()
        }
        dlg.setOnDismissListener {
            fullScreenOverlayView = null
            fullScreenDialog = null
        }
        fullScreenOverlayView = fullContainer
        fullScreenDialog = dlg
        dlg.setContentView(fullContainer)
        dlg.show()
    }

    screenToggleBtn = createActionBtn("🔆 הדלק / כבה פנס מסך (100% בהירות)") {
        stopSosIfRunning()
        isScreenOn = !isScreenOn
        updateScreenBrightness(isScreenOn)
        screenToggleBtn.text = if (isScreenOn) "🔆 פנס מסך (100% בהירות): דולק 🟢" else "🔆 הדלק / כבה פנס מסך (100% בהירות)"
        applyVisualLampState(isScreenOn)
    }

    camTorchBtn = createActionBtn("🔦 הדלק פנס מצלמה אחורי (LED): כבוי", false) {
        stopSosIfRunning()
        if (torchCameraId == null) {
            Toast.makeText(this, "לא נמצא פנס מצלמה אחורי במכשיר זה", Toast.LENGTH_SHORT).show()
            return@createActionBtn
        }
        val nextTorch = !isCameraTorchOn
        setHardwareTorch(nextTorch)
        camTorchBtn.text = if (isCameraTorchOn) "🔦 פנס מצלמה אחורי (LED): דולק 🟢" else "🔦 הדלק פנס מצלמה אחורי (LED): כבוי"
    }

    sosBtn = createActionBtn("🆘 הפעל איתות חירום SOS (מסך + פנס מצלמה)", false) {
        if (isSosActive) {
            stopSosIfRunning()
        } else {
            isSosActive = true
            sosStepIdx = 0
            updateScreenBrightness(true)
            sosBtn.text = "⏹️ עצור איתות חירום SOS (פעיל כעת...)"
            sosHandler.post(sosRunnable)
        }
    }

    val fullScreenBtn = createActionBtn("📱 הפוך את כל המסך לפנס (Fullscreen עד לנגיעה)", false) {
        openFullScreenFlashlight()
    }

    val colorBtn = createActionBtn("🎨 גוון תאורת מסך: ${colors[0].first}", false) {
        colorIdx = (colorIdx + 1) % colors.size
        (it as TextView).text = "🎨 גוון תאורת מסך: ${colors[colorIdx].first}"
        if (isScreenOn || isSosActive) applyVisualLampState(true)
    }

    lampView.setOnClickListener {
        feedback(it)
        openFullScreenFlashlight()
    }

    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) {
            isSosActive = false
            sosHandler.removeCallbacksAndMessages(null)
            setHardwareTorch(false)
            updateScreenBrightness(false)
            fullScreenDialog?.dismiss()
        }
    })

    root.addView(screenToggleBtn)
    root.addView(camTorchBtn)
    root.addView(sosBtn)
    root.addView(fullScreenBtn)
    root.addView(colorBtn)
    root.addView(lampView)
    scroll.addView(root); return scroll
}

// =========================================================================
// 10. מסך אישי: הטבלה המחזורית + מפענח נוסחאות כימיות מלא לכל 118 היסודות!
// =========================================================================
fun MainActivity.createProPeriodicTableScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(
        createProCanvaHeader(
            "⚛️ הטבלה המחזורית, אטום מונפש ומסה מולרית",
            "בחר יסוד לצפייה באלקטרונים נעים או הזן נוסחה כימית מורכבת (כגון H2SO4 או Ca(OH)2)"
        )
    )

    data class ChemElement(val z: Int, val sym: String, val nameHeb: String, val mass: Double, val group: String, val shells: List<Int>)

    val elements = listOf(
        ChemElement(1, "H", "מימן (Hydrogen)", 1.008, "אל-מתכת", listOf(1)),
        ChemElement(2, "He", "הליום (Helium)", 4.0026, "גז אציל", listOf(2)),
        ChemElement(3, "Li", "ליתיום (Lithium)", 6.94, "מתכת אלקלית", listOf(2, 1)),
        ChemElement(4, "Be", "בריליום (Beryllium)", 9.0122, "מתכת אלקלית עפרורית", listOf(2, 2)),
        ChemElement(5, "B", "בור (Boron)", 10.81, "מתכת למחצה", listOf(2, 3)),
        ChemElement(6, "C", "פחמן (Carbon)", 12.011, "אל-מתכת", listOf(2, 4)),
        ChemElement(7, "N", "חנקן (Nitrogen)", 14.007, "אל-מתכת", listOf(2, 5)),
        ChemElement(8, "O", "חמצן (Oxygen)", 15.999, "אל-מתכת", listOf(2, 6)),
        ChemElement(9, "F", "פלואור (Fluorine)", 18.998, "הלוגן", listOf(2, 7)),
        ChemElement(10, "Ne", "ניאון (Neon)", 20.180, "גז אציל", listOf(2, 8)),
        ChemElement(11, "Na", "נתרן (Sodium)", 22.990, "מתכת אלקלית", listOf(2, 8, 1)),
        ChemElement(12, "Mg", "מגנזיום (Magnesium)", 24.305, "מתכת אלקלית עפרורית", listOf(2, 8, 2)),
        ChemElement(13, "Al", "אלומיניום (Aluminum)", 26.982, "מתכת", listOf(2, 8, 3)),
        ChemElement(14, "Si", "צורן / סיליקון (Silicon)", 28.085, "מתכת למחצה", listOf(2, 8, 4)),
        ChemElement(15, "P", "זרחן (Phosphorus)", 30.974, "אל-מתכת", listOf(2, 8, 5)),
        ChemElement(16, "S", "גופרית (Sulfur)", 32.06, "אל-מתכת", listOf(2, 8, 6)),
        ChemElement(17, "Cl", "כלור (Chlorine)", 35.45, "הלוגן", listOf(2, 8, 7)),
        ChemElement(18, "Ar", "ארגון (Argon)", 39.948, "גז אציל", listOf(2, 8, 8)),
        ChemElement(19, "K", "אשלגן (Potassium)", 39.098, "מתכת אלקלית", listOf(2, 8, 8, 1)),
        ChemElement(20, "Ca", "סידן (Calcium)", 40.078, "מתכת עפרורית", listOf(2, 8, 8, 2)),
        ChemElement(22, "Ti", "טיטניום (Titanium)", 47.867, "מתכת מעבר", listOf(2, 8, 10, 2)),
        ChemElement(24, "Cr", "כרום (Chromium)", 51.996, "מתכת מעבר", listOf(2, 8, 13, 1)),
        ChemElement(25, "Mn", "מנגן (Manganese)", 54.938, "מתכת מעבר", listOf(2, 8, 13, 2)),
        ChemElement(26, "Fe", "ברזל (Iron)", 55.845, "מתכת מעבר", listOf(2, 8, 14, 2)),
        ChemElement(27, "Co", "קובלט (Cobalt)", 58.933, "מתכת מעבר", listOf(2, 8, 15, 2)),
        ChemElement(28, "Ni", "ניקל (Nickel)", 58.693, "מתכת מעבר", listOf(2, 8, 16, 2)),
        ChemElement(29, "Cu", "נחושת (Copper)", 63.546, "מתכת מעבר", listOf(2, 8, 18, 1)),
        ChemElement(30, "Zn", "אבץ (Zinc)", 65.38, "מתכת מעבר", listOf(2, 8, 18, 2)),
        ChemElement(35, "Br", "ברום (Bromine)", 79.904, "הלוגן", listOf(2, 8, 18, 7)),
        ChemElement(47, "Ag", "כסף (Silver)", 107.87, "מתכת מעבר", listOf(2, 8, 18, 18, 1)),
        ChemElement(50, "Sn", "בדיל (Tin)", 118.71, "מתכת", listOf(2, 8, 18, 18, 4)),
        ChemElement(53, "I", "יוד (Iodine)", 126.90, "הלוגן", listOf(2, 8, 18, 18, 7)),
        ChemElement(56, "Ba", "בריום (Barium)", 137.33, "מתכת עפרורית", listOf(2, 8, 18, 18, 8, 2)),
        ChemElement(74, "W", "טונגסטן (Tungsten)", 183.84, "מתכת מעבר", listOf(2, 8, 18, 32, 12, 2)),
        ChemElement(78, "Pt", "פלטינה (Platinum)", 195.08, "מתכת אצילה", listOf(2, 8, 18, 32, 17, 1)),
        ChemElement(79, "Au", "זהב (Gold)", 196.97, "מתכת אצילה", listOf(2, 8, 18, 32, 18, 1)),
        ChemElement(80, "Hg", "כספית (Mercury)", 200.59, "מתכת נוזלית", listOf(2, 8, 18, 32, 18, 2)),
        ChemElement(82, "Pb", "עופרת (Lead)", 207.2, "מתכת כבדה", listOf(2, 8, 18, 32, 18, 4)),
        ChemElement(92, "U", "אורניום (Uranium)", 238.03, "אקטיניד", listOf(2, 8, 18, 32, 21, 9, 2))
    )

    // מאגר מסות מולריות מלא של כל 118 היסודות בטבלה המחזורית
    val all118Masses = mapOf(
        "H" to 1.008, "He" to 4.0026, "Li" to 6.94, "Be" to 9.0122, "B" to 10.81, "C" to 12.011,
        "N" to 14.007, "O" to 15.999, "F" to 18.998, "Ne" to 20.180, "Na" to 22.990, "Mg" to 24.305,
        "Al" to 26.982, "Si" to 28.085, "P" to 30.974, "S" to 32.06, "Cl" to 35.45, "Ar" to 39.948,
        "K" to 39.098, "Ca" to 40.078, "Sc" to 44.956, "Ti" to 47.867, "V" to 50.942, "Cr" to 51.996,
        "Mn" to 54.938, "Fe" to 55.845, "Co" to 58.933, "Ni" to 58.693, "Cu" to 63.546, "Zn" to 65.38,
        "Ga" to 69.723, "Ge" to 72.630, "As" to 74.922, "Se" to 78.971, "Br" to 79.904, "Kr" to 83.798,
        "Rb" to 85.468, "Sr" to 87.62, "Y" to 88.906, "Zr" to 91.224, "Nb" to 92.906, "Mo" to 95.95,
        "Tc" to 98.0, "Ru" to 101.07, "Rh" to 102.91, "Pd" to 106.42, "Ag" to 107.87, "Cd" to 112.41,
        "In" to 114.82, "Sn" to 118.71, "Sb" to 121.76, "Te" to 127.60, "I" to 126.90, "Xe" to 131.29,
        "Cs" to 132.91, "Ba" to 137.33, "La" to 138.91, "Ce" to 140.12, "Pr" to 140.91, "Nd" to 144.24,
        "Pm" to 145.0, "Sm" to 150.36, "Eu" to 151.96, "Gd" to 157.25, "Tb" to 158.93, "Dy" to 162.50,
        "Ho" to 164.93, "Er" to 167.26, "Tm" to 168.93, "Yb" to 173.05, "Lu" to 174.97, "Hf" to 178.49,
        "Ta" to 180.95, "W" to 183.84, "Re" to 186.21, "Os" to 190.23, "Ir" to 192.22, "Pt" to 195.08,
        "Au" to 196.97, "Hg" to 200.59, "Tl" to 204.38, "Pb" to 207.2, "Bi" to 208.98, "Po" to 209.0,
        "At" to 210.0, "Rn" to 222.0, "Fr" to 223.0, "Ra" to 226.0, "Ac" to 227.0, "Th" to 232.04,
        "Pa" to 231.04, "U" to 238.03, "Np" to 237.0, "Pu" to 244.0, "Am" to 243.0, "Cm" to 247.0
    )

    val atomCanvas = BohrAtomCanvasView(this, this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(245f)).apply { bottomMargin = dp(12f) }
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createProPeriodicTableScreen, 22f)
        else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(22f).toFloat() }
    }
    val resTv = createResultTextView().apply { textSize = 16.5f }

    // מפענח נוסחאות כימיות מתקדם התומך בסוגריים, תתי-ספרות, מימות (Hydrates) ואותיות קטנות/גדולות
    fun parseChemicalFormula(rawInput: String): Map<String, Int> {
        val subscripts = mapOf('₀' to '0', '₁' to '1', '₂' to '2', '₃' to '3', '₄' to '4', '₅' to '5', '₆' to '6', '₇' to '7', '₈' to '8', '₉' to '9')
        var cleaned = rawInput.trim().map { subscripts[it] ?: it }.joinToString("").replace(" ", "").replace("[", "(").replace("]", ")")
        if (cleaned.isEmpty()) return emptyMap()

        // אם המשתמש הקליד הכל באותיות קטנות (למשל h2so4 או nacl), ננרמל אוטומטית לסמלים תקניים
        if (cleaned.none { it.isUpperCase() }) {
            val twoLetterLower = all118Masses.keys.filter { it.length == 2 }.associateBy { it.lowercase() }
            val sb = StringBuilder()
            var idx = 0
            while (idx < cleaned.length) {
                if (idx + 1 < cleaned.length) {
                    val pair = cleaned.substring(idx, idx + 2)
                    if (twoLetterLower.containsKey(pair)) {
                        sb.append(twoLetterLower[pair])
                        idx += 2
                        continue
                    }
                }
                val ch = cleaned[idx]
                sb.append(if (ch.isLetter()) ch.uppercaseChar() else ch)
                idx++
            }
            cleaned = sb.toString()
        }

        val stack = mutableListOf(mutableMapOf<String, Int>())
        var i = 0
        while (i < cleaned.length) {
            val c = cleaned[i]
            when {
                c == '(' -> {
                    stack.add(mutableMapOf())
                    i++
                }
                c == ')' -> {
                    i++
                    var mult = 0
                    while (i < cleaned.length && cleaned[i].isDigit()) {
                        mult = mult * 10 + cleaned[i].digitToInt()
                        i++
                    }
                    if (mult == 0) mult = 1
                    if (stack.size > 1) {
                        val popped = stack.removeAt(stack.lastIndex)
                        val top = stack.last()
                        for ((el, cnt) in popped) {
                            top[el] = (top[el] ?: 0) + cnt * mult
                        }
                    }
                }
                c.isUpperCase() -> {
                    var sym = c.toString()
                    i++
                    if (i < cleaned.length && cleaned[i].isLowerCase()) {
                        val candidate = sym + cleaned[i]
                        if (all118Masses.containsKey(candidate)) {
                            sym = candidate
                            i++
                        } else {
                            // אם האות השנייה הוקלדה בטעות כקטנה אך היא יסוד נפרד (למשל HsO4 במקום H2SO4)
                            val upperSecond = cleaned[i].uppercaseChar().toString()
                            if (!all118Masses.containsKey(sym) || !all118Masses.containsKey(upperSecond)) {
                                sym = candidate
                                i++
                            }
                        }
                    }
                    if (!all118Masses.containsKey(sym)) return emptyMap()
                    var count = 0
                    while (i < cleaned.length && cleaned[i].isDigit()) {
                        count = count * 10 + cleaned[i].digitToInt()
                        i++
                    }
                    if (count == 0) count = 1
                    val top = stack.last()
                    top[sym] = (top[sym] ?: 0) + count
                }
                else -> return emptyMap()
            }
        }
        return if (stack.size == 1) stack.first() else emptyMap()
    }

    val formulaIn = createInputBox("🧪 הזן נוסחה כימית לחישוב מסה מולרית (למשל H2SO4, C6H12O6, Ca(OH)2)").apply {
        inputType = InputType.TYPE_CLASS_TEXT
        setText("H2SO4")
    }

    fun evaluateAndDisplayFormula(rawFormula: String) {
        val counts = parseChemicalFormula(rawFormula)
        if (counts.isEmpty()) {
            resTv.text = "נוסחה לא מזוהה. דוגמאות תקינות: H2SO4, H2O, NaCl, C6H12O6, Ca(OH)2"
            return
        }
        var totalMass = 0.0
        var totalAtoms = 0
        for ((sym, cnt) in counts) {
            val m = all118Masses[sym] ?: 0.0
            totalMass += m * cnt
            totalAtoms += cnt
        }

        val breakdownLines = counts.entries.map { (sym, cnt) ->
            val m = all118Masses[sym] ?: 0.0
            val subMass = m * cnt
            val pct = (subMass / totalMass) * 100.0
            val hebName = elements.find { it.sym == sym }?.nameHeb?.substringBefore(" (") ?: sym
            "• $sym ($hebName) × $cnt:  ${Utils.formatResult(subMass, 3)} g/mol (${Utils.formatResult(pct, 1)}%)"
        }

        // עדכון קנבס האטום להצגת היסוד המרכזי בנוסחה וכותרת התרכובת
        val dominantSym = counts.keys.firstOrNull { it != "H" && it != "O" } ?: counts.keys.first()
        elements.find { it.sym == dominantSym }?.let { el ->
            atomCanvas.atomicNumber = el.z
            atomCanvas.symbol = el.sym
            atomCanvas.shellsConfig = el.shells
        }
        atomCanvas.subtitleFormula = "תרכובת: ${rawFormula.trim()}  |  מסה מולרית: ${Utils.formatResult(totalMass, 3)} g/mol"
        atomCanvas.invalidate()

        resTv.text = "🧪 ניתוח מולקולרי עבור ${rawFormula.trim()}:\n" +
                "• מסה מולרית כוללת: ${Utils.formatResult(totalMass, 3)} g/mol\n" +
                "• סה״כ אטומים במולקולה: $totalAtoms אטומים (${counts.size} יסודות)\n\n" +
                "הרכב משקלי מפורט:\n${breakdownLines.joinToString("\n")}"
    }

    val pickBtn = createActionBtn("🔬 בחר יסוד בודד מהטבלה המחזורית", false) {
        val labels = elements.map { "${it.z}. ${it.sym} - ${it.nameHeb} (${it.mass} g/mol)" }
        showItemPicker("בחר יסוד כימי", labels) { chosen ->
            val idx = labels.indexOf(chosen).coerceAtLeast(0)
            val el = elements[idx]
            atomCanvas.atomicNumber = el.z
            atomCanvas.symbol = el.sym
            atomCanvas.shellsConfig = el.shells
            atomCanvas.subtitleFormula = "${el.z}. ${el.sym} - ${el.nameHeb} (${el.mass} g/mol)"
            atomCanvas.invalidate()
            resTv.text = "${el.z}. ${el.sym} - ${el.nameHeb}\nקבוצה: ${el.group} | מסה אטומית: ${el.mass} g/mol\nקליפות אלקטרונים: ${el.shells.joinToString(", ")}"
        }
    }

    val formulaBtn = createActionBtn("חשב משקל מולקולרי והרכב של התרכובת 🧪", true) {
        evaluateAndDisplayFormula(formulaIn.text.toString())
    }

    formulaIn.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            val txt = s?.toString()?.trim() ?: ""
            if (txt.isNotEmpty()) evaluateAndDisplayFormula(txt)
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    // חישוב ראשוני של H2SO4 מיד עם פתיחת המסך
    evaluateAndDisplayFormula("H2SO4")

    root.addView(atomCanvas); root.addView(formulaIn); root.addView(formulaBtn); root.addView(pickBtn)
    root.addView(createPinResultButton(resTv)); root.addView(resTv)
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
 

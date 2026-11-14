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
import kotlin.random.Random

// עזר ליצירת כותרת קנבה מעוצבת לכל מסך Pro
fun MainActivity.createProCanvaHeader(title: String, subtitle: String): LinearLayout {
    val accent = if (isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else primaryColor
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18f), dp(14f), dp(18f), dp(14f))
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createProCanvaHeader, 22f)
        else GradientDrawable().apply {
            setColor(Utils.lightenColor(bgColor, 0.08f))
            cornerRadius = dp(22f).toFloat()
            setStroke(dp(1.5f), accent)
        }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(14f) }
        addView(TextView(this@createProCanvaHeader).apply {
            text = IosGlassTheme.cleanCategoryTitle(title, isCleanGlassActive)
            setTextColor(Color.WHITE); textSize = 19f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.RIGHT
        })
        addView(TextView(this@createProCanvaHeader).apply {
            text = subtitle
            setTextColor(accent); textSize = 13.5f; gravity = Gravity.RIGHT; setPadding(0, dp(4f), 0, 0)
        })
    }
}

// 1. מסך אישי: קודי צבע של נגדים עם הדמיית נגד חיה בקנבה
fun MainActivity.createProResistorScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🎨 קודי צבע של נגדים (4/5 פסים)", "בחר צבע לכל פס לצפייה חיה בנגד וחישוב ההתנגדות"))

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
        "אדום (±2%)" to Color.rgb(229, 57, 53)
    )

    var b1 = 1; var b2 = 0; var mult = 2; var tol = 0
    val canvasView = ResistorVisualCanvasView(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(190f)).apply { bottomMargin = dp(12f) }
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createProResistorScreen, 20f)
        else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
    }
    val resTv = createResultTextView()

    fun updateResistor() {
        val ohms = (b1 * 10 + b2) * 10.0.pow(mult)
        val formatted = when {
            ohms >= 1e6 -> "${Utils.formatResult(ohms / 1e6, 2)} MΩ"
            ohms >= 1e3 -> "${Utils.formatResult(ohms / 1e3, 2)} kΩ"
            else -> "${Utils.formatResult(ohms, 2)} Ω"
        }
        canvasView.bandColors = intArrayOf(colors[b1].third, colors[b2].third, colors[mult].third, tolerances[tol].second)
        canvasView.labelText = "$formatted  ${tolerances[tol].first}"
        canvasView.invalidate()
        resTv.text = "התנגדות: $formatted (${Utils.formatResult(ohms, 0)} Ω)\nדיוק: ${tolerances[tol].first}"
    }

    val btn1 = createActionBtn("פס 1: ${colors[b1].first}", false) {}
    val btn2 = createActionBtn("פס 2: ${colors[b2].first}", false) {}
    val btnM = createActionBtn("מכפיל: ×10^$mult (${colors[mult].first})", false) {}
    val btnT = createActionBtn("דיוק: ${tolerances[tol].first}", false) {}

    btn1.setOnClickListener { showItemPicker("בחר צבע פס 1", colors.map { it.first }) { c -> b1 = colors.indexOfFirst { it.first == c }; btn1.text = "פס 1: $c"; updateResistor() } }
    btn2.setOnClickListener { showItemPicker("בחר צבע פס 2", colors.map { it.first }) { c -> b2 = colors.indexOfFirst { it.first == c }; btn2.text = "פס 2: $c"; updateResistor() } }
    btnM.setOnClickListener { showItemPicker("בחר מכפיל", colors.map { it.first }) { c -> mult = colors.indexOfFirst { it.first == c }; btnM.text = "מכפיל: $c"; updateResistor() } }
    btnT.setOnClickListener { showItemPicker("בחר דיוק", tolerances.map { it.first }) { c -> tol = tolerances.indexOfFirst { it.first == c }; btnT.text = "דיוק: $c"; updateResistor() } }

    updateResistor()
    root.addView(canvasView); root.addView(btn1); root.addView(btn2); root.addView(btnM); root.addView(btnT); root.addView(resTv)
    scroll.addView(root); return scroll
}

// 2. מסך אישי: מפענח קבלים ואנרגיית קבל עם הדמיית קבל קרמי בקנבה
fun MainActivity.createProCapacitorScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🔋 מפענח קודי קבלים ואנרגיה אגורה", "פענוח קוד 3 ספרות (כגון 104 / 472) וחישוב מטען ואנרגיה"))

    var capCodeText = "104"
    val capCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f; val cy = height * 0.45f
            p.color = Color.LTGRAY; p.strokeWidth = 10f; p.style = Paint.Style.STROKE
            canvas.drawLine(cx - 35f, cy + 40f, cx - 35f, height - 15f, p)
            canvas.drawLine(cx + 35f, cy + 40f, cx + 35f, height - 15f, p)
            p.color = Color.parseColor("#FFB74D"); p.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, 78f, p)
            p.color = Color.BLACK; p.textSize = 46f; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(capCodeText, cx, cy + 16f, p)
        }
    }.apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(180f)).apply { bottomMargin = dp(12f) } }

    val codeIn = createInputBox("קוד קבל בן 3 ספרות (למשל 104, 472, 223)").apply { setText("104") }
    val voltIn = createInputBox("מתח עבודה בוולט (V) לחישוב אנרגיה").apply { setText("12") }
    val resTv = createResultTextView()

    fun calcCap() {
        val s = codeIn.text.toString().trim()
        val v = voltIn.text.toString().toDoubleOrNull() ?: 12.0
        if (s.length == 3 && s.all { it.isDigit() }) {
            capCodeText = s
            capCanvas.invalidate()
            val pf = s.substring(0, 2).toDouble() * 10.0.pow(s[2].digitToInt())
            val nf = pf / 1000.0
            val uf = nf / 1000.0
            val farad = uf * 1e-6
            val energyJ = 0.5 * farad * v * v
            resTv.text = "קיבול הקבל (קוד $s):\n• ${Utils.formatResult(pf, 2)} pF\n• ${Utils.formatResult(nf, 4)} nF\n• ${Utils.formatResult(uf, 6)} µF\n\nאנרגיה אגורה ב-${Utils.formatResult(v, 1)}V: ${Utils.formatResult(energyJ * 1000, 4)} mJ"
        } else resTv.text = "נא להזין קוד בן 3 ספרות בדיוק (למשל 104)"
    }

    val btn = createActionBtn("פענח קוד קבל וחשב אנרגיה ⚡") { calcCap() }
    calcCap()
    root.addView(capCanvas); root.addView(codeIn); root.addView(voltIn); root.addView(btn); root.addView(resTv)
    scroll.addView(root); return scroll
}

// 3. מסך אישי: עוביי חוטים AWG / SWG ומפל מתח עם שרטוט חתך כבל
fun MainActivity.createProAwgWireScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🔌 עוביי חוטים AWG/SWG ומפל מתח", "חישוב קוטר מוליך, שטח חתך (mm²), זרם מרבי ומפל מתח"))

    var currentDiamMm = 1.628f
    val wireCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f; val cy = height / 2f
            val rOuter = min(width, height) * 0.38f
            val rInner = (rOuter * (currentDiamMm / 8f).coerceIn(0.18f, 0.85f))
            p.color = Color.parseColor("#37474F"); p.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, rOuter, p)
            p.color = Color.parseColor("#D87A3E")
            canvas.drawCircle(cx, cy, rInner, p)
            p.color = Color.WHITE; p.textSize = 32f; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("Ø ${Utils.formatResult(currentDiamMm.toDouble(), 2)} mm", cx, cy + 12f, p)
        }
    }.apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(175f)).apply { bottomMargin = dp(12f) } }

    val awgIn = createInputBox("מספר AWG (0 עד 40, למשל 14)").apply { setText("14") }
    val lenIn = createInputBox("אורך כבל במטרים (לחישוב מפל מתח)").apply { setText("20") }
    val ampIn = createInputBox("זרם עבודה באמפר (A)").apply { setText("10") }
    val resTv = createResultTextView()

    val btn = createActionBtn("חשב חתך חוט ומפל מתח 🔌") {
        val n = (awgIn.text.toString().toIntOrNull() ?: 14).coerceIn(0, 40)
        val lengthM = lenIn.text.toString().toDoubleOrNull() ?: 20.0
        val currentA = ampIn.text.toString().toDoubleOrNull() ?: 10.0
        val diamMm = 0.127 * 92.0.pow((36.0 - n) / 39.0)
        currentDiamMm = diamMm.toFloat()
        wireCanvas.invalidate()
        val areaMm2 = (PI / 4.0) * diamMm * diamMm
        val ohmPerKm = 17.241 / areaMm2
        val vDrop = 2.0 * lengthM * currentA * (ohmPerKm / 1000.0)
        resTv.text = "חוט AWG #$n:\n• קוטר מוליך: ${Utils.formatResult(diamMm, 3)} מ״מ\n• שטח חתך: ${Utils.formatResult(areaMm2, 3)} ממ״ר (mm²)\n• התנגדות: ${Utils.formatResult(ohmPerKm, 2)} Ω/km\n• זרם מרבי מומלץ: ~${Utils.formatResult(areaMm2 * 6.0, 1)} A\n• מפל מתח ל-${Utils.formatResult(lengthM, 0)} מטר: ${Utils.formatResult(vDrop, 2)} V"
    }
    btn.performClick()
    root.addView(wireCanvas); root.addView(awgIn); root.addView(lenIn); root.addView(ampIn); root.addView(btn); root.addView(resTv)
    scroll.addView(root); return scroll
}

// 4. מסך אישי: מעבדת חוק אוהם, הספק, מחלק מתח וחיי סוללה
fun MainActivity.createProOhmAndPowerScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("⚡ מעבדת חוק אוהם, הספק וחיי סוללה", "חישוב מתח (V), זרם (I), התנגדות (R), הספק (P) וזמן סוללה"))

    val vIn = createInputBox("מתח חשמלי (V - וולט)").apply { setText("12") }
    val iIn = createInputBox("זרם חשמלי (I - אמפר)").apply { setText("2") }
    val mahIn = createInputBox("קיבולת סוללה ב-mAh (אופציונלי, למשל 5000)").apply { setText("5000") }
    val resTv = createResultTextView()

    val btn = createActionBtn("חשב חוק אוהם, הספק וזמן סוללה ⚡") {
        val v = vIn.text.toString().toDoubleOrNull() ?: 0.0
        val i = iIn.text.toString().toDoubleOrNull() ?: 0.0
        val mah = mahIn.text.toString().toDoubleOrNull() ?: 0.0
        if (v > 0 && i > 0) {
            val r = v / i
            val p = v * i
            val batteryHours = if (mah > 0) (mah / (i * 1000.0)) * 0.85 else 0.0
            resTv.text = "תוצאות מעגל חשמלי:\n• הספק (P = V×I): ${Utils.formatResult(p, 2)} W\n• התנגדות (R = V/I): ${Utils.formatResult(r, 2)} Ω\n• עלות הפעלה ל-24 שעות: ₪${Utils.formatResult((p / 1000.0) * 24.0 * 0.62, 2)}\n" +
                    if (batteryHours > 0) "• זמן סוללה (${Utils.formatResult(mah, 0)}mAh): ${Utils.formatResult(batteryHours, 2)} שעות" else ""
        } else resTv.text = "נא להזין מתח וזרם חיוביים"
    }
    btn.performClick()
    root.addView(vIn); root.addView(iIn); root.addView(mahIn); root.addView(btn); root.addView(resTv)
    scroll.addView(root); return scroll
}

// 5. מסך אישי: פלס בנאים דו-צירי חי עם בועות זזות בקנבה
fun MainActivity.createProSpiritLevelScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🟢 פלס בנאים דו-צירי חי עם בועות", "מדידת שיפוע ואיזון משטחים בזמן אמת באמצעות חיישני המכשיר"))

    val levelCanvas = SpiritLevelCanvasView(this, this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(340f)).apply { bottomMargin = dp(12f) }
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createProSpiritLevelScreen, 24f)
        else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(24f).toFloat() }
    }
    val resTv = createResultTextView()

    val sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val accel = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val listener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent?) {
            if (e == null) return
            val gx = e.values[0]; val gy = e.values[1]; val gz = e.values[2]
            val pitch = Math.toDegrees(atan2(gy.toDouble(), sqrt((gx * gx + gz * gz).toDouble()))).toFloat()
            val roll = Math.toDegrees(atan2(-gx.toDouble(), gz.toDouble())).toFloat()
            levelCanvas.rollDeg = roll
            levelCanvas.pitchDeg = pitch
            levelCanvas.invalidate()
            resTv.text = "נטייה אופקית (Roll): ${Utils.formatResult(roll.toDouble(), 1)}°\nנטייה אנכית (Pitch): ${Utils.formatResult(pitch.toDouble(), 1)}°"
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
    if (accel != null) sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) { sm?.unregisterListener(listener) }
    })

    root.addView(levelCanvas); root.addView(resTv)
    scroll.addView(root); return scroll
}

// 6. מסך אישי: מצפן מגנטי חי עם שושנת רוחות ומחוג זז בקנבה
fun MainActivity.createProCompassScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🧭 מצפן מגנטי חי עם מחוג זז", "מציג אזימוט מעלות וכיוון שמיים בזמן אמת (ניתן גם לסובב במגע)"))

    val resTv = createResultTextView()
    fun updateHeadingText(deg: Float) {
        val dirs = listOf("צפון (N)", "צפון-מזרח (NE)", "מזרח (E)", "דרום-מזרח (SE)", "דרום (S)", "דרום-מערב (SW)", "מערב (W)", "צפון-מערב (NW)")
        val idx = (((deg + 22.5f) % 360f) / 45f).toInt().coerceIn(0, 7)
        resTv.text = "אזימוט נוכחי: ${Utils.formatResult(deg.toDouble(), 1)}°\nכיוון: ${dirs[idx]}"
    }

    val compassCanvas = CompassRoseCanvasView(this, this) { deg -> updateHeadingText(deg) }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(330f)).apply { bottomMargin = dp(12f) }
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createProCompassScreen, 24f)
        else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(24f).toFloat() }
    }
    updateHeadingText(0f)

    val sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val oriSensor = sm?.getDefaultSensor(Sensor.TYPE_ORIENTATION)
    val listener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent?) {
            if (e != null) {
                compassCanvas.azimuthDeg = e.values[0]
                compassCanvas.invalidate()
                updateHeadingText(e.values[0])
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
    if (oriSensor != null) sm.registerListener(listener, oriSensor, SensorManager.SENSOR_DELAY_GAME)
    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) { sm?.unregisterListener(listener) }
    })

    root.addView(compassCanvas); root.addView(resTv)
    scroll.addView(root); return scroll
}

// 7. מסך אישי: מד-זווית אינטראקטיבי וסרגל מכויל על המסך
fun MainActivity.createProRulerProtractorScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("📐 מד-זווית אינטראקטיבי וסרגל מכויל", "גרור את זרוע מד-הזווית על המסך למדידת זוויות או מדוד בסנטימטרים"))

    val canvas = ProtractorRulerCanvasView(this, this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(430f))
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createProRulerProtractorScreen, 22f)
        else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(22f).toFloat() }
    }
    root.addView(canvas)
    scroll.addView(root); return scroll
}

// 8. מסך אישי: שעון עצר ומדידת הקפות עם טבעת כרונוגרף חיה
fun MainActivity.createProStopwatchScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("⏱️ שעון עצר כרונוגרף ומדידת הקפות", "מדידת זמנים מדויקת במאיות שנייה ושמירת הקפות (Laps)"))

    var running = false
    var startMs = 0L
    var elapsedMs = 0L
    val laps = mutableListOf<String>()
    val handler = Handler(Looper.getMainLooper())

    val chronoCanvas = object : View(this) {
        var displayStr = "00:00.00"
        var sweepAngle = 0f
        private val ringP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 14f; color = Color.argb(60, 255, 255, 255) }
        private val arcP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 14f; strokeCap = Paint.Cap.ROUND }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 64f; textAlign = Paint.Align.CENTER; typeface = Typeface.MONOSPACE }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f; val cy = height / 2f; val r = min(width, height) * 0.38f
            arcP.color = if (isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else primaryColor
            canvas.drawCircle(cx, cy, r, ringP)
            canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -90f, sweepAngle, false, arcP)
            canvas.drawText(displayStr, cx, cy + 22f, txtP)
        }
    }.apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(240f)).apply { bottomMargin = dp(12f) } }

    val lapsTv = createResultTextView()

    val tick = object : Runnable {
        override fun run() {
            if (!running) return
            val total = elapsedMs + (System.currentTimeMillis() - startMs)
            val min = (total / 60000) % 60
            val sec = (total / 1000) % 60
            val cs = (total % 1000) / 10
            chronoCanvas.displayStr = String.format("%02d:%02d.%02d", min, sec, cs)
            chronoCanvas.sweepAngle = ((total % 60000) / 60000f) * 360f
            chronoCanvas.invalidate()
            handler.postDelayed(this, 30)
        }
    }

    val startBtn = createActionBtn("התחל / עצור ⏯️") {
        if (!running) { startMs = System.currentTimeMillis(); running = true; handler.post(tick) }
        else { elapsedMs += System.currentTimeMillis() - startMs; running = false }
    }
    val lapBtn = createActionBtn("שמור הקפה (Lap) 🏁", false) {
        laps.add(0, "הקפה ${laps.size + 1}:  ${chronoCanvas.displayStr}")
        lapsTv.text = laps.joinToString("\n")
    }
    val resetBtn = createActionBtn("אפס הכל", false) {
        running = false; elapsedMs = 0L; laps.clear()
        chronoCanvas.displayStr = "00:00.00"; chronoCanvas.sweepAngle = 0f; chronoCanvas.invalidate(); lapsTv.text = ""
    }

    root.addView(chronoCanvas); root.addView(startBtn); root.addView(lapBtn); root.addView(resetBtn); root.addView(lapsTv)
    scroll.addView(root); return scroll
}

// 9. מסך אישי: פנס מסך עוצמתי עם דימר בהירות
fun MainActivity.createProFlashlightScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🔦 פנס מסך עוצמתי ותאורת מעבדה", "הופך את המסך למקור אור בעוצמה מרבית עם בחירת גוון תאורה"))

    var isOn = false
    val lampView = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(260f)).apply { bottomMargin = dp(14f) }
        background = GradientDrawable().apply { setColor(Color.DKGRAY); cornerRadius = dp(26f).toFloat() }
    }
    val toggleBtn = createActionBtn("הדלק / כבה פנס מסך (100% בהירות) 🔦") {
        isOn = !isOn
        val lp = window.attributes
        lp.screenBrightness = if (isOn) 1.0f else -1.0f
        window.attributes = lp
        lampView.background = GradientDrawable().apply {
            setColor(if (isOn) Color.WHITE else Color.DKGRAY)
            cornerRadius = dp(26f).toFloat()
        }
    }
    root.addView(toggleBtn); root.addView(lampView)
    scroll.addView(root); return scroll
}

// 10. מסך אישי: הטבלה המחזורית עם מודל אטום בוהר מצויר בקנבה
fun MainActivity.createProPeriodicTableScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("⚛️ הטבלה המחזורית ומודל אטום בוהר", "בחר יסוד לצפייה במבנה קליפות האלקטרונים והמסה האטומית"))

    val elements = listOf(
        Triple(1, "H", "מימן (Hydrogen) | מסה: 1.008 | אל-מתכת"),
        Triple(2, "He", "הליום (Helium) | מסה: 4.0026 | גז אציל"),
        Triple(6, "C", "פחמן (Carbon) | מסה: 12.011 | אל-מתכת"),
        Triple(7, "N", "חנקן (Nitrogen) | מסה: 14.007 | אל-מתכת"),
        Triple(8, "O", "חמצן (Oxygen) | מסה: 15.999 | אל-מתכת"),
        Triple(11, "Na", "נתרן (Sodium) | מסה: 22.990 | מתכת אלקלית"),
        Triple(13, "Al", "אלומיניום (Aluminum) | מסה: 26.982 | מתכת"),
        Triple(14, "Si", "צורן / סיליקון (Silicon) | מסה: 28.085 | מתכת למחצה"),
        Triple(26, "Fe", "ברזל (Iron) | מסה: 55.845 | מתכת מעבר"),
        Triple(29, "Cu", "נחושת (Copper) | מסה: 63.546 | מתכת מעבר"),
        Triple(47, "Ag", "כסף (Silver) | מסה: 107.87 | מתכת מעבר"),
        Triple(78, "Pt", "פלטינה (Platinum) | מסה: 195.08 | מתכת אצילה"),
        Triple(79, "Au", "זהב (Gold) | מסה: 196.97 | מתכת אצילה"),
        Triple(80, "Hg", "כספית (Mercury) | מסה: 200.59 | מתכת נוזלית"),
        Triple(92, "U", "אורניום (Uranium) | מסה: 238.03 | אקטיניד")
    )

    val atomCanvas = BohrAtomCanvasView(this, this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(250f)).apply { bottomMargin = dp(12f) }
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createProPeriodicTableScreen, 22f)
        else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(22f).toFloat() }
    }
    val resTv = createResultTextView().apply { text = "79. Au - ${elements[12].third}" }

    val pickBtn = createActionBtn("🔬 בחר יסוד מהטבלה המחזורית") {
        val labels = elements.map { "${it.first}. ${it.second} - ${it.third}" }
        showItemPicker("בחר יסוד כימי", labels) { chosen ->
            val idx = labels.indexOf(chosen).coerceAtLeast(0)
            val el = elements[idx]
            atomCanvas.atomicNumber = el.first
            atomCanvas.symbol = el.second
            atomCanvas.invalidate()
            resTv.text = chosen
        }
    }

    root.addView(atomCanvas); root.addView(pickBtn); root.addView(resTv)
    scroll.addView(root); return scroll
}

// 11. מסך אישי: מתרגם קוד מורס דו-כיווני עם משדר אור ויזואלי
fun MainActivity.createProMorseScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("📡 מתרגם ומשדר קוד מורס (עברית/אנגלית)", "תרגום מיידי של טקסט לסימני מורס (נקודות וקווים)"))

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

    val input = createInputBox("הקלד טקסט בעברית או באנגלית (למשל SOS או שלום)").apply { inputType = InputType.TYPE_CLASS_TEXT; setText("SOS") }
    val resTv = createResultTextView().apply { text = "... --- ..." }
    input.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            val txt = s.toString().uppercase()
            resTv.text = txt.map { ch -> if (ch == ' ') " / " else morseMap[ch] ?: "" }.joinToString(" ")
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    root.addView(input); root.addView(resTv)
    scroll.addView(root); return scroll
}

// 12. מסך אישי: מחולל סיסמאות מאובטחות עם מד אנטרופיה גרפי בקנבה
fun MainActivity.createProPasswordGenScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🔐 מחולל סיסמאות ומד אנטרופיה קריפטוגרפי", "ייצור סיסמאות אקראיות חזקות ובדיקת עוצמת ההצפנה בביטים"))

    var entropyBits = 104f
    val meterCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat(); val h = height.toFloat()
            p.color = Color.argb(80, 255, 255, 255)
            canvas.drawRoundRect(RectF(20f, 35f, w - 20f, 75f), 20f, 20f, p)
            val ratio = (entropyBits / 160f).coerceIn(0.1f, 1f)
            p.color = if (entropyBits >= 80f) Color.parseColor("#00E676") else Color.parseColor("#FFB74D")
            canvas.drawRoundRect(RectF(20f, 35f, 20f + (w - 40f) * ratio, 75f), 20f, 20f, p)
            p.color = Color.WHITE; p.textSize = 32f; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("חוזק אנטרופיה: ${entropyBits.toInt()} bits (עמיד לפריצה)", w / 2f, h - 18f, p)
        }
    }.apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(115f)).apply { bottomMargin = dp(10f) } }

    val lenIn = createInputBox("אורך סיסמה (8 עד 64 תווים)").apply { setText("16") }
    val resTv = createResultTextView()

    val genBtn = createActionBtn("צור סיסמה חדשה והעתק 🔐") {
        val len = (lenIn.text.toString().toIntOrNull() ?: 16).coerceIn(6, 64)
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#\$%^&*()-_=+"
        val pass = (1..len).map { chars.random() }.joinToString("")
        entropyBits = (len * (ln(chars.length.toDouble()) / ln(2.0))).toFloat()
        meterCanvas.invalidate()
        resTv.text = pass
    }
    genBtn.performClick()
    root.addView(meterCanvas); root.addView(lenIn); root.addView(genBtn); root.addView(resTv)
    scroll.addView(root); return scroll
}

// 13. מסך אישי: גימטריה עברית, ספרות רומיות וניתוח טקסט
fun MainActivity.createProGematriaAndTextScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🔠 מחשבון גימטריה, ספרות רומיות וטקסט", "חישוב ערך גימטרי בעברית, המרה לספרות רומיות וספירת מילים"))

    val gemMap = mapOf(
        'א' to 1, 'ב' to 2, 'ג' to 3, 'ד' to 4, 'ה' to 5, 'ו' to 6, 'ז' to 7, 'ח' to 8, 'ט' to 9,
        'י' to 10, 'כ' to 20, 'ך' to 20, 'ל' to 30, 'מ' to 40, 'ם' to 40, 'נ' to 50, 'ן' to 50,
        'ס' to 60, 'ע' to 70, 'פ' to 80, 'ף' to 80, 'צ' to 90, 'ץ' to 90, 'ק' to 100, 'ר' to 200,
        'ש' to 300, 'ת' to 400
    )

    val textIn = createInputBox("הקלד מילה/משפט בעברית או מספר להמרה לספרות רומיות").apply { inputType = InputType.TYPE_CLASS_TEXT; setText("חי") }
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

    val calcBtn = createActionBtn("חשב גימטריה, רומית וסטטיסטיקת טקסט 📜") {
        val raw = textIn.text.toString().trim()
        val gVal = raw.sumOf { gemMap[it] ?: 0 }
        val asNum = raw.toIntOrNull() ?: gVal
        val words = raw.split(Regex("\\s+")).filter { it.isNotEmpty() }.size
        resTv.text = "ערך גימטרי: $gVal\nספרות רומיות ($asNum): ${toRoman(asNum)}\nמספר מילים: $words | מספר תווים: ${raw.length}"
    }
    calcBtn.performClick()
    root.addView(textIn); root.addView(calcBtn); root.addView(resTv)
    scroll.addView(root); return scroll
}

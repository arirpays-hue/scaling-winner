package com.example.calculator

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfDocument
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Base64
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

// =========================================================================
// 1. גלאי מתכות ושדה מגנטי חי (Metal Detector & EMF Gaussmeter - Tab 66)
// =========================================================================
class MetalDetectorRadarCanvasView(context: Context, private val activity: MainActivity) : View(context) {
    var currentUt: Float = 46.0f
    var baselineUt: Float = 46.0f
    var peakUt: Float = 46.0f
    private var pulsePhase: Float = 0f

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val isMin = activity.isMinimalistMode
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h * 0.54f
        val maxR = min(w, h) * 0.36f

        val delta = (currentUt - baselineUt).coerceAtLeast(0f)
        val alertLevel = when {
            delta >= 45f || currentUt >= 100f -> 2
            delta >= 15f || currentUt >= 68f -> 1
            else -> 0
        }

        val statusColor = when (alertLevel) {
            2 -> Color.parseColor("#FF3B30")
            1 -> Color.parseColor("#FF9F0A")
            else -> if (isMin) Color.parseColor("#2E7D32") else Color.parseColor("#00E676")
        }

        for (i in 1..3) {
            val r = maxR * (0.30f * i + (pulsePhase % 0.30f))
            val alpha = ((1f - (r / (maxR * 1.15f))).coerceIn(0f, 1f) * 140).toInt()
            ringPaint.color = Color.argb(alpha, Color.red(statusColor), Color.green(statusColor), Color.blue(statusColor))
            ringPaint.strokeWidth = 2.2f * d
            canvas.drawCircle(cx, cy, r, ringPaint)
        }

        val arcRect = RectF(cx - maxR, cy - maxR, cx + maxR, cy + maxR)
        ringPaint.strokeWidth = 8f * d
        ringPaint.color = if (isMin) Color.argb(35, 44, 44, 44) else Color.argb(45, 255, 255, 255)
        canvas.drawArc(arcRect, 180f, 180f, false, ringPaint)

        val sweep = ((currentUt / 200f).coerceIn(0.03f, 1f)) * 180f
        ringPaint.color = statusColor
        canvas.drawArc(arcRect, 180f, sweep, false, ringPaint)

        val angleRad = Math.toRadians((180f + sweep).toDouble())
        needlePaint.strokeWidth = 3.5f * d
        needlePaint.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        val nx = (cx + cos(angleRad) * (maxR * 0.86f)).toFloat()
        val ny = (cy + sin(angleRad) * (maxR * 0.86f)).toFloat()
        canvas.drawLine(cx, cy, nx, ny, needlePaint)
        fillPaint.color = statusColor
        canvas.drawCircle(cx, cy, 6.5f * d, fillPaint)

        textPaint.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        val mainStr = "\u200E${Utils.formatResult(currentUt.toDouble(), 1)} µT\u200E  (Δ +\u200E${Utils.formatResult(delta.toDouble(), 1)} µT\u200E)"
        CanvasUiHelper.drawFittedText(canvas, mainStr, cx, cy + 28f * d, w - 28f * d, 18f * d, 11f * d, textPaint)

        textPaint.color = statusColor
        val badgeText = when (alertLevel) {
            2 -> "🔴 זוהתה מתכת / שדה מגנטי חזק מאוד!"
            1 -> "🟡 זוהתה התקרבות לעצם מתכתי / מגנטי"
            else -> "🟢 שדה מגנטי סביבתי תקין (קרב למתכת לבדיקה)"
        }
        CanvasUiHelper.drawFittedText(canvas, badgeText, cx, 24f * d, w - 24f * d, 13.5f * d, 9.5f * d, textPaint)

        pulsePhase = (pulsePhase + (if (alertLevel == 2) 0.025f else if (alertLevel == 1) 0.015f else 0.007f)) % 1f
        if (isAttachedToWindow) postInvalidateOnAnimation()
    }
}

fun MainActivity.createProMetalDetectorScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🧲 גלאי מתכות ושדה מגנטי (EMF)", "מדידת שדה מגנטי ב-µT/mG, איפוס רקע (Tare) והתראת צפצוף בקרבת מתכת"))

    var soundBeepEnabled = true
    var lastBeepMs = 0L
    var toneGen: ToneGenerator? = try { ToneGenerator(AudioManager.STREAM_MUSIC, 90) } catch (_: Exception) { null }

    val radarCanvas = MetalDetectorRadarCanvasView(this, this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(255f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProMetalDetectorScreen, 22f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProMetalDetectorScreen, 22f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(22f).toFloat() }
        }
    }
    val resTv = createResultTextView().apply { textSize = 16.5f }

    val tareBtn = createActionBtn("🎯 אפס שדה רקע נוכחי (Tare Baseline)", true) {
        radarCanvas.baselineUt = radarCanvas.currentUt
        radarCanvas.peakUt = radarCanvas.currentUt
        radarCanvas.invalidate()
        Toast.makeText(this, "שדה הרקע אופס ל-${Utils.formatResult(radarCanvas.baselineUt.toDouble(), 1)} µT", Toast.LENGTH_SHORT).show()
    }

    val soundBtn = createActionBtn("🔊 צפצוף התראה בקרבת מתכת: מופעל 🟢", false) {
        soundBeepEnabled = !soundBeepEnabled
        (it as TextView).text = if (soundBeepEnabled) "🔊 צפצוף התראה בקרבת מתכת: מופעל 🟢" else "🔇 צפצוף התראה בקרבת מתכת: כבוי 🔴"
    }

    val sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val magSensor = sm?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    var hasInitBaseline = false

    val listener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent?) {
            if (e == null) return
            val bx = e.values[0]; val by = e.values[1]; val bz = e.values[2]
            val ut = sqrt(bx * bx + by * by + bz * bz)
            if (!hasInitBaseline) {
                radarCanvas.baselineUt = ut
                hasInitBaseline = true
            }
            radarCanvas.currentUt = ut
            if (ut > radarCanvas.peakUt) radarCanvas.peakUt = ut
            val milliGauss = ut * 10f
            val delta = (ut - radarCanvas.baselineUt).coerceAtLeast(0f)

            resTv.text = "עוצמת שדה מגנטי כוללת: \u200E${Utils.formatResult(ut.toDouble(), 2)} µT\u200E (\u200E${Utils.formatResult(milliGauss.toDouble(), 0)} mG\u200E)\n" +
                    "• חריגה מעל שדה הרקע: \u200E+${Utils.formatResult(delta.toDouble(), 2)} µT\u200E | שיא: \u200E${Utils.formatResult(radarCanvas.peakUt.toDouble(), 1)} µT\u200E\n" +
                    "• רכיבי צירים: X=\u200E${Utils.formatResult(bx.toDouble(), 1)}\u200E | Y=\u200E${Utils.formatResult(by.toDouble(), 1)}\u200E | Z=\u200E${Utils.formatResult(bz.toDouble(), 1)}\u200E"

            if (soundBeepEnabled && delta >= 15f) {
                val now = System.currentTimeMillis()
                val interval = (550L - (delta * 5f).toLong()).coerceIn(110L, 550L)
                if (now - lastBeepMs >= interval) {
                    lastBeepMs = now
                    try { toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 55) } catch (_: Exception) {}
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    if (magSensor != null) {
        sm.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_UI)
    } else {
        resTv.text = "לא זוהה חיישן מגנטומטר במכשיר זה"
    }

    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) {
            sm?.unregisterListener(listener)
            try { toneGen?.release() } catch (_: Exception) {}
            toneGen = null
        }
    })

    root.addView(radarCanvas); root.addView(tareBtn); root.addView(soundBtn)
    root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 2. מד רטט וסיסמוגרף חי (Vibrometer & Seismograph - Tab 67)
// =========================================================================
class SeismographWaveCanvasView(context: Context, private val activity: MainActivity) : View(context) {
    val samples = FloatArray(90) { 0f }
    var writeIdx = 0
    var peakVibrationMs2: Float = 0f
    var currentVibrationMs2: Float = 0f

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

    fun pushSample(v: Float) {
        samples[writeIdx] = v
        writeIdx = (writeIdx + 1) % samples.size
        currentVibrationMs2 = v
        if (v > peakVibrationMs2) peakVibrationMs2 = v
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val isMin = activity.isMinimalistMode
        val w = width.toFloat()
        val h = height.toFloat()
        val midY = h * 0.52f

        gridPaint.strokeWidth = 1f * d
        gridPaint.color = if (isMin) Color.argb(32, 44, 44, 44) else Color.argb(40, 255, 255, 255)
        canvas.drawLine(16f * d, midY, w - 16f * d, midY, gridPaint)
        canvas.drawLine(16f * d, h * 0.25f, w - 16f * d, h * 0.25f, gridPaint)
        canvas.drawLine(16f * d, h * 0.78f, w - 16f * d, h * 0.78f, gridPaint)

        val accent = when {
            currentVibrationMs2 > 2.5f -> Color.parseColor("#FF3B30")
            currentVibrationMs2 > 0.6f -> Color.parseColor("#FF9F0A")
            isMin -> MinimalistTheme.TEXT_CHARCOAL
            else -> CanvasUiHelper.ensureReadableAccent(activity.primaryColor, activity.isAuroraMode)
        }
        wavePaint.color = accent
        wavePaint.strokeWidth = 2.4f * d

        val path = Path()
        val usableW = w - 32f * d
        val maxAmp = h * 0.34f
        for (i in samples.indices) {
            val idx = (writeIdx + i) % samples.size
            val s = samples[idx]
            val sign = if (i % 2 == 0) 1f else -1f
            val norm = (s / 8.0f).coerceIn(0f, 1f)
            val x = 16f * d + (i.toFloat() / (samples.size - 1)) * usableW
            val y = midY + sign * norm * maxAmp
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, wavePaint)

        textPaint.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        val header = "רעידה כעת: \u200E${Utils.formatResult(currentVibrationMs2.toDouble(), 2)} m/s²\u200E  |  שיא: \u200E${Utils.formatResult(peakVibrationMs2.toDouble(), 2)} m/s²\u200E"
        CanvasUiHelper.drawFittedText(canvas, header, w / 2f, 20f * d, w - 24f * d, 13.5f * d, 9.5f * d, textPaint)
    }
}

fun MainActivity.createProVibrometerScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("📈 מד רטט וסיסמוגרף חי (Vibrometer)", "ניתוח רעידות משטח בתלת-ממד, גרף סיסמוגרף בזמן אמת וסולם עוצמת רעש/רטט"))

    var isPaused = false
    val seismoCanvas = SeismographWaveCanvasView(this, this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(220f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProVibrometerScreen, 22f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProVibrometerScreen, 22f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(22f).toFloat() }
        }
    }
    val resTv = createResultTextView().apply { textSize = 16f }

    val resetBtn = createActionBtn("🔄 אפס שיא מדידה וגרף סיסמוגרף", true) {
        seismoCanvas.peakVibrationMs2 = 0f
        seismoCanvas.samples.fill(0f)
        seismoCanvas.invalidate()
    }
    val pauseBtn = createActionBtn("⏸️ הקפא / המשך דגימת רטט", false) {
        isPaused = !isPaused
        (it as TextView).text = if (isPaused) "▶️ המשך דגימת רטט" else "⏸️ הקפא / המשך דגימת רטט"
    }

    val sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val accel = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val grav = FloatArray(3) { 0f }
    var initialized = false

    val listener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent?) {
            if (e == null || isPaused) return
            if (!initialized) {
                grav[0] = e.values[0]; grav[1] = e.values[1]; grav[2] = e.values[2]
                initialized = true
            }
            val alpha = 0.82f
            grav[0] = alpha * grav[0] + (1 - alpha) * e.values[0]
            grav[1] = alpha * grav[1] + (1 - alpha) * e.values[1]
            grav[2] = alpha * grav[2] + (1 - alpha) * e.values[2]

            val lx = e.values[0] - grav[0]
            val ly = e.values[1] - grav[1]
            val lz = e.values[2] - grav[2]
            val mag = sqrt(lx * lx + ly * ly + lz * lz)
            seismoCanvas.pushSample(mag)

            val peak = seismoCanvas.peakVibrationMs2
            val mercalliDesc = when {
                peak < 0.15f -> "I - יציב ושקט לחלוטין (ללא רטט מורגש)"
                peak < 0.60f -> "II-III - רטט קל מאוד (מאוורר / מנוע מרוחק)"
                peak < 1.80f -> "IV-V - רטט בינוני מורגש היטב במשטח"
                peak < 4.50f -> "VI-VII - רטט חזק (מכונה כבדה / טלטול)"
                else -> "VIII+ - רעידה עזה מאוד!"
            }
            resTv.text = "ניתוח סיסמוגרף חי:\n" +
                    "• תאוצת רטט רגעית: \u200E${Utils.formatResult(mag.toDouble(), 3)} m/s²\u200E\n" +
                    "• שיא רעידה שנרשם (Peak PGA): \u200E${Utils.formatResult(peak.toDouble(), 3)} m/s²\u200E\n" +
                    "• סיווג עוצמה (סולם מרקאלי): $mercalliDesc"
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    if (accel != null) sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) { sm?.unregisterListener(listener) }
    })

    root.addView(seismoCanvas); root.addView(resetBtn); root.addView(pauseBtn)
    root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 3. צופר חירום והתרעה קולית (Emergency Siren & Strobe - Tab 68)
// =========================================================================
fun MainActivity.createProEmergencySirenScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🚨 צופר חירום והתרעה קולית (Siren)", "צופר חירום עוצמתי עם 4 דפוסי סירנה (משטרה, אמבולנס, אזעקה ו-SOS) והבהוב חירום"))

    var isSirenPlaying = false
    var sirenMode = 0
    var beamAngle = 0f
    var strobePhase = false
    val handler = Handler(Looper.getMainLooper())
    var toneGen: ToneGenerator? = try { ToneGenerator(AudioManager.STREAM_ALARM, 100) } catch (_: Exception) { null }

    val beaconCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val cx = width / 2f; val cy = height * 0.50f
            val r = min(width, height) * 0.34f

            if (isSirenPlaying) {
                val col1 = if (strobePhase) Color.parseColor("#FF1744") else Color.parseColor("#2979FF")
                p.style = Paint.Style.FILL
                p.color = Color.argb(85, Color.red(col1), Color.green(col1), Color.blue(col1))
                canvas.save()
                canvas.rotate(beamAngle, cx, cy)
                canvas.drawArc(RectF(cx - r * 1.25f, cy - r * 1.25f, cx + r * 1.25f, cy + r * 1.25f), -35f, 70f, true, p)
                canvas.drawArc(RectF(cx - r * 1.25f, cy - r * 1.25f, cx + r * 1.25f, cy + r * 1.25f), 145f, 70f, true, p)
                canvas.restore()
                beamAngle = (beamAngle + 8f) % 360f
                postInvalidateOnAnimation()
            }

            p.style = Paint.Style.FILL
            p.color = if (isSirenPlaying) {
                if (strobePhase) Color.parseColor("#FF1744") else Color.parseColor("#2979FF")
            } else if (isMinimalistMode) MinimalistTheme.ACCENT_TERRACOTTA else Color.parseColor("#455A64")
            canvas.drawCircle(cx, cy, r * 0.62f, p)

            p.style = Paint.Style.STROKE
            p.strokeWidth = 2.5f * d
            p.color = if (isMinimalistMode) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            canvas.drawCircle(cx, cy, r * 0.62f, p)

            p.style = Paint.Style.FILL
            p.textAlign = Paint.Align.CENTER
            p.typeface = Typeface.DEFAULT_BOLD
            p.textSize = 15f * d
            canvas.drawText(if (isSirenPlaying) "🚨 צופר פועל!" else "מוכן להפעלה", cx, height - 14f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(220f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProEmergencySirenScreen, 22f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProEmergencySirenScreen, 22f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(22f).toFloat() }
        }
    }

    val modeNames = listOf("🚓 סירנת משטרה דו-טונית (Hi-Lo)", "🚑 סירנת אמבולנס מהירה (Yelp)", "⚠️ אזעקת חירום רציפה", "🆘 צפצוף מצוקה במורס (SOS)")
    val modeBtn = createActionBtn("סוג צופר: ${modeNames[0]}", false) {}
    lateinit var triggerBtn: TextView

    val sirenRunner = object : Runnable {
        override fun run() {
            if (!isSirenPlaying) return
            strobePhase = !strobePhase
            beaconCanvas.invalidate()
            val tone = when (sirenMode) {
                0 -> if (strobePhase) ToneGenerator.TONE_DTMF_D else ToneGenerator.TONE_DTMF_1
                1 -> if (strobePhase) ToneGenerator.TONE_DTMF_A else ToneGenerator.TONE_DTMF_6
                2 -> ToneGenerator.TONE_CDMA_HIGH_L
                else -> ToneGenerator.TONE_DTMF_S
            }
            val dur = if (sirenMode == 1) 180 else 340
            try { toneGen?.startTone(tone, dur) } catch (_: Exception) {}
            handler.postDelayed(this, dur.toLong() + 30L)
        }
    }

    triggerBtn = createActionBtn("🚨 הפעל / כבה צופר חירום עכשיו!", true) {
        isSirenPlaying = !isSirenPlaying
        if (isSirenPlaying) {
            triggerBtn.text = "⏹️ עצור צופר חירום (פועל כעת)"
            handler.post(sirenRunner)
        } else {
            triggerBtn.text = "🚨 הפעל / כבה צופר חירום עכשיו!"
            handler.removeCallbacksAndMessages(null)
            try { toneGen?.stopTone() } catch (_: Exception) {}
            beaconCanvas.invalidate()
        }
    }

    modeBtn.setOnClickListener {
        showItemPicker("בחר דפוס סירנה", modeNames) { chosen ->
            sirenMode = modeNames.indexOf(chosen).coerceAtLeast(0)
            modeBtn.text = "סוג צופר: $chosen"
        }
    }

    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) {
            isSirenPlaying = false
            handler.removeCallbacksAndMessages(null)
            try { toneGen?.release() } catch (_: Exception) {}
            toneGen = null
        }
    })

    root.addView(beaconCanvas); root.addView(triggerBtn); root.addView(modeBtn)
    scroll.addView(root); return scroll
}

// =========================================================================
// 4. מטרונום מוזיקלי מונפש עם Tap Tempo (Metronome BPM - Tab 69)
// =========================================================================
fun MainActivity.createProMetronomeScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🎵 מטרונום מוזיקלי מונפש (BPM)", "מטוטלת קצב מדויקת, בחירת משקל (4/4, 3/4, 6/8) וכפתור Tap Tempo"))

    var bpm = 120
    var beatsPerMeasure = 4
    var currentBeat = 0
    var isPlaying = false
    var swingDir = 1f
    val handler = Handler(Looper.getMainLooper())
    var toneGen: ToneGenerator? = try { ToneGenerator(AudioManager.STREAM_MUSIC, 95) } catch (_: Exception) { null }
    val tapTimes = mutableListOf<Long>()

    val pendulumCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val pivotX = w / 2f; val pivotY = h * 0.80f
            val armLen = h * 0.56f

            val dotSpacing = 28f * d
            val startX = pivotX - ((beatsPerMeasure - 1) * dotSpacing) / 2f
            for (b in 0 until beatsPerMeasure) {
                val isCurrent = isPlaying && (b == currentBeat)
                p.style = Paint.Style.FILL
                p.color = when {
                    isCurrent && b == 0 -> Color.parseColor("#FF3B30")
                    isCurrent -> Color.parseColor("#00E676")
                    isMinimalistMode -> Color.argb(55, 44, 44, 44)
                    else -> Color.argb(65, 255, 255, 255)
                }
                canvas.drawCircle(startX + b * dotSpacing, 26f * d, if (isCurrent) 9f * d else 6.5f * d, p)
            }

            val angleDeg = if (isPlaying) (swingDir * 26f) else 0f
            val rad = Math.toRadians((angleDeg - 90f).toDouble())
            val tipX = (pivotX + cos(rad) * armLen).toFloat()
            val tipY = (pivotY + sin(rad) * armLen).toFloat()

            p.style = Paint.Style.STROKE
            p.strokeWidth = 3.5f * d
            p.color = if (isMinimalistMode) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            canvas.drawLine(pivotX, pivotY, tipX, tipY, p)

            p.style = Paint.Style.FILL
            p.color = if (isMinimalistMode) MinimalistTheme.ACCENT_TERRACOTTA else primaryColor
            canvas.drawCircle(tipX, tipY, 10f * d, p)
            canvas.drawCircle(pivotX, pivotY, 7f * d, p)

            p.color = if (isMinimalistMode) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD; p.textSize = 15f * d
            val tempoName = when {
                bpm < 60 -> "Largo (איטי מאוד)"
                bpm < 76 -> "Adagio (איטי)"
                bpm < 108 -> "Andante (הליכה)"
                bpm < 120 -> "Moderato (מתון)"
                bpm < 156 -> "Allegro (מהיר)"
                else -> "Presto (מהיר מאוד)"
            }
            canvas.drawText("$bpm BPM • $tempoName (${beatsPerMeasure}/4)", pivotX, h - 10f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(235f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProMetronomeScreen, 22f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProMetronomeScreen, 22f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(22f).toFloat() }
        }
    }

    val bpmIn = createInputBox("קצב בפעמות לדקה (BPM 30-240)").apply { setText("120") }
    val resTv = createResultTextView().apply { text = "120 BPM (0.50 שניות לפעמה)" }

    val beatRunner = object : Runnable {
        override fun run() {
            if (!isPlaying) return
            val isAccent = (currentBeat == 0)
            try {
                toneGen?.startTone(if (isAccent) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP, 35)
            } catch (_: Exception) {}
            swingDir = -swingDir
            pendulumCanvas.invalidate()
            currentBeat = (currentBeat + 1) % beatsPerMeasure
            val delayMs = (60000L / bpm.coerceIn(30, 240))
            handler.postDelayed(this, delayMs)
        }
    }

    val playBtn = createActionBtn("▶️ הפעל / עצור מטרונום", true) {
        bpm = (bpmIn.text.toString().toIntOrNull() ?: 120).coerceIn(30, 240)
        isPlaying = !isPlaying
        if (isPlaying) {
            currentBeat = 0
            (it as TextView).text = "⏹️ עצור מטרונום ($bpm BPM)"
            handler.post(beatRunner)
        } else {
            (it as TextView).text = "▶️ הפעל / עצור מטרונום"
            handler.removeCallbacksAndMessages(null)
            pendulumCanvas.invalidate()
        }
        resTv.text = "$bpm BPM (\u200E${Utils.formatResult(60.0 / bpm, 3)}\u200E שניות לפעמה)"
    }

    val tapBtn = createActionBtn("👆 קבע קצב בלחיצות (Tap Tempo)", false) {
        val now = System.currentTimeMillis()
        if (tapTimes.isNotEmpty() && now - tapTimes.last() > 2500L) tapTimes.clear()
        tapTimes.add(now)
        if (tapTimes.size >= 2) {
            val intervals = (1 until tapTimes.size).map { tapTimes[it] - tapTimes[it - 1] }
            val avgMs = intervals.average()
            if (avgMs > 0) {
                bpm = (60000.0 / avgMs).roundToInt().coerceIn(30, 240)
                bpmIn.setText(bpm.toString())
                pendulumCanvas.invalidate()
                resTv.text = "קצב שנקבע בלחיצות: $bpm BPM"
            }
        }
    }

    val sigBtn = createActionBtn("משקל תיבה: 4/4 (לחץ להחלפה)", false) {
        beatsPerMeasure = when (beatsPerMeasure) { 4 -> 3; 3 -> 6; 6 -> 2; else -> 4 }
        currentBeat = 0
        (it as TextView).text = "משקל תיבה: $beatsPerMeasure/4"
        pendulumCanvas.invalidate()
    }

    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) {
            isPlaying = false
            handler.removeCallbacksAndMessages(null)
            try { toneGen?.release() } catch (_: Exception) {}
            toneGen = null
        }
    })

    root.addView(pendulumCanvas); root.addView(bpmIn); root.addView(playBtn); root.addView(tapBtn); root.addView(sigBtn); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 5. מונה לחיצות חכם (Tally Counter - Tab 70)
// =========================================================================
fun MainActivity.createProTallyCounterScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🔢 מונה לחיצות חכם (Tally Counter)", "מנייה מהירה בלחיצה, הגדרת יעד וגודל צעד עם שמירה אוטומטית"))

    var count = prefs?.getInt("pro_tally_count", 0) ?: 0
    var targetGoal = prefs?.getInt("pro_tally_target", 100) ?: 100

    val counterCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val cx = width / 2f; val cy = height / 2f
            val r = min(width, height) * 0.36f
            val isMin = isMinimalistMode

            p.style = Paint.Style.STROKE
            p.strokeWidth = 9f * d
            p.color = if (isMin) Color.argb(35, 44, 44, 44) else Color.argb(45, 255, 255, 255)
            canvas.drawCircle(cx, cy, r, p)

            val progress = (count.toFloat() / targetGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
            p.color = if (isMin) MinimalistTheme.ACCENT_SAGE_GREEN else primaryColor
            p.strokeCap = Paint.Cap.ROUND
            canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -90f, progress * 360f, false, p)

            p.style = Paint.Style.FILL
            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
            p.textSize = 36f * d
            canvas.drawText(count.toString(), cx, cy + 10f * d, p)

            p.textSize = 12.5f * d
            p.color = if (isMin) MinimalistTheme.TEXT_MUTED_GREY else Color.LTGRAY
            canvas.drawText("יעד: $count / $targetGoal (${(progress * 100).toInt()}%) • לחץ להוספה", cx, height - 14f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(230f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProTallyCounterScreen, 22f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProTallyCounterScreen, 22f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(22f).toFloat() }
        }
    }

    val stepIn = createInputBox("גודל צעד בכל לחיצה (ברירת מחדל 1)").apply { setText("1") }
    val targetIn = createInputBox("יעד מנייה (למשל 100)").apply { setText(targetGoal.toString()) }
    val resTv = createResultTextView().apply { text = "מונה נוכחי: $count" }

    fun updateCounter(delta: Int) {
        targetGoal = (targetIn.text.toString().toIntOrNull() ?: 100).coerceAtLeast(1)
        count += delta
        prefs?.edit()?.putInt("pro_tally_count", count)?.putInt("pro_tally_target", targetGoal)?.apply()
        counterCanvas.invalidate()
        val rem = (targetGoal - count).coerceAtLeast(0)
        resTv.text = "מונה נוכחי: $count  |  נותרו ליעד: $rem"
    }

    counterCanvas.setOnClickListener {
        feedback(it)
        val step = stepIn.text.toString().toIntOrNull() ?: 1
        updateCounter(step)
    }

    val plusBtn = createActionBtn("➕ הוסף למניין (+)", true) {
        val step = stepIn.text.toString().toIntOrNull() ?: 1
        updateCounter(step)
    }
    val minusBtn = createActionBtn("➖ הפחת מהמניין (-)", false) {
        val step = stepIn.text.toString().toIntOrNull() ?: 1
        updateCounter(-step)
    }
    val resetBtn = createActionBtn("🔄 אפס מונה ל-0", false) {
        count = 0
        updateCounter(0)
    }

    root.addView(counterCanvas); root.addView(plusBtn); root.addView(minusBtn); root.addView(resetBtn)
    root.addView(stepIn); root.addView(targetIn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 6. קודי צבע של סלילים (Inductor Color Code Calculator - Tab 71)
// =========================================================================
fun MainActivity.createProInductorColorScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🌀 קודי צבע של סלילים (Inductors)", "פענוח סלילי השראות ב-µH/mH/H וחישוב היגב השראתי XL = 2πfL"))

    val colors = listOf(
        Triple("שחור (0)", 0, Color.BLACK), Triple("חום (1)", 1, Color.rgb(121, 85, 72)),
        Triple("אדום (2)", 2, Color.rgb(229, 57, 53)), Triple("כתום (3)", 3, Color.rgb(251, 140, 0)),
        Triple("צהוב (4)", 4, Color.rgb(253, 216, 53)), Triple("ירוק (5)", 5, Color.rgb(67, 160, 71)),
        Triple("כחול (6)", 6, Color.rgb(30, 136, 229)), Triple("סגול (7)", 7, Color.rgb(142, 36, 170)),
        Triple("אפור (8)", 8, Color.rgb(117, 117, 117)), Triple("לבן (9)", 9, Color.WHITE)
    )
    var b1 = 1; var b2 = 0; var mult = 1
    var bandCols = intArrayOf(colors[1].third, colors[0].third, colors[1].third, Color.rgb(212, 175, 55))
    var labelStr = "100 µH ±5%"

    val coilCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val cy = h * 0.44f
            val isMin = isMinimalistMode

            p.style = Paint.Style.STROKE; p.strokeWidth = 5f * d
            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.LTGRAY
            canvas.drawLine(w * 0.08f, cy, w * 0.92f, cy, p)

            val bodyRect = RectF(w * 0.22f, cy - 24f * d, w * 0.78f, cy + 24f * d)
            p.style = Paint.Style.FILL; p.color = Color.parseColor("#80CBC4")
            canvas.drawRoundRect(bodyRect, 18f * d, 18f * d, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 1.8f * d
            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            canvas.drawRoundRect(bodyRect, 18f * d, 18f * d, p)

            val pos = floatArrayOf(0.30f, 0.42f, 0.54f, 0.68f)
            p.style = Paint.Style.FILL
            for (i in bandCols.indices) {
                val bx = w * pos[i]
                p.color = bandCols[i]
                canvas.drawRect(bx - 6f * d, cy - 24f * d, bx + 6f * d, cy + 24f * d, p)
            }

            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD; p.textSize = 15f * d
            canvas.drawText("השראות הסליל: $labelStr", w / 2f, h - 14f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(175f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProInductorColorScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProInductorColorScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val btn1 = createActionBtn("פס 1: ${colors[b1].first}", false) {}
    val btn2 = createActionBtn("פס 2: ${colors[b2].first}", false) {}
    val btnM = createActionBtn("מכפיל (µH): ×10^$mult (${colors[mult].first})", false) {}
    val freqIn = createInputBox("תדר עבודה ב-kHz לחישוב היגב השראתי XL").apply { setText("100") }
    val resTv = createResultTextView()

    fun updateInductor() {
        val microHenries = (b1 * 10 + b2) * 10.0.pow(mult)
        val milliHenries = microHenries / 1000.0
        val freqHz = (freqIn.text.toString().toDoubleOrNull() ?: 100.0) * 1000.0
        val xlOhms = 2.0 * PI * freqHz * (microHenries * 1e-6)

        bandCols = intArrayOf(colors[b1].third, colors[b2].third, colors[mult].third, Color.rgb(212, 175, 55))
        labelStr = if (microHenries >= 1000) "${Utils.formatResult(milliHenries, 2)} mH ±5%" else "${Utils.formatResult(microHenries, 1)} µH ±5%"
        coilCanvas.invalidate()

        btn1.text = "פס 1: ${colors[b1].first}"
        btn2.text = "פס 2: ${colors[b2].first}"
        btnM.text = "מכפיל (µH): ×10^$mult (${colors[mult].first})"
        resTv.text = "השראות הסליל (L):\n" +
                "• \u200E${Utils.formatResult(microHenries, 2)} µH\u200E  |  \u200E${Utils.formatResult(milliHenries, 4)} mH\u200E\n" +
                "• היגב השראתי (XL = 2πfL): \u200E${Utils.formatResult(xlOhms, 2)} Ω\u200E"
    }

    btn1.setOnClickListener { showItemPicker("בחר צבע פס 1", colors.map { it.first }) { c -> b1 = colors.indexOfFirst { it.first == c }; updateInductor() } }
    btn2.setOnClickListener { showItemPicker("בחר צבע פס 2", colors.map { it.first }) { c -> b2 = colors.indexOfFirst { it.first == c }; updateInductor() } }
    btnM.setOnClickListener { showItemPicker("בחר מכפיל", colors.map { it.first }) { c -> mult = colors.indexOfFirst { it.first == c }; updateInductor() } }
    val calcBtn = createActionBtn("חשב השראות והיגב סליל 🌀") { updateInductor() }

    updateInductor()
    root.addView(coilCanvas); root.addView(btn1); root.addView(btn2); root.addView(btnM)
    root.addView(freqIn); root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 7. סימולטור שערים לוגיים וטבלאות אמת (Logic Gates Simulator - Tab 72)
// =========================================================================
fun MainActivity.createProLogicGatesScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🔌 שערים לוגיים וטבלאות אמת", "סימולטור שערים דיגיטליים (AND, OR, XOR, NAND, NOR, XNOR, NOT) וטבלת אמת חיה"))

    val gates = listOf("AND (ו-לוגי)", "OR (או-לוגי)", "XOR (או מוציא)", "NAND (לא-ו)", "NOR (לא-או)", "XNOR (שקילות)", "NOT (שלילה / מהפך)")
    var gateIdx = 0
    var inA = true
    var inB = false

    fun evalGate(g: Int, a: Boolean, b: Boolean): Boolean = when (g) {
        0 -> a && b
        1 -> a || b
        2 -> a xor b
        3 -> !(a && b)
        4 -> !(a || b)
        5 -> !(a xor b)
        else -> !a
    }

    val gateCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val cx = w / 2f; val cy = h * 0.46f
            val isMin = isMinimalistMode
            val outY = evalGate(gateIdx, inA, inB)

            val highCol = if (isMin) Color.parseColor("#2E7D32") else Color.parseColor("#00E676")
            val lowCol = if (isMin) Color.parseColor("#757575") else Color.parseColor("#90A4AE")

            p.style = Paint.Style.STROKE; p.strokeWidth = 3.2f * d
            p.color = if (inA) highCol else lowCol
            canvas.drawLine(w * 0.14f, cy - 18f * d, cx - 34f * d, cy - 18f * d, p)
            if (gateIdx != 6) {
                p.color = if (inB) highCol else lowCol
                canvas.drawLine(w * 0.14f, cy + 18f * d, cx - 34f * d, cy + 18f * d, p)
            }
            p.color = if (outY) highCol else lowCol
            canvas.drawLine(cx + 38f * d, cy, w * 0.86f, cy, p)

            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            canvas.drawRoundRect(RectF(cx - 34f * d, cy - 32f * d, cx + 34f * d, cy + 32f * d), 14f * d, 14f * d, p)

            p.style = Paint.Style.FILL; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
            p.textSize = 15f * d
            canvas.drawText(gates[gateIdx].substringBefore(" "), cx, cy + 5f * d, p)

            p.textSize = 13f * d
            canvas.drawText("A=${if (inA) 1 else 0}", w * 0.14f, cy - 26f * d, p)
            if (gateIdx != 6) canvas.drawText("B=${if (inB) 1 else 0}", w * 0.14f, cy + 36f * d, p)
            p.color = if (outY) highCol else lowCol
            canvas.drawText("Y=${if (outY) 1 else 0}", w * 0.86f, cy - 12f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(185f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProLogicGatesScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProLogicGatesScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val gateBtn = createActionBtn("שער נבחר: ${gates[0]}", true) {}
    val toggleABtn = createActionBtn("כניסה A: 1 (גבוה / True)", false) {}
    val toggleBBtn = createActionBtn("כניסה B: 0 (נמוך / False)", false) {}
    val resTv = createResultTextView().apply { textSize = 16.5f }

    fun refreshLogic() {
        val y = evalGate(gateIdx, inA, inB)
        gateCanvas.invalidate()
        toggleABtn.text = "כניסה A: ${if (inA) "1 (True)" else "0 (False)"}"
        toggleBBtn.text = "כניסה B: ${if (inB) "1 (True)" else "0 (False)"}"
        toggleBBtn.visibility = if (gateIdx == 6) View.GONE else View.VISIBLE

        val sb = StringBuilder("מוצא השער (${gates[gateIdx].substringBefore(" ")}):  Y = ${if (y) "1 (גבוה)" else "0 (נמוך)"}\n\n📋 טבלת אמת מלאה (Truth Table):\n")
        for (a in listOf(false, true)) {
            for (b in listOf(false, true)) {
                if (gateIdx == 6 && b) continue
                val r = evalGate(gateIdx, a, b)
                val mark = if (a == inA && (gateIdx == 6 || b == inB)) " ◄ (פעיל)" else ""
                if (gateIdx == 6) {
                    sb.append("• A=${if (a) 1 else 0}  ➔  Y=${if (r) 1 else 0}$mark\n")
                } else {
                    sb.append("• A=${if (a) 1 else 0} , B=${if (b) 1 else 0}  ➔  Y=${if (r) 1 else 0}$mark\n")
                }
            }
        }
        resTv.text = sb.toString().trim()
    }

    gateBtn.setOnClickListener {
        showItemPicker("בחר שער לוגי", gates) { chosen ->
            gateIdx = gates.indexOf(chosen).coerceAtLeast(0)
            gateBtn.text = "שער נבחר: $chosen"
            refreshLogic()
        }
    }
    toggleABtn.setOnClickListener { feedback(it); inA = !inA; refreshLogic() }
    toggleBBtn.setOnClickListener { feedback(it); inB = !inB; refreshLogic() }

    refreshLogic()
    root.addView(gateCanvas); root.addView(gateBtn); root.addView(toggleABtn); root.addView(toggleBBtn)
    root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 8. ממיר צבעים HEX ל-RGB / HSL / CMYK (Color Converter - Tab 73)
// =========================================================================
fun MainActivity.createProHexRgbColorScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🎨 ממיר צבעים HEX / RGB / HSL / CMYK", "המרה דו-כיוונית בין קוד HEX לערכי RGB, HSL ודפוס CMYK עם הרמוניית צבעים"))

    var currentColor = Color.parseColor("#00F5D4")
    val swatchCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val hsv = FloatArray(3)
            Color.colorToHSV(currentColor, hsv)
            val compColor = Color.HSVToColor(floatArrayOf((hsv[0] + 180f) % 360f, hsv[1], hsv[2]))

            p.style = Paint.Style.FILL
            p.color = currentColor
            canvas.drawRoundRect(RectF(16f * d, 14f * d, w * 0.62f, h - 14f * d), 16f * d, 16f * d, p)

            p.color = compColor
            canvas.drawRoundRect(RectF(w * 0.66f, 14f * d, w - 16f * d, h - 14f * d), 16f * d, 16f * d, p)

            p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD; p.textSize = 13f * d
            p.color = MinimalistTheme.ensureReadableTextOnBg(Color.WHITE, currentColor, 2.4)
            canvas.drawText(String.format("#%06X", 0xFFFFFF and currentColor), w * 0.39f, h / 2f + 5f * d, p)

            p.color = MinimalistTheme.ensureReadableTextOnBg(Color.WHITE, compColor, 2.4)
            canvas.drawText("משלים", w * 0.81f, h / 2f + 5f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(130f)).apply { bottomMargin = dp(12f) }
    }

    val hexIn = createInputBox("קוד HEX (למשל #00F5D4 או FF5733)").apply {
        inputType = InputType.TYPE_CLASS_TEXT
        setText("#00F5D4")
    }
    val rgbIn = createInputBox("או הזן RGB מופרד בפסיק (למשל 0, 245, 212)").apply {
        inputType = InputType.TYPE_CLASS_TEXT
        setText("0, 245, 212")
    }
    val resTv = createResultTextView().apply { textSize = 16.5f }

    fun updateColorDetails(col: Int) {
        currentColor = col
        swatchCanvas.invalidate()
        val r = Color.red(col); val g = Color.green(col); val b = Color.blue(col)
        val hsv = FloatArray(3)
        Color.colorToHSV(col, hsv)

        val rf = r / 255f; val gf = g / 255f; val bf = b / 255f
        val k = 1f - maxOf(rf, gf, bf)
        val c = if (k < 1f) (1f - rf - k) / (1f - k) else 0f
        val m = if (k < 1f) (1f - gf - k) / (1f - k) else 0f
        val y = if (k < 1f) (1f - bf - k) / (1f - k) else 0f

        val hexStr = String.format(Locale.US, "#%02X%02X%02X", r, g, b)
        resTv.text = "פלט מרחבי צבע:\n" +
                "• HEX: \u200E$hexStr\u200E\n" +
                "• RGB: \u200E($r, $g, $b)\u200E\n" +
                "• HSV / HSB: \u200E(${hsv[0].roundToInt()}°, ${(hsv[1] * 100).roundToInt()}%, ${(hsv[2] * 100).roundToInt()}%)\u200E\n" +
                "• CMYK (דפוס): \u200EC:${(c * 100).roundToInt()}% M:${(m * 100).roundToInt()}% Y:${(y * 100).roundToInt()}% K:${(k * 100).roundToInt()}%\u200E"
    }

    val fromHexBtn = createActionBtn("המר מקוד HEX ל-RGB/CMYK 🎨", true) {
        val raw = hexIn.text.toString().trim().removePrefix("#")
        try {
            val col = Color.parseColor("#$raw")
            rgbIn.setText("${Color.red(col)}, ${Color.green(col)}, ${Color.blue(col)}")
            updateColorDetails(col)
        } catch (_: Exception) {
            Toast.makeText(this, "קוד HEX לא תקין (דוגמה: #FF5733)", Toast.LENGTH_SHORT).show()
        }
    }

    val fromRgbBtn = createActionBtn("המר מערכי RGB לקוד HEX 🔄", false) {
        val parts = rgbIn.text.toString().split(",", " ").mapNotNull { it.trim().toIntOrNull() }
        if (parts.size >= 3) {
            val col = Color.rgb(parts[0].coerceIn(0, 255), parts[1].coerceIn(0, 255), parts[2].coerceIn(0, 255))
            hexIn.setText(String.format(Locale.US, "#%02X%02X%02X", Color.red(col), Color.green(col), Color.blue(col)))
            updateColorDetails(col)
        }
    }

    updateColorDetails(currentColor)
    root.addView(swatchCanvas); root.addView(hexIn); root.addView(fromHexBtn)
    root.addView(rgbIn); root.addView(fromRgbBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 9. יחס גובה-רוחב ורזולוציה (Aspect Ratio Calculator - Tab 74)
// =========================================================================
fun MainActivity.createProAspectRatioScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🖥️ יחס גובה-רוחב ורזולוציה (Aspect Ratio)", "חישוב יחס מסך ווידאו (16:9, 21:9, 4:3), צפיפות פיקסלים PPI ומגה-פיקסל"))

    var wPx = 1920.0; var hPx = 1080.0
    var ratioLabel = "16 : 9"

    val screenCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val cx = width / 2f; val cy = height / 2f
            val maxW = width * 0.72f; val maxH = height * 0.72f
            val ratio = (wPx / hPx.coerceAtLeast(1.0)).toFloat().coerceIn(0.4f, 3.0f)

            val rectW: Float; val rectH: Float
            if (ratio >= 1f) {
                rectW = maxW
                rectH = (maxW / ratio).coerceAtMost(maxH)
            } else {
                rectH = maxH
                rectW = (maxH * ratio).coerceAtMost(maxW)
            }

            val rRect = RectF(cx - rectW / 2f, cy - rectH / 2f, cx + rectW / 2f, cy + rectH / 2f)
            p.style = Paint.Style.FILL
            p.color = if (isMinimalistMode) MinimalistTheme.ACCENT_PASTEL_BLUE else Color.argb(55, 0, 245, 212)
            canvas.drawRoundRect(rRect, 12f * d, 12f * d, p)

            p.style = Paint.Style.STROKE; p.strokeWidth = 2.4f * d
            p.color = if (isMinimalistMode) MinimalistTheme.TEXT_CHARCOAL else primaryColor
            canvas.drawRoundRect(rRect, 12f * d, 12f * d, p)

            p.style = Paint.Style.FILL; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
            p.textSize = 16f * d
            p.color = if (isMinimalistMode) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            canvas.drawText(ratioLabel, cx, cy + 6f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(170f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProAspectRatioScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProAspectRatioScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val widthIn = createInputBox("רוחב בפיקסלים / ס״מ (למשל 1920)").apply { setText("1920") }
    val heightIn = createInputBox("גובה בפיקסלים / ס״מ (למשל 1080)").apply { setText("1080") }
    val diagInchIn = createInputBox("אלכסון מסך באינצ'ים לחישוב PPI (למשל 24)").apply { setText("24") }
    val resTv = createResultTextView()

    val calcBtn = createActionBtn("חשב יחס גובה-רוחב ו-PPI 🖥️", true) {
        wPx = (widthIn.text.toString().toDoubleOrNull() ?: 1920.0).coerceAtLeast(1.0)
        hPx = (heightIn.text.toString().toDoubleOrNull() ?: 1080.0).coerceAtLeast(1.0)
        val diagInch = (diagInchIn.text.toString().toDoubleOrNull() ?: 24.0).coerceAtLeast(0.5)

        val g = Utils.gcd(wPx.roundToLong(), hPx.roundToLong()).coerceAtLeast(1L)
        val rw = wPx.roundToLong() / g
        val rh = hPx.roundToLong() / g
        ratioLabel = "\u200E$rw : $rh\u200E"
        screenCanvas.invalidate()

        val diagPx = hypot(wPx, hPx)
        val ppi = diagPx / diagInch
        val megaPixels = (wPx * hPx) / 1e6

        resTv.text = "ניתוח תצוגה ורזולוציה:\n" +
                "• יחס גובה-רוחב מצומצם: \u200E$rw : $rh\u200E (יחס עשרוני: \u200E${Utils.formatResult(wPx / hPx, 3)} : 1\u200E)\n" +
                "• צפיפות פיקסלים ב-\u200E${Utils.formatResult(diagInch, 1)}\"\u200E: \u200E${Utils.formatResult(ppi, 1)} PPI\u200E\n" +
                "• סך פיקסלים: \u200E${Utils.formatResult(megaPixels, 2)} MP\u200E (אלכסון: \u200E${Utils.formatResult(diagPx, 0)} px\u200E)"
    }
    calcBtn.performClick()

    root.addView(screenCanvas); root.addView(widthIn); root.addView(heightIn); root.addView(diagInchIn)
    root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 10. מוניטור מצב ובריאות הסוללה (Battery Status Monitor - Tab 75)
// =========================================================================
fun MainActivity.createProBatteryMonitorScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🔋 מוניטור מצב ובריאות הסוללה", "נתוני סוללה בזמן אמת: מתח (mV), טמפרטורה (°C), מצב בריאות וטכנולוגיה"))

    var pctLevel = 85
    var tempC = 29.5f
    var voltageMv = 4120

    val batCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val isMin = isMinimalistMode

            val bodyRect = RectF(w * 0.22f, 26f * d, w * 0.76f, h - 32f * d)
            val capRect = RectF(w * 0.76f, h * 0.36f, w * 0.80f, h * 0.56f)

            p.style = Paint.Style.STROKE; p.strokeWidth = 3f * d
            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            canvas.drawRoundRect(bodyRect, 16f * d, 16f * d, p)
            p.style = Paint.Style.FILL
            canvas.drawRoundRect(capRect, 4f * d, 4f * d, p)

            val fillW = (bodyRect.width() - 12f * d) * (pctLevel / 100f).coerceIn(0.05f, 1f)
            p.color = when {
                pctLevel <= 20 -> Color.parseColor("#FF3B30")
                isMin -> MinimalistTheme.ACCENT_SAGE_GREEN
                else -> Color.parseColor("#00E676")
            }
            canvas.drawRoundRect(
                RectF(bodyRect.left + 6f * d, bodyRect.top + 6f * d, bodyRect.left + 6f * d + fillW, bodyRect.bottom - 6f * d),
                12f * d, 12f * d, p
            )

            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD; p.textSize = 22f * d
            canvas.drawText("$pctLevel%", bodyRect.centerX(), bodyRect.centerY() + 8f * d, p)

            p.textSize = 13f * d
            canvas.drawText("\u200E$voltageMv mV\u200E  •  \u200E${Utils.formatResult(tempC.toDouble(), 1)} °C\u200E", w / 2f, h - 8f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(165f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProBatteryMonitorScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProBatteryMonitorScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val resTv = createResultTextView().apply { textSize = 16.5f }

    fun readBatteryTelemetry() {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (intent != null) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            pctLevel = ((level * 100f) / scale).roundToInt().coerceIn(0, 100)
            tempC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280) / 10f
            voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4000)
            val tech = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

            val statusStr = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "בטעינה ⚡"
                BatteryManager.BATTERY_STATUS_FULL -> "טעונה במלואה (100%) 🟢"
                else -> "בשימוש (פריקה)"
            }
            val plugStr = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "מטען קיר (AC)"
                BatteryManager.BATTERY_PLUGGED_USB -> "חיבור USB"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "טעינה אלחוטית"
                else -> "ללא מקור מתח חיצוני"
            }
            val healthStr = when (health) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "תקינה (Good) 🟢"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "התחממות יתר 🔴"
                BatteryManager.BATTERY_HEALTH_DEAD -> "חלשה / דורשת החלפה 🔴"
                else -> "תקינה"
            }

            batCanvas.invalidate()
            resTv.text = "דו״ח דיאגנוסטיקת סוללה חי:\n" +
                    "• רמת טעינה: $pctLevel% ($statusStr)\n" +
                    "• מקור מתח: $plugStr\n" +
                    "• בריאות הסוללה: $healthStr\n" +
                    "• מתח הדקים: \u200E$voltageMv mV\u200E (\u200E${Utils.formatResult(voltageMv / 1000.0, 3)} V\u200E)\n" +
                    "• טמפרטורת סוללה: \u200E${Utils.formatResult(tempC.toDouble(), 1)} °C\u200E | טכנולוגיה: $tech"
        }
    }

    val refreshBtn = createActionBtn("רענן נתוני סוללה בזמן אמת 🔋", true) { readBatteryTelemetry() }
    readBatteryTelemetry()

    root.addView(batCanvas); root.addView(refreshBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 11. מחשבון חשבון חשמל וצריכת אנרגיה (Electricity Bill & Energy - Tab 76)
// =========================================================================
fun MainActivity.createProElectricityBillScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("⚡ מחשבון חשבון חשמל וצריכת אנרגיה", "חישוב צריכת קוט״ש (kWh) ועלות יומית, חודשית ושנתית לפי תעריף החשמל"))

    var monthlyCostIls = 140.0
    var monthlyKwh = 225.0

    val energyCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val isMin = isMinimalistMode

            p.style = Paint.Style.FILL
            p.color = if (isMin) MinimalistTheme.ACCENT_BUTTER_YELLOW else Color.argb(55, 255, 213, 79)
            canvas.drawRoundRect(RectF(18f * d, 18f * d, w - 18f * d, h - 18f * d), 18f * d, 18f * d, p)

            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
            p.textSize = 22f * d
            canvas.drawText("₪${Utils.formatResult(monthlyCostIls, 1)} לחודש", w / 2f, h * 0.48f, p)
            p.textSize = 13.5f * d
            canvas.drawText("צריכה חודשית: \u200E${Utils.formatResult(monthlyKwh, 1)} kWh\u200E (קוט״ש)", w / 2f, h * 0.74f, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(135f)).apply { bottomMargin = dp(12f) }
    }

    val wattsIn = createInputBox("הספק מכשיר בוואט (W - למשל 1500 למזגן/תנור)").apply { setText("1500") }
    val hoursIn = createInputBox("שעות הפעלה ביום (למשל 5)").apply { setText("5") }
    val tariffIn = createInputBox("תעריף לקוט״ש בש״ח (כולל מע״מ, למשל 0.64)").apply { setText("0.64") }
    val resTv = createResultTextView()

    val calcBtn = createActionBtn("חשב צריכת אנרגיה וחשבון חשמל ⚡", true) {
        val w = wattsIn.text.toString().toDoubleOrNull() ?: 1500.0
        val hrs = hoursIn.text.toString().toDoubleOrNull() ?: 5.0
        val rate = tariffIn.text.toString().toDoubleOrNull() ?: 0.64

        val dailyKwh = (w * hrs) / 1000.0
        monthlyKwh = dailyKwh * 30.0
        val yearlyKwh = dailyKwh * 365.0

        val dailyCost = dailyKwh * rate
        monthlyCostIls = monthlyKwh * rate
        val yearlyCost = yearlyKwh * rate
        energyCanvas.invalidate()

        resTv.text = "סיכום צריכת חשמל ועלויות:\n" +
                "• יומי: \u200E${Utils.formatResult(dailyKwh, 2)} kWh\u200E  ➔  ₪${Utils.formatResult(dailyCost, 2)}\n" +
                "• חודשי (30 יום): \u200E${Utils.formatResult(monthlyKwh, 1)} kWh\u200E  ➔  ₪${Utils.formatResult(monthlyCostIls, 2)}\n" +
                "• דו-חודשי (חשבון חברת חשמל): ₪${Utils.formatResult(monthlyCostIls * 2, 2)}\n" +
                "• שנתי (365 יום): \u200E${Utils.formatResult(yearlyKwh, 0)} kWh\u200E  ➔  ₪${Utils.formatResult(yearlyCost, 0)}"
    }
    calcBtn.performClick()

    root.addView(energyCanvas); root.addView(wattsIn); root.addView(hoursIn); root.addView(tariffIn)
    root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 12. מעבדת הצפנה, צפנים ו-Hash (Encryption & Hash Lab - Tab 77)
// =========================================================================
fun MainActivity.createProEncryptionLabScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🔐 הצפנת טקסט, צפנים ו-Hash", "צופן קיסר (עברית/אנגלית), קידוד Base64 דו-כיווני וגיבוב SHA-256 / MD5"))

    val textIn = createInputBox("הזן טקסט להצפנה או פענוח...").apply {
        inputType = InputType.TYPE_CLASS_TEXT
        setText("שלום עולם 2026")
    }
    val shiftIn = createInputBox("היסט צופן קיסר (Shift - למשל 3 או -3 לפענוח)").apply { setText("3") }
    val resTv = createResultTextView().apply { textSize = 15.5f }

    fun caesarShift(input: String, shift: Int): String {
        val heb = "אבגדהוזחטיכלמנסעפצקרשת"
        val engU = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val engL = "abcdefghijklmnopqrstuvwxyz"
        return input.map { ch ->
            when {
                heb.contains(ch) -> heb[(heb.indexOf(ch) + shift % heb.length + heb.length) % heb.length]
                engU.contains(ch) -> engU[(engU.indexOf(ch) + shift % 26 + 26) % 26]
                engL.contains(ch) -> engL[(engL.indexOf(ch) + shift % 26 + 26) % 26]
                else -> ch
            }
        }.joinToString("")
    }

    fun hashString(input: String, algo: String): String {
        val bytes = MessageDigest.getInstance(algo).digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    val encBtn = createActionBtn("הצפן וחשב חתימות Hash 🔐", true) {
        val raw = textIn.text.toString()
        val s = shiftIn.text.toString().toIntOrNull() ?: 3
        val caesar = caesarShift(raw, s)
        val b64 = Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val sha256 = hashString(raw, "SHA-256")
        val md5 = hashString(raw, "MD5")

        resTv.text = "• צופן קיסר (היסט $s):\n$caesar\n\n" +
                "• קידוד Base64:\n\u200E$b64\u200E\n\n" +
                "• חתימת MD5:\n\u200E$md5\u200E\n\n" +
                "• חתימת SHA-256:\n\u200E${sha256.take(32)}...\u200E"
    }

    val decB64Btn = createActionBtn("פענח טקסט מקוד Base64 🔓", false) {
        try {
            val decoded = String(Base64.decode(textIn.text.toString().trim(), Base64.DEFAULT), Charsets.UTF_8)
            resTv.text = "טקסט מפוענח מ-Base64:\n$decoded"
        } catch (_: Exception) {
            Toast.makeText(this, "הטקסט אינו קוד Base64 תקין", Toast.LENGTH_SHORT).show()
        }
    }

    encBtn.performClick()
    root.addView(textIn); root.addView(shiftIn); root.addView(encBtn); root.addView(decB64Btn)
    root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 13. פתקים חכמים ורשימות עם חישוב אוטומטי (Smart Notes - Tab 78)
// =========================================================================
fun MainActivity.createProSmartNotesScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("📝 פתקים חכמים ורשימות (Smart Notes)", "מחברת פתקים אישית עם שמירה אוטומטית וחישוב משוואות מתמטיות בתוך הטקסט"))

    val titleIn = createInputBox("כותרת הפתק (למשל: רשימת קניות / מידות חדר)").apply { inputType = InputType.TYPE_CLASS_TEXT }
    val bodyIn = createInputBox("תוכן הפתק או תרגיל (למשל: 150 + 240 * 2)...").apply {
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        minLines = 3
    }
    val notesListContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

    fun loadNotes(): MutableList<JSONObject> {
        val list = mutableListOf<JSONObject>()
        val raw = prefs?.getString("pro_smart_notes_json", "[]") ?: "[]"
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) list.add(arr.getJSONObject(i))
        } catch (_: Exception) {}
        return list
    }

    fun saveNotes(list: List<JSONObject>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs?.edit()?.putString("pro_smart_notes_json", arr.toString())?.apply()
    }

    fun renderNotes() {
        notesListContainer.removeAllViews()
        val notes = loadNotes()
        val textCol = if (isMinimalistMode) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        for ((idx, note) in notes.withIndex()) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
                background = when {
                    isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProSmartNotesScreen, 18f, 0)
                    isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProSmartNotesScreen, 18f)
                    else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.08f)); cornerRadius = dp(18f).toFloat() }
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(10f) }
            }
            val tv = TextView(this).apply {
                text = "📌 ${note.optString("title")}\n${note.optString("body")}\n🕒 ${note.optString("date")}"
                setTextColor(textCol); textSize = 15f; gravity = Gravity.RIGHT
                setOnClickListener { copyToClipboard(note.optString("body")) }
            }
            val delBtn = TextView(this).apply {
                text = "🗑️ מחק פתק"
                setTextColor(if (isMinimalistMode) MinimalistTheme.ALERT_RED_TEXT else Color.parseColor("#FF5252"))
                textSize = 13f; setPadding(0, dp(8f), 0, 0)
                setOnClickListener {
                    val updated = loadNotes().apply { removeAt(idx) }
                    saveNotes(updated); renderNotes()
                }
            }
            card.addView(tv); card.addView(delBtn)
            notesListContainer.addView(card)
        }
    }

    val evalMathBtn = createActionBtn("🧮 חשב ביטוי מתמטי שכתוב בפתק", false) {
        val raw = bodyIn.text.toString().trim()
        if (raw.isNotEmpty()) {
            try {
                val res = Utils.evalMathExpression(raw)
                bodyIn.setText("$raw = ${Utils.formatResult(res, calcPrecision)}")
            } catch (_: Exception) {
                Toast.makeText(this, "לא זוהה תרגיל מתמטי תקין בשדה התוכן", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val saveBtn = createActionBtn("💾 שמור פתק חדש במחברת", true) {
        val t = titleIn.text.toString().trim().ifEmpty { "פתק מהיר" }
        val b = bodyIn.text.toString().trim()
        if (b.isNotEmpty()) {
            val list = loadNotes()
            list.add(0, JSONObject().apply {
                put("title", t)
                put("body", b)
                put("date", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()))
            })
            saveNotes(list)
            titleIn.setText(""); bodyIn.setText("")
            renderNotes()
        }
    }

    renderNotes()
    root.addView(titleIn); root.addView(bodyIn); root.addView(evalMathBtn); root.addView(saveBtn); root.addView(notesListContainer)
    scroll.addView(root); return scroll
}

// =========================================================================
// 14. סורק ומחולל מסמכי PDF (PDF Document Creator & Scanner - Tab 79)
// =========================================================================
fun MainActivity.createProPdfScannerCreatorScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("📄 סורק ומחולל מסמכי PDF", "יצירת מסמך PDF רשמי מטקסט, טבלאות מדידה וחישובים עם תצוגת דף A4"))

    val docTitleIn = createInputBox("כותרת המסמך (למשל: דוח מדידות הנדסי)").apply {
        inputType = InputType.TYPE_CLASS_TEXT
        setText("דוח חישובים ומדידות - Pro fx-991")
    }
    val docBodyIn = createInputBox("תוכן המסמך שייכתב לדף ה-PDF...").apply {
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        minLines = 4
        setText("1. מדידת שטח וחומרים: תקין\n2. תוצאה אחרונה במחשבון: $currentInput")
    }
    val resTv = createResultTextView()

    val generateBtn = createActionBtn("📄 צור ושמור קובץ PDF כעת", true) {
        try {
            val pdf = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdf.startPage(pageInfo)
            val c = page.canvas

            val headerP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1E293B"); textSize = 22f; typeface = Typeface.DEFAULT_BOLD }
            val bodyP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 15f }
            val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; strokeWidth = 1.5f }

            c.drawText(docTitleIn.text.toString(), 45f, 55f, headerP)
            c.drawLine(45f, 70f, 550f, 70f, lineP)

            var y = 105f
            for (line in docBodyIn.text.toString().split("\n")) {
                c.drawText(line, 45f, y, bodyP)
                y += 24f
            }
            pdf.finishPage(page)

            val file = File(externalCacheDir ?: cacheDir, "ProDoc_${System.currentTimeMillis()}.pdf")
            file.outputStream().use { pdf.writeTo(it) }
            pdf.close()
            resTv.text = "✅ קובץ ה-PDF נוצר בהצלחה!\nנתיב: \u200E${file.name}\u200E"
        } catch (e: Exception) {
            resTv.text = "שגיאה ביצירת PDF: ${e.message}"
        }
    }

    root.addView(docTitleIn); root.addView(docBodyIn); root.addView(generateBtn); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 15. מדידת טבעת מכוילת על המסך (Multi-Calibrated Ring Sizer - Tab 80)
// =========================================================================
fun MainActivity.createProRingSizerScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(
        createProCanvaHeader(
            "💍 מדידת טבעת מכוילת על המסך (Ring Sizer)",
            "מותאם לכל גודל מסך: כולל כיול מול מטבע (1/2/5/10 ש״ח) או הזנת אינצ׳ים של המסך לדיוק של 0.05 מ״מ"
        )
    )

    var diamMm = 17.30f
    var calibFactor = (prefs?.getFloat("pro_ruler_calib_factor", 1.0f) ?: 1.0f).coerceIn(0.60f, 1.50f)
    var isCoinCalibrationMode = false
    var selectedCoinName = "מטבע 10 ש״ח (23.0 מ״מ)"
    var selectedCoinMm = 23.0f

    val coinsCatalog = listOf(
        "מטבע 1 ש״ח (18.0 מ״מ)" to 18.0f,
        "מטבע 2 ש״ח (21.6 מ״מ)" to 21.6f,
        "מטבע 10 ש״ח (23.0 מ״מ)" to 23.0f,
        "מטבע 5 ש״ח (24.0 מ״מ)" to 24.0f,
        "מטבע 10 אגורות (22.0 מ״מ)" to 22.0f
    )

    fun getBaseHardwarePxPerMm(): Float {
        val dm = resources.displayMetrics
        val xDpi = if (dm.xdpi in 100f..700f) dm.xdpi else dm.densityDpi.toFloat()
        val yDpi = if (dm.ydpi in 100f..700f) dm.ydpi else dm.densityDpi.toFloat()
        val avgDpi = (xDpi + yDpi) / 2f
        return avgDpi / 25.4f
    }

    val resTv = createResultTextView().apply { textSize = 16.0f }
    lateinit var updateRingStats: () -> Unit

    val ringCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private var lastTouchX = 0f

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    lastTouchX = event.x
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    val dx = event.x - lastTouchX
                    lastTouchX = event.x
                    val d = resources.displayMetrics.density
                    if (isCoinCalibrationMode) {
                        calibFactor = (calibFactor + (dx / (220f * d))).coerceIn(0.60f, 1.50f)
                        prefs?.edit()?.putFloat("pro_ruler_calib_factor", calibFactor)?.apply()
                    } else {
                        diamMm = (diamMm + (dx / (28f * d))).coerceIn(11.50f, 25.00f)
                    }
                    updateRingStats()
                    return true
                }
            }
            return super.onTouchEvent(event)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val cx = width / 2f
            val cy = height * 0.46f
            val isMin = isMinimalistMode

            val pxPerMm = getBaseHardwarePxPerMm() * calibFactor
            val activeDiameterMm = if (isCoinCalibrationMode) selectedCoinMm else diamMm
            val radiusPx = (activeDiameterMm / 2f) * pxPerMm

            p.style = Paint.Style.STROKE
            p.strokeWidth = 0.8f * d
            p.color = if (isMin) Color.argb(28, 44, 44, 44) else Color.argb(32, 255, 255, 255)
            var mmStep = -15
            while (mmStep <= 15) {
                val offsetPx = mmStep * pxPerMm
                if (abs(offsetPx) < width * 0.44f) {
                    canvas.drawLine(cx + offsetPx, 36f * d, cx + offsetPx, height - 42f * d, p)
                }
                mmStep += 5
            }

            val mainColor = when {
                isCoinCalibrationMode -> Color.parseColor("#FFB300")
                isMin -> MinimalistTheme.ACCENT_SAGE_GREEN
                else -> Color.argb(75, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
            }
            val strokeCol = when {
                isCoinCalibrationMode -> if (isMin) Color.parseColor("#E65100") else Color.parseColor("#FFD54F")
                isMin -> MinimalistTheme.TEXT_CHARCOAL
                else -> primaryColor
            }

            p.style = Paint.Style.FILL
            p.color = if (isCoinCalibrationMode) Color.argb(65, 255, 193, 7) else mainColor
            canvas.drawCircle(cx, cy, radiusPx, p)

            p.style = Paint.Style.STROKE
            p.strokeWidth = 2.4f * d
            p.color = strokeCol
            canvas.drawCircle(cx, cy, radiusPx, p)

            p.strokeWidth = 1.3f * d
            canvas.drawLine(cx - radiusPx, cy - 26f * d, cx - radiusPx, cy + 26f * d, p)
            canvas.drawLine(cx + radiusPx, cy - 26f * d, cx + radiusPx, cy + 26f * d, p)
            canvas.drawLine(cx - radiusPx - 14f * d, cy, cx + radiusPx + 14f * d, cy, p)
            canvas.drawLine(cx, cy - radiusPx - 14f * d, cx, cy + radiusPx + 14f * d, p)

            p.style = Paint.Style.FILL
            p.textAlign = Paint.Align.CENTER
            p.typeface = Typeface.DEFAULT_BOLD
            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE

            val topHint = if (isCoinCalibrationMode) {
                "🪙 מצב כיול: הנח $selectedCoinName וגרור ימינה/שמאלה להתאמה"
            } else {
                "👆 גרור ימינה/שמאלה על העיגול להתאמה מדויקת לקוטר הפנימי של הטבעת"
            }
            CanvasUiHelper.drawFittedText(canvas, topHint, cx, 22f * d, width - 24f * d, 12.2f * d, 8.5f * d, p)

            val bottomTxt = if (isCoinCalibrationMode) {
                "כיול מסך פעיל: ×\u200E${Utils.formatResult(calibFactor.toDouble(), 3)}\u200E  |  קוטר מטבע: \u200E${Utils.formatResult(selectedCoinMm.toDouble(), 1)} מ״מ\u200E"
            } else {
                "קוטר פנימי: \u200E${Utils.formatResult(diamMm.toDouble(), 2)} מ״מ\u200E  |  היקף: \u200E${Utils.formatResult((diamMm * PI).toDouble(), 1)} מ״מ\u200E"
            }
            CanvasUiHelper.drawFittedText(canvas, bottomTxt, cx, height - 12f * d, width - 24f * d, 14.0f * d, 9.5f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(255f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProRingSizerScreen, 22f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProRingSizerScreen, 22f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(22f).toFloat() }
        }
    }

    updateRingStats = {
        ringCanvas.invalidate()
        val circMm = diamMm * PI.toFloat()
        val euIsoSize = circMm.roundToInt()
        val usSize = ((diamMm - 11.63f) / 0.8128f)
        val ukLetters = listOf("F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z")
        val ukIdx = ((usSize - 3f) * 2f).roundToInt().coerceIn(0, ukLetters.lastIndex)
        val effDpi = getBaseHardwarePxPerMm() * 25.4f * calibFactor
        val calibDesc = if (abs(calibFactor - 1.0f) < 0.005f) "חיישן תצוגה אוטומטי" else "מכויל אישית למסך זה (×${Utils.formatResult(calibFactor.toDouble(), 3)})"

        resTv.text = "💍 טבלת מידות טבעת מדויקת:\n" +
                "• קוטר פנימי: \u200E${Utils.formatResult(diamMm.toDouble(), 2)} מ״מ\u200E | היקף אצבע: \u200E${Utils.formatResult(circMm.toDouble(), 1)} מ״מ\u200E\n" +
                "• מידה אירופאית / ישראל (EU / ISO): $euIsoSize\n" +
                "• מידה אמריקאית (US / Canada): \u200E${Utils.formatResult(usSize.toDouble(), 2)}\u200E\n" +
                "• מידה בריטית (UK / Australia): ${ukLetters[ukIdx]}\n" +
                "• קנה-מידה מסך: \u200E${Utils.formatResult(effDpi.toDouble(), 1)} PPI\u200E ($calibDesc)"
    }

    val stepRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    val minusFineBtn = createActionBtn("-0.05 מ״מ", false) {
        if (isCoinCalibrationMode) {
            calibFactor = (calibFactor - 0.004f).coerceIn(0.60f, 1.50f)
            prefs?.edit()?.putFloat("pro_ruler_calib_factor", calibFactor)?.apply()
        } else {
            diamMm = (diamMm - 0.05f).coerceIn(11.50f, 25.00f)
        }
        updateRingStats()
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(4f) } }

    val minusStepBtn = createActionBtn("-0.25 מ״מ", false) {
        if (isCoinCalibrationMode) {
            calibFactor = (calibFactor - 0.015f).coerceIn(0.60f, 1.50f)
            prefs?.edit()?.putFloat("pro_ruler_calib_factor", calibFactor)?.apply()
        } else {
            diamMm = (diamMm - 0.25f).coerceIn(11.50f, 25.00f)
        }
        updateRingStats()
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(4f); rightMargin = dp(4f) } }

    val plusStepBtn = createActionBtn("+0.25 מ״מ", false) {
        if (isCoinCalibrationMode) {
            calibFactor = (calibFactor + 0.015f).coerceIn(0.60f, 1.50f)
            prefs?.edit()?.putFloat("pro_ruler_calib_factor", calibFactor)?.apply()
        } else {
            diamMm = (diamMm + 0.25f).coerceIn(11.50f, 25.00f)
        }
        updateRingStats()
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(4f); rightMargin = dp(4f) } }

    val plusFineBtn = createActionBtn("+0.05 מ״מ", true) {
        if (isCoinCalibrationMode) {
            calibFactor = (calibFactor + 0.004f).coerceIn(0.60f, 1.50f)
            prefs?.edit()?.putFloat("pro_ruler_calib_factor", calibFactor)?.apply()
        } else {
            diamMm = (diamMm + 0.05f).coerceIn(11.50f, 25.00f)
        }
        updateRingStats()
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(4f) } }
    stepRow.addView(minusFineBtn); stepRow.addView(minusStepBtn); stepRow.addView(plusStepBtn); stepRow.addView(plusFineBtn)

    val pickCoinBtn = createActionBtn("🪙 בחר מטבע לכיול: $selectedCoinName", false) {}.apply { visibility = View.GONE }
    val coinCalibToggleBtn = createActionBtn("🪙 כייל מסך מול מטבע ישראלי (1 / 2 / 5 / 10 ש״ח)", false) {
        isCoinCalibrationMode = !isCoinCalibrationMode
        pickCoinBtn.visibility = if (isCoinCalibrationMode) View.VISIBLE else View.GONE
        (it as TextView).text = if (isCoinCalibrationMode) "✅ שמור וסיים כיול מטבע (פעיל כעת)" else "🪙 כייל מסך מול מטבע ישראלי (1 / 2 / 5 / 10 ש״ח)"
        updateRingStats()
    }

    pickCoinBtn.setOnClickListener {
        showItemPicker("בחר מטבע להנחה על המסך", coinsCatalog.map { it.first }) { chosen ->
            val pair = coinsCatalog.find { it.first == chosen } ?: coinsCatalog[2]
            selectedCoinName = pair.first
            selectedCoinMm = pair.second
            pickCoinBtn.text = "🪙 בחר מטבע לכיול: $selectedCoinName"
            updateRingStats()
        }
    }

    val screenDiagIn = createInputBox("📱 או הזן גודל מסך הטלפון באינצ׳ים לכיול אוטומטי (למשל 6.5)")
    val calibByInchBtn = createActionBtn("כייל אוטומטית לפי אינצ׳ים של המסך 📐", false) {
        val diagInches = screenDiagIn.text.toString().toFloatOrNull()
        if (diagInches != null && diagInches in 3.5f..16.0f) {
            val realMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(realMetrics)
            val diagPx = hypot(realMetrics.widthPixels.toDouble(), realMetrics.heightPixels.toDouble()).toFloat()
            val truePxPerMm = (diagPx / diagInches) / 25.4f
            calibFactor = (truePxPerMm / getBaseHardwarePxPerMm()).coerceIn(0.60f, 1.50f)
            prefs?.edit()?.putFloat("pro_ruler_calib_factor", calibFactor)?.apply()
            isCoinCalibrationMode = false
            pickCoinBtn.visibility = View.GONE
            coinCalibToggleBtn.text = "🪙 כייל מסך מול מטבע ישראלי (1 / 2 / 5 / 10 ש״ח)"
            updateRingStats()
            Toast.makeText(this, "✅ המסך כויל במדויק לגודל $diagInches אינץ׳!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "נא להזין גודל מסך תקין באינצ׳ים (למשל 6.1 או 6.7)", Toast.LENGTH_SHORT).show()
        }
    }

    updateRingStats()
    root.addView(ringCanvas)
    root.addView(stepRow)
    root.addView(coinCalibToggleBtn)
    root.addView(pickCoinBtn)
    root.addView(screenDiagIn)
    root.addView(calibByInchBtn)
    root.addView(createPinResultButton(resTv))
    root.addView(resTv)
    scroll.addView(root)
    return scroll
}

// =========================================================================
// 16. ממיר מידות נעליים, בגדים וכובעים (Shoe, Clothing & Hat Sizer - Tab 81)
// =========================================================================
fun MainActivity.createProShoeClothingHatSizeScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("👟 ממיר מידות נעליים, בגדים וכובעים", "המרת מידות בינלאומית לנעליים (EU/US/UK/CM), חולצות/מכנסיים והיקף ראש לכובעים"))

    val footCmIn = createInputBox("👟 אורך כף רגל בס״מ (למשל 26.5 ס״מ = מידה 42)").apply { setText("26.5") }
    val chestCmIn = createInputBox("👕 היקף חזה בס״מ לבגדים (למשל 98)").apply { setText("98") }
    val headCmIn = createInputBox("🎩 היקף ראש בס״מ לכובע (למשל 58)").apply { setText("58") }
    val resTv = createResultTextView().apply { textSize = 15.5f }

    val calcBtn = createActionBtn("חשב מידות נעליים, בגדים וכובע 👟", true) {
        val footCm = (footCmIn.text.toString().toDoubleOrNull() ?: 26.5).coerceIn(15.0, 35.0)
        val chestCm = (chestCmIn.text.toString().toDoubleOrNull() ?: 98.0).coerceIn(70.0, 140.0)
        val headCm = (headCmIn.text.toString().toDoubleOrNull() ?: 58.0).coerceIn(50.0, 66.0)

        val shoeEu = ((footCm + 1.5) * 1.5 * 2).roundToInt() / 2.0
        val shoeUsMen = (shoeEu - 33.0).coerceAtLeast(4.0)
        val shoeUsWomen = shoeUsMen + 1.5
        val shoeUk = (shoeUsMen - 0.5).coerceAtLeast(3.0)

        val (intlCloth, euCloth, usCloth) = when {
            chestCm < 88 -> Triple("XS", "44", "34")
            chestCm < 94 -> Triple("S", "46", "36")
            chestCm < 100 -> Triple("M", "48-50", "38-40")
            chestCm < 106 -> Triple("L", "52", "42")
            chestCm < 114 -> Triple("XL", "54-56", "44-46")
            else -> Triple("XXL", "58+", "48+")
        }

        val hatInch = headCm / 2.54
        val hatUs = headCm / PI / 2.54
        val hatLetter = when {
            headCm < 55.5 -> "S (Small)"
            headCm < 57.5 -> "M (Medium)"
            headCm < 59.5 -> "L (Large)"
            else -> "XL (Extra Large)"
        }

        resTv.text = "👟 מידות נעליים (עבור ${Utils.formatResult(footCm, 1)} ס״מ):\n" +
                "• אירופה / ישראל (EU): \u200E${Utils.formatResult(shoeEu, 1)}\u200E\n" +
                "• ארה״ב גברים (US M): \u200E${Utils.formatResult(shoeUsMen, 1)}\u200E | נשים (US W): \u200E${Utils.formatResult(shoeUsWomen, 1)}\u200E\n" +
                "• בריטניה (UK): \u200E${Utils.formatResult(shoeUk, 1)}\u200E\n\n" +
                "👕 מידות בגדים (חזה ${Utils.formatResult(chestCm, 0)} ס״מ):\n" +
                "• בינלאומי: $intlCloth  |  EU: $euCloth  |  US/UK: $usCloth\n\n" +
                "🎩 מידות כובע (היקף ראש ${Utils.formatResult(headCm, 1)} ס״מ / \u200E${Utils.formatResult(hatInch, 1)}\"\u200E):\n" +
                "• מידה: $hatLetter  |  US Hat: \u200E${Utils.formatResult(hatUs, 2)}\u200E"
    }
    calcBtn.performClick()

    root.addView(footCmIn); root.addView(chestCmIn); root.addView(headCmIn)
    root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

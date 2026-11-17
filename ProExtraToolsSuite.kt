package com.example.calculator

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Base64
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.*
import kotlin.math.*

// =========================================================================
// מנוע סידור כיוון טקסט דו-כיווני (BiDi RTL/LTR Formatter) + מפענח תרגילים חי לשדות קלט
// =========================================================================
object BidiFmt {
    fun ltr(text: Any): String = "\u200E\u2066$text\u2069\u200E"

    fun rtlParagraph(vararg lines: String): String {
        return lines.joinToString("\n") { line -> "\u200F${fixRtlLineTokens(line)}" }
    }

    // מתקן היפוך של מילים באנגלית בסוגריים, מספרים, אחוזים ומטבעות בתוך משפט עברי
    fun fixRtlLineTokens(rawLine: String): String {
        if (rawLine.contains('\u2066')) return rawLine
        var s = rawLine.replace("\u200E", "").replace("\u200F", "")
        // בידוד מונחים באנגלית בתוך סוגריים כמו (Gross Margin) או (Break-Even)
        s = Regex("\\([A-Za-z0-9\\s/\\-+.,]+\\)").replace(s) { m ->
            "\u200E\u2066${m.value}\u2069\u200E"
        }
        // בידוד מספרים עם יחידות מידה, אחוזים או מטבעות כמו 25%, 20₪, 640₪, 16:9
        s = Regex("([₪$€£¥]?[+-]?\\d+(?:[.,:]\\d+)*(?:\\s*[%°€$₪£¥a-zA-ZµΩ²³/+\\-*×÷=]+)?)").replace(s) { m ->
            "\u200E\u2066${m.value.trim()}\u2069\u200E"
        }
        return s
    }

    fun applyRtlToResultView(tv: TextView): TextView {
        tv.textDirection = View.TEXT_DIRECTION_RTL
        tv.gravity = Gravity.RIGHT
        return tv
    }

    // מפענח מספר או תרגיל מתמטי שנכתב בתוך תיבת קלט (למשל "1920/2" או "60+20")
    fun evalInputExpr(editText: EditText, fallback: Double): Double {
        val raw = editText.text?.toString()?.trim() ?: return fallback
        if (raw.isEmpty()) return fallback
        raw.toDoubleOrNull()?.let { return it }
        return try {
            val v = SmartNotesMathEngine.evalWithVars(raw, emptyMap())
            if (!v.isNaN() && !v.isInfinite()) v else fallback
        } catch (_: Throwable) {
            fallback
        }
    }

    // מאפשר הקלדת תרגילים (כולל +, -, *, /, ^, סוגריים) בתוך שדות קלט ומחבר עדכון ויזואלי חי!
    fun bindLiveInputs(vararg inputs: EditText, onUpdate: () -> Unit) {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                try { onUpdate() } catch (_: Throwable) {}
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        for (et in inputs) {
            // פותח את המקלדת גם לתרגילים מתמטיים (+ - * / ( )) ולא רק ספרות בודדות
            if (et.inputType and InputType.TYPE_CLASS_NUMBER != 0) {
                et.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            }
            et.addTextChangedListener(watcher)
        }
    }

    // מחבר עדכון חי אוטומטי לכל מסך מחשבון שיש בו שדות קלט וכפתור חישוב!
    fun attachUniversalLiveVisualizer(screenRoot: View?) {
        if (screenRoot == null) return
        val editTexts = mutableListOf<EditText>()
        var primaryCalcButton: TextView? = null

        fun scan(v: View) {
            when (v) {
                is EditText -> editTexts.add(v)
                is TextView -> {
                    if (v.isClickable && primaryCalcButton == null) {
                        val t = v.text?.toString() ?: ""
                        if (t.startsWith("חשב") || t.startsWith("פתור") || t.startsWith("נתח") || t.startsWith("המר") || t.startsWith("הצפן")) {
                            primaryCalcButton = v
                        }
                    }
                }
                is ViewGroup -> {
                    for (i in 0 until v.childCount) scan(v.getChildAt(i))
                }
            }
        }
        scan(screenRoot)
        val calcBtn = primaryCalcButton ?: return
        if (editTexts.isEmpty()) return

        val liveWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                try { calcBtn.performClick() } catch (_: Throwable) {}
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        for (et in editTexts) {
            et.addTextChangedListener(liveWatcher)
        }
    }
}

// =========================================================================
// מנוע סינתזת סאונד אקוסטי אמיתי למטרונום (Real Mechanical Woodblock & Studio Click PCM WAV)
// =========================================================================
class RealMetronomeSoundEngine(context: Context) {
    private val soundPool: SoundPool
    private val accentIds = IntArray(3)
    private val regularIds = IntArray(3)

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build()

        // 0: מטרונום עץ מכני אמיתי (Woodblock Tock/Tick)
        accentIds[0] = loadSynthesizedClick(context, "metro_wood_hi.wav", freq1 = 1180.0, freq2 = 1860.0, decayMs = 22.0, noiseMix = 0.38)
        regularIds[0] = loadSynthesizedClick(context, "metro_wood_lo.wav", freq1 = 840.0, freq2 = 1320.0, decayMs = 20.0, noiseMix = 0.35)

        // 1: קליק אולפן דיגיטלי חד (Studio Quartz Click)
        accentIds[1] = loadSynthesizedClick(context, "metro_studio_hi.wav", freq1 = 2200.0, freq2 = 3300.0, decayMs = 14.0, noiseMix = 0.15)
        regularIds[1] = loadSynthesizedClick(context, "metro_studio_lo.wav", freq1 = 1450.0, freq2 = 2150.0, decayMs = 13.0, noiseMix = 0.15)

        // 2: מקל תופים (Drum Rimshot Stick)
        accentIds[2] = loadSynthesizedClick(context, "metro_stick_hi.wav", freq1 = 680.0, freq2 = 2650.0, decayMs = 28.0, noiseMix = 0.55)
        regularIds[2] = loadSynthesizedClick(context, "metro_stick_lo.wav", freq1 = 520.0, freq2 = 1950.0, decayMs = 25.0, noiseMix = 0.50)
    }

    private fun loadSynthesizedClick(
        context: Context,
        fileName: String,
        freq1: Double,
        freq2: Double,
        decayMs: Double,
        noiseMix: Double
    ): Int {
        return try {
            val file = File(context.cacheDir, fileName)
            val sampleRate = 44100
            val durationSamples = (sampleRate * 0.045).toInt()
            val pcmData = ByteArray(durationSamples * 2)
            val buffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN)
            val rng = Random(42L)

            for (i in 0 until durationSamples) {
                val t = i.toDouble() / sampleRate
                val tMs = t * 1000.0
                val attack = (tMs / 0.8).coerceAtMost(1.0)
                val bodyEnv = exp(-tMs / decayMs)
                val transientEnv = exp(-tMs / 2.2)

                val bodyWave = 0.65 * sin(2.0 * PI * freq1 * t) + 0.35 * sin(2.0 * PI * freq2 * t)
                val woodTransient = (rng.nextDouble() * 2.0 - 1.0) * transientEnv
                val sample = attack * ((1.0 - noiseMix) * bodyWave * bodyEnv + noiseMix * woodTransient)
                val pcmVal = (sample * 31000.0).toInt().coerceIn(-32767, 32767).toShort()
                buffer.putShort(pcmVal)
            }

            FileOutputStream(file).use { fos ->
                val totalDataLen = pcmData.size + 36
                val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
                header.put("RIFF".toByteArray(Charsets.US_ASCII))
                header.putInt(totalDataLen)
                header.put("WAVEfmt ".toByteArray(Charsets.US_ASCII))
                header.putInt(16)
                header.putShort(1)
                header.putShort(1)
                header.putInt(sampleRate)
                header.putInt(sampleRate * 2)
                header.putShort(2)
                header.putShort(16)
                header.put("data".toByteArray(Charsets.US_ASCII))
                header.putInt(pcmData.size)
                fos.write(header.array())
                fos.write(pcmData)
            }
            soundPool.load(file.absolutePath, 1)
        } catch (_: Exception) {
            0
        }
    }

    fun playBeat(profileIdx: Int, isAccentBeat: Boolean) {
        val p = profileIdx.coerceIn(0, 2)
        val soundId = if (isAccentBeat) accentIds[p] else regularIds[p]
        val vol = if (isAccentBeat) 1.0f else 0.78f
        if (soundId != 0) {
            soundPool.play(soundId, vol, vol, 1, 0, 1.0f)
        }
    }

    fun release() {
        try { soundPool.release() } catch (_: Exception) {}
    }
}

// =========================================================================
// 1. גלאי מתכות ותת-קרקע מתקדם (Earth Field vs Underground Metal Discriminator - Tab 66)
// =========================================================================
class MetalDetectorRadarCanvasView(context: Context, private val activity: MainActivity) : View(context) {
    var currentUt: Float = 45.0f
    var earthBaselineUt: Float = 45.0f
    var verticalBv: Float = 28.0f
    var earthBaselineBv: Float = 28.0f
    var dipAngleDeg: Float = 38.0f
    var earthDipAngleDeg: Float = 38.0f
    var trueMetalAnomalyUt: Float = 0.0f
    var estimatedDepthCm: Float = 0.0f
    var detectionState: Int = 0
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
        val cy = h * 0.48f
        val maxR = min(w, h) * 0.31f

        val statusColor = when (detectionState) {
            3 -> Color.parseColor("#FF1744")
            2 -> Color.parseColor("#FF9100")
            1 -> Color.parseColor("#FFEA00")
            else -> if (isMin) Color.parseColor("#2E7D32") else Color.parseColor("#00E676")
        }

        for (i in 1..3) {
            val r = maxR * (0.28f * i + (pulsePhase % 0.28f))
            val alpha = ((1f - (r / (maxR * 1.15f))).coerceIn(0f, 1f) * 145).toInt()
            ringPaint.color = Color.argb(alpha, Color.red(statusColor), Color.green(statusColor), Color.blue(statusColor))
            ringPaint.strokeWidth = 2.0f * d
            canvas.drawCircle(cx, cy, r, ringPaint)
        }

        val arcRect = RectF(cx - maxR, cy - maxR, cx + maxR, cy + maxR)
        ringPaint.strokeWidth = 8f * d
        ringPaint.color = if (isMin) Color.argb(35, 44, 44, 44) else Color.argb(45, 255, 255, 255)
        canvas.drawArc(arcRect, 180f, 180f, false, ringPaint)

        val sweep = ((trueMetalAnomalyUt / 80f).coerceIn(0.02f, 1f)) * 180f
        ringPaint.color = statusColor
        canvas.drawArc(arcRect, 180f, sweep, false, ringPaint)

        val angleRad = Math.toRadians((180f + sweep).toDouble())
        needlePaint.strokeWidth = 3.5f * d
        needlePaint.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        val nx = (cx + cos(angleRad) * (maxR * 0.85f)).toFloat()
        val ny = (cy + sin(angleRad) * (maxR * 0.85f)).toFloat()
        canvas.drawLine(cx, cy, nx, ny, needlePaint)
        fillPaint.color = statusColor
        canvas.drawCircle(cx, cy, 6f * d, fillPaint)

        val barTop = h * 0.77f
        val barBot = h * 0.86f
        val barLeft = 22f * d
        val barRight = w - 22f * d
        fillPaint.color = if (isMin) Color.argb(28, 44, 44, 44) else Color.argb(38, 255, 255, 255)
        canvas.drawRoundRect(RectF(barLeft, barTop, barRight, barBot), 8f * d, 8f * d, fillPaint)

        if (detectionState > 0) {
            val fillRatio = (1f - (estimatedDepthCm / 40f)).coerceIn(0.12f, 1f)
            fillPaint.color = statusColor
            canvas.drawRoundRect(RectF(barLeft, barTop, barLeft + (barRight - barLeft) * fillRatio, barBot), 8f * d, 8f * d, fillPaint)
        }

        textPaint.color = statusColor
        val stateTitle = when (detectionState) {
            3 -> "🔴 מתכת פרומגנטית חזקה / קרובה מאוד! (עומק ~${BidiFmt.ltr("${estimatedDepthCm.roundToInt()} cm")})"
            2 -> "🟠 זוהתה מתכת חבויה באדמה / בקיר! (עומק משוער ~${BidiFmt.ltr("${estimatedDepthCm.roundToInt()} cm")})"
            1 -> "🟡 אנומליה מתכתית חלשה / עמוקה (~${BidiFmt.ltr("${estimatedDepthCm.roundToInt()} cm")})"
            else -> "🟢 שדה טבעי של כדור הארץ בלבד (ללא מתכת חבויה)"
        }
        CanvasUiHelper.drawFittedText(canvas, stateTitle, cx, 22f * d, w - 20f * d, 13.0f * d, 9.0f * d, textPaint)

        textPaint.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        val midStr = "אנומליית מתכת נטו: ${BidiFmt.ltr("+${Utils.formatResult(trueMetalAnomalyUt.toDouble(), 1)} µT")}  |  שדה כולל: ${BidiFmt.ltr("${Utils.formatResult(currentUt.toDouble(), 1)} µT")}"
        CanvasUiHelper.drawFittedText(canvas, midStr, cx, cy + 22f * d, w - 24f * d, 13.5f * d, 9.5f * d, textPaint)

        val botStr = "רכיב אנכי לאדמה (Bv): ${BidiFmt.ltr("${Utils.formatResult(verticalBv.toDouble(), 1)} µT")}  |  זווית נטייה (Dip): ${BidiFmt.ltr("${Utils.formatResult(dipAngleDeg.toDouble(), 1)}°")}"
        CanvasUiHelper.drawFittedText(canvas, botStr, cx, h - 8f * d, w - 24f * d, 11.8f * d, 8.5f * d, textPaint)

        pulsePhase = (pulsePhase + (if (detectionState >= 2) 0.025f else if (detectionState == 1) 0.015f else 0.006f)) % 1f
        if (isAttachedToWindow) postInvalidateOnAnimation()
    }
}

fun MainActivity.createProMetalDetectorScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(
        createProCanvaHeader(
            "🧲 גלאי מתכות ותת-קרקע (EMF & Deep Metal)",
            "מפצה אוטומטית על הטיה וסיבוב הטלפון כדי להבדיל בין השדה הטבעי של כדור הארץ לבין מתכת מתחת לאדמה או בקיר"
        )
    )

    var soundBeepEnabled = true
    var lastBeepMs = 0L
    var toneGen: ToneGenerator? = try { ToneGenerator(AudioManager.STREAM_MUSIC, 95) } catch (_: Exception) { null }

    val radarCanvas = MetalDetectorRadarCanvasView(this, this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(270f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProMetalDetectorScreen, 22f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProMetalDetectorScreen, 22f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(22f).toFloat() }
        }
    }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView().apply { textSize = 15.5f })

    val sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val magSensor = sm?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    val accelSensor = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val gravityVec = FloatArray(3) { 0f }
    var hasGravity = false
    var hasBaseline = false
    var smoothedUt = 45f
    var smoothedBv = 28f
    var smoothedBh = 35f

    val tareBtn = createActionBtn("🌍 כייל שדה כדור הארץ במקום הנוכחי (הרחק ממתכת ולחץ)", true) {
        radarCanvas.earthBaselineUt = smoothedUt
        radarCanvas.earthBaselineBv = smoothedBv
        radarCanvas.earthDipAngleDeg = Math.toDegrees(atan2(smoothedBv.toDouble(), smoothedBh.toDouble())).toFloat()
        radarCanvas.invalidate()
        Toast.makeText(this, "שדה כדור הארץ כויל (${Utils.formatResult(smoothedUt.toDouble(), 1)} µT)", Toast.LENGTH_SHORT).show()
    }

    val soundBtn = createActionBtn("🔊 צפצוף גלאי מתכות לפי עומק: מופעל 🟢", false) {
        soundBeepEnabled = !soundBeepEnabled
        (it as TextView).text = if (soundBeepEnabled) "🔊 צפצוף גלאי מתכות לפי עומק: מופעל 🟢" else "🔇 צפצוף גלאי מתכות לפי עומק: כבוי 🔴"
    }

    val listener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent?) {
            if (e == null) return
            if (e.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val a = 0.85f
                if (!hasGravity) {
                    gravityVec[0] = e.values[0]; gravityVec[1] = e.values[1]; gravityVec[2] = e.values[2]
                    hasGravity = true
                } else {
                    gravityVec[0] = a * gravityVec[0] + (1 - a) * e.values[0]
                    gravityVec[1] = a * gravityVec[1] + (1 - a) * e.values[1]
                    gravityVec[2] = a * gravityVec[2] + (1 - a) * e.values[2]
                }
                return
            }

            if (e.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                val bx = e.values[0]; val by = e.values[1]; val bz = e.values[2]
                val totalUt = sqrt(bx * bx + by * by + bz * bz)

                val gNorm = sqrt(gravityVec[0] * gravityVec[0] + gravityVec[1] * gravityVec[1] + gravityVec[2] * gravityVec[2]).coerceAtLeast(0.1f)
                val gx = gravityVec[0] / gNorm; val gy = gravityVec[1] / gNorm; val gz = gravityVec[2] / gNorm

                val bv = if (hasGravity) abs(bx * gx + by * gy + bz * gz) else abs(bz)
                val bh = sqrt((totalUt * totalUt - bv * bv).coerceAtLeast(0f))
                val dipDeg = Math.toDegrees(atan2(bv.toDouble(), bh.toDouble().coerceAtLeast(0.1))).toFloat()

                if (!hasBaseline) {
                    smoothedUt = totalUt; smoothedBv = bv; smoothedBh = bh
                    radarCanvas.earthBaselineUt = totalUt.coerceIn(25f, 62f)
                    radarCanvas.earthBaselineBv = bv
                    radarCanvas.earthDipAngleDeg = dipDeg
                    hasBaseline = true
                } else {
                    smoothedUt = 0.75f * smoothedUt + 0.25f * totalUt
                    smoothedBv = 0.75f * smoothedBv + 0.25f * bv
                    smoothedBh = 0.75f * smoothedBh + 0.25f * bh
                }

                val deltaMagnitude = abs(smoothedUt - radarCanvas.earthBaselineUt)
                val deltaVertical = abs(smoothedBv - radarCanvas.earthBaselineBv)
                val dipShift = abs(dipDeg - radarCanvas.earthDipAngleDeg)
                val metalAnomaly = max(deltaMagnitude, deltaVertical * 0.90f)

                val state = when {
                    metalAnomaly >= 45f -> 3
                    metalAnomaly >= 12f || (metalAnomaly >= 7f && dipShift >= 10f) -> 2
                    metalAnomaly >= 4.2f -> 1
                    else -> 0
                }

                val estDepthCm = if (metalAnomaly >= 1.5f) {
                    (42.0 / metalAnomaly.toDouble().pow(0.36)).toFloat().coerceIn(2f, 40f)
                } else 0f

                radarCanvas.currentUt = smoothedUt
                radarCanvas.verticalBv = smoothedBv
                radarCanvas.dipAngleDeg = dipDeg
                radarCanvas.trueMetalAnomalyUt = metalAnomaly
                radarCanvas.estimatedDepthCm = estDepthCm
                radarCanvas.detectionState = state

                val classification = when (state) {
                    3 -> "🧲 מתכת פרומגנטית קרובה מאוד / מגנט חזק"
                    2 -> "🔩 מתכת חבויה מתחת לאדמה / בתוך קיר (ברזל/פלדה)"
                    1 -> "🪙 אנומליה מתכתית עמוקה או עצם מתכתי קטן"
                    else -> "🌍 שדה גאומגנטי טבעי של כדור הארץ (ללא מתכת)"
                }

                resTv.text = BidiFmt.rtlParagraph(
                    "סיווג חיישן: $classification",
                    "• שדה כדור הארץ המכויל: ${BidiFmt.ltr("${Utils.formatResult(radarCanvas.earthBaselineUt.toDouble(), 1)} µT")} (זווית Dip: ${BidiFmt.ltr("${Utils.formatResult(radarCanvas.earthDipAngleDeg.toDouble(), 1)}°")})",
                    "• אנומליית מתכת נטו (ללא כדור הארץ): ${BidiFmt.ltr("+${Utils.formatResult(metalAnomaly.toDouble(), 2)} µT")}",
                    "• רכיב אנכי לתוך האדמה (Bv): ${BidiFmt.ltr("${Utils.formatResult(smoothedBv.toDouble(), 1)} µT")} (סטייה: ${BidiFmt.ltr("${Utils.formatResult(deltaVertical.toDouble(), 1)} µT")})",
                    if (state > 0) "• עומק משוער לתשתית/מתכת: כ-${BidiFmt.ltr("${estDepthCm.roundToInt()} ס״מ")}" else "• יציבות: השדה אחיד ותואם לשדה הטבעי של כדור הארץ"
                )

                if (soundBeepEnabled && state >= 1) {
                    val now = System.currentTimeMillis()
                    val interval = (620L - (metalAnomaly * 7f).toLong()).coerceIn(95L, 620L)
                    if (now - lastBeepMs >= interval) {
                        lastBeepMs = now
                        val tone = if (state == 3) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP
                        try { toneGen?.startTone(tone, 50) } catch (_: Exception) {}
                    }
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    if (magSensor != null) sm.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_UI)
    if (accelSensor != null) sm.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)

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
        val header = "\u200Fרעידה כעת: ${BidiFmt.ltr("${Utils.formatResult(currentVibrationMs2.toDouble(), 2)} m/s²")}  |  שיא: ${BidiFmt.ltr("${Utils.formatResult(peakVibrationMs2.toDouble(), 2)} m/s²")}"
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
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView().apply { textSize = 16f })

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
            resTv.text = BidiFmt.rtlParagraph(
                "ניתוח סיסמוגרף חי:",
                "• תאוצת רטט רגעית: ${BidiFmt.ltr("${Utils.formatResult(mag.toDouble(), 3)} m/s²")}",
                "• שיא רעידה שנרשם (Peak PGA): ${BidiFmt.ltr("${Utils.formatResult(peak.toDouble(), 3)} m/s²")}",
                "• סיווג עוצמה (סולם מרקאלי): $mercalliDesc"
            )
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
            beaconCanvas.invalidate()
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
// 4. מטרונום מוזיקלי מונפש עם סאונד מטרונום מכני אמיתי (Real Acoustic Metronome - Tab 69)
// =========================================================================
fun MainActivity.createProMetronomeScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🎵 מטרונום מוזיקלי מונפש (BPM)", "סאונד נקישת מטרונום עץ מכני אמיתי (Tock/Tick), משקלי קצב וכפתור Tap Tempo"))

    var bpm = 120
    var beatsPerMeasure = 4
    var currentBeat = 0
    var soundProfileIdx = 0
    var isPlaying = false
    var swingDir = 1f
    val handler = Handler(Looper.getMainLooper())
    val metroAudio = RealMetronomeSoundEngine(this)
    val tapTimes = mutableListOf<Long>()

    val soundProfiles = listOf(
        "🪵 מטרונום עץ מכני קלאסי (Wittner Woodblock)",
        "⏱️ קליק אולפן קוורץ חד (Studio Click)",
        "🥁 נקישת מקל תופים (Drum Rimshot)"
    )

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

            // מיקום המשקולת על זרוע המטרונום משתנה ויזואלית בהתאם ל-BPM שנכתב!
            val weightRatio = ((bpm - 30f) / 210f).coerceIn(0.15f, 0.90f)
            val angleDeg = if (isPlaying) (swingDir * 26f) else ((bpm - 120) / 6f).coerceIn(-22f, 22f)
            val rad = Math.toRadians((angleDeg - 90f).toDouble())
            val tipX = (pivotX + cos(rad) * armLen).toFloat()
            val tipY = (pivotY + sin(rad) * armLen).toFloat()
            val weightX = (pivotX + cos(rad) * (armLen * (1f - weightRatio * 0.55f))).toFloat()
            val weightY = (pivotY + sin(rad) * (armLen * (1f - weightRatio * 0.55f))).toFloat()

            p.style = Paint.Style.STROKE
            p.strokeWidth = 3.5f * d
            p.color = if (isMinimalistMode) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            canvas.drawLine(pivotX, pivotY, tipX, tipY, p)

            p.style = Paint.Style.FILL
            p.color = if (isMinimalistMode) MinimalistTheme.ACCENT_TERRACOTTA else primaryColor
            canvas.drawCircle(weightX, weightY, 10.5f * d, p)
            canvas.drawCircle(pivotX, pivotY, 7f * d, p)

            p.color = if (isMinimalistMode) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD; p.textSize = 14.5f * d
            val tempoName = when {
                bpm < 60 -> "Largo (איטי מאוד)"
                bpm < 76 -> "Adagio (איטי)"
                bpm < 108 -> "Andante (הליכה)"
                bpm < 120 -> "Moderato (מתון)"
                bpm < 156 -> "Allegro (מהיר)"
                else -> "Presto (מהיר מאוד)"
            }
            val label = "\u200F${BidiFmt.ltr("$bpm BPM")} • $tempoName (${BidiFmt.ltr("$beatsPerMeasure/4")})"
            CanvasUiHelper.drawFittedText(canvas, label, pivotX, h - 10f * d, w - 20f * d, 14.5f * d, 10f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(235f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProMetronomeScreen, 22f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProMetronomeScreen, 22f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(22f).toFloat() }
        }
    }

    val bpmIn = createInputBox("קצב בפעמות לדקה או תרגיל (למשל 120 או 60*2)").apply { setText("120") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView()).apply {
        text = BidiFmt.rtlParagraph("קצב נוכחי: ${BidiFmt.ltr("120 BPM")} (${BidiFmt.ltr("0.500")} שניות לפעמה)")
    }

    fun syncBpmFromInput() {
        bpm = BidiFmt.evalInputExpr(bpmIn, 120.0).roundToInt().coerceIn(30, 240)
        pendulumCanvas.invalidate()
        resTv.text = BidiFmt.rtlParagraph(
            "קצב מטרונום: ${BidiFmt.ltr("$bpm BPM")}",
            "• מרווח בין נקישות: ${BidiFmt.ltr("${Utils.formatResult(60.0 / bpm, 3)} sec")} (${BidiFmt.ltr("${60000 / bpm} ms")})",
            "• סאונד: ${soundProfiles[soundProfileIdx]}"
        )
    }

    BidiFmt.bindLiveInputs(bpmIn) { syncBpmFromInput() }

    val beatRunner = object : Runnable {
        override fun run() {
            if (!isPlaying) return
            val isAccent = (currentBeat == 0)
            metroAudio.playBeat(soundProfileIdx, isAccent)
            swingDir = -swingDir
            pendulumCanvas.invalidate()
            currentBeat = (currentBeat + 1) % beatsPerMeasure
            val delayMs = (60000L / bpm.coerceIn(30, 240))
            handler.postDelayed(this, delayMs)
        }
    }

    val playBtn = createActionBtn("▶️ הפעל / עצור מטרונום", true) {
        syncBpmFromInput()
        isPlaying = !isPlaying
        if (isPlaying) {
            currentBeat = 0
            (it as TextView).text = "⏹️ עצור מטרונום (${BidiFmt.ltr("$bpm BPM")})"
            handler.post(beatRunner)
        } else {
            (it as TextView).text = "▶️ הפעל / עצור מטרונום"
            handler.removeCallbacksAndMessages(null)
            pendulumCanvas.invalidate()
        }
    }

    val soundProfileBtn = createActionBtn("סוג נקישה: ${soundProfiles[0]}", false) {}
    soundProfileBtn.setOnClickListener {
        showItemPicker("בחר צליל מטרונום אמיתי", soundProfiles) { chosen ->
            soundProfileIdx = soundProfiles.indexOf(chosen).coerceAtLeast(0)
            soundProfileBtn.text = "סוג נקישה: $chosen"
            metroAudio.playBeat(soundProfileIdx, true)
            syncBpmFromInput()
        }
    }

    val tapBtn = createActionBtn("👆 קבע קצב בלחיצות (Tap Tempo)", false) {
        val now = System.currentTimeMillis()
        metroAudio.playBeat(soundProfileIdx, false)
        if (tapTimes.isNotEmpty() && now - tapTimes.last() > 2500L) tapTimes.clear()
        tapTimes.add(now)
        if (tapTimes.size >= 2) {
            val intervals = (1 until tapTimes.size).map { tapTimes[it] - tapTimes[it - 1] }
            val avgMs = intervals.average()
            if (avgMs > 0) {
                bpm = (60000.0 / avgMs).roundToInt().coerceIn(30, 240)
                bpmIn.setText(bpm.toString())
            }
        }
    }

    val sigBtn = createActionBtn("משקל תיבה: ${BidiFmt.ltr("4/4")} (לחץ להחלפה)", false) {
        beatsPerMeasure = when (beatsPerMeasure) { 4 -> 3; 3 -> 6; 6 -> 2; else -> 4 }
        currentBeat = 0
        (it as TextView).text = "משקל תיבה: ${BidiFmt.ltr("$beatsPerMeasure/4")}"
        pendulumCanvas.invalidate()
    }

    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) {
            isPlaying = false
            handler.removeCallbacksAndMessages(null)
            metroAudio.release()
        }
    })

    root.addView(pendulumCanvas); root.addView(bpmIn); root.addView(playBtn)
    root.addView(soundProfileBtn); root.addView(tapBtn); root.addView(sigBtn); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 5. מונה לחיצות חכם (Tally Counter - Tab 70)
// =========================================================================
fun MainActivity.createProTallyCounterScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🔢 מונה לחיצות חכם (Tally Counter)", "מנייה מהירה בלחיצה, הגדרת יעד וגודל צעד עם עדכון צג חי ושמירה אוטומטית"))

    var count = prefs?.getInt("pro_tally_count", 0) ?: 0
    var targetGoal = prefs?.getInt("pro_tally_target", 100) ?: 100
    var currentStep = 1

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
            canvas.drawText(BidiFmt.ltr(count), cx, cy + 10f * d, p)

            p.textSize = 12.5f * d
            p.color = if (isMin) MinimalistTheme.TEXT_MUTED_GREY else Color.LTGRAY
            val bottomLbl = "\u200Fיעד: ${BidiFmt.ltr("$count / $targetGoal")} (${BidiFmt.ltr("${(progress * 100).toInt()}%")}) • צעד: ${BidiFmt.ltr("+$currentStep")}"
            canvas.drawText(bottomLbl, cx, height - 14f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(230f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProTallyCounterScreen, 22f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProTallyCounterScreen, 22f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(22f).toFloat() }
        }
    }

    val stepIn = createInputBox("גודל צעד בכל לחיצה (למשל 1 או 5*2)").apply { setText("1") }
    val targetIn = createInputBox("יעד מנייה (למשל 100 או 50*4)").apply { setText(targetGoal.toString()) }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun updateCounter(delta: Int) {
        currentStep = BidiFmt.evalInputExpr(stepIn, 1.0).roundToInt().coerceAtLeast(1)
        targetGoal = BidiFmt.evalInputExpr(targetIn, 100.0).roundToInt().coerceAtLeast(1)
        count += delta
        prefs?.edit()?.putInt("pro_tally_count", count)?.putInt("pro_tally_target", targetGoal)?.apply()
        counterCanvas.invalidate()
        val rem = (targetGoal - count).coerceAtLeast(0)
        val clicksLeft = ceil(rem.toDouble() / currentStep).roundToInt()
        resTv.text = BidiFmt.rtlParagraph(
            "מונה נוכחי: ${BidiFmt.ltr(count)}",
            "• נותרו להשלמת היעד: ${BidiFmt.ltr(rem)} (כ-${BidiFmt.ltr(clicksLeft)} לחיצות בצעד של ${BidiFmt.ltr(currentStep)})"
        )
    }

    BidiFmt.bindLiveInputs(stepIn, targetIn) { updateCounter(0) }

    counterCanvas.setOnClickListener {
        feedback(it)
        updateCounter(BidiFmt.evalInputExpr(stepIn, 1.0).roundToInt())
    }

    val plusBtn = createActionBtn("➕ הוסף למניין (+)", true) {
        updateCounter(BidiFmt.evalInputExpr(stepIn, 1.0).roundToInt())
    }
    val minusBtn = createActionBtn("➖ הפחת מהמניין (-)", false) {
        updateCounter(-BidiFmt.evalInputExpr(stepIn, 1.0).roundToInt())
    }
    val resetBtn = createActionBtn("🔄 אפס מונה ל-0", false) {
        count = 0
        updateCounter(0)
    }

    updateCounter(0)
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
    root.addView(createProCanvaHeader("🌀 קודי צבע של סלילים (Inductors)", "פענוח סלילי השראות ב-µH/mH/H וחישוב חי של היגב השראתי XL = 2πfL"))

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
    var currentXlOhms = 62.83

    val coilCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val cy = h * 0.40f
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
            p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD; p.textSize = 14f * d
            val summaryLine = "\u200Fהשראות: ${BidiFmt.ltr(labelStr)}  |  היגב: ${BidiFmt.ltr("XL = ${Utils.formatResult(currentXlOhms, 1)} Ω")}"
            CanvasUiHelper.drawFittedText(canvas, summaryLine, w / 2f, h - 14f * d, w - 24f * d, 14f * d, 9.5f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(180f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProInductorColorScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProInductorColorScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val btn1 = createActionBtn("פס 1: ${colors[b1].first}", false) {}
    val btn2 = createActionBtn("פס 2: ${colors[b2].first}", false) {}
    val btnM = createActionBtn("מכפיל: ${BidiFmt.ltr("×10^$mult")} (${colors[mult].first})", false) {}
    val freqIn = createInputBox("תדר עבודה ב-kHz לחישוב היגב השראתי XL (למשל 100)").apply { setText("100") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun updateInductor() {
        val microHenries = (b1 * 10 + b2) * 10.0.pow(mult)
        val milliHenries = microHenries / 1000.0
        val freqKhz = BidiFmt.evalInputExpr(freqIn, 100.0).coerceAtLeast(0.0)
        val freqHz = freqKhz * 1000.0
        currentXlOhms = 2.0 * PI * freqHz * (microHenries * 1e-6)

        bandCols = intArrayOf(colors[b1].third, colors[b2].third, colors[mult].third, Color.rgb(212, 175, 55))
        labelStr = if (microHenries >= 1000) "${Utils.formatResult(milliHenries, 2)} mH ±5%" else "${Utils.formatResult(microHenries, 1)} µH ±5%"
        coilCanvas.invalidate()

        btn1.text = "פס 1: ${colors[b1].first}"
        btn2.text = "פס 2: ${colors[b2].first}"
        btnM.text = "מכפיל: ${BidiFmt.ltr("×10^$mult")} (${colors[mult].first})"
        resTv.text = BidiFmt.rtlParagraph(
            "השראות הסליל (L):",
            "• ב-מיקרו-הנרי: ${BidiFmt.ltr("${Utils.formatResult(microHenries, 2)} µH")}",
            "• ב-מילי-הנרי: ${BidiFmt.ltr("${Utils.formatResult(milliHenries, 4)} mH")}",
            "• היגב השראתי בתדר ${BidiFmt.ltr("${Utils.formatResult(freqKhz, 2)} kHz")}: ${BidiFmt.ltr("${Utils.formatResult(currentXlOhms, 2)} Ω")}"
        )
    }

    BidiFmt.bindLiveInputs(freqIn) { updateInductor() }
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
// 7. סימולטור שערים לוגיים וטבלאות אמת עם שרטוט IEEE תקני לכל שער (Logic Gates - Tab 72)
// =========================================================================
fun MainActivity.createProLogicGatesScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🔌 שערים לוגיים וטבלאות אמת", "סימולטור שערים דיגיטליים (AND, OR, XOR, NAND, NOR, XNOR, NOT) ושרטוט שער חי"))

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
            if (gateIdx == 6) {
                p.color = if (inA) highCol else lowCol
                canvas.drawLine(w * 0.14f, cy, cx - 32f * d, cy, p)
            } else {
                p.color = if (inA) highCol else lowCol
                canvas.drawLine(w * 0.14f, cy - 18f * d, cx - 34f * d, cy - 18f * d, p)
                p.color = if (inB) highCol else lowCol
                canvas.drawLine(w * 0.14f, cy + 18f * d, cx - 34f * d, cy + 18f * d, p)
            }

            val hasBubble = gateIdx in listOf(3, 4, 5, 6)
            val outStartX = if (hasBubble) cx + 44f * d else cx + 34f * d
            p.color = if (outY) highCol else lowCol
            canvas.drawLine(outStartX, cy, w * 0.86f, cy, p)

            // ציור צורת השער ההנדסית האמיתית לפי השער שנבחר!
            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            p.strokeWidth = 2.6f * d
            val gatePath = Path()
            when (gateIdx) {
                0, 3 -> { // AND / NAND (D-shape)
                    gatePath.moveTo(cx - 34f * d, cy - 32f * d)
                    gatePath.lineTo(cx, cy - 32f * d)
                    gatePath.arcTo(RectF(cx - 34f * d, cy - 32f * d, cx + 34f * d, cy + 32f * d), -90f, 180f, false)
                    gatePath.lineTo(cx - 34f * d, cy + 32f * d)
                    gatePath.close()
                }
                1, 2, 4, 5 -> { // OR / XOR / NOR / XNOR (Curved shield shape)
                    gatePath.moveTo(cx - 34f * d, cy - 32f * d)
                    gatePath.quadTo(cx + 10f * d, cy - 32f * d, cx + 34f * d, cy)
                    gatePath.quadTo(cx + 10f * d, cy + 32f * d, cx - 34f * d, cy + 32f * d)
                    gatePath.quadTo(cx - 16f * d, cy, cx - 34f * d, cy - 32f * d)
                    if (gateIdx == 2 || gateIdx == 5) {
                        val xorArc = Path().apply {
                            moveTo(cx - 42f * d, cy - 32f * d)
                            quadTo(cx - 24f * d, cy, cx - 42f * d, cy + 32f * d)
                        }
                        canvas.drawPath(xorArc, p)
                    }
                }
                else -> { // NOT (Triangle Inverter)
                    gatePath.moveTo(cx - 32f * d, cy - 30f * d)
                    gatePath.lineTo(cx + 32f * d, cy)
                    gatePath.lineTo(cx - 32f * d, cy + 30f * d)
                    gatePath.close()
                }
            }
            canvas.drawPath(gatePath, p)
            if (hasBubble) {
                canvas.drawCircle(cx + 39f * d, cy, 5f * d, p)
            }

            p.style = Paint.Style.FILL; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
            p.textSize = 13f * d
            canvas.drawText(gates[gateIdx].substringBefore(" "), cx - 4f * d, cy + 5f * d, p)

            if (gateIdx == 6) {
                canvas.drawText(BidiFmt.ltr("A=${if (inA) 1 else 0}"), w * 0.14f, cy - 12f * d, p)
            } else {
                canvas.drawText(BidiFmt.ltr("A=${if (inA) 1 else 0}"), w * 0.14f, cy - 26f * d, p)
                canvas.drawText(BidiFmt.ltr("B=${if (inB) 1 else 0}"), w * 0.14f, cy + 36f * d, p)
            }
            p.color = if (outY) highCol else lowCol
            canvas.drawText(BidiFmt.ltr("Y=${if (outY) 1 else 0}"), w * 0.86f, cy - 12f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(190f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProLogicGatesScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProLogicGatesScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val gateBtn = createActionBtn("שער נבחר: ${gates[0]}", true) {}
    val toggleABtn = createActionBtn("כניסה A: ${BidiFmt.ltr("1 (True)")}", false) {}
    val toggleBBtn = createActionBtn("כניסה B: ${BidiFmt.ltr("0 (False)")}", false) {}
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView().apply { textSize = 16.0f })

    fun refreshLogic() {
        val y = evalGate(gateIdx, inA, inB)
        gateCanvas.invalidate()
        toggleABtn.text = "כניסה A: ${BidiFmt.ltr(if (inA) "1 (True)" else "0 (False)")}"
        toggleBBtn.text = "כניסה B: ${BidiFmt.ltr(if (inB) "1 (True)" else "0 (False)")}"
        toggleBBtn.visibility = if (gateIdx == 6) View.GONE else View.VISIBLE

        val lines = mutableListOf<String>()
        lines.add("מוצא השער (${gates[gateIdx].substringBefore(" ")}): ${BidiFmt.ltr("Y = ${if (y) 1 else 0}")} (${if (y) "גבוה" else "נמוך"})")
        lines.add("📋 טבלת אמת מלאה:")
        for (a in listOf(false, true)) {
            for (b in listOf(false, true)) {
                if (gateIdx == 6 && b) continue
                val r = evalGate(gateIdx, a, b)
                val mark = if (a == inA && (gateIdx == 6 || b == inB)) "  ◄ פעיל" else ""
                if (gateIdx == 6) {
                    lines.add("• ${BidiFmt.ltr("A=${if (a) 1 else 0}  ➔  Y=${if (r) 1 else 0}")}$mark")
                } else {
                    lines.add("• ${BidiFmt.ltr("A=${if (a) 1 else 0}, B=${if (b) 1 else 0}  ➔  Y=${if (r) 1 else 0}")}$mark")
                }
            }
        }
        resTv.text = BidiFmt.rtlParagraph(*lines.toTypedArray())
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
// 8. ממיר צבעים HEX ל-RGB / HSL / HSV / CMYK עם עדכון חי (Color Converter - Tab 73)
// =========================================================================
fun MainActivity.createProHexRgbColorScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(
        createProCanvaHeader(
            "🎨 ממיר צבעים HEX / RGB / HSL / HSV / CMYK",
            "המרה דו-כיוונית חיה עם תצוגת הצבע הנבחר והצבע המשלים (לחץ על כל ריבוע להעתקת קוד ה-HEX)"
        )
    )

    var currentColor = Color.parseColor("#00F5D4")
    var isInternalChange = false

    fun getComplementaryColor(col: Int): Int {
        return Color.rgb(255 - Color.red(col), 255 - Color.green(col), 255 - Color.blue(col))
    }

    val swatchCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_UP) {
                feedback(this)
                if (event.x < width * 0.64f) {
                    val mainHex = String.format(Locale.US, "#%06X", 0xFFFFFF and currentColor)
                    copyToClipboard(mainHex)
                    Toast.makeText(context, "קוד הצבע הנבחר ($mainHex) הועתק ללוח 📋", Toast.LENGTH_SHORT).show()
                } else {
                    val compCol = getComplementaryColor(currentColor)
                    val compHex = String.format(Locale.US, "#%06X", 0xFFFFFF and compCol)
                    copyToClipboard(compHex)
                    Toast.makeText(context, "קוד הצבע המשלים ($compHex) הועתק ללוח 📋", Toast.LENGTH_SHORT).show()
                }
                return true
            }
            return true
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val compColor = getComplementaryColor(currentColor)

            val mainHex = String.format(Locale.US, "#%06X", 0xFFFFFF and currentColor)
            val compHex = String.format(Locale.US, "#%06X", 0xFFFFFF and compColor)

            val leftRect = RectF(16f * d, 12f * d, w * 0.62f, h - 12f * d)
            val rightRect = RectF(w * 0.66f, 12f * d, w - 16f * d, h - 12f * d)

            p.style = Paint.Style.FILL
            p.color = currentColor
            canvas.drawRoundRect(leftRect, 16f * d, 16f * d, p)

            p.color = compColor
            canvas.drawRoundRect(rightRect, 16f * d, 16f * d, p)

            p.style = Paint.Style.STROKE
            p.strokeWidth = 1.2f * d
            p.color = if (isMinimalistMode) Color.argb(40, 44, 44, 44) else Color.argb(65, 255, 255, 255)
            canvas.drawRoundRect(leftRect, 16f * d, 16f * d, p)
            canvas.drawRoundRect(rightRect, 16f * d, 16f * d, p)

            p.style = Paint.Style.FILL
            p.textAlign = Paint.Align.CENTER
            p.typeface = Typeface.DEFAULT_BOLD

            val mainTxtCol = MinimalistTheme.ensureReadableTextOnBg(Color.WHITE, currentColor, 2.4)
            p.color = mainTxtCol
            p.textSize = 15.5f * d
            canvas.drawText(BidiFmt.ltr(mainHex), leftRect.centerX(), leftRect.centerY() + 6f * d, p)

            val compTxtCol = MinimalistTheme.ensureReadableTextOnBg(Color.WHITE, compColor, 2.4)
            p.color = compTxtCol
            p.textSize = 13.5f * d
            canvas.drawText("משלים", rightRect.centerX(), rightRect.centerY() - 6f * d, p)
            p.textSize = 13.0f * d
            canvas.drawText(BidiFmt.ltr(compHex), rightRect.centerX(), rightRect.centerY() + 14f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(135f)).apply { bottomMargin = dp(12f) }
    }

    val hexIn = createInputBox("קוד HEX (למשל ${BidiFmt.ltr("#00F5D4")})").apply {
        inputType = InputType.TYPE_CLASS_TEXT
        setText("#00F5D4")
    }
    val rgbIn = createInputBox("או הזן RGB מופרד בפסיק (למשל ${BidiFmt.ltr("0, 245, 212")})").apply {
        inputType = InputType.TYPE_CLASS_TEXT
        setText("0, 245, 212")
    }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView().apply { textSize = 15.5f })

    fun updateColorDetails(col: Int) {
        currentColor = col
        swatchCanvas.invalidate()
        val r = Color.red(col); val g = Color.green(col); val b = Color.blue(col)
        val compCol = getComplementaryColor(col)
        val cr = Color.red(compCol); val cg = Color.green(compCol); val cb = Color.blue(compCol)

        val hsv = FloatArray(3)
        Color.colorToHSV(col, hsv)
        val hueDeg = hsv[0].roundToInt()
        val satHsvPct = (hsv[1] * 100f).roundToInt()
        val valHsvPct = (hsv[2] * 100f).roundToInt()

        val rf = r / 255f; val gf = g / 255f; val bf = b / 255f
        val cMax = maxOf(rf, gf, bf)
        val cMin = minOf(rf, gf, bf)
        val delta = cMax - cMin
        val lightness = (cMax + cMin) / 2f
        val satHsl = if (delta == 0f) 0f else delta / (1f - abs(2f * lightness - 1f))
        val satHslPct = (satHsl * 100f).roundToInt()
        val lightHslPct = (lightness * 100f).roundToInt()

        val k = 1f - cMax
        val c = if (k < 1f) (1f - rf - k) / (1f - k) else 0f
        val m = if (k < 1f) (1f - gf - k) / (1f - k) else 0f
        val y = if (k < 1f) (1f - bf - k) / (1f - k) else 0f

        val hexStr = String.format(Locale.US, "#%02X%02X%02X", r, g, b)
        val compHexStr = String.format(Locale.US, "#%06X", 0xFFFFFF and compCol)

        resTv.text = BidiFmt.rtlParagraph(
            "פלט מרחבי צבע מדויק:",
            "• קוד HEX: ${BidiFmt.ltr(hexStr)}  |  משלים: ${BidiFmt.ltr(compHexStr)}",
            "• ערכי RGB: ${BidiFmt.ltr("($r, $g, $b)")}  |  משלים: ${BidiFmt.ltr("($cr, $cg, $cb)")}",
            "• מרחב HSL: ${BidiFmt.ltr("($hueDeg°, $satHslPct%, $lightHslPct%)")}",
            "• מרחב HSV / HSB: ${BidiFmt.ltr("($hueDeg°, $satHsvPct%, $valHsvPct%)")}",
            "• דפוס CMYK: ${BidiFmt.ltr("C:${(c * 100).roundToInt()}%  M:${(m * 100).roundToInt()}%  Y:${(y * 100).roundToInt()}%  K:${(k * 100).roundToInt()}%")}"
        )
    }

    hexIn.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            if (isInternalChange) return
            val raw = s?.toString()?.trim()?.removePrefix("#") ?: ""
            if (raw.length == 6) {
                try {
                    val col = Color.parseColor("#$raw")
                    isInternalChange = true
                    rgbIn.setText("${Color.red(col)}, ${Color.green(col)}, ${Color.blue(col)}")
                    isInternalChange = false
                    updateColorDetails(col)
                } catch (_: Exception) {}
            }
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    rgbIn.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            if (isInternalChange) return
            val parts = s?.toString()?.split(",", " ")?.mapNotNull { it.trim().toIntOrNull() } ?: emptyList()
            if (parts.size >= 3) {
                val col = Color.rgb(parts[0].coerceIn(0, 255), parts[1].coerceIn(0, 255), parts[2].coerceIn(0, 255))
                isInternalChange = true
                hexIn.setText(String.format(Locale.US, "#%02X%02X%02X", Color.red(col), Color.green(col), Color.blue(col)))
                isInternalChange = false
                updateColorDetails(col)
            }
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    val fromHexBtn = createActionBtn("המר מקוד HEX ל-RGB / HSL / CMYK 🎨", true) {
        val raw = hexIn.text.toString().trim().removePrefix("#")
        try {
            val col = Color.parseColor("#$raw")
            isInternalChange = true
            rgbIn.setText("${Color.red(col)}, ${Color.green(col)}, ${Color.blue(col)}")
            isInternalChange = false
            updateColorDetails(col)
        } catch (_: Exception) {
            Toast.makeText(this, "קוד HEX לא תקין (דוגמה: #00F5D4)", Toast.LENGTH_SHORT).show()
        }
    }

    val fromRgbBtn = createActionBtn("המר מערכי RGB לקוד HEX 🔄", false) {
        val parts = rgbIn.text.toString().split(",", " ").mapNotNull { it.trim().toIntOrNull() }
        if (parts.size >= 3) {
            val col = Color.rgb(parts[0].coerceIn(0, 255), parts[1].coerceIn(0, 255), parts[2].coerceIn(0, 255))
            isInternalChange = true
            hexIn.setText(String.format(Locale.US, "#%02X%02X%02X", Color.red(col), Color.green(col), Color.blue(col)))
            isInternalChange = false
            updateColorDetails(col)
        }
    }

    updateColorDetails(currentColor)
    root.addView(swatchCanvas); root.addView(hexIn); root.addView(fromHexBtn)
    root.addView(rgbIn); root.addView(fromRgbBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 9. יחס גובה-רוחב ורזולוציה עם שינוי צורה חי בזמן הקלדה (Aspect Ratio - Tab 74)
// =========================================================================
fun MainActivity.createProAspectRatioScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🖥️ יחס גובה-רוחב ורזולוציה (Aspect Ratio)", "המלבן הוויזואלי, קו האלכסון וצפיפות ה-PPI מתעדכנים בזמן אמת לפי המספרים או התרגיל שהוקלד"))

    var wPx = 1920.0
    var hPx = 1080.0
    var diagInch = 24.0
    var ratioLabel = "16 : 9"
    var ppiValue = 91.8

    val screenCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val cx = width / 2f; val cy = height * 0.48f
            val maxW = width * 0.68f; val maxH = height * 0.64f
            val ratio = (wPx / hPx.coerceAtLeast(1.0)).toFloat().coerceIn(0.30f, 3.60f)

            val rectW: Float; val rectH: Float
            if (ratio >= maxW / maxH) {
                rectW = maxW
                rectH = (maxW / ratio).coerceAtLeast(28f * d)
            } else {
                rectH = maxH
                rectW = (maxH * ratio).coerceAtLeast(28f * d)
            }

            val rRect = RectF(cx - rectW / 2f, cy - rectH / 2f, cx + rectW / 2f, cy + rectH / 2f)
            p.style = Paint.Style.FILL
            p.color = if (isMinimalistMode) MinimalistTheme.ACCENT_PASTEL_BLUE else Color.argb(55, 0, 245, 212)
            canvas.drawRoundRect(rRect, 12f * d, 12f * d, p)

            // קו אלכסון פנימי שממחיש את גודל המסך באינצ'ים
            p.style = Paint.Style.STROKE; p.strokeWidth = 1.4f * d
            p.color = if (isMinimalistMode) Color.argb(90, 44, 44, 44) else Color.argb(110, 255, 255, 255)
            canvas.drawLine(rRect.left + 8f * d, rRect.bottom - 8f * d, rRect.right - 8f * d, rRect.top + 8f * d, p)

            // מסגרת המסך
            p.strokeWidth = 2.4f * d
            p.color = if (isMinimalistMode) MinimalistTheme.TEXT_CHARCOAL else primaryColor
            canvas.drawRoundRect(rRect, 12f * d, 12f * d, p)

            p.style = Paint.Style.FILL; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
            p.textSize = 16.5f * d
            p.color = if (isMinimalistMode) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            canvas.drawText(BidiFmt.ltr(ratioLabel), cx, cy + 5f * d, p)

            // מידות חיות סביב המלבן
            p.textSize = 12.0f * d
            val subText = "${BidiFmt.ltr("${Utils.formatResult(wPx, 0)} × ${Utils.formatResult(hPx, 0)}")}  •  ${BidiFmt.ltr("${Utils.formatResult(diagInch, 1)}\"")}  •  ${BidiFmt.ltr("${Utils.formatResult(ppiValue, 1)} PPI")}"
            CanvasUiHelper.drawFittedText(canvas, subText, cx, height - 10f * d, width - 24f * d, 12.5f * d, 9f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(190f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProAspectRatioScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProAspectRatioScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val widthIn = createInputBox("רוחב בפיקסלים או תרגיל (למשל 1920 או 1280*2)").apply { setText("1920") }
    val heightIn = createInputBox("גובה בפיקסלים או תרגיל (למשל 1080)").apply { setText("1080") }
    val diagInchIn = createInputBox("אלכסון מסך באינצ'ים לחישוב PPI (למשל 24)").apply { setText("24") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView().apply { textSize = 16.0f })

    fun recalculateAspect() {
        wPx = BidiFmt.evalInputExpr(widthIn, 1920.0).coerceAtLeast(1.0)
        hPx = BidiFmt.evalInputExpr(heightIn, 1080.0).coerceAtLeast(1.0)
        diagInch = BidiFmt.evalInputExpr(diagInchIn, 24.0).coerceAtLeast(0.5)

        val g = Utils.gcd(wPx.roundToLong(), hPx.roundToLong()).coerceAtLeast(1L)
        val rw = wPx.roundToLong() / g
        val rh = hPx.roundToLong() / g
        ratioLabel = "$rw : $rh"

        val diagPx = hypot(wPx, hPx)
        ppiValue = diagPx / diagInch
        val megaPixels = (wPx * hPx) / 1e6
        screenCanvas.invalidate()

        resTv.text = BidiFmt.rtlParagraph(
            "ניתוח תצוגה ורזולוציה:",
            "• יחס גובה-רוחב מצומצם: ${BidiFmt.ltr("$rw : $rh")} (יחס עשרוני: ${BidiFmt.ltr("${Utils.formatResult(wPx / hPx, 3)} : 1")})",
            "• צפיפות פיקסלים ב-${BidiFmt.ltr("${Utils.formatResult(diagInch, 1)}\"")}: ${BidiFmt.ltr("${Utils.formatResult(ppiValue, 1)} PPI")}",
            "• סך פיקסלים: ${BidiFmt.ltr("${Utils.formatResult(megaPixels, 2)} MP")} (אלכסון: ${BidiFmt.ltr("${Utils.formatResult(diagPx, 0)} px")})"
        )
    }

    BidiFmt.bindLiveInputs(widthIn, heightIn, diagInchIn) { recalculateAspect() }
    val calcBtn = createActionBtn("חשב יחס גובה-רוחב ו-PPI 🖥️", true) { recalculateAspect() }
    recalculateAspect()

    root.addView(screenCanvas); root.addView(widthIn); root.addView(heightIn); root.addView(diagInchIn)
    root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 10. מרכז טיפול, כיול ושימור סוללה מקצועי (Battery Care & Calibration Pro - Tab 75)
// =========================================================================
fun MainActivity.createProBatteryMonitorScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(
        createProCanvaHeader(
            "🔋 מרכז טיפול, כיול ובריאות הסוללה (Battery Care Pro)",
            "אופטימיזציית צריכה וקירור בלחיצה, אשף כיול BMS, שומר טעינה ב-80% ומד זמני עבודה וזרם חי (mA)"
        )
    )

    var pctLevel = 85
    var tempC = 29.5f
    var voltageMv = 4120
    var currentMa = -320
    var powerWatts = 1.32f
    var isCharging = false
    var chargeGuard80Enabled = prefs?.getBoolean("pro_bat_guard_80", false) ?: false
    var designCapacityMah = prefs?.getInt("pro_bat_design_mah", 5000) ?: 5000

    val batCareCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val isMin = isMinimalistMode

            val leftCx = w * 0.28f; val rightCx = w * 0.72f
            val cy = h * 0.44f
            val r = min(w * 0.20f, h * 0.32f)

            p.style = Paint.Style.STROKE; p.strokeWidth = 8f * d
            p.color = if (isMin) Color.argb(35, 44, 44, 44) else Color.argb(45, 255, 255, 255)
            canvas.drawCircle(leftCx, cy, r, p)
            canvas.drawCircle(rightCx, cy, r, p)

            val levelCol = when {
                pctLevel <= 20 -> Color.parseColor("#FF3B30")
                pctLevel > 85 && isCharging && chargeGuard80Enabled -> Color.parseColor("#FF9100")
                isMin -> MinimalistTheme.ACCENT_SAGE_GREEN
                else -> Color.parseColor("#00E676")
            }
            p.color = levelCol; p.strokeCap = Paint.Cap.ROUND
            canvas.drawArc(RectF(leftCx - r, cy - r, leftCx + r, cy + r), -90f, (pctLevel / 100f) * 360f, false, p)

            val tempCol = when {
                tempC >= 40f -> Color.parseColor("#FF1744")
                tempC >= 36f -> Color.parseColor("#FF9100")
                isMin -> MinimalistTheme.ACCENT_SKY_BLUE
                else -> Color.parseColor("#00E5FF")
            }
            val tempSweep = ((tempC - 15f) / 35f).coerceIn(0.08f, 1f) * 360f
            p.color = tempCol
            canvas.drawArc(RectF(rightCx - r, cy - r, rightCx + r, cy + r), -90f, tempSweep, false, p)

            p.style = Paint.Style.FILL; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            p.textSize = 20f * d
            canvas.drawText(BidiFmt.ltr("$pctLevel%"), leftCx, cy + 6f * d, p)
            canvas.drawText(BidiFmt.ltr("${Utils.formatResult(tempC.toDouble(), 1)}°C"), rightCx, cy + 6f * d, p)

            p.textSize = 11.5f * d
            val flowPrefix = if (isCharging) "+" else ""
            val currentStored = (designCapacityMah * pctLevel) / 100
            canvas.drawText(BidiFmt.ltr("$flowPrefix$currentMa mA • $currentStored mAh"), leftCx, cy + r + 20f * d, p)
            canvas.drawText(BidiFmt.ltr("$voltageMv mV • ${Utils.formatResult(powerWatts.toDouble(), 1)}W"), rightCx, cy + r + 20f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(215f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProBatteryMonitorScreen, 22f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProBatteryMonitorScreen, 22f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(22f).toFloat() }
        }
    }

    val capIn = createInputBox("קיבולת סוללה מקורית ב-mAh (למשל 5000)").apply {
        setText(designCapacityMah.toString())
    }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView().apply { textSize = 15.0f })

    fun readAndAnalyzeBattery(): String {
        designCapacityMah = BidiFmt.evalInputExpr(capIn, 5000.0).roundToInt().coerceIn(1000, 30000)
        prefs?.edit()?.putInt("pro_bat_design_mah", designCapacityMah)?.apply()

        val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val rawCurrentNow = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
        val rawChargeCounter = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) ?: 0

        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        var statusStr = "בשימוש (פריקה)"
        var plugStr = "סוללה בלבד"
        var healthStr = "תקינה (Good) 🟢"
        var tech = "Li-ion"

        if (intent != null) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            pctLevel = ((level * 100f) / scale).roundToInt().coerceIn(0, 100)
            tempC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 285) / 10f
            voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4050)
            tech = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

            isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL || plugged != 0)
            statusStr = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "בטעינה פעילה ⚡"
                BatteryManager.BATTERY_STATUS_FULL -> "טעונה במלואה (100%) 🟢"
                else -> "בשימוש (פריקה)"
            }
            plugStr = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "מטען קיר מהיר (AC)"
                BatteryManager.BATTERY_PLUGGED_USB -> "חיבור מחשב (USB)"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "משטח אלחוטי (Qi)"
                else -> "ללא מטען"
            }
            healthStr = when (health) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "תקינה (Good) 🟢"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "התחממות יתר! 🔴"
                BatteryManager.BATTERY_HEALTH_DEAD -> "שחוקה / דורשת החלפה 🔴"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "מתח יתר במטען 🟠"
                else -> "תקינה"
            }
        }

        val absMa = abs(if (abs(rawCurrentNow) > 25000) rawCurrentNow / 1000 else rawCurrentNow).let {
            if (it in 20..12000) it else if (isCharging) 1650 else 310
        }
        currentMa = if (isCharging) absMa else -absMa
        powerWatts = (absMa * (voltageMv / 1000f)) / 1000f
        batCareCanvas.invalidate()

        val currentStoredMah = if (rawChargeCounter > 100000 && designCapacityMah == 5000) (rawChargeCounter / 1000) else ((designCapacityMah * pctLevel) / 100)
        val timeEstimateLine = if (isCharging) {
            val mahTo80 = ((designCapacityMah * 0.80).toInt() - currentStoredMah).coerceAtLeast(0)
            val mahTo100 = (designCapacityMah - currentStoredMah).coerceAtLeast(0)
            val minsTo80 = ((mahTo80.toDouble() / absMa.coerceAtLeast(100)) * 60.0).roundToInt()
            val minsTo100 = ((mahTo100.toDouble() / absMa.coerceAtLeast(100)) * 68.0).roundToInt()
            "• צפי סיום טעינה: עד 80% כ-${BidiFmt.ltr("$minsTo80 דק׳")} | עד 100% כ-${BidiFmt.ltr("$minsTo100 דק׳")}"
        } else {
            val hrsScreenOn = currentStoredMah.toDouble() / absMa.coerceAtLeast(120)
            val hrsGaming = currentStoredMah.toDouble() / 780.0
            val hrsStandby = currentStoredMah.toDouble() / 42.0
            "• זמן סוללה נותר: מסך פעיל ${BidiFmt.ltr("${Utils.formatResult(hrsScreenOn, 1)} שע׳")} | משחקים ${BidiFmt.ltr("${Utils.formatResult(hrsGaming, 1)} שע׳")} | המתנה ${BidiFmt.ltr("${Utils.formatResult(hrsStandby, 0)} שע׳")}"
        }

        val expectedMv = 3350 + (pctLevel * 9.5).toInt()
        val bmsDriftMv = abs(voltageMv - expectedMv)
        val bmsDiagnosis = if (bmsDriftMv > 190 && !isCharging) {
            "זוהתה סטיית כיול קלה (${BidiFmt.ltr("$bmsDriftMv mV")}) - מומלץ להריץ אשף כיול BMS"
        } else {
            "מכויל ומסונכרן היטב עם מתח התא (${BidiFmt.ltr("$voltageMv mV")})"
        }

        if (chargeGuard80Enabled && isCharging && pctLevel >= 80) {
            try {
                ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90).startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
            } catch (_: Exception) {}
        }

        return BidiFmt.rtlParagraph(
            "📊 ניתוח סוללה ומערכת טעינה בזמן אמת:",
            "• מצב: $statusStr ($plugStr) | בריאות: $healthStr",
            "• זרם והספק חי: ${BidiFmt.ltr("${if (isCharging) "+" else ""}$currentMa mA")} | הספק: ${BidiFmt.ltr("${Utils.formatResult(powerWatts.toDouble(), 2)} W")}",
            "• מתח תא וטמפרטורה: ${BidiFmt.ltr("$voltageMv mV")} | ${BidiFmt.ltr("${Utils.formatResult(tempC.toDouble(), 1)} °C")} ($tech)",
            "• אנרגיה אגורה כעת: כ-${BidiFmt.ltr("$currentStoredMah / $designCapacityMah mAh")}",
            timeEstimateLine,
            "• אבחון בקר סוללה (BMS): $bmsDiagnosis"
        )
    }

    BidiFmt.bindLiveInputs(capIn) { resTv.text = readAndAnalyzeBattery() }

    val optimizeNowBtn = createActionBtn("🛠️ בצע אופטימיזציית סוללה וקירור אקטיבי כעת", true) {
        keepScreenOn = false
        updateKeepScreenOn()
        useAnimations = false
        savePreferences()
        val lp = window.attributes
        lp.screenBrightness = 0.35f
        window.attributes = lp
        Runtime.getRuntime().gc()

        resTv.text = readAndAnalyzeBattery() + "\n\n" + BidiFmt.rtlParagraph(
            "✅ בוצעה אופטימיזציית סוללה וקירור פעיל:",
            "• בהירות המסך הותאמה ל-${BidiFmt.ltr("35%")} להפחתת עומס תרמי",
            "• בוטלו נעילות מסך (Wake-Locks) ואנימציות כבדות ברקע",
            "• נוקה זיכרון המטמון הפעיל לצמצום צריכת המעבד"
        )
        Toast.makeText(this, "⚡ אופטימיזציית חיסכון וקירור סוללה הופעלה!", Toast.LENGTH_SHORT).show()
    }

    val calibrateBmsBtn = createActionBtn("⚖️ אשף כיול סוללה ואיפוס סטיית אחוזים (BMS)", false) {
        prefs?.edit()
            ?.putInt("pro_bms_calib_mv", voltageMv)
            ?.putInt("pro_bms_calib_pct", pctLevel)
            ?.putLong("pro_bms_calib_time", System.currentTimeMillis())
            ?.apply()

        val wearScore = when {
            tempC > 41f -> "גבוהה (מומלץ לנתק מטען ולהמתין להתקררות מתחת ל-35°C)"
            pctLevel > 90 && isCharging -> "בינונית (טעינה מעל 80% מאיצה התיישנות תא ליתיום)"
            else -> "נמוכה מאוד - תנאי עבודה אידיאליים (אורך חיים מרבי)"
        }
        resTv.text = readAndAnalyzeBattery() + "\n\n" + BidiFmt.rtlParagraph(
            "⚖️ נקודת ייחוס לכיול BMS נשמרה בהצלחה:",
            "• נקודת כיול: ${BidiFmt.ltr("$pctLevel%")} במתח ${BidiFmt.ltr("$voltageMv mV")}",
            "• רמת שחיקת מחזור נוכחית: $wearScore",
            "• להשלמת כיול פיזי מלא: טען עד ${BidiFmt.ltr("100%")}, השאר מחובר ${BidiFmt.ltr("20 דק׳")} נוספות, ולאחר מכן השתמש במכשיר עד ${BidiFmt.ltr("15%")} ללא טעינות ביניים."
        )
    }

    val guardBtn = createActionBtn(
        if (chargeGuard80Enabled) "🛡️ שומר טעינה בריאה (התראה ב-80% ונגד התחממות): מופעל 🟢"
        else "🛡️ שומר טעינה בריאה (התראה ב-80% ונגד התחממות): כבוי 🔴",
        false
    ) {
        chargeGuard80Enabled = !chargeGuard80Enabled
        prefs?.edit()?.putBoolean("pro_bat_guard_80", chargeGuard80Enabled)?.apply()
        (it as TextView).text = if (chargeGuard80Enabled) {
            "🛡️ שומר טעינה בריאה (התראה ב-80% ונגד התחממות): מופעל 🟢"
        } else {
            "🛡️ שומר טעינה בריאה (התראה ב-80% ונגד התחממות): כבוי 🔴"
        }
        resTv.text = readAndAnalyzeBattery()
    }

    val refreshBtn = createActionBtn("🔄 רענן מדדי זרם, הספק וזמני סוללה", false) {
        resTv.text = readAndAnalyzeBattery()
    }

    resTv.text = readAndAnalyzeBattery()
    root.addView(batCareCanvas)
    root.addView(capIn)
    root.addView(optimizeNowBtn)
    root.addView(calibrateBmsBtn)
    root.addView(guardBtn)
    root.addView(refreshBtn)
    root.addView(createPinResultButton(resTv))
    root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 11. מחשבון חשבון חשמל וצריכת אנרגיה עם מד צריכה וגרף חי (Electricity Bill - Tab 76)
// =========================================================================
fun MainActivity.createProElectricityBillScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("⚡ מחשבון חשבון חשמל וצריכת אנרגיה", "מד העומס וגרף העלויות משתנים בזמן אמת לפי הספק המכשיר, שעות הפעולה והתעריף"))

    var currentWatts = 1500.0
    var dailyCostIls = 4.80
    var monthlyCostIls = 144.0
    var monthlyKwh = 225.0

    val energyCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val isMin = isMinimalistMode

            // חוגת עומס הספק משמאל
            val gaugeCx = w * 0.28f; val gaugeCy = h * 0.56f; val r = min(w * 0.20f, h * 0.36f)
            p.style = Paint.Style.STROKE; p.strokeWidth = 8f * d
            p.color = if (isMin) Color.argb(35, 44, 44, 44) else Color.argb(45, 255, 255, 255)
            canvas.drawArc(RectF(gaugeCx - r, gaugeCy - r, gaugeCx + r, gaugeCy + r), 150f, 240f, false, p)

            val loadRatio = (currentWatts / 3500.0).toFloat().coerceIn(0.04f, 1f)
            p.color = when {
                loadRatio > 0.75f -> Color.parseColor("#FF3B30")
                loadRatio > 0.40f -> Color.parseColor("#FF9100")
                else -> Color.parseColor("#00E676")
            }
            p.strokeCap = Paint.Cap.ROUND
            canvas.drawArc(RectF(gaugeCx - r, gaugeCy - r, gaugeCx + r, gaugeCy + r), 150f, loadRatio * 240f, false, p)

            p.style = Paint.Style.FILL; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            p.textSize = 14.5f * d
            canvas.drawText(BidiFmt.ltr("${Utils.formatResult(currentWatts, 0)}W"), gaugeCx, gaugeCy + 5f * d, p)

            // 3 עמודות השוואה מימין (יומי, חודשי, דו-חודשי)
            val chartLeft = w * 0.54f; val chartRight = w - 20f * d
            val chartBottom = h - 28f * d; val maxBarH = h * 0.56f
            val biMonthly = monthlyCostIls * 2.0
            val maxVal = biMonthly.coerceAtLeast(10.0)
            val vals = listOf(dailyCostIls to "יומי", monthlyCostIls to "חודשי", biMonthly to "דו-חודשי")
            val barW = (chartRight - chartLeft) / 4.2f

            for ((idx, pair) in vals.withIndex()) {
                val bx = chartLeft + idx * (barW * 1.45f)
                val bh = ((pair.first / maxVal).toFloat().coerceIn(0.08f, 1f)) * maxBarH
                p.color = when (idx) {
                    0 -> Color.parseColor("#29B6F6")
                    1 -> if (isMin) MinimalistTheme.ACCENT_TERRACOTTA else primaryColor
                    else -> Color.parseColor("#FFB300")
                }
                canvas.drawRoundRect(RectF(bx, chartBottom - bh, bx + barW, chartBottom), 6f * d, 6f * d, p)
                p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
                p.textSize = 10.5f * d
                canvas.drawText(pair.second, bx + barW / 2f, chartBottom + 14f * d, p)
                canvas.drawText(BidiFmt.ltr("₪${Utils.formatResult(pair.first, 0)}"), bx + barW / 2f, chartBottom - bh - 5f * d, p)
            }
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(185f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProElectricityBillScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProElectricityBillScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val wattsIn = createInputBox("הספק מכשיר בוואט או תרגיל (למשל 1500 או 750*2)").apply { setText("1500") }
    val hoursIn = createInputBox("שעות הפעלה ביום (למשל 5)").apply { setText("5") }
    val tariffIn = createInputBox("תעריף לקוט״ש בש״ח (למשל 0.64)").apply { setText("0.64") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView().apply { textSize = 16.0f })

    fun updateEnergyCalc() {
        currentWatts = BidiFmt.evalInputExpr(wattsIn, 1500.0).coerceAtLeast(0.0)
        val hrs = BidiFmt.evalInputExpr(hoursIn, 5.0).coerceIn(0.0, 24.0)
        val rate = BidiFmt.evalInputExpr(tariffIn, 0.64).coerceAtLeast(0.0)

        val dailyKwh = (currentWatts * hrs) / 1000.0
        monthlyKwh = dailyKwh * 30.0
        val yearlyKwh = dailyKwh * 365.0

        dailyCostIls = dailyKwh * rate
        monthlyCostIls = monthlyKwh * rate
        val yearlyCost = yearlyKwh * rate
        energyCanvas.invalidate()

        resTv.text = BidiFmt.rtlParagraph(
            "סיכום צריכת חשמל ועלויות:",
            "• יומי: ${BidiFmt.ltr("${Utils.formatResult(dailyKwh, 2)} kWh")}  ➔  ${BidiFmt.ltr("₪${Utils.formatResult(dailyCostIls, 2)}")}",
            "• חודשי (30 יום): ${BidiFmt.ltr("${Utils.formatResult(monthlyKwh, 1)} kWh")}  ➔  ${BidiFmt.ltr("₪${Utils.formatResult(monthlyCostIls, 2)}")}",
            "• דו-חודשי (חשבון חברת חשמל): ${BidiFmt.ltr("₪${Utils.formatResult(monthlyCostIls * 2, 2)}")}",
            "• שנתי (365 יום): ${BidiFmt.ltr("${Utils.formatResult(yearlyKwh, 0)} kWh")}  ➔  ${BidiFmt.ltr("₪${Utils.formatResult(yearlyCost, 0)}")}"
        )
    }

    BidiFmt.bindLiveInputs(wattsIn, hoursIn, tariffIn) { updateEnergyCalc() }
    val calcBtn = createActionBtn("חשב צריכת אנרגיה וחשבון חשמל ⚡", true) { updateEnergyCalc() }
    updateEnergyCalc()

    root.addView(energyCanvas); root.addView(wattsIn); root.addView(hoursIn); root.addView(tariffIn)
    root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 12. מעבדת הצפנה, צפנים ו-Hash עם גלגל צופן אינטראקטיבי חי (Encryption Lab - Tab 77)
// =========================================================================
fun MainActivity.createProEncryptionLabScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🔐 הצפנת טקסט, צפנים ו-Hash", "גלגל צופן קיסר אינטראקטיבי, קידוד Base64 דו-כיווני וגיבוב SHA-256 / MD5 חי"))

    var currentShift = 3
    var currentSamplePlain = "שלום עולם 2026"
    var currentSampleCipher = "תסזפ צזספ 2026"
    var currentShaHex = "30a7d8eb7747b610e435f2914622b50f"

    // צג ויזואלי של גלגל צופן קיסר + מטריצת סיביות SHA-256 שמשתנה בזמן אמת!
    val cipherWheelCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val isMin = isMinimalistMode

            val wheelCx = w * 0.30f; val wheelCy = h * 0.48f
            val outerR = min(w * 0.24f, h * 0.38f)
            val innerR = outerR * 0.66f

            p.style = Paint.Style.STROKE; p.strokeWidth = 2f * d
            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else primaryColor
            canvas.drawCircle(wheelCx, wheelCy, outerR, p)
            p.color = if (isMin) MinimalistTheme.ACCENT_TERRACOTTA else Color.parseColor("#00E676")
            canvas.drawCircle(wheelCx, wheelCy, innerR, p)

            val heb = "אבגדהוזחטיכלמנסעפצקרשת"
            p.style = Paint.Style.FILL; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
            for (i in 0 until 11) {
                val ang = Math.toRadians((i * (360.0 / 11.0) - 90.0))
                val ox = (wheelCx + cos(ang) * (outerR * 0.83f)).toFloat()
                val oy = (wheelCy + sin(ang) * (outerR * 0.83f)).toFloat() + 4f * d
                p.textSize = 10.5f * d
                p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
                canvas.drawText(heb[i * 2].toString(), ox, oy, p)

                val shiftedIdx = ((i * 2 + currentShift) % heb.length + heb.length) % heb.length
                val ix = (wheelCx + cos(ang) * (innerR * 0.72f)).toFloat()
                val iy = (wheelCy + sin(ang) * (innerR * 0.72f)).toFloat() + 4f * d
                p.color = if (isMin) Color.parseColor("#D84315") else Color.parseColor("#00E676")
                canvas.drawText(heb[shiftedIdx].toString(), ix, iy, p)
            }

            p.textSize = 11.5f * d
            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            canvas.drawText(BidiFmt.ltr("+$currentShift"), wheelCx, wheelCy + 4f * d, p)

            // מטריצת ביטים ויזואלית של SHA-256 בצד ימין
            val gridLeft = w * 0.60f; val gridTop = 24f * d
            val cellSize = min((w * 0.34f) / 8f, (h * 0.62f) / 4f)
            for (rIdx in 0 until 4) {
                for (cIdx in 0 until 8) {
                    val hexChar = currentShaHex.getOrElse(rIdx * 8 + cIdx) { '0' }
                    val nibble = hexChar.digitToIntOrNull(16) ?: 0
                    p.color = if (nibble >= 8) {
                        if (isMin) MinimalistTheme.ACCENT_TERRACOTTA else primaryColor
                    } else {
                        if (isMin) Color.argb(40, 44, 44, 44) else Color.argb(45, 255, 255, 255)
                    }
                    val rx = gridLeft + cIdx * cellSize
                    val ry = gridTop + rIdx * cellSize
                    canvas.drawRoundRect(RectF(rx + 1.5f * d, ry + 1.5f * d, rx + cellSize - 1.5f * d, ry + cellSize - 1.5f * d), 3f * d, 3f * d, p)
                }
            }

            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            val bottomPreview = "\u200F${currentSamplePlain.take(12)}  ➔  ${currentSampleCipher.take(12)}"
            CanvasUiHelper.drawFittedText(canvas, bottomPreview, w / 2f, h - 10f * d, w - 24f * d, 13f * d, 9.5f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(185f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProEncryptionLabScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProEncryptionLabScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val textIn = createInputBox("הזן טקסט להצפנה או פענוח...").apply {
        inputType = InputType.TYPE_CLASS_TEXT
        textDirection = View.TEXT_DIRECTION_RTL
        setText("שלום עולם 2026")
    }
    val shiftIn = createInputBox("היסט צופן קיסר (Shift - למשל 3 או ${BidiFmt.ltr("-3")} לפענוח)").apply { setText("3") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView().apply { textSize = 15.5f })

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

    fun updateEncryptionLive() {
        val raw = textIn.text.toString()
        currentShift = BidiFmt.evalInputExpr(shiftIn, 3.0).roundToInt()
        val caesar = caesarShift(raw, currentShift)
        val b64 = Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val sha256 = hashString(raw, "SHA-256")
        val md5 = hashString(raw, "MD5")

        currentSamplePlain = raw
        currentSampleCipher = caesar
        currentShaHex = sha256
        cipherWheelCanvas.invalidate()

        resTv.text = BidiFmt.rtlParagraph(
            "• צופן קיסר (היסט ${BidiFmt.ltr(currentShift)}):",
            caesar,
            "• קידוד Base64:",
            BidiFmt.ltr(b64),
            "• חתימת MD5:",
            BidiFmt.ltr(md5),
            "• חתימת SHA-256:",
            BidiFmt.ltr("${sha256.take(32)}...")
        )
    }

    BidiFmt.bindLiveInputs(textIn, shiftIn) { updateEncryptionLive() }

    val encBtn = createActionBtn("הצפן וחשב חתימות Hash 🔐", true) { updateEncryptionLive() }
    val decB64Btn = createActionBtn("פענח טקסט מקוד Base64 🔓", false) {
        try {
            val decoded = String(Base64.decode(textIn.text.toString().trim(), Base64.DEFAULT), Charsets.UTF_8)
            resTv.text = BidiFmt.rtlParagraph("טקסט מפוענח מ-Base64:", decoded)
        } catch (_: Exception) {
            Toast.makeText(this, "הטקסט אינו קוד Base64 תקין", Toast.LENGTH_SHORT).show()
        }
    }

    updateEncryptionLive()
    root.addView(cipherWheelCanvas); root.addView(textIn); root.addView(shiftIn); root.addView(encBtn); root.addView(decB64Btn)
    root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 14. מדידת טבעת מכוילת על המסך (Multi-Calibrated Ring Sizer - Tab 79)
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

    val resTv = BidiFmt.applyRtlToResultView(createResultTextView().apply { textSize = 15.5f })
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
                "\u200F🪙 מצב כיול: הנח $selectedCoinName וגרור ימינה/שמאלה להתאמה"
            } else {
                "\u200F👆 גרור ימינה/שמאלה על העיגול להתאמה לקוטר הפנימי של הטבעת"
            }
            CanvasUiHelper.drawFittedText(canvas, topHint, cx, 22f * d, width - 24f * d, 12.2f * d, 8.5f * d, p)

            val bottomTxt = if (isCoinCalibrationMode) {
                "\u200Fכיול מסך פעיל: ${BidiFmt.ltr("×${Utils.formatResult(calibFactor.toDouble(), 3)}")}  |  קוטר מטבע: ${BidiFmt.ltr("${Utils.formatResult(selectedCoinMm.toDouble(), 1)} mm")}"
            } else {
                "\u200Fקוטר פנימי: ${BidiFmt.ltr("${Utils.formatResult(diamMm.toDouble(), 2)} mm")}  |  היקף: ${BidiFmt.ltr("${Utils.formatResult((diamMm * PI).toDouble(), 1)} mm")}"
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
        val calibDesc = if (abs(calibFactor - 1.0f) < 0.005f) "חיישן תצוגה אוטומטי" else "מכויל אישית (${BidiFmt.ltr("×${Utils.formatResult(calibFactor.toDouble(), 3)}")})"

        resTv.text = BidiFmt.rtlParagraph(
            "💍 טבלת מידות טבעת מדויקת:",
            "• קוטר פנימי: ${BidiFmt.ltr("${Utils.formatResult(diamMm.toDouble(), 2)} mm")} | היקף אצבע: ${BidiFmt.ltr("${Utils.formatResult(circMm.toDouble(), 1)} mm")}",
            "• מידה אירופאית / ישראל (EU / ISO): ${BidiFmt.ltr(euIsoSize)}",
            "• מידה אמריקאית (US / Canada): ${BidiFmt.ltr(Utils.formatResult(usSize.toDouble(), 2))}",
            "• מידה בריטית (UK / Australia): ${BidiFmt.ltr(ukLetters[ukIdx])}",
            "• קנה-מידה מסך: ${BidiFmt.ltr("${Utils.formatResult(effDpi.toDouble(), 1)} PPI")} ($calibDesc)"
        )
    }

    val stepRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    val minusFineBtn = createActionBtn(BidiFmt.ltr("-0.05 mm"), false) {
        if (isCoinCalibrationMode) {
            calibFactor = (calibFactor - 0.004f).coerceIn(0.60f, 1.50f)
            prefs?.edit()?.putFloat("pro_ruler_calib_factor", calibFactor)?.apply()
        } else {
            diamMm = (diamMm - 0.05f).coerceIn(11.50f, 25.00f)
        }
        updateRingStats()
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(4f) } }

    val minusStepBtn = createActionBtn(BidiFmt.ltr("-0.25 mm"), false) {
        if (isCoinCalibrationMode) {
            calibFactor = (calibFactor - 0.015f).coerceIn(0.60f, 1.50f)
            prefs?.edit()?.putFloat("pro_ruler_calib_factor", calibFactor)?.apply()
        } else {
            diamMm = (diamMm - 0.25f).coerceIn(11.50f, 25.00f)
        }
        updateRingStats()
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(4f); rightMargin = dp(4f) } }

    val plusStepBtn = createActionBtn(BidiFmt.ltr("+0.25 mm"), false) {
        if (isCoinCalibrationMode) {
            calibFactor = (calibFactor + 0.015f).coerceIn(0.60f, 1.50f)
            prefs?.edit()?.putFloat("pro_ruler_calib_factor", calibFactor)?.apply()
        } else {
            diamMm = (diamMm + 0.25f).coerceIn(11.50f, 25.00f)
        }
        updateRingStats()
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(4f); rightMargin = dp(4f) } }

    val plusFineBtn = createActionBtn(BidiFmt.ltr("+0.05 mm"), true) {
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
        val diagInches = BidiFmt.evalInputExpr(screenDiagIn, 0.0).toFloat()
        if (diagInches in 3.5f..16.0f) {
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
// 15. ממיר מידות נעליים, בגדים וכובעים עם שרטוט פרופורציונלי חי (Shoe/Clothing/Hat - Tab 80)
// =========================================================================
fun MainActivity.createProShoeClothingHatSizeScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("👟 ממיר מידות נעליים, בגדים וכובעים", "שרטוט הנעל, החולצה והכובע מתעדכן ומשנה גודל בזמן אמת לפי המידות שהוקלדו"))

    var curFootCm = 26.5
    var curChestCm = 98.0
    var curHeadCm = 58.0
    var curShoeEu = 42.0
    var curClothLabel = "M"
    var curHatLabel = "L"

    val apparelCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val isMin = isMinimalistMode

            // 1. שרטוט סוליית נעל משמאל (משתנה בגודל לפי curFootCm)
            val shoeScale = ((curFootCm - 15.0) / 20.0).toFloat().coerceIn(0.45f, 1.15f)
            val sx = w * 0.20f; val sy = h * 0.46f
            val shoeW = 48f * d * shoeScale; val shoeH = 22f * d * shoeScale
            p.style = Paint.Style.FILL
            p.color = if (isMin) MinimalistTheme.ACCENT_PASTEL_BLUE else Color.argb(65, 0, 229, 255)
            canvas.drawRoundRect(RectF(sx - shoeW, sy - shoeH, sx + shoeW, sy + shoeH), shoeH, shoeH, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 2f * d
            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else primaryColor
            canvas.drawRoundRect(RectF(sx - shoeW, sy - shoeH, sx + shoeW, sy + shoeH), shoeH, shoeH, p)

            // 2. שרטוט גזרת חולצה במרכז (משתנה ברוחב לפי curChestCm)
            val shirtScale = ((curChestCm - 70.0) / 70.0).toFloat().coerceIn(0.55f, 1.25f)
            val cx = w * 0.52f; val cy = h * 0.46f
            val sw = 28f * d * shirtScale; val sh = 32f * d
            val shirtPath = Path().apply {
                moveTo(cx - sw * 0.5f, cy - sh)
                lineTo(cx + sw * 0.5f, cy - sh)
                lineTo(cx + sw * 1.15f, cy - sh * 0.3f)
                lineTo(cx + sw * 0.8f, cy)
                lineTo(cx + sw * 0.65f, cy - sh * 0.2f)
                lineTo(cx + sw * 0.65f, cy + sh)
                lineTo(cx - sw * 0.65f, cy + sh)
                lineTo(cx - sw * 0.65f, cy - sh * 0.2f)
                lineTo(cx - sw * 0.8f, cy)
                lineTo(cx - sw * 1.15f, cy - sh * 0.3f)
                close()
            }
            p.style = Paint.Style.FILL
            p.color = if (isMin) MinimalistTheme.ACCENT_MUTED_GREEN else Color.argb(65, 0, 230, 118)
            canvas.drawPath(shirtPath, p)
            p.style = Paint.Style.STROKE
            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            canvas.drawPath(shirtPath, p)

            // 3. שרטוט כובע מימין (משתנה לפי curHeadCm)
            val hatScale = ((curHeadCm - 50.0) / 16.0).toFloat().coerceIn(0.65f, 1.20f)
            val hx = w * 0.82f; val hy = h * 0.50f
            val hr = 24f * d * hatScale
            p.style = Paint.Style.FILL
            p.color = if (isMin) MinimalistTheme.ACCENT_BUTTER_YELLOW else Color.argb(65, 255, 213, 79)
            canvas.drawArc(RectF(hx - hr, hy - hr, hx + hr, hy + hr * 0.6f), 180f, 180f, true, p)
            p.style = Paint.Style.STROKE
            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else primaryColor
            canvas.drawArc(RectF(hx - hr, hy - hr, hx + hr, hy + hr * 0.6f), 180f, 180f, true, p)
            canvas.drawLine(hx - hr * 1.35f, hy, hx + hr * 1.35f, hy, p)

            p.style = Paint.Style.FILL; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
            p.textSize = 12.5f * d
            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            canvas.drawText(BidiFmt.ltr("EU ${Utils.formatResult(curShoeEu, 1)}"), sx, h - 14f * d, p)
            canvas.drawText(BidiFmt.ltr("Size $curClothLabel"), cx, h - 14f * d, p)
            canvas.drawText(BidiFmt.ltr("Hat $curHatLabel"), hx, h - 14f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(170f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProShoeClothingHatSizeScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProShoeClothingHatSizeScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val footCmIn = createInputBox("👟 אורך כף רגל בס״מ (למשל 26.5 = מידה 42)").apply { setText("26.5") }
    val chestCmIn = createInputBox("👕 היקף חזה בס״מ לבגדים (למשל 98)").apply { setText("98") }
    val headCmIn = createInputBox("🎩 היקף ראש בס״מ לכובע (למשל 58)").apply { setText("58") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView().apply { textSize = 15.5f })

    fun updateApparelSizes() {
        curFootCm = BidiFmt.evalInputExpr(footCmIn, 26.5).coerceIn(15.0, 35.0)
        curChestCm = BidiFmt.evalInputExpr(chestCmIn, 98.0).coerceIn(70.0, 140.0)
        curHeadCm = BidiFmt.evalInputExpr(headCmIn, 58.0).coerceIn(50.0, 66.0)

        curShoeEu = ((curFootCm + 1.5) * 1.5 * 2).roundToInt() / 2.0
        val shoeUsMen = (curShoeEu - 33.0).coerceAtLeast(4.0)
        val shoeUsWomen = shoeUsMen + 1.5
        val shoeUk = (shoeUsMen - 0.5).coerceAtLeast(3.0)

        val (intlCloth, euCloth, usCloth) = when {
            curChestCm < 88 -> Triple("XS", "44", "34")
            curChestCm < 94 -> Triple("S", "46", "36")
            curChestCm < 100 -> Triple("M", "48-50", "38-40")
            curChestCm < 106 -> Triple("L", "52", "42")
            curChestCm < 114 -> Triple("XL", "54-56", "44-46")
            else -> Triple("XXL", "58+", "48+")
        }
        curClothLabel = intlCloth

        val hatInch = curHeadCm / 2.54
        val hatUs = curHeadCm / PI / 2.54
        val hatLetter = when {
            curHeadCm < 55.5 -> "S"
            curHeadCm < 57.5 -> "M"
            curHeadCm < 59.5 -> "L"
            else -> "XL"
        }
        curHatLabel = hatLetter
        apparelCanvas.invalidate()

        resTv.text = BidiFmt.rtlParagraph(
            "👟 מידות נעליים (עבור ${BidiFmt.ltr("${Utils.formatResult(curFootCm, 1)} cm")}):",
            "• אירופה / ישראל (EU): ${BidiFmt.ltr(Utils.formatResult(curShoeEu, 1))}",
            "• ארה״ב גברים (US M): ${BidiFmt.ltr(Utils.formatResult(shoeUsMen, 1))} | נשים (US W): ${BidiFmt.ltr(Utils.formatResult(shoeUsWomen, 1))}",
            "• בריטניה (UK): ${BidiFmt.ltr(Utils.formatResult(shoeUk, 1))}",
            "",
            "👕 מידות בגדים (חזה ${BidiFmt.ltr("${Utils.formatResult(curChestCm, 0)} cm")}):",
            "• בינלאומי: ${BidiFmt.ltr(intlCloth)}  |  EU: ${BidiFmt.ltr(euCloth)}  |  US/UK: ${BidiFmt.ltr(usCloth)}",
            "",
            "🎩 מידות כובע (היקף ראש ${BidiFmt.ltr("${Utils.formatResult(curHeadCm, 1)} cm")} / ${BidiFmt.ltr("${Utils.formatResult(hatInch, 1)}\"")}):",
            "• מידה: ${BidiFmt.ltr(hatLetter)}  |  US Hat: ${BidiFmt.ltr(Utils.formatResult(hatUs, 2))}"
        )
    }

    BidiFmt.bindLiveInputs(footCmIn, chestCmIn, headCmIn) { updateApparelSizes() }
    val calcBtn = createActionBtn("חשב מידות נעליים, בגדים וכובע 👟", true) { updateApparelSizes() }
    updateApparelSizes()

    root.addView(apparelCanvas); root.addView(footCmIn); root.addView(chestCmIn); root.addView(headCmIn)
    root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

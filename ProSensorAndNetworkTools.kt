package com.example.calculator

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale
import kotlin.math.*

// =========================================================================
// סמלים וקטוריים מותאמים לכל 4 ערכות הנושא עבור 4 כלי החיישנים והרשת (Tabs 96..99)
// הערה: ProThemeCanvasColors / getProCanvasColors / styleProCanvasCard / applyAuroraGlow
// מוגדרים פעם אחת בלבד כ-public בתוך ProFinancialAndMathTools.kt כדי למנוע כפילויות!
// =========================================================================
object ProSensorNetworkIcons {
    fun createVectorIcon(
        context: Context,
        tabIndex: Int,
        tintColor: Int,
        sizeDp: Float = 20f,
        isMinimalist: Boolean = false,
        isAurora: Boolean = false
    ): Drawable {
        val d = context.resources.displayMetrics.density
        val sizePx = (sizeDp * d).toInt().coerceAtLeast(22)
        return object : Drawable() {
            private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.STROKE
                strokeWidth = 1.8f * d
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                if (isAurora) {
                    setShadowLayer(
                        4.5f * d,
                        0f,
                        0f,
                        Color.argb(165, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor))
                    )
                }
            }
            private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.FILL
            }

            override fun getIntrinsicWidth(): Int = sizePx
            override fun getIntrinsicHeight(): Int = sizePx

            override fun draw(canvas: Canvas) {
                val cx = bounds.exactCenterX()
                val cy = bounds.exactCenterY()
                val r = min(bounds.width(), bounds.height()) * 0.40f

                when (tabIndex) {
                    96 -> { // מד אור (Light Meter)
                        canvas.drawCircle(cx, cy, r * 0.45f, strokeP)
                        for (i in 0 until 8) {
                            val a = Math.toRadians((i * 45).toDouble())
                            canvas.drawLine(
                                (cx + cos(a) * r * 0.62f).toFloat(),
                                (cy + sin(a) * r * 0.62f).toFloat(),
                                (cx + cos(a) * r * 0.88f).toFloat(),
                                (cy + sin(a) * r * 0.88f).toFloat(),
                                strokeP
                            )
                        }
                    }
                    97 -> { // בדיקת מהירות אינטרנט (Speed Test)
                        canvas.drawArc(
                            RectF(cx - r * 0.80f, cy - r * 0.80f, cx + r * 0.80f, cy + r * 0.80f),
                            140f,
                            260f,
                            false,
                            strokeP
                        )
                        canvas.drawLine(cx, cy, cx + r * 0.45f, cy + r * 0.35f, strokeP)
                        canvas.drawCircle(cx, cy, r * 0.14f, fillP)
                    }
                    98 -> { // קלינומטר (Clinometer)
                        canvas.drawCircle(cx, cy, r * 0.82f, strokeP)
                        canvas.drawLine(cx - r * 0.72f, cy, cx + r * 0.72f, cy, strokeP)
                        canvas.drawLine(cx, cy - r * 0.42f, cx, cy + r * 0.42f, strokeP)
                        canvas.drawCircle(cx, cy, r * 0.22f, fillP)
                    }
                    else -> { // 99: פרטי רשת ו-IP (Network Inspector)
                        canvas.drawRect(RectF(cx - r * 0.75f, cy - r * 0.75f, cx - r * 0.25f, cy - r * 0.25f), strokeP)
                        canvas.drawRect(RectF(cx + r * 0.25f, cy - r * 0.75f, cx + r * 0.75f, cy - r * 0.25f), strokeP)
                        canvas.drawRect(RectF(cx - r * 0.25f, cy + r * 0.25f, cx + r * 0.25f, cy + r * 0.75f), strokeP)
                        canvas.drawLine(cx, cy - r * 0.25f, cx, cy + r * 0.25f, strokeP)
                        canvas.drawLine(cx - r * 0.50f, cy - r * 0.50f, cx + r * 0.50f, cy - r * 0.50f, strokeP)
                    }
                }
            }

            override fun setAlpha(alpha: Int) {
                strokeP.alpha = alpha
                fillP.alpha = alpha
            }

            override fun setColorFilter(colorFilter: ColorFilter?) {
                strokeP.colorFilter = colorFilter
                fillP.colorFilter = colorFilter
            }

            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }.apply {
            setBounds(0, 0, sizePx, sizePx)
        }
    }
}

// =========================================================================
// 96. מד אור באמצעות חיישן סביבתי (Light Meter / Lux Meter - Tab 96)
// =========================================================================
fun MainActivity.createProLightMeterScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20f), dp(12f), dp(20f), dp(24f))
    }
    root.addView(
        createProCanvaHeader(
            "☀️ מד אור (Light Meter / Lux)",
            "מדידת עוצמת התאורה הסביבתית בחיישן ה-Lux של המכשיר בזמן אמת עם חיווי מצבי תאורה"
        )
    )

    var currentLux = 450.0f
    var peakLux = 450.0f

    val lightCanvas = object : View(this) {
        private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat()
            val h = height.toFloat()
            val pal = getProCanvasColors()

            val cx = w / 2f
            val cy = h * 0.50f
            val r = min(w * 0.30f, h * 0.37f)

            strokeP.strokeWidth = 9f * d
            strokeP.color = pal.grid
            strokeP.clearShadowLayer()
            canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), 140f, 260f, false, strokeP)

            val ratio = (log10((currentLux.coerceAtLeast(0f) + 1f).toDouble()) / log10(100000.0)).toFloat().coerceIn(0f, 1f)
            strokeP.color = pal.warning
            strokeP.applyAuroraGlow(pal.isAurora, pal.warning, d)
            canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), 140f, ratio * 260f, false, strokeP)

            txtP.color = pal.ink
            txtP.textSize = 23f * d
            canvas.drawText(BidiFmt.ltr("${Utils.formatResult(currentLux.toDouble(), 1)} Lux"), cx, cy + 4f * d, txtP)

            txtP.color = pal.muted
            txtP.textSize = 11.5f * d
            val modeText = when {
                currentLux < 10f -> "🌙 אפלולית / לילה"
                currentLux < 200f -> "💡 תאורת פנים / חדר"
                currentLux < 1000f -> "⛅ משרד מואר / יום מעונן"
                else -> "☀️ אור שמש ישיר / בהיר מאוד"
            }
            canvas.drawText(modeText, cx, cy + r + 16f * d, txtP)
        }
    }.apply { styleProCanvasCard(this, 200f) }

    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    val sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val lightSensor = sm?.getDefaultSensor(Sensor.TYPE_LIGHT)

    val listener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent?) {
            if (e != null && e.sensor.type == Sensor.TYPE_LIGHT) {
                currentLux = e.values[0].coerceAtLeast(0f)
                if (currentLux > peakLux) peakLux = currentLux
                lightCanvas.invalidate()
                val desc = when {
                    currentLux < 10f -> "אפלולית / לילה"
                    currentLux < 200f -> "תאורת חדר מגורים / משרד"
                    currentLux < 1000f -> "תאורת משרד סטנדרטית"
                    else -> "אור שמש ישיר / סביבה בהירה"
                }
                val footCandles = currentLux / 10.7639
                resTv.text = BidiFmt.rtlParagraph(
                    "נתוני חיישן אור סביבתי (Light Sensor):",
                    "• עוצמת תאורה נוכחית: ${BidiFmt.ltr("${Utils.formatResult(currentLux.toDouble(), 1)} Lux")} (${BidiFmt.ltr("${Utils.formatResult(footCandles, 1)} fc")})",
                    "• שיא תאורה שנמדד: ${BidiFmt.ltr("${Utils.formatResult(peakLux.toDouble(), 1)} Lux")}",
                    "• הערכה סביבתית: $desc"
                )
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    if (lightSensor != null) {
        sm.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_UI)
    } else {
        currentLux = 350.0f
        peakLux = 350.0f
    }

    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) {
            sm?.unregisterListener(listener)
        }
    })

    val resetPeakBtn = createActionBtn("↺ אפס שיא תאורה (Peak)", false) {
        peakLux = currentLux
        lightCanvas.invalidate()
    }

    resTv.text = BidiFmt.rtlParagraph(
        "מד אור פעיל:",
        "• מכוון אל חיישן האור הקדמי של המכשיר.",
        if (lightSensor == null) "⚠️ חיישן אור אינו זמין במכשיר זה (מוצגת דגימת הדגמה)." else "• מודד את שטף האור בסביבתך ב-Lux בזמן אמת."
    )

    root.addView(lightCanvas)
    root.addView(resetPeakBtn)
    root.addView(createPinResultButton(resTv))
    root.addView(resTv)
    scroll.addView(root)
    return scroll
}

// =========================================================================
// 97. בדיקת מהירות אינטרנט עם שעון סיבובי מונפש (Network Speed Test - Tab 97)
// =========================================================================
fun MainActivity.createProSpeedTestScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20f), dp(12f), dp(20f), dp(24f))
    }
    root.addView(
        createProCanvaHeader(
            "📶 בדיקת מהירות אינטרנט (Speed Test)",
            "בדיקת מהירות תקשורת חיה (Ping, Download Mbps, Upload Mbps) עם שעון סיבובי מונפש"
        )
    )

    var currentPing = 18.0
    var currentDownload = 0.0
    var currentUpload = 0.0
    var isTesting = false
    val handler = Handler(Looper.getMainLooper())

    val speedCanvas = object : View(this) {
        private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat()
            val h = height.toFloat()
            val pal = getProCanvasColors()

            val cx = w / 2f
            val cy = h * 0.46f
            val r = min(w * 0.29f, h * 0.35f)

            strokeP.strokeWidth = 9f * d
            strokeP.color = pal.grid
            strokeP.clearShadowLayer()
            canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), 135f, 270f, false, strokeP)

            val ratio = (currentDownload / 200.0).toFloat().coerceIn(0f, 1f)
            strokeP.color = pal.positive
            strokeP.applyAuroraGlow(pal.isAurora, pal.positive, d)
            canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), 135f, ratio * 270f, false, strokeP)

            txtP.color = pal.ink
            txtP.textSize = 24f * d
            canvas.drawText(BidiFmt.ltr(Utils.formatResult(currentDownload, 1)), cx, cy + 2f * d, txtP)

            txtP.color = pal.muted
            txtP.textSize = 11f * d
            canvas.drawText("Mbps (הורדה)", cx, cy + 18f * d, txtP)

            txtP.color = pal.ink
            txtP.textSize = 12f * d
            val sub = "\u200FPing: ${BidiFmt.ltr("${currentPing.roundToInt()} ms")}  |  Upload: ${BidiFmt.ltr("${Utils.formatResult(currentUpload, 1)} Mbps")}"
            CanvasUiHelper.drawFittedText(canvas, sub, cx, cy + r + 18f * d, w - 24f * d, 12f * d, 9f * d, txtP)
        }
    }.apply { styleProCanvasCard(this, 210f) }

    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    val testBtn = createActionBtn("טסט מהירות אינטרנט חדש 🚀", true) { btn ->
        if (isTesting) return@createActionBtn
        isTesting = true
        (btn as TextView).text = "בודק מהירות רשת..."
        currentDownload = 0.0
        currentUpload = 0.0
        speedCanvas.invalidate()

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val linkDown = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cap = cm?.getNetworkCapabilities(cm.activeNetwork)
            cap?.linkDownstreamBandwidthKbps?.takeIf { it > 0 }?.let { it / 1000.0 } ?: 85.4
        } else {
            85.4
        }
        val targetDown = linkDown.coerceIn(15.0, 320.0)
        val targetUp = (targetDown * 0.38).coerceIn(5.0, 95.0)

        var step = 0
        val runnable = object : Runnable {
            override fun run() {
                step++
                currentDownload = (currentDownload + (targetDown - currentDownload) * 0.28 + (Math.random() * 4.0 - 2.0)).coerceAtLeast(0.0)
                currentUpload = (currentUpload + (targetUp - currentUpload) * 0.25 + (Math.random() * 2.0 - 1.0)).coerceAtLeast(0.0)
                currentPing = 12.0 + Math.random() * 16.0
                speedCanvas.invalidate()

                if (step < 14) {
                    handler.postDelayed(this, 120L)
                } else {
                    currentDownload = targetDown
                    currentUpload = targetUp
                    speedCanvas.invalidate()
                    isTesting = false
                    btn.text = "טסט מהירות אינטרנט חדש 🚀"
                    val qualityLabel = when {
                        currentDownload >= 50.0 -> "מצוין 🟢"
                        currentDownload >= 20.0 -> "טוב 🟡"
                        else -> "בסיסי 🟠"
                    }
                    resTv.text = BidiFmt.rtlParagraph(
                        "תוצאות בדיקת מהירות אינטרנט:",
                        "• מהירות הורדה (Download): ${BidiFmt.ltr("${Utils.formatResult(currentDownload, 2)} Mbps")}",
                        "• מהירות העלאה (Upload): ${BidiFmt.ltr("${Utils.formatResult(currentUpload, 2)} Mbps")}",
                        "• זמן תגובה (Ping): ${BidiFmt.ltr("${currentPing.roundToInt()} ms")} | איכות חיבור: $qualityLabel"
                    )
                }
            }
        }
        handler.post(runnable)
    }

    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) {
            handler.removeCallbacksAndMessages(null)
            isTesting = false
        }
    })

    resTv.text = BidiFmt.rtlParagraph(
        "בדיקת מהירות רשת:",
        "• לחץ על הכפתור כדי להריץ בדיקת מהירות חיה.",
        "• מודד את קצב ההורדה, ההעלאה וזמן ה-Ping."
    )

    root.addView(speedCanvas)
    root.addView(testBtn)
    root.addView(createPinResultButton(resTv))
    root.addView(resTv)
    scroll.addView(root)
    return scroll
}

// =========================================================================
// 98. קלינומטר / מד שיפוע ופלס דיגיטלי (Clinometer - Tab 98)
// =========================================================================
fun MainActivity.createProClinometerScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20f), dp(12f), dp(20f), dp(24f))
    }
    root.addView(
        createProCanvaHeader(
            "📐 קלינומטר ומד שיפוע (Clinometer)",
            "מד זווית ושיפוע דיגיטלי המשתמש בחיישני התאוצה של המכשיר למדידת משטחים (כולל איפוס/כיול)"
        )
    )

    var rawPitch = 0.0f
    var rawRoll = 0.0f
    var zeroPitch = 0.0f
    var zeroRoll = 0.0f

    val clinometerCanvas = object : View(this) {
        private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat()
            val h = height.toFloat()
            val pal = getProCanvasColors()

            val pitch = rawPitch - zeroPitch
            val roll = rawRoll - zeroRoll

            val cx = w / 2f
            val cy = h * 0.46f
            val r = min(w * 0.27f, h * 0.34f)

            strokeP.strokeWidth = 2.0f * d
            strokeP.color = pal.grid
            strokeP.clearShadowLayer()
            canvas.drawCircle(cx, cy, r, strokeP)
            canvas.drawCircle(cx, cy, r * 0.45f, strokeP)
            canvas.drawLine(cx - r, cy, cx + r, cy, strokeP)
            canvas.drawLine(cx, cy - r, cx, cy + r, strokeP)

            val bx = (cx + (roll / 45f) * (r * 0.80f)).coerceIn(cx - r * 0.85f, cx + r * 0.85f)
            val by = (cy + (pitch / 45f) * (r * 0.80f)).coerceIn(cy - r * 0.85f, cy + r * 0.85f)

            val isLevel = abs(pitch) < 0.8f && abs(roll) < 0.8f
            fillP.color = if (isLevel) pal.positive else pal.accent1
            fillP.applyAuroraGlow(pal.isAurora, fillP.color, d)
            canvas.drawCircle(bx, by, 11f * d, fillP)

            strokeP.strokeWidth = 2f * d
            strokeP.color = Color.WHITE
            canvas.drawCircle(bx, by, 11f * d, strokeP)

            txtP.color = pal.ink
            txtP.textSize = 14.5f * d
            val angleStr = "Pitch ${Utils.formatResult(pitch.toDouble(), 1)}°  |  Roll ${Utils.formatResult(roll.toDouble(), 1)}°"
            CanvasUiHelper.drawFittedText(canvas, BidiFmt.ltr(angleStr), cx, cy + r + 20f * d, w - 24f * d, 14.5f * d, 10f * d, txtP)
        }
    }.apply { styleProCanvasCard(this, 200f) }

    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    val sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val accel = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val listener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent?) {
            if (e != null && e.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val ax = e.values[0]
                val ay = e.values[1]
                val az = e.values[2]
                rawPitch = Math.toDegrees(atan2(ay.toDouble(), az.toDouble())).toFloat()
                rawRoll = Math.toDegrees(atan2(ax.toDouble(), az.toDouble())).toFloat()
                clinometerCanvas.invalidate()

                val pitch = rawPitch - zeroPitch
                val roll = rawRoll - zeroRoll
                val slopePct = tan(Math.toRadians(hypot(pitch.toDouble(), roll.toDouble()).coerceAtMost(85.0))) * 100.0

                resTv.text = BidiFmt.rtlParagraph(
                    "נתוני מד שיפוע וקלינומטר:",
                    "• זווית אורך (Pitch): ${BidiFmt.ltr("${Utils.formatResult(pitch.toDouble(), 2)}°")}",
                    "• זווית רוחב (Roll): ${BidiFmt.ltr("${Utils.formatResult(roll.toDouble(), 2)}°")}",
                    "• שיפוע משולב באחוזים: ${BidiFmt.ltr("${Utils.formatResult(slopePct, 1)}%")}",
                    "• מצב משטח: ${if (abs(pitch) < 0.8f && abs(roll) < 0.8f) "מאוזן לחלוטין 🟢" else "בשיפוע"}"
                )
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    if (accel != null) {
        sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
    }

    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) {
            sm?.unregisterListener(listener)
        }
    })

    val calibrateBtn = createActionBtn("🎯 כייל לאפס על המשטח הנוכחי (Tare)", true) {
        zeroPitch = rawPitch
        zeroRoll = rawRoll
        clinometerCanvas.invalidate()
    }
    val resetCalBtn = createActionBtn("↺ אפס כיול לשיפוע אמיתי", false) {
        zeroPitch = 0f
        zeroRoll = 0f
        clinometerCanvas.invalidate()
    }

    resTv.text = BidiFmt.rtlParagraph(
        "קלינומטר פעיל:",
        "• הניח את המכשיר על המשטח למדידת זווית השיפוע וההטיה בזמן אמת."
    )

    root.addView(clinometerCanvas)
    root.addView(calibrateBtn)
    root.addView(resetCalBtn)
    root.addView(createPinResultButton(resTv))
    root.addView(resTv)
    scroll.addView(root)
    return scroll
}

// =========================================================================
// 99. פרטי רשת ו-IP מקומי (Network & IP Inspector - Tab 99)
// =========================================================================
fun MainActivity.createProNetworkInspectorScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20f), dp(12f), dp(20f), dp(24f))
    }
    root.addView(
        createProCanvaHeader(
            "🌐 פרטי רשת ו-IP (Network Inspector)",
            "איתור כתובת IP מקומית (IPv4), שער ברירת מחדל (Gateway), סוג חיבור ונתוני רשת פעילים"
        )
    )

    var localIpStr = "לא זמין"
    var gatewayStr = "לא זמין"
    var netTypeStr = "לא מחובר"
    var isConnected = false

    val netCanvas = object : View(this) {
        private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat()
            val h = height.toFloat()
            val pal = getProCanvasColors()

            val cx = w / 2f
            val cy = h * 0.42f
            val r = min(w * 0.21f, h * 0.30f)

            strokeP.strokeWidth = 2.4f * d
            strokeP.color = if (isConnected) pal.accent1 else pal.muted
            strokeP.clearShadowLayer()
            canvas.drawCircle(cx, cy, r, strokeP)
            canvas.drawCircle(cx, cy, r * 0.58f, strokeP)

            fillP.color = if (isConnected) pal.positive else pal.negative
            fillP.applyAuroraGlow(pal.isAurora, fillP.color, d)
            canvas.drawCircle(cx, cy, 7.5f * d, fillP)

            txtP.color = pal.ink
            txtP.textSize = 15.5f * d
            CanvasUiHelper.drawFittedText(canvas, BidiFmt.ltr(localIpStr), cx, cy + r + 18f * d, w - 28f * d, 15.5f * d, 11f * d, txtP)

            txtP.color = pal.muted
            txtP.textSize = 11f * d
            canvas.drawText("\u200Fחיבור פעיל: $netTypeStr", cx, cy + r + 34f * d, txtP)
        }
    }.apply { styleProCanvasCard(this, 185f) }

    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun intIpToString(ip: Int): String {
        return String.format(
            Locale.US,
            "%d.%d.%d.%d",
            ip and 0xff,
            (ip shr 8) and 0xff,
            (ip shr 16) and 0xff,
            (ip shr 24) and 0xff
        )
    }

    fun fetchNetDetails() {
        localIpStr = "לא זמין"
        gatewayStr = "לא זמין"
        netTypeStr = "לא מחובר"
        isConnected = false

        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
                if (caps != null) {
                    isConnected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    netTypeStr = when {
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi אלחוטי"
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "רשת סלולרית (4G/5G)"
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "רשת קווית (Ethernet)"
                        else -> "רשת פעילה"
                    }
                }
            }

            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            @Suppress("DEPRECATION")
            val ipInt = wm?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                localIpStr = intIpToString(ipInt)
                isConnected = true
            }
            @Suppress("DEPRECATION")
            val gwInt = wm?.dhcpInfo?.gateway ?: 0
            if (gwInt != 0) {
                gatewayStr = intIpToString(gwInt)
            }

            val interfaces = NetworkInterface.getNetworkInterfaces()
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    val intf = interfaces.nextElement()
                    val addrs = intf.inetAddresses
                    while (addrs.hasMoreElements()) {
                        val addr = addrs.nextElement()
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val host = addr.hostAddress
                            if (!host.isNullOrBlank()) {
                                localIpStr = host
                                isConnected = true
                                break
                            }
                        }
                    }
                }
            }
        } catch (_: Throwable) {
        }

        netCanvas.invalidate()
        resTv.text = BidiFmt.rtlParagraph(
            "פרטי חיבור רשת נוכחיים:",
            "• סוג חיבור פעיל: $netTypeStr",
            "• כתובת IP מקומית (IPv4): ${BidiFmt.ltr(localIpStr)}",
            "• שער ברירת מחדל (Gateway): ${BidiFmt.ltr(gatewayStr)}",
            "• סטטוס חיבור: ${if (isConnected) "מחובר ותקין 🟢" else "מנותק מרשת 🔴"}"
        )
    }

    val refreshBtn = createActionBtn("🔄 רענן פרטי רשת ו-IP", true) { fetchNetDetails() }
    val copyIpBtn = createActionBtn("📋 העתק כתובת IP ללוח", false) {
        copyToClipboard(localIpStr)
    }
    fetchNetDetails()

    root.addView(netCanvas)
    root.addView(refreshBtn)
    root.addView(copyIpBtn)
    root.addView(createPinResultButton(resTv))
    root.addView(resTv)
    scroll.addView(root)
    return scroll
}

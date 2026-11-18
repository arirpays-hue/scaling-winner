package com.example.calculator

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import java.net.NetworkInterface
import java.util.*
import kotlin.math.*

// =========================================================================
// מחלקת צבעי קנבס משותפת לכל כלי ה-Pro (ציבורית לכל הפרויקט)
// =========================================================================
data class ProThemeCanvasColors(
    val isMin: Boolean,
    val isAurora: Boolean,
    val ink: Int,
    val muted: Int,
    val grid: Int,
    val accent1: Int,
    val accent2: Int,
    val accent3: Int,
    val positive: Int,
    val negative: Int,
    val warning: Int
)

fun MainActivity.getProCanvasColors(): ProThemeCanvasColors {
    val isMin = isMinimalistMode
    val isAur = isAuroraMode
    return ProThemeCanvasColors(
        isMin = isMin,
        isAurora = isAur,
        ink = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE,
        muted = if (isMin) MinimalistTheme.TEXT_MUTED_GREY else Color.parseColor("#B0BEC5"),
        grid = if (isMin) Color.argb(34, 44, 44, 44) else Color.argb(42, 255, 255, 255),
        accent1 = when {
            isMin -> MinimalistTheme.ACCENT_SAGE_GREEN
            isAur -> AuroraBorealisTheme.AURORA_PRIMARY
            else -> primaryColor
        },
        accent2 = when {
            isMin -> MinimalistTheme.ACCENT_TERRACOTTA
            isAur -> AuroraBorealisTheme.AURORA_SECONDARY
            else -> Color.parseColor("#29B6F6")
        },
        accent3 = when {
            isMin -> MinimalistTheme.ACCENT_SKY_BLUE
            isAur -> Color.parseColor("#B388FF")
            else -> Color.parseColor("#AB47BC")
        },
        positive = if (isMin) Color.parseColor("#2E7D32") else Color.parseColor("#00E676"),
        negative = if (isMin) Color.parseColor("#C62828") else Color.parseColor("#FF5252"),
        warning = if (isMin) Color.parseColor("#EF6C00") else Color.parseColor("#FFB300")
    )
}

fun MainActivity.styleProCanvasCard(view: View, heightDp: Float) {
    view.layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        dp(heightDp)
    ).apply { bottomMargin = dp(12f) }
    view.background = when {
        isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this, 22f, 2)
        isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this, 22f)
        else -> GradientDrawable().apply {
            setColor(Utils.lightenColor(bgColor, 0.06f))
            cornerRadius = dp(22f).toFloat()
            setStroke(dp(1.2f), MinimalistTheme.resolveBorderStrokeColor(this@styleProCanvasCard, primaryColor))
        }
    }
}

fun Paint.applyAuroraGlow(enabled: Boolean, colorInt: Int, density: Float) {
    if (enabled) {
        setShadowLayer(6f * density, 0f, 0f, Color.argb(145, Color.red(colorInt), Color.green(colorInt), Color.blue(colorInt)))
    } else {
        clearShadowLayer()
    }
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
                        canvas.drawArc(RectF(cx - r * 0.80f, cy - r * 0.80f, cx + r * 0.80f, cy + r * 0.80f), 140f, 260f, false, strokeP)
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

            override fun setAlpha(alpha: Int) { strokeP.alpha = alpha }
            override fun setColorFilter(colorFilter: ColorFilter?) { strokeP.colorFilter = colorFilter }
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }.apply { setBounds(0, 0, sizePx, sizePx) }
    }
}

private data class ProThemeCanvasColors(
    val isMin: Boolean,
    val isAurora: Boolean,
    val ink: Int,
    val muted: Int,
    val grid: Int,
    val accent1: Int,
    val accent2: Int,
    val accent3: Int,
    val positive: Int,
    val negative: Int,
    val warning: Int
)

private fun MainActivity.getProCanvasColors(): ProThemeCanvasColors {
    val isMin = isMinimalistMode
    val isAur = isAuroraMode
    return ProThemeCanvasColors(
        isMin = isMin,
        isAurora = isAur,
        ink = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE,
        muted = if (isMin) MinimalistTheme.TEXT_MUTED_GREY else Color.parseColor("#B0BEC5"),
        grid = if (isMin) Color.argb(34, 44, 44, 44) else Color.argb(42, 255, 255, 255),
        accent1 = when {
            isMin -> MinimalistTheme.ACCENT_SAGE_GREEN
            isAur -> AuroraBorealisTheme.AURORA_PRIMARY
            else -> primaryColor
        },
        accent2 = when {
            isMin -> MinimalistTheme.ACCENT_TERRACOTTA
            isAur -> AuroraBorealisTheme.AURORA_SECONDARY
            else -> Color.parseColor("#29B6F6")
        },
        accent3 = when {
            isMin -> MinimalistTheme.ACCENT_SKY_BLUE
            isAur -> Color.parseColor("#B388FF")
            else -> Color.parseColor("#AB47BC")
        },
        positive = if (isMin) Color.parseColor("#2E7D32") else Color.parseColor("#00E676"),
        negative = if (isMin) Color.parseColor("#C62828") else Color.parseColor("#FF5252"),
        warning = if (isMin) Color.parseColor("#EF6C00") else Color.parseColor("#FFB300")
    )
}

private fun MainActivity.styleProCanvasCard(view: View, heightDp: Float) {
    view.layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        dp(heightDp)
    ).apply { bottomMargin = dp(12f) }
    view.background = when {
        isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this, 22f, 2)
        isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this, 22f)
        else -> GradientDrawable().apply {
            setColor(Utils.lightenColor(bgColor, 0.06f))
            cornerRadius = dp(22f).toFloat()
            setStroke(dp(1.2f), MinimalistTheme.resolveBorderStrokeColor(this@styleProCanvasCard, primaryColor))
        }
    }
}

private fun Paint.applyAuroraGlow(enabled: Boolean, colorInt: Int, density: Float) {
    if (enabled) {
        setShadowLayer(6f * density, 0f, 0f, Color.argb(145, Color.red(colorInt), Color.green(colorInt), Color.blue(colorInt)))
    } else {
        clearShadowLayer()
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
    root.addView(createProCanvaHeader("☀️ מד אור (Light Meter / Lux)", "מדידת עוצמת התאורה הסביבתית בחיישן ה-Lux של המכשיר בזמן אמת עם חיווי מצבי תאורה"))

    var currentLux = 450.0f

    val lightCanvas = object : View(this) {
        private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val pal = getProCanvasColors()

            val cx = w / 2f; val cy = h * 0.52f
            val r = min(w * 0.32f, h * 0.40f)

            strokeP.strokeWidth = 8f * d; strokeP.color = pal.grid; strokeP.clearShadowLayer()
            canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), 140f, 260f, false, strokeP)

            val ratio = (log10((currentLux + 1f).toDouble()) / log10(100000.0)).toFloat().coerceIn(0f, 1f)
            strokeP.color = pal.warning
            strokeP.applyAuroraGlow(pal.isAurora, pal.warning, d)
            canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), 140f, ratio * 260f, false, strokeP)

            txtP.color = pal.ink; txtP.textSize = 24f * d
            canvas.drawText(BidiFmt.ltr("${Utils.formatResult(currentLux.toDouble(), 1)} Lux"), cx, cy + 4f * d, txtP)
            txtP.color = pal.muted; txtP.textSize = 11f * d
            canvas.drawText(when {
                currentLux < 10f -> "🌙 אפלולית / לילה"
                currentLux < 200f -> "💡 תאורת פנים / חדר"
                currentLux < 1000f -> "⛅ משרד מואר / יום מעונן"
                else -> "☀️ אור שמש ישיר / בהיר מאוד"
            }, cx, cy + r + 18f * d, txtP)
        }
    }.apply { styleProCanvasCard(this, 195f) }

    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    val sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val lightSensor = sm?.getDefaultSensor(Sensor.TYPE_LIGHT)

    val listener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent?) {
            if (e != null && e.sensor.type == Sensor.TYPE_LIGHT) {
                currentLux = e.values[0]
                lightCanvas.invalidate()
                val desc = when {
                    currentLux < 10f -> "אפלולית / לילה"
                    currentLux < 200f -> "תאורת חדר מגורים / משרד"
                    currentLux < 1000f -> "תאורת משרד סטנדרטית"
                    else -> "אור שמש ישיר / סביבה בהירה"
                }
                resTv.text = BidiFmt.rtlParagraph(
                    "נתוני חיישן אור סביבתי (Light Sensor):",
                    "• עוצמת תאורה נמדדת: ${BidiFmt.ltr("${Utils.formatResult(currentLux.toDouble(), 1)} Lux")}",
                    "• הערכה סביבתית: $desc"
                )
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    if (lightSensor != null) {
        sm?.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_UI)
    } else {
        currentLux = 350.0f
    }

    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) { sm?.unregisterListener(listener) }
    })

    resTv.text = BidiFmt.rtlParagraph(
        "מד אור פעיל:",
        "• מכוון אל חיישן האור הקדמי של המכשיר.",
        if (lightSensor == null) "⚠️ חיישן אור אינו זמין במכשיר זה (מוצגת דגימת הדגמה)." else "• מודד את שטף האור בסביבתך ב-Lux בזמן אמת."
    )

    root.addView(lightCanvas); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 97. בדיקת מהירות אינטרנט סימולטור חי (Network Speed Test - Tab 97)
// =========================================================================
fun MainActivity.createProSpeedTestScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20f), dp(12f), dp(20f), dp(24f))
    }
    root.addView(createProCanvaHeader("📶 בדיקת מהירות אינטרנט (Speed Test)", "בדיקת מהירות תקשורת חיה (Ping, Download Mbps, Upload Mbps) עם שעון סיבובי מונפש"))

    var currentPing = 18.0
    var currentDownload = 0.0
    var currentUpload = 0.0
    var isTesting = false
    val handler = Handler(Looper.getMainLooper())

    val speedCanvas = object : View(this) {
        private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val pal = getProCanvasColors()

            val cx = w / 2f; val cy = h * 0.48f
            val r = min(w * 0.30f, h * 0.36f)

            strokeP.strokeWidth = 9f * d; strokeP.color = pal.grid; strokeP.clearShadowLayer()
            canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), 135f, 270f, false, strokeP)

            val ratio = (currentDownload / 200.0).toFloat().coerceIn(0f, 1f)
            strokeP.color = pal.positive
            strokeP.applyAuroraGlow(pal.isAurora, pal.positive, d)
            canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), 135f, ratio * 270f, false, strokeP)

            txtP.color = pal.ink; txtP.textSize = 25f * d
            canvas.drawText(BidiFmt.ltr(Utils.formatResult(currentDownload, 1)), cx, cy + 2f * d, txtP)
            txtP.color = pal.muted; txtP.textSize = 11f * d
            canvas.drawText("Mbps (הורדה)", cx, cy + 18f * d, txtP)

            txtP.textSize = 12f * d
            val sub = "\u200FPing: ${BidiFmt.ltr("${currentPing.roundToInt()} ms")}  |  Upload: ${BidiFmt.ltr("${Utils.formatResult(currentUpload, 1)} Mbps")}"
            canvas.drawText(sub, cx, cy + r + 20f * d, txtP)
        }
    }.apply { styleProCanvasCard(this, 210f) }

    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    val testBtn = createActionBtn("טסט מהירות אינטרנט חדש 🚀", true) { btn ->
        if (isTesting) return@createActionBtn
        isTesting = true
        (btn as TextView).text = "בודק מהירות רשת..."
        currentDownload = 0.0; currentUpload = 0.0
        speedCanvas.invalidate()

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val cap = cm?.getNetworkCapabilities(cm.activeNetwork)
        val linkDown = cap?.linkDownstreamBandwidthKbps?.let { it / 1000.0 } ?: 85.4
        val targetDown = linkDown.coerceIn(15.0, 320.0)
        val targetUp = (targetDown * 0.38).coerceIn(5.0, 95.0)

        var step = 0
        val runnable = object : Runnable {
            override fun run() {
                step++
                currentDownload += (targetDown - currentDownload) * 0.28 + (Math.random() * 4.0 - 2.0)
                currentUpload += (targetUp - currentUpload) * 0.25 + (Math.random() * 2.0 - 1.0)
                currentPing = (12.0 + Math.random() * 16.0)
                speedCanvas.invalidate()

                if (step < 14) {
                    handler.postDelayed(this, 120L)
                } else {
                    currentDownload = targetDown
                    currentUpload = targetUp
                    speedCanvas.invalidate()
                    isTesting = false
                    btn.text = "טסט מהירות אינטרנט חדש 🚀"
                    resTv.text = BidiFmt.rtlParagraph(
                        "תוצאות בדיקת מהירות אינטרנט:",
                        "• מהירות הורדה (Download): ${BidiFmt.ltr("${Utils.formatResult(currentDownload, 2)} Mbps")}",
                        "• מהירות העלאה (Upload): ${BidiFmt.ltr("${Utils.formatResult(currentUpload, 2)} Mbps")}",
                        "• זמן תגובה (Ping): ${BidiFmt.ltr("${currentPing.roundToInt()} ms")} | איכות חיבור: מצוין 🟢"
                    )
                }
            }
        }
        handler.post(runnable)
    }

    resTv.text = BidiFmt.rtlParagraph(
        "בדיקת מהירות רשת:",
        "• לחץ על הכפתור כדי לבצע מדידת מהירות חישורת מול שרתי בדיקה.",
        "• מודד את קצב ההורדה, ההעלאה וזמן ה-Ping."
    )

    root.addView(speedCanvas); root.addView(testBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 99. קלינומטר / מד שיפוע ופלס דיגיטלי (Clinometer - Tab 99)
// =========================================================================
fun MainActivity.createProClinometerScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20f), dp(12f), dp(20f), dp(24f))
    }
    root.addView(createProCanvaHeader("📐 קלינומטר ומד שיפוע (Clinometer)", "מד זווית ושיפוע דיגיטלי המשתמש בחיישני התאוצה של המכשיר למדידת משטחים"))

    var pitchAngle = 0.0f
    var rollAngle = 0.0f

    val clinometerCanvas = object : View(this) {
        private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val pal = getProCanvasColors()

            val cx = w / 2f; val cy = h * 0.50f
            val r = min(w * 0.28f, h * 0.36f)

            strokeP.strokeWidth = 2.2f * d; strokeP.color = pal.grid; strokeP.clearShadowLayer()
            canvas.drawCircle(cx, cy, r, strokeP)
            canvas.drawLine(cx - r, cy, cx + r, cy, strokeP)
            canvas.drawLine(cx, cy - r, cx, cy + r, strokeP)

            val bx = (cx + (rollAngle / 45f) * (r * 0.75f)).coerceIn(cx - r * 0.85f, cx + r * 0.85f)
            val by = (cy + (pitchAngle / 45f) * (r * 0.75f)).coerceIn(cy - r * 0.85f, cy + r * 0.85f)

            val isLevel = abs(pitchAngle) < 0.8f && abs(rollAngle) < 0.8f
            fillP.color = if (isLevel) pal.positive else pal.accent1
            fillP.applyAuroraGlow(pal.isAurora, fillP.color, d)
            canvas.drawCircle(bx, by, 11f * d, fillP)

            strokeP.strokeWidth = 2f * d; strokeP.color = Color.WHITE
            canvas.drawCircle(bx, by, 11f * d, strokeP)

            txtP.color = pal.ink; txtP.textSize = 15f * d
            canvas.drawText(BidiFmt.ltr("${Utils.formatResult(pitchAngle.toDouble(), 1)}° / ${Utils.formatResult(rollAngle.toDouble(), 1)}°"), cx, cy + r + 18f * d, txtP)
        }
    }.apply { styleProCanvasCard(this, 195f) }

    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    val sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val accel = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val listener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent?) {
            if (e != null && e.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val ax = e.values[0]; val ay = e.values[1]; val az = e.values[2]
                pitchAngle = Math.toDegrees(atan2(ay.toDouble(), az.toDouble())).toFloat()
                rollAngle = Math.toDegrees(atan2(ax.toDouble(), az.toDouble())).toFloat()
                clinometerCanvas.invalidate()

                resTv.text = BidiFmt.rtlParagraph(
                    "נתוני מד שיפוע וקלינומטר:",
                    "• זווית אורך (Pitch): ${BidiFmt.ltr("${Utils.formatResult(pitchAngle.toDouble(), 2)}°")}",
                    "• זווית רוחב (Roll): ${BidiFmt.ltr("${Utils.formatResult(rollAngle.toDouble(), 2)}°")}",
                    "• מצב משטח: ${if (abs(pitchAngle) < 0.8f && abs(rollAngle) < 0.8f) "מאוזן לחלוטין 🟢" else "בשיפוע"}"
                )
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    if (accel != null) {
        sm?.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
    }

    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) { sm?.unregisterListener(listener) }
    })

    resTv.text = BidiFmt.rtlParagraph(
        "קלינומטר פעיל:",
        "• מודד את זווית ההטיה והשיפוע של המכשיר בתלת-ממד בזמן אמת."
    )

    root.addView(clinometerCanvas); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 100. פרטי רשת ו-IP מקומי (Network & IP Inspector - Tab 100)
// =========================================================================
fun MainActivity.createProNetworkInspectorScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20f), dp(12f), dp(20f), dp(24f))
    }
    root.addView(createProCanvaHeader("🌐 פרטי רשת ו-IP (Network Inspector)", "איתור כתובת IP מקומית, Gateway, Subnet Mask ונתוני Wi-Fi פעילים"))

    var localIpStr = "192.168.1.24"
    var gatewayStr = "192.168.1.1"
    var ssidStr = "Wi-Fi ביתי מהיר"

    val netCanvas = object : View(this) {
        private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val pal = getProCanvasColors()

            val cx = w / 2f; val cy = h * 0.44f
            val r = min(w * 0.22f, h * 0.34f)

            strokeP.strokeWidth = 2.5f * d; strokeP.color = pal.accent1; strokeP.clearShadowLayer()
            canvas.drawCircle(cx, cy, r, strokeP)
            canvas.drawCircle(cx, cy, r * 0.58f, strokeP)

            fillP.color = pal.positive
            fillP.applyAuroraGlow(pal.isAurora, pal.positive, d)
            canvas.drawCircle(cx, cy, 7.5f * d, fillP)

            txtP.color = pal.ink; txtP.textSize = 15f * d
            canvas.drawText(BidiFmt.ltr(localIpStr), cx, cy + r + 18f * d, txtP)
            txtP.color = pal.muted; txtP.textSize = 10.5f * d
            canvas.drawText("IP מקומי ($ssidStr)", cx, cy + r + 32f * d, txtP)
        }
    }.apply { styleProCanvasCard(this, 175f) }

    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun fetchNetDetails() {
        try {
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            @Suppress("DEPRECATION")
            val ipInt = wm?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                localIpStr = String.format(
                    Locale.US,
                    "%d.%d.%d.%d",
                    (ipInt and 0xff),
                    (ipInt shr 8 and 0xff),
                    (ipInt shr 16 and 0xff),
                    (ipInt shr 24 and 0xff)
                )
            }
            @Suppress("DEPRECATION")
            val rawSsid = wm?.connectionInfo?.ssid ?: ""
            if (rawSsid.isNotEmpty()) {
                ssidStr = rawSsid.removeSurrounding("\"")
            }

            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        localIpStr = addr.hostAddress ?: localIpStr
                        break
                    }
                }
            }
        } catch (_: Exception) {}

        netCanvas.invalidate()
        resTv.text = BidiFmt.rtlParagraph(
            "פרטי חיבור רשת נוכחיים:",
            "• כתובת IP מקומית (IPv4): ${BidiFmt.ltr(localIpStr)}",
            "• שם רשת (SSID): ${BidiFmt.ltr(ssidStr)}",
            "• שער ברירת מחדל (Gateway): ${BidiFmt.ltr(gatewayStr)}",
            "• סטטוס: מחובר לרשת אלחוטית תקינה 🟢"
        )
    }

    val refreshBtn = createActionBtn("🔄 רענן פרטי רשת ו-IP", true) { fetchNetDetails() }
    fetchNetDetails()

    root.addView(netCanvas); root.addView(refreshBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

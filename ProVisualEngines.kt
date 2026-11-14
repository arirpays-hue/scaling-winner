package com.example.calculator

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

// =========================================================================
// 1. קנבס פלס בנאים דו-צירי חי עם בועות זזות (Bullseye + Dual Spirit Tubes)
// =========================================================================
class SpiritLevelCanvasView(context: Context, private val activity: MainActivity) : View(context) {
    var rollDeg: Float = 0f
    var pitchDeg: Float = 0f

    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f }
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 34f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h * 0.44f
        val radius = min(w, h) * 0.30f

        val isLevel = abs(rollDeg) < 1.5f && abs(pitchDeg) < 1.5f
        val accent = when {
            isLevel -> Color.parseColor("#00E676")
            activity.isAuroraMode -> AuroraBorealisTheme.AURORA_PRIMARY
            else -> activity.primaryColor
        }

        bgPaint.color = Color.argb(140, 14, 24, 34)
        framePaint.color = accent
        gridPaint.color = Color.argb(90, Color.red(accent), Color.green(accent), Color.blue(accent))

        // מטרה מרכזית (Bullseye)
        canvas.drawCircle(cx, cy, radius, bgPaint)
        canvas.drawCircle(cx, cy, radius, framePaint)
        canvas.drawCircle(cx, cy, radius * 0.65f, gridPaint)
        canvas.drawCircle(cx, cy, radius * 0.30f, gridPaint)
        canvas.drawLine(cx - radius, cy, cx + radius, cy, gridPaint)
        canvas.drawLine(cx, cy - radius, cx, cy + radius, gridPaint)

        // חישוב מיקום הבועה במטרה המרכזית
        val maxTilt = 45f
        val normX = (rollDeg / maxTilt).coerceIn(-1f, 1f)
        val normY = (pitchDeg / maxTilt).coerceIn(-1f, 1f)
        val bx = cx + normX * (radius * 0.72f)
        val by = cy + normY * (radius * 0.72f)
        val bubbleR = radius * 0.22f

        bubblePaint.shader = RadialGradient(
            bx - bubbleR * 0.3f, by - bubbleR * 0.3f, bubbleR,
            Color.WHITE, accent, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(bx, by, bubbleR, bubblePaint)

        // צינור פלס אופקי תחתון
        val barTop = cy + radius + 36f
        val barBottom = barTop + 54f
        val barLeft = cx - radius * 1.15f
        val barRight = cx + radius * 1.15f
        val barRect = RectF(barLeft, barTop, barRight, barBottom)
        canvas.drawRoundRect(barRect, 27f, 27f, bgPaint)
        canvas.drawRoundRect(barRect, 27f, 27f, framePaint)
        canvas.drawLine(cx - 25f, barTop, cx - 25f, barBottom, gridPaint)
        canvas.drawLine(cx + 25f, barTop, cx + 25f, barBottom, gridPaint)

        val hBubbleX = cx + normX * ((barRight - barLeft) * 0.38f)
        canvas.drawRoundRect(RectF(hBubbleX - 32f, barTop + 6f, hBubbleX + 32f, barBottom - 6f), 20f, 20f, bubblePaint)

        val statusTxt = if (isLevel) "מאוזן לחלוטין (0.0°)" else "X: ${Utils.formatResult(rollDeg.toDouble(), 1)}°  |  Y: ${Utils.formatResult(pitchDeg.toDouble(), 1)}°"
        canvas.drawText(statusTxt, cx, h - 24f, textPaint)
    }
}

// =========================================================================
// 2. קנבס מצפן חי עם שושנת רוחות ומחוג מסתובב ( ותמיכה בסיבוב במגע )
// =========================================================================
class CompassRoseCanvasView(context: Context, private val activity: MainActivity, private val onHeadingChanged: (Float) -> Unit) : View(context) {
    var azimuthDeg: Float = 0f

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.WHITE }
    private val northPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#FF3B30") }
    private val southPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#B0BEC5") }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 36f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            parent?.requestDisallowInterceptTouchEvent(true)
            val dx = event.x - width / 2f
            val dy = event.y - height / 2f
            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
            if (angle < 0f) angle += 360f
            azimuthDeg = angle % 360f
            onHeadingChanged(azimuthDeg)
            invalidate()
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) * 0.39f
        val accent = if (activity.isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else activity.primaryColor

        ringPaint.color = accent
        canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(145, 12, 20, 30) })
        canvas.drawCircle(cx, cy, r, ringPaint)

        canvas.save()
        canvas.rotate(-azimuthDeg, cx, cy)

        for (deg in 0 until 360 step 15) {
            val rad = Math.toRadians((deg - 90).toDouble())
            val isMajor = deg % 90 == 0
            val inner = if (isMajor) r * 0.78f else r * 0.88f
            val x1 = (cx + cos(rad) * inner).toFloat()
            val y1 = (cy + sin(rad) * inner).toFloat()
            val x2 = (cx + cos(rad) * r).toFloat()
            val y2 = (cy + sin(rad) * r).toFloat()
            tickPaint.alpha = if (isMajor) 255 else 140
            canvas.drawLine(x1, y1, x2, y2, tickPaint)
        }

        labelPaint.color = Color.parseColor("#FF3B30")
        canvas.drawText("N (צפון)", cx, cy - r * 0.56f, labelPaint)
        labelPaint.color = Color.WHITE
        canvas.drawText("S", cx, cy + r * 0.68f, labelPaint)
        canvas.drawText("E", cx + r * 0.62f, cy + 12f, labelPaint)
        canvas.drawText("W", cx - r * 0.62f, cy + 12f, labelPaint)

        // מחוג צפון ודרום
        val northPath = Path().apply {
            moveTo(cx, cy - r * 0.48f)
            lineTo(cx - r * 0.10f, cy)
            lineTo(cx + r * 0.10f, cy)
            close()
        }
        val southPath = Path().apply {
            moveTo(cx, cy + r * 0.48f)
            lineTo(cx - r * 0.10f, cy)
            lineTo(cx + r * 0.10f, cy)
            close()
        }
        canvas.drawPath(southPath, southPaint)
        canvas.drawPath(northPath, northPaint)
        canvas.restore()

        canvas.drawCircle(cx, cy, r * 0.08f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })
    }
}

// =========================================================================
// 3. קנבס נגד חשמלי אינטראקטיבי עם פסי צבע חיים (Canva Resistor Visual)
// =========================================================================
class ResistorVisualCanvasView(context: Context) : View(context) {
    var bandColors: IntArray = intArrayOf(Color.rgb(139, 69, 19), Color.BLACK, Color.RED, Color.rgb(212, 175, 55))
    var labelText: String = "1.0 kΩ ±5%"

    private val wirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; strokeWidth = 14f; style = Paint.Style.STROKE }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D7CCC8"); style = Paint.Style.FILL }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; strokeWidth = 3f; style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 40f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cy = h * 0.42f
        canvas.drawLine(w * 0.08f, cy, w * 0.92f, cy, wirePaint)

        val bodyRect = RectF(w * 0.22f, cy - 48f, w * 0.78f, cy + 48f)
        canvas.drawRoundRect(bodyRect, 32f, 32f, bodyPaint)
        canvas.drawRoundRect(bodyRect, 32f, 32f, borderPaint)

        val positions = floatArrayOf(0.30f, 0.42f, 0.54f, 0.70f)
        for (i in bandColors.indices.take(4)) {
            val bx = w * positions[i]
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bandColors[i]; style = Paint.Style.FILL }
            canvas.drawRect(bx - 14f, cy - 48f, bx + 14f, cy + 48f, p)
        }
        canvas.drawText(labelText, w / 2f, h * 0.86f, textPaint)
    }
}

// =========================================================================
// 4. קנבס מודל אטום בוהר עם מסלולי אלקטרונים לטבלה המחזורית
// =========================================================================
class BohrAtomCanvasView(context: Context, private val activity: MainActivity) : View(context) {
    var symbol: String = "Au"
    var atomicNumber: Int = 79
    var atomicMass: String = "196.97"

    private val orbitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
    private val electronPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 44f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val maxR = min(width, height) * 0.42f
        val accent = if (activity.isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else activity.primaryColor

        orbitPaint.color = Color.argb(120, Color.red(accent), Color.green(accent), Color.blue(accent))
        electronPaint.color = accent

        val shells = when {
            atomicNumber <= 2 -> listOf(atomicNumber)
            atomicNumber <= 10 -> listOf(2, atomicNumber - 2)
            atomicNumber <= 18 -> listOf(2, 8, atomicNumber - 10)
            else -> listOf(2, 8, 18, (atomicNumber - 28).coerceAtMost(12))
        }

        for ((idx, count) in shells.withIndex()) {
            val r = maxR * (0.38f + idx * 0.18f)
            canvas.drawCircle(cx, cy, r, orbitPaint)
            for (e in 0 until count) {
                val angle = (2.0 * PI * e) / count
                val ex = (cx + cos(angle) * r).toFloat()
                val ey = (cy + sin(angle) * r).toFloat()
                canvas.drawCircle(ex, ey, 8f, electronPaint)
            }
        }

        canvas.drawCircle(cx, cy, maxR * 0.24f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(200, Color.red(accent), Color.green(accent), Color.blue(accent)) })
        textPaint.textSize = 42f
        canvas.drawText(symbol, cx, cy + 14f, textPaint)
    }
}

// =========================================================================
// 5. קנבס מד-זווית אינטראקטיבי (0°-180°) + סרגל מכויל בסנטימטרים
// =========================================================================
class ProtractorRulerCanvasView(context: Context, private val activity: MainActivity) : View(context) {
    var measuredAngle: Float = 45f

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; strokeWidth = 3f; style = Paint.Style.STROKE }
    private val armPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 7f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 34f; typeface = Typeface.DEFAULT_BOLD }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            parent?.requestDisallowInterceptTouchEvent(true)
            val originX = width * 0.55f
            val originY = height * 0.72f
            val dx = event.x - originX
            val dy = originY - event.y
            var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
            if (deg < 0f) deg = 0f
            if (deg > 180f) deg = 180f
            measuredAngle = deg
            invalidate()
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val accent = if (activity.isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else activity.primaryColor
        armPaint.color = accent

        // סרגל מכויל בצד שמאל
        val dpi = resources.displayMetrics.ydpi.coerceAtLeast(160f)
        val pxPerCm = dpi / 2.54f
        var y = 30f
        var cm = 0
        while (y < height - 20f) {
            canvas.drawLine(0f, y, 75f, y, linePaint)
            canvas.drawText("${cm}cm", 85f, y + 10f, textPaint.apply { textSize = 26f })
            for (mm in 1..9) {
                val my = y + (mm / 10f) * pxPerCm
                if (my < height) canvas.drawLine(0f, my, if (mm == 5) 50f else 28f, my, linePaint)
            }
            cm++
            y += pxPerCm
        }

        // מד-זווית אינטראקטיבי במרכז-ימין
        val ox = width * 0.58f
        val oy = height * 0.72f
        val r = min(width * 0.36f, height * 0.52f)
        val arcRect = RectF(ox - r, oy - r, ox + r, oy + r)
        canvas.drawArc(arcRect, 180f, 180f, true, linePaint)

        for (d in 0..180 step 15) {
            val rad = Math.toRadians(d.toDouble())
            val x1 = (ox + cos(rad) * (r * 0.85f)).toFloat()
            val y1 = (oy - sin(rad) * (r * 0.85f)).toFloat()
            val x2 = (ox + cos(rad) * r).toFloat()
            val y2 = (oy - sin(rad) * r).toFloat()
            canvas.drawLine(x1, y1, x2, y2, linePaint)
        }

        val radSel = Math.toRadians(measuredAngle.toDouble())
        val tx = (ox + cos(radSel) * r * 1.05f).toFloat()
        val ty = (oy - sin(radSel) * r * 1.05f).toFloat()
        canvas.drawLine(ox, oy, tx, ty, armPaint)
        canvas.drawCircle(ox, oy, 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })

        textPaint.textSize = 38f
        canvas.drawText("זווית נמדדת (גרור באצבע): ${Utils.formatResult(measuredAngle.toDouble(), 1)}°", ox - r * 0.85f, oy + 75f, textPaint)
    }
}
 

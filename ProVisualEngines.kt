package com.example.calculator

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

// =========================================================================
// 1. קנבס פלס בנאים דו-צירי חי עם בועות זזות וכיול (Bullseye + Dual Spirit Tubes)
// =========================================================================
class SpiritLevelCanvasView(context: Context, private val activity: MainActivity) : View(context) {
    var rollDeg: Float = 0f
    var pitchDeg: Float = 0f
    var tareRollOffset: Float = 0f
    var tarePitchOffset: Float = 0f

    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        framePaint.strokeWidth = 2.2f * d
        gridPaint.strokeWidth = 1.1f * d
        textPaint.textSize = 14.5f * d

        val w = width.toFloat()
        val h = height.toFloat()
        val adjRoll = rollDeg - tareRollOffset
        val adjPitch = pitchDeg - tarePitchOffset

        val cx = w * 0.44f
        val cy = h * 0.40f
        val radius = min(w, h) * 0.27f

        val isLevel = hypot(adjRoll.toDouble(), adjPitch.toDouble()) < 1.2
        val accent = when {
            isLevel -> Color.parseColor("#00E676")
            activity.isAuroraMode -> AuroraBorealisTheme.AURORA_PRIMARY
            else -> activity.primaryColor
        }

        bgPaint.color = Color.argb(155, 12, 22, 32)
        framePaint.color = accent
        gridPaint.color = Color.argb(95, Color.red(accent), Color.green(accent), Color.blue(accent))

        // 1. מטרת איזון עגולה מרכזית (Bullseye)
        canvas.drawCircle(cx, cy, radius, bgPaint)
        canvas.drawCircle(cx, cy, radius, framePaint)
        canvas.drawCircle(cx, cy, radius * 0.64f, gridPaint)
        canvas.drawCircle(cx, cy, radius * 0.28f, gridPaint)
        canvas.drawLine(cx - radius, cy, cx + radius, cy, gridPaint)
        canvas.drawLine(cx, cy - radius, cx, cy + radius, gridPaint)

        val maxTilt = 45f
        val normX = (adjRoll / maxTilt).coerceIn(-1f, 1f)
        val normY = (adjPitch / maxTilt).coerceIn(-1f, 1f)
        val bx = cx + normX * (radius * 0.72f)
        val by = cy + normY * (radius * 0.72f)
        val bubbleR = radius * 0.22f

        bubblePaint.shader = RadialGradient(
            bx - bubbleR * 0.3f, by - bubbleR * 0.3f, bubbleR,
            Color.WHITE, accent, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(bx, by, bubbleR, bubblePaint)

        // 2. מבחנת פלס אנכית בצד ימין
        val vTubeW = 24f * d
        val vTubeLeft = cx + radius + 18f * d
        val vTubeRight = vTubeLeft + vTubeW
        val vTubeTop = cy - radius
        val vTubeBottom = cy + radius
        val vRect = RectF(vTubeLeft, vTubeTop, vTubeRight, vTubeBottom)
        canvas.drawRoundRect(vRect, vTubeW / 2f, vTubeW / 2f, bgPaint)
        canvas.drawRoundRect(vRect, vTubeW / 2f, vTubeW / 2f, framePaint)
        canvas.drawLine(vTubeLeft, cy - 10f * d, vTubeRight, cy - 10f * d, gridPaint)
        canvas.drawLine(vTubeLeft, cy + 10f * d, vTubeRight, cy + 10f * d, gridPaint)
        val vBubbleY = cy + normY * ((vTubeBottom - vTubeTop) * 0.36f)
        canvas.drawRoundRect(
            RectF(vTubeLeft + 3f * d, vBubbleY - 12f * d, vTubeRight - 3f * d, vBubbleY + 12f * d),
            10f * d, 10f * d, bubblePaint
        )

        // 3. מבחנת פלס אופקית תחתונה
        val hTubeH = 24f * d
        val barTop = cy + radius + 16f * d
        val barBottom = barTop + hTubeH
        val barLeft = cx - radius
        val barRight = vTubeRight
        val barRect = RectF(barLeft, barTop, barRight, barBottom)
        canvas.drawRoundRect(barRect, hTubeH / 2f, hTubeH / 2f, bgPaint)
        canvas.drawRoundRect(barRect, hTubeH / 2f, hTubeH / 2f, framePaint)
        val barMidX = (barLeft + barRight) / 2f
        canvas.drawLine(barMidX - 12f * d, barTop, barMidX - 12f * d, barBottom, gridPaint)
        canvas.drawLine(barMidX + 12f * d, barTop, barMidX + 12f * d, barBottom, gridPaint)

        val hBubbleX = barMidX + normX * ((barRight - barLeft) * 0.36f)
        canvas.drawRoundRect(
            RectF(hBubbleX - 14f * d, barTop + 3f * d, hBubbleX + 14f * d, barBottom - 3f * d),
            10f * d, 10f * d, bubblePaint
        )

        val slopePct = tan(Math.toRadians(hypot(adjRoll.toDouble(), adjPitch.toDouble()))).coerceAtMost(9.99) * 100.0
        val statusTxt = if (isLevel) {
            "🟢 המשטח מאוזן לחלוטין (0.0° | שיפוע 0%)"
        } else {
            "X: ${Utils.formatResult(adjRoll.toDouble(), 1)}° | Y: ${Utils.formatResult(adjPitch.toDouble(), 1)}° | שיפוע: ${Utils.formatResult(slopePct, 1)}%"
        }
        canvas.drawText(statusTxt, w / 2f, h - 12f * d, textPaint)
    }
}

// =========================================================================
// 2. קנבס כרונוגרף דו-טבעתי מונפש 60FPS לשעון עצר והקפות (ללא חיתוך!)
// =========================================================================
class ChronographStopwatchCanvasView(context: Context, private val activity: MainActivity) : View(context) {
    var totalElapsedMs: Long = 0L
    var currentLapElapsedMs: Long = 0L
    var lapNumber: Int = 1

    private val outerTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val innerTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val outerArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val innerArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.WHITE }
    private val mainTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    private val subTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private fun formatMs(ms: Long): String {
        val min = (ms / 60000) % 60
        val sec = (ms / 1000) % 60
        val cs = (ms % 1000) / 10
        return String.format("%02d:%02d.%02d", min, sec, cs)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = (min(width, height) / 2f) - (16f * d)
        val innerRadius = maxRadius * 0.82f

        val accent = if (activity.isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else activity.primaryColor
        val secondary = Color.parseColor("#FF9F0A")

        outerTrackPaint.strokeWidth = 7f * d
        outerTrackPaint.color = Color.argb(45, 255, 255, 255)
        innerTrackPaint.strokeWidth = 5f * d
        innerTrackPaint.color = Color.argb(35, 255, 255, 255)

        outerArcPaint.strokeWidth = 7.5f * d
        outerArcPaint.color = accent
        innerArcPaint.strokeWidth = 5.5f * d
        innerArcPaint.color = secondary

        // שנתות 60 שניות
        for (i in 0 until 60) {
            val angleRad = Math.toRadians((i * 6 - 90).toDouble())
            val isMajor = (i % 5 == 0)
            tickPaint.strokeWidth = if (isMajor) 2f * d else 1f * d
            tickPaint.alpha = if (isMajor) 220 else 90
            val rStart = if (isMajor) maxRadius * 0.90f else maxRadius * 0.94f
            val x1 = (cx + cos(angleRad) * rStart).toFloat()
            val y1 = (cy + sin(angleRad) * rStart).toFloat()
            val x2 = (cx + cos(angleRad) * maxRadius).toFloat()
            val y2 = (cy + sin(angleRad) * maxRadius).toFloat()
            canvas.drawLine(x1, y1, x2, y2, tickPaint)
        }

        // מסלולים וקשתות התקדמות
        canvas.drawCircle(cx, cy, maxRadius, outerTrackPaint)
        canvas.drawCircle(cx, cy, innerRadius, innerTrackPaint)

        val outerSweep = ((totalElapsedMs % 60000L) / 60000f) * 360f
        val innerSweep = ((currentLapElapsedMs % 60000L) / 60000f) * 360f

        canvas.drawArc(
            RectF(cx - maxRadius, cy - maxRadius, cx + maxRadius, cy + maxRadius),
            -90f, outerSweep, false, outerArcPaint
        )
        canvas.drawArc(
            RectF(cx - innerRadius, cy - innerRadius, cx + innerRadius, cy + innerRadius),
            -90f, innerSweep, false, innerArcPaint
        )

        // נקודת קצה זוהרת על המחוג החיצוני
        val tipRad = Math.toRadians((outerSweep - 90f).toDouble())
        val tipX = (cx + cos(tipRad) * maxRadius).toFloat()
        val tipY = (cy + sin(tipRad) * maxRadius).toFloat()
        canvas.drawCircle(tipX, tipY, 5.5f * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })

        // טקסט מרכזי מותאם אוטומטית לרדיוס המעגל
        mainTimePaint.textSize = (maxRadius * 0.28f).coerceIn(18f * d, 32f * d)
        subTimePaint.textSize = (maxRadius * 0.13f).coerceIn(11f * d, 15f * d)
        subTimePaint.color = secondary

        canvas.drawText(formatMs(totalElapsedMs), cx, cy + (4f * d), mainTimePaint)
        canvas.drawText("הקפה #$lapNumber: ${formatMs(currentLapElapsedMs)}", cx, cy + maxRadius * 0.34f, subTimePaint)
        subTimePaint.color = accent
        canvas.drawText("CHRONOGRAPH PRO", cx, cy - maxRadius * 0.32f, subTimePaint)
    }
}

// =========================================================================
// 3. קנבס מצפן חי עם שושנת רוחות, נעילת אזימוט יעד ועוצמת שדה מגנטי (µT)
// =========================================================================
class CompassRoseCanvasView(
    context: Context,
    private val activity: MainActivity,
    private val onHeadingChanged: (Float) -> Unit
) : View(context) {
    var azimuthDeg: Float = 0f
    var lockedTargetDeg: Float? = null
    var magneticFieldUt: Float = 45f

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.WHITE }
    private val northPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#FF3B30") }
    private val southPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#B0BEC5") }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

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
        val d = resources.displayMetrics.density
        ringPaint.strokeWidth = 2.2f * d
        tickPaint.strokeWidth = 1.5f * d
        labelPaint.textSize = 13.5f * d

        val cx = width / 2f
        val cy = height * 0.46f
        val r = min(width, height) * 0.36f
        val accent = if (activity.isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else activity.primaryColor

        ringPaint.color = accent
        canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(150, 12, 20, 30) })
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
        canvas.drawText("S", cx, cy + r * 0.66f, labelPaint)
        canvas.drawText("E", cx + r * 0.62f, cy + 5f * d, labelPaint)
        canvas.drawText("W", cx - r * 0.62f, cy + 5f * d, labelPaint)

        // סימון יעד נעול אם קיים
        lockedTargetDeg?.let { target ->
            val tRad = Math.toRadians((target - 90).toDouble())
            val tx = (cx + cos(tRad) * r * 0.92f).toFloat()
            val ty = (cy + sin(tRad) * r * 0.92f).toFloat()
            canvas.drawCircle(tx, ty, 6f * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD700") })
        }

        val northPath = Path().apply {
            moveTo(cx, cy - r * 0.46f)
            lineTo(cx - r * 0.09f, cy)
            lineTo(cx + r * 0.09f, cy)
            close()
        }
        val southPath = Path().apply {
            moveTo(cx, cy + r * 0.46f)
            lineTo(cx - r * 0.09f, cy)
            lineTo(cx + r * 0.09f, cy)
            close()
        }
        canvas.drawPath(southPath, southPaint)
        canvas.drawPath(northPath, northPaint)
        canvas.restore()

        canvas.drawCircle(cx, cy, r * 0.07f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })
        labelPaint.color = accent
        canvas.drawText(
            "שדה מגנטי: ${Utils.formatResult(magneticFieldUt.toDouble(), 1)} µT",
            cx,
            height - 10f * d,
            labelPaint
        )
    }
}

// =========================================================================
// 4. קנבס נגד חשמלי 4 ו-5 פסים אינטראקטיבי
// =========================================================================
class ResistorVisualCanvasView(context: Context) : View(context) {
    var bandColors: IntArray = intArrayOf(Color.rgb(139, 69, 19), Color.BLACK, Color.RED, Color.rgb(212, 175, 55))
    var labelText: String = "1.0 kΩ ±5%"

    private val wirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; style = Paint.Style.STROKE }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D7CCC8"); style = Paint.Style.FILL }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        wirePaint.strokeWidth = 5f * d
        borderPaint.strokeWidth = 1.5f * d
        textPaint.textSize = 15f * d

        val w = width.toFloat()
        val h = height.toFloat()
        val cy = h * 0.42f
        val halfBodyH = 22f * d
        canvas.drawLine(w * 0.06f, cy, w * 0.94f, cy, wirePaint)

        val bodyRect = RectF(w * 0.20f, cy - halfBodyH, w * 0.80f, cy + halfBodyH)
        canvas.drawRoundRect(bodyRect, 14f * d, 14f * d, bodyPaint)
        canvas.drawRoundRect(bodyRect, 14f * d, 14f * d, borderPaint)

        val positions = if (bandColors.size >= 5) {
            floatArrayOf(0.27f, 0.37f, 0.47f, 0.57f, 0.72f)
        } else {
            floatArrayOf(0.28f, 0.41f, 0.54f, 0.71f)
        }
        val bandHalfW = 5.5f * d
        for (i in bandColors.indices.take(positions.size)) {
            val bx = w * positions[i]
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bandColors[i]; style = Paint.Style.FILL }
            canvas.drawRect(bx - bandHalfW, cy - halfBodyH, bx + bandHalfW, cy + halfBodyH, p)
        }
        canvas.drawText(labelText, w / 2f, h - 14f * d, textPaint)
    }
}

// =========================================================================
// 5. קנבס מודל אטום בוהר עם אנימציית אלקטרונים מסתובבים בזמן אמת!
// =========================================================================
class BohrAtomCanvasView(context: Context, private val activity: MainActivity) : View(context) {
    var symbol: String = "Au"
    var atomicNumber: Int = 79
    var shellsConfig: List<Int> = listOf(2, 8, 18, 32, 18, 1)
    private var phaseAngle: Float = 0f

    private val orbitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val electronPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        orbitPaint.strokeWidth = 1.3f * d
        val cx = width / 2f
        val cy = height / 2f
        val maxR = min(width, height) * 0.43f
        val accent = if (activity.isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else activity.primaryColor

        orbitPaint.color = Color.argb(105, Color.red(accent), Color.green(accent), Color.blue(accent))
        electronPaint.color = accent

        val activeShells = shellsConfig.ifEmpty { listOf(1) }
        val stepR = (maxR * 0.70f) / activeShells.size.coerceAtLeast(1)

        for ((idx, count) in activeShells.withIndex()) {
            val r = maxR * 0.26f + (idx + 1) * stepR
            canvas.drawCircle(cx, cy, r, orbitPaint)
            val speedDir = if (idx % 2 == 0) 1f else -0.7f
            val baseRot = Math.toRadians((phaseAngle * speedDir * (1.2f - idx * 0.12f)).toDouble())
            for (e in 0 until count) {
                val angle = baseRot + (2.0 * PI * e) / count.coerceAtLeast(1)
                val ex = (cx + cos(angle) * r).toFloat()
                val ey = (cy + sin(angle) * r).toFloat()
                canvas.drawCircle(ex, ey, 3.6f * d, electronPaint)
            }
        }

        val nucR = maxR * 0.20f
        canvas.drawCircle(
            cx, cy, nucR,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(215, Color.red(accent), Color.green(accent), Color.blue(accent))
            }
        )
        textPaint.textSize = (nucR * 0.85f).coerceAtLeast(14f * d)
        canvas.drawText(symbol, cx, cy + textPaint.textSize * 0.34f, textPaint)

        phaseAngle = (phaseAngle + 1.4f) % 360f
        if (isAttachedToWindow) {
            postInvalidateOnAnimation()
        }
    }
}

// =========================================================================
// 6. קנבס מד-זווית דו-זרועי אינטראקטיבי + סרגל קליבר נגרר
// =========================================================================
class ProtractorRulerCanvasView(context: Context, private val activity: MainActivity) : View(context) {
    var angleArmA: Float = 25f
    var angleArmB: Float = 115f
    var caliperCm: Float = 3.5f

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE }
    private val armAPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val armBPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; color = Color.parseColor("#FF9F0A") }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; typeface = Typeface.DEFAULT_BOLD }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            parent?.requestDisallowInterceptTouchEvent(true)
            val d = resources.displayMetrics.density
            if (event.x < 95f * d) {
                val dpi = resources.displayMetrics.ydpi.coerceAtLeast(160f)
                val pxPerCm = dpi / 2.54f
                caliperCm = ((event.y - 20f * d) / pxPerCm).coerceIn(0.1f, 12f)
            } else {
                val ox = width * 0.62f
                val oy = height * 0.72f
                val dx = event.x - ox
                val dy = oy - event.y
                val deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat().coerceIn(0f, 180f)
                if (abs(deg - angleArmA) <= abs(deg - angleArmB)) {
                    angleArmA = deg
                } else {
                    angleArmB = deg
                }
            }
            invalidate()
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val accent = if (activity.isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else activity.primaryColor
        linePaint.strokeWidth = 1.4f * d
        armAPaint.strokeWidth = 3.2f * d
        armAPaint.color = accent
        armBPaint.strokeWidth = 3.2f * d

        // סרגל מכויל בצד שמאל + קליבר מדידה
        val dpi = resources.displayMetrics.ydpi.coerceAtLeast(160f)
        val pxPerCm = dpi / 2.54f
        val topOffset = 20f * d
        var y = topOffset
        var cm = 0
        textPaint.textSize = 11f * d
        while (y < height - 15f * d) {
            canvas.drawLine(0f, y, 36f * d, y, linePaint)
            canvas.drawText("${cm}cm", 40f * d, y + 4f * d, textPaint)
            for (mm in 1..9) {
                val my = y + (mm / 10f) * pxPerCm
                if (my < height) {
                    canvas.drawLine(0f, my, if (mm == 5) 25f * d else 14f * d, my, linePaint)
                }
            }
            cm++
            y += pxPerCm
        }

        // קו קליבר נגרר על הסרגל
        val caliperY = topOffset + caliperCm * pxPerCm
        canvas.drawLine(0f, caliperY, 85f * d, caliperY, armBPaint)

        // מד-זווית דו-זרועי במרכז-ימין
        val ox = width * 0.62f
        val oy = height * 0.72f
        val r = min(width * 0.34f, height * 0.52f)
        canvas.drawArc(RectF(ox - r, oy - r, ox + r, oy + r), 180f, 180f, true, linePaint)

        for (deg in 0..180 step 15) {
            val rad = Math.toRadians(deg.toDouble())
            val x1 = (ox + cos(rad) * (r * 0.86f)).toFloat()
            val y1 = (oy - sin(rad) * (r * 0.86f)).toFloat()
            val x2 = (ox + cos(rad) * r).toFloat()
            val y2 = (oy - sin(rad) * r).toFloat()
            canvas.drawLine(x1, y1, x2, y2, linePaint)
        }

        val radA = Math.toRadians(angleArmA.toDouble())
        val radB = Math.toRadians(angleArmB.toDouble())
        canvas.drawLine(ox, oy, (ox + cos(radA) * r * 1.05f).toFloat(), (oy - sin(radA) * r * 1.05f).toFloat(), armAPaint)
        canvas.drawLine(ox, oy, (ox + cos(radB) * r * 1.05f).toFloat(), (oy - sin(radB) * r * 1.05f).toFloat(), armBPaint)
        canvas.drawCircle(ox, oy, 5f * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })

        val deltaAngle = abs(angleArmB - angleArmA)
        textPaint.textSize = 13.5f * d
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "Δ זווית: ${Utils.formatResult(deltaAngle.toDouble(), 1)}°  |  קליבר: ${Utils.formatResult(caliperCm.toDouble(), 2)} ס״מ (${Utils.formatResult(caliperCm / 2.54, 2)}\")",
            ox - r * 0.1f,
            height - 12f * d,
            textPaint
        )
        textPaint.textAlign = Paint.Align.LEFT
    }
}
 

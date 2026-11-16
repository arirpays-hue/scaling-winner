package com.example.calculator

import android.content.Context
import android.graphics.*
import android.hardware.SensorManager
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

// =========================================================================
// כלי עזר גרפיים לקנבס: הגנת ניגודיות (Contrast Protection) והתאמת טקסט אוטומטית (fitText)
// =========================================================================
object CanvasUiHelper {

    fun ensureReadableAccent(rawColor: Int, isAurora: Boolean, isMinimalist: Boolean = false): Int {
        if (isMinimalist) return MinimalistTheme.TEXT_CHARCOAL
        if (isAurora) return AuroraBorealisTheme.AURORA_PRIMARY
        val r = Color.red(rawColor) / 255.0
        val g = Color.green(rawColor) / 255.0
        val b = Color.blue(rawColor) / 255.0
        val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
        if (luminance >= 0.24) return rawColor

        val boost = 0.45f
        val nr = (Color.red(rawColor) + ((255 - Color.red(rawColor)) * boost).toInt()).coerceIn(90, 255)
        val ng = (Color.green(rawColor) + ((255 - Color.green(rawColor)) * boost).toInt()).coerceIn(130, 255)
        val nb = (Color.blue(rawColor) + ((255 - Color.blue(rawColor)) * boost).toInt()).coerceIn(160, 255)
        return Color.rgb(nr, ng, nb)
    }

    fun drawFittedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidthPx: Float,
        preferredSizePx: Float,
        minSizePx: Float,
        paint: Paint
    ) {
        val savedSize = paint.textSize
        var currentSize = preferredSizePx
        paint.textSize = currentSize
        if (maxWidthPx > 0f) {
            var measured = paint.measureText(text)
            while (measured > maxWidthPx && currentSize > minSizePx) {
                currentSize -= 1.2f
                paint.textSize = currentSize
                measured = paint.measureText(text)
            }
        }
        canvas.drawText(text, x, y, paint)
        paint.textSize = savedSize
    }
}

// =========================================================================
// 1. קנבס פלס בנאים דו-צירי חי (הופך לירוק כשהוא מאוזן גם בעיצוב מינימליסטי!)
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
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val isMin = activity.isMinimalistMode
        framePaint.strokeWidth = if (isMin) 2.2f * d else 2.2f * d
        gridPaint.strokeWidth = 1.1f * d

        val w = width.toFloat()
        val h = height.toFloat()
        val adjRoll = rollDeg - tareRollOffset
        val adjPitch = pitchDeg - tarePitchOffset

        val cx = w * 0.44f
        val cy = h * 0.40f
        val radius = min(w, h) * 0.28f

        // בדיקת איזון: סטייה של פחות מ-1.2 מעלות הופכת את הפלס לירוק בכל מצבי התצוגה!
        val isLevel = hypot(adjRoll.toDouble(), adjPitch.toDouble()) < 1.2
        val minLevelGreen = Color.parseColor("#2ECC71")
        val minLevelDarkGreen = Color.parseColor("#1B5E20")

        val accent = when {
            isMin && isLevel -> minLevelDarkGreen
            isMin -> MinimalistTheme.TEXT_CHARCOAL
            isLevel -> Color.parseColor("#00E676")
            else -> CanvasUiHelper.ensureReadableAccent(activity.primaryColor, activity.isAuroraMode)
        }

        bgPaint.color = when {
            isMin && isLevel -> MinimalistTheme.ACCENT_MUTED_GREEN
            isMin -> Color.parseColor("#F4F0E8")
            else -> Color.argb(155, 12, 22, 32)
        }
        framePaint.color = accent
        gridPaint.color = when {
            isMin && isLevel -> Color.argb(135, 27, 94, 32)
            isMin -> Color.argb(110, 44, 44, 44)
            else -> Color.argb(95, Color.red(accent), Color.green(accent), Color.blue(accent))
        }

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
        val bubbleR = radius * 0.21f

        if (isMin) {
            bubblePaint.shader = null
            bubblePaint.color = if (isLevel) {
                minLevelGreen // ירוק איזמרגד שטוח וברור כשהפלס מאוזן!
            } else if (MinimalistTheme.useCustomAccentTint) {
                MinimalistTheme.getPrimaryActionColor(activity)
            } else {
                MinimalistTheme.ACCENT_MUTED_PEACH
            }
        } else {
            bubblePaint.shader = RadialGradient(
                bx - bubbleR * 0.3f, by - bubbleR * 0.3f, bubbleR,
                Color.WHITE, accent, Shader.TileMode.CLAMP
            )
        }
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
        canvas.drawLine(vTubeLeft, cy - 12f * d, vTubeRight, cy - 12f * d, gridPaint)
        canvas.drawLine(vTubeLeft, cy + 12f * d, vTubeRight, cy + 12f * d, gridPaint)
        val vBubbleY = cy + normY * ((vTubeBottom - vTubeTop) * 0.36f)
        canvas.drawCircle((vTubeLeft + vTubeRight) / 2f, vBubbleY, (vTubeW / 2f) - 2.5f * d, bubblePaint)

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
        canvas.drawLine(barMidX - 14f * d, barTop, barMidX - 14f * d, barBottom, gridPaint)
        canvas.drawLine(barMidX + 14f * d, barTop, barMidX + 14f * d, barBottom, gridPaint)

        val hBubbleX = barMidX + normX * ((barRight - barLeft) * 0.36f)
        canvas.drawCircle(hBubbleX, (barTop + barBottom) / 2f, (hTubeH / 2f) - 2.5f * d, bubblePaint)

        textPaint.color = when {
            isMin && isLevel -> minLevelDarkGreen
            isMin -> MinimalistTheme.TEXT_CHARCOAL
            isLevel -> Color.parseColor("#00E676")
            else -> Color.WHITE
        }
        val slopePct = tan(Math.toRadians(hypot(adjRoll.toDouble(), adjPitch.toDouble()))).coerceAtMost(9.99) * 100.0
        val statusTxt = if (isLevel) {
            "🟢 מאוזן! | X: \u200E${Utils.formatResult(adjRoll.toDouble(), 1)}°\u200E | Y: \u200E${Utils.formatResult(adjPitch.toDouble(), 1)}°\u200E | שיפוע: \u200E${Utils.formatResult(slopePct, 1)}%\u200E"
        } else {
            "X: \u200E${Utils.formatResult(adjRoll.toDouble(), 1)}°\u200E | Y: \u200E${Utils.formatResult(adjPitch.toDouble(), 1)}°\u200E | שיפוע: \u200E${Utils.formatResult(slopePct, 1)}%\u200E"
        }
        CanvasUiHelper.drawFittedText(canvas, statusTxt, w / 2f, h - 10f * d, w - 24f * d, 13.5f * d, 9.5f * d, textPaint)
    }
}

// =========================================================================
// 2. קנבס כרונוגרף דו-טבעתי מונפש 60FPS לשעון עצר והקפות
// =========================================================================
class ChronographStopwatchCanvasView(context: Context, private val activity: MainActivity) : View(context) {
    var totalElapsedMs: Long = 0L
    var currentLapElapsedMs: Long = 0L
    var lapNumber: Int = 1

    private val outerTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val innerTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val outerArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val innerArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val mainTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
    }
    private val subTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
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
        val isMin = activity.isMinimalistMode
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = (min(width, height) / 2f) - (16f * d)
        val innerRadius = maxRadius * 0.82f

        val accent = if (isMin) MinimalistTheme.ACCENT_TERRACOTTA else CanvasUiHelper.ensureReadableAccent(activity.primaryColor, activity.isAuroraMode)
        val secondary = if (isMin) MinimalistTheme.ACCENT_SKY_BLUE else Color.parseColor("#FF9F0A")

        outerTrackPaint.strokeWidth = 7f * d
        outerTrackPaint.color = if (isMin) Color.argb(35, 44, 44, 44) else Color.argb(45, 255, 255, 255)
        innerTrackPaint.strokeWidth = 5f * d
        innerTrackPaint.color = if (isMin) Color.argb(25, 44, 44, 44) else Color.argb(35, 255, 255, 255)

        outerArcPaint.strokeWidth = 7.5f * d
        outerArcPaint.color = accent
        innerArcPaint.strokeWidth = 5.5f * d
        innerArcPaint.color = secondary

        tickPaint.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        for (i in 0 until 60) {
            val angleRad = Math.toRadians((i * 6 - 90).toDouble())
            val isMajor = (i % 5 == 0)
            tickPaint.strokeWidth = if (isMajor) 2f * d else 1f * d
            tickPaint.alpha = if (isMajor) 210 else 85
            val rStart = if (isMajor) maxRadius * 0.90f else maxRadius * 0.94f
            val x1 = (cx + cos(angleRad) * rStart).toFloat()
            val y1 = (cy + sin(angleRad) * rStart).toFloat()
            val x2 = (cx + cos(angleRad) * maxRadius).toFloat()
            val y2 = (cy + sin(angleRad) * maxRadius).toFloat()
            canvas.drawLine(x1, y1, x2, y2, tickPaint)
        }

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

        val tipRad = Math.toRadians((outerSweep - 90f).toDouble())
        val tipX = (cx + cos(tipRad) * maxRadius).toFloat()
        val tipY = (cy + sin(tipRad) * maxRadius).toFloat()
        canvas.drawCircle(tipX, tipY, 5.5f * d, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        })

        val maxTextW = innerRadius * 1.55f
        val prefMainSize = (maxRadius * 0.28f).coerceIn(18f * d, 32f * d)
        val prefSubSize = (maxRadius * 0.13f).coerceIn(11f * d, 15f * d)

        mainTimePaint.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        CanvasUiHelper.drawFittedText(canvas, formatMs(totalElapsedMs), cx, cy + (4f * d), maxTextW, prefMainSize, 13f * d, mainTimePaint)
        subTimePaint.color = if (isMin) MinimalistTheme.TEXT_MUTED_GREY else secondary
        CanvasUiHelper.drawFittedText(canvas, "הקפה #$lapNumber: ${formatMs(currentLapElapsedMs)}", cx, cy + maxRadius * 0.34f, maxTextW, prefSubSize, 9.5f * d, subTimePaint)
        subTimePaint.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else accent
        CanvasUiHelper.drawFittedText(canvas, "CHRONOGRAPH PRO", cx, cy - maxRadius * 0.32f, maxTextW, prefSubSize, 9.5f * d, subTimePaint)
    }
}

// =========================================================================
// 3. קנבס מצפן חי (מותאם לעיצוב הפסטל השטוח ולכל מצבי התצוגה)
// =========================================================================
class CompassRoseCanvasView(
    context: Context,
    private val activity: MainActivity,
    private val onHeadingChanged: (Float) -> Unit
) : View(context) {
    var azimuthDeg: Float = 0f
    var lockedTargetDeg: Float? = null
    var magneticFieldUt: Float = 0f
    var sensorAccuracy: Int = -1
    var hasSensorHardware: Boolean = true
    var hasReceivedFirstReading: Boolean = false
    var pitchTiltDeg: Float = 0f
    var rollTiltDeg: Float = 0f

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val northPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#FF9E80") }
    private val southPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#9AA5A0") }
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            parent?.requestDisallowInterceptTouchEvent(true)
            val dx = event.x - width / 2f
            val dy = event.y - height * 0.52f
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
        val isMin = activity.isMinimalistMode
        ringPaint.strokeWidth = if (isMin) 1.8f * d else 2.2f * d
        tickPaint.strokeWidth = 1.5f * d

        val cx = width / 2f
        val cy = height * 0.52f
        val r = min(width, height) * 0.34f
        val accent = CanvasUiHelper.ensureReadableAccent(activity.primaryColor, activity.isAuroraMode, isMin)

        val isMagAnomaly = hasReceivedFirstReading && magneticFieldUt > 0.1f && (magneticFieldUt < 20f || magneticFieldUt > 70f)
        val (statusText, statusColor, pillBgColor) = if (isMin) {
            when {
                isMagAnomaly -> Triple(
                    "❕ הפרעה מגנטית (${Utils.formatResult(magneticFieldUt.toDouble(), 1)} µT) - הרחק ממתכת/מגנט",
                    MinimalistTheme.TEXT_CHARCOAL,
                    MinimalistTheme.ACCENT_TERRACOTTA
                )
                !hasSensorHardware || sensorAccuracy == -2 -> Triple(
                    "מצב סיבוב ידני במגע (לא זוהה חיישן מצפן במכשיר)",
                    MinimalistTheme.TEXT_CHARCOAL,
                    MinimalistTheme.ACCENT_BUTTER_YELLOW
                )
                !hasReceivedFirstReading || sensorAccuracy == -1 -> Triple(
                    "מאתחל חיישן מצפן תלת-ממדי... (הזז קלות את המכשיר)",
                    MinimalistTheme.TEXT_CHARCOAL,
                    MinimalistTheme.ACCENT_PASTEL_BLUE
                )
                sensorAccuracy >= SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> Triple(
                    "✓ אמינות מצפן גבוהה (מכויל | פיצוי הטיה 3D פעיל)",
                    MinimalistTheme.TEXT_CHARCOAL,
                    MinimalistTheme.ACCENT_SAGE_GREEN
                )
                else -> Triple(
                    "❕ מומלץ לסובב את המכשיר בתנועת 8 לכיול המצפן",
                    MinimalistTheme.TEXT_CHARCOAL,
                    MinimalistTheme.ACCENT_BUTTER_YELLOW
                )
            }
        } else {
            val (txt, col) = when {
                !hasSensorHardware || sensorAccuracy == -2 ->
                    "⚪ לא זוהה חיישן מצפן במכשיר (מצב סיבוב ידני במגע פעיל)" to Color.parseColor("#90CAF9")
                !hasReceivedFirstReading || sensorAccuracy == -1 ->
                    "⏳ מאתחל חיישן מצפן תלת-ממדי... (הזז קלות את המכשיר)" to Color.parseColor("#80DEEA")
                isMagAnomaly ->
                    "⚠️ הפרעה מגנטית (${Utils.formatResult(magneticFieldUt.toDouble(), 1)} µT) - הרחק מתכת/מגנט" to Color.parseColor("#FF5252")
                sensorAccuracy >= SensorManager.SENSOR_STATUS_ACCURACY_HIGH ->
                    "🟢 אמינות מצפן גבוהה (מכויל | פיצוי הטיה 3D פעיל)" to Color.parseColor("#00E676")
                sensorAccuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM ->
                    "🟡 אמינות בינונית (מומלץ לסובב בתנועת 8 לכיול)" to Color.parseColor("#FFB74D")
                else ->
                    "🔴 אמינות נמוכה - נדרש כיול (סובב את המכשיר בצורת 8)" to Color.parseColor("#FF5252")
            }
            Triple(txt, col, Color.argb(45, Color.red(col), Color.green(col), Color.blue(col)))
        }

        badgePaint.color = pillBgColor
        val badgeRect = RectF(16f * d, 12f * d, width - 16f * d, 40f * d)
        canvas.drawRoundRect(badgeRect, 20f * d, 20f * d, badgePaint)
        labelPaint.color = statusColor
        CanvasUiHelper.drawFittedText(
            canvas,
            statusText,
            cx,
            30.5f * d,
            badgeRect.width() - 18f * d,
            12.0f * d,
            8.2f * d,
            labelPaint
        )

        val dialFillColor = if (isMin) Color.parseColor("#F4F0E8") else Color.argb(165, 12, 20, 30)
        canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dialFillColor; style = Paint.Style.FILL })
        ringPaint.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else accent
        canvas.drawCircle(cx, cy, r, ringPaint)

        canvas.save()
        canvas.rotate(-azimuthDeg, cx, cy)

        tickPaint.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        for (deg in 0 until 360 step 15) {
            val rad = Math.toRadians((deg - 90).toDouble())
            val isMajor = deg % 90 == 0
            val inner = if (isMajor) r * 0.80f else r * 0.88f
            val outer = if (isMin) r * 0.95f else r
            val x1 = (cx + cos(rad) * inner).toFloat()
            val y1 = (cy + sin(rad) * inner).toFloat()
            val x2 = (cx + cos(rad) * outer).toFloat()
            val y2 = (cy + sin(rad) * outer).toFloat()
            tickPaint.strokeWidth = if (isMajor) 2.0f * d else 1.2f * d
            tickPaint.alpha = if (isMajor) 230 else 135
            canvas.drawLine(x1, y1, x2, y2, tickPaint)
        }

        labelPaint.textSize = 14.5f * d
        if (isMin) {
            labelPaint.color = MinimalistTheme.TEXT_CHARCOAL
            canvas.drawText("N", cx, cy - r * 0.62f, labelPaint)
            canvas.drawText("S", cx, cy + r * 0.72f, labelPaint)
            canvas.drawText("E", cx + r * 0.66f, cy + 5f * d, labelPaint)
            canvas.drawText("W", cx - r * 0.66f, cy + 5f * d, labelPaint)
        } else {
            labelPaint.color = Color.parseColor("#FF3B30")
            canvas.drawText("N (צפון)", cx, cy - r * 0.56f, labelPaint)
            labelPaint.color = Color.WHITE
            canvas.drawText("S", cx, cy + r * 0.66f, labelPaint)
            canvas.drawText("E", cx + r * 0.62f, cy + 5f * d, labelPaint)
            canvas.drawText("W", cx - r * 0.62f, cy + 5f * d, labelPaint)
        }

        lockedTargetDeg?.let { target ->
            val tRad = Math.toRadians((target - 90).toDouble())
            val tx = (cx + cos(tRad) * r * 0.92f).toFloat()
            val ty = (cy + sin(tRad) * r * 0.92f).toFloat()
            canvas.drawCircle(tx, ty, 6f * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = MinimalistTheme.ACCENT_WARM_MUSTARD })
        }

        val needleLen = r * 0.45f
        val needleHalfW = if (isMin) r * 0.12f else r * 0.09f
        val northPath = Path().apply {
            moveTo(cx, cy - needleLen)
            lineTo(cx - needleHalfW, cy)
            lineTo(cx + needleHalfW, cy)
            close()
        }
        val southPath = Path().apply {
            moveTo(cx, cy + needleLen)
            lineTo(cx - needleHalfW, cy)
            lineTo(cx + needleHalfW, cy)
            close()
        }
        northPaint.color = if (isMin) MinimalistTheme.ACCENT_MUTED_PEACH else Color.parseColor("#FF3B30")
        canvas.drawPath(southPath, southPaint)
        canvas.drawPath(northPath, northPaint)
        canvas.restore()

        canvas.drawCircle(cx, cy, r * 0.07f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            style = Paint.Style.FILL
        })

        labelPaint.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else accent
        val bottomSummary = if (magneticFieldUt > 0.1f) {
            "שדה מגנטי: \u200E${Utils.formatResult(magneticFieldUt.toDouble(), 1)} µT\u200E (תקין: \u200E25-65 µT\u200E)  |  הטיה: \u200E${Utils.formatResult(hypot(rollTiltDeg.toDouble(), pitchTiltDeg.toDouble()), 1)}°\u200E"
        } else {
            "אזימוט: \u200E${Utils.formatResult(azimuthDeg.toDouble(), 1)}°\u200E  |  ניתן לסובב את המצפן גם במגע"
        }
        CanvasUiHelper.drawFittedText(
            canvas,
            bottomSummary,
            cx,
            height - 12f * d,
            width - 24f * d,
            12.5f * d,
            8.5f * d,
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
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val isMin = (context as? MainActivity)?.isMinimalistMode == true
        wirePaint.strokeWidth = 5f * d
        wirePaint.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.LTGRAY
        borderPaint.strokeWidth = 1.5f * d
        borderPaint.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE

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
        textPaint.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        CanvasUiHelper.drawFittedText(canvas, labelText, w / 2f, h - 14f * d, w - 24f * d, 15f * d, 10f * d, textPaint)
    }
}

// =========================================================================
// 5. קנבס מודל אטום בוהר עם אנימציית אלקטרונים מסתובבים בזמן אמת!
// =========================================================================
class BohrAtomCanvasView(context: Context, private val activity: MainActivity) : View(context) {
    var symbol: String = "Au"
    var atomicNumber: Int = 79
    var shellsConfig: List<Int> = listOf(2, 8, 18, 32, 18, 1)
    var subtitleFormula: String = ""
    private var phaseAngle: Float = 0f

    private val orbitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val electronPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val isMin = activity.isMinimalistMode
        orbitPaint.strokeWidth = 1.4f * d
        val cx = width / 2f
        val cy = if (subtitleFormula.isNotEmpty()) height * 0.46f else height / 2f
        val maxR = min(width, height) * 0.40f
        val accent = if (isMin) MinimalistTheme.ACCENT_MUTED_PEACH else CanvasUiHelper.ensureReadableAccent(activity.primaryColor, activity.isAuroraMode)

        orbitPaint.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.argb(105, Color.red(accent), Color.green(accent), Color.blue(accent))
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
                canvas.drawCircle(ex, ey, 3.8f * d, electronPaint)
            }
        }

        val nucR = maxR * 0.20f
        canvas.drawCircle(
            cx, cy, nucR,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isMin) MinimalistTheme.ACCENT_SAGE_GREEN else Color.argb(225, Color.red(accent), Color.green(accent), Color.blue(accent))
            }
        )
        textPaint.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        textPaint.textSize = (nucR * 0.82f).coerceAtLeast(13f * d)
        canvas.drawText(symbol, cx, cy + textPaint.textSize * 0.34f, textPaint)

        if (subtitleFormula.isNotEmpty()) {
            textPaint.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else accent
            CanvasUiHelper.drawFittedText(canvas, subtitleFormula, cx, height - 10f * d, width - 24f * d, 12.5f * d, 9f * d, textPaint)
        }

        phaseAngle = (phaseAngle + 1.4f) % 360f
        if (isAttachedToWindow) {
            postInvalidateOnAnimation()
        }
    }
}

// =========================================================================
// 6. קנבס מד-זווית דו-זרועי + סרגל וקליבר מכויל פיזית מול כרטיס אשראי (ISO 85.60mm / 53.98mm)
// =========================================================================
class ProtractorRulerCanvasView(
    context: Context,
    private val activity: MainActivity,
    private val onMeasurementChanged: ((Float, Float, Float) -> Unit)? = null
) : View(context) {
    var angleArmA: Float = 25f
    var angleArmB: Float = 115f
    var caliperCm: Float = 5.398f
    var calibrationScaleFactor: Float = 1.0f
    var isCreditCardCalibrationMode: Boolean = false

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val armAPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val armBPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; color = Color.parseColor("#FF9E80") }
    private val cardOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.argb(65, 163, 213, 181) }
    private val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.parseColor("#2E7D32") }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) }

    fun getEffectivePxPerCm(): Float {
        val rawDpi = resources.displayMetrics.ydpi.let {
            if (it in 120f..640f) it else resources.displayMetrics.densityDpi.toFloat()
        }
        return (rawDpi / 2.54f) * calibrationScaleFactor.coerceIn(0.65f, 1.45f)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            parent?.requestDisallowInterceptTouchEvent(true)
            val d = resources.displayMetrics.density
            val topOffset = 24f * d
            val pxPerCm = getEffectivePxPerCm()

            if (isCreditCardCalibrationMode || event.x < 105f * d) {
                if (isCreditCardCalibrationMode) {
                    val rawDpi = resources.displayMetrics.ydpi.let {
                        if (it in 120f..640f) it else resources.displayMetrics.densityDpi.toFloat()
                    }
                    val basePxForCard = (rawDpi / 2.54f) * 5.398f
                    val draggedPx = (event.y - topOffset).coerceIn(basePxForCard * 0.70f, basePxForCard * 1.35f)
                    calibrationScaleFactor = (draggedPx / basePxForCard).coerceIn(0.70f, 1.35f)
                    caliperCm = 5.398f
                } else {
                    caliperCm = ((event.y - topOffset) / pxPerCm).coerceIn(0.05f, 14f)
                }
            } else {
                val ox = width * 0.63f
                val oy = height * 0.70f
                val dx = event.x - ox
                val dy = oy - event.y
                val deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat().coerceIn(0f, 180f)
                if (abs(deg - angleArmA) <= abs(deg - angleArmB)) {
                    angleArmA = deg
                } else {
                    angleArmB = deg
                }
            }
            onMeasurementChanged?.invoke(abs(angleArmB - angleArmA), caliperCm, calibrationScaleFactor)
            invalidate()
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val isMin = activity.isMinimalistMode
        val baseInk = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        val accent = if (isMin) MinimalistTheme.ACCENT_TERRACOTTA else CanvasUiHelper.ensureReadableAccent(activity.primaryColor, activity.isAuroraMode)

        linePaint.color = baseInk
        linePaint.strokeWidth = 1.4f * d
        armAPaint.strokeWidth = 3.2f * d
        armAPaint.color = accent
        armBPaint.strokeWidth = 3.2f * d
        cardBorderPaint.strokeWidth = 2.0f * d

        val pxPerCm = getEffectivePxPerCm()
        val topOffset = 24f * d

        if (isCreditCardCalibrationMode) {
            val cardBottomY = topOffset + 5.398f * pxPerCm
            val cardRect = RectF(0f, topOffset, width * 0.52f, cardBottomY)
            canvas.drawRoundRect(cardRect, 8f * d, 8f * d, cardOverlayPaint)
            canvas.drawRoundRect(cardRect, 8f * d, 8f * d, cardBorderPaint)
            textPaint.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.parseColor("#00E676")
            textPaint.textSize = 11f * d
            canvas.drawText("💳 הנח כרטיס אשראי לאורך הקו העליון", 12f * d, topOffset + 22f * d, textPaint)
            canvas.drawText("וגרור קו זה לקצה התחתון של הכרטיס (53.98 מ״מ)", 12f * d, cardBottomY - 8f * d, textPaint)
        }

        var y = topOffset
        var cm = 0
        textPaint.color = baseInk
        textPaint.textSize = 11f * d
        while (y < height - 22f * d) {
            canvas.drawLine(0f, y, 36f * d, y, linePaint)
            canvas.drawText("${cm}cm", 40f * d, y + 4f * d, textPaint)
            for (mm in 1..9) {
                val my = y + (mm / 10f) * pxPerCm
                if (my < height - 22f * d) {
                    canvas.drawLine(0f, my, if (mm == 5) 25f * d else 14f * d, my, linePaint)
                }
            }
            cm++
            y += pxPerCm
        }

        canvas.drawLine(0f, topOffset, 92f * d, topOffset, armAPaint)
        val caliperY = topOffset + caliperCm * pxPerCm
        canvas.drawLine(0f, caliperY, 92f * d, caliperY, armBPaint)
        canvas.drawCircle(85f * d, caliperY, 5.5f * d, armBPaint)

        val ox = width * 0.63f
        val oy = height * 0.70f
        val r = min(width * 0.33f, height * 0.48f)
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
        val mmVal = caliperCm * 10f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = baseInk
        val summaryLine = "Δ זווית: ${Utils.formatResult(deltaAngle.toDouble(), 1)}°  |  קליבר: ${Utils.formatResult(mmVal.toDouble(), 1)} מ״מ (${Utils.formatResult(caliperCm.toDouble(), 2)} ס״מ)"
        CanvasUiHelper.drawFittedText(
            canvas,
            summaryLine,
            width / 2f,
            height - 8f * d,
            width - 20f * d,
            12.8f * d,
            8.5f * d,
            textPaint
        )
        textPaint.textAlign = Paint.Align.LEFT
    }
}
 

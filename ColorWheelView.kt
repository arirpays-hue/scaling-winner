package com.example.calculator

import android.app.Dialog
import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.Locale
import kotlin.math.*

// =========================================================================
// 1. גלגל צבעים מקצועי ב-60fps (הבורר זז אך ורק בלחיצה/גרירה ישירה על הגלגל!)
// =========================================================================
class ColorWheelView(
    context: Context,
    initialHue: Float,
    initialSat: Float,
    initialValue: Float,
    private val onWheelTouchHueSatChanged: (Float, Float) -> Unit
) : View(context) {

    var onInteractionFinished: (() -> Unit)? = null

    // קואורדינטות הסמן הפיזי על הגלגל (משתנות אך ורק במגע ישיר על הגלגל או בבחירת צבע חדש)
    private var wheelCursorHue: Float = initialHue.coerceIn(0f, 360f)
    private var wheelCursorSat: Float = initialSat.coerceIn(0f, 1f)

    // הצבע הפעיל המוצג בתוך טבעת הסמן (מתעדכן גם מהסליידרים מבלי להזיז את מיקום הסמן!)
    private var previewColor: Int = Color.HSVToColor(floatArrayOf(wheelCursorHue, wheelCursorSat, initialValue.coerceIn(0f, 1f)))

    private val density = context.resources.displayMetrics.density
    private val wheelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * density
        color = Color.argb(115, 160, 160, 160)
    }
    private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
        color = Color.argb(120, 255, 255, 255)
    }
    private val selOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        color = Color.WHITE
    }
    private val selInnerStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        color = Color.BLACK
    }
    private val selFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private var cx = 0f
    private var cy = 0f
    private var r = 0f
    private var isDragging = false

    // מעדכן רק את צבע המילוי של הסמן מבלי להזיז את מיקום הסמן על הגלגל!
    fun updateActiveColorOnly(activeColor: Int) {
        previewColor = activeColor
        invalidate()
    }

    // מעדכן את מיקום הסמן רק כאשר נבחר אלמנט חדש, פלטה מהירה או קוד HEX
    fun setCursorPositionExplicitly(h: Float, s: Float, activeColor: Int) {
        wheelCursorHue = h.coerceIn(0f, 360f)
        wheelCursorSat = s.coerceIn(0f, 1f)
        previewColor = activeColor
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        cx = w / 2f
        cy = h / 2f
        r = (min(w, h) / 2f) - (14f * density)
        if (r > 0f) {
            val sweepColors = intArrayOf(
                Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
            )
            wheelPaint.shader = ComposeShader(
                SweepGradient(cx, cy, sweepColors, null),
                RadialGradient(cx, cy, r, Color.WHITE, 0x00FFFFFF, Shader.TileMode.CLAMP),
                PorterDuff.Mode.SRC_OVER
            )
        }
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        if (r <= 0f) return

        c.drawCircle(cx, cy, r, wheelPaint)
        c.drawCircle(cx, cy, r, outerRingPaint)

        val rad = Math.toRadians(wheelCursorHue.toDouble())
        val dist = wheelCursorSat * r
        val sx = (cx + dist * cos(rad)).toFloat()
        val sy = (cy + dist * sin(rad)).toFloat()

        if (isDragging) {
            c.drawLine(cx - r, sy, cx + r, sy, crosshairPaint)
            c.drawLine(sx, cy - r, sx, cy + r, crosshairPaint)
        }

        val knobR = (if (isDragging) 13.5f else 11f) * density
        val opaquePreview = Color.rgb(Color.red(previewColor), Color.green(previewColor), Color.blue(previewColor))

        selFillPaint.color = opaquePreview
        c.drawCircle(sx, sy, knobR, selFillPaint)
        c.drawCircle(sx, sy, knobR, selOuterPaint)
        c.drawCircle(sx, sy, knobR - 2.1f * density, selInnerStrokePaint)

        if (isDragging) {
            val loupeR = 19f * density
            val loupeY = (sy - 34f * density).coerceAtLeast(loupeR + 4f * density)
            selFillPaint.color = opaquePreview
            c.drawCircle(sx, loupeY, loupeR, selFillPaint)
            c.drawCircle(sx, loupeY, loupeR, selOuterPaint)
            c.drawCircle(sx, loupeY, loupeR - 2f * density, selInnerStrokePaint)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                isDragging = true
                val dx = e.x - cx
                val dy = e.y - cy
                wheelCursorSat = (hypot(dx.toDouble(), dy.toDouble()).toFloat() / r.coerceAtLeast(1f)).coerceIn(0f, 1f)
                var a = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                if (a < 0f) a += 360f
                wheelCursorHue = a.coerceIn(0f, 360f)
                onWheelTouchHueSatChanged(wheelCursorHue, wheelCursorSat)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                onInteractionFinished?.invoke()
                return true
            }
        }
        return super.onTouchEvent(e)
    }
}

// =========================================================================
// 2. סמלים וקטוריים מותאמים לכל 4 ערכות הנושא עבור 4 הסליידרים
//    sliderType: 0 = גוון (Hue), 1 = רוויה (Saturation), 2 = בהירות (Brightness), 3 = שקיפות (Opacity)
// =========================================================================
fun createSliderThemeVectorIcon(
    context: Context,
    sliderType: Int,
    tintColor: Int,
    isMinimalist: Boolean,
    isAurora: Boolean,
    isGlass: Boolean
): Drawable {
    val d = context.resources.displayMetrics.density
    val sizePx = (20f * d).toInt().coerceAtLeast(22)
    return object : Drawable() {
        private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tintColor
            style = Paint.Style.STROKE
            strokeWidth = 1.75f * d
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            if (isAurora) {
                setShadowLayer(4.5f * d, 0f, 0f, Color.argb(165, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor)))
            }
        }
        private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tintColor
            style = Paint.Style.FILL
        }
        private val badgeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = when {
                isMinimalist -> when (sliderType) {
                    0 -> MinimalistTheme.ACCENT_BUTTER_YELLOW
                    1 -> MinimalistTheme.ACCENT_PASTEL_BLUE
                    2 -> MinimalistTheme.ACCENT_MUTED_PEACH
                    else -> MinimalistTheme.ACCENT_MUTED_GREEN
                }
                isAurora || isGlass -> Color.argb(38, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor))
                else -> Color.argb(28, 255, 255, 255)
            }
        }

        override fun getIntrinsicWidth(): Int = sizePx
        override fun getIntrinsicHeight(): Int = sizePx

        override fun draw(canvas: Canvas) {
            val cx = bounds.exactCenterX()
            val cy = bounds.exactCenterY()
            val r = min(bounds.width(), bounds.height()) * 0.42f

            if (isMinimalist) {
                canvas.drawCircle(cx, cy, r * 1.05f, badgeP)
            } else {
                val rect = RectF(cx - r * 1.08f, cy - r * 1.08f, cx + r * 1.08f, cy + r * 1.08f)
                canvas.drawRoundRect(rect, r * 0.42f, r * 0.42f, badgeP)
            }

            when (sliderType) {
                0 -> {
                    // 0: סמל גוון (Hue) - גלגל ספקטרום עם מחוג ונקודות גוון
                    canvas.drawCircle(cx, cy, r * 0.72f, strokeP)
                    canvas.drawCircle(cx, cy, r * 0.16f, fillP)
                    canvas.drawLine(cx, cy, cx + r * 0.48f, cy - r * 0.42f, strokeP)
                }
                1 -> {
                    // 1: סמל רוויה (Saturation) - טיפת צבע עם מפלס פנימי
                    val drop = Path().apply {
                        moveTo(cx, cy - r * 0.78f)
                        quadTo(cx + r * 0.68f, cy + r * 0.12f, cx, cy + r * 0.76f)
                        quadTo(cx - r * 0.68f, cy + r * 0.12f, cx, cy - r * 0.78f)
                        close()
                    }
                    canvas.drawPath(drop, strokeP)
                    canvas.drawArc(
                        RectF(cx - r * 0.36f, cy - r * 0.05f, cx + r * 0.36f, cy + r * 0.56f),
                        0f, 180f, true, fillP
                    )
                }
                2 -> {
                    // 2: סמל בהירות (Brightness) - שמש ו-8 קרניים
                    canvas.drawCircle(cx, cy, r * 0.34f, strokeP)
                    for (i in 0 until 8) {
                        val a = Math.toRadians((i * 45).toDouble())
                        canvas.drawLine(
                            (cx + cos(a) * r * 0.50f).toFloat(),
                            (cy + sin(a) * r * 0.50f).toFloat(),
                            (cx + cos(a) * r * 0.78f).toFloat(),
                            (cy + sin(a) * r * 0.78f).toFloat(),
                            strokeP
                        )
                    }
                }
                else -> {
                    // 3: סמל שקיפות (Opacity) - שני עיגולים חופפים (אלפא/שכבות)
                    canvas.drawCircle(cx - r * 0.24f, cy - r * 0.08f, r * 0.46f, strokeP)
                    canvas.drawCircle(cx + r * 0.24f, cy + r * 0.12f, r * 0.46f, strokeP)
                }
            }
        }

        override fun setAlpha(alpha: Int) { strokeP.alpha = alpha }
        override fun setColorFilter(colorFilter: ColorFilter?) { strokeP.colorFilter = colorFilter }
        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }.apply { setBounds(0, 0, sizePx, sizePx) }
}

// =========================================================================
// 3. סליידר גרדיאנט חי ב-60fps (מוגן מחטיפת מגע של ScrollView)
// =========================================================================
class ProGradientSliderView(
    context: Context,
    initialProgress: Float = 1.0f,
    private val onProgressChanged: (Float, Boolean) -> Unit
) : View(context) {

    var progress: Float = initialProgress.coerceIn(0f, 1f)
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    var onTouchReleased: (() -> Unit)? = null

    private var gradientColors: IntArray = intArrayOf(Color.BLACK, Color.WHITE)
    private var showCheckerboard: Boolean = false
    private val d = resources.displayMetrics.density
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * d
        color = Color.argb(100, 140, 140, 140)
    }
    private val checkerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val thumbFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val thumbStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.4f * d
        color = Color.parseColor("#2C2C2C")
    }

    fun setTrackGradient(colors: IntArray, withCheckerboard: Boolean = false) {
        gradientColors = colors
        showCheckerboard = withCheckerboard
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val pad = 14f * d
                val usableW = (width - 2f * pad).coerceAtLeast(1f)
                val newProg = ((event.x - pad) / usableW).coerceIn(0f, 1f)
                progress = newProg
                onProgressChanged(progress, true)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                onTouchReleased?.invoke()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pad = 14f * d
        val trackH = 13f * d
        val cy = height / 2f
        val rect = RectF(pad, cy - trackH / 2f, width - pad, cy + trackH / 2f)
        val corner = trackH / 2f

        if (showCheckerboard) {
            canvas.save()
            val clipPath = Path().apply { addRoundRect(rect, corner, corner, Path.Direction.CW) }
            canvas.clipPath(clipPath)
            val sq = 4.5f * d
            var y = rect.top
            var row = 0
            while (y < rect.bottom) {
                var x = rect.left
                var col = row % 2
                while (x < rect.right) {
                    checkerPaint.color = if (col % 2 == 0) Color.LTGRAY else Color.WHITE
                    canvas.drawRect(x, y, x + sq, y + sq, checkerPaint)
                    x += sq
                    col++
                }
                y += sq
                row++
            }
            canvas.restore()
        }

        val positions = if (gradientColors.size > 2) null else floatArrayOf(0f, 1f)
        trackPaint.shader = LinearGradient(
            rect.left, cy, rect.right, cy,
            gradientColors, positions, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, corner, corner, trackPaint)
        canvas.drawRoundRect(rect, corner, corner, strokePaint)

        val kx = rect.left + progress * rect.width()
        val kr = 10f * d
        canvas.drawCircle(kx, cy, kr, thumbFillPaint)
        canvas.drawCircle(kx, cy, kr, thumbStrokePaint)
    }
}

// =========================================================================
// 4. קומפוננטת בורר הצבעים המלאה (createColorWheelPicker)
// =========================================================================
fun createColorWheelPicker(
    context: Context,
    initialColor: Int,
    onColorChanged: (Int) -> Unit
): View {
    fun dpLocal(v: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, context.resources.displayMetrics).toInt()
    val act = context as? MainActivity
    val isMin = act?.isMinimalistMode == true
    val isAurora = act?.isAuroraMode == true
    val isGlass = act?.isIosGlassMode == true
    val isCleanGlass = act?.isCleanGlassActive == true

    val hsv = FloatArray(3)
    Color.colorToHSV(initialColor, hsv)
    var currentAlpha = Color.alpha(initialColor)
    var isSyncingUi = false

    val cardInk = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
    val iconTint = when {
        isMin -> MinimalistTheme.TEXT_CHARCOAL
        isAurora -> AuroraBorealisTheme.AURORA_PRIMARY
        else -> act?.primaryColor ?: Color.WHITE
    }

    // כרטיס נקי (ללא צהוב קבוע שמסתיר את רקע האפליקציה)
    val card = LinearLayout(context).apply {
        tag = "MINIMALIST_CUSTOMIZER_IGNORE"
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dpLocal(14f), dpLocal(12f), dpLocal(14f), dpLocal(12f))
        background = when {
            isMin -> GradientDrawable().apply {
                cornerRadius = dpLocal(22f).toFloat()
                setColor(Color.parseColor("#FFFFFF"))
                setStroke(dpLocal(1.2f), Color.parseColor("#E3DFD9"))
            }
            act != null && isCleanGlass -> IosGlassTheme.createGlassCardDrawable(act, 22f)
            else -> GradientDrawable().apply {
                setColor(Utils.lightenColor(act?.bgColor ?: Color.parseColor("#14171F"), 0.08f))
                cornerRadius = dpLocal(22f).toFloat()
                setStroke(dpLocal(1.2f), act?.primaryColor ?: Color.WHITE)
            }
        }
    }

    // שורת תצוגה מקדימה קומפקטית בראש הכרטיס
    val previewRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dpLocal(6f) }
    }

    val previewCircle = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(dpLocal(40f), dpLocal(40f)).apply { rightMargin = dpLocal(10f) }
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(initialColor)
            setStroke(dpLocal(1.8f), cardInk)
        }
    }

    val infoCol = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    }

    val hexLabel = TextView(context).apply {
        text = String.format(Locale.US, "#%08X", initialColor)
        setTextColor(cardInk)
        textSize = 14.5f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    val rgbHsvSubLabel = TextView(context).apply {
        setTextColor(if (isMin) MinimalistTheme.TEXT_MUTED_GREY else Color.LTGRAY)
        textSize = 11f
        typeface = Typeface.MONOSPACE
    }

    val editHexBtn = TextView(context).apply {
        tag = "PRO_STAR_CHIP"
        text = "✎ הזן HEX"
        setTextColor(cardInk)
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setPadding(dpLocal(11f), dpLocal(6f), dpLocal(11f), dpLocal(6f))
        background = GradientDrawable().apply {
            cornerRadius = dpLocal(999f).toFloat()
            setColor(if (isMin) MinimalistTheme.NEUTRAL_SECONDARY_BG else Color.argb(55, 255, 255, 255))
            setStroke(dpLocal(1f), cardInk)
        }
    }

    infoCol.addView(hexLabel)
    infoCol.addView(rgbHsvSubLabel)
    previewRow.addView(previewCircle)
    previewRow.addView(infoCol)
    previewRow.addView(editHexBtn)

    lateinit var wheelView: ColorWheelView
    lateinit var hueSlider: ProGradientSliderView
    lateinit var satSlider: ProGradientSliderView
    lateinit var brightSlider: ProGradientSliderView
    lateinit var alphaSlider: ProGradientSliderView

    lateinit var hueValTv: TextView
    lateinit var satValTv: TextView
    lateinit var brightValTv: TextView
    lateinit var alphaValTv: TextView

    val harmonyCircles = Array(5) { View(context) }
    val harmonyColors = IntArray(5)

    // moveWheelCursor = false כאשר הגרירה מגיעה מהסליידרים (כך שהבורר בתוך גלגל הצבעים לא יזוז!)
    fun refreshAllVisualsAndNotify(moveWheelCursor: Boolean, notifyListener: Boolean = true) {
        if (isSyncingUi) return
        isSyncingUi = true
        try {
            val finalColor = Color.HSVToColor(currentAlpha, hsv)
            (previewCircle.background as? GradientDrawable)?.setColor(finalColor)
            hexLabel.text = String.format(Locale.US, "#%08X", finalColor)
            val r = Color.red(finalColor)
            val g = Color.green(finalColor)
            val b = Color.blue(finalColor)
            rgbHsvSubLabel.text = String.format(
                Locale.US,
                "RGB(%d,%d,%d) • H:%d° S:%d%% V:%d%%",
                r, g, b, hsv[0].roundToInt(), (hsv[1] * 100).roundToInt(), (hsv[2] * 100).roundToInt()
            )

            if (moveWheelCursor) {
                wheelView.setCursorPositionExplicitly(hsv[0], hsv[1], finalColor)
            } else {
                wheelView.updateActiveColorOnly(finalColor)
            }

            hueSlider.progress = (hsv[0] / 360f).coerceIn(0f, 1f)
            satSlider.progress = hsv[1].coerceIn(0f, 1f)
            brightSlider.progress = hsv[2].coerceIn(0f, 1f)
            alphaSlider.progress = (currentAlpha / 255f).coerceIn(0f, 1f)

            hueValTv.text = "${hsv[0].roundToInt()}°"
            satValTv.text = "${(hsv[1] * 100).roundToInt()}%"
            brightValTv.text = "${(hsv[2] * 100).roundToInt()}%"
            alphaValTv.text = "${((currentAlpha / 255f) * 100).roundToInt()}%"

            val zeroSatColor = Color.HSVToColor(floatArrayOf(hsv[0], 0f, hsv[2].coerceAtLeast(0.15f)))
            val fullSatColor = Color.HSVToColor(floatArrayOf(hsv[0], 1f, hsv[2].coerceAtLeast(0.15f)))
            val fullBrightColor = Color.HSVToColor(floatArrayOf(hsv[0], hsv[1], 1f))
            val opaqueColor = Color.HSVToColor(255, hsv)
            val transparentColor = Color.HSVToColor(0, hsv)

            satSlider.setTrackGradient(intArrayOf(zeroSatColor, fullSatColor), false)
            brightSlider.setTrackGradient(intArrayOf(Color.BLACK, fullBrightColor), false)
            alphaSlider.setTrackGradient(intArrayOf(transparentColor, opaqueColor), true)

            val hShifts = floatArrayOf(0f, 180f, 30f, 330f, 120f)
            for (i in 0 until 5) {
                val hCol = Color.HSVToColor(
                    currentAlpha,
                    floatArrayOf((hsv[0] + hShifts[i]) % 360f, hsv[1].coerceAtLeast(0.25f), hsv[2].coerceAtLeast(0.25f))
                )
                harmonyColors[i] = hCol
                (harmonyCircles[i].background as? GradientDrawable)?.setColor(hCol)
            }

            if (notifyListener) {
                onColorChanged(finalColor)
            }
        } finally {
            isSyncingUi = false
        }
    }

    wheelView = ColorWheelView(context, hsv[0], hsv[1], hsv[2]) { h, s ->
        hsv[0] = h
        hsv[1] = s
        if (hsv[2] < 0.08f) hsv[2] = 1.0f
        refreshAllVisualsAndNotify(moveWheelCursor = true, notifyListener = true)
    }.apply {
        layoutParams = LinearLayout.LayoutParams(dpLocal(188f), dpLocal(188f)).apply {
            bottomMargin = dpLocal(6f)
        }
    }

    fun buildSliderControlRow(
        sliderType: Int,
        titleText: String,
        sliderView: ProGradientSliderView,
        valueTv: TextView,
        stepDelta: Float,
        onStepTriggered: (Float) -> Unit
    ): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpLocal(4f) }
        }

        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        valueTv.apply {
            setTextColor(cardInk)
            textSize = 12f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val sliderIcon = createSliderThemeVectorIcon(context, sliderType, iconTint, isMin, isAurora, isGlass)
        val titleTv = TextView(context).apply {
            text = titleText
            setTextColor(cardInk)
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            compoundDrawablePadding = dpLocal(6f)
            setCompoundDrawables(null, null, sliderIcon, null)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerRow.addView(valueTv)
        headerRow.addView(titleTv)

        val controlsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }

        fun makeStepButton(symbol: String, delta: Float): TextView {
            return TextView(context).apply {
                tag = "PRO_STAR_CHIP"
                text = symbol
                setTextColor(cardInk)
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (isMin) MinimalistTheme.NEUTRAL_SECONDARY_BG else Color.argb(45, 255, 255, 255))
                    setStroke(dpLocal(1f), Color.argb(80, Color.red(cardInk), Color.green(cardInk), Color.blue(cardInk)))
                }
                layoutParams = LinearLayout.LayoutParams(dpLocal(26f), dpLocal(26f))
                setOnClickListener {
                    act?.feedback(it)
                    val next = (sliderView.progress + delta).coerceIn(0f, 1f)
                    sliderView.progress = next
                    onStepTriggered(next)
                }
            }
        }

        val minusBtn = makeStepButton("−", -stepDelta)
        val plusBtn = makeStepButton("+", stepDelta)
        sliderView.layoutParams = LinearLayout.LayoutParams(0, dpLocal(30f), 1f)

        controlsRow.addView(minusBtn)
        controlsRow.addView(sliderView)
        controlsRow.addView(plusBtn)

        container.addView(headerRow)
        container.addView(controlsRow)
        return container
    }

    hueValTv = TextView(context)
    satValTv = TextView(context)
    brightValTv = TextView(context)
    alphaValTv = TextView(context)

    // הזזת הסליידרים קוראת ל-refreshAllVisualsAndNotify(moveWheelCursor = false) כדי שהבורר בגלגל הצבעים לא יזוז!
    hueSlider = ProGradientSliderView(context, hsv[0] / 360f) { prog, _ ->
        hsv[0] = (prog * 360f).coerceIn(0f, 360f)
        refreshAllVisualsAndNotify(moveWheelCursor = false, notifyListener = true)
    }.apply {
        setTrackGradient(
            intArrayOf(Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED),
            false
        )
    }

    satSlider = ProGradientSliderView(context, hsv[1]) { prog, _ ->
        hsv[1] = prog.coerceIn(0f, 1f)
        refreshAllVisualsAndNotify(moveWheelCursor = false, notifyListener = true)
    }

    brightSlider = ProGradientSliderView(context, hsv[2]) { prog, _ ->
        hsv[2] = prog.coerceIn(0f, 1f)
        refreshAllVisualsAndNotify(moveWheelCursor = false, notifyListener = true)
    }

    alphaSlider = ProGradientSliderView(context, currentAlpha / 255f) { prog, _ ->
        currentAlpha = (prog * 255f).roundToInt().coerceIn(0, 255)
        refreshAllVisualsAndNotify(moveWheelCursor = false, notifyListener = true)
    }

    val harmoniesHeader = TextView(context).apply {
        text = "✨ הרמוניות צבעים חיות (משלים, סמוכים וטריאדה):"
        setTextColor(cardInk)
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.RIGHT
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dpLocal(4f); bottomMargin = dpLocal(3f) }
    }

    val harmoniesRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dpLocal(6f) }
    }
    val harmonyNames = listOf("נוכחי", "משלים", "סמוך 1", "סמוך 2", "טריאדה")
    for (i in 0 until 5) {
        val itemCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val swatch = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpLocal(30f), dpLocal(30f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setStroke(dpLocal(1.4f), cardInk)
            }
            setOnClickListener {
                act?.feedback(it)
                val picked = harmonyColors[i]
                Color.colorToHSV(picked, hsv)
                refreshAllVisualsAndNotify(moveWheelCursor = true, notifyListener = true)
            }
        }
        harmonyCircles[i] = swatch
        val lbl = TextView(context).apply {
            text = harmonyNames[i]
            setTextColor(cardInk)
            textSize = 10f
            gravity = Gravity.CENTER
        }
        itemCol.addView(swatch)
        itemCol.addView(lbl)
        harmoniesRow.addView(itemCol)
    }

    val quickSwatchesHeader = TextView(context).apply {
        text = "🎨 גווני קצה מהירים (שחור מוחלט, לבן טהור, ניאון ופסטל):"
        setTextColor(cardInk)
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.RIGHT
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dpLocal(3f) }
    }

    val presetColors = listOf(
        "#FFFFFF" to "לבן",
        "#000000" to "שחור",
        "#FCF9F5" to "שמנת",
        "#2C2C2C" to "פחם",
        "#08090C" to "לילה",
        "#00F5D4" to "אורורה",
        "#FF9F0A" to "ענבר",
        "#64B5F6" to "קרח",
        "#FFA48E" to "טרקוטה",
        "#A3D5B5" to "מרווה",
        "#FFE094" to "חמאה",
        "#FF1744" to "רובי",
        "#B388FF" to "אמטיסט",
        "#FFD700" to "זהב"
    )

    val presetsScroll = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }
    val presetsRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dpLocal(2f), dpLocal(2f), dpLocal(2f), dpLocal(2f))
    }

    for ((hexStr, name) in presetColors) {
        val presetCol = Color.parseColor(hexStr)
        val chip = TextView(context).apply {
            tag = "PRO_STAR_CHIP"
            text = name
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(MinimalistTheme.ensureReadableTextOnBg(Color.WHITE, presetCol, 2.5))
            gravity = Gravity.CENTER
            setPadding(dpLocal(11f), dpLocal(6f), dpLocal(11f), dpLocal(6f))
            background = GradientDrawable().apply {
                cornerRadius = dpLocal(999f).toFloat()
                setColor(presetCol)
                setStroke(dpLocal(1.1f), cardInk)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dpLocal(3f), 0, dpLocal(3f), 0) }
            setOnClickListener {
                act?.feedback(it)
                Color.colorToHSV(presetCol, hsv)
                currentAlpha = 255
                refreshAllVisualsAndNotify(moveWheelCursor = true, notifyListener = true)
            }
        }
        presetsRow.addView(chip)
    }
    presetsScroll.addView(presetsRow)

    val openHexDialogClickListener = View.OnClickListener {
        act?.feedback(it)
        val dlg = Dialog(context)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpLocal(20f), dpLocal(20f), dpLocal(20f), dpLocal(18f))
            background = GradientDrawable().apply {
                cornerRadius = dpLocal(24f).toFloat()
                setColor(if (isMin) MinimalistTheme.BG_CREAM else Color.parseColor("#1E222B"))
                setStroke(dpLocal(1.2f), cardInk)
            }
        }
        val title = TextView(context).apply {
            text = "הזן קוד צבע HEX (#RRGGBB או #AARRGGBB)"
            setTextColor(if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(0, 0, 0, dpLocal(10f))
        }
        val hexEdit = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            setText(String.format(Locale.US, "#%06X", 0xFFFFFF and Color.HSVToColor(currentAlpha, hsv)))
            setTextColor(if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE)
            setPadding(dpLocal(14f), dpLocal(12f), dpLocal(14f), dpLocal(12f))
            background = GradientDrawable().apply {
                cornerRadius = dpLocal(14f).toFloat()
                setColor(if (isMin) Color.WHITE else Color.argb(40, 255, 255, 255))
                setStroke(dpLocal(1f), cardInk)
            }
        }
        val applyBtn = TextView(context).apply {
            text = "החל צבע ✔️"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dpLocal(14f), dpLocal(12f), dpLocal(14f), dpLocal(12f))
            background = GradientDrawable().apply {
                cornerRadius = dpLocal(999f).toFloat()
                setColor(Color.parseColor("#2E7D32"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dpLocal(12f) }
            setOnClickListener {
                val raw = hexEdit.text.toString().trim().removePrefix("#")
                try {
                    val parsed = Color.parseColor("#$raw")
                    Color.colorToHSV(parsed, hsv)
                    currentAlpha = if (raw.length == 8) Color.alpha(parsed) else 255
                    refreshAllVisualsAndNotify(moveWheelCursor = true, notifyListener = true)
                    dlg.dismiss()
                } catch (_: Exception) {
                    Toast.makeText(context, "קוד HEX לא תקין (למשל #00F5D4)", Toast.LENGTH_SHORT).show()
                }
            }
        }
        box.addView(title); box.addView(hexEdit); box.addView(applyBtn)
        dlg.setContentView(box)
        dlg.show()
        dlg.window?.setLayout((context.resources.displayMetrics.widthPixels * 0.88f).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
    }

    editHexBtn.setOnClickListener(openHexDialogClickListener)
    hexLabel.setOnClickListener(openHexDialogClickListener)
    previewCircle.setOnClickListener {
        act?.copyToClipboard(hexLabel.text.toString())
    }

    card.addView(previewRow)
    card.addView(wheelView)
    card.addView(
        buildSliderControlRow(0, "גוון (Hue 0°–360°)", hueSlider, hueValTv, 1f / 360f) { prog ->
            hsv[0] = (prog * 360f).coerceIn(0f, 360f)
            refreshAllVisualsAndNotify(moveWheelCursor = false, notifyListener = true)
        }
    )
    card.addView(
        buildSliderControlRow(1, "רוויה (Saturation 0%–100%)", satSlider, satValTv, 0.02f) { prog ->
            hsv[1] = prog.coerceIn(0f, 1f)
            refreshAllVisualsAndNotify(moveWheelCursor = false, notifyListener = true)
        }
    )
    card.addView(
        buildSliderControlRow(2, "בהירות מלאה (Brightness 0%–100%)", brightSlider, brightValTv, 0.02f) { prog ->
            hsv[2] = prog.coerceIn(0f, 1f)
            refreshAllVisualsAndNotify(moveWheelCursor = false, notifyListener = true)
        }
    )
    card.addView(
        buildSliderControlRow(3, "שקיפות / מיזוג (Opacity 0%–100%)", alphaSlider, alphaValTv, 0.02f) { prog ->
            currentAlpha = (prog * 255f).roundToInt().coerceIn(0, 255)
            refreshAllVisualsAndNotify(moveWheelCursor = false, notifyListener = true)
        }
    )
    card.addView(harmoniesHeader)
    card.addView(harmoniesRow)
    card.addView(quickSwatchesHeader)
    card.addView(presetsScroll)

    refreshAllVisualsAndNotify(moveWheelCursor = true, notifyListener = false)
    return card
}

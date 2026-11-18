package com.example.calculator

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.Locale
import kotlin.math.*

// =========================================================================
// מעטפת עיצוב קנבס אחידה המותאמת אוטומטית לכל 4 ערכות הנושא
// (Minimalist / Aurora Borealis / Liquid Glass / Classic)
// =========================================================================
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
// סמלים וקטוריים מותאמים לכל 4 ערכות הנושא עבור הכלים החדשים (Tabs 81..95)
// =========================================================================
object ProFinancialAndMathIcons {
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
                    setShadowLayer(4.5f * d, 0f, 0f, Color.argb(165, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor)))
                }
            }
            private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.FILL
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }

            override fun getIntrinsicWidth(): Int = sizePx
            override fun getIntrinsicHeight(): Int = sizePx

            override fun draw(canvas: Canvas) {
                val cx = bounds.exactCenterX()
                val cy = bounds.exactCenterY()
                val r = min(bounds.width(), bounds.height()) * 0.40f

                when (tabIndex) {
                    81 -> {
                        fillP.textSize = r * 1.05f
                        canvas.drawText("Aa", cx, cy + r * 0.25f, fillP)
                        canvas.drawLine(cx - r * 0.65f, cy + r * 0.65f, cx + r * 0.65f, cy + r * 0.65f, strokeP)
                    }
                    82 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.78f, cy - r * 0.78f, cx + r * 0.78f, cy + r * 0.78f), r * 0.22f, r * 0.22f, strokeP)
                        canvas.drawLine(cx - r * 0.35f, cy - r * 0.38f, cx + r * 0.35f, cy + r * 0.38f, strokeP)
                        canvas.drawLine(cx + r * 0.35f, cy - r * 0.38f, cx - r * 0.35f, cy + r * 0.38f, strokeP)
                    }
                    83 -> {
                        fillP.textSize = r * 0.92f
                        canvas.drawText("123", cx, cy + r * 0.32f, fillP)
                    }
                    84 -> {
                        canvas.drawCircle(cx, cy, r * 0.78f, strokeP)
                        canvas.drawLine(cx, cy, cx, cy - r * 0.78f, strokeP)
                        canvas.drawLine(cx, cy, cx + r * 0.68f, cy + r * 0.32f, strokeP)
                    }
                    85 -> {
                        canvas.drawLine(cx - r * 0.75f, cy + r * 0.65f, cx + r * 0.75f, cy + r * 0.65f, strokeP)
                        canvas.drawLine(cx - r * 0.65f, cy - r * 0.75f, cx - r * 0.65f, cy + r * 0.75f, strokeP)
                        canvas.drawLine(cx - r * 0.55f, cy + r * 0.45f, cx + r * 0.65f, cy - r * 0.65f, strokeP)
                    }
                    86 -> {
                        canvas.drawCircle(cx, cy, r * 0.78f, strokeP)
                        canvas.drawCircle(cx, cy, r * 0.38f, strokeP)
                        canvas.drawCircle(cx, cy, r * 0.14f, fillP)
                    }
                    87 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.80f, cy - r * 0.50f, cx + r * 0.80f, cy + r * 0.50f), r * 0.16f, r * 0.16f, strokeP)
                        canvas.drawCircle(cx, cy, r * 0.22f, strokeP)
                        canvas.drawLine(cx - r * 0.45f, cy - r * 0.82f, cx + r * 0.45f, cy - r * 0.82f, strokeP)
                    }
                    88 -> {
                        canvas.drawLine(cx - r * 0.60f, cy + r * 0.60f, cx + r * 0.60f, cy - r * 0.60f, strokeP)
                        canvas.drawCircle(cx - r * 0.38f, cy - r * 0.38f, r * 0.18f, strokeP)
                        canvas.drawCircle(cx + r * 0.38f, cy + r * 0.38f, r * 0.18f, strokeP)
                    }
                    89 -> {
                        canvas.drawLine(cx - r * 0.75f, cy + r * 0.45f, cx - r * 0.20f, cy - r * 0.10f, strokeP)
                        canvas.drawLine(cx - r * 0.20f, cy - r * 0.10f, cx + r * 0.18f, cy + r * 0.22f, strokeP)
                        canvas.drawLine(cx + r * 0.18f, cy + r * 0.22f, cx + r * 0.75f, cy - r * 0.55f, strokeP)
                        canvas.drawCircle(cx + r * 0.75f, cy - r * 0.55f, r * 0.13f, fillP)
                    }
                    90 -> {
                        val roof = Path().apply {
                            moveTo(cx - r * 0.82f, cy - r * 0.22f)
                            lineTo(cx, cy - r * 0.80f)
                            lineTo(cx + r * 0.82f, cy - r * 0.22f)
                            close()
                        }
                        canvas.drawPath(roof, strokeP)
                        canvas.drawRect(cx - r * 0.58f, cy - r * 0.22f, cx + r * 0.58f, cy + r * 0.68f, strokeP)
                    }
                    91 -> {
                        canvas.drawArc(RectF(cx - r * 0.78f, cy - r * 0.78f, cx + r * 0.78f, cy + r * 0.78f), -60f, 290f, false, strokeP)
                        canvas.drawLine(cx, cy - r * 0.38f, cx, cy + r * 0.38f, strokeP)
                        canvas.drawLine(cx - r * 0.38f, cy, cx + r * 0.38f, cy, strokeP)
                    }
                    92 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.78f, cy - r * 0.12f, cx + r * 0.78f, cy + r * 0.45f), r * 0.16f, r * 0.16f, strokeP)
                        canvas.drawLine(cx - r * 0.48f, cy - r * 0.12f, cx - r * 0.28f, cy - r * 0.55f, strokeP)
                        canvas.drawLine(cx - r * 0.28f, cy - r * 0.55f, cx + r * 0.28f, cy - r * 0.55f, strokeP)
                        canvas.drawLine(cx + r * 0.28f, cy - r * 0.55f, cx + r * 0.48f, cy - r * 0.12f, strokeP)
                        canvas.drawCircle(cx - r * 0.45f, cy + r * 0.55f, r * 0.16f, fillP)
                        canvas.drawCircle(cx + r * 0.45f, cy + r * 0.55f, r * 0.16f, fillP)
                    }
                    93 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.76f, cy - r * 0.65f, cx + r * 0.76f, cy + r * 0.76f), r * 0.18f, r * 0.18f, strokeP)
                        canvas.drawLine(cx - r * 0.35f, cy + r * 0.15f, cx + r * 0.35f, cy + r * 0.15f, strokeP)
                        canvas.drawLine(cx + r * 0.10f, cy - r * 0.12f, cx + r * 0.35f, cy + r * 0.15f, strokeP)
                        canvas.drawLine(cx + r * 0.10f, cy + r * 0.42f, cx + r * 0.35f, cy + r * 0.15f, strokeP)
                    }
                    94 -> {
                        canvas.drawCircle(cx - r * 0.36f, cy, r * 0.36f, strokeP)
                        canvas.drawCircle(cx + r * 0.36f, cy, r * 0.36f, strokeP)
                    }
                    else -> {
                        val house = Path().apply {
                            moveTo(cx - r * 0.78f, cy - r * 0.05f)
                            lineTo(cx, cy - r * 0.78f)
                            lineTo(cx + r * 0.78f, cy - r * 0.05f)
                        }
                        canvas.drawPath(house, strokeP)
                        canvas.drawRect(cx - r * 0.52f, cy - r * 0.05f, cx + r * 0.52f, cy + r * 0.74f, strokeP)
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

// =========================================================================
// 81. כלי טקסט ועיבוד מחרוזות עם טבעת הרכב ומדי צפיפות חיים (Text Tools - Tab 81)
// =========================================================================
fun MainActivity.createProTextToolsStudioScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply {
        tag = "SKIP_UNIVERSAL_LIVE"
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20f), dp(12f), dp(20f), dp(24f))
    }
    root.addView(
        createProCanvaHeader(
            "🔤 כלי טקסט ועיבוד מחרוזות (Text Tools)",
            "טבעת ניתוח תווים חיה, ספירת מילים ושורות בזמן אמת ו-11 פעולות עיבוד, ניקוי ומיון"
        )
    )

    var totalChars = 0
    var noSpaceChars = 0
    var wordCount = 0
    var lineCount = 0
    var lettersCount = 0
    var digitsCount = 0
    var punctCount = 0

    val textAnalyticsCanvas = object : View(this) {
        private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val pal = getProCanvasColors()

            // טבעת Donut דינמית בצד שמאל של הרכב התווים
            val cx = w * 0.23f; val cy = h * 0.50f
            val r = min(w * 0.16f, h * 0.34f)
            val ringRect = RectF(cx - r, cy - r, cx + r, cy + r)
            strokeP.strokeWidth = 9f * d
            strokeP.color = pal.grid
            strokeP.clearShadowLayer()
            canvas.drawCircle(cx, cy, r, strokeP)

            if (totalChars > 0) {
                val spaces = (totalChars - noSpaceChars).coerceAtLeast(0)
                val segs = listOf(
                    lettersCount to pal.accent1,
                    digitsCount to pal.warning,
                    spaces to pal.accent2,
                    punctCount to pal.accent3
                )
                var startAng = -90f
                for ((cnt, col) in segs) {
                    if (cnt <= 0) continue
                    val sweep = (cnt.toFloat() / totalChars) * 360f
                    strokeP.color = col
                    strokeP.applyAuroraGlow(pal.isAurora, col, d)
                    canvas.drawArc(ringRect, startAng, sweep.coerceAtLeast(2f), false, strokeP)
                    startAng += sweep
                }
            }

            txtP.clearShadowLayer()
            txtP.color = pal.ink
            txtP.textSize = 15f * d
            canvas.drawText(BidiFmt.ltr(wordCount), cx, cy + 2f * d, txtP)
            txtP.color = pal.muted
            txtP.textSize = 9.5f * d
            canvas.drawText("מילים", cx, cy + 14f * d, txtP)

            // 3 מדי פס חיים בצד ימין: אותיות, ספרות, רווחים/פיסוק
            val barL = w * 0.44f; val barR = w - 18f * d; val barW = (barR - barL).coerceAtLeast(10f)
            val denom = totalChars.coerceAtLeast(1).toFloat()
            val rows = listOf(
                Triple("אותיות ($lettersCount)", lettersCount / denom, pal.accent1),
                Triple("ספרות ($digitsCount)", digitsCount / denom, pal.warning),
                Triple("רווחים (${totalChars - noSpaceChars})", (totalChars - noSpaceChars) / denom, pal.accent2)
            )
            txtP.textAlign = Paint.Align.RIGHT
            for ((idx, item) in rows.withIndex()) {
                val ry = h * (0.24f + idx * 0.25f)
                txtP.color = pal.ink; txtP.textSize = 11f * d
                canvas.drawText("\u200F${item.first}", barR, ry - 4f * d, txtP)

                fillP.color = pal.grid
                canvas.drawRoundRect(RectF(barL, ry, barR, ry + 7f * d), 4f * d, 4f * d, fillP)
                fillP.color = item.third
                val fillW = (barW * item.second.coerceIn(0f, 1f))
                if (fillW > 1f) {
                    canvas.drawRoundRect(RectF(barL, ry, barL + fillW, ry + 7f * d), 4f * d, 4f * d, fillP)
                }
            }

            val readSec = ((wordCount / 200.0) * 60.0).roundToInt()
            txtP.textAlign = Paint.Align.CENTER
            txtP.color = pal.muted; txtP.textSize = 11f * d
            CanvasUiHelper.drawFittedText(canvas, "\u200Fקריאה: ~${BidiFmt.ltr("$readSec ש׳")} • ${BidiFmt.ltr("${totalChars * 2} Bytes")}", (barL + barR) / 2f, h - 8f * d, barW, 11.5f * d, 8.5f * d, txtP)
        }
    }.apply { styleProCanvasCard(this, 145f) }

    val textIn = createInputBox("הקלד או הדבק טקסט כאן...").apply {
        tag = "HAS_CUSTOM_LIVE_WATCHER"
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        textDirection = View.TEXT_DIRECTION_RTL
        gravity = Gravity.RIGHT or Gravity.TOP
        minLines = 5
    }

    val liveCounterTv = TextView(this).apply {
        text = "\u200F0 תווים • 0 ללא רווחים • 0 מילים • 0 שורות"
        setTextColor(if (isMinimalistMode) MinimalistTheme.TEXT_MUTED_GREY else Color.LTGRAY)
        textSize = 13.5f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.RIGHT
        setPadding(dp(4f), 0, dp(4f), dp(10f))
    }

    fun updateTextCounters() {
        val s = textIn.text?.toString() ?: ""
        totalChars = s.length
        noSpaceChars = s.count { !it.isWhitespace() }
        wordCount = if (s.isBlank()) 0 else s.trim().split(Regex("\\s+")).size
        lineCount = if (s.isEmpty()) 0 else s.lines().size
        lettersCount = s.count { it.isLetter() || it in 'א'..'ת' }
        digitsCount = s.count { it.isDigit() }
        punctCount = (noSpaceChars - lettersCount - digitsCount).coerceAtLeast(0)

        liveCounterTv.text = "\u200F${BidiFmt.ltr(totalChars)} תווים • ${BidiFmt.ltr(noSpaceChars)} ללא רווחים • ${BidiFmt.ltr(wordCount)} מילים • ${BidiFmt.ltr(lineCount)} שורות"
        textAnalyticsCanvas.invalidate()
    }

    textIn.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { updateTextCounters() }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    val chipTransforms: List<Pair<String, (String) -> String>> = listOf(
        "אותיות גדולות" to { s -> s.uppercase(Locale.getDefault()) },
        "אותיות קטנות" to { s -> s.lowercase(Locale.getDefault()) },
        "כל מילה באות גדולה" to { s ->
            s.split(" ").joinToString(" ") { w ->
                w.lowercase(Locale.getDefault()).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
        },
        "אות גדולה בתחילת משפט" to { s ->
            val sb = StringBuilder(s.lowercase(Locale.getDefault()))
            var capNext = true
            for (i in 0 until sb.length) {
                val c = sb[i]
                if (capNext && c.isLetter()) {
                    sb.setCharAt(i, c.uppercaseChar())
                    capNext = false
                } else if (c == '.' || c == '!' || c == '?' || c == '\n') {
                    capNext = true
                }
            }
            sb.toString()
        },
        "היפוך רישיות" to { s ->
            s.map { c ->
                when {
                    c.isUpperCase() -> c.lowercaseChar()
                    c.isLowerCase() -> c.uppercaseChar()
                    else -> c
                }
            }.joinToString("")
        },
        "קיצוץ רווחים" to { s -> s.lines().joinToString("\n") { line -> line.trim().replace(Regex("[ \\t]+"), " ") }.trim() },
        "הסרת כל הרווחים" to { s -> s.filter { !it.isWhitespace() } },
        "הסרת שורות ריקות" to { s -> s.lines().filter { it.isNotBlank() }.joinToString("\n") },
        "הסרת שורות כפולות" to { s -> s.lines().distinct().joinToString("\n") },
        "מיון שורות" to { s -> s.lines().sorted().joinToString("\n") },
        "היפוך" to { s -> s.reversed() }
    )

    val chipsScroll = HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12f) }
    }
    val chipsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }

    for ((label, transformFn) in chipTransforms) {
        val chip = TextView(this).apply {
            tag = "PRO_STAR_CHIP"
            text = label
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            val chipBg = if (isMinimalistMode) MinimalistTheme.CARD_WHITE else Utils.lightenColor(bgColor, 0.12f)
            setTextColor(MinimalistTheme.ensureReadableTextOnBg(if (isMinimalistMode) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE, chipBg, 2.3))
            gravity = Gravity.CENTER
            setPadding(dp(16f), dp(10f), dp(16f), dp(10f))
            background = GradientDrawable().apply {
                cornerRadius = dp(14f).toFloat()
                setColor(chipBg)
                setStroke(dp(1.2f), if (isMinimalistMode) Color.parseColor("#E0DBD3") else primaryColor)
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(4f), dp(2f), dp(4f), dp(2f)) }
            setOnClickListener {
                feedback(it)
                val current = textIn.text?.toString() ?: ""
                textIn.setText(transformFn(current))
                textIn.setSelection(textIn.text.length)
            }
        }
        chipsRow.addView(chip)
    }
    chipsScroll.addView(chipsRow)

    val bottomActionsRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    }
    val copyBtn = createActionBtn("📋 העתק", true) {
        val txt = textIn.text?.toString() ?: ""
        if (txt.isNotEmpty()) copyToClipboard(txt) else Toast.makeText(this, "אין טקסט להעתקה", Toast.LENGTH_SHORT).show()
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.1f).apply { rightMargin = dp(5f) } }

    val shareBtn = createActionBtn("📤 שיתוף", false) {
        val txt = textIn.text?.toString() ?: ""
        if (txt.isNotEmpty()) {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, txt) }, "שתף טקסט באמצעות"))
        }
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(4f); rightMargin = dp(4f) } }

    val clearBtn = createActionBtn("🧹 נקה", false) { textIn.setText("") }.apply {
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f).apply { leftMargin = dp(5f) }
    }
    bottomActionsRow.addView(copyBtn); bottomActionsRow.addView(shareBtn); bottomActionsRow.addView(clearBtn)

    updateTextCounters()
    root.addView(textAnalyticsCanvas); root.addView(textIn); root.addView(liveCounterTv); root.addView(chipsScroll); root.addView(bottomActionsRow)
    scroll.addView(root); return scroll
}

// =========================================================================
// 82. ספרות רומיות עם אבני פירוק צבעוניות דינמיות (Roman Numerals - Tab 82)
// =========================================================================
fun MainActivity.createProRomanNumeralsScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🏛️ ספרות רומיות (Roman Numerals)", "המרה דו-כיוונית חיה עם לוח אבני פירוק צבעוניות המשתנה לפי אלפים, מאות, עשרות ואחדות"))

    var curDecimal = 2026
    var curRoman = "MMXXVI"
    var curBlocks: List<Pair<String, Int>> = listOf("MM" to 2000, "XX" to 20, "V" to 5, "I" to 1)

    val romanCanvas = object : View(this) {
        private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val pal = getProCanvasColors()

            // כותרת חקוקה בראש לוח האבן
            txtP.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            txtP.color = pal.ink
            CanvasUiHelper.drawFittedText(canvas, BidiFmt.ltr(curRoman), w / 2f, h * 0.30f, w - 36f * d, 28f * d, 14f * d, txtP)

            // שורת אבני הפירוק הפרופורציונליות החיות
            val blocks = curBlocks.ifEmpty { listOf("?" to 1) }
            val padX = 16f * d
            val gap = 6f * d
            val totalW = (w - 2 * padX - gap * (blocks.size - 1)).coerceAtLeast(20f)
            val blockW = totalW / blocks.size
            val topY = h * 0.44f; val botY = h * 0.80f

            for ((i, pair) in blocks.withIndex()) {
                val bx = padX + i * (blockW + gap)
                val rect = RectF(bx, topY, bx + blockW, botY)
                val col = when {
                    pair.second >= 1000 -> pal.warning
                    pair.second >= 100 -> pal.accent2
                    pair.second >= 10 -> pal.accent1
                    else -> pal.accent3
                }
                fillP.color = if (pal.isMin) col else Color.argb(65, Color.red(col), Color.green(col), Color.blue(col))
                canvas.drawRoundRect(rect, 12f * d, 12f * d, fillP)

                strokeP.strokeWidth = 1.6f * d
                strokeP.color = col
                strokeP.applyAuroraGlow(pal.isAurora, col, d)
                canvas.drawRoundRect(rect, 12f * d, 12f * d, strokeP)

                val textCol = if (pal.isMin) MinimalistTheme.ensureReadableTextOnBg(pal.ink, col, 2.3) else Color.WHITE
                txtP.color = textCol
                txtP.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                CanvasUiHelper.drawFittedText(canvas, pair.first, rect.centerX(), topY + (botY - topY) * 0.48f, blockW - 6f * d, 14.5f * d, 9.5f * d, txtP)

                txtP.typeface = Typeface.DEFAULT_BOLD
                CanvasUiHelper.drawFittedText(canvas, BidiFmt.ltr("+${pair.second}"), rect.centerX(), botY - 8f * d, blockW - 6f * d, 10.5f * d, 8f * d, txtP)
            }

            txtP.color = pal.muted; txtP.typeface = Typeface.DEFAULT_BOLD; txtP.textSize = 11.5f * d
            canvas.drawText(BidiFmt.ltr("= $curDecimal"), w / 2f, h - 8f * d, txtP)
        }
    }.apply { styleProCanvasCard(this, 175f) }

    val decIn = createInputBox("מספר עשרוני (1 עד 3999 - למשל 2026)").apply { setText("2026") }
    val romIn = createInputBox("או הקלד ספרות רומיות (למשל MMXXVI או MCMXLVIII)").apply {
        tag = "HAS_CUSTOM_LIVE_WATCHER"
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
    }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    val romanTable = listOf(
        1000 to "M", 900 to "CM", 500 to "D", 400 to "CD",
        100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
        10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I"
    )

    fun toRomanWithBlocks(num: Int): Pair<String, List<Pair<String, Int>>> {
        if (num !in 1..3999) return "מחוץ לטווח" to emptyList()
        var rem = num
        val sb = StringBuilder()
        val blocks = mutableListOf<Pair<String, Int>>()
        for ((v, sym) in romanTable) {
            var count = 0
            while (rem >= v) {
                sb.append(sym); rem -= v; count++
            }
            if (count > 0) blocks.add(sym.repeat(count) to (count * v))
        }
        return sb.toString() to blocks
    }

    fun fromRomanString(s: String): Int {
        val clean = s.trim().uppercase(Locale.US)
        if (clean.isEmpty()) return -1
        val map = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)
        var sum = 0
        for (i in clean.indices) {
            val cVal = map[clean[i]] ?: return -1
            val nVal = if (i + 1 < clean.length) (map[clean[i + 1]] ?: 0) else 0
            if (cVal < nVal) sum -= cVal else sum += cVal
        }
        return sum
    }

    fun updateFromDecimal() {
        curDecimal = BidiFmt.evalInputExpr(decIn, 2026.0).roundToInt().coerceIn(1, 3999)
        val (rom, blocks) = toRomanWithBlocks(curDecimal)
        curRoman = rom; curBlocks = blocks
        romanCanvas.invalidate()
        resTv.text = BidiFmt.rtlParagraph(
            "תוצאת המרת ספרות רומיות:",
            "• מספר עשרוני: ${BidiFmt.ltr(curDecimal)}  ➔  ספרות רומיות: ${BidiFmt.ltr(curRoman)}",
            "• פירוק אבני בניין: ${BidiFmt.ltr(blocks.joinToString(" + ") { "${it.first}(${it.second})" })}",
            "• מפתח: ${BidiFmt.ltr("I=1, V=5, X=10, L=50, C=100, D=500, M=1000")}"
        )
    }

    BidiFmt.bindLiveInputs(decIn) { updateFromDecimal() }
    romIn.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            if (!romIn.hasFocus()) return
            val parsed = fromRomanString(s?.toString() ?: "")
            if (parsed in 1..3999) {
                curDecimal = parsed
                val (rom, blocks) = toRomanWithBlocks(parsed)
                curRoman = rom; curBlocks = blocks
                romanCanvas.invalidate()
                resTv.text = BidiFmt.rtlParagraph(
                    "פענוח ספרות רומיות:",
                    "• ספרות רומיות: ${BidiFmt.ltr(s.toString().uppercase(Locale.US))}  ➔  ערך עשרוני: ${BidiFmt.ltr(parsed)}",
                    "• פירוק תקני: ${BidiFmt.ltr(blocks.joinToString(" + ") { "${it.first}(${it.second})" })}"
                )
            }
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    val calcBtn = createActionBtn("המר ופענח ספרות רומיות 🏛️", true) { updateFromDecimal() }
    updateFromDecimal()

    root.addView(romanCanvas); root.addView(decIn); root.addView(romIn); root.addView(calcBtn)
    root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 83. סדרת מספרים עם גרף דו-כיווני (חיובי/שלילי) ועקומת מגמה חיה (Tab 83)
// =========================================================================
fun MainActivity.createProNumberSeriesScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🔢 סדרת מספרים (Arithmetic, Geometric & Fibonacci)", "גרף עמודות ועקומת מגמה חי התומך גם בסדרות עולות, יורדות ושליליות"))

    val seriesModes = listOf("סדרה חשבונית (הפרש קבוע d)", "סדרה הנדסית (מנה קבועה q)", "סדרת פיבונאצ׳י (Fn = Fn-1 + Fn-2)")
    var modeIdx = 0
    var previewTerms = listOf(2.0, 5.0, 8.0, 11.0, 14.0, 17.0, 20.0, 23.0)

    val seriesCanvas = object : View(this) {
        private val barP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
        private val axisP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val pal = getProCanvasColors()

            val terms = previewTerms.ifEmpty { listOf(1.0) }
            val minV = min(0.0, terms.minOrNull() ?: 0.0)
            val maxV = max(0.0, terms.maxOrNull() ?: 1.0)
            val span = (maxV - minV).coerceAtLeast(1e-6)

            val padL = 18f * d; val padR = w - 18f * d
            val topY = 24f * d; val botY = h - 26f * d
            val plotH = (botY - topY).coerceAtLeast(10f)
            val zeroY = (topY + (maxV / span) * plotH).toFloat()

            axisP.strokeWidth = 1.3f * d; axisP.color = pal.grid
            canvas.drawLine(padL, zeroY, padR, zeroY, axisP)

            val slotW = (padR - padL) / terms.size.coerceAtLeast(1)
            val curvePath = Path()

            for ((i, v) in terms.withIndex()) {
                val cx = padL + i * slotW + slotW / 2f
                val valY = (topY + ((maxV - v) / span) * plotH).toFloat()
                val bw = slotW * 0.56f

                barP.color = if (v >= 0) {
                    Color.argb(if (pal.isMin) 210 else 145, Color.red(pal.accent1), Color.green(pal.accent1), Color.blue(pal.accent1))
                } else {
                    Color.argb(if (pal.isMin) 210 else 145, Color.red(pal.negative), Color.green(pal.negative), Color.blue(pal.negative))
                }
                val rTop = min(zeroY, valY); val rBot = max(zeroY, valY)
                canvas.drawRoundRect(RectF(cx - bw / 2f, rTop, cx + bw / 2f, rBot.coerceAtLeast(rTop + 3f * d)), 5f * d, 5f * d, barP)

                if (i == 0) curvePath.moveTo(cx, valY) else curvePath.lineTo(cx, valY)

                txtP.color = pal.ink; txtP.textSize = 9.5f * d
                val lblY = if (v >= 0) (valY - 5f * d).coerceAtLeast(12f * d) else (valY + 12f * d).coerceAtMost(h - 16f * d)
                canvas.drawText(BidiFmt.ltr(Utils.formatResult(v, 1)), cx, lblY, txtP)

                txtP.color = pal.muted; txtP.textSize = 9.5f * d
                canvas.drawText(BidiFmt.ltr("a${i + 1}"), cx, h - 8f * d, txtP)
            }

            lineP.strokeWidth = 2.4f * d; lineP.color = pal.accent2
            lineP.applyAuroraGlow(pal.isAurora, pal.accent2, d)
            canvas.drawPath(curvePath, lineP)

            barP.color = pal.ink
            for ((i, v) in terms.withIndex()) {
                val cx = padL + i * slotW + slotW / 2f
                val valY = (topY + ((maxV - v) / span) * plotH).toFloat()
                canvas.drawCircle(cx, valY, 3.2f * d, barP)
            }
        }
    }.apply { styleProCanvasCard(this, 195f) }

    val modeBtn = createActionBtn("סוג סדרה: ${seriesModes[0]}", true) {}
    val a1In = createInputBox("איבר ראשון (a₁ - למשל 2)").apply { setText("2") }
    val diffIn = createInputBox("הפרש d / מנה q (למשל 3 או -2)").apply { setText("3") }
    val nIn = createInputBox("מספר האיברים n (למשל 10)").apply { setText("10") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun updateSeries() {
        val a1 = BidiFmt.evalInputExpr(a1In, 2.0)
        val dq = BidiFmt.evalInputExpr(diffIn, 3.0)
        val n = BidiFmt.evalInputExpr(nIn, 10.0).roundToInt().coerceIn(1, 200)
        diffIn.visibility = if (modeIdx == 2) View.GONE else View.VISIBLE

        val countPreview = min(n, 9)
        when (modeIdx) {
            0 -> {
                previewTerms = (1..countPreview).map { i -> a1 + (i - 1) * dq }
                val an = a1 + (n - 1) * dq
                val sn = (n / 2.0) * (a1 + an)
                resTv.text = BidiFmt.rtlParagraph(
                    "תוצאות סדרה חשבונית (${BidiFmt.ltr("a₁=${Utils.formatResult(a1, 2)}, d=${Utils.formatResult(dq, 2)}")}):",
                    "• האיבר ה-${BidiFmt.ltr(n)} (${BidiFmt.ltr("aₙ")}): ${BidiFmt.ltr(Utils.formatResult(an, calcPrecision))}",
                    "• סכום ${BidiFmt.ltr(n)} האיברים הראשונים (${BidiFmt.ltr("Sₙ")}): ${BidiFmt.ltr(Utils.formatResult(sn, calcPrecision))}",
                    "• ממוצע איברי הסדרה: ${BidiFmt.ltr(Utils.formatResult(sn / n, 3))}",
                    "• תחילת הסדרה: ${BidiFmt.ltr(previewTerms.joinToString(", ") { Utils.formatResult(it, 2) })}"
                )
            }
            1 -> {
                previewTerms = (1..countPreview).map { i -> a1 * dq.pow((i - 1).toDouble()) }
                val an = a1 * dq.pow((n - 1).toDouble())
                val sn = if (abs(dq - 1.0) < 1e-9) a1 * n else a1 * (dq.pow(n.toDouble()) - 1.0) / (dq - 1.0)
                val infSum = if (abs(dq) < 1.0) "• סכום סדרה אינסופית מתכנסת (${BidiFmt.ltr("S∞")}): ${BidiFmt.ltr(Utils.formatResult(a1 / (1.0 - dq), calcPrecision))}" else "• התכנסות: הסדרה אינה מתכנסת (${BidiFmt.ltr("|q| ≥ 1")})"
                resTv.text = BidiFmt.rtlParagraph(
                    "תוצאות סדרה הנדסית (${BidiFmt.ltr("a₁=${Utils.formatResult(a1, 2)}, q=${Utils.formatResult(dq, 2)}")}):",
                    "• האיבר ה-${BidiFmt.ltr(n)} (${BidiFmt.ltr("aₙ")}): ${BidiFmt.ltr(Utils.formatResult(an, calcPrecision))}",
                    "• סכום ${BidiFmt.ltr(n)} האיברים הראשונים (${BidiFmt.ltr("Sₙ")}): ${BidiFmt.ltr(Utils.formatResult(sn, calcPrecision))}",
                    infSum
                )
            }
            else -> {
                val fib = mutableListOf<Double>()
                for (i in 0 until n.coerceAtMost(80)) {
                    when (i) {
                        0 -> fib.add(a1)
                        1 -> fib.add(if (a1 == 0.0) 1.0 else a1)
                        else -> fib.add(fib[i - 1] + fib[i - 2])
                    }
                }
                previewTerms = fib.take(9)
                resTv.text = BidiFmt.rtlParagraph(
                    "תוצאות סדרת פיבונאצ׳י:",
                    "• האיבר ה-${BidiFmt.ltr(fib.size)}: ${BidiFmt.ltr(Utils.formatResult(fib.lastOrNull() ?: a1, 0))}",
                    "• סכום ${BidiFmt.ltr(fib.size)} האיברים: ${BidiFmt.ltr(Utils.formatResult(fib.sum(), 0))}",
                    "• יחס הזהב בין שני האחרונים (φ): ${BidiFmt.ltr(if (fib.size >= 2 && fib[fib.size - 2] != 0.0) Utils.formatResult(fib.last() / fib[fib.size - 2], 6) else "1.618034")}"
                )
            }
        }
        seriesCanvas.invalidate()
    }

    modeBtn.setOnClickListener {
        showItemPicker("בחר סוג סדרת מספרים", seriesModes) { chosen ->
            modeIdx = seriesModes.indexOf(chosen).coerceAtLeast(0)
            modeBtn.text = "סוג סדרה: $chosen"
            updateSeries()
        }
    }

    BidiFmt.bindLiveInputs(a1In, diffIn, nIn) { updateSeries() }
    val calcBtn = createActionBtn("חשב סדרת מספרים 🔢", false) { updateSeries() }
    updateSeries()

    root.addView(seriesCanvas); root.addView(modeBtn); root.addView(a1In); root.addView(diffIn); root.addView(nIn)
    root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 84. פרופורציות ויחס עם תרשים עוגה חי ומאזני פרופורציה כפולים (Tab 84)
// =========================================================================
fun MainActivity.createProProportionsAndRatioScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("⚖️ פרופורציות ויחס (Proportions & Ratio)", "תרשים עוגה חי ופסי השוואת פרופורציה A : B = C : X המשתנים בזמן אמת"))

    var valA = 4.0; var valB = 6.0; var valC = 10.0; var valX = 15.0

    val propCanvas = object : View(this) {
        private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val pal = getProCanvasColors()

            val sumAB = (abs(valA) + abs(valB)).coerceAtLeast(1e-6)
            val ratioA = (abs(valA) / sumAB).toFloat().coerceIn(0.05f, 0.95f)

            // צד שמאל: תרשים עוגה חי ליחס A:B
            val pieCx = w * 0.21f; val pieCy = h * 0.44f
            val pieR = min(w * 0.14f, h * 0.30f)
            val pieRect = RectF(pieCx - pieR, pieCy - pieR, pieCx + pieR, pieCy + pieR)

            fillP.color = pal.accent1
            canvas.drawArc(pieRect, -90f, ratioA * 360f, true, fillP)
            fillP.color = pal.accent2
            canvas.drawArc(pieRect, -90f + ratioA * 360f, (1f - ratioA) * 360f, true, fillP)
            strokeP.strokeWidth = 1.6f * d; strokeP.color = pal.ink
            canvas.drawCircle(pieCx, pieCy, pieR, strokeP)

            // צד ימין: שני פסי פרופורציה מקבילים A:B ו-C:X
            val barL = w * 0.41f; val barR = w - 18f * d; val barW = (barR - barL).coerceAtLeast(20f)
            val splitX = barL + barW * ratioA

            // פס 1: A : B
            fillP.color = pal.accent1
            canvas.drawRoundRect(RectF(barL, 20f * d, splitX - 2f * d, 46f * d), 8f * d, 8f * d, fillP)
            fillP.color = pal.accent2
            canvas.drawRoundRect(RectF(splitX + 2f * d, 20f * d, barR, 46f * d), 8f * d, 8f * d, fillP)

            // פס 2: C : X (באותו יחס בדיוק!)
            fillP.color = Color.argb(190, Color.red(pal.accent1), Color.green(pal.accent1), Color.blue(pal.accent1))
            canvas.drawRoundRect(RectF(barL, 58f * d, splitX - 2f * d, 84f * d), 8f * d, 8f * d, fillP)
            fillP.color = Color.argb(190, Color.red(pal.accent2), Color.green(pal.accent2), Color.blue(pal.accent2))
            canvas.drawRoundRect(RectF(splitX + 2f * d, 58f * d, barR, 84f * d), 8f * d, 8f * d, fillP)

            val txtOnA = MinimalistTheme.ensureReadableTextOnBg(Color.WHITE, pal.accent1, 2.3)
            val txtOnB = MinimalistTheme.ensureReadableTextOnBg(Color.WHITE, pal.accent2, 2.3)
            txtP.textSize = 11f * d
            txtP.color = txtOnA
            canvas.drawText(BidiFmt.ltr("A=${Utils.formatResult(valA, 1)}"), (barL + splitX) / 2f, 37f * d, txtP)
            canvas.drawText(BidiFmt.ltr("C=${Utils.formatResult(valC, 1)}"), (barL + splitX) / 2f, 75f * d, txtP)
            txtP.color = txtOnB
            canvas.drawText(BidiFmt.ltr("B=${Utils.formatResult(valB, 1)}"), (splitX + barR) / 2f, 37f * d, txtP)
            canvas.drawText(BidiFmt.ltr("X=${Utils.formatResult(valX, 1)}"), (splitX + barR) / 2f, 75f * d, txtP)

            txtP.color = pal.ink
            val eqLine = "${Utils.formatResult(valA, 2)} : ${Utils.formatResult(valB, 2)}  =  ${Utils.formatResult(valC, 2)} : ${Utils.formatResult(valX, 2)}   (${(ratioA * 100).roundToInt()}% / ${((1f - ratioA) * 100).roundToInt()}%)"
            CanvasUiHelper.drawFittedText(canvas, BidiFmt.ltr(eqLine), w / 2f, h - 10f * d, w - 24f * d, 13.5f * d, 9f * d, txtP)
        }
    }.apply { styleProCanvasCard(this, 145f) }

    val aIn = createInputBox("ערך A (למשל 4)").apply { setText("4") }
    val bIn = createInputBox("ערך B (למשל 6)").apply { setText("6") }
    val cIn = createInputBox("ערך C למציאת X בפרופורציה A:B = C:X (למשל 10)").apply { setText("10") }
    val amountIn = createInputBox("סכום כולל לחלוקה לפי היחס A:B (למשל 1000)").apply { setText("1000") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun updateProportions() {
        valA = BidiFmt.evalInputExpr(aIn, 4.0)
        valB = BidiFmt.evalInputExpr(bIn, 6.0)
        valC = BidiFmt.evalInputExpr(cIn, 10.0)
        val totalAmount = BidiFmt.evalInputExpr(amountIn, 1000.0)

        valX = if (abs(valA) > 1e-12) (valB * valC) / valA else 0.0
        propCanvas.invalidate()

        val g = Utils.gcd(valA.roundToLong().coerceAtLeast(1L), valB.roundToLong().coerceAtLeast(1L)).coerceAtLeast(1L)
        val simpA = valA.roundToLong() / g; val simpB = valB.roundToLong() / g
        val sumParts = (valA + valB).let { if (abs(it) < 1e-12) 1.0 else it }
        val shareA = totalAmount * (valA / sumParts); val shareB = totalAmount * (valB / sumParts)

        resTv.text = BidiFmt.rtlParagraph(
            "פתרון פרופורציה וניתוח יחס:",
            "• הנעלם בפרופורציה (${BidiFmt.ltr("X = B × C / A")}): ${BidiFmt.ltr("X = ${Utils.formatResult(valX, calcPrecision)}")}",
            "• יחס מצומצם של ${BidiFmt.ltr("A : B")}: ${BidiFmt.ltr("$simpA : $simpB")} (מנה עשרונית: ${BidiFmt.ltr(Utils.formatResult(if (abs(valB) > 1e-12) valA / valB else 0.0, 4))})",
            "• חלוקת ${BidiFmt.ltr(Utils.formatResult(totalAmount, 2))} לפי היחס ${BidiFmt.ltr("A:B")}:",
            "  חלק A (${BidiFmt.ltr("${Utils.formatResult((valA / sumParts) * 100, 1)}%")}): ${BidiFmt.ltr(Utils.formatResult(shareA, 2))}  |  חלק B (${BidiFmt.ltr("${Utils.formatResult((valB / sumParts) * 100, 1)}%")}): ${BidiFmt.ltr(Utils.formatResult(shareB, 2))}"
        )
    }

    BidiFmt.bindLiveInputs(aIn, bIn, cIn, amountIn) { updateProportions() }
    val calcBtn = createActionBtn("פתור פרופורציה ויחס ⚖️", true) { updateProportions() }
    updateProportions()

    root.addView(propCanvas); root.addView(aIn); root.addView(bIn); root.addView(cIn); root.addView(amountIn)
    root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 85. משוואה לינארית עם מערכת צירים מדויקת, חיתוכים וזווית שיפוע חיה (Tab 85)
// =========================================================================
fun MainActivity.createProLinearEquationScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("📈 משוואה לינארית וגרף ישר (aX + b = c)", "מערכת צירים מתמטית מדויקת המתכווננת אוטומטית לפי השיפוע ונקודות החיתוך עם הצירים"))

    var curA = 2.0; var curB = -4.0; var curC = 6.0; var rootX = 5.0; var yIntercept = -10.0

    val linearCanvas = object : View(this) {
        private val gridP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val axisP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val dotP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val pal = getProCanvasColors()

            val padL = 22f * d; val padR = w - 22f * d
            val padT = 20f * d; val padB = h - 30f * d
            val plotW = (padR - padL).coerceAtLeast(10f)
            val plotH = (padB - padT).coerceAtLeast(10f)

            val maxRange = maxOf(abs(rootX) * 1.45, abs(yIntercept) * 1.35, 6.0).coerceAtMost(500.0)
            fun toPxX(x: Double): Float = (padL + ((x + maxRange) / (2.0 * maxRange)) * plotW).toFloat()
            fun toPxY(y: Double): Float = (padB - ((y + maxRange) / (2.0 * maxRange)) * plotH).toFloat()

            // רשת קרטזית
            gridP.strokeWidth = 0.8f * d; gridP.color = pal.grid
            for (k in -2..2) {
                if (k == 0) continue
                val v = k * (maxRange / 2.0)
                canvas.drawLine(toPxX(v), padT, toPxX(v), padB, gridP)
                canvas.drawLine(padL, toPxY(v), padR, toPxY(v), gridP)
            }

            // צירי X ו-Y ראשיים
            val ox = toPxX(0.0); val oy = toPxY(0.0)
            axisP.strokeWidth = 1.6f * d; axisP.color = pal.muted
            canvas.drawLine(padL, oy, padR, oy, axisP)
            canvas.drawLine(ox, padT, ox, padB, axisP)

            // שרטוט הישר המדויק y = a*x + (b - c) חתוך לגבולות הגרף
            canvas.save()
            canvas.clipRect(padL, padT, padR, padB)
            val xLeft = -maxRange; val yLeft = curA * xLeft + yIntercept
            val xRight = maxRange; val yRight = curA * xRight + yIntercept
            lineP.strokeWidth = 3f * d; lineP.color = pal.accent2
            lineP.applyAuroraGlow(pal.isAurora, pal.accent2, d)
            canvas.drawLine(toPxX(xLeft), toPxY(yLeft), toPxX(xRight), toPxY(yRight), lineP)
            canvas.restore()

            // נקודת חיתוך עם ציר X (הפתרון)
            if (abs(curA) > 1e-12) {
                val rPx = toPxX(rootX).coerceIn(padL, padR)
                dotP.color = pal.positive
                canvas.drawCircle(rPx, oy, 6f * d, dotP)
                txtP.color = pal.positive; txtP.textSize = 10.5f * d
                canvas.drawText(BidiFmt.ltr("X=${Utils.formatResult(rootX, 2)}"), rPx, (oy - 10f * d).coerceAtLeast(padT + 10f * d), txtP)
            }

            // נקודת חיתוך עם ציר Y
            val yIntPx = toPxY(yIntercept).coerceIn(padT, padB)
            dotP.color = pal.accent3
            canvas.drawCircle(ox, yIntPx, 5.5f * d, dotP)

            txtP.color = pal.ink
            val eqStr = "${Utils.formatResult(curA, 2)}x + (${Utils.formatResult(curB, 2)}) = ${Utils.formatResult(curC, 2)}   ➔   x = ${Utils.formatResult(rootX, 3)}"
            CanvasUiHelper.drawFittedText(canvas, BidiFmt.ltr(eqStr), w / 2f, h - 8f * d, w - 24f * d, 13.5f * d, 9.5f * d, txtP)
        }
    }.apply { styleProCanvasCard(this, 210f) }

    val aIn = createInputBox("מקדם הנעלם a (למשל 2)").apply { setText("2") }
    val bIn = createInputBox("איבר חופשי באגף שמאל b (למשל -4)").apply { setText("-4") }
    val cIn = createInputBox("תוצאה באגף ימין c (למשל 6)").apply { setText("6") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun solveLinear() {
        curA = BidiFmt.evalInputExpr(aIn, 2.0)
        curB = BidiFmt.evalInputExpr(bIn, -4.0)
        curC = BidiFmt.evalInputExpr(cIn, 6.0)
        yIntercept = curB - curC

        if (abs(curA) < 1e-12) {
            rootX = 0.0
            linearCanvas.invalidate()
            resTv.text = if (abs(curB - curC) < 1e-9) {
                BidiFmt.rtlParagraph("פתרון המשוואה:", "• אינסוף פתרונות (זהות מתמטית לכל X)")
            } else {
                BidiFmt.rtlParagraph("פתרון המשוואה:", "• אין פתרון (סתירה מתמטית ∅)")
            }
            return
        }

        val rhsDiff = curC - curB
        rootX = rhsDiff / curA
        val slopeAngle = Math.toDegrees(atan(curA))
        linearCanvas.invalidate()

        resTv.text = BidiFmt.rtlParagraph(
            "שלבי פתרון המשוואה הלינארית:",
            "• משוואה נתונה: ${BidiFmt.ltr("${Utils.formatResult(curA, 2)}x + (${Utils.formatResult(curB, 2)}) = ${Utils.formatResult(curC, 2)}")}",
            "• העברת אגפים: ${BidiFmt.ltr("${Utils.formatResult(curA, 2)}x = ${Utils.formatResult(rhsDiff, 2)}")}",
            "• פתרון סופי: ${BidiFmt.ltr("x = ${Utils.formatResult(rootX, calcPrecision)}")}",
            "• שיפוע (m): ${BidiFmt.ltr(Utils.formatResult(curA, 2))} (${BidiFmt.ltr("${Utils.formatResult(slopeAngle, 1)}°")}) | חיתוך Y: ${BidiFmt.ltr("(0, ${Utils.formatResult(yIntercept, 2)})")}"
        )
    }

    BidiFmt.bindLiveInputs(aIn, bIn, cIn) { solveLinear() }
    val calcBtn = createActionBtn("פתור משוואה לינארית 📈", true) { solveLinear() }
    solveLinear()

    root.addView(linearCanvas); root.addView(aIn); root.addView(bIn); root.addView(cIn)
    root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 86. יעד חיסכון וזמן הכפלה עם גרף צמיחה דו-שכבתי וסמן הכפלה חי (Tab 86)
// =========================================================================
fun MainActivity.createProSavingsGoalAndDoublingScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🎯 יעד חיסכון וזמן הכפלה (Savings Goal & Rule of 72)", "עקומת צבירה חיה של קרן מול ריבית עד השגת היעד + סמן זמן הכפלת הכסף"))

    var curInitial = 10000.0; var curTarget = 100000.0; var curYears = 3.0; var curRatePct = 6.0
    var curMonthlyPmt = 2150.0; var curDoubleYears = 11.9

    val goalCanvas = object : View(this) {
        private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val dashP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f) }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val pal = getProCanvasColors()

            val padL = 22f * d; val padR = w - 22f * d
            val padT = 26f * d; val padB = h - 30f * d
            val plotW = (padR - padL).coerceAtLeast(10f)
            val plotH = (padB - padT).coerceAtLeast(10f)

            val steps = 24
            val rM = (curRatePct / 100.0) / 12.0
            val totalM = (curYears * 12.0).coerceAtLeast(1.0)
            val maxY = maxOf(curTarget, curInitial, 1.0)

            val totalPath = Path()
            val totalArea = Path()
            val contribPath = Path()

            for (s in 0..steps) {
                val frac = s.toDouble() / steps
                val m = frac * totalM
                val fvInit = curInitial * (1.0 + rM).pow(m)
                val fvPmt = if (rM > 1e-9) curMonthlyPmt * (((1.0 + rM).pow(m) - 1.0) / rM) else curMonthlyPmt * m
                val bal = fvInit + fvPmt
                val contrib = curInitial + curMonthlyPmt * m

                val px = (padL + frac * plotW).toFloat()
                val pyBal = (padB - (bal / maxY).coerceIn(0.0, 1.0) * plotH).toFloat()
                val pyCon = (padB - (contrib / maxY).coerceIn(0.0, 1.0) * plotH).toFloat()

                if (s == 0) {
                    totalPath.moveTo(px, pyBal)
                    totalArea.moveTo(px, padB); totalArea.lineTo(px, pyBal)
                    contribPath.moveTo(px, pyCon)
                } else {
                    totalPath.lineTo(px, pyBal)
                    totalArea.lineTo(px, pyBal)
                    contribPath.lineTo(px, pyCon)
                }
            }
            totalArea.lineTo(padR, padB); totalArea.close()

            fillP.color = Color.argb(55, Color.red(pal.positive), Color.green(pal.positive), Color.blue(pal.positive))
            canvas.drawPath(totalArea, fillP)

            dashP.strokeWidth = 1.6f * d; dashP.color = pal.accent2
            canvas.drawPath(contribPath, dashP)

            lineP.strokeWidth = 2.8f * d; lineP.color = pal.positive
            lineP.applyAuroraGlow(pal.isAurora, pal.positive, d)
            canvas.drawPath(totalPath, lineP)

            // קו יעד עליון
            dashP.color = pal.grid; dashP.strokeWidth = 1.2f * d
            canvas.drawLine(padL, padT, padR, padT, dashP)

            txtP.color = pal.ink
            val topHeader = "\u200Fהפקדה נדרשת: ${BidiFmt.ltr("₪${Utils.formatResult(curMonthlyPmt, 0)} / חודש")}  |  הכפלה: ${BidiFmt.ltr("${Utils.formatResult(curDoubleYears, 1)} שנים")}"
            CanvasUiHelper.drawFittedText(canvas, topHeader, w / 2f, 18f * d, w - 24f * d, 13f * d, 9f * d, txtP)

            txtP.color = pal.muted; txtP.textSize = 11f * d
            canvas.drawText("\u200Fירוק = יתרה כוללת עם ריבית  •  מקווקו = קרן מהכיס", w / 2f, h - 8f * d, txtP)
        }
    }.apply { styleProCanvasCard(this, 185f) }

    val targetIn = createInputBox("יעד חיסכון מבוקש בש״ח (למשל 100000)").apply { setText("100000") }
    val initIn = createInputBox("סכום התחלתי קיים בש״ח (למשל 10000)").apply { setText("10000") }
    val yearsIn = createInputBox("טווח שנים להשגת היעד (למשל 3)").apply { setText("3") }
    val rateIn = createInputBox("תשואה שנתית צפויה ב-% (למשל 6)").apply { setText("6") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun calcSavingsGoal() {
        curTarget = BidiFmt.evalInputExpr(targetIn, 100000.0).coerceAtLeast(1.0)
        curInitial = BidiFmt.evalInputExpr(initIn, 10000.0).coerceAtLeast(0.0)
        curYears = BidiFmt.evalInputExpr(yearsIn, 3.0).coerceAtLeast(0.1)
        curRatePct = BidiFmt.evalInputExpr(rateIn, 6.0).coerceAtLeast(0.0)

        val months = (curYears * 12.0).coerceAtLeast(1.0)
        val rMonth = (curRatePct / 100.0) / 12.0
        val fvInitial = curInitial * (1.0 + rMonth).pow(months)
        val remGoal = (curTarget - fvInitial).coerceAtLeast(0.0)

        curMonthlyPmt = if (rMonth > 1e-9) remGoal * rMonth / ((1.0 + rMonth).pow(months) - 1.0) else remGoal / months
        curDoubleYears = if (curRatePct > 0.01) ln(2.0) / ln(1.0 + curRatePct / 100.0) else 0.0
        val rule72Years = if (curRatePct > 0.01) 72.0 / curRatePct else 0.0
        val totalContributions = curInitial + curMonthlyPmt * months
        val totalInterestEarned = (curTarget - totalContributions).coerceAtLeast(0.0)
        goalCanvas.invalidate()

        resTv.text = BidiFmt.rtlParagraph(
            "תוכנית הגעה ליעד חיסכון (${BidiFmt.ltr("₪${Utils.formatResult(curTarget, 0)}")}):",
            "• הפקדה חודשית נדרשת: ${BidiFmt.ltr("₪${Utils.formatResult(curMonthlyPmt, 2)}")} (למשך ${BidiFmt.ltr("${months.roundToInt()} חודשים")})",
            "• סך קרן שתופקד: ${BidiFmt.ltr("₪${Utils.formatResult(totalContributions, 0)}")} | רווח ריבית: ${BidiFmt.ltr("₪${Utils.formatResult(totalInterestEarned, 0)}")}",
            "",
            "⏳ זמן הכפלת הכסף בריבית ${BidiFmt.ltr("${Utils.formatResult(curRatePct, 2)}%")} לשנה:",
            "• זמן הכפלה מדויק: ${BidiFmt.ltr("${Utils.formatResult(curDoubleYears, 2)} שנים")} | לפי חוק ה-72: ${BidiFmt.ltr("${Utils.formatResult(rule72Years, 2)} שנים")}"
        )
    }

    BidiFmt.bindLiveInputs(targetIn, initIn, yearsIn, rateIn) { calcSavingsGoal() }
    val calcBtn = createActionBtn("חשב יעד חיסכון וזמן הכפלה 🎯", true) { calcSavingsGoal() }
    calcSavingsGoal()

    root.addView(goalCanvas); root.addView(targetIn); root.addView(initIn); root.addView(yearsIn); root.addView(rateIn)
    root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 87. ממיר שכר עם גרף לוגריתמי חי ומד עומס שעות שבועי (Tab 87)
// =========================================================================
fun MainActivity.createProSalaryPeriodConverterScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("💼 ממיר שכר (Salary Period Converter)", "גרף עמודות דינמי בסולם יחסי המשתנה בזמן אמת לפי גובה השכר והיקף שעות העבודה"))

    val periods = listOf("שכר חודשי", "שכר שעתי", "שכר יומי", "שכר שבועי", "שכר שנתי")
    var periodIdx = 0
    var hourlyVal = 65.0; var dailyVal = 546.0; var weeklyVal = 2730.0; var monthlyVal = 12000.0; var yearlyVal = 144000.0
    var curWeeklyHours = 42.0

    val salaryCanvas = object : View(this) {
        private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val pal = getProCanvasColors()

            // מד עומס משרה עגול בצד שמאל
            val gCx = w * 0.16f; val gCy = h * 0.48f; val gR = min(w * 0.11f, h * 0.28f)
            strokeP.strokeWidth = 6f * d; strokeP.color = pal.grid; strokeP.clearShadowLayer()
            canvas.drawCircle(gCx, gCy, gR, strokeP)
            val jobLoad = (curWeeklyHours / 42.0).toFloat().coerceIn(0.05f, 1.5f)
            strokeP.color = if (jobLoad > 1.05f) pal.warning else pal.accent1
            strokeP.applyAuroraGlow(pal.isAurora, strokeP.color, d)
            canvas.drawArc(RectF(gCx - gR, gCy - gR, gCx + gR, gCy + gR), -90f, (jobLoad.coerceAtMost(1f)) * 360f, false, strokeP)

            txtP.color = pal.ink; txtP.textSize = 11f * d
            canvas.drawText(BidiFmt.ltr("${(jobLoad * 100).roundToInt()}%"), gCx, gCy + 4f * d, txtP)
            txtP.color = pal.muted; txtP.textSize = 9.5f * d
            canvas.drawText("משרה", gCx, gCy + gR + 14f * d, txtP)

            // 5 עמודות לוגריתמיות דינמיות בצד ימין
            val items = listOf("שעתי" to hourlyVal, "יומי" to dailyVal, "שבועי" to weeklyVal, "חודשי" to monthlyVal, "שנתי" to yearlyVal)
            val maxLog = ln(yearlyVal.coerceAtLeast(10.0) + 1.0)
            val padL = w * 0.31f; val padR = w - 14f * d
            val slotW = (padR - padL) / 5f
            val baseBot = h - 26f * d; val maxH = h * 0.56f

            for ((i, pair) in items.withIndex()) {
                val logRatio = (ln(pair.second.coerceAtLeast(0.0) + 1.0) / maxLog.coerceAtLeast(1.0)).toFloat().coerceIn(0.12f, 1f)
                val bh = logRatio * maxH
                val bx = padL + i * slotW + slotW * 0.14f
                val bw = slotW * 0.72f
                val isSelectedCol = (
                    (periodIdx == 1 && i == 0) || (periodIdx == 2 && i == 1) ||
                    (periodIdx == 3 && i == 2) || (periodIdx == 0 && i == 3) || (periodIdx == 4 && i == 4)
                )
                fillP.color = if (isSelectedCol) pal.accent2 else pal.accent1
                canvas.drawRoundRect(RectF(bx, baseBot - bh, bx + bw, baseBot), 6f * d, 6f * d, fillP)

                txtP.color = pal.ink; txtP.textSize = 9.5f * d
                val shortNum = if (pair.second >= 10000) "${Utils.formatResult(pair.second / 1000.0, 0)}K" else Utils.formatResult(pair.second, 0)
                canvas.drawText(BidiFmt.ltr("₪$shortNum"), bx + bw / 2f, baseBot - bh - 5f * d, txtP)
                txtP.color = pal.muted
                canvas.drawText(pair.first, bx + bw / 2f, baseBot + 14f * d, txtP)
            }
        }
    }.apply { styleProCanvasCard(this, 175f) }

    val periodBtn = createActionBtn("סוג הקלט: ${periods[0]}", true) {}
    val amountIn = createInputBox("סכום השכר בש״ח (למשל 12000)").apply { setText("12000") }
    val hoursWeekIn = createInputBox("שעות עבודה בשבוע (משרה מלאה = 42)").apply { setText("42") }
    val daysWeekIn = createInputBox("ימי עבודה בשבוע (למשל 5)").apply { setText("5") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun convertSalary() {
        val amt = BidiFmt.evalInputExpr(amountIn, 12000.0).coerceAtLeast(0.0)
        curWeeklyHours = BidiFmt.evalInputExpr(hoursWeekIn, 42.0).coerceIn(1.0, 120.0)
        val daysPerWeek = BidiFmt.evalInputExpr(daysWeekIn, 5.0).coerceIn(1.0, 7.0)

        yearlyVal = when (periodIdx) {
            0 -> amt * 12.0
            1 -> amt * curWeeklyHours * 52.1429
            2 -> amt * daysPerWeek * 52.1429
            3 -> amt * 52.1429
            else -> amt
        }
        monthlyVal = yearlyVal / 12.0
        weeklyVal = yearlyVal / 52.1429
        dailyVal = weeklyVal / daysPerWeek
        hourlyVal = weeklyVal / curWeeklyHours
        salaryCanvas.invalidate()

        resTv.text = BidiFmt.rtlParagraph(
            "פירוט שכר מלא (${BidiFmt.ltr("${Utils.formatResult(curWeeklyHours, 1)} שע׳/שבוע, ${Utils.formatResult(daysPerWeek, 1)} ימים/שבוע")}):",
            "• שכר שעתי: ${BidiFmt.ltr("₪${Utils.formatResult(hourlyVal, 2)}")} | שכר יומי: ${BidiFmt.ltr("₪${Utils.formatResult(dailyVal, 2)}")}",
            "• שכר שבועי: ${BidiFmt.ltr("₪${Utils.formatResult(weeklyVal, 2)}")}",
            "• שכר חודשי: ${BidiFmt.ltr("₪${Utils.formatResult(monthlyVal, 2)}")}",
            "• שכר שנתי כולל (12 חודשים): ${BidiFmt.ltr("₪${Utils.formatResult(yearlyVal, 0)}")}"
        )
    }

    periodBtn.setOnClickListener {
        showItemPicker("בחר את תקופת השכר שהזנת", periods) { chosen ->
            periodIdx = periods.indexOf(chosen).coerceAtLeast(0)
            periodBtn.text = "סוג הקלט: $chosen"
            convertSalary()
        }
    }

    BidiFmt.bindLiveInputs(amountIn, hoursWeekIn, daysWeekIn) { convertSalary() }
    val calcBtn = createActionBtn("המר שכר לכל התקופות 💼", false) { convertSalary() }
    convertSalary()

    root.addView(salaryCanvas); root.addView(periodBtn); root.addView(amountIn); root.addView(hoursWeekIn); root.addView(daysWeekIn)
    root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 88. הנחות מצטברות ומס שירותים עם תרשים מפל מחיר (Waterfall) חי (Tab 88)
// =========================================================================
fun MainActivity.createProStackedDiscountsScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🏷️ הנחות מצטברות ומס שירותים", "תרשים מפל מחיר (Waterfall) חי המראה את ההוזלה בכל הנחה ואת תוספת המס"))

    var origP = 500.0; var afterD1 = 400.0; var afterD2 = 360.0; var finalWithTax = 360.0

    val discCanvas = object : View(this) {
        private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val dashP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(6f, 5f), 0f) }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val pal = getProCanvasColors()

            val maxP = maxOf(origP, finalWithTax, 1.0)
            val padL = 18f * d; val padR = w - 18f * d
            val topY = 24f * d; val botY = h - 28f * d
            val plotH = (botY - topY).coerceAtLeast(10f)
            fun yOf(v: Double): Float = (botY - (v / maxP).coerceIn(0.0, 1.0) * plotH).toFloat()

            val slotW = (padR - padL) / 4f
            val bw = slotW * 0.62f

            val x0 = padL + slotW * 0.5f; val x1 = padL + slotW * 1.5f
            val x2 = padL + slotW * 2.5f; val x3 = padL + slotW * 3.5f

            val yOrig = yOf(origP); val yD1 = yOf(afterD1); val yD2 = yOf(afterD2); val yFin = yOf(finalWithTax)

            // עמודה 1: מחיר מקורי
            fillP.color = pal.muted
            canvas.drawRoundRect(RectF(x0 - bw / 2f, yOrig, x0 + bw / 2f, botY), 6f * d, 6f * d, fillP)

            // קו חיבור מקווקו + נפילת הנחה 1
            dashP.strokeWidth = 1.2f * d; dashP.color = pal.grid
            canvas.drawLine(x0 + bw / 2f, yOrig, x1 - bw / 2f, yOrig, dashP)
            fillP.color = pal.negative
            canvas.drawRoundRect(RectF(x1 - bw / 2f, yOrig, x1 + bw / 2f, yD1.coerceAtLeast(yOrig + 3f * d)), 5f * d, 5f * d, fillP)

            // נפילת הנחה 2
            canvas.drawLine(x1 + bw / 2f, yD1, x2 - bw / 2f, yD1, dashP)
            fillP.color = pal.warning
            canvas.drawRoundRect(RectF(x2 - bw / 2f, yD1, x2 + bw / 2f, yD2.coerceAtLeast(yD1 + 3f * d)), 5f * d, 5f * d, fillP)

            // עמודה סופית לתשלום (כולל מס אם קיים)
            canvas.drawLine(x2 + bw / 2f, yD2, x3 - bw / 2f, yD2, dashP)
            fillP.color = pal.positive
            canvas.drawRoundRect(RectF(x3 - bw / 2f, yFin, x3 + bw / 2f, botY), 6f * d, 6f * d, fillP)

            val labels = listOf(
                Triple(x0, "מקורי", "₪${Utils.formatResult(origP, 0)}"),
                Triple(x1, "הנחה 1", "-₪${Utils.formatResult(origP - afterD1, 0)}"),
                Triple(x2, "הנחה 2", "-₪${Utils.formatResult(afterD1 - afterD2, 0)}"),
                Triple(x3, "סופי", "₪${Utils.formatResult(finalWithTax, 0)}")
            )
            txtP.textSize = 10.5f * d
            for ((idx, item) in labels.withIndex()) {
                val topLblY = when (idx) { 0 -> yOrig; 1 -> yOrig; 2 -> yD1; else -> yFin }
                txtP.color = pal.ink
                canvas.drawText(BidiFmt.ltr(item.third), item.first, (topLblY - 5f * d).coerceAtLeast(12f * d), txtP)
                txtP.color = pal.muted
                canvas.drawText(item.second, item.first, h - 8f * d, txtP)
            }
        }
    }.apply { styleProCanvasCard(this, 180f) }

    val priceIn = createInputBox("מחיר מקורי בש״ח (למשל 500)").apply { setText("500") }
    val d1In = createInputBox("הנחה ראשונה ב-% (למשל 20)").apply { setText("20") }
    val d2In = createInputBox("הנחה שנייה נוספת ב-% (למשל 10)").apply { setText("10") }
    val serviceTaxIn = createInputBox("מס שירותים / דמי שירות ב-% (למשל 0)").apply { setText("0") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun calcDiscounts() {
        origP = BidiFmt.evalInputExpr(priceIn, 500.0).coerceAtLeast(0.0)
        val d1 = BidiFmt.evalInputExpr(d1In, 20.0).coerceIn(0.0, 100.0)
        val d2 = BidiFmt.evalInputExpr(d2In, 10.0).coerceIn(0.0, 100.0)
        val srvTax = BidiFmt.evalInputExpr(serviceTaxIn, 0.0).coerceAtLeast(0.0)

        afterD1 = origP * (1.0 - d1 / 100.0)
        afterD2 = afterD1 * (1.0 - d2 / 100.0)
        val taxAmount = afterD2 * (srvTax / 100.0)
        finalWithTax = afterD2 + taxAmount
        val totalSaved = origP - afterD2
        val effectiveDiscPct = if (origP > 0) (totalSaved / origP) * 100.0 else 0.0
        discCanvas.invalidate()

        resTv.text = BidiFmt.rtlParagraph(
            "ניתוח הנחות מצטברות ומס שירותים:",
            "• מחיר אחרי הנחה ראשונה (${BidiFmt.ltr("${Utils.formatResult(d1, 1)}%")}): ${BidiFmt.ltr("₪${Utils.formatResult(afterD1, 2)}")}",
            "• מחיר אחרי הנחה שנייה (${BidiFmt.ltr("${Utils.formatResult(d2, 1)}%")}): ${BidiFmt.ltr("₪${Utils.formatResult(afterD2, 2)}")}",
            "• אחוז הנחה משוקלל אמיתי: ${BidiFmt.ltr("${Utils.formatResult(effectiveDiscPct, 2)}%")} | סך חיסכון: ${BidiFmt.ltr("₪${Utils.formatResult(totalSaved, 2)}")}",
            if (srvTax > 0) "• מס שירותים (${BidiFmt.ltr("${Utils.formatResult(srvTax, 1)}%")}): ${BidiFmt.ltr("₪${Utils.formatResult(taxAmount, 2)}")}  ➔  לתשלום: ${BidiFmt.ltr("₪${Utils.formatResult(finalWithTax, 2)}")}" else "• סופי לתשלום: ${BidiFmt.ltr("₪${Utils.formatResult(finalWithTax, 2)}")}"
        )
    }

    BidiFmt.bindLiveInputs(priceIn, d1In, d2In, serviceTaxIn) { calcDiscounts() }
    val calcBtn = createActionBtn("חשב הנחות מצטברות ומס שירותים 🏷️", true) { calcDiscounts() }
    calcDiscounts()

    root.addView(discCanvas); root.addView(priceIn); root.addView(d1In); root.addView(d2In); root.addView(serviceTaxIn)
    root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 89. תשואת מניות ודיבידנד עם טבעת פילוח תיק ונר רווח/הפסד חי (Tab 89)
// =========================================================================
fun MainActivity.createProStockAndDividendScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("📈 תשואת מניות ודיבידנד (Stocks & Dividend Yield)", "טבעת פילוח תיק ונר שינוי מחיר חי כולל תשואת דיבידנד וניכוי מס רווחי הון (25%)"))

    var curBuyTotal = 10000.0; var curSellTotal = 12500.0; var curCapGain = 2500.0; var curDivTotal = 450.0; var curTax = 737.5

    val stockCanvas = object : View(this) {
        private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val pal = getProCanvasColors()

            val netProfit = curCapGain + curDivTotal - curTax
            val roiPct = if (curBuyTotal > 0) (netProfit / curBuyTotal) * 100.0 else 0.0
            val isProfit = netProfit >= 0

            // צד שמאל: טבעת Donut של קרן / רווח נקי / דיבידנד / מס
            val cx = w * 0.25f; val cy = h * 0.50f; val r = min(w * 0.17f, h * 0.34f)
            val rect = RectF(cx - r, cy - r, cx + r, cy + r)
            strokeP.strokeWidth = 9f * d; strokeP.color = pal.grid; strokeP.clearShadowLayer()
            canvas.drawCircle(cx, cy, r, strokeP)

            val gainPart = max(0.0, curCapGain)
            val totalPie = (curBuyTotal + gainPart + curDivTotal + curTax).coerceAtLeast(1.0)
            val slices = listOf(
                curBuyTotal to pal.accent2,
                gainPart to pal.positive,
                curDivTotal to pal.warning,
                curTax to pal.negative
            )
            var ang = -90f
            for ((v, col) in slices) {
                if (v <= 0.0) continue
                val sw = ((v / totalPie) * 360.0).toFloat()
                strokeP.color = col
                strokeP.applyAuroraGlow(pal.isAurora, col, d)
                canvas.drawArc(rect, ang, sw.coerceAtLeast(2f), false, strokeP)
                ang += sw
            }

            txtP.color = if (isProfit) pal.positive else pal.negative; txtP.textSize = 13.5f * d
            val sign = if (isProfit) "+" else ""
            canvas.drawText(BidiFmt.ltr("$sign${Utils.formatResult(roiPct, 1)}%"), cx, cy + 5f * d, txtP)

            // צד ימין: השוואת עמודות שווי קנייה מול שווי מכירה + דיבידנד
            val colL = w * 0.52f; val colR = w - 20f * d
            val maxBarV = maxOf(curBuyTotal, curSellTotal + curDivTotal, 1.0)
            val botY = h - 26f * d; val maxH = h * 0.54f
            val bw = (colR - colL) * 0.36f

            val hBuy = ((curBuyTotal / maxBarV).toFloat().coerceIn(0.12f, 1f)) * maxH
            val hNow = (((curSellTotal + curDivTotal) / maxBarV).toFloat().coerceIn(0.12f, 1f)) * maxH

            val bx1 = colL + bw * 0.15f; val bx2 = colR - bw * 1.15f
            fillP.color = pal.accent2
            canvas.drawRoundRect(RectF(bx1, botY - hBuy, bx1 + bw, botY), 7f * d, 7f * d, fillP)
            fillP.color = if (isProfit) pal.positive else pal.negative
            canvas.drawRoundRect(RectF(bx2, botY - hNow, bx2 + bw, botY), 7f * d, 7f * d, fillP)

            txtP.color = pal.ink; txtP.textSize = 10.5f * d
            canvas.drawText(BidiFmt.ltr("₪${Utils.formatResult(curBuyTotal, 0)}"), bx1 + bw / 2f, botY - hBuy - 5f * d, txtP)
            canvas.drawText(BidiFmt.ltr("₪${Utils.formatResult(curSellTotal + curDivTotal, 0)}"), bx2 + bw / 2f, botY - hNow - 5f * d, txtP)
            txtP.color = pal.muted
            canvas.drawText("קנייה", bx1 + bw / 2f, h - 8f * d, txtP)
            canvas.drawText("שווי+דיב׳", bx2 + bw / 2f, h - 8f * d, txtP)
        }
    }.apply { styleProCanvasCard(this, 170f) }

    val buyPriceIn = createInputBox("מחיר קנייה למניה (למשל 100)").apply { setText("100") }
    val sellPriceIn = createInputBox("מחיר מכירה / נוכחי למניה (למשל 125)").apply { setText("125") }
    val sharesIn = createInputBox("כמות מניות (למשל 100)").apply { setText("100") }
    val divPerShareIn = createInputBox("דיבידנד שנתי למניה (למשל 4.5)").apply { setText("4.5") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun calcStocks() {
        val buyP = BidiFmt.evalInputExpr(buyPriceIn, 100.0).coerceAtLeast(0.01)
        val sellP = BidiFmt.evalInputExpr(sellPriceIn, 125.0).coerceAtLeast(0.0)
        val shares = BidiFmt.evalInputExpr(sharesIn, 100.0).coerceAtLeast(0.0)
        val divPerShare = BidiFmt.evalInputExpr(divPerShareIn, 4.5).coerceAtLeast(0.0)

        curBuyTotal = buyP * shares
        curSellTotal = sellP * shares
        curCapGain = curSellTotal - curBuyTotal
        curDivTotal = divPerShare * shares

        val divYieldOnCost = (divPerShare / buyP) * 100.0
        val divYieldCurrent = if (sellP > 0) (divPerShare / sellP) * 100.0 else 0.0
        val grossProfit = curCapGain + curDivTotal
        curTax = if (grossProfit > 0) grossProfit * 0.25 else 0.0
        val netProfit = grossProfit - curTax
        stockCanvas.invalidate()

        resTv.text = BidiFmt.rtlParagraph(
            "סיכום השקעה במניות ודיבידנדים:",
            "• שווי קנייה: ${BidiFmt.ltr("₪${Utils.formatResult(curBuyTotal, 2)}")}  ➔  שווי מכירה: ${BidiFmt.ltr("₪${Utils.formatResult(curSellTotal, 2)}")}",
            "• רווח הון ברוטו: ${BidiFmt.ltr("₪${Utils.formatResult(curCapGain, 2)}")} (${BidiFmt.ltr("${Utils.formatResult(((sellP - buyP) / buyP) * 100, 2)}%")})",
            "• הכנסה מדיבידנדים: ${BidiFmt.ltr("₪${Utils.formatResult(curDivTotal, 2)}")} לשנה (${BidiFmt.ltr("₪${Utils.formatResult(curDivTotal / 4.0, 2)}")} לרבעון)",
            "• תשואת דיבידנד: נוכחית ${BidiFmt.ltr("${Utils.formatResult(divYieldCurrent, 2)}%")} | על העלות ${BidiFmt.ltr("${Utils.formatResult(divYieldOnCost, 2)}%")}",
            "• רווח נטו לאחר מס (25%): ${BidiFmt.ltr("₪${Utils.formatResult(netProfit, 2)}")}"
        )
    }

    BidiFmt.bindLiveInputs(buyPriceIn, sellPriceIn, sharesIn, divPerShareIn) { calcStocks() }
    val calcBtn = createActionBtn("חשב תשואת מניות ודיבידנד 📈", true) { calcStocks() }
    calcStocks()

    root.addView(stockCanvas); root.addView(buyPriceIn); root.addView(sellPriceIn); root.addView(sharesIn); root.addView(divPerShareIn)
    root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 90. פיקדון קבוע, תעודת פיקדון וריבית פשוטה עם גרף 2 עקומות חי (Tab 90)
// =========================================================================
fun MainActivity.createProFixedDepositAndSimpleInterestScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🏦 פיקדון קבוע, תעודת פיקדון וריבית פשוטה", "גרף השוואת עקומות חי המדגים את פער הרווח בין ריבית פשוטה לפיקדון בריבית דריבית"))

    var curPrincipal = 50000.0; var curRate = 0.05; var curYears = 3.0
    var curSimpleInterest = 7500.0; var curCompoundInterest = 8073.0

    val fdCanvas = object : View(this) {
        private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val dashP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(9f, 6f), 0f) }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val pal = getProCanvasColors()

            val padL = 22f * d; val padR = w - 22f * d
            val padT = 26f * d; val padB = h - 30f * d
            val plotW = (padR - padL).coerceAtLeast(10f); val plotH = (padB - padT).coerceAtLeast(10f)

            val minV = curPrincipal * 0.92
            val maxV = maxOf(curPrincipal + curCompoundInterest, curPrincipal + 10.0)
            val span = (maxV - minV).coerceAtLeast(1.0)
            fun yOf(v: Double): Float = (padB - ((v - minV) / span).coerceIn(0.0, 1.0) * plotH).toFloat()

            val steps = 24
            val compPath = Path(); val simpPath = Path(); val gapPoly = Path()
            val simpPoints = ArrayList<Pair<Float, Float>>(steps + 1)

            for (s in 0..steps) {
                val frac = s.toDouble() / steps
                val t = frac * curYears
                val vComp = curPrincipal * (1.0 + curRate / 12.0).pow(t * 12.0)
                val vSimp = curPrincipal * (1.0 + curRate * t)
                val px = (padL + frac * plotW).toFloat()
                val pyC = yOf(vComp); val pyS = yOf(vSimp)
                simpPoints.add(px to pyS)
                if (s == 0) {
                    compPath.moveTo(px, pyC); simpPath.moveTo(px, pyS); gapPoly.moveTo(px, pyC)
                } else {
                    compPath.lineTo(px, pyC); simpPath.lineTo(px, pyS); gapPoly.lineTo(px, pyC)
                }
            }
            for (s in steps downTo 0) gapPoly.lineTo(simpPoints[s].first, simpPoints[s].second)
            gapPoly.close()

            fillP.color = Color.argb(65, Color.red(pal.positive), Color.green(pal.positive), Color.blue(pal.positive))
            canvas.drawPath(gapPoly, fillP)

            dashP.strokeWidth = 2f * d; dashP.color = pal.accent2
            canvas.drawPath(simpPath, dashP)

            lineP.strokeWidth = 2.8f * d; lineP.color = pal.positive
            lineP.applyAuroraGlow(pal.isAurora, pal.positive, d)
            canvas.drawPath(compPath, lineP)

            txtP.color = pal.ink
            val bonus = (curCompoundInterest - curSimpleInterest).coerceAtLeast(0.0)
            CanvasUiHelper.drawFittedText(canvas, "\u200Fדריבית: ${BidiFmt.ltr("₪${Utils.formatResult(curPrincipal + curCompoundInterest, 0)}")}  |  יתרון דריבית: ${BidiFmt.ltr("+₪${Utils.formatResult(bonus, 0)}")}", w / 2f, 18f * d, w - 24f * d, 13f * d, 9f * d, txtP)
            txtP.color = pal.muted; txtP.textSize = 11f * d
            canvas.drawText("\u200Fעקומה ירוקה = פיקדון דריבית  •  קו מקווקו = ריבית פשוטה", w / 2f, h - 8f * d, txtP)
        }
    }.apply { styleProCanvasCard(this, 180f) }

    val principalIn = createInputBox("סכום הפקדה לפיקדון בש״ח (למשל 50000)").apply { setText("50000") }
    val rateIn = createInputBox("ריבית שנתית ב-% (למשל 5)").apply { setText("5") }
    val yearsIn = createInputBox("תקופת הפיקדון בשנים (למשל 3)").apply { setText("3") }
    val taxRateIn = createInputBox("מס רווחי פיקדון ב-% (נומינלי = 15%, ריאלי = 25%)").apply { setText("15") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun calcDeposits() {
        curPrincipal = BidiFmt.evalInputExpr(principalIn, 50000.0).coerceAtLeast(0.0)
        val rPct = BidiFmt.evalInputExpr(rateIn, 5.0).coerceAtLeast(0.0)
        curYears = BidiFmt.evalInputExpr(yearsIn, 3.0).coerceAtLeast(0.1)
        val taxPct = BidiFmt.evalInputExpr(taxRateIn, 15.0).coerceIn(0.0, 100.0)

        curRate = rPct / 100.0
        curSimpleInterest = curPrincipal * curRate * curYears
        val simpleNet = curSimpleInterest * (1.0 - taxPct / 100.0)
        val compoundTotalMonthly = curPrincipal * (1.0 + curRate / 12.0).pow(curYears * 12.0)
        curCompoundInterest = compoundTotalMonthly - curPrincipal
        val compoundNet = curCompoundInterest * (1.0 - taxPct / 100.0)
        fdCanvas.invalidate()

        resTv.text = BidiFmt.rtlParagraph(
            "1️⃣ ריבית פשוטה (Simple Interest):",
            "• רווח ברוטו: ${BidiFmt.ltr("₪${Utils.formatResult(curSimpleInterest, 2)}")} | נטו לאחר מס (${BidiFmt.ltr("${Utils.formatResult(taxPct, 0)}%")}): ${BidiFmt.ltr("₪${Utils.formatResult(simpleNet, 2)}")}",
            "• סכום פדיון נטו: ${BidiFmt.ltr("₪${Utils.formatResult(curPrincipal + simpleNet, 2)}")}",
            "",
            "2️⃣ פיקדון קבוע / תעודת פיקדון (FD / CD צבירה חודשית):",
            "• רווח ברוטו: ${BidiFmt.ltr("₪${Utils.formatResult(curCompoundInterest, 2)}")} | נטו לאחר מס: ${BidiFmt.ltr("₪${Utils.formatResult(compoundNet, 2)}")}",
            "• סכום פדיון נטו: ${BidiFmt.ltr("₪${Utils.formatResult(curPrincipal + compoundNet, 2)}")} (יתרון דריבית: ${BidiFmt.ltr("+₪${Utils.formatResult(compoundNet - simpleNet, 2)}")})"
        )
    }

    BidiFmt.bindLiveInputs(principalIn, rateIn, yearsIn, taxRateIn) { calcDeposits() }
    val calcBtn = createActionBtn("חשב פיקדון וריבית פשוטה 🏦", true) { calcDeposits() }
    calcDeposits()

    root.addView(fdCanvas); root.addView(principalIn); root.addView(rateIn); root.addView(yearsIn); root.addView(taxRateIn)
    root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 91. מחשבון SIP ואינפלציה עם גרף 3 עקומות (הפקדות / נומינלי / ריאלי) (Tab 91)
// =========================================================================
fun MainActivity.createProSipAndInflationScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🌱 מחשבון SIP ושחיקת אינפלציה", "גרף 3 עקומות חי: הפקדות מהכיס, שווי נומינלי וכוח קנייה ריאלי בניכוי אינפלציה"))

    var curPmt = 1000.0; var curYears = 10.0; var curRetPct = 12.0; var curInfPct = 2.5
    var curInvested = 120000.0; var curSipFuture = 232000.0; var curRealPurchasingPower = 180000.0

    val sipCanvas = object : View(this) {
        private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val dashP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f) }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val pal = getProCanvasColors()

            val padL = 22f * d; val padR = w - 22f * d
            val padT = 26f * d; val padB = h - 30f * d
            val plotW = (padR - padL).coerceAtLeast(10f); val plotH = (padB - padT).coerceAtLeast(10f)
            val maxY = maxOf(curSipFuture, curInvested, 100.0)
            fun yOf(v: Double): Float = (padB - (v / maxY).coerceIn(0.0, 1.0) * plotH).toFloat()

            val steps = 24
            val rM = (curRetPct / 100.0) / 12.0
            val nomPath = Path(); val realPath = Path(); val depPath = Path(); val erosionPoly = Path()
            val realPts = ArrayList<Pair<Float, Float>>(steps + 1)

            for (s in 0..steps) {
                val frac = s.toDouble() / steps
                val yr = frac * curYears
                val m = yr * 12.0
                val dep = curPmt * m
                val nom = if (rM > 1e-9) curPmt * (((1.0 + rM).pow(m) - 1.0) / rM) * (1.0 + rM) else dep
                val real = nom / (1.0 + curInfPct / 100.0).pow(yr)

                val px = (padL + frac * plotW).toFloat()
                val pyN = yOf(nom); val pyR = yOf(real); val pyD = yOf(dep)
                realPts.add(px to pyR)

                if (s == 0) {
                    nomPath.moveTo(px, pyN); realPath.moveTo(px, pyR); depPath.moveTo(px, pyD); erosionPoly.moveTo(px, pyN)
                } else {
                    nomPath.lineTo(px, pyN); realPath.lineTo(px, pyR); depPath.lineTo(px, pyD); erosionPoly.lineTo(px, pyN)
                }
            }
            for (s in steps downTo 0) erosionPoly.lineTo(realPts[s].first, realPts[s].second)
            erosionPoly.close()

            fillP.color = Color.argb(55, Color.red(pal.warning), Color.green(pal.warning), Color.blue(pal.warning))
            canvas.drawPath(erosionPoly, fillP)

            dashP.strokeWidth = 1.6f * d; dashP.color = pal.accent2
            canvas.drawPath(depPath, dashP)

            lineP.strokeWidth = 2.4f * d; lineP.color = pal.warning; lineP.clearShadowLayer()
            canvas.drawPath(realPath, lineP)

            lineP.strokeWidth = 2.8f * d; lineP.color = pal.positive
            lineP.applyAuroraGlow(pal.isAurora, pal.positive, d)
            canvas.drawPath(nomPath, lineP)

            txtP.color = pal.ink
            CanvasUiHelper.drawFittedText(canvas, "\u200Fנומינלי: ${BidiFmt.ltr("₪${Utils.formatResult(curSipFuture, 0)}")}  |  ריאלי: ${BidiFmt.ltr("₪${Utils.formatResult(curRealPurchasingPower, 0)}")}", w / 2f, 18f * d, w - 24f * d, 13f * d, 9f * d, txtP)
            txtP.color = pal.muted; txtP.textSize = 10.5f * d
            canvas.drawText("\u200Fירוק = נומינלי • כתום = כוח קנייה ריאלי • מקווקו = הפקדות", w / 2f, h - 8f * d, txtP)
        }
    }.apply { styleProCanvasCard(this, 190f) }

    val monthlyIn = createInputBox("הפקדה חודשית קבועה ב-SIP בש״ח (למשל 1000)").apply { setText("1000") }
    val yearsIn = createInputBox("תקופת השקעה בשנים (למשל 10)").apply { setText("10") }
    val returnIn = createInputBox("תשואה שנתית צפויה ב-% (למשל 12)").apply { setText("12") }
    val inflationIn = createInputBox("שיעור אינפלציה שנתי ממוצע ב-% (למשל 2.5)").apply { setText("2.5") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun calcSipAndInflation() {
        curPmt = BidiFmt.evalInputExpr(monthlyIn, 1000.0).coerceAtLeast(0.0)
        curYears = BidiFmt.evalInputExpr(yearsIn, 10.0).coerceAtLeast(0.1)
        curRetPct = BidiFmt.evalInputExpr(returnIn, 12.0).coerceAtLeast(0.0)
        curInfPct = BidiFmt.evalInputExpr(inflationIn, 2.5).coerceAtLeast(0.0)

        val n = curYears * 12.0; val i = (curRetPct / 100.0) / 12.0
        curInvested = curPmt * n
        curSipFuture = if (i > 1e-9) curPmt * (((1.0 + i).pow(n) - 1.0) / i) * (1.0 + i) else curInvested
        val infFactor = (1.0 + curInfPct / 100.0).pow(curYears)
        curRealPurchasingPower = curSipFuture / infFactor
        sipCanvas.invalidate()

        resTv.text = BidiFmt.rtlParagraph(
            "תוצאות תוכנית השקעה שיטתית (SIP):",
            "• סך הפקדות מהכיס (${BidiFmt.ltr("${n.roundToInt()} חודשים")}): ${BidiFmt.ltr("₪${Utils.formatResult(curInvested, 0)}")}",
            "• רווח הון נומינלי: ${BidiFmt.ltr("₪${Utils.formatResult(curSipFuture - curInvested, 0)}")}  ➔  שווי תיק: ${BidiFmt.ltr("₪${Utils.formatResult(curSipFuture, 0)}")}",
            "",
            "📉 השפעת האינפלציה (${BidiFmt.ltr("${Utils.formatResult(curInfPct, 1)}%")} לשנה):",
            "• כוח קנייה ריאלי במונחי היום: ${BidiFmt.ltr("₪${Utils.formatResult(curRealPurchasingPower, 0)}")}",
            "• שחיקת אינפלציה מצטברת: ${BidiFmt.ltr("${Utils.formatResult((1.0 - 1.0 / infFactor) * 100, 1)}%")}"
        )
    }

    BidiFmt.bindLiveInputs(monthlyIn, yearsIn, returnIn, inflationIn) { calcSipAndInflation() }
    val calcBtn = createActionBtn("חשב צמיחת SIP ואינפלציה 🌱", true) { calcSipAndInflation() }
    calcSipAndInflation()

    root.addView(sipCanvas); root.addView(monthlyIn); root.addView(yearsIn); root.addView(returnIn); root.addView(inflationIn)
    root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 92. הלוואת רכב ו-E.M.I עם טבעת פילוח 4-חלקים ועקומת סילוקין חיה (Tab 92)
// =========================================================================
fun MainActivity.createProCarLoanAndEmiScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🚗 הלוואת רכב ו-E.M.I (Auto Financing & EMI)", "טבעת פילוח עלות הרכב ועקומת ירידת יתרת החוב עד תום ההלוואה או הבלון"))

    var curDown = 40000.0; var curNetLoan = 90000.0; var curBalloon = 0.0; var curTotalInterest = 14800.0
    var curEmi = 1850.0; var curMonths = 60.0; var curRateYr = 6.5

    val emiCanvas = object : View(this) {
        private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val pal = getProCanvasColors()

            // צד שמאל: טבעת Donut ל-4 רכיבי עלות הרכב
            val cx = w * 0.24f; val cy = h * 0.50f; val r = min(w * 0.16f, h * 0.34f)
            val rect = RectF(cx - r, cy - r, cx + r, cy + r)
            strokeP.strokeWidth = 9f * d; strokeP.color = pal.grid; strokeP.clearShadowLayer()
            canvas.drawCircle(cx, cy, r, strokeP)

            val amortPrincipal = (curNetLoan - curBalloon).coerceAtLeast(0.0)
            val grandTotal = (curDown + amortPrincipal + curBalloon + curTotalInterest).coerceAtLeast(1.0)
            val parts = listOf(
                curDown to pal.accent2,
                amortPrincipal to pal.positive,
                curBalloon to pal.warning,
                curTotalInterest to pal.negative
            )
            var ang = -90f
            for ((v, col) in parts) {
                if (v <= 0.0) continue
                val sw = ((v / grandTotal) * 360.0).toFloat()
                strokeP.color = col
                strokeP.applyAuroraGlow(pal.isAurora, col, d)
                canvas.drawArc(rect, ang, sw.coerceAtLeast(2f), false, strokeP)
                ang += sw
            }

            txtP.color = pal.ink; txtP.textSize = 11.5f * d
            canvas.drawText(BidiFmt.ltr("₪${Utils.formatResult(curEmi, 0)}"), cx, cy + 2f * d, txtP)
            txtP.color = pal.muted; txtP.textSize = 9f * d
            canvas.drawText("EMI/חודש", cx, cy + 14f * d, txtP)

            // צד ימין: עקומת סילוקין חיה של יתרת ההלוואה מ-month 0 ועד month N (כולל מדרגת בלון!)
            val gL = w * 0.46f; val gR = w - 18f * d; val gT = 24f * d; val gB = h - 26f * d
            val gW = (gR - gL).coerceAtLeast(10f); val gH = (gB - gT).coerceAtLeast(10f)
            lineP.strokeWidth = 1.1f * d; lineP.color = pal.grid; lineP.clearShadowLayer()
            canvas.drawLine(gL, gB, gR, gB, lineP)
            canvas.drawLine(gL, gT, gL, gB, lineP)

            val rM = (curRateYr / 100.0) / 12.0
            val balPath = Path()
            val maxBal = curNetLoan.coerceAtLeast(1.0)
            var runningBal = curNetLoan
            val totalSteps = curMonths.roundToInt().coerceIn(1, 240)
            balPath.moveTo(gL, gT)
            for (m in 1..totalSteps) {
                val interestM = runningBal * rM
                runningBal = (runningBal + interestM - curEmi).coerceAtLeast(curBalloon)
                val px = gL + (m.toFloat() / totalSteps) * gW
                val py = (gB - (runningBal / maxBal).coerceIn(0.0, 1.0) * gH).toFloat()
                balPath.lineTo(px, py)
            }
            if (curBalloon > 0.0) balPath.lineTo(gR, gB)

            lineP.strokeWidth = 2.6f * d; lineP.color = pal.accent1
            lineP.applyAuroraGlow(pal.isAurora, pal.accent1, d)
            canvas.drawPath(balPath, lineP)

            txtP.color = pal.ink; txtP.textSize = 11f * d
            CanvasUiHelper.drawFittedText(canvas, "\u200Fירידת יתרת חוב (${BidiFmt.ltr("${totalSteps} חוד׳")})", (gL + gR) / 2f, 16f * d, gW, 11.5f * d, 8.5f * d, txtP)
            txtP.color = pal.muted; txtP.textSize = 10f * d
            canvas.drawText(if (curBalloon > 0) "\u200Fבלון: ${BidiFmt.ltr("₪${Utils.formatResult(curBalloon, 0)}")}" else "יתרה בסוף: ₪0", (gL + gR) / 2f, h - 8f * d, txtP)
        }
    }.apply { styleProCanvasCard(this, 175f) }

    val carPriceIn = createInputBox("מחיר הרכב / סכום העסקה בש״ח (למשל 130000)").apply { setText("130000") }
    val downPaymentIn = createInputBox("מקדמה + טרייד-אין בש״ח (למשל 40000)").apply { setText("40000") }
    val monthsIn = createInputBox("תקופת הלוואה בחודשים (למשל 60)").apply { setText("60") }
    val rateIn = createInputBox("ריבית שנתית ב-% (למשל 6.5)").apply { setText("6.5") }
    val balloonIn = createInputBox("יתרת בלון בסוף תקופה בש״ח (0 אם אין)").apply { setText("0") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun calcCarEmi() {
        val price = BidiFmt.evalInputExpr(carPriceIn, 130000.0).coerceAtLeast(0.0)
        curDown = BidiFmt.evalInputExpr(downPaymentIn, 40000.0).coerceAtLeast(0.0)
        curMonths = BidiFmt.evalInputExpr(monthsIn, 60.0).coerceAtLeast(1.0)
        curRateYr = BidiFmt.evalInputExpr(rateIn, 6.5).coerceAtLeast(0.0)
        curBalloon = BidiFmt.evalInputExpr(balloonIn, 0.0).coerceAtLeast(0.0)

        curNetLoan = (price - curDown).coerceAtLeast(0.0)
        val r = (curRateYr / 100.0) / 12.0
        val amortizedPrincipal = (curNetLoan - curBalloon / (1.0 + r).pow(curMonths)).coerceAtLeast(0.0)

        curEmi = if (r > 1e-9) {
            amortizedPrincipal * r * (1.0 + r).pow(curMonths) / ((1.0 + r).pow(curMonths) - 1.0)
        } else {
            (curNetLoan - curBalloon) / curMonths
        }
        val totalPaidOnLoan = curEmi * curMonths + curBalloon
        curTotalInterest = (totalPaidOnLoan - curNetLoan).coerceAtLeast(0.0)
        emiCanvas.invalidate()

        resTv.text = BidiFmt.rtlParagraph(
            "סיכום מימון רכב והחזר E.M.I:",
            "• סכום הלוואה נטו למימון: ${BidiFmt.ltr("₪${Utils.formatResult(curNetLoan, 0)}")}",
            "• החזר חודשי קבוע (E.M.I): ${BidiFmt.ltr("₪${Utils.formatResult(curEmi, 2)}")}",
            if (curBalloon > 0) "• תשלום בלון בסוף התקופה: ${BidiFmt.ltr("₪${Utils.formatResult(curBalloon, 0)}")}" else "• סוג סילוקין: שפיצר מלא ללא בלון",
            "• סך ריבית כוללת: ${BidiFmt.ltr("₪${Utils.formatResult(curTotalInterest, 0)}")}",
            "• עלות כוללת של הרכב: ${BidiFmt.ltr("₪${Utils.formatResult(curDown + totalPaidOnLoan, 0)}")}"
        )
    }

    BidiFmt.bindLiveInputs(carPriceIn, downPaymentIn, monthsIn, rateIn, balloonIn) { calcCarEmi() }
    val calcBtn = createActionBtn("חשב מימון רכב ו-E.M.I 🚗", true) { calcCarEmi() }
    calcCarEmi()

    root.addView(emiCanvas); root.addView(carPriceIn); root.addView(downPaymentIn); root.addView(monthsIn); root.addView(rateIn); root.addView(balloonIn)
    root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 93. ערך עתידי וערך נוכחי עם עקומת היוון וצמיחה מעריכית חיה (Tab 93)
// =========================================================================
fun MainActivity.createProFutureAndPresentValueScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("⏳ ערך עתידי וערך נוכחי (FV & PV)", "עקומת הזמן של הכסף המשתנה בזמן אמת מהיוון העבר (PV) דרך היום אל העתיד (FV)"))

    var curBaseAmount = 100000.0; var curRate = 0.07; var curYears = 5.0
    var curFv = 140255.0; var curPv = 71298.0

    val fvPvCanvas = object : View(this) {
        private val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val pal = getProCanvasColors()

            val padL = 28f * d; val padR = w - 28f * d
            val padT = 26f * d; val padB = h - 30f * d
            val plotW = (padR - padL).coerceAtLeast(10f); val plotH = (padB - padT).coerceAtLeast(10f)

            val maxV = maxOf(curFv, curBaseAmount, curPv, 1.0)
            fun yOf(v: Double): Float = (padB - (v / maxV).coerceIn(0.0, 1.0) * plotH).toFloat()

            val steps = 28
            val curvePath = Path(); val areaPath = Path()
            for (s in 0..steps) {
                val frac = s.toDouble() / steps // 0 = -n (PV), 0.5 = today, 1 = +n (FV)
                val t = (frac - 0.5) * 2.0 * curYears
                val v = curBaseAmount * (1.0 + curRate).pow(t)
                val px = (padL + frac * plotW).toFloat()
                val py = yOf(v)
                if (s == 0) {
                    curvePath.moveTo(px, py)
                    areaPath.moveTo(px, padB); areaPath.lineTo(px, py)
                } else {
                    curvePath.lineTo(px, py); areaPath.lineTo(px, py)
                }
            }
            areaPath.lineTo(padR, padB); areaPath.close()

            fillP.color = Color.argb(45, Color.red(pal.accent1), Color.green(pal.accent1), Color.blue(pal.accent1))
            canvas.drawPath(areaPath, fillP)

            lineP.strokeWidth = 2.8f * d; lineP.color = pal.accent1
            lineP.applyAuroraGlow(pal.isAurora, pal.accent1, d)
            canvas.drawPath(curvePath, lineP)

            val pts = listOf(
                Triple(padL, yOf(curPv), "PV: ₪${Utils.formatResult(curPv, 0)}" to pal.accent2),
                Triple(padL + plotW * 0.5f, yOf(curBaseAmount), "היום: ₪${Utils.formatResult(curBaseAmount, 0)}" to pal.warning),
                Triple(padR, yOf(curFv), "FV: ₪${Utils.formatResult(curFv, 0)}" to pal.positive)
            )
            for ((px, py, pair) in pts) {
                fillP.color = pair.second
                canvas.drawCircle(px, py, 6f * d, fillP)
                txtP.color = pal.ink; txtP.textSize = 10.5f * d
                val tx = px.coerceIn(50f * d, w - 50f * d)
                canvas.drawText(BidiFmt.ltr(pair.first), tx, (py - 10f * d).coerceAtLeast(14f * d), txtP)
            }

            txtP.color = pal.muted; txtP.textSize = 11f * d
            canvas.drawText("\u200F${BidiFmt.ltr("-${Utils.formatResult(curYears, 1)}y")}  ◄──────  היום  ──────►  ${BidiFmt.ltr("+${Utils.formatResult(curYears, 1)}y")}", w / 2f, h - 8f * d, txtP)
        }
    }.apply { styleProCanvasCard(this, 175f) }

    val amountIn = createInputBox("סכום כספי לחישוב בש״ח (למשל 100000)").apply { setText("100000") }
    val rateIn = createInputBox("ריבית / שיעור היוון שנתי ב-% (למשל 7)").apply { setText("7") }
    val yearsIn = createInputBox("מספר שנים (n - למשל 5)").apply { setText("5") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun calcFvPv() {
        curBaseAmount = BidiFmt.evalInputExpr(amountIn, 100000.0).coerceAtLeast(0.0)
        curRate = BidiFmt.evalInputExpr(rateIn, 7.0) / 100.0
        curYears = BidiFmt.evalInputExpr(yearsIn, 5.0).coerceAtLeast(0.0)

        val factor = (1.0 + curRate).pow(curYears)
        curFv = curBaseAmount * factor
        curPv = if (factor > 1e-9) curBaseAmount / factor else 0.0
        fvPvCanvas.invalidate()

        resTv.text = BidiFmt.rtlParagraph(
            "ערך הזמן של הכסף (${BidiFmt.ltr("r = ${Utils.formatResult(curRate * 100, 2)}%, n = ${Utils.formatResult(curYears, 1)} שנים")}):",
            "• ערך עתידי (${BidiFmt.ltr("FV")}): ${BidiFmt.ltr("₪${Utils.formatResult(curFv, 2)}")}",
            "• ערך נוכחי מהוון (${BidiFmt.ltr("PV")}): ${BidiFmt.ltr("₪${Utils.formatResult(curPv, 2)}")}",
            "• מקדם היוון / צמיחה: ${BidiFmt.ltr("×${Utils.formatResult(factor, 4)}")}"
        )
    }

    BidiFmt.bindLiveInputs(amountIn, rateIn, yearsIn) { calcFvPv() }
    val calcBtn = createActionBtn("חשב ערך עתידי וערך נוכחי ⏳", true) { calcFvPv() }
    calcFvPv()

    root.addView(fvPvCanvas); root.addView(amountIn); root.addView(rateIn); root.addView(yearsIn)
    root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 94. אנונה ואנונה נצחית עם קנבס תזרים תשלומים מהוון חי (Tab 94 - חדש!)
// =========================================================================
fun MainActivity.createProAnnuityAndPerpetuityScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("♾️ אנונה ואנונה נצחית (Annuity & Perpetuity)", "קנבס תזרים מזומנים מהוון חי המציג את התכווצות ערך כל תשלום מול שווי האנונה הנצחית"))

    var curPmt = 5000.0; var curRate = 0.06; var curPeriods = 20
    var curPvAnnuity = 57349.6; var curPvPerpetuity = 83333.3

    val annuityCanvas = object : View(this) {
        private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val pal = getProCanvasColors()

            val padL = 18f * d; val padR = w - 18f * d
            val topY = 28f * d; val midY = h * 0.64f
            val barsH = (midY - topY).coerceAtLeast(10f)

            val showN = curPeriods.coerceIn(1, 14)
            val slotW = (padR - padL) / showN
            val maxPmt = curPmt.coerceAtLeast(1.0)

            for (k in 1..showN) {
                val pvK = curPmt / (1.0 + curRate).pow(k.toDouble())
                val bh = ((pvK / maxPmt).toFloat().coerceIn(0.08f, 1f)) * barsH
                val bx = padL + (k - 1) * slotW + slotW * 0.16f
                val bw = slotW * 0.68f

                fillP.color = pal.accent1
                canvas.drawRoundRect(RectF(bx, midY - bh, bx + bw, midY), 4f * d, 4f * d, fillP)
            }

            lineP.strokeWidth = 1.2f * d; lineP.color = pal.grid
            canvas.drawLine(padL, midY, padR, midY, lineP)

            // מד השוואה תחתון: אחוז כיסוי של אנונה סופית מתוך אנונה נצחית (PV Annuity / PV Perpetuity)
            val ratioOfInf = (curPvAnnuity / curPvPerpetuity.coerceAtLeast(1.0)).toFloat().coerceIn(0.05f, 1f)
            val barT = h * 0.74f; val barB = h * 0.84f
            fillP.color = pal.grid
            canvas.drawRoundRect(RectF(padL, barT, padR, barB), 6f * d, 6f * d, fillP)
            fillP.color = pal.accent2
            canvas.drawRoundRect(RectF(padL, barT, padL + (padR - padL) * ratioOfInf, barB), 6f * d, 6f * d, fillP)

            txtP.color = pal.ink
            CanvasUiHelper.drawFittedText(canvas, "\u200Fאנונה (${curPeriods} תק׳): ${BidiFmt.ltr("₪${Utils.formatResult(curPvAnnuity, 0)}")}  |  נצחית: ${BidiFmt.ltr("₪${Utils.formatResult(curPvPerpetuity, 0)}")}", w / 2f, 18f * d, w - 24f * d, 13f * d, 9f * d, txtP)
            txtP.color = pal.muted; txtP.textSize = 10.5f * d
            canvas.drawText("\u200Fהאנונה מהווה ${BidiFmt.ltr("${(ratioOfInf * 100).roundToInt()}%")} משווי האנונה הנצחית האינסופית", w / 2f, h - 5f * d, txtP)
        }
    }.apply { styleProCanvasCard(this, 185f) }

    val pmtIn = createInputBox("תשלום תקופתי קבוע בש״ח (PMT - למשל 5000)").apply { setText("5000") }
    val rateIn = createInputBox("ריבית לתקופה ב-% (r - למשל 6)").apply { setText("6") }
    val periodsIn = createInputBox("מספר תקופות לאנונה רגילה (n - למשל 20)").apply { setText("20") }
    val growthIn = createInputBox("שיעור צמיחה לאנונה נצחית צומחת ב-% (g - למשל 2)").apply { setText("2") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun calcAnnuity() {
        curPmt = BidiFmt.evalInputExpr(pmtIn, 5000.0).coerceAtLeast(0.0)
        curRate = (BidiFmt.evalInputExpr(rateIn, 6.0) / 100.0).coerceAtLeast(1e-6)
        curPeriods = BidiFmt.evalInputExpr(periodsIn, 20.0).roundToInt().coerceAtLeast(1)
        val g = (BidiFmt.evalInputExpr(growthIn, 2.0) / 100.0).coerceAtLeast(0.0)

        curPvAnnuity = curPmt * (1.0 - (1.0 + curRate).pow(-curPeriods.toDouble())) / curRate
        val fvOrdinary = curPmt * ((1.0 + curRate).pow(curPeriods.toDouble()) - 1.0) / curRate
        val pvDue = curPvAnnuity * (1.0 + curRate)
        val fvDue = fvOrdinary * (1.0 + curRate)

        curPvPerpetuity = curPmt / curRate
        val pvGrowingPerpetuity = if (curRate > g) curPmt / (curRate - g) else Double.POSITIVE_INFINITY
        annuityCanvas.invalidate()

        val growStr = if (pvGrowingPerpetuity.isInfinite()) "לא מתכנס (g ≥ r)" else BidiFmt.ltr("₪${Utils.formatResult(pvGrowingPerpetuity, 2)}")

        resTv.text = BidiFmt.rtlParagraph(
            "1️⃣ אנונה רגילה (${BidiFmt.ltr("$curPeriods תקופות")}):",
            "• ערך נוכחי (PV): ${BidiFmt.ltr("₪${Utils.formatResult(curPvAnnuity, 2)}")} (תחילת תקופה: ${BidiFmt.ltr("₪${Utils.formatResult(pvDue, 2)}")})",
            "• ערך עתידי (FV): ${BidiFmt.ltr("₪${Utils.formatResult(fvOrdinary, 2)}")} (תחילת תקופה: ${BidiFmt.ltr("₪${Utils.formatResult(fvDue, 2)}")})",
            "",
            "2️⃣ אנונה נצחית (Perpetuity):",
            "• שווי קבועה (${BidiFmt.ltr("PMT / r")}): ${BidiFmt.ltr("₪${Utils.formatResult(curPvPerpetuity, 2)}")}",
            "• שווי צומחת (${BidiFmt.ltr("PMT / (r - g)")}): $growStr"
        )
    }

    BidiFmt.bindLiveInputs(pmtIn, rateIn, periodsIn, growthIn) { calcAnnuity() }
    val calcBtn = createActionBtn("חשב אנונה ואנונה נצחית ♾️", true) { calcAnnuity() }
    calcAnnuity()

    root.addView(annuityCanvas); root.addView(pmtIn); root.addView(rateIn); root.addView(periodsIn); root.addView(growthIn)
    root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 95. מחשבון פרישה וקצבה עם גרף מחזור חיים מתמטי מלא (צבירה + משיכה) (Tab 95)
// =========================================================================
fun MainActivity.createProRetirementPlannerScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🏖️ מחשבון פרישה וקצבה חודשית", "עקומת מחזור חיים אמיתית המחשבת בזמן אמת את שלב הצבירה ושלב משיכת הקצבה עד גיל 90"))

    var curAgeNow = 30.0; var curAgeRet = 67.0; var curSavedNow = 150000.0; var curMonthlyDep = 2500.0; var curAnnualRet = 5.5
    var curNestEgg = 2450000.0; var curMonthlyPension = 12250.0

    val retireCanvas = object : View(this) {
        private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val dashP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(7f, 6f), 0f) }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val pal = getProCanvasColors()

            val padL = 20f * d; val padR = w - 20f * d
            val padT = 26f * d; val padB = h - 30f * d
            val plotW = (padR - padL).coerceAtLeast(10f); val plotH = (padB - padT).coerceAtLeast(10f)

            val endAge = max(90.0, curAgeRet + 15.0)
            val totalSpanYears = (endAge - curAgeNow).coerceAtLeast(2.0)
            val maxY = maxOf(curNestEgg, curSavedNow, 1000.0)

            fun xOfAge(age: Double): Float = (padL + ((age - curAgeNow) / totalSpanYears).coerceIn(0.0, 1.0) * plotW).toFloat()
            fun yOfBal(bal: Double): Float = (padB - (bal / maxY).coerceIn(0.0, 1.0) * plotH).toFloat()

            // שלב 1: צבירה מ-curAgeNow עד curAgeRet
            val rM = (curAnnualRet / 100.0) / 12.0
            val accPath = Path(); val accArea = Path()
            val accSteps = 18
            for (s in 0..accSteps) {
                val age = curAgeNow + (s.toDouble() / accSteps) * (curAgeRet - curAgeNow)
                val m = (age - curAgeNow) * 12.0
                val bal = curSavedNow * (1.0 + rM).pow(m) + if (rM > 1e-9) curMonthlyDep * (((1.0 + rM).pow(m) - 1.0) / rM) else curMonthlyDep * m
                val px = xOfAge(age); val py = yOfBal(bal)
                if (s == 0) {
                    accPath.moveTo(px, py); accArea.moveTo(px, padB); accArea.lineTo(px, py)
                } else {
                    accPath.lineTo(px, py); accArea.lineTo(px, py)
                }
            }
            val retX = xOfAge(curAgeRet); val retY = yOfBal(curNestEgg)
            accArea.lineTo(retX, padB); accArea.close()

            // שלב 2: משיכת קצבה מ-curAgeRet עד endAge (בריבית פנסיונית שמרנית 3.5%)
            val drawPath = Path(); val drawArea = Path()
            drawPath.moveTo(retX, retY); drawArea.moveTo(retX, padB); drawArea.lineTo(retX, retY)
            var balDraw = curNestEgg
            val drawYears = (endAge - curAgeRet).roundToInt().coerceAtLeast(1)
            val postRateM = 0.035 / 12.0
            for (yr in 1..drawYears) {
                for (m in 1..12) {
                    balDraw = (balDraw * (1.0 + postRateM) - curMonthlyPension).coerceAtLeast(0.0)
                }
                val px = xOfAge(curAgeRet + yr); val py = yOfBal(balDraw)
                drawPath.lineTo(px, py); drawArea.lineTo(px, py)
            }
            drawArea.lineTo(padR, padB); drawArea.close()

            fillP.color = Color.argb(55, Color.red(pal.positive), Color.green(pal.positive), Color.blue(pal.positive))
            canvas.drawPath(accArea, fillP)
            fillP.color = Color.argb(50, Color.red(pal.accent2), Color.green(pal.accent2), Color.blue(pal.accent2))
            canvas.drawPath(drawArea, fillP)

            dashP.strokeWidth = 1.4f * d; dashP.color = pal.warning
            canvas.drawLine(retX, padT, retX, padB, dashP)

            lineP.strokeWidth = 2.8f * d; lineP.color = pal.positive
            lineP.applyAuroraGlow(pal.isAurora, pal.positive, d)
            canvas.drawPath(accPath, lineP)

            lineP.color = pal.accent2; lineP.applyAuroraGlow(pal.isAurora, pal.accent2, d)
            canvas.drawPath(drawPath, lineP)

            fillP.color = pal.warning
            canvas.drawCircle(retX, retY, 6f * d, fillP)

            txtP.color = pal.ink
            val topTxt = "\u200Fהון בגיל ${BidiFmt.ltr(curAgeRet.roundToInt())}: ${BidiFmt.ltr("₪${Utils.formatResult(curNestEgg, 0)}")}  ➔  קצבה: ${BidiFmt.ltr("₪${Utils.formatResult(curMonthlyPension, 0)}/חודש")}"
            CanvasUiHelper.drawFittedText(canvas, topTxt, w / 2f, 18f * d, w - 24f * d, 13f * d, 9f * d, txtP)
            txtP.color = pal.muted; txtP.textSize = 10.5f * d
            canvas.drawText("\u200Fגיל ${BidiFmt.ltr(curAgeNow.roundToInt())}  ── צבירה (ירוק) ──  פרישה (${BidiFmt.ltr(curAgeRet.roundToInt())})  ── קצבה ──  גיל ${BidiFmt.ltr(endAge.roundToInt())}", w / 2f, h - 8f * d, txtP)
        }
    }.apply { styleProCanvasCard(this, 190f) }

    val curAgeIn = createInputBox("גיל נוכחי (למשל 30)").apply { setText("30") }
    val retAgeIn = createInputBox("גיל פרישה מתוכנן (למשל 67)").apply { setText("67") }
    val savedNowIn = createInputBox("חיסכון פנסיוני צבור כיום בש״ח (למשל 150000)").apply { setText("150000") }
    val monthlyDepositIn = createInputBox("הפקדה חודשית לפנסיה בש״ח (למשל 2500)").apply { setText("2500") }
    val annualReturnIn = createInputBox("תשואה שנתית ממוצעת נטו ב-% (למשל 5.5)").apply { setText("5.5") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun calcRetirement() {
        curAgeNow = BidiFmt.evalInputExpr(curAgeIn, 30.0).coerceIn(15.0, 95.0)
        curAgeRet = BidiFmt.evalInputExpr(retAgeIn, 67.0).coerceAtLeast(curAgeNow + 1.0)
        curSavedNow = BidiFmt.evalInputExpr(savedNowIn, 150000.0).coerceAtLeast(0.0)
        curMonthlyDep = BidiFmt.evalInputExpr(monthlyDepositIn, 2500.0).coerceAtLeast(0.0)
        curAnnualRet = BidiFmt.evalInputExpr(annualReturnIn, 5.5).coerceAtLeast(0.0)

        val yearsToRetire = curAgeRet - curAgeNow
        val months = yearsToRetire * 12.0
        val rMonth = (curAnnualRet / 100.0) / 12.0

        val fvSaved = curSavedNow * (1.0 + rMonth).pow(months)
        val fvDeposits = if (rMonth > 1e-9) curMonthlyDep * (((1.0 + rMonth).pow(months) - 1.0) / rMonth) else curMonthlyDep * months

        curNestEgg = fvSaved + fvDeposits
        curMonthlyPension = curNestEgg / 200.0
        val safe4PctMonthly = (curNestEgg * 0.04) / 12.0
        retireCanvas.invalidate()

        resTv.text = BidiFmt.rtlParagraph(
            "תחזית חיסכון וקצבת פרישה (בעוד ${BidiFmt.ltr("${Utils.formatResult(yearsToRetire, 1)} שנים")}):",
            "• הון צבור בגיל פרישה (${BidiFmt.ltr(curAgeRet.roundToInt())}): ${BidiFmt.ltr("₪${Utils.formatResult(curNestEgg, 0)}")}",
            "• סך הפקדות קרן: ${BidiFmt.ltr("₪${Utils.formatResult(curSavedNow + curMonthlyDep * months, 0)}")} | רווחי תשואה: ${BidiFmt.ltr("₪${Utils.formatResult(curNestEgg - (curSavedNow + curMonthlyDep * months), 0)}")}",
            "• קצבה חודשית צפויה (מקדם 200): ${BidiFmt.ltr("₪${Utils.formatResult(curMonthlyPension, 0)}")} לחודש",
            "• משיכה חודשית בטוחה לפי כלל 4%: ${BidiFmt.ltr("₪${Utils.formatResult(safe4PctMonthly, 0)}")} לחודש"
        )
    }

    BidiFmt.bindLiveInputs(curAgeIn, retAgeIn, savedNowIn, monthlyDepositIn, annualReturnIn) { calcRetirement() }
    val calcBtn = createActionBtn("חשב חיסכון וקצבת פרישה 🏖️", true) { calcRetirement() }
    calcRetirement()

    root.addView(retireCanvas); root.addView(curAgeIn); root.addView(retAgeIn); root.addView(savedNowIn); root.addView(monthlyDepositIn); root.addView(annualReturnIn)
    root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

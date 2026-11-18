package com.example.calculator

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
// סמלים וקטוריים מותאמים לכל 4 ערכות הנושא עבור הכלים החדשים (Tabs 81..95)
// =========================================================================
object ProFinancialAndMathIcons {
    fun createVectorIcon(
        context: android.content.Context,
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
                    81 -> { // כלי טקסט (Aa)
                        fillP.textSize = r * 1.05f
                        canvas.drawText("Aa", cx, cy + r * 0.25f, fillP)
                        canvas.drawLine(cx - r * 0.65f, cy + r * 0.65f, cx + r * 0.65f, cy + r * 0.65f, strokeP)
                    }
                    82 -> { // ספרות רומיות (XII)
                        canvas.drawRoundRect(RectF(cx - r * 0.78f, cy - r * 0.78f, cx + r * 0.78f, cy + r * 0.78f), r * 0.22f, r * 0.22f, strokeP)
                        canvas.drawLine(cx - r * 0.35f, cy - r * 0.38f, cx + r * 0.35f, cy + r * 0.38f, strokeP)
                        canvas.drawLine(cx + r * 0.35f, cy - r * 0.38f, cx - r * 0.35f, cy + r * 0.38f, strokeP)
                    }
                    83 -> { // סדרת מספרים (123)
                        fillP.textSize = r * 0.92f
                        canvas.drawText("123", cx, cy + r * 0.32f, fillP)
                    }
                    84 -> { // פרופורציות ויחס (21:9)
                        canvas.drawCircle(cx, cy, r * 0.78f, strokeP)
                        canvas.drawLine(cx, cy, cx, cy - r * 0.78f, strokeP)
                        canvas.drawLine(cx, cy, cx + r * 0.68f, cy + r * 0.32f, strokeP)
                    }
                    85 -> { // משוואה לינארית (Y=aX+b)
                        canvas.drawLine(cx - r * 0.75f, cy + r * 0.65f, cx + r * 0.75f, cy + r * 0.65f, strokeP)
                        canvas.drawLine(cx - r * 0.65f, cy - r * 0.75f, cx - r * 0.65f, cy + r * 0.75f, strokeP)
                        canvas.drawLine(cx - r * 0.55f, cy + r * 0.45f, cx + r * 0.65f, cy - r * 0.65f, strokeP)
                    }
                    86 -> { // יעד חיסכון וזמן הכפלה
                        canvas.drawCircle(cx, cy, r * 0.78f, strokeP)
                        canvas.drawCircle(cx, cy, r * 0.38f, strokeP)
                        canvas.drawCircle(cx, cy, r * 0.14f, fillP)
                    }
                    87 -> { // ממיר שכר
                        canvas.drawRoundRect(RectF(cx - r * 0.80f, cy - r * 0.50f, cx + r * 0.80f, cy + r * 0.50f), r * 0.16f, r * 0.16f, strokeP)
                        canvas.drawCircle(cx, cy, r * 0.22f, strokeP)
                        canvas.drawLine(cx - r * 0.45f, cy - r * 0.82f, cx + r * 0.45f, cy - r * 0.82f, strokeP)
                    }
                    88 -> { // הנחות מצטברות ומס שירותים
                        canvas.drawLine(cx - r * 0.60f, cy + r * 0.60f, cx + r * 0.60f, cy - r * 0.60f, strokeP)
                        canvas.drawCircle(cx - r * 0.38f, cy - r * 0.38f, r * 0.18f, strokeP)
                        canvas.drawCircle(cx + r * 0.38f, cy + r * 0.38f, r * 0.18f, strokeP)
                    }
                    89 -> { // תשואת מניות ודיבידנד
                        canvas.drawLine(cx - r * 0.75f, cy + r * 0.45f, cx - r * 0.20f, cy - r * 0.10f, strokeP)
                        canvas.drawLine(cx - r * 0.20f, cy - r * 0.10f, cx + r * 0.18f, cy + r * 0.22f, strokeP)
                        canvas.drawLine(cx + r * 0.18f, cy + r * 0.22f, cx + r * 0.75f, cy - r * 0.55f, strokeP)
                        canvas.drawCircle(cx + r * 0.75f, cy - r * 0.55f, r * 0.13f, fillP)
                    }
                    90 -> { // פיקדון קבוע, תעודת פיקדון וריבית פשוטה
                        val roof = Path().apply {
                            moveTo(cx - r * 0.82f, cy - r * 0.22f)
                            lineTo(cx, cy - r * 0.80f)
                            lineTo(cx + r * 0.82f, cy - r * 0.22f)
                            close()
                        }
                        canvas.drawPath(roof, strokeP)
                        canvas.drawRect(cx - r * 0.58f, cy - r * 0.22f, cx + r * 0.58f, cy + r * 0.68f, strokeP)
                    }
                    91 -> { // מחשבון SIP ואינפלציה
                        canvas.drawArc(RectF(cx - r * 0.78f, cy - r * 0.78f, cx + r * 0.78f, cy + r * 0.78f), -60f, 290f, false, strokeP)
                        canvas.drawLine(cx, cy - r * 0.38f, cx, cy + r * 0.38f, strokeP)
                        canvas.drawLine(cx - r * 0.38f, cy, cx + r * 0.38f, cy, strokeP)
                    }
                    92 -> { // הלוואת רכב ו-E.M.I
                        canvas.drawRoundRect(RectF(cx - r * 0.78f, cy - r * 0.12f, cx + r * 0.78f, cy + r * 0.45f), r * 0.16f, r * 0.16f, strokeP)
                        canvas.drawLine(cx - r * 0.48f, cy - r * 0.12f, cx - r * 0.28f, cy - r * 0.55f, strokeP)
                        canvas.drawLine(cx - r * 0.28f, cy - r * 0.55f, cx + r * 0.28f, cy - r * 0.55f, strokeP)
                        canvas.drawLine(cx + r * 0.28f, cy - r * 0.55f, cx + r * 0.48f, cy - r * 0.12f, strokeP)
                        canvas.drawCircle(cx - r * 0.45f, cy + r * 0.55f, r * 0.16f, fillP)
                        canvas.drawCircle(cx + r * 0.45f, cy + r * 0.55f, r * 0.16f, fillP)
                    }
                    93 -> { // ערך עתידי וערך נוכחי (FV / PV)
                        canvas.drawRoundRect(RectF(cx - r * 0.76f, cy - r * 0.65f, cx + r * 0.76f, cy + r * 0.76f), r * 0.18f, r * 0.18f, strokeP)
                        canvas.drawLine(cx - r * 0.35f, cy + r * 0.15f, cx + r * 0.35f, cy + r * 0.15f, strokeP)
                        canvas.drawLine(cx + r * 0.10f, cy - r * 0.12f, cx + r * 0.35f, cy + r * 0.15f, strokeP)
                        canvas.drawLine(cx + r * 0.10f, cy + r * 0.42f, cx + r * 0.35f, cy + r * 0.15f, strokeP)
                    }
                    94 -> { // אנונה ואנונה נצחית
                        canvas.drawCircle(cx - r * 0.36f, cy, r * 0.36f, strokeP)
                        canvas.drawCircle(cx + r * 0.36f, cy, r * 0.36f, strokeP)
                    }
                    else -> { // 95: מחשבון פרישה וקצבה
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
// 81. כלי טקסט ועיבוד מחרוזות (Text Tools Studio - Tab 81)
// תואם במדויק לסרטון: תיבת עריכה רחבה, מונה תווים/ללא רווחים/מילים/שורות,
// 11 כפתורי המרה וניקוי טקסט, כפתורי העתק / שיתוף / נקה וקנבס ניתוח חי!
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
            "ספירת תווים, מילים ושורות בזמן אמת, שינוי רישיות, ניקוי רווחים, הסרת שורות כפולות ומיון"
        )
    )

    var totalChars = 0
    var noSpaceChars = 0
    var wordCount = 0
    var lineCount = 0
    var lettersCount = 0
    var digitsCount = 0

    val textAnalyticsCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val isMin = isMinimalistMode

            val barLeft = 20f * d; val barRight = w - 20f * d
            val barW = (barRight - barLeft).coerceAtLeast(1f)
            val barTop = h * 0.34f; val barBot = h * 0.56f

            p.style = Paint.Style.FILL
            p.color = if (isMin) Color.argb(30, 44, 44, 44) else Color.argb(40, 255, 255, 255)
            canvas.drawRoundRect(RectF(barLeft, barTop, barRight, barBot), 8f * d, 8f * d, p)

            if (totalChars > 0) {
                val letRatio = (lettersCount.toFloat() / totalChars).coerceIn(0f, 1f)
                val digRatio = (digitsCount.toFloat() / totalChars).coerceIn(0f, 1f - letRatio)
                val spcRatio = ((totalChars - noSpaceChars).toFloat() / totalChars).coerceIn(0f, 1f - letRatio - digRatio)

                var curX = barLeft
                if (letRatio > 0f) {
                    p.color = if (isMin) MinimalistTheme.ACCENT_SAGE_GREEN else primaryColor
                    canvas.drawRoundRect(RectF(curX, barTop, curX + barW * letRatio, barBot), 8f * d, 8f * d, p)
                    curX += barW * letRatio
                }
                if (digRatio > 0f) {
                    p.color = Color.parseColor("#FFB300")
                    canvas.drawRect(curX, barTop, curX + barW * digRatio, barBot, p)
                    curX += barW * digRatio
                }
                if (spcRatio > 0f) {
                    p.color = Color.parseColor("#29B6F6")
                    canvas.drawRect(curX, barTop, curX + barW * spcRatio, barBot, p)
                }
            }

            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
            p.textSize = 13.0f * d
            val topStat = "\u200Fאותיות: ${BidiFmt.ltr(lettersCount)}  |  ספרות: ${BidiFmt.ltr(digitsCount)}  |  רווחים: ${BidiFmt.ltr(totalChars - noSpaceChars)}"
            CanvasUiHelper.drawFittedText(canvas, topStat, w / 2f, 20f * d, w - 24f * d, 13f * d, 9.5f * d, p)

            val readSec = ((wordCount / 200.0) * 60.0).roundToInt()
            val botStat = "\u200Fזמן קריאה משוער: כ-${BidiFmt.ltr("$readSec שניות")} (${BidiFmt.ltr("${totalChars * 2} Bytes")})"
            CanvasUiHelper.drawFittedText(canvas, botStat, w / 2f, h - 10f * d, w - 24f * d, 12.2f * d, 9f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(105f)).apply { bottomMargin = dp(10f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProTextToolsStudioScreen, 18f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProTextToolsStudioScreen, 18f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(18f).toFloat() }
        }
    }

    val textIn = createInputBox("הקלד או הדבק טקסט כאן...").apply {
        tag = "HAS_CUSTOM_LIVE_WATCHER"
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        textDirection = View.TEXT_DIRECTION_RTL
        gravity = Gravity.RIGHT or Gravity.TOP
        minLines = 6
    }

    // שורת המונה החיה בדיוק כמו בסרטון: "0 תווים • 0 ללא רווחים • 0 מילים • 0 שורות"
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

        liveCounterTv.text = "\u200F${BidiFmt.ltr(totalChars)} תווים • ${BidiFmt.ltr(noSpaceChars)} ללא רווחים • ${BidiFmt.ltr(wordCount)} מילים • ${BidiFmt.ltr(lineCount)} שורות"
        textAnalyticsCanvas.invalidate()
    }

    textIn.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { updateTextCounters() }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    // כל 11 כפתורי הפעולה מהסרטון ברצועת גלילה אופקית
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
        "קיצוץ רווחים" to { s ->
            s.lines().joinToString("\n") { line -> line.trim().replace(Regex("[ \\t]+"), " ") }.trim()
        },
        "הסרת כל הרווחים" to { s ->
            s.filter { !it.isWhitespace() }
        },
        "הסרת שורות ריקות" to { s ->
            s.lines().filter { it.isNotBlank() }.joinToString("\n")
        },
        "הסרת שורות כפולות" to { s ->
            s.lines().distinct().joinToString("\n")
        },
        "מיון שורות" to { s ->
            s.lines().sorted().joinToString("\n")
        },
        "היפוך" to { s ->
            s.reversed()
        }
    )

    val chipsScroll = HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(12f) }
    }
    val chipsRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

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
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(4f), dp(2f), dp(4f), dp(2f)) }
            setOnClickListener {
                feedback(it)
                val current = textIn.text?.toString() ?: ""
                val transformed = transformFn(current)
                textIn.setText(transformed)
                textIn.setSelection(textIn.text.length)
            }
        }
        chipsRow.addView(chip)
    }
    chipsScroll.addView(chipsRow)

    // שורת 3 כפתורים תחתונה כמו בסרטון: העתק | שיתוף | נקה
    val bottomActionsRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    val copyBtn = createActionBtn("📋 העתק", true) {
        val txt = textIn.text?.toString() ?: ""
        if (txt.isNotEmpty()) copyToClipboard(txt)
        else Toast.makeText(this, "אין טקסט להעתקה", Toast.LENGTH_SHORT).show()
    }.apply {
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.1f).apply { rightMargin = dp(5f) }
    }

    val shareBtn = createActionBtn("📤 שיתוף", false) {
        val txt = textIn.text?.toString() ?: ""
        if (txt.isNotEmpty()) {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, txt)
            }
            startActivity(Intent.createChooser(sendIntent, "שתף טקסט באמצעות"))
        } else {
            Toast.makeText(this, "הזן טקסט לשיתוף", Toast.LENGTH_SHORT).show()
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(4f); rightMargin = dp(4f) }
    }

    val clearBtn = createActionBtn("🧹 נקה", false) {
        textIn.setText("")
    }.apply {
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f).apply { leftMargin = dp(5f) }
    }

    bottomActionsRow.addView(copyBtn)
    bottomActionsRow.addView(shareBtn)
    bottomActionsRow.addView(clearBtn)

    updateTextCounters()
    root.addView(textAnalyticsCanvas)
    root.addView(textIn)
    root.addView(liveCounterTv)
    root.addView(chipsScroll)
    root.addView(bottomActionsRow)
    scroll.addView(root)
    return scroll
}

// =========================================================================
// 82. ספרות רומיות (Roman Numerals Converter - Tab 82)
// =========================================================================
fun MainActivity.createProRomanNumeralsScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🏛️ ספרות רומיות (Roman Numerals)", "המרה דו-כיוונית חיה בין מספרים עשרוניים לספרות רומיות (I, V, X, L, C, D, M) ופירוק ערכים"))

    var curDecimal = 2026
    var curRoman = "MMXXVI"

    val romanCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val isMin = isMinimalistMode

            val tablet = RectF(20f * d, 14f * d, w - 20f * d, h - 14f * d)
            p.style = Paint.Style.FILL
            p.color = if (isMin) MinimalistTheme.ACCENT_BUTTER_YELLOW else Color.argb(45, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
            canvas.drawRoundRect(tablet, 18f * d, 18f * d, p)

            p.style = Paint.Style.STROKE; p.strokeWidth = 2f * d
            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else primaryColor
            canvas.drawRoundRect(tablet, 18f * d, 18f * d, p)

            p.style = Paint.Style.FILL; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            CanvasUiHelper.drawFittedText(canvas, BidiFmt.ltr(curRoman), w / 2f, h * 0.48f, w - 56f * d, 28f * d, 14f * d, p)

            p.typeface = Typeface.DEFAULT_BOLD; p.textSize = 13.5f * d
            canvas.drawText(BidiFmt.ltr("= $curDecimal"), w / 2f, h * 0.78f, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(145f)).apply { bottomMargin = dp(12f) }
    }

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

    fun toRomanWithBreakdown(num: Int): Pair<String, List<String>> {
        if (num !in 1..3999) return "מחוץ לטווח (1..3999)" to emptyList()
        var rem = num
        val sb = StringBuilder()
        val parts = mutableListOf<String>()
        for ((v, sym) in romanTable) {
            var count = 0
            while (rem >= v) {
                sb.append(sym)
                rem -= v
                count++
            }
            if (count > 0) {
                parts.add("${sym.repeat(count)} (${count * v})")
            }
        }
        return sb.toString() to parts
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
        val (rom, breakdown) = toRomanWithBreakdown(curDecimal)
        curRoman = rom
        romanCanvas.invalidate()
        resTv.text = BidiFmt.rtlParagraph(
            "תוצאת המרת ספרות רומיות:",
            "• מספר עשרוני: ${BidiFmt.ltr(curDecimal)}  ➔  ספרות רומיות: ${BidiFmt.ltr(curRoman)}",
            "• פירוק אבני בניין: ${BidiFmt.ltr(breakdown.joinToString(" + "))}",
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
                val (rom, breakdown) = toRomanWithBreakdown(parsed)
                curRoman = rom
                romanCanvas.invalidate()
                resTv.text = BidiFmt.rtlParagraph(
                    "פענוח ספרות רומיות:",
                    "• ספרות רומיות: ${BidiFmt.ltr(s.toString().uppercase(Locale.US))}  ➔  ערך עשרוני: ${BidiFmt.ltr(parsed)}",
                    "• פירוק תקני: ${BidiFmt.ltr(breakdown.joinToString(" + "))}"
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
// 83. סדרת מספרים חשבונית, הנדסית ופיבונאצ׳י (Number Series - Tab 83)
// =========================================================================
fun MainActivity.createProNumberSeriesScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🔢 סדרת מספרים (Arithmetic, Geometric & Fibonacci)", "חישוב האיבר ה-n, סכום הסדרה (Sn) וגרף עמודות חי של איברי הסדרה"))

    val seriesModes = listOf("סדרה חשבונית (הפרש קבוע d)", "סדרה הנדסית (מנה קבועה q)", "סדרת פיבונאצ׳י (Fn = Fn-1 + Fn-2)")
    var modeIdx = 0
    var previewTerms = listOf(2.0, 5.0, 8.0, 11.0, 14.0, 17.0, 20.0, 23.0)

    val seriesCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val isMin = isMinimalistMode

            val terms = previewTerms.ifEmpty { listOf(1.0) }
            val maxAbs = terms.maxOf { abs(it) }.coerceAtLeast(1.0)
            val leftPad = 20f * d; val rightPad = w - 20f * d
            val bottomY = h - 26f * d; val maxBarH = h * 0.56f
            val slotW = (rightPad - leftPad) / terms.size.coerceAtLeast(1)

            for ((i, v) in terms.withIndex()) {
                val ratio = (abs(v) / maxAbs).toFloat().coerceIn(0.08f, 1f)
                val bh = ratio * maxBarH
                val bx = leftPad + i * slotW + slotW * 0.15f
                val bw = slotW * 0.70f

                p.style = Paint.Style.FILL
                p.color = if (isMin) MinimalistTheme.ACCENT_SAGE_GREEN else primaryColor
                canvas.drawRoundRect(RectF(bx, bottomY - bh, bx + bw, bottomY), 6f * d, 6f * d, p)

                p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
                p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD; p.textSize = 10f * d
                canvas.drawText(BidiFmt.ltr("a${i + 1}"), bx + bw / 2f, bottomY + 14f * d, p)
                canvas.drawText(BidiFmt.ltr(Utils.formatResult(v, 1)), bx + bw / 2f, bottomY - bh - 4f * d, p)
            }
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(175f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProNumberSeriesScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProNumberSeriesScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val modeBtn = createActionBtn("סוג סדרה: ${seriesModes[0]}", true) {}
    val a1In = createInputBox("איבר ראשון (a₁ - למשל 2)").apply { setText("2") }
    val diffIn = createInputBox("הפרש d / מנה q (למשל 3)").apply { setText("3") }
    val nIn = createInputBox("מספר האיברים n (למשל 10)").apply { setText("10") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun updateSeries() {
        val a1 = BidiFmt.evalInputExpr(a1In, 2.0)
        val dq = BidiFmt.evalInputExpr(diffIn, 3.0)
        val n = BidiFmt.evalInputExpr(nIn, 10.0).roundToInt().coerceIn(1, 200)
        diffIn.visibility = if (modeIdx == 2) View.GONE else View.VISIBLE

        val countPreview = min(n, 8)
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
                val infSum = if (abs(dq) < 1.0) "• סכום סדרה הנדסית אינסופית מתכנסת (${BidiFmt.ltr("S∞")}): ${BidiFmt.ltr(Utils.formatResult(a1 / (1.0 - dq), calcPrecision))}" else "• התכנסות: הסדרה אינה מתכנסת (${BidiFmt.ltr("|q| ≥ 1")})"
                resTv.text = BidiFmt.rtlParagraph(
                    "תוצאות סדרה הנדסית (${BidiFmt.ltr("a₁=${Utils.formatResult(a1, 2)}, q=${Utils.formatResult(dq, 2)}")}):",
                    "• האיבר ה-${BidiFmt.ltr(n)} (${BidiFmt.ltr("aₙ")}): ${BidiFmt.ltr(Utils.formatResult(an, calcPrecision))}",
                    "• סכום ${BidiFmt.ltr(n)} האיברים הראשונים (${BidiFmt.ltr("Sₙ")}): ${BidiFmt.ltr(Utils.formatResult(sn, calcPrecision))}",
                    infSum,
                    "• תחילת הסדרה: ${BidiFmt.ltr(previewTerms.joinToString(", ") { Utils.formatResult(it, 2) })}"
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
                previewTerms = fib.take(8)
                val an = fib.lastOrNull() ?: a1
                val sn = fib.sum()
                resTv.text = BidiFmt.rtlParagraph(
                    "תוצאות סדרת פיבונאצ׳י:",
                    "• האיבר ה-${BidiFmt.ltr(fib.size)}: ${BidiFmt.ltr(Utils.formatResult(an, 0))}",
                    "• סכום ${BidiFmt.ltr(fib.size)} האיברים: ${BidiFmt.ltr(Utils.formatResult(sn, 0))}",
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
// 84. פרופורציות ויחס (Proportions & Ratio Solver A:B = C:X - Tab 84)
// =========================================================================
fun MainActivity.createProProportionsAndRatioScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("⚖️ פרופורציות ויחס (Proportions & Ratio)", "פתרון משוואת פרופורציה A : B = C : X, צמצום יחס וחלוקת סכום לפי יחס"))

    var valA = 4.0; var valB = 6.0; var valC = 10.0; var valX = 15.0

    val propCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val isMin = isMinimalistMode

            val sumAB = (abs(valA) + abs(valB)).coerceAtLeast(1.0)
            val ratioA = (abs(valA) / sumAB).toFloat().coerceIn(0.12f, 0.88f)
            val barLeft = 24f * d; val barRight = w - 24f * d; val barW = barRight - barLeft

            // פס עליון A : B
            val splitX = barLeft + barW * ratioA
            p.style = Paint.Style.FILL
            p.color = if (isMin) MinimalistTheme.ACCENT_SAGE_GREEN else primaryColor
            canvas.drawRoundRect(RectF(barLeft, 24f * d, splitX - 3f * d, 58f * d), 10f * d, 10f * d, p)
            p.color = if (isMin) MinimalistTheme.ACCENT_TERRACOTTA else Color.parseColor("#FF9100")
            canvas.drawRoundRect(RectF(splitX + 3f * d, 24f * d, barRight, 58f * d), 10f * d, 10f * d, p)

            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD; p.textSize = 13f * d
            canvas.drawText(BidiFmt.ltr("A = ${Utils.formatResult(valA, 1)}"), (barLeft + splitX) / 2f, 46f * d, p)
            canvas.drawText(BidiFmt.ltr("B = ${Utils.formatResult(valB, 1)}"), (splitX + barRight) / 2f, 46f * d, p)

            val eqLine = "${Utils.formatResult(valA, 2)} : ${Utils.formatResult(valB, 2)}  =  ${Utils.formatResult(valC, 2)} : ${Utils.formatResult(valX, 2)}"
            p.textSize = 15f * d
            CanvasUiHelper.drawFittedText(canvas, BidiFmt.ltr(eqLine), w / 2f, h - 16f * d, w - 32f * d, 15.5f * d, 10f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(115f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProProportionsAndRatioScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProProportionsAndRatioScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

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
        val simpA = valA.roundToLong() / g
        val simpB = valB.roundToLong() / g
        val sumParts = (valA + valB).let { if (abs(it) < 1e-12) 1.0 else it }
        val shareA = totalAmount * (valA / sumParts)
        val shareB = totalAmount * (valB / sumParts)

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
// 85. משוואה לינארית וגרף קו ישר (Linear Equation Solver aX + b = c - Tab 85)
// =========================================================================
fun MainActivity.createProLinearEquationScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("📈 משוואה לינארית וגרף ישר (aX + b = c)", "פתרון משוואה לינארית עם שלבי דרך ושרטוט חי של הפונקציה הקווית Y = aX + (b - c)"))

    var curA = 2.0; var curB = -4.0; var curC = 6.0; var rootX = 5.0

    val linearCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val isMin = isMinimalistMode

            val ox = w * 0.50f; val oy = h * 0.52f
            p.style = Paint.Style.STROKE; p.strokeWidth = 1.4f * d
            p.color = if (isMin) Color.argb(70, 44, 44, 44) else Color.argb(80, 255, 255, 255)
            canvas.drawLine(18f * d, oy, w - 18f * d, oy, p)
            canvas.drawLine(ox, 16f * d, ox, h - 26f * d, p)

            // שרטוט הישר
            val slope = curA.coerceIn(-8.0, 8.0).toFloat()
            val spanX = w * 0.34f
            val x1 = ox - spanX; val y1 = (oy + spanX * slope * 0.22f).coerceIn(16f * d, h - 28f * d)
            val x2 = ox + spanX; val y2 = (oy - spanX * slope * 0.22f).coerceIn(16f * d, h - 28f * d)

            p.strokeWidth = 3f * d
            p.color = if (isMin) MinimalistTheme.ACCENT_TERRACOTTA else primaryColor
            canvas.drawLine(x1, y1, x2, y2, p)

            p.style = Paint.Style.FILL
            p.color = Color.parseColor("#00E676")
            val rxPx = (ox + (rootX.coerceIn(-10.0, 10.0).toFloat() / 10f) * spanX)
            canvas.drawCircle(rxPx, oy, 6f * d, p)

            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD; p.textSize = 13.5f * d
            val eqStr = "${Utils.formatResult(curA, 2)}x + (${Utils.formatResult(curB, 2)}) = ${Utils.formatResult(curC, 2)}   ➔   x = ${Utils.formatResult(rootX, 3)}"
            CanvasUiHelper.drawFittedText(canvas, BidiFmt.ltr(eqStr), w / 2f, h - 8f * d, w - 24f * d, 13.5f * d, 9.5f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(185f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProLinearEquationScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProLinearEquationScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val aIn = createInputBox("מקדם הנעלם a (למשל 2)").apply { setText("2") }
    val bIn = createInputBox("איבר חופשי באגף שמאל b (למשל -4)").apply { setText("-4") }
    val cIn = createInputBox("תוצאה באגף ימין c (למשל 6)").apply { setText("6") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun solveLinear() {
        curA = BidiFmt.evalInputExpr(aIn, 2.0)
        curB = BidiFmt.evalInputExpr(bIn, -4.0)
        curC = BidiFmt.evalInputExpr(cIn, 6.0)

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
            "• העברת אגפים: ${BidiFmt.ltr("${Utils.formatResult(curA, 2)}x = ${Utils.formatResult(curC, 2)} - (${Utils.formatResult(curB, 2)}) = ${Utils.formatResult(rhsDiff, 2)}")}",
            "• חלוקה במקדם (${BidiFmt.ltr(Utils.formatResult(curA, 2))}): ${BidiFmt.ltr("x = ${Utils.formatResult(rootX, calcPrecision)}")}",
            "• שיפוע הישר (m): ${BidiFmt.ltr(Utils.formatResult(curA, 2))} (זווית: ${BidiFmt.ltr("${Utils.formatResult(slopeAngle, 1)}°")}) | חיתוך ציר Y: ${BidiFmt.ltr("(0, ${Utils.formatResult(curB - curC, 2)})")}"
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
// 86. יעד חיסכון וזמן הכפלה (Savings Goal & Doubling Time - Tab 86)
// =========================================================================
fun MainActivity.createProSavingsGoalAndDoublingScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🎯 יעד חיסכון וזמן הכפלה (Savings Goal & Rule of 72)", "חישוב ההפקדה החודשית הנדרשת להשגת יעד כספי וזמן הכפלת ההון"))

    var curInitial = 10000.0; var curTarget = 100000.0; var curMonthlyPmt = 2150.0; var curDoubleYears = 12.0

    val goalCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val isMin = isMinimalistMode

            val cx = w * 0.26f; val cy = h * 0.50f; val r = min(w * 0.18f, h * 0.34f)
            p.style = Paint.Style.STROKE; p.strokeWidth = 8f * d
            p.color = if (isMin) Color.argb(35, 44, 44, 44) else Color.argb(45, 255, 255, 255)
            canvas.drawCircle(cx, cy, r, p)

            val startPct = (curInitial / curTarget.coerceAtLeast(1.0)).toFloat().coerceIn(0.05f, 1f)
            p.color = if (isMin) MinimalistTheme.ACCENT_SAGE_GREEN else Color.parseColor("#00E676")
            p.strokeCap = Paint.Cap.ROUND
            canvas.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -90f, startPct * 360f, false, p)

            p.style = Paint.Style.FILL; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            p.textSize = 13f * d
            canvas.drawText(BidiFmt.ltr("${(startPct * 100).roundToInt()}%"), cx, cy + 5f * d, p)

            val textX = w * 0.65f
            p.textSize = 15f * d
            CanvasUiHelper.drawFittedText(canvas, "\u200Fהפקדה: ${BidiFmt.ltr("₪${Utils.formatResult(curMonthlyPmt, 0)} / חודש")}", textX, h * 0.40f, w * 0.58f, 15f * d, 10f * d, p)
            p.textSize = 13f * d
            CanvasUiHelper.drawFittedText(canvas, "\u200Fזמן הכפלת כסף: ${BidiFmt.ltr("${Utils.formatResult(curDoubleYears, 1)} שנים")}", textX, h * 0.70f, w * 0.58f, 13.5f * d, 9.5f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(145f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProSavingsGoalAndDoublingScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProSavingsGoalAndDoublingScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val targetIn = createInputBox("יעד חיסכון מבוקש בש״ח (למשל 100000)").apply { setText("100000") }
    val initIn = createInputBox("סכום התחלתי קיים בש״ח (למשל 10000)").apply { setText("10000") }
    val yearsIn = createInputBox("טווח שנים להשגת היעד (למשל 3)").apply { setText("3") }
    val rateIn = createInputBox("תשואה שנתית צפויה ב-% (למשל 6)").apply { setText("6") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun calcSavingsGoal() {
        curTarget = BidiFmt.evalInputExpr(targetIn, 100000.0).coerceAtLeast(1.0)
        curInitial = BidiFmt.evalInputExpr(initIn, 10000.0).coerceAtLeast(0.0)
        val years = BidiFmt.evalInputExpr(yearsIn, 3.0).coerceAtLeast(0.1)
        val annualRatePct = BidiFmt.evalInputExpr(rateIn, 6.0).coerceAtLeast(0.0)

        val months = (years * 12.0).coerceAtLeast(1.0)
        val rMonth = (annualRatePct / 100.0) / 12.0
        val fvInitial = curInitial * (1.0 + rMonth).pow(months)
        val remGoal = (curTarget - fvInitial).coerceAtLeast(0.0)

        curMonthlyPmt = if (rMonth > 1e-9) {
            remGoal * rMonth / ((1.0 + rMonth).pow(months) - 1.0)
        } else {
            remGoal / months
        }

        curDoubleYears = if (annualRatePct > 0.01) ln(2.0) / ln(1.0 + annualRatePct / 100.0) else 0.0
        val rule72Years = if (annualRatePct > 0.01) 72.0 / annualRatePct else 0.0
        val totalContributions = curInitial + curMonthlyPmt * months
        val totalInterestEarned = (curTarget - totalContributions).coerceAtLeast(0.0)
        goalCanvas.invalidate()

        resTv.text = BidiFmt.rtlParagraph(
            "תוכנית הגעה ליעד חיסכון (${BidiFmt.ltr("₪${Utils.formatResult(curTarget, 0)}")}):",
            "• הפקדה חודשית נדרשת: ${BidiFmt.ltr("₪${Utils.formatResult(curMonthlyPmt, 2)}")} (למשך ${BidiFmt.ltr("${months.roundToInt()} חודשים")})",
            "• סך קרן שתופקד: ${BidiFmt.ltr("₪${Utils.formatResult(totalContributions, 0)}")} | רווח ריבית: ${BidiFmt.ltr("₪${Utils.formatResult(totalInterestEarned, 0)}")}",
            "",
            "⏳ זמן הכפלת הכסף בריבית ${BidiFmt.ltr("${Utils.formatResult(annualRatePct, 2)}%")} לשנה:",
            "• זמן הכפלה מדויק (לוגריתמי): ${BidiFmt.ltr("${Utils.formatResult(curDoubleYears, 2)} שנים")}",
            "• אומדן מהיר לפי חוק ה-72: ${BidiFmt.ltr("${Utils.formatResult(rule72Years, 2)} שנים")}"
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
// 87. ממיר שכר (שעתי / יומי / שבועי / חודשי / שנתי) (Salary Converter - Tab 87)
// =========================================================================
fun MainActivity.createProSalaryPeriodConverterScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("💼 ממיר שכר (Salary Period Converter)", "המרת שכר בין שעתי, יומי, שבועי, חודשי ושנתי בהתאם להיקף המשרה"))

    val periods = listOf("שכר חודשי", "שכר שעתי", "שכר יומי", "שכר שבועי", "שכר שנתי")
    var periodIdx = 0
    var hourlyVal = 64.5
    var dailyVal = 542.0
    var weeklyVal = 2710.0
    var monthlyVal = 11750.0
    var yearlyVal = 141000.0

    val salaryCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val isMin = isMinimalistMode

            val items = listOf("שעתי" to hourlyVal, "יומי" to dailyVal, "שבועי" to weeklyVal, "חודשי" to monthlyVal)
            val padL = 22f * d; val padR = w - 22f * d
            val slotW = (padR - padL) / 4f
            val baseBot = h - 26f * d

            for ((i, pair) in items.withIndex()) {
                val bh = (20f + i * 22f) * d
                val bx = padL + i * slotW + slotW * 0.14f
                val bw = slotW * 0.72f
                p.style = Paint.Style.FILL
                p.color = if (isMin) MinimalistTheme.ACCENT_PASTEL_BLUE else Color.argb(120 + i * 35, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
                canvas.drawRoundRect(RectF(bx, baseBot - bh, bx + bw, baseBot), 7f * d, 7f * d, p)

                p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
                p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD; p.textSize = 11f * d
                canvas.drawText(pair.first, bx + bw / 2f, baseBot + 14f * d, p)
                canvas.drawText(BidiFmt.ltr("₪${Utils.formatResult(pair.second, 0)}"), bx + bw / 2f, baseBot - bh - 5f * d, p)
            }
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(160f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProSalaryPeriodConverterScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProSalaryPeriodConverterScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val periodBtn = createActionBtn("סוג הקלט: ${periods[0]}", true) {}
    val amountIn = createInputBox("סכום השכר בש״ח (למשל 12000)").apply { setText("12000") }
    val hoursWeekIn = createInputBox("שעות עבודה בשבוע (משרה מלאה = 42)").apply { setText("42") }
    val daysWeekIn = createInputBox("ימי עבודה בשבוע (למשל 5)").apply { setText("5") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun convertSalary() {
        val amt = BidiFmt.evalInputExpr(amountIn, 12000.0).coerceAtLeast(0.0)
        val hrsPerWeek = BidiFmt.evalInputExpr(hoursWeekIn, 42.0).coerceIn(1.0, 120.0)
        val daysPerWeek = BidiFmt.evalInputExpr(daysWeekIn, 5.0).coerceIn(1.0, 7.0)
        val weeksPerMonth = 4.34524

        yearlyVal = when (periodIdx) {
            0 -> amt * 12.0
            1 -> amt * hrsPerWeek * 52.1429
            2 -> amt * daysPerWeek * 52.1429
            3 -> amt * 52.1429
            else -> amt
        }
        monthlyVal = yearlyVal / 12.0
        weeklyVal = yearlyVal / 52.1429
        dailyVal = weeklyVal / daysPerWeek
        hourlyVal = weeklyVal / hrsPerWeek
        salaryCanvas.invalidate()

        resTv.text = BidiFmt.rtlParagraph(
            "פירוט שכר מלא (${BidiFmt.ltr("${Utils.formatResult(hrsPerWeek, 1)} שע׳/שבוע, ${Utils.formatResult(daysPerWeek, 1)} ימים/שבוע")}):",
            "• שכר שעתי: ${BidiFmt.ltr("₪${Utils.formatResult(hourlyVal, 2)}")} לשעה",
            "• שכר יומי: ${BidiFmt.ltr("₪${Utils.formatResult(dailyVal, 2)}")} ליום",
            "• שכר שבועי: ${BidiFmt.ltr("₪${Utils.formatResult(weeklyVal, 2)}")} לשבוע",
            "• שכר חודשי (${BidiFmt.ltr("${Utils.formatResult(hrsPerWeek * weeksPerMonth, 0)} שעות/חודש")}): ${BidiFmt.ltr("₪${Utils.formatResult(monthlyVal, 2)}")}",
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
// 88. הנחות מצטברות ומס שירותים (Stacked Discounts & Service Tax - Tab 88)
// =========================================================================
fun MainActivity.createProStackedDiscountsScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🏷️ הנחות מצטברות ומס שירותים", "חישוב כפל הנחות (למשל 20% + 10% נוספים על היתרה) בתוספת מס שירותים או מע״מ"))

    var origP = 500.0; var afterD1 = 400.0; var afterD2 = 360.0; var finalWithTax = 360.0

    val discCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val isMin = isMinimalistMode

            val maxP = maxOf(origP, finalWithTax, 1.0)
            val steps = listOf("מקורי" to origP, "אחרי הנחה 1" to afterD1, "אחרי הנחה 2" to afterD2, "סופי לתשלום" to finalWithTax)
            val padL = 20f * d; val padR = w - 20f * d; val slotW = (padR - padL) / 4f
            val baseBot = h - 26f * d; val maxH = h * 0.54f

            for ((i, pair) in steps.withIndex()) {
                val ratio = (pair.second / maxP).toFloat().coerceIn(0.10f, 1f)
                val bh = ratio * maxH
                val bx = padL + i * slotW + slotW * 0.14f
                val bw = slotW * 0.72f
                p.style = Paint.Style.FILL
                p.color = when (i) {
                    0 -> Color.parseColor("#90A4AE")
                    1 -> Color.parseColor("#29B6F6")
                    2 -> Color.parseColor("#00E676")
                    else -> if (isMin) MinimalistTheme.ACCENT_TERRACOTTA else primaryColor
                }
                canvas.drawRoundRect(RectF(bx, baseBot - bh, bx + bw, baseBot), 7f * d, 7f * d, p)

                p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
                p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD; p.textSize = 10f * d
                canvas.drawText(pair.first, bx + bw / 2f, baseBot + 14f * d, p)
                canvas.drawText(BidiFmt.ltr("₪${Utils.formatResult(pair.second, 0)}"), bx + bw / 2f, baseBot - bh - 5f * d, p)
            }
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(165f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProStackedDiscountsScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProStackedDiscountsScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val priceIn = createInputBox("מחיר מקורי בש״ח (למשל 500)").apply { setText("500") }
    val d1In = createInputBox("הנחה ראשונה ב-% (למשל 20)").apply { setText("20") }
    val d2In = createInputBox("הנחה שנייה נוספת ב-% (למשל 10)").apply { setText("10") }
    val serviceTaxIn = createInputBox("מס שירותים / דמי שירות ב-% (אופציונלי, למשל 0)").apply { setText("0") }
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
            "• אחוז הנחה משוקלל אמיתי: ${BidiFmt.ltr("${Utils.formatResult(effectiveDiscPct, 2)}%")} (ולא ${BidiFmt.ltr("${Utils.formatResult(d1 + d2, 1)}%")})",
            "• סך חיסכון מהמחיר המקורי: ${BidiFmt.ltr("₪${Utils.formatResult(totalSaved, 2)}")}",
            if (srvTax > 0) "• תוספת מס שירותים (${BidiFmt.ltr("${Utils.formatResult(srvTax, 1)}%")}): ${BidiFmt.ltr("₪${Utils.formatResult(taxAmount, 2)}")}  ➔  לתשלום: ${BidiFmt.ltr("₪${Utils.formatResult(finalWithTax, 2)}")}" else "• סופי לתשלום: ${BidiFmt.ltr("₪${Utils.formatResult(finalWithTax, 2)}")}"
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
// 89. מחשבון תשואת מניות ודיבידנד (Stock Return & Dividend Yield - Tab 89)
// =========================================================================
fun MainActivity.createProStockAndDividendScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("📈 תשואת מניות ודיבידנד (Stocks & Dividend Yield)", "חישוב רווח הון במניות, תשואת דיבידנד שנתית, מס רווחי הון (25%) ותשואה כוללת"))

    var curBuyTotal = 10000.0; var curCapGain = 2500.0; var curDivTotal = 450.0; var curTax = 737.5

    val stockCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val isMin = isMinimalistMode

            val netProfit = curCapGain + curDivTotal - curTax
            val roiPct = if (curBuyTotal > 0) (netProfit / curBuyTotal) * 100.0 else 0.0
            val isPositive = netProfit >= 0

            p.style = Paint.Style.FILL; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
            p.color = if (isPositive) Color.parseColor("#00E676") else Color.parseColor("#FF5252")
            p.textSize = 20f * d
            val sign = if (isPositive) "+" else ""
            canvas.drawText(BidiFmt.ltr("${sign}₪${Utils.formatResult(netProfit, 1)} (${sign}${Utils.formatResult(roiPct, 2)}%)"), w / 2f, h * 0.44f, p)

            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            p.textSize = 12.5f * d
            val sub = "\u200Fרווח הון: ${BidiFmt.ltr("₪${Utils.formatResult(curCapGain, 0)}")}  |  דיבידנד: ${BidiFmt.ltr("₪${Utils.formatResult(curDivTotal, 0)}")}  |  מס (25%): ${BidiFmt.ltr("₪${Utils.formatResult(curTax, 0)}")}"
            CanvasUiHelper.drawFittedText(canvas, sub, w / 2f, h * 0.78f, w - 24f * d, 12.5f * d, 9f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(130f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProStockAndDividendScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProStockAndDividendScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

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
        val sellTotal = sellP * shares
        curCapGain = sellTotal - curBuyTotal
        curDivTotal = divPerShare * shares

        val divYieldOnCost = (divPerShare / buyP) * 100.0
        val divYieldCurrent = if (sellP > 0) (divPerShare / sellP) * 100.0 else 0.0
        val grossProfit = curCapGain + curDivTotal
        curTax = if (grossProfit > 0) grossProfit * 0.25 else 0.0
        val netProfit = grossProfit - curTax
        stockCanvas.invalidate()

        resTv.text = BidiFmt.rtlParagraph(
            "סיכום השקעה במניות ודיבידנדים:",
            "• שווי קנייה כולל: ${BidiFmt.ltr("₪${Utils.formatResult(curBuyTotal, 2)}")}  ➔  שווי מכירה: ${BidiFmt.ltr("₪${Utils.formatResult(sellTotal, 2)}")}",
            "• רווח הון ברוטו: ${BidiFmt.ltr("₪${Utils.formatResult(curCapGain, 2)}")} (${BidiFmt.ltr("${Utils.formatResult(((sellP - buyP) / buyP) * 100, 2)}%")})",
            "• הכנסה מדיבידנדים: ${BidiFmt.ltr("₪${Utils.formatResult(curDivTotal, 2)}")} לשנה (${BidiFmt.ltr("₪${Utils.formatResult(curDivTotal / 4.0, 2)}")} לרבעון)",
            "• תשואת דיבידנד (Dividend Yield): נוכחית ${BidiFmt.ltr("${Utils.formatResult(divYieldCurrent, 2)}%")} | על העלות ${BidiFmt.ltr("${Utils.formatResult(divYieldOnCost, 2)}%")}",
            "• רווח נטו לאחר מס רווחי הון (25%): ${BidiFmt.ltr("₪${Utils.formatResult(netProfit, 2)}")}"
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
// 90. פיקדון קבוע, תעודת פיקדון וריבית פשוטה (FD / CD & Simple Interest - Tab 90)
// =========================================================================
fun MainActivity.createProFixedDepositAndSimpleInterestScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🏦 פיקדון קבוע, תעודת פיקדון וריבית פשוטה", "השוואה חיה בין ריבית פשוטה לפיקדון בריבית דריבית (FD/CD) כולל ניכוי מס פיקדונות (15%)"))

    var curPrincipal = 50000.0; var curSimpleInterest = 7500.0; var curCompoundInterest = 8052.55

    val fdCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val isMin = isMinimalistMode

            val maxVal = maxOf(curPrincipal + curCompoundInterest, 1.0)
            val cols = listOf("קרן פיקדון" to curPrincipal, "ריבית פשוטה" to (curPrincipal + curSimpleInterest), "פיקדון דריבית" to (curPrincipal + curCompoundInterest))
            val padL = 24f * d; val padR = w - 24f * d; val slotW = (padR - padL) / 3f
            val baseBot = h - 26f * d; val maxH = h * 0.54f

            for ((i, pair) in cols.withIndex()) {
                val bh = ((pair.second / maxVal).toFloat().coerceIn(0.15f, 1f)) * maxH
                val bx = padL + i * slotW + slotW * 0.16f
                val bw = slotW * 0.68f
                p.style = Paint.Style.FILL
                p.color = when (i) {
                    0 -> Color.parseColor("#90A4AE")
                    1 -> Color.parseColor("#29B6F6")
                    else -> Color.parseColor("#00E676")
                }
                canvas.drawRoundRect(RectF(bx, baseBot - bh, bx + bw, baseBot), 8f * d, 8f * d, p)

                p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
                p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD; p.textSize = 11f * d
                canvas.drawText(pair.first, bx + bw / 2f, baseBot + 14f * d, p)
                canvas.drawText(BidiFmt.ltr("₪${Utils.formatResult(pair.second, 0)}"), bx + bw / 2f, baseBot - bh - 5f * d, p)
            }
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(165f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProFixedDepositAndSimpleInterestScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProFixedDepositAndSimpleInterestScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val principalIn = createInputBox("סכום הפקדה לפיקדון בש״ח (למשל 50000)").apply { setText("50000") }
    val rateIn = createInputBox("ריבית שנתית ב-% (למשל 5)").apply { setText("5") }
    val yearsIn = createInputBox("תקופת הפיקדון בשנים (למשל 3)").apply { setText("3") }
    val taxRateIn = createInputBox("מס רווחי פיקדון ב-% (נומינלי = 15%, ריאלי = 25%)").apply { setText("15") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun calcDeposits() {
        curPrincipal = BidiFmt.evalInputExpr(principalIn, 50000.0).coerceAtLeast(0.0)
        val rPct = BidiFmt.evalInputExpr(rateIn, 5.0).coerceAtLeast(0.0)
        val yrs = BidiFmt.evalInputExpr(yearsIn, 3.0).coerceAtLeast(0.0)
        val taxPct = BidiFmt.evalInputExpr(taxRateIn, 15.0).coerceIn(0.0, 100.0)

        val r = rPct / 100.0
        curSimpleInterest = curPrincipal * r * yrs
        val simpleNet = curSimpleInterest * (1.0 - taxPct / 100.0)

        val compoundTotalMonthly = curPrincipal * (1.0 + r / 12.0).pow(yrs * 12.0)
        curCompoundInterest = compoundTotalMonthly - curPrincipal
        val compoundNet = curCompoundInterest * (1.0 - taxPct / 100.0)
        fdCanvas.invalidate()

        resTv.text = BidiFmt.rtlParagraph(
            "1️⃣ ריבית פשוטה (Simple Interest - ללא צבירה):",
            "• רווח ריבית ברוטו: ${BidiFmt.ltr("₪${Utils.formatResult(curSimpleInterest, 2)}")} | נטו לאחר מס (${BidiFmt.ltr("${Utils.formatResult(taxPct, 0)}%")}): ${BidiFmt.ltr("₪${Utils.formatResult(simpleNet, 2)}")}",
            "• סכום סופי נטו בחשבון: ${BidiFmt.ltr("₪${Utils.formatResult(curPrincipal + simpleNet, 2)}")}",
            "",
            "2️⃣ פיקדון קבוע / תעודת פיקדון (FD / CD צבירה חודשית):",
            "• רווח ריבית ברוטו: ${BidiFmt.ltr("₪${Utils.formatResult(curCompoundInterest, 2)}")} | נטו לאחר מס: ${BidiFmt.ltr("₪${Utils.formatResult(compoundNet, 2)}")}",
            "• סכום סופי נטו בפדיון: ${BidiFmt.ltr("₪${Utils.formatResult(curPrincipal + compoundNet, 2)}")} (יתרון דריבית: ${BidiFmt.ltr("+₪${Utils.formatResult(compoundNet - simpleNet, 2)}")})"
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
// 91. מחשבון SIP ואינפלציה (SIP Investment & Inflation Erosion - Tab 91)
// =========================================================================
fun MainActivity.createProSipAndInflationScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🌱 מחשבון SIP ושחיקת אינפלציה", "תוכנית השקעה חודשית קבועה (Systematic Investment Plan) וחישוב כוח הקנייה הריאלי בניכוי אינפלציה"))

    var curInvested = 120000.0; var curSipFuture = 232000.0; var curRealPurchasingPower = 180000.0

    val sipCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val isMin = isMinimalistMode

            val barL = 22f * d; val barR = w - 22f * d; val barW = (barR - barL).coerceAtLeast(1f)
            val invRatio = (curInvested / curSipFuture.coerceAtLeast(1.0)).toFloat().coerceIn(0.10f, 0.92f)

            p.style = Paint.Style.FILL
            p.color = Color.parseColor("#29B6F6")
            canvas.drawRoundRect(RectF(barL, 26f * d, barL + barW * invRatio, 54f * d), 10f * d, 10f * d, p)
            p.color = Color.parseColor("#00E676")
            canvas.drawRoundRect(RectF(barL + barW * invRatio, 26f * d, barR, 54f * d), 10f * d, 10f * d, p)

            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD; p.textSize = 13.5f * d
            val topStr = "\u200Fנומינלי: ${BidiFmt.ltr("₪${Utils.formatResult(curSipFuture, 0)}")}  |  כוח קנייה ריאלי: ${BidiFmt.ltr("₪${Utils.formatResult(curRealPurchasingPower, 0)}")}"
            CanvasUiHelper.drawFittedText(canvas, topStr, w / 2f, h - 14f * d, w - 24f * d, 13.5f * d, 9.5f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(105f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProSipAndInflationScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProSipAndInflationScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val monthlyIn = createInputBox("הפקדה חודשית קבועה ב-SIP בש״ח (למשל 1000)").apply { setText("1000") }
    val yearsIn = createInputBox("תקופת השקעה בשנים (למשל 10)").apply { setText("10") }
    val returnIn = createInputBox("תשואה שנתית צפויה ב-% (למשל 12)").apply { setText("12") }
    val inflationIn = createInputBox("שיעור אינפלציה שנתי ממוצע ב-% (למשל 2.5)").apply { setText("2.5") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun calcSipAndInflation() {
        val pmt = BidiFmt.evalInputExpr(monthlyIn, 1000.0).coerceAtLeast(0.0)
        val yrs = BidiFmt.evalInputExpr(yearsIn, 10.0).coerceAtLeast(0.1)
        val retPct = BidiFmt.evalInputExpr(returnIn, 12.0).coerceAtLeast(0.0)
        val infPct = BidiFmt.evalInputExpr(inflationIn, 2.5).coerceAtLeast(0.0)

        val n = yrs * 12.0
        val i = (retPct / 100.0) / 12.0
        curInvested = pmt * n
        curSipFuture = if (i > 1e-9) {
            pmt * (((1.0 + i).pow(n) - 1.0) / i) * (1.0 + i)
        } else {
            curInvested
        }

        val inflationFactor = (1.0 + infPct / 100.0).pow(yrs)
        curRealPurchasingPower = curSipFuture / inflationFactor
        val profitNominal = curSipFuture - curInvested
        sipCanvas.invalidate()

        resTv.text = BidiFmt.rtlParagraph(
            "תוצאות תוכנית השקעה שיטתית (SIP):",
            "• סך הפקדות מהכיס (${BidiFmt.ltr("${n.roundToInt()} חודשים")}): ${BidiFmt.ltr("₪${Utils.formatResult(curInvested, 0)}")}",
            "• רווח הון מצטבר: ${BidiFmt.ltr("₪${Utils.formatResult(profitNominal, 0)}")}",
            "• שווי תיק עתידי נומינלי: ${BidiFmt.ltr("₪${Utils.formatResult(curSipFuture, 0)}")}",
            "",
            "📉 השפעת האינפלציה (${BidiFmt.ltr("${Utils.formatResult(infPct, 1)}%")} לשנה למשך ${BidiFmt.ltr("${Utils.formatResult(yrs, 1)} שנים")}):",
            "• שווי התיק במונחי כוח קנייה של היום: ${BidiFmt.ltr("₪${Utils.formatResult(curRealPurchasingPower, 0)}")}",
            "• שחיקת אינפלציה מצטברת: ${BidiFmt.ltr("${Utils.formatResult((1.0 - 1.0 / inflationFactor) * 100, 1)}%")}"
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
// 92. הלוואת רכב ו-E.M.I (Car Loan & EMI Breakdown - Tab 92)
// =========================================================================
fun MainActivity.createProCarLoanAndEmiScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🚗 הלוואת רכב ו-E.M.I (Auto Financing & EMI)", "חישוב החזר חודשי קבוע (EMI), מקדמה, הלוואת בלון ופילוח קרן מול ריבית"))

    var curEmi = 1850.0; var curNetLoan = 90000.0; var curTotalInterest = 14800.0

    val emiCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val isMin = isMinimalistMode

            val totalPay = (curNetLoan + curTotalInterest).coerceAtLeast(1.0)
            val principalRatio = (curNetLoan / totalPay).toFloat().coerceIn(0.10f, 0.95f)
            val barL = 22f * d; val barR = w - 22f * d; val barW = barR - barL

            p.style = Paint.Style.FILL
            p.color = Color.parseColor("#00E676")
            canvas.drawRoundRect(RectF(barL, 22f * d, barL + barW * principalRatio, 48f * d), 10f * d, 10f * d, p)
            p.color = Color.parseColor("#FF5252")
            canvas.drawRoundRect(RectF(barL + barW * principalRatio, 22f * d, barR, 48f * d), 10f * d, 10f * d, p)

            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD; p.textSize = 15.5f * d
            val mainTxt = "\u200Fהחזר חודשי (EMI): ${BidiFmt.ltr("₪${Utils.formatResult(curEmi, 0)}")}"
            CanvasUiHelper.drawFittedText(canvas, mainTxt, w / 2f, h * 0.68f, w - 24f * d, 15.5f * d, 10f * d, p)

            p.textSize = 12f * d
            val subTxt = "\u200Fקרן (ירוק): ${BidiFmt.ltr("${(principalRatio * 100).roundToInt()}%")}  |  ריבית (אדום): ${BidiFmt.ltr("${((1f - principalRatio) * 100).roundToInt()}%")}"
            canvas.drawText(subTxt, w / 2f, h - 10f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(130f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProCarLoanAndEmiScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProCarLoanAndEmiScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val carPriceIn = createInputBox("מחיר הרכב / סכום העסקה בש״ח (למשל 130000)").apply { setText("130000") }
    val downPaymentIn = createInputBox("מקדמה + טרייד-אין בש״ח (למשל 40000)").apply { setText("40000") }
    val monthsIn = createInputBox("תקופת הלוואה בחודשים (למשל 60)").apply { setText("60") }
    val rateIn = createInputBox("ריבית שנתית ב-% (למשל 6.5)").apply { setText("6.5") }
    val balloonIn = createInputBox("יתרת בלון בסוף תקופה בש״ח (0 אם אין)").apply { setText("0") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun calcCarEmi() {
        val price = BidiFmt.evalInputExpr(carPriceIn, 130000.0).coerceAtLeast(0.0)
        val down = BidiFmt.evalInputExpr(downPaymentIn, 40000.0).coerceAtLeast(0.0)
        val months = BidiFmt.evalInputExpr(monthsIn, 60.0).coerceAtLeast(1.0)
        val rateYr = BidiFmt.evalInputExpr(rateIn, 6.5).coerceAtLeast(0.0)
        val balloon = BidiFmt.evalInputExpr(balloonIn, 0.0).coerceAtLeast(0.0)

        curNetLoan = (price - down).coerceAtLeast(0.0)
        val r = (rateYr / 100.0) / 12.0
        val amortizedPrincipal = (curNetLoan - balloon / (1.0 + r).pow(months)).coerceAtLeast(0.0)

        curEmi = if (r > 1e-9) {
            amortizedPrincipal * r * (1.0 + r).pow(months) / ((1.0 + r).pow(months) - 1.0)
        } else {
            (curNetLoan - balloon) / months
        }

        val totalPaidOnLoan = curEmi * months + balloon
        curTotalInterest = (totalPaidOnLoan - curNetLoan).coerceAtLeast(0.0)
        emiCanvas.invalidate()

        resTv.text = BidiFmt.rtlParagraph(
            "סיכום מימון רכב והחזר E.M.I:",
            "• סכום הלוואה נטו למימון: ${BidiFmt.ltr("₪${Utils.formatResult(curNetLoan, 0)}")}",
            "• החזר חודשי קבוע (E.M.I): ${BidiFmt.ltr("₪${Utils.formatResult(curEmi, 2)}")}",
            if (balloon > 0) "• תשלום בלון בסוף התקופה: ${BidiFmt.ltr("₪${Utils.formatResult(balloon, 0)}")}" else "• סוג סילוקין: שפיצר מלא ללא בלון",
            "• סך ריבית שתשולם לבנק/מימון: ${BidiFmt.ltr("₪${Utils.formatResult(curTotalInterest, 0)}")}",
            "• עלות כוללת של הרכב (מקדמה + החזרים): ${BidiFmt.ltr("₪${Utils.formatResult(down + totalPaidOnLoan, 0)}")}"
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
// 93. ערך עתידי וערך נוכחי (Future Value FV & Present Value PV - Tab 93)
// =========================================================================
fun MainActivity.createProFutureAndPresentValueScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("⏳ ערך עתידי וערך נוכחי (FV & PV)", "היוון סכום עתידי לערכו הנוכחי היום (PV) וחישוב ערך עתידי (FV) לפי שיעור היוון"))

    var curBaseAmount = 100000.0; var curFv = 140255.0; var curPv = 71298.0

    val fvPvCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val isMin = isMinimalistMode

            val cy = h * 0.45f
            p.style = Paint.Style.STROKE; p.strokeWidth = 2.5f * d
            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else primaryColor
            canvas.drawLine(36f * d, cy, w - 36f * d, cy, p)

            val xs = listOf(w * 0.18f, w * 0.50f, w * 0.82f)
            val labels = listOf("PV (היוון)" to curPv, "סכום בסיס" to curBaseAmount, "FV (עתידי)" to curFv)
            for ((i, pair) in labels.withIndex()) {
                p.style = Paint.Style.FILL
                p.color = when (i) {
                    0 -> Color.parseColor("#29B6F6")
                    1 -> Color.parseColor("#FFB300")
                    else -> Color.parseColor("#00E676")
                }
                canvas.drawCircle(xs[i], cy, 9f * d, p)
                p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
                p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD; p.textSize = 11.5f * d
                canvas.drawText(pair.first, xs[i], cy - 16f * d, p)
                canvas.drawText(BidiFmt.ltr("₪${Utils.formatResult(pair.second, 0)}"), xs[i], cy + 26f * d, p)
            }
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(125f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProFutureAndPresentValueScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProFutureAndPresentValueScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val amountIn = createInputBox("סכום כספי לחישוב בש״ח (למשל 100000)").apply { setText("100000") }
    val rateIn = createInputBox("ריבית / שיעור היוון שנתי ב-% (למשל 7)").apply { setText("7") }
    val yearsIn = createInputBox("מספר שנים (n - למשל 5)").apply { setText("5") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun calcFvPv() {
        curBaseAmount = BidiFmt.evalInputExpr(amountIn, 100000.0).coerceAtLeast(0.0)
        val r = BidiFmt.evalInputExpr(rateIn, 7.0) / 100.0
        val n = BidiFmt.evalInputExpr(yearsIn, 5.0).coerceAtLeast(0.0)

        val factor = (1.0 + r).pow(n)
        curFv = curBaseAmount * factor
        curPv = if (factor > 1e-9) curBaseAmount / factor else 0.0
        fvPvCanvas.invalidate()

        resTv.text = BidiFmt.rtlParagraph(
            "ערך הזמן של הכסף (${BidiFmt.ltr("r = ${Utils.formatResult(r * 100, 2)}%, n = ${Utils.formatResult(n, 1)} שנים")}):",
            "• ערך עתידי (${BidiFmt.ltr("FV = PV × (1+r)ⁿ")}): אם תפקיד היום ${BidiFmt.ltr("₪${Utils.formatResult(curBaseAmount, 0)}")}, בעוד ${BidiFmt.ltr(Utils.formatResult(n, 1))} שנים הם יהיו שווים ${BidiFmt.ltr("₪${Utils.formatResult(curFv, 2)}")}",
            "• ערך נוכחי מהוון (${BidiFmt.ltr("PV = FV / (1+r)ⁿ")}): סכום של ${BidiFmt.ltr("₪${Utils.formatResult(curBaseAmount, 0)}")} שיתקבל בעוד ${BidiFmt.ltr(Utils.formatResult(n, 1))} שנים שווה היום ${BidiFmt.ltr("₪${Utils.formatResult(curPv, 2)}")}",
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
// 94. אנונה ואנונה נצחית (Annuity & Perpetuity Calculator - Tab 94)
// =========================================================================
fun MainActivity.createProAnnuityAndPerpetuityScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("♾️ אנונה ואנונה נצחית (Annuity & Perpetuity)", "חישוב ערך נוכחי ועתידי של סדרת תשלומים קבועה (אנונה) ושווי אנונה נצחית קבועה או צומחת"))

    val pmtIn = createInputBox("תשלום תקופתי קבוע בש״ח (PMT - למשל 5000)").apply { setText("5000") }
    val rateIn = createInputBox("ריבית לתקופה ב-% (r - למשל 6)").apply { setText("6") }
    val periodsIn = createInputBox("מספר תקופות לאנונה רגילה (n - למשל 20)").apply { setText("20") }
    val growthIn = createInputBox("שיעור צמיחה לאנונה נצחית צומחת ב-% (g - למשל 2)").apply { setText("2") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun calcAnnuity() {
        val pmt = BidiFmt.evalInputExpr(pmtIn, 5000.0).coerceAtLeast(0.0)
        val r = (BidiFmt.evalInputExpr(rateIn, 6.0) / 100.0).coerceAtLeast(1e-6)
        val n = BidiFmt.evalInputExpr(periodsIn, 20.0).coerceAtLeast(1.0)
        val g = (BidiFmt.evalInputExpr(growthIn, 2.0) / 100.0).coerceAtLeast(0.0)

        // אנונה רגילה (סוף תקופה) ואנונה מיידית (תחילת תקופה)
        val pvOrdinary = pmt * (1.0 - (1.0 + r).pow(-n)) / r
        val fvOrdinary = pmt * ((1.0 + r).pow(n) - 1.0) / r
        val pvDue = pvOrdinary * (1.0 + r)
        val fvDue = fvOrdinary * (1.0 + r)

        // אנונה נצחית (Perpetuity)
        val pvPerpetuity = pmt / r
        val pvGrowingPerpetuity = if (r > g) pmt / (r - g) else Double.POSITIVE_INFINITY

        val growStr = if (pvGrowingPerpetuity.isInfinite()) {
            "לא מתכנס (שיעור הצמיחה g גבוה או שווה לריבית r)"
        } else {
            BidiFmt.ltr("₪${Utils.formatResult(pvGrowingPerpetuity, 2)}")
        }

        resTv.text = BidiFmt.rtlParagraph(
            "1️⃣ אנונה רגילה (${BidiFmt.ltr("${n.roundToInt()} תקופות")}):",
            "• ערך נוכחי של האנונה (PV): ${BidiFmt.ltr("₪${Utils.formatResult(pvOrdinary, 2)}")} (בתחילת תקופה: ${BidiFmt.ltr("₪${Utils.formatResult(pvDue, 2)}")})",
            "• ערך עתידי של האנונה (FV): ${BidiFmt.ltr("₪${Utils.formatResult(fvOrdinary, 2)}")} (בתחילת תקופה: ${BidiFmt.ltr("₪${Utils.formatResult(fvDue, 2)}")})",
            "",
            "2️⃣ אנונה נצחית (Perpetuity - תשלום אינסופי):",
            "• שווי אנונה נצחית קבועה (${BidiFmt.ltr("PV = PMT / r")}): ${BidiFmt.ltr("₪${Utils.formatResult(pvPerpetuity, 2)}")}",
            "• שווי אנונה נצחית צומחת (${BidiFmt.ltr("PV = PMT / (r - g)")}): $growStr"
        )
    }

    BidiFmt.bindLiveInputs(pmtIn, rateIn, periodsIn, growthIn) { calcAnnuity() }
    val calcBtn = createActionBtn("חשב אנונה ואנונה נצחית ♾️", true) { calcAnnuity() }
    calcAnnuity()

    root.addView(pmtIn); root.addView(rateIn); root.addView(periodsIn); root.addView(growthIn)
    root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 95. מחשבון פרישה וקצבה (Retirement & Pension Nest-Egg Planner - Tab 95)
// =========================================================================
fun MainActivity.createProRetirementPlannerScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🏖️ מחשבון פרישה וקצבה חודשית", "סימולטור צבירת הון לגיל פרישה וחישוב הקצבה החודשית הצפויה"))

    var curNestEgg = 2450000.0; var curMonthlyPension = 12250.0

    val retireCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            val isMin = isMinimalistMode

            val path = Path().apply {
                moveTo(20f * d, h - 24f * d)
                quadTo(w * 0.35f, h * 0.72f, w * 0.65f, 24f * d)
                lineTo(w - 20f * d, h * 0.58f)
            }
            p.style = Paint.Style.STROKE; p.strokeWidth = 3f * d
            p.color = if (isMin) MinimalistTheme.ACCENT_SAGE_GREEN else Color.parseColor("#00E676")
            canvas.drawPath(path, p)

            p.style = Paint.Style.FILL; p.color = Color.parseColor("#FFB300")
            canvas.drawCircle(w * 0.65f, 24f * d, 6.5f * d, p)

            p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD; p.textSize = 13.5f * d
            val caption = "\u200Fהון בפרישה: ${BidiFmt.ltr("₪${Utils.formatResult(curNestEgg, 0)}")}  ➔  קצבה: ${BidiFmt.ltr("₪${Utils.formatResult(curMonthlyPension, 0)} / חודש")}"
            CanvasUiHelper.drawFittedText(canvas, caption, w / 2f, h - 6f * d, w - 24f * d, 13.5f * d, 9.5f * d, p)
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(135f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProRetirementPlannerScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProRetirementPlannerScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val curAgeIn = createInputBox("גיל נוכחי (למשל 30)").apply { setText("30") }
    val retAgeIn = createInputBox("גיל פרישה מתוכנן (למשל 67)").apply { setText("67") }
    val savedNowIn = createInputBox("חיסכון פנסיוני צבור כיום בש״ח (למשל 150000)").apply { setText("150000") }
    val monthlyDepositIn = createInputBox("הפקדה חודשית לפנסיה בש״ח (למשל 2500)").apply { setText("2500") }
    val annualReturnIn = createInputBox("תשואה שנתית ממוצעת נטו ב-% (למשל 5.5)").apply { setText("5.5") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView())

    fun calcRetirement() {
        val ageNow = BidiFmt.evalInputExpr(curAgeIn, 30.0).coerceIn(15.0, 95.0)
        val ageRet = BidiFmt.evalInputExpr(retAgeIn, 67.0).coerceAtLeast(ageNow + 1.0)
        val savedNow = BidiFmt.evalInputExpr(savedNowIn, 150000.0).coerceAtLeast(0.0)
        val monthlyDep = BidiFmt.evalInputExpr(monthlyDepositIn, 2500.0).coerceAtLeast(0.0)
        val annualRet = BidiFmt.evalInputExpr(annualReturnIn, 5.5).coerceAtLeast(0.0)

        val yearsToRetire = ageRet - ageNow
        val months = yearsToRetire * 12.0
        val rMonth = (annualRet / 100.0) / 12.0

        val fvSaved = savedNow * (1.0 + rMonth).pow(months)
        val fvDeposits = if (rMonth > 1e-9) {
            monthlyDep * (((1.0 + rMonth).pow(months) - 1.0) / rMonth)
        } else {
            monthlyDep * months
        }

        curNestEgg = fvSaved + fvDeposits
        // חישוב קצבה לפי מקדם המרה פנסיוני סטנדרטי (200) ולפי כלל 4% שנתי
        curMonthlyPension = curNestEgg / 200.0
        val safe4PctMonthly = (curNestEgg * 0.04) / 12.0
        retireCanvas.invalidate()

        resTv.text = BidiFmt.rtlParagraph(
            "תחזית חיסכון וקצבת פרישה (בעוד ${BidiFmt.ltr("${Utils.formatResult(yearsToRetire, 1)} שנים")}):",
            "• סך הון צבור בגיל פרישה (${BidiFmt.ltr(Utils.formatResult(ageRet, 0))}): ${BidiFmt.ltr("₪${Utils.formatResult(curNestEgg, 0)}")}",
            "• מתוכו סך הפקדות קרן: ${BidiFmt.ltr("₪${Utils.formatResult(savedNow + monthlyDep * months, 0)}")} | רווחי תשואה: ${BidiFmt.ltr("₪${Utils.formatResult(curNestEgg - (savedNow + monthlyDep * months), 0)}")}",
            "• קצבה חודשית צפויה (לפי מקדם קצבה 200): ${BidiFmt.ltr("₪${Utils.formatResult(curMonthlyPension, 0)}")} לחודש",
            "• משיכה חודשית בטוחה לפי כלל 4% (ללא שחיקת הקרן): ${BidiFmt.ltr("₪${Utils.formatResult(safe4PctMonthly, 0)}")} לחודש"
        )
    }

    BidiFmt.bindLiveInputs(curAgeIn, retAgeIn, savedNowIn, monthlyDepositIn, annualReturnIn) { calcRetirement() }
    val calcBtn = createActionBtn("חשב חיסכון וקצבת פרישה 🏖️", true) { calcRetirement() }
    calcRetirement()

    root.addView(retireCanvas); root.addView(curAgeIn); root.addView(retAgeIn); root.addView(savedNowIn); root.addView(monthlyDepositIn); root.addView(annualReturnIn)
    root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

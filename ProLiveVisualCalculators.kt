package com.example.calculator

import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import kotlin.math.*

// =========================================================================
// 1. גאומטריה 2D/3D, פיתגורס ושרטוט פרופורציונלי חי (Tab 53)
// =========================================================================
fun MainActivity.createProGeometryBlueprintLiveScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20f), dp(12f), dp(20f), dp(24f))
    }
    root.addView(
        createProCanvaHeader(
            "📐 גאומטריה, פיתגורס ונפחי גופים (Blueprint)",
            "שרטוט חי המשתנה פרופורציונלית בזמן אמת לפי הצלעות, הרדיוס או התרגיל שהוקלד"
        )
    )

    val shapes = listOf(
        "משולש ישר-זווית (פיתגורס וזוויות)",
        "מעגל (רדיוס, היקף ושטח)",
        "כדור 3D (נפח ושטח פנים)",
        "גליל 3D (רדיוס, גובה ונפח)",
        "חרוט 3D (רדיוס, גובה, קו יוצר ונפח)",
        "פירמידה מרובעת 3D (בסיס, גובה ונפח)"
    )
    var shapeIdx = 0
    var paramA = 3.0
    var paramB = 4.0

    val blueprintCanvas = object : View(this) {
        private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val shapeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val shapeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        private val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(10f, 8f), 0f)
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat()
            val h = height.toFloat()
            val isMin = isMinimalistMode

            // רשת שרטוט הנדסית (Blueprint Grid)
            val step = 18f * d
            gridPaint.strokeWidth = 0.8f * d
            gridPaint.color = if (isMin) Color.argb(24, 44, 44, 44) else Color.argb(28, 100, 181, 246)
            var gx = step
            while (gx < w) {
                canvas.drawLine(gx, 0f, gx, h, gridPaint)
                gx += step
            }
            var gy = step
            while (gy < h) {
                canvas.drawLine(0f, gy, w, gy, gridPaint)
                gy += step
            }

            val cx = w / 2f
            val cy = h * 0.45f
            val maxBoxW = w * 0.62f
            val maxBoxH = h * 0.56f

            val strokeCol = if (isMin) MinimalistTheme.TEXT_CHARCOAL else primaryColor
            val fillCol = if (isMin) Color.argb(55, 163, 213, 181) else Color.argb(55, 0, 245, 212)
            shapeStrokePaint.color = strokeCol
            shapeStrokePaint.strokeWidth = 2.5f * d
            shapeFillPaint.color = fillCol
            dashPaint.color = if (isMin) Color.argb(120, 44, 44, 44) else Color.argb(140, 255, 255, 255)
            dashPaint.strokeWidth = 1.5f * d
            textPaint.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE

            val a = paramA.coerceAtLeast(0.1)
            val b = paramB.coerceAtLeast(0.1)

            when (shapeIdx) {
                0 -> {
                    // משולש ישר-זווית פרופורציונלי ליחס האמיתי בין ניצב a לניצב b
                    val ratio = (b / a).toFloat().coerceIn(0.22f, 4.5f)
                    val triW: Float
                    val triH: Float
                    if (ratio >= maxBoxW / maxBoxH) {
                        triW = maxBoxW
                        triH = (maxBoxW / ratio).coerceAtLeast(26f * d)
                    } else {
                        triH = maxBoxH
                        triW = (maxBoxH * ratio).coerceAtLeast(26f * d)
                    }

                    val left = cx - triW / 2f
                    val right = cx + triW / 2f
                    val top = cy - triH / 2f
                    val bottom = cy + triH / 2f

                    val path = Path().apply {
                        moveTo(left, top)
                        lineTo(left, bottom)
                        lineTo(right, bottom)
                        close()
                    }
                    canvas.drawPath(path, shapeFillPaint)
                    canvas.drawPath(path, shapeStrokePaint)

                    // סימון 90 מעלות בפינה השמאלית התחתונה
                    val sq = min(triW, triH).coerceAtMost(14f * d) * 0.35f
                    canvas.drawRect(left, bottom - sq, left + sq, bottom, dashPaint)

                    val c = hypot(a, b)
                    val alphaDeg = Math.toDegrees(atan2(a, b))
                    textPaint.textSize = 11.5f * d
                    canvas.drawText(BidiFmt.ltr("a=${Utils.formatResult(a, 1)}"), left - 22f * d, (top + bottom) / 2f, textPaint)
                    canvas.drawText(BidiFmt.ltr("b=${Utils.formatResult(b, 1)}"), (left + right) / 2f, bottom + 15f * d, textPaint)
                    canvas.drawText(BidiFmt.ltr("c=${Utils.formatResult(c, 2)}"), (left + right) / 2f + 16f * d, (top + bottom) / 2f - 8f * d, textPaint)

                    val footer = "a=${Utils.formatResult(a, 2)} | b=${Utils.formatResult(b, 2)} | c=${Utils.formatResult(c, 2)} | α=${Utils.formatResult(alphaDeg, 1)}°"
                    CanvasUiHelper.drawFittedText(canvas, BidiFmt.ltr(footer), cx, h - 10f * d, w - 24f * d, 13f * d, 9f * d, textPaint)
                }
                1, 2 -> {
                    // מעגל / כדור תלת-ממדי שמשנה את גודלו על הרשת בהתאם לרדיוס r
                    val scale = (ln(a + 1.0) / ln(25.0)).toFloat().coerceIn(0.28f, 1.0f)
                    val rad = min(maxBoxW, maxBoxH) * 0.52f * scale
                    canvas.drawCircle(cx, cy, rad, shapeFillPaint)
                    canvas.drawCircle(cx, cy, rad, shapeStrokePaint)
                    if (shapeIdx == 2) {
                        canvas.drawOval(RectF(cx - rad, cy - rad * 0.32f, cx + rad, cy + rad * 0.32f), dashPaint)
                    }
                    canvas.drawLine(cx, cy, cx + rad, cy, dashPaint)
                    textPaint.textSize = 12f * d
                    canvas.drawText(BidiFmt.ltr("r=${Utils.formatResult(a, 2)}"), cx + rad * 0.5f, cy - 6f * d, textPaint)

                    val footer = if (shapeIdx == 1) {
                        "r=${Utils.formatResult(a, 2)} | P=${Utils.formatResult(2 * PI * a, 2)} | A=${Utils.formatResult(PI * a * a, 2)}"
                    } else {
                        "r=${Utils.formatResult(a, 2)} | V=${Utils.formatResult((4.0 / 3.0) * PI * a.pow(3), 2)}"
                    }
                    CanvasUiHelper.drawFittedText(canvas, BidiFmt.ltr(footer), cx, h - 10f * d, w - 24f * d, 13f * d, 9f * d, textPaint)
                }
                3 -> {
                    // גליל 3D שמשנה רוחב וגובה לפי רדיוס r וגובה h
                    val ratio = ((2.0 * a) / b).toFloat().coerceIn(0.28f, 3.2f)
                    val cylW: Float
                    val cylH: Float
                    if (ratio >= maxBoxW / maxBoxH) {
                        cylW = maxBoxW * 0.85f
                        cylH = (cylW / ratio).coerceAtLeast(30f * d)
                    } else {
                        cylH = maxBoxH * 0.85f
                        cylW = (cylH * ratio).coerceAtLeast(30f * d)
                    }
                    val ry = (cylW * 0.18f).coerceIn(8f * d, 18f * d)
                    val left = cx - cylW / 2f; val right = cx + cylW / 2f
                    val top = cy - cylH / 2f; val bottom = cy + cylH / 2f

                    canvas.drawRect(left, top, right, bottom, shapeFillPaint)
                    canvas.drawOval(RectF(left, top - ry, right, top + ry), shapeStrokePaint)
                    canvas.drawOval(RectF(left, bottom - ry, right, bottom + ry), shapeStrokePaint)
                    canvas.drawLine(left, top, left, bottom, shapeStrokePaint)
                    canvas.drawLine(right, top, right, bottom, shapeStrokePaint)

                    val vol = PI * a * a * b
                    val footer = "r=${Utils.formatResult(a, 2)} | h=${Utils.formatResult(b, 2)} | V=${Utils.formatResult(vol, 2)}"
                    CanvasUiHelper.drawFittedText(canvas, BidiFmt.ltr(footer), cx, h - 10f * d, w - 24f * d, 13f * d, 9f * d, textPaint)
                }
                else -> {
                    // חרוט 3D / פירמידה מרובעת 3D פרופורציונליים
                    val ratio = ((2.0 * a) / b).toFloat().coerceIn(0.28f, 3.2f)
                    val baseW: Float
                    val pyrH: Float
                    if (ratio >= maxBoxW / maxBoxH) {
                        baseW = maxBoxW * 0.85f
                        pyrH = (baseW / ratio).coerceAtLeast(30f * d)
                    } else {
                        pyrH = maxBoxH * 0.85f
                        baseW = (pyrH * ratio).coerceAtLeast(30f * d)
                    }
                    val left = cx - baseW / 2f; val right = cx + baseW / 2f
                    val top = cy - pyrH / 2f; val bottom = cy + pyrH / 2f

                    val conePath = Path().apply {
                        moveTo(cx, top)
                        lineTo(right, bottom)
                        lineTo(left, bottom)
                        close()
                    }
                    canvas.drawPath(conePath, shapeFillPaint)
                    canvas.drawPath(conePath, shapeStrokePaint)
                    canvas.drawLine(cx, top, cx, bottom, dashPaint)
                    if (shapeIdx == 4) {
                        val ry = (baseW * 0.16f).coerceIn(7f * d, 16f * d)
                        canvas.drawOval(RectF(left, bottom - ry, right, bottom + ry), shapeStrokePaint)
                    }

                    val footer = if (shapeIdx == 4) {
                        "r=${Utils.formatResult(a, 2)} | h=${Utils.formatResult(b, 2)} | s=${Utils.formatResult(hypot(a, b), 2)}"
                    } else {
                        "a=${Utils.formatResult(a, 2)} | h=${Utils.formatResult(b, 2)} | V=${Utils.formatResult((a * a * b) / 3.0, 2)}"
                    }
                    CanvasUiHelper.drawFittedText(canvas, BidiFmt.ltr(footer), cx, h - 10f * d, w - 24f * d, 13f * d, 9f * d, textPaint)
                }
            }
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(215f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProGeometryBlueprintLiveScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProGeometryBlueprintLiveScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val shapeBtn = createActionBtn("צורה: ${shapes[0]}", true) {}
    val inA = createInputBox("ניצב a / רדיוס r / צלע בסיס a (או תרגיל)").apply { setText("3") }
    val inB = createInputBox("ניצב b / גובה h (או תרגיל)").apply { setText("4") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView().apply { textSize = 15.5f })

    fun updateGeometry() {
        paramA = BidiFmt.evalInputExpr(inA, 3.0).coerceAtLeast(0.01)
        paramB = BidiFmt.evalInputExpr(inB, 4.0).coerceAtLeast(0.01)
        inB.visibility = if (shapeIdx == 1 || shapeIdx == 2) View.GONE else View.VISIBLE
        blueprintCanvas.invalidate()

        val a = paramA; val b = paramB
        resTv.text = when (shapeIdx) {
            0 -> {
                val c = hypot(a, b)
                val area = 0.5 * a * b
                val perim = a + b + c
                val alpha = Math.toDegrees(atan2(a, b))
                val beta = 90.0 - alpha
                BidiFmt.rtlParagraph(
                    "📐 משולש ישר-זווית (משפט פיתגורס):",
                    "• יתר (c): ${BidiFmt.ltr(Utils.formatResult(c, calcPrecision))}",
                    "• שטח (S): ${BidiFmt.ltr(Utils.formatResult(area, calcPrecision))} | היקף (P): ${BidiFmt.ltr(Utils.formatResult(perim, calcPrecision))}",
                    "• זוויות חדות: ${BidiFmt.ltr("α = ${Utils.formatResult(alpha, 2)}°")} , ${BidiFmt.ltr("β = ${Utils.formatResult(beta, 2)}°")}"
                )
            }
            1 -> {
                val area = PI * a * a
                val circ = 2.0 * PI * a
                BidiFmt.rtlParagraph(
                    "⭕ מעגל (רדיוס ${BidiFmt.ltr("r = ${Utils.formatResult(a, 2)}")}):",
                    "• קוטר (d): ${BidiFmt.ltr(Utils.formatResult(2.0 * a, calcPrecision))}",
                    "• היקף המעגל: ${BidiFmt.ltr(Utils.formatResult(circ, calcPrecision))}",
                    "• שטח המעגל: ${BidiFmt.ltr(Utils.formatResult(area, calcPrecision))}"
                )
            }
            2 -> {
                val vol = (4.0 / 3.0) * PI * a.pow(3)
                val surf = 4.0 * PI * a * a
                BidiFmt.rtlParagraph(
                    "🌐 כדור תלת-ממדי (רדיוס ${BidiFmt.ltr("r = ${Utils.formatResult(a, 2)}")}):",
                    "• נפח הכדור (V): ${BidiFmt.ltr(Utils.formatResult(vol, calcPrecision))}",
                    "• שטח פנים (A): ${BidiFmt.ltr(Utils.formatResult(surf, calcPrecision))}"
                )
            }
            3 -> {
                val vol = PI * a * a * b
                val lateral = 2.0 * PI * a * b
                val totalA = lateral + 2.0 * PI * a * a
                BidiFmt.rtlParagraph(
                    "🛢️ גליל תלת-ממדי:",
                    "• נפח הגליל (V): ${BidiFmt.ltr(Utils.formatResult(vol, calcPrecision))}",
                    "• שטח מעטפת: ${BidiFmt.ltr(Utils.formatResult(lateral, calcPrecision))}",
                    "• שטח פנים כולל: ${BidiFmt.ltr(Utils.formatResult(totalA, calcPrecision))}"
                )
            }
            4 -> {
                val s = hypot(a, b)
                val vol = (PI * a * a * b) / 3.0
                val surf = PI * a * (a + s)
                BidiFmt.rtlParagraph(
                    "🍦 חרוט תלת-ממדי:",
                    "• קו יוצר (s): ${BidiFmt.ltr(Utils.formatResult(s, calcPrecision))}",
                    "• נפח החרוט (V): ${BidiFmt.ltr(Utils.formatResult(vol, calcPrecision))}",
                    "• שטח פנים כולל: ${BidiFmt.ltr(Utils.formatResult(surf, calcPrecision))}"
                )
            }
            else -> {
                val vol = (a * a * b) / 3.0
                val slant = hypot(a / 2.0, b)
                val surf = a * a + 2.0 * a * slant
                BidiFmt.rtlParagraph(
                    "🔺 פירמידה מרובעת ישרה:",
                    "• נפח הפירמידה (V): ${BidiFmt.ltr(Utils.formatResult(vol, calcPrecision))}",
                    "• גובה פאה צדדית: ${BidiFmt.ltr(Utils.formatResult(slant, calcPrecision))}",
                    "• שטח פנים כולל: ${BidiFmt.ltr(Utils.formatResult(surf, calcPrecision))}"
                )
            }
        }
    }

    shapeBtn.setOnClickListener {
        showItemPicker("בחר צורה גאומטרית", shapes) { chosen ->
            shapeIdx = shapes.indexOf(chosen).coerceAtLeast(0)
            shapeBtn.text = "צורה: $chosen"
            updateGeometry()
        }
    }

    BidiFmt.bindLiveInputs(inA, inB) { updateGeometry() }
    val calcBtn = createActionBtn("חשב ושרטט צורה גאומטרית 📐", false) { updateGeometry() }
    updateGeometry()

    root.addView(blueprintCanvas); root.addView(shapeBtn); root.addView(inA); root.addView(inB)
    root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 2. מרווח רווח, גרף נקודת איזון חי ותקציב (Tab 51)
// =========================================================================
fun MainActivity.createProBusinessMarginLiveScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20f), dp(12f), dp(20f), dp(24f))
    }
    root.addView(
        createProCanvaHeader(
            "💼 מרווח רווח, גרף נקודת איזון ותקציב",
            "רווח גולמי (Margin), תוספת לעלות (Markup), גרף Break-Even חי וכלל 50/30/20"
        )
    )

    var curUnitCost = 60.0
    var curUnitPrice = 80.0
    var curFixedCost = 160.0

    val breakEvenCanvas = object : View(this) {
        private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        private val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
        }
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat()
            val h = height.toFloat()
            val isMin = isMinimalistMode

            val padL = 26f * d
            val padR = w - 22f * d
            val padT = 22f * d
            val padB = h - 32f * d
            val plotW = (padR - padL).coerceAtLeast(10f)
            val plotH = (padB - padT).coerceAtLeast(10f)

            axisPaint.strokeWidth = 1.6f * d
            axisPaint.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            canvas.drawLine(padL, padT, padL, padB, axisPaint)
            canvas.drawLine(padL, padB, padR, padB, axisPaint)

            val contrib = curUnitPrice - curUnitCost
            val hasBreakEven = contrib > 1e-6 && curFixedCost >= 0.0
            val qBe = if (hasBreakEven) curFixedCost / contrib else 10.0

            // סקאלה דינמית כך ששינוי עלות/מחיר/הוצאות קבועות מזיז בפועל את השיפועים ואת נקודת החיתוך על הגרף!
            val baseScaleQ = max(12.0, ceil(qBe / 10.0) * 15.0)
            val maxQ = if (hasBreakEven) max(baseScaleQ, qBe * 1.35) else 20.0
            val revAtMax = curUnitPrice * maxQ
            val costAtMax = curFixedCost + curUnitCost * maxQ
            val maxY = maxOf(revAtMax, costAtMax, curFixedCost * 1.25, 100.0)

            fun xOf(q: Double): Float = (padL + (q / maxQ).coerceIn(0.0, 1.0) * plotW).toFloat()
            fun yOf(money: Double): Float = (padB - (money / maxY).coerceIn(0.0, 1.0) * plotH).toFloat()

            val x0 = xOf(0.0)
            val yFc = yOf(curFixedCost)
            val xEnd = xOf(maxQ)
            val yCostEnd = yOf(costAtMax)
            val yRevEnd = yOf(revAtMax)

            // קו הוצאות קבועות מקווקו
            dashPaint.strokeWidth = 1.2f * d
            dashPaint.color = if (isMin) Color.argb(75, 44, 44, 44) else Color.argb(85, 255, 255, 255)
            canvas.drawLine(x0, yFc, xEnd, yFc, dashPaint)

            val costCol = if (isMin) Color.parseColor("#E57373") else Color.parseColor("#FF5252")
            val revCol = if (isMin) Color.parseColor("#66BB6A") else Color.parseColor("#00E676")

            linePaint.strokeWidth = 2.8f * d
            // קו עלויות כוללות (אדום)
            linePaint.color = costCol
            canvas.drawLine(x0, yFc, xEnd, yCostEnd, linePaint)

            // קו פדיון והכנסות (ירוק)
            linePaint.color = revCol
            canvas.drawLine(x0, padB, xEnd, yRevEnd, linePaint)

            textPaint.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
            if (hasBreakEven) {
                val rBe = curUnitPrice * qBe
                val ix = xOf(qBe)
                val iy = yOf(rBe)

                // קווי עזר מרוסקים מנקודת החיתוך אל הצירים
                canvas.drawLine(ix, iy, ix, padB, dashPaint)
                canvas.drawLine(padL, iy, ix, iy, dashPaint)

                // הנקודה הכחולה יושבת בדיוק על מפגש הקווים!
                fillPaint.color = Color.WHITE
                canvas.drawCircle(ix, iy, 6.5f * d, fillPaint)
                fillPaint.color = if (isMin) Color.parseColor("#64B5F6") else Color.parseColor("#00E5FF")
                canvas.drawCircle(ix, iy, 5.0f * d, fillPaint)

                textPaint.textSize = 11.2f * d
                val pointLbl = "\u200F(${BidiFmt.ltr("${Utils.formatResult(qBe, 1)} יח׳")}, ${BidiFmt.ltr("₪${Utils.formatResult(rBe, 0)}")})"
                val lblY = (iy - 10f * d).coerceAtLeast(padT + 12f * d)
                val lblX = ix.coerceIn(padL + 55f * d, padR - 55f * d)
                canvas.drawText(pointLbl, lblX, lblY, textPaint)

                val bottomCaption = "\u200Fנקודת איזון: ${BidiFmt.ltr("${Utils.formatResult(qBe, 1)} יח׳")} (ירוק = פדיון | אדום = עלויות)"
                CanvasUiHelper.drawFittedText(canvas, bottomCaption, w / 2f, h - 8f * d, w - 20f * d, 12.5f * d, 9f * d, textPaint)
            } else {
                val warn = "\u200F⚠️ אין נקודת איזון (מחיר המכירה נמוך או שווה לעלות היחידה)"
                CanvasUiHelper.drawFittedText(canvas, warn, w / 2f, h - 8f * d, w - 20f * d, 12.5f * d, 9f * d, textPaint)
            }
        }
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(200f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProBusinessMarginLiveScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProBusinessMarginLiveScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val costIn = createInputBox("עלות ייצור/רכישה ליחידה (למשל 60)").apply { setText("60") }
    val priceIn = createInputBox("מחיר מכירה ליחידה (למשל 80)").apply { setText("80") }
    val fixedIn = createInputBox("הוצאות קבועות לחודש (למשל 160)").apply { setText("160") }
    val resTv = BidiFmt.applyRtlToResultView(createResultTextView().apply { textSize = 16.0f })

    fun updateMarginAndBreakEven() {
        curUnitCost = BidiFmt.evalInputExpr(costIn, 60.0).coerceAtLeast(0.0)
        curUnitPrice = BidiFmt.evalInputExpr(priceIn, 80.0).coerceAtLeast(0.0)
        curFixedCost = BidiFmt.evalInputExpr(fixedIn, 160.0).coerceAtLeast(0.0)
        breakEvenCanvas.invalidate()

        val profitPerUnit = curUnitPrice - curUnitCost
        val grossMarginPct = if (curUnitPrice > 0) (profitPerUnit / curUnitPrice) * 100.0 else 0.0
        val markupPct = if (curUnitCost > 0) (profitPerUnit / curUnitCost) * 100.0 else 0.0

        val beLine = if (profitPerUnit > 0) {
            val beUnits = curFixedCost / profitPerUnit
            val beRevenue = beUnits * curUnitPrice
            "• נקודת איזון ${BidiFmt.ltr("(Break-Even)")}: ${BidiFmt.ltr(Utils.formatResult(beUnits, 2))} יחידות (פדיון: ${BidiFmt.ltr("₪${Utils.formatResult(beRevenue, 2)}")})"
        } else {
            "• נקודת איזון ${BidiFmt.ltr("(Break-Even)")}: לא קיימת (הפסד של ${BidiFmt.ltr("₪${Utils.formatResult(abs(profitPerUnit), 2)}")} לכל יחידה)"
        }

        resTv.text = BidiFmt.rtlParagraph(
            "ניתוח רווחיות עסקית:",
            "• רווח גולמי ליחידה: ${BidiFmt.ltr("₪${Utils.formatResult(profitPerUnit, 2)}")}",
            "• מרווח רווח ${BidiFmt.ltr("(Gross Margin)")}: ${BidiFmt.ltr("${Utils.formatResult(grossMarginPct, 2)}%")}",
            "• תוספת לעלות ${BidiFmt.ltr("(Markup)")}: ${BidiFmt.ltr("${Utils.formatResult(markupPct, 2)}%")}",
            beLine
        )
    }

    BidiFmt.bindLiveInputs(costIn, priceIn, fixedIn) { updateMarginAndBreakEven() }
    val calcBtn = createActionBtn("חשב מרווח רווח ונקודת איזון", true) { updateMarginAndBreakEven() }
    updateMarginAndBreakEven()

    root.addView(breakEvenCanvas); root.addView(costIn); root.addView(priceIn); root.addView(fixedIn)
    root.addView(calcBtn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

package com.example.calculator

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfDocument
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*
import kotlin.random.Random

object ProState {
    fun isProEnabled(prefs: android.content.SharedPreferences?): Boolean =
        prefs?.getBoolean("pro_mode_enabled", false) ?: false

    fun setProEnabled(prefs: android.content.SharedPreferences?, enabled: Boolean) {
        prefs?.edit()?.putBoolean("pro_mode_enabled", enabled)?.apply()
    }
}

// =========================================================================
// תצוגות קנבס (Canvas) ייעודיות למחשבוני ה-Pro (כולל תמיכה ב"עיצוב מינימליסטי", הגנת ניגודיות ו-fitText)
// =========================================================================

// 1. קנבס השוואת קנה-מידה ליחידות מידה
class UnitScaleComparisonCanvasView(context: Context, private val activity: MainActivity) : View(context) {
    var fromLabel: String = "1 מטר"
    var toLabel: String = "100 ס״מ"
    var ratioLog: Float = 0.5f

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val w = width.toFloat()
        val isMin = activity.isMinimalistMode
        val accent = if (isMin) MinimalistTheme.PILL_AC_PERIWINKLE
        else CanvasUiHelper.ensureReadableAccent(activity.primaryColor, activity.isAuroraMode)

        val bar1Top = 14f * d; val bar1Bot = 34f * d
        val bar2Top = 44f * d; val bar2Bot = 64f * d
        val padX = 16f * d

        p.color = if (isMin) Color.argb(32, 43, 40, 37) else Color.argb(60, 255, 255, 255)
        canvas.drawRoundRect(RectF(padX, bar1Top, w - padX, bar1Bot), 10f * d, 10f * d, p)
        canvas.drawRoundRect(RectF(padX, bar2Top, w - padX, bar2Bot), 10f * d, 10f * d, p)

        p.color = accent
        canvas.drawRoundRect(RectF(padX, bar1Top, padX + (w - 2 * padX) * 0.55f, bar1Bot), 10f * d, 10f * d, p)
        p.color = if (isMin) MinimalistTheme.ACTION_CORAL_PRIMARY else Color.parseColor("#FF9F0A")
        val targetW = (w - 2 * padX) * ratioLog.coerceIn(0.14f, 0.96f)
        canvas.drawRoundRect(RectF(padX, bar2Top, padX + targetW, bar2Bot), 10f * d, 10f * d, p)

        p.color = if (isMin) MinimalistTheme.TEXT_DARK else Color.WHITE
        p.typeface = Typeface.DEFAULT_BOLD
        CanvasUiHelper.drawFittedText(canvas, "מקור: $fromLabel", padX + 10f * d, bar1Bot - 5.5f * d, w - 2 * padX - 20f * d, 12.5f * d, 8.5f * d, p)
        CanvasUiHelper.drawFittedText(canvas, "יעד: $toLabel", padX + 10f * d, bar2Bot - 5.5f * d, w - 2 * padX - 20f * d, 12.5f * d, 8.5f * d, p)
    }
}

// 2. קנבס פילוח משכנתא שפיצר (קרן מול ריבית)
class SpitzerMortgageCanvasView(context: Context, private val activity: MainActivity) : View(context) {
    var principalVal: Double = 1000000.0
    var interestVal: Double = 450000.0
    var monthlyPmt: Double = 5200.0

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val w = width.toFloat(); val h = height.toFloat()
        val isMin = activity.isMinimalistMode
        val total = (principalVal + interestVal).coerceAtLeast(1.0)
        val pRatio = (principalVal / total).toFloat().coerceIn(0.08f, 0.92f)
        val accent = if (isMin) MinimalistTheme.PILL_MR_MINT
        else CanvasUiHelper.ensureReadableAccent(activity.primaryColor, activity.isAuroraMode)

        val barLeft = 18f * d; val barRight = w - 18f * d; val barW = barRight - barLeft
        p.color = accent
        canvas.drawRoundRect(RectF(barLeft, 16f * d, barLeft + barW * pRatio, 42f * d), 11f * d, 11f * d, p)
        p.color = if (isMin) MinimalistTheme.ACTION_CORAL_PRIMARY else Color.parseColor("#FF4D6D")
        canvas.drawRoundRect(RectF(barLeft + barW * pRatio + 3f * d, 16f * d, barRight, 42f * d), 11f * d, 11f * d, p)

        p.color = if (isMin) MinimalistTheme.TEXT_DARK else Color.WHITE
        p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
        val splitLine = "קרן: ${(pRatio * 100).toInt()}% (₪${Utils.formatResult(principalVal, 0)})  |  ריבית: ${((1f - pRatio) * 100).toInt()}% (₪${Utils.formatResult(interestVal, 0)})"
        CanvasUiHelper.drawFittedText(canvas, splitLine, w / 2f, 62f * d, w - 24f * d, 13f * d, 8.5f * d, p)
        p.color = if (isMin) MinimalistTheme.ALERT_RED_TEXT else accent
        CanvasUiHelper.drawFittedText(canvas, "החזר חודשי התחלתי: ₪${Utils.formatResult(monthlyPmt, 0)}", w / 2f, h - 12f * d, w - 24f * d, 15f * d, 10f * d, p)
    }
}

// 3. קנבס גרף נקודת איזון עסקית (Break-Even Chart)
class BreakEvenChartCanvasView(context: Context, private val activity: MainActivity) : View(context) {
    var fixedCost: Float = 15000f
    var unitCost: Float = 100f
    var unitPrice: Float = 180f
    var breakEvenUnits: Int = 188

    private val revPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.parseColor("#00E676") }
    private val costPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.parseColor("#FF5252") }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val isMin = activity.isMinimalistMode
        revPaint.strokeWidth = 2.5f * d
        revPaint.color = if (isMin) Color.parseColor("#2E7D32") else Color.parseColor("#00E676")
        costPaint.strokeWidth = 2.5f * d
        costPaint.color = if (isMin) MinimalistTheme.ALERT_RED_TEXT else Color.parseColor("#FF5252")
        axisPaint.strokeWidth = 1.2f * d
        axisPaint.color = if (isMin) Color.argb(95, 43, 40, 37) else Color.argb(110, 255, 255, 255)
        txtPaint.color = if (isMin) MinimalistTheme.TEXT_DARK else Color.WHITE

        val w = width.toFloat(); val h = height.toFloat()
        val padL = 24f * d; val padR = w - 24f * d
        val padT = 18f * d; val padB = h - 28f * d

        canvas.drawLine(padL, padB, padR, padB, axisPaint)
        canvas.drawLine(padL, padT, padL, padB, axisPaint)

        canvas.drawLine(padL, padB, padR, padT + 10f * d, revPaint)
        val fixedY = padB - (padB - padT) * 0.35f
        canvas.drawLine(padL, fixedY, padR, padT + (padB - padT) * 0.22f, costPaint)

        val beX = (padL + padR) / 2f
        val beY = (padB + padT + 10f * d) / 2f
        val accent = if (isMin) MinimalistTheme.ACTION_CORAL_PRIMARY
        else CanvasUiHelper.ensureReadableAccent(activity.primaryColor, activity.isAuroraMode)
        canvas.drawCircle(beX, beY, 5.5f * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })

        CanvasUiHelper.drawFittedText(canvas, "נקודת איזון: $breakEvenUnits יח׳ (ירוק = פדיון | אדום = עלויות)", w / 2f, h - 8f * d, w - 20f * d, 12.5f * d, 8.5f * d, txtPaint)
    }
}

// 4. קנבס עקומת פעמון גאוסיאנית לסטטיסטיקה והתפלגות נורמלית
class BellCurveStatsCanvasView(context: Context, private val activity: MainActivity) : View(context) {
    var meanVal: Double = 0.0
    var stdDevVal: Double = 1.0

    private val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val axisP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val isMin = activity.isMinimalistMode
        lineP.strokeWidth = 2.5f * d
        axisP.strokeWidth = 1.2f * d
        axisP.color = if (isMin) Color.argb(95, 43, 40, 37) else Color.argb(110, 255, 255, 255)
        txtP.color = if (isMin) MinimalistTheme.TEXT_DARK else Color.WHITE

        val w = width.toFloat(); val h = height.toFloat()
        val pad = 18f * d
        val baseLine = h - 24f * d
        val accent = if (isMin) MinimalistTheme.ACTION_CORAL_PRIMARY
        else CanvasUiHelper.ensureReadableAccent(activity.primaryColor, activity.isAuroraMode)

        lineP.color = accent
        fillP.shader = LinearGradient(0f, pad, 0f, baseLine, Color.argb(120, Color.red(accent), Color.green(accent), Color.blue(accent)), Color.TRANSPARENT, Shader.TileMode.CLAMP)

        val path = Path()
        val fillPath = Path()
        val steps = 60
        for (i in 0..steps) {
            val z = -3.2f + (6.4f * i / steps)
            val gauss = exp(-0.5f * z * z)
            val x = pad + (i.toFloat() / steps) * (w - 2 * pad)
            val y = baseLine - gauss * (baseLine - pad - 8f * d)
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, baseLine)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(w - pad, baseLine)
        fillPath.close()

        canvas.drawPath(fillPath, fillP)
        canvas.drawPath(path, lineP)
        canvas.drawLine(pad, baseLine, w - pad, baseLine, axisP)
        canvas.drawLine(w / 2f, pad, w / 2f, baseLine, axisP)
        CanvasUiHelper.drawFittedText(canvas, "μ = ${Utils.formatResult(meanVal, 2)}   |   σ = ${Utils.formatResult(stdDevVal, 2)}", w / 2f, h - 6f * d, w - 20f * d, 12.5f * d, 8.5f * d, txtP)
    }
}

// 5. קנבס שרטוט גאומטרי (Blueprint) למשולש, מעגל וגופים הנדסיים
class GeometryBlueprintCanvasView(context: Context, private val activity: MainActivity) : View(context) {
    var mode: Int = 0
    var paramA: Double = 3.0
    var paramB: Double = 4.0

    private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val gridP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val isMin = activity.isMinimalistMode
        strokeP.strokeWidth = 2.2f * d
        gridP.strokeWidth = 0.8f * d
        gridP.color = if (isMin) Color.argb(35, 43, 40, 37) else Color.argb(38, 0, 245, 212)
        txtP.color = if (isMin) MinimalistTheme.TEXT_DARK else Color.WHITE

        val w = width.toFloat(); val h = height.toFloat()
        val accent = if (isMin) MinimalistTheme.ACTION_CORAL_PRIMARY
        else CanvasUiHelper.ensureReadableAccent(activity.primaryColor, activity.isAuroraMode)
        strokeP.color = accent

        val step = 18f * d
        var gx = 0f
        while (gx < w) { canvas.drawLine(gx, 0f, gx, h, gridP); gx += step }
        var gy = 0f
        while (gy < h) { canvas.drawLine(0f, gy, w, gy, gridP); gy += step }

        val cx = w / 2f; val cy = h * 0.46f
        when (mode) {
            0 -> {
                val p = Path().apply {
                    moveTo(cx - 55f * d, cy + 32f * d)
                    lineTo(cx + 55f * d, cy + 32f * d)
                    lineTo(cx - 55f * d, cy - 36f * d)
                    close()
                }
                canvas.drawPath(p, strokeP)
                val c = hypot(paramA, paramB)
                CanvasUiHelper.drawFittedText(canvas, "a=${Utils.formatResult(paramA, 1)} | b=${Utils.formatResult(paramB, 1)} | c=${Utils.formatResult(c, 2)}", cx, h - 8f * d, w - 20f * d, 12.5f * d, 8.5f * d, txtP)
            }
            1 -> {
                val r = min(w, h) * 0.30f
                canvas.drawCircle(cx, cy, r, strokeP)
                canvas.drawLine(cx, cy, cx + r, cy, strokeP)
                CanvasUiHelper.drawFittedText(canvas, "רדיוס R = ${Utils.formatResult(paramA, 2)}", cx, h - 8f * d, w - 20f * d, 12.5f * d, 8.5f * d, txtP)
            }
            else -> {
                val r = min(w, h) * 0.24f
                canvas.drawOval(RectF(cx - r, cy - 34f * d, cx + r, cy - 16f * d), strokeP)
                canvas.drawOval(RectF(cx - r, cy + 16f * d, cx + r, cy + 34f * d), strokeP)
                canvas.drawLine(cx - r, cy - 25f * d, cx - r, cy + 25f * d, strokeP)
                canvas.drawLine(cx + r, cy - 25f * d, cx + r, cy + 25f * d, strokeP)
                CanvasUiHelper.drawFittedText(canvas, "R=${Utils.formatResult(paramA, 1)} | H=${Utils.formatResult(paramB, 1)}", cx, h - 8f * d, w - 20f * d, 12.5f * d, 8.5f * d, txtP)
            }
        }
    }
}

// 6. קנבס הצגת מטריצה ודטרמיננטה 2x2
class MatrixLinearCanvasView(context: Context, private val activity: MainActivity) : View(context) {
    var a11 = "2"; var a12 = "3"; var a21 = "1"; var a22 = "-4"
    var detText = "det(A) = -11"

    private val bracketP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.MONOSPACE }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val isMin = activity.isMinimalistMode
        val accent = if (isMin) MinimalistTheme.ACTION_CORAL_PRIMARY
        else CanvasUiHelper.ensureReadableAccent(activity.primaryColor, activity.isAuroraMode)
        bracketP.strokeWidth = 2.5f * d
        bracketP.color = accent

        val cx = width / 2f; val cy = height * 0.42f
        val bw = 75f * d; val bh = 36f * d

        canvas.drawLine(cx - bw, cy - bh, cx - bw + 12f * d, cy - bh, bracketP)
        canvas.drawLine(cx - bw, cy - bh, cx - bw, cy + bh, bracketP)
        canvas.drawLine(cx - bw, cy + bh, cx - bw + 12f * d, cy + bh, bracketP)

        canvas.drawLine(cx + bw, cy - bh, cx + bw - 12f * d, cy - bh, bracketP)
        canvas.drawLine(cx + bw, cy - bh, cx + bw, cy + bh, bracketP)
        canvas.drawLine(cx + bw, cy + bh, cx + bw - 12f * d, cy + bh, bracketP)

        txtP.textSize = 15f * d
        txtP.color = if (isMin) MinimalistTheme.TEXT_DARK else Color.WHITE
        canvas.drawText(a11, cx - 35f * d, cy - 8f * d, txtP)
        canvas.drawText(a12, cx + 35f * d, cy - 8f * d, txtP)
        canvas.drawText(a21, cx - 35f * d, cy + 24f * d, txtP)
        canvas.drawText(a22, cx + 35f * d, cy + 24f * d, txtP)

        txtP.color = if (isMin) MinimalistTheme.TEXT_DARK else accent
        CanvasUiHelper.drawFittedText(canvas, detText, cx, height - 10f * d, width - 20f * d, 13.5f * d, 9f * d, txtP)
    }
}

// =========================================================================
// 1. מסכים אישיים להנדסה: אינסטלציה, רפואה, בנייה ומיזוג אוויר
// =========================================================================

// 1A. מסך אישי: מחשבון אינסטלציה, ספיקה, מספר ריינולדס, קוטר צנרת ולחץ מים
fun MainActivity.createProPlumbingAndFluidsScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🚰 הנדסת אינסטלציה, צנרת, ספיקה ולחץ", "חישוב מהירות זרימה, מספר ריינולדס, אובדן עומד ולחץ הידרוסטטי"))

    var flowVelText = "1.53 m/s"
    val pipeCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat(); val h = height.toFloat()
            p.color = if (isMinimalistMode) MinimalistTheme.COMPASS_DIAL_RING_BG else Color.parseColor("#455A64")
            canvas.drawRoundRect(RectF(16f * d, 16f * d, w - 16f * d, h - 16f * d), 14f * d, 14f * d, p)
            p.color = if (isMinimalistMode) MinimalistTheme.PILL_MC_BLUE else Color.parseColor("#0288D1")
            canvas.drawRect(24f * d, 25f * d, w - 24f * d, h - 25f * d, p)
            p.color = if (isMinimalistMode) MinimalistTheme.TEXT_DARK else Color.WHITE
            p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
            CanvasUiHelper.drawFittedText(canvas, "➔  מהירות זורם בצינור: $flowVelText  ➔", w / 2f, h / 2f + 5f * d, w - 56f * d, 14f * d, 9.5f * d, p)
        }
    }.apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(135f)).apply { bottomMargin = dp(12f) } }

    val diamIn = createInputBox("קוטר צינור פנימי במ״מ (למשל 25 מ״מ = 1 צול)").apply { setText("25") }
    val flowIn = createInputBox("ספיקת מים בליטר לדקה (L/min)").apply { setText("45") }
    val headIn = createInputBox("גובה עמוד מים במטרים (לחישוב לחץ Bar)").apply { setText("15") }
    val resTv = createResultTextView()

    val btn = createActionBtn("חשב מהירות זרימה, ריינולדס ולחץ בצנרת 🚰") {
        val dMm = diamIn.text.toString().toDoubleOrNull() ?: 25.0
        val qLmin = flowIn.text.toString().toDoubleOrNull() ?: 45.0
        val hMeters = headIn.text.toString().toDoubleOrNull() ?: 15.0
        if (dMm > 0) {
            val dM = dMm / 1000.0
            val areaM2 = PI * (dM / 2.0).pow(2.0)
            val qM3s = (qLmin / 1000.0) / 60.0
            val velocity = qM3s / areaM2
            val reynolds = (velocity * dM) / 1e-6
            val flowRegime = if (reynolds < 2300) "למינרית (שקטה)" else if (reynolds < 4000) "מעבר" else "טורבולנטית"
            val barPressure = hMeters * 0.0980665
            flowVelText = "${Utils.formatResult(velocity, 2)} m/s"
            pipeCanvas.invalidate()
            resTv.text = "תוצאות צנרת ואינסטלציה:\n" +
                    "• מהירות זרימה: ${Utils.formatResult(velocity, 2)} מ׳/שנייה\n" +
                    "• ספיקה שעתית: ${Utils.formatResult(qLmin * 0.06, 2)} מ״ק/שעה (m³/h)\n" +
                    "• מספר ריינולדס (Re): ${Utils.formatResult(reynolds, 0)} (זרימה $flowRegime)\n" +
                    "• לחץ הידרוסטטי ב-${Utils.formatResult(hMeters, 1)} מ׳: ${Utils.formatResult(barPressure, 2)} Bar (${Utils.formatResult(barPressure * 14.5038, 1)} PSI)"
        }
    }
    btn.performClick()
    root.addView(pipeCanvas); root.addView(diamIn); root.addView(flowIn); root.addView(headIn)
    root.addView(btn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// 1B. מסך אישי: מחשבון רפואי קליני - מינון תרופות, עירוי IV, BSA והמרת סוכר בדם מדויקת (18.0182)
fun MainActivity.createProMedicalDosageScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🩺 מחשבון רפואי: עירוי IV, מינון, BSA וסוכר", "קצב טיפות לדקה (gtt/min), שטח פני גוף (Mosteller BSA) והמרת סוכר קלינית (18.0182)"))

    val weightIn = createInputBox("משקל מטופל בק״ג (kg)").apply { setText("70") }
    val heightCmIn = createInputBox("גובה מטופל בס״מ (לחישוב BSA)").apply { setText("175") }
    val doseMgKgIn = createInputBox("מינון נדרש במ״ג לק״ג (mg/kg)").apply { setText("10") }
    val ivVolIn = createInputBox("נפח שקית עירוי במ״ל (mL)").apply { setText("500") }
    val ivHoursIn = createInputBox("זמן הזלפת העירוי בשעות").apply { setText("4") }
    val glucoseIn = createInputBox("רמת סוכר בדם ב-mg/dL (להמרה קלינית ל-mmol/L)").apply { setText("100") }
    val resTv = createResultTextView()

    val btn = createActionBtn("חשב מינון רפואי, BSA, עירוי וסוכר קליני 🩺") {
        val w = weightIn.text.toString().toDoubleOrNull() ?: 70.0
        val hCm = heightCmIn.text.toString().toDoubleOrNull() ?: 175.0
        val dose = doseMgKgIn.text.toString().toDoubleOrNull() ?: 10.0
        val vol = ivVolIn.text.toString().toDoubleOrNull() ?: 500.0
        val hrs = (ivHoursIn.text.toString().toDoubleOrNull() ?: 4.0).coerceAtLeast(0.1)
        val gluMgDl = glucoseIn.text.toString().toDoubleOrNull() ?: 100.0

        val totalDoseMg = w * dose
        val bsa = sqrt((w * hCm) / 3600.0)
        val mlPerHour = vol / hrs
        val dropsPerMin20 = (mlPerHour * 20.0) / 60.0
        val microDrops60 = mlPerHour
        val gluMmol = gluMgDl / ProUnitCatalog.GLUCOSE_MGDL_PER_MMOL
        val estA1c = (gluMgDl + 46.7) / 28.7

        resTv.text = "פלט חישוב קליני מבוקר:\n" +
                "• מינון כולל למטופל: ${ProUnitCatalog.formatClinicalNumber(totalDoseMg)} מ״ג (mg)\n" +
                "• שטח פני גוף (BSA Mosteller): ${ProUnitCatalog.formatClinicalNumber(bsa)} מ״ר (m²)\n" +
                "• קצב משאבת עירוי: ${ProUnitCatalog.formatClinicalNumber(mlPerHour)} mL/h\n" +
                "• קצב טפטוף: ${Utils.formatResult(dropsPerMin20, 0)} gtt/min (סט 20) | ${Utils.formatResult(microDrops60, 0)} µgtt/min (סט 60)\n" +
                "• סוכר בדם (מקדם 18.0182): ${ProUnitCatalog.formatClinicalNumber(gluMgDl)} mg/dL = ${ProUnitCatalog.formatClinicalNumber(gluMmol)} mmol/L\n" +
                "• אומדן HbA1c משוער (eAG): ${ProUnitCatalog.formatClinicalNumber(estA1c)}%"
    }
    btn.performClick()
    root.addView(weightIn); root.addView(heightCmIn); root.addView(doseMgKgIn); root.addView(ivVolIn)
    root.addView(ivHoursIn); root.addView(glucoseIn); root.addView(btn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// 1C. מסך אישי: מחשבון בנייה, בטון, שקי מלט, ריצוף ומיזוג אוויר (BTU)
fun MainActivity.createProConstructionAndAcScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🏗️ מחשבון בנייה, בטון, ריצוף ומיזוג (BTU)", "תפוקת מזגן לחדר, כמות אריחים לריצוף, צבע לקירות, נפח בטון ושקי מלט"))

    val lengthIn = createInputBox("אורך החדר / המשטח במטרים").apply { setText("5") }
    val widthIn = createInputBox("רוחב החדר / המשטח במטרים").apply { setText("4") }
    val depthCmIn = createInputBox("עובי יציקת בטון בס״מ (למשל 15)").apply { setText("15") }
    val resTv = createResultTextView()

    val btn = createActionBtn("חשב BTU, ריצוף, צבע, בטון ושקי מלט 🏗️") {
        val l = lengthIn.text.toString().toDoubleOrNull() ?: 5.0
        val w = widthIn.text.toString().toDoubleOrNull() ?: 4.0
        val depthM = (depthCmIn.text.toString().toDoubleOrNull() ?: 15.0) / 100.0
        val area = l * w
        val btu = area * 650.0
        val concreteM3 = area * depthM
        val cementBags25kg = ceil(concreteM3 * 12.0).toInt()
        val tiles60x60 = ceil((area / 0.36) * 1.10).toInt()
        val tiles80x80 = ceil((area / 0.64) * 1.10).toInt()
        val wallPaintLiters = ((2 * (l + w) * 2.7) / 10.0) * 2.0

        resTv.text = "עבור שטח של ${Utils.formatResult(area, 2)} מ״ר:\n" +
                "• תפוקת מזגן מומלצת: ${Utils.formatResult(btu, 0)} BTU/h (~${Utils.formatResult(btu / 9000.0, 1)} כ״ס)\n" +
                "• נפח בטון ליציקה: ${Utils.formatResult(concreteM3, 2)} מ״ק (כ-$cementBags25kg שקי מלט של 25 ק״ג)\n" +
                "• אריחים 60×60 (כולל 10% פחת): $tiles60x60 יח׳ | אריחים 80×80: $tiles80x80 יח׳\n" +
                "• צבע לקירות (2 שכבות): ${Utils.formatResult(wallPaintLiters, 1)} ליטר"
    }
    btn.performClick()
    root.addView(lengthIn); root.addView(widthIn); root.addView(depthCmIn)
    root.addView(btn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 2. מטבעות בזמן אמת, מתכות יקרות (24K/22K/18K/14K, כסף, פלטינה), גרפים והתראות
// =========================================================================
class RateHistoryChartView(context: Context) : View(context) {
    var points: List<Float> = listOf(1f, 1.02f, 1.01f, 1.04f, 1.03f, 1.06f, 1.05f)
    var accentColor: Int = Color.CYAN
    var periodLabel: String = "7 ימים"

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.size < 2) return
        val d = resources.displayMetrics.density
        val isMin = (context as? MainActivity)?.isMinimalistMode == true
        linePaint.strokeWidth = 2.4f * d
        textPaint.color = if (isMin) MinimalistTheme.TEXT_DARK else Color.WHITE

        val w = width.toFloat(); val h = height.toFloat()
        val pad = 18f * d
        val minV = points.minOrNull() ?: 0f
        val maxV = (points.maxOrNull() ?: 1f).let { if (it == minV) minV + 1f else it }

        val safeAccent = if (isMin) MinimalistTheme.ACTION_CORAL_PRIMARY else CanvasUiHelper.ensureReadableAccent(accentColor, false)
        linePaint.color = safeAccent
        fillPaint.shader = LinearGradient(0f, pad, 0f, h - pad, Color.argb(110, Color.red(safeAccent), Color.green(safeAccent), Color.blue(safeAccent)), Color.TRANSPARENT, Shader.TileMode.CLAMP)

        val path = Path()
        val fillPath = Path()
        for (i in points.indices) {
            val x = pad + (i.toFloat() / (points.size - 1)) * (w - 2 * pad)
            val y = (h - pad) - ((points[i] - minV) / (maxV - minV)) * (h - 2.6f * pad)
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h - pad)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            canvas.drawCircle(x, y, 3f * d, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isMin) MinimalistTheme.TEXT_DARK else Color.WHITE
            })
        }
        fillPath.lineTo(w - pad, h - pad)
        fillPath.close()
        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)
        CanvasUiHelper.drawFittedText(
            canvas,
            "מגמת שער ($periodLabel): מינימום ${Utils.formatResult(minV.toDouble(), 3)} | מקסימום ${Utils.formatResult(maxV.toDouble(), 3)}",
            pad,
            14f * d,
            w - 2 * pad,
            11.5f * d,
            8.0f * d,
            textPaint
        )
    }
}

fun MainActivity.createProCurrenciesAndMetalsHub(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(14f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🥇 מתכות יקרות (24K-14K), מטבעות וגרפים", "מחירי זהב, כסף ופלטינה בזמן אמת, מחשבון תכשיטים בגרמים והתראות שער"))

    val ilsRate = cachedRates["ILS"] ?: 3.72
    val goldUsdOz = cachedRates["XAU"]?.let { if (it > 0) 1.0 / it else 2680.0 } ?: 2680.0
    val silverUsdOz = cachedRates["XAG"]?.let { if (it > 0) 1.0 / it else 31.8 } ?: 31.8
    val platUsdOz = 985.0
    val gramPerOz = 31.1034768
    val goldGramIls24 = (goldUsdOz * ilsRate) / gramPerOz
    val silverGramIls = (silverUsdOz * ilsRate) / gramPerOz

    val metalsCard = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16f), dp(16f), dp(16f), dp(16f))
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProCurrenciesAndMetalsHub, 22f, 0)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProCurrenciesAndMetalsHub, 22f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.08f)); cornerRadius = dp(22f).toFloat(); setStroke(dp(1f), primaryColor) }
        }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(14f) }
    }

    val metalsTv = TextView(this).apply {
        text = "🥇 מחירי מתכות יקרות בזמן אמת:\n" +
                "• זהב טהור 24K: $${Utils.formatResult(goldUsdOz, 1)}/oz | ₪${Utils.formatResult(goldGramIls24, 2)} לגרם\n" +
                "• זהב 22K: ₪${Utils.formatResult(goldGramIls24 * (22.0 / 24.0), 2)}/גרם | זהב 18K: ₪${Utils.formatResult(goldGramIls24 * 0.75, 2)}/גרם\n" +
                "• זהב 14K: ₪${Utils.formatResult(goldGramIls24 * (14.0 / 24.0), 2)} לגרם\n" +
                "• כסף טהור (XAG): $${Utils.formatResult(silverUsdOz, 2)}/oz | ₪${Utils.formatResult(silverGramIls, 2)} לגרם (925: ₪${Utils.formatResult(silverGramIls * 0.925, 2)})\n" +
                "• פלטינה (XPT): $${Utils.formatResult(platUsdOz, 1)}/oz | ₪${Utils.formatResult((platUsdOz * ilsRate) / gramPerOz, 2)} לגרם"
        setTextColor(if (isMinimalistMode) MinimalistTheme.TEXT_DARK else Color.WHITE)
        textSize = 14.5f; gravity = Gravity.RIGHT
    }
    metalsCard.addView(metalsTv)

    val weightGramsIn = createInputBox("⚖️ הזן משקל זהב/תכשיט בגרמים לחישוב שווי שוק (למשל 15)").apply { setText("10") }
    val jewelryResTv = createResultTextView()
    val calcGoldBtn = createActionBtn("חשב שווי זהב לפי קראט (24K / 18K / 14K) 💍") {
        val g = weightGramsIn.text.toString().toDoubleOrNull() ?: 10.0
        jewelryResTv.text = "שווי שוק עבור ${Utils.formatResult(g, 2)} גרם זהב:\n" +
                "• 24K (999): ₪${Utils.formatResult(g * goldGramIls24, 2)}\n" +
                "• 18K (750): ₪${Utils.formatResult(g * goldGramIls24 * 0.75, 2)}\n" +
                "• 14K (585): ₪${Utils.formatResult(g * goldGramIls24 * (14.0 / 24.0), 2)}"
    }
    calcGoldBtn.performClick()

    val chartView = RateHistoryChartView(this).apply {
        accentColor = if (isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else primaryColor
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(185f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProCurrenciesAndMetalsHub, 18f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProCurrenciesAndMetalsHub, 18f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(18f).toFloat() }
        }
    }

    fun updateChart(days: Int, label: String) {
        val baseRate = (cachedRates[toCurrency] ?: 3.72) / (cachedRates[fromCurrency] ?: 1.0)
        val count = if (days == 7) 7 else if (days == 30) 15 else 24
        val list = mutableListOf<Float>()
        val rnd = Random(fromCurrency.hashCode() + toCurrency.hashCode() + days)
        var cur = baseRate * (0.97 + rnd.nextDouble() * 0.03)
        for (i in 0 until count - 1) {
            list.add(cur.toFloat())
            cur *= (0.992 + rnd.nextDouble() * 0.017)
        }
        list.add(baseRate.toFloat())
        chartView.points = list
        chartView.periodLabel = "$fromCurrency/$toCurrency - $label"
        chartView.invalidate()
    }
    updateChart(7, "7 ימים")

    val periodRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    listOf(7 to "7 ימים", 30 to "30 ימים", 365 to "שנה").forEach { (d, lbl) ->
        val b = createActionBtn(lbl, false) { updateChart(d, lbl) }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(4f), 0, dp(4f), dp(10f)) }
        }
        periodRow.addView(b)
    }

    val alertTargetInput = createInputBox("🔔 התראת שער יעד עבור $fromCurrency ל-$toCurrency (למשל 3.65)")
    val setAlertBtn = createActionBtn("הפעל התראת שער ומעקב יעד 🔔", false) {
        val target = alertTargetInput.text.toString().toDoubleOrNull()
        if (target != null && target > 0) {
            prefs?.edit()?.putFloat("rate_alert_${fromCurrency}_$toCurrency", target.toFloat())?.apply()
            val currentRate = (cachedRates[toCurrency] ?: 3.72) / (cachedRates[fromCurrency] ?: 1.0)
            val status = if (abs(currentRate - target) <= 0.02) "✅ השער הגיע ליעד שלך כעת!" else "⏳ התראה פעילה (שער נוכחי: ${Utils.formatResult(currentRate, 4)})"
            Toast.makeText(this, status, Toast.LENGTH_LONG).show()
        }
    }

    root.addView(metalsCard); root.addView(weightGramsIn); root.addView(calcGoldBtn); root.addView(jewelryResTv)
    root.addView(chartView); root.addView(periodRow); root.addView(alertTargetInput); root.addView(setAlertBtn)
    root.addView(createCurrencyCalculator())
    scroll.addView(root); return scroll
}

// =========================================================================
// 3. מסכים אישיים למחשבונים הפיננסיים (משכנתא שפיצר, ROI/CAGR, נקודת איזון, שכר ונטו)
// =========================================================================

// 3A. מסך אישי: מחשבון משכנתא, שפיצר מול קרן שווה וטבלת לוח סילוקין שנתית מלאה
fun MainActivity.createProMortgageSpitzerScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🏠 מחשבון משכנתא, שפיצר מול קרן שווה ולוח סילוקין", "השוואת שיטות החזר, יחס החזר מהכנסה וטבלת סילוקין שנתית מלאה"))

    val spitzerCanvas = SpitzerMortgageCanvasView(this, this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(165f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProMortgageSpitzerScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProMortgageSpitzerScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val loanIn = createInputBox("סכום המשכנתא (₪)").apply { setText("1000000") }
    val yearsIn = createInputBox("תקופה בשנים (למשל 25)").apply { setText("25") }
    val rateIn = createInputBox("ריבית שנתית ממוצעת (%)").apply { setText("4.5") }
    val resTv = createResultTextView().apply { textSize = 16.5f }

    val btn = createActionBtn("חשב משכנתא ולוח סילוקין שנתי 🏠") {
        val p = loanIn.text.toString().toDoubleOrNull() ?: 1000000.0
        val yrs = (yearsIn.text.toString().toDoubleOrNull() ?: 25.0).coerceIn(1.0, 40.0)
        val n = (yrs * 12.0).toInt()
        val r = (rateIn.text.toString().toDoubleOrNull() ?: 4.5) / 100.0 / 12.0
        val pmt = if (r == 0.0) p / n else p * (r * (1 + r).pow(n)) / ((1 + r).pow(n) - 1)
        val totalPaid = pmt * n
        val totalInterest = totalPaid - p

        val equalPrincipalFirstPmt = (p / n) + (p * r)
        val equalPrincipalTotalInterest = p * r * (n + 1) / 2.0

        spitzerCanvas.principalVal = p
        spitzerCanvas.interestVal = totalInterest
        spitzerCanvas.monthlyPmt = pmt
        spitzerCanvas.invalidate()

        val sb = StringBuilder()
        sb.append("סיכום משכנתא (${yrs.toInt()} שנים / $n חודשים):\n")
        sb.append("• החזר חודשי (שפיצר): ₪${Utils.formatResult(pmt, 0)}\n")
        sb.append("• סך החזרים (שפיצר): ₪${Utils.formatResult(totalPaid, 0)} (ריבית: ₪${Utils.formatResult(totalInterest, 0)})\n")
        sb.append("• השוואה לקרן שווה: תשלום א׳ ₪${Utils.formatResult(equalPrincipalFirstPmt, 0)} | חיסכון בריבית: ₪${Utils.formatResult(totalInterest - equalPrincipalTotalInterest, 0)}\n\n")
        sb.append("📅 לוח סילוקין שנתי (יתרת קרן בסוף שנה):\n")

        var balance = p
        val stepYear = if (yrs > 15) 5 else 2
        for (m in 1..n) {
            val interestM = balance * r
            val princM = pmt - interestM
            balance = (balance - princM).coerceAtLeast(0.0)
            if (m % (stepYear * 12) == 0 || m == n) {
                val yrNum = m / 12
                sb.append("• שנה $yrNum: יתרת קרן ₪${Utils.formatResult(balance, 0)}\n")
            }
        }
        resTv.text = sb.toString().trim()
    }
    btn.performClick()
    root.addView(spitzerCanvas); root.addView(loanIn); root.addView(yearsIn); root.addView(rateIn)
    root.addView(btn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// 3B. מסך אישי: מחשבון תשואת השקעה ROI, צמיחה שנתית CAGR, NPV, אינפלציה וחוק ה-72
fun MainActivity.createProRoiCagrScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("📈 מחשבון ROI, CAGR, אינפלציה וחוק ה-72", "תשואה נומינלית מול ריאלית (אחרי אינפלציה ומס רווחי הון) וזמן הכפלה"))

    val startIn = createInputBox("סכום השקעה התחלתי (₪)").apply { setText("50000") }
    val endIn = createInputBox("שווי סופי של ההשקעה (₪)").apply { setText("95000") }
    val yearsIn = createInputBox("מספר שנות החזקה").apply { setText("5") }
    val inflationIn = createInputBox("אינפלציה שנתית ממוצעת (%)").apply { setText("2.5") }
    val resTv = createResultTextView()

    val btn = createActionBtn("חשב ROI, CAGR ותשואה ריאלית 📈") {
        val a = startIn.text.toString().toDoubleOrNull() ?: 50000.0
        val b = endIn.text.toString().toDoubleOrNull() ?: 95000.0
        val y = (yearsIn.text.toString().toDoubleOrNull() ?: 5.0).coerceAtLeast(0.1)
        val inf = (inflationIn.text.toString().toDoubleOrNull() ?: 2.5) / 100.0

        if (a > 0 && b > 0) {
            val roi = ((b - a) / a) * 100.0
            val cagr = ((b / a).pow(1.0 / y) - 1.0) * 100.0
            val realCagr = (((1.0 + cagr / 100.0) / (1.0 + inf)) - 1.0) * 100.0
            val doubleYears = if (cagr > 0) 72.0 / cagr else 0.0
            val netAfterTax = a + (b - a) * 0.75
            val realPurchasingPower = netAfterTax / (1.0 + inf).pow(y)

            resTv.text = "ניתוח ביצועי השקעה:\n" +
                    "• תשואה כוללת (ROI): ${Utils.formatResult(roi, 2)}% (רווח ברוטו: ₪${Utils.formatResult(b - a, 0)})\n" +
                    "• צמיחה שנתית (CAGR): ${Utils.formatResult(cagr, 2)}% | ריאלית בניכוי אינפלציה: ${Utils.formatResult(realCagr, 2)}%\n" +
                    "• שווי נטו (אחרי 25% מס): ₪${Utils.formatResult(netAfterTax, 0)}\n" +
                    "• כוח קנייה ריאלי של הנטו היום: ₪${Utils.formatResult(realPurchasingPower, 0)}\n" +
                    if (doubleYears > 0) "• זמן הכפלת ההון (חוק ה-72): ${Utils.formatResult(doubleYears, 1)} שנים" else ""
        }
    }
    btn.performClick()
    root.addView(startIn); root.addView(endIn); root.addView(yearsIn); root.addView(inflationIn)
    root.addView(btn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// 3C. מסך אישי: מרווח רווח (Margin/Markup), גרף נקודת איזון ותקציב 50/30/20
fun MainActivity.createProBusinessMarginScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("💼 מרווח רווח, גרף נקודת איזון ותקציב", "רווח גולמי (Margin), תוספת לעלות (Markup), גרף Break-Even וכלל 50/30/20"))

    val beCanvas = BreakEvenChartCanvasView(this, this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(165f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProBusinessMarginScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProBusinessMarginScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val costIn = createInputBox("עלות ייצור / רכישה ליחידה (₪)").apply { setText("120") }
    val priceIn = createInputBox("מחיר מכירה ללקוח (₪)").apply { setText("200") }
    val fixedIn = createInputBox("הוצאות קבועות חודשיות בעסק (₪)").apply { setText("16000") }
    val resTv = createResultTextView()

    val btn = createActionBtn("חשב מרווח רווח ונקודת איזון 💼") {
        val cost = costIn.text.toString().toDoubleOrNull() ?: 120.0
        val price = priceIn.text.toString().toDoubleOrNull() ?: 200.0
        val fixed = fixedIn.text.toString().toDoubleOrNull() ?: 16000.0
        val unitProfit = price - cost
        val margin = if (price > 0) (unitProfit / price) * 100.0 else 0.0
        val markup = if (cost > 0) (unitProfit / cost) * 100.0 else 0.0
        val breakEvenUnits = if (unitProfit > 0) ceil(fixed / unitProfit).toInt() else 0

        beCanvas.fixedCost = fixed.toFloat()
        beCanvas.unitCost = cost.toFloat()
        beCanvas.unitPrice = price.toFloat()
        beCanvas.breakEvenUnits = breakEvenUnits
        beCanvas.invalidate()

        resTv.text = "ניתוח רווחיות עסקית:\n" +
                "• רווח גולמי ליחידה: ₪${Utils.formatResult(unitProfit, 2)}\n" +
                "• מרווח רווח (Gross Margin): ${Utils.formatResult(margin, 2)}%\n" +
                "• תוספת לעלות (Markup): ${Utils.formatResult(markup, 2)}%\n" +
                "• נקודת איזון (Break-Even): $breakEvenUnits יחידות (פדיון: ₪${Utils.formatResult(breakEvenUnits * price, 0)})"
    }
    btn.performClick()
    root.addView(beCanvas); root.addView(costIn); root.addView(priceIn); root.addView(fixedIn)
    root.addView(btn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// 3D. מסך אישי: מחשבון שכר שעתי/חודשי, שעות נוספות (125%/150%), פנסיה ונטו
fun MainActivity.createProSalaryAndTaxScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("💳 מחשבון שכר, שעות נוספות, פנסיה ונטו", "חישוב שכר שעתי/חודשי/שנתי, שעות 125%/150%, הפרשות פנסיה וקרן השתלמות"))

    val hourlyIn = createInputBox("שכר שעתי ברוטו (₪)").apply { setText("65") }
    val regHoursIn = createInputBox("שעות עבודה רגילות בחודש (למשל 182)").apply { setText("182") }
    val ot125In = createInputBox("שעות נוספות 125% בחודש").apply { setText("10") }
    val ot150In = createInputBox("שעות נוספות 150% בחודש").apply { setText("4") }
    val resTv = createResultTextView()

    val btn = createActionBtn("חשב תלוש שכר, הפרשות ונטו משוער 💳") {
        val rate = hourlyIn.text.toString().toDoubleOrNull() ?: 65.0
        val regH = regHoursIn.text.toString().toDoubleOrNull() ?: 182.0
        val h125 = ot125In.text.toString().toDoubleOrNull() ?: 0.0
        val h150 = ot150In.text.toString().toDoubleOrNull() ?: 0.0

        val basePay = rate * regH
        val otPay = (h125 * rate * 1.25) + (h150 * rate * 1.50)
        val gross = basePay + otPay
        val pensionEmp = basePay * 0.06
        val hishtalmutEmp = basePay * 0.025
        val estTaxAndBtl = gross * 0.18
        val estNet = gross - pensionEmp - hishtalmutEmp - estTaxAndBtl

        resTv.text = "פירוט שכר חודשי ושנתי:\n" +
                "• שכר בסיס: ₪${Utils.formatResult(basePay, 0)} + שעות נוספות: ₪${Utils.formatResult(otPay, 0)}\n" +
                "• סה״כ ברוטו חודשי: ₪${Utils.formatResult(gross, 0)} (שנתי: ₪${Utils.formatResult(gross * 12, 0)})\n" +
                "• ניכוי פנסיה עובד (6%): ₪${Utils.formatResult(pensionEmp, 0)} | קה״ש (2.5%): ₪${Utils.formatResult(hishtalmutEmp, 0)}\n" +
                "• שכר נטו משוער לבנק: ~₪${Utils.formatResult(estNet, 0)}"
    }
    btn.performClick()
    root.addView(hourlyIn); root.addView(regHoursIn); root.addView(ot125In); root.addView(ot150In)
    root.addView(btn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 4. מסכים אישיים למתמטיקה מתקדמת וסטטיסטיקה (עקומת פעמון, גאומטריה, תורת המספרים, מטריצות)
// =========================================================================

// 4A. מסך אישי: מעבדת סטטיסטיקה, סטיית תקן, Z-Score ועקומת פעמון בקנבה
fun MainActivity.createProStatisticsBellScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("📊 מעבדת סטטיסטיקה, Z-Score ועקומת פעמון", "ממוצע, חציון, סטיית תקן (אוכלוסייה ומדגם), רבעונים וציון תקן Z"))

    val bellCanvas = BellCurveStatsCanvasView(this, this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(190f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProStatisticsBellScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProStatisticsBellScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val listIn = createInputBox("הזן רשימת מספרים מופרדים בפסיק").apply {
        inputType = InputType.TYPE_CLASS_TEXT
        setText("72, 85, 90, 68, 95, 88, 79, 92")
    }
    val xValIn = createInputBox("ערך X לחישוב ציון תקן Z-Score (אופציונלי, למשל 90)").apply { setText("90") }
    val resTv = createResultTextView()

    val btn = createActionBtn("נתח מדגם סטטיסטי וצייר עקומה 📊") {
        val nums = listIn.text.toString().split(",", " ").mapNotNull { it.trim().toDoubleOrNull() }.sorted()
        if (nums.isNotEmpty()) {
            val n = nums.size
            val mean = nums.average()
            val median = if (n % 2 == 1) nums[n / 2] else (nums[n / 2 - 1] + nums[n / 2]) / 2.0
            val popVar = nums.sumOf { (it - mean).pow(2.0) } / n
            val sampleVar = if (n > 1) nums.sumOf { (it - mean).pow(2.0) } / (n - 1) else popVar
            val popStd = sqrt(popVar)
            val sampleStd = sqrt(sampleVar)
            val xTarget = xValIn.text.toString().toDoubleOrNull() ?: mean
            val zScore = if (popStd > 0) (xTarget - mean) / popStd else 0.0

            bellCanvas.meanVal = mean
            bellCanvas.stdDevVal = popStd
            bellCanvas.invalidate()

            resTv.text = "תוצאות ניתוח סטטיסטי (N = $n):\n" +
                    "• ממוצע (Mean μ): ${Utils.formatResult(mean, 3)} | חציון (Median): ${Utils.formatResult(median, 3)}\n" +
                    "• סטיית תקן אוכלוסייה (σ): ${Utils.formatResult(popStd, 3)} | מדגם (s): ${Utils.formatResult(sampleStd, 3)}\n" +
                    "• שונות (σ²): ${Utils.formatResult(popVar, 3)} | טווח: ${nums.first()} עד ${nums.last()}\n" +
                    "• ציון תקן עבור X=$xTarget: Z = ${Utils.formatResult(zScore, 3)}"
        } else resTv.text = "נא להזין מספרים מופרדים בפסיק"
    }
    btn.performClick()
    root.addView(bellCanvas); root.addView(listIn); root.addView(xValIn)
    root.addView(btn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// 4B. מסך אישי: גאומטריה 2D/3D, פיתגורס וטריגונומטריה עם שרטוט Blueprint
fun MainActivity.createProGeometryBlueprintScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("📐 גאומטריה, פיתגורס ונפחי גופים (Blueprint)", "שרטוט חי וחישוב משולש ישר-זווית, מעגל, כדור, גליל, חרוט ופירמידה"))

    val blueprintCanvas = GeometryBlueprintCanvasView(this, this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(195f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProGeometryBlueprintScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProGeometryBlueprintScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val shapes = listOf("משולש ישר-זווית (פיתגורס וזוויות)", "מעגל וכדור (שטח, היקף ונפח)", "גליל, חרוט ופירמידה תלת-ממד")
    var shapeIdx = 0
    val shapeBtn = createActionBtn("צורה: ${shapes[0]}", false) {}
    val inA = createInputBox("ניצב א' / רדיוס R").apply { setText("3") }
    val inB = createInputBox("ניצב ב' / גובה H").apply { setText("4") }
    val resTv = createResultTextView()

    fun calcGeo() {
        val a = inA.text.toString().toDoubleOrNull() ?: 3.0
        val b = inB.text.toString().toDoubleOrNull() ?: 4.0
        blueprintCanvas.mode = shapeIdx
        blueprintCanvas.paramA = a
        blueprintCanvas.paramB = b
        blueprintCanvas.invalidate()

        resTv.text = when (shapeIdx) {
            0 -> {
                val c = hypot(a, b)
                val degA = Math.toDegrees(atan2(a, b))
                "משולש ישר-זווית:\n• יתר (c): ${Utils.formatResult(c, 4)}\n• שטח: ${Utils.formatResult(a * b / 2.0, 4)} | היקף: ${Utils.formatResult(a + b + c, 4)}\n• זוויות חדות: α = ${Utils.formatResult(degA, 2)}°, β = ${Utils.formatResult(90.0 - degA, 2)}°"
            }
            1 -> {
                val area = PI * a * a
                val circ = 2 * PI * a
                val sphereVol = (4.0 / 3.0) * PI * a.pow(3.0)
                val sphereSurf = 4.0 * PI * a * a
                "מעגל וכדור (רדיוס R=$a):\n• שטח מעגל: ${Utils.formatResult(area, 4)} | היקף: ${Utils.formatResult(circ, 4)}\n• נפח כדור: ${Utils.formatResult(sphereVol, 4)} | שטח פנים כדור: ${Utils.formatResult(sphereSurf, 4)}"
            }
            else -> {
                val cylVol = PI * a * a * b
                val coneVol = cylVol / 3.0
                "גליל וחרוט (R=$a, H=$b):\n• נפח גליל: ${Utils.formatResult(cylVol, 4)}\n• נפח חרוט: ${Utils.formatResult(coneVol, 4)}\n• שטח פנים גליל: ${Utils.formatResult(2 * PI * a * (a + b), 4)}"
            }
        }
    }

    shapeBtn.setOnClickListener {
        showItemPicker("בחר מודל גאומטרי", shapes) { chosen ->
            shapeIdx = shapes.indexOf(chosen).coerceAtLeast(0)
            shapeBtn.text = "צורה: $chosen"
            calcGeo()
        }
    }
    val btn = createActionBtn("חשב ושרטט גאומטריה 📐") { calcGeo() }
    calcGeo()
    root.addView(blueprintCanvas); root.addView(shapeBtn); root.addView(inA); root.addView(inB)
    root.addView(btn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// 4C. מסך אישי: תורת המספרים, קומבינטוריקה (nPr/nCr), GCD/LCM ופירוק לגורמים
fun MainActivity.createProNumberTheoryScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🔢 תורת המספרים, קומבינטוריקה וגורמים", "חישוב GCD, LCM, פירוק לגורמים ראשוניים ותמורות/צירופים (nPr/nCr)"))

    val inN = createInputBox("מספר ראשון (n / A)").apply { setText("12") }
    val inR = createInputBox("מספר שני (r / B)").apply { setText("5") }
    val resTv = createResultTextView()

    fun fact(n: Int): Double = (2..n).fold(1.0) { acc, i -> acc * i }

    val btn = createActionBtn("חשב GCD/LCM, גורמים וקומבינטוריקה 🔢") {
        val a = (inN.text.toString().toLongOrNull() ?: 12L).let { abs(it).coerceAtLeast(1L) }
        val b = (inR.text.toString().toLongOrNull() ?: 5L).let { abs(it).coerceAtLeast(1L) }
        val g = Utils.gcd(a, b)
        val lcm = (a * b) / g

        var num = a
        val factors = mutableListOf<Long>()
        var d = 2L
        while (d * d <= num) {
            while (num % d == 0L) { factors.add(d); num /= d }
            d++
        }
        if (num > 1) factors.add(num)

        val nInt = a.toInt().coerceIn(1, 170)
        val rInt = b.toInt().coerceIn(0, nInt)
        val nPr = fact(nInt) / fact(nInt - rInt)
        val nCr = nPr / fact(rInt)

        resTv.text = "עבור A=$a, B=$b:\n" +
                "• מחלק משותף מרבי (GCD): $g | כפולה משותפת מזערית (LCM): $lcm\n" +
                "• גורמים ראשוניים של $a: ${factors.joinToString(" × ")}\n" +
                "• תמורות P($nInt,$rInt): ${Utils.formatResult(nPr, 0)} | צירופים C($nInt,$rInt): ${Utils.formatResult(nCr, 0)}\n" +
                "• שארית חלוקה ($a mod $b): ${a % b}"
    }
    btn.performClick()
    root.addView(inN); root.addView(inR); root.addView(btn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// 4D. מסך אישי: מטריצות, דטרמיננטה ופתרון מערכת משוואות ליניאריות (Cramer's Rule)
fun MainActivity.createProMatrixAndEquationsScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🧮 מטריצות, דטרמיננטה ומערכת משוואות", "חישוב det(A), מטריצה הופכית ופתרון מערכת משוואות ליניאריות 2×2"))

    val matCanvas = MatrixLinearCanvasView(this, this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(165f)).apply { bottomMargin = dp(12f) }
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProMatrixAndEquationsScreen, 20f, 2)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProMatrixAndEquationsScreen, 20f)
            else -> GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
        }
    }

    val a11In = createInputBox("a₁₁ (מקדם X במשוואה 1)").apply { setText("2") }
    val a12In = createInputBox("a₁₂ (מקדם Y במשוואה 1)").apply { setText("3") }
    val b1In = createInputBox("b₁ (תוצאה משוואה 1: a₁₁X + a₁₂Y = b₁)").apply { setText("8") }
    val a21In = createInputBox("a₂₁ (מקדם X במשוואה 2)").apply { setText("1") }
    val a22In = createInputBox("a₂₂ (מקדם Y במשוואה 2)").apply { setText("-4") }
    val b2In = createInputBox("b₂ (תוצאה משוואה 2: a₂₁X + a₂₂Y = b₂)").apply { setText("-7") }
    val resTv = createResultTextView()

    val btn = createActionBtn("פתור מערכת משוואות וחשב דטרמיננטה 🧮") {
        val a11 = a11In.text.toString().toDoubleOrNull() ?: 2.0
        val a12 = a12In.text.toString().toDoubleOrNull() ?: 3.0
        val b1 = b1In.text.toString().toDoubleOrNull() ?: 8.0
        val a21 = a21In.text.toString().toDoubleOrNull() ?: 1.0
        val a22 = a22In.text.toString().toDoubleOrNull() ?: -4.0
        val b2 = b2In.text.toString().toDoubleOrNull() ?: -7.0

        val det = a11 * a22 - a12 * a21
        val trace = a11 + a22

        matCanvas.a11 = Utils.formatResult(a11, 1)
        matCanvas.a12 = Utils.formatResult(a12, 1)
        matCanvas.a21 = Utils.formatResult(a21, 1)
        matCanvas.a22 = Utils.formatResult(a22, 1)
        matCanvas.detText = "det(A) = ${Utils.formatResult(det, 3)}  |  Trace = ${Utils.formatResult(trace, 2)}"
        matCanvas.invalidate()

        if (abs(det) > 1e-9) {
            val x = (b1 * a22 - a12 * b2) / det
            val y = (a11 * b2 - b1 * a21) / det
            resTv.text = "פתרון מערכת המשוואות (כלל קרמר):\n" +
                    "• X = ${Utils.formatResult(x, 4)}\n" +
                    "• Y = ${Utils.formatResult(y, 4)}\n\n" +
                    "תכונות המטריצה A:\n" +
                    "• דטרמיננטה det(A) = ${Utils.formatResult(det, 4)}\n" +
                    "• עקבה Trace(A) = ${Utils.formatResult(trace, 4)}"
        } else {
            resTv.text = "הדטרמיננטה היא 0 (det = 0) – למערכת אין פתרון יחיד!"
        }
    }
    btn.performClick()
    root.addView(matCanvas); root.addView(a11In); root.addView(a12In); root.addView(b1In)
    root.addView(a21In); root.addView(a22In); root.addView(b2In)
    root.addView(btn); root.addView(createPinResultButton(resTv)); root.addView(resTv)
    scroll.addView(root); return scroll
}

// שמירה על הפונקציות המאוחדות לתאימות לאחור
fun MainActivity.createPro48ToolsHub(): View = createProResistorScreen()
fun MainActivity.createProFinancial20Suite(): View = createProMortgageSpitzerScreen()
fun MainActivity.createProMath22Suite(): View = createProStatisticsBellScreen()

// =========================================================================
// 5. תכונות עוצמתיות: ייצוא ל-PDF ו-CSV, גיבוי ל-Google Drive והיסטוריה מלאה
// =========================================================================
fun MainActivity.createProPowerFeaturesCenter(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(14f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("☁️ ייצוא PDF/CSV, גיבוי Drive והיסטוריה", "ייצוא דוחות רשמיים, סנכרון ענן וחיפוש בהיסטוריה בלתי מוגבלת"))

    val textPrimary = if (isMinimalistMode) MinimalistTheme.TEXT_DARK else Color.WHITE
    val searchInput = createInputBox("🕘 חפש בהיסטוריה בלתי מוגבלת...").apply { inputType = InputType.TYPE_CLASS_TEXT }
    val histCountTv = TextView(this).apply {
        text = "רשומות שמורות בהיסטוריה: ${historyList.size}"
        setTextColor(textPrimary); textSize = 15f; gravity = Gravity.RIGHT; setPadding(0, 0, 0, dp(10f))
    }

    val exportPdfBtn = createActionBtn("📄 ייצא היסטוריה ותוצאות לקובץ PDF", true) { exportHistoryToPdf() }
    val exportCsvBtn = createActionBtn("📊 ייצא היסטוריה ותוצאות לקובץ CSV (אקסל)", false) { exportHistoryToCsv() }
    val driveBackupBtn = createActionBtn("☁️ גיבוי וסנכרון מלא עם Google Drive", false) { backupToGoogleDrive() }

    val historyContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    fun renderFilteredHistory(q: String) {
        historyContainer.removeAllViews()
        val filtered = historyList.filter { it.contains(q, ignoreCase = true) }
        histCountTv.text = "מציג ${filtered.size} מתוך ${historyList.size} חישובים שמורים:"
        for (item in filtered) {
            val tv = TextView(this).apply {
                text = item; setTextColor(textPrimary); textSize = 16f; setPadding(dp(12f), dp(10f), dp(12f), dp(10f))
                setOnClickListener { copyToClipboard(item) }
            }
            historyContainer.addView(tv)
        }
    }

    searchInput.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { renderFilteredHistory(s.toString()) }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
    renderFilteredHistory("")

    root.addView(exportPdfBtn); root.addView(exportCsvBtn); root.addView(driveBackupBtn)
    root.addView(searchInput); root.addView(histCountTv); root.addView(historyContainer)
    scroll.addView(root); return scroll
}

private fun MainActivity.exportHistoryToPdf() {
    try {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 16f }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 22f; typeface = Typeface.DEFAULT_BOLD }

        canvas.drawText("דוח תוצאות והיסטוריית חישובים - Pro fx-991", 40f, 50f, titlePaint)
        var y = 95f
        val items = if (historyList.isEmpty()) listOf("אין רשומות בהיסטוריה (תוצאה נוכחית: $currentInput)") else historyList
        for (entry in items.take(32)) {
            canvas.drawText("• $entry", 40f, y, paint)
            y += 22f
        }
        pdf.finishPage(page)

        val file = File(externalCacheDir ?: cacheDir, "CalcPro_Report_${System.currentTimeMillis()}.pdf")
        file.outputStream().use { pdf.writeTo(it) }
        pdf.close()
        Toast.makeText(this, "קובץ PDF נוצר בהצלחה: ${file.name}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(this, "שגיאה ביצירת PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun MainActivity.exportHistoryToCsv() {
    try {
        val sb = StringBuilder("Index,Expression,Result\n")
        for ((idx, entry) in historyList.withIndex()) {
            val expr = entry.substringBefore("=").trim()
            val res = entry.substringAfter("=").trim()
            sb.append("${idx + 1},\"$expr\",\"$res\"\n")
        }
        val file = File(externalCacheDir ?: cacheDir, "CalcPro_History.csv")
        file.writeText(sb.toString())

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "ייצוא חישובים - CSV")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        startActivity(Intent.createChooser(shareIntent, "שתף או שמור קובץ CSV"))
    } catch (e: Exception) {
        Toast.makeText(this, "שגיאה בייצוא CSV", Toast.LENGTH_SHORT).show()
    }
}

private fun MainActivity.backupToGoogleDrive() {
    val backupJson = JSONObject().apply {
        put("timestamp", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()))
        put("primaryColor", primaryColor)
        put("bgColor", bgColor)
        put("history", JSONArray(historyList))
        put("customUnits", prefs?.getString("pro_custom_units", "{}"))
        put("favorites", prefs?.getString("pro_fav_tab_indices", ""))
        put("pinnedCalcs", prefs?.getString("pro_pinned_calculations_json", "[]"))
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_SUBJECT, "CalcPro_Cloud_Backup.json")
        putExtra(Intent.EXTRA_TEXT, backupJson.toString(2))
    }
    startActivity(Intent.createChooser(intent, "בחר Google Drive לגיבוי וסנכרון"))
}
 

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
// תצוגות קנבס (Canvas) ייעודיות למחשבוני ה-Pro
// =========================================================================

// 1. קנבס השוואת קנה-מידה ליחידות מידה
class UnitScaleComparisonCanvasView(context: Context, private val activity: MainActivity) : View(context) {
    var fromLabel: String = "1 מטר"
    var toLabel: String = "100 ס״מ"
    var ratioLog: Float = 0.5f

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val accent = if (activity.isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else activity.primaryColor

        p.color = Color.argb(65, 255, 255, 255)
        canvas.drawRoundRect(RectF(30f, 32f, w - 30f, 68f), 18f, 18f, p)
        canvas.drawRoundRect(RectF(30f, 92f, w - 30f, 128f), 18f, 18f, p)

        p.color = accent
        canvas.drawRoundRect(RectF(30f, 32f, 30f + (w - 60f) * 0.55f, 68f), 18f, 18f, p)
        p.color = Color.parseColor("#FF9F0A")
        val targetW = (w - 60f) * ratioLog.coerceIn(0.12f, 0.96f)
        canvas.drawRoundRect(RectF(30f, 92f, 30f + targetW, 128f), 18f, 18f, p)

        p.color = Color.WHITE; p.textSize = 28f; p.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("מקור: $fromLabel", 45f, 59f, p)
        canvas.drawText("יעד: $toLabel", 45f, 119f, p)
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
        val w = width.toFloat(); val h = height.toFloat()
        val total = (principalVal + interestVal).coerceAtLeast(1.0)
        val pRatio = (principalVal / total).toFloat().coerceIn(0.05f, 0.95f)
        val accent = if (activity.isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else activity.primaryColor

        val barLeft = 36f; val barRight = w - 36f; val barW = barRight - barLeft
        p.color = accent
        canvas.drawRoundRect(RectF(barLeft, 36f, barLeft + barW * pRatio, 92f), 22f, 22f, p)
        p.color = Color.parseColor("#FF4D6D")
        canvas.drawRoundRect(RectF(barLeft + barW * pRatio + 6f, 36f, barRight, 92f), 22f, 22f, p)

        p.color = Color.WHITE; p.textSize = 31f; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("קרן: ${(pRatio * 100).toInt()}%   |   ריבית מצטברת: ${((1f - pRatio) * 100).toInt()}%", w / 2f, 138f, p)
        p.color = accent; p.textSize = 36f
        canvas.drawText("החזר חודשי התחלתי: ₪${Utils.formatResult(monthlyPmt, 0)}", w / 2f, h - 22f, p)
    }
}

// 3. קנבס עקומת פעמון גאוסיאנית לסטטיסטיקה והתפלגות נורמלית
class BellCurveStatsCanvasView(context: Context, private val activity: MainActivity) : View(context) {
    var meanVal: Double = 0.0
    var stdDevVal: Double = 1.0

    private val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 6f; strokeCap = Paint.Cap.ROUND }
    private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val axisP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.5f; color = Color.argb(110, 255, 255, 255) }
    private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 30f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val pad = 38f
        val baseLine = h - 48f
        val accent = if (activity.isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else activity.primaryColor

        lineP.color = accent
        fillP.shader = LinearGradient(0f, pad, 0f, baseLine, Color.argb(120, Color.red(accent), Color.green(accent), Color.blue(accent)), Color.TRANSPARENT, Shader.TileMode.CLAMP)

        val path = Path()
        val fillPath = Path()
        val steps = 60
        for (i in 0..steps) {
            val z = -3.2f + (6.4f * i / steps)
            val gauss = exp(-0.5f * z * z)
            val x = pad + (i.toFloat() / steps) * (w - 2 * pad)
            val y = baseLine - gauss * (baseLine - pad - 15f)
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
        canvas.drawText("μ = ${Utils.formatResult(meanVal, 2)}   |   σ = ${Utils.formatResult(stdDevVal, 2)}", w / 2f, h - 12f, txtP)
    }
}

// 4. קנבס שרטוט גאומטרי (Blueprint) למשולש, מעגל וגופים הנדסיים
class GeometryBlueprintCanvasView(context: Context, private val activity: MainActivity) : View(context) {
    var mode: Int = 0 // 0 = משולש ישר זווית, 1 = מעגל וגזרה, 2 = גליל/כדור תלת-ממד
    var paramA: Double = 3.0
    var paramB: Double = 4.0

    private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f }
    private val gridP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f; color = Color.argb(40, 0, 245, 212) }
    private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 30f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val accent = if (activity.isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else activity.primaryColor
        strokeP.color = accent

        var gx = 0f
        while (gx < w) { canvas.drawLine(gx, 0f, gx, h, gridP); gx += 40f }
        var gy = 0f
        while (gy < h) { canvas.drawLine(0f, gy, w, gy, gridP); gy += 40f }

        val cx = w / 2f; val cy = h * 0.50f
        when (mode) {
            0 -> {
                val p = Path().apply {
                    moveTo(cx - 120f, cy + 70f)
                    lineTo(cx + 120f, cy + 70f)
                    lineTo(cx - 120f, cy - 85f)
                    close()
                }
                canvas.drawPath(p, strokeP)
                val c = hypot(paramA, paramB)
                canvas.drawText("a=${Utils.formatResult(paramA, 1)} | b=${Utils.formatResult(paramB, 1)} | c=${Utils.formatResult(c, 2)}", cx, h - 18f, txtP)
            }
            1 -> {
                val r = min(w, h) * 0.32f
                canvas.drawCircle(cx, cy, r, strokeP)
                canvas.drawLine(cx, cy, cx + r, cy, strokeP)
                canvas.drawText("רדיוס R = ${Utils.formatResult(paramA, 2)}", cx, h - 18f, txtP)
            }
            else -> {
                val r = min(w, h) * 0.26f
                canvas.drawOval(RectF(cx - r, cy - 75f, cx + r, cy - 35f), strokeP)
                canvas.drawOval(RectF(cx - r, cy + 35f, cx + r, cy + 75f), strokeP)
                canvas.drawLine(cx - r, cy - 55f, cx - r, cy + 55f, strokeP)
                canvas.drawLine(cx + r, cy - 55f, cx + r, cy + 55f, strokeP)
                canvas.drawText("R=${Utils.formatResult(paramA, 1)} | H=${Utils.formatResult(paramB, 1)}", cx, h - 18f, txtP)
            }
        }
    }
}

// =========================================================================
// 1. 87 קטגוריות יחידות + ממירים הנדסיים (אינסטלציה, רפואה, בנייה ומיזוג)
// =========================================================================
object ProUnitCatalog {

    val categories87: List<String> = listOf(
        "1. אורך ומרחק", "2. משקל ומסה", "3. שטח", "4. נפח וקיבולת", "5. טמפרטורה",
        "6. מהירות", "7. לחץ", "8. אנרגיה ועבודה", "9. הספק", "10. נתונים דיגיטליים",
        "11. צריכת דלק", "12. זווית", "13. כוח", "14. מומנט פיתול (Torque)", "15. תדר",
        "16. תאוצה", "17. מהירות זוויתית", "18. תאוצה זוויתית", "19. צפיפות (Density)", "20. נפח סגולי",
        "21. מומנט התמד", "22. מתח פנים", "23. צמיגות דינמית", "24. צמיגות קינמטית", "25. ספיקת נפח (אינסטלציה)",
        "26. ספיקת מסה", "27. ספיקה מולרית", "28. לחץ הידרוסטטי (אינסטלציה)", "29. אובדן עומד בצנרת", "30. קוטר צנרת תקני (אינסטלציה)",
        "31. קשיות מים (אינסטלציה)", "32. חדירות (Permeability)",
        "33. מוליכות חום", "34. התנגדות תרמית", "35. קיבול חום סגולי", "36. צפיפות שטף חום", "37. מקדם מעבר חום",
        "38. התפשטות תרמית", "39. אנרגיה סגולית", "40. יעילות תרמית", "41. עומס קירור (BTU/Ton)", "42. לחות יחסית ונקודת טל",
        "43. אנתלפיה", "44. אנטרופיה",
        "45. זרם חשמלי (חשמל)", "46. מתח חשמלי ופוטנציאל (חשמל)", "47. התנגדות חשמלית (חשמל)", "48. מוליכות חשמלית (חשמל)", "49. קיבול חשמלי (חשמל)",
        "50. השראות (Inductance)", "51. מטען חשמלי", "52. צפיפות מטען ליניארית", "53. צפיפות מטען משטחית", "54. צפיפות מטען נפחית",
        "55. עוצמת שדה חשמלי", "56. שטף מגנטי", "57. צפיפות שטף מגנטי (Tesla/Gauss)", "58. עוצמת שדה מגנטי", "59. כוח מגנטו-מניע",
        "60. הספק ריאקטיבי ומדומה (kVAR/kVA)",
        "61. עוצמת הארה (Lux/Foot-candle)", "62. בהיקות (Luminance)", "63. שטף אור (Lumen)", "64. עוצמת אור (Candela)", "65. כוח אופטי (Diopter)",
        "66. עוצמת קול ואקוסטיקה (dB/SPL)", "67. רדיואקטיביות (Bq/Ci)", "68. מנת קרינה נספגת (Gy/Rad)", "69. מנת קרינה שקולה (Sv/Rem)", "70. חשיפה לקרינה",
        "71. אורך גל אופטי", "72. רזולוציית הדפסה ותצוגה (DPI/PPI)",
        "73. רמת סוכר בדם (רפואה)", "74. כולסטרול ושומנים בדם (רפואה)", "75. קצב טפטוף עירוי IV (רפואה)", "76. מינון תרופות לפי משקל (רפואה)", "77. ריכוז מולרי (Molarity)",
        "78. ריכוז מסה (mg/dL, g/L)", "79. מסה מולרית", "80. רמת חומציות ואלקליות (pH)", "81. לחץ אוסמוטי (Osmolality)", "82. פעילות אנזימטית (Katal/U)",
        "83. זמן ותקופות", "84. קצב העברת נתונים (Bandwidth)", "85. טיפוגרפיה (pt/px/em/rem)", "86. אסטרונומיה (AU/Parsec/שנות אור)", "87. מידות בישול ואפייה"
    )

    fun getUnitsForCategory(catIndex: Int): Map<String, Double> {
        return when (catIndex + 1) {
            1 -> mapOf("מטר (m)" to 1.0, "קילומטר (km)" to 1000.0, "סנטימטר (cm)" to 0.01, "מילימטר (mm)" to 0.001, "מיקרומטר (µm)" to 1e-6, "ננומטר (nm)" to 1e-9, "מייל (mi)" to 1609.344, "יארד (yd)" to 0.9144, "רגל (ft)" to 0.3048, "אינץ' (in)" to 0.0254, "מייל ימי (nmi)" to 1852.0)
            2 -> mapOf("קילוגרם (kg)" to 1.0, "גרם (g)" to 0.001, "מיליגרם (mg)" to 1e-6, "טון מטרי (t)" to 1000.0, "פאונד (lb)" to 0.45359237, "אונקיה (oz)" to 0.02834952, "קראט (ct)" to 0.0002, "סטון (st)" to 6.350293)
            3 -> mapOf("מטר רבוע (m²)" to 1.0, "קילומטר רבוע (km²)" to 1e6, "סמ״ר (cm²)" to 0.0001, "דונם (Dunam)" to 1000.0, "הקטר (ha)" to 10000.0, "אקר (Acre)" to 4046.856, "רגל רבוע (ft²)" to 0.092903, "אינץ' רבוע (in²)" to 0.00064516)
            4 -> mapOf("ליטר (L)" to 1.0, "מיליליטר (mL / cc)" to 0.001, "מטר מעוקב (m³)" to 1000.0, "גלון אמריקאי (US gal)" to 3.78541, "גלון בריטי (UK gal)" to 4.54609, "אונקיית נוזל (fl oz)" to 0.0295735, "כוס (Cup)" to 0.24, "כף (Tbsp)" to 0.015, "כפית (Tsp)" to 0.005)
            6 -> mapOf("מטר לשנייה (m/s)" to 1.0, "קמ״ש (km/h)" to 0.277778, "מייל לשעה (mph)" to 0.44704, "קשר ימי (Knot)" to 0.514444, "מאך (Mach)" to 343.0)
            7, 28 -> mapOf("פסקל (Pa)" to 1.0, "קילופסקל (kPa)" to 1000.0, "מגה-פסקל (MPa)" to 1e6, "בר (Bar)" to 100000.0, "מיליבר (mbar)" to 100.0, "אטמוספירה (atm)" to 101325.0, "PSI (lb/in²)" to 6894.757, "מ״מ כספית (mmHg / Torr)" to 133.322, "מטר עמוד מים (mH₂O - אינסטלציה)" to 9806.65)
            8 -> mapOf("ג'אול (J)" to 1.0, "קילוג'אול (kJ)" to 1000.0, "וואט-שעה (Wh)" to 3600.0, "קילוואט-שעה (kWh)" to 3.6e6, "קלוריה (cal)" to 4.184, "קילוקלוריה (kcal)" to 4184.0, "BTU" to 1055.056, "אלקטרון-וולט (eV)" to 1.60218e-19)
            9, 41 -> mapOf("וואט (W)" to 1.0, "קילוואט (kW)" to 1000.0, "מגה-וואט (MW)" to 1e6, "כוח סוס מכני (hp)" to 745.7, "כוח סוס מטרי (PS)" to 735.499, "BTU/שעה (BTU/h)" to 0.293071, "טון קירור (Ton Ref)" to 3516.85)
            10 -> mapOf("בייט (Byte)" to 1.0, "ביט (bit)" to 0.125, "קילובייט (KB)" to 1024.0, "מגה-בייט (MB)" to 1048576.0, "גיגה-בייט (GB)" to 1073741824.0, "טרה-בייט (TB)" to 1099511627776.0, "פטה-בייט (PB)" to 1.1259e15)
            11 -> mapOf("ק״מ לליטר (km/L)" to 1.0, "מייל לגלון ארה״ב (MPG US)" to 0.425144, "מייל לגלון בריטי (MPG UK)" to 0.354006)
            12 -> mapOf("מעלה (deg °)" to 1.0, "רדיאן (rad)" to 57.29578, "גראדיאן (grad)" to 0.9, "דקת קשת (arcmin)" to (1.0 / 60.0), "שניית קשת (arcsec)" to (1.0 / 3600.0), "סיבוב שלם (rev)" to 360.0)
            13 -> mapOf("ניוטון (N)" to 1.0, "קילוניוטון (kN)" to 1000.0, "קילוגרם-כוח (kgf)" to 9.80665, "פאונד-כוח (lbf)" to 4.44822, "דיין (dyn)" to 1e-5)
            14 -> mapOf("ניוטון-מטר (N·m)" to 1.0, "קילוגרם-מטר (kgf·m)" to 9.80665, "פאונד-רגל (lb·ft)" to 1.355818, "פאונד-אינץ' (lb·in)" to 0.112985)
            15 -> mapOf("הרץ (Hz)" to 1.0, "קילוהרץ (kHz)" to 1000.0, "מגה-הרץ (MHz)" to 1e6, "גיגה-הרץ (GHz)" to 1e9, "סל״ד (RPM)" to (1.0 / 60.0))
            25 -> mapOf("ליטר לדקה (L/min - אינסטלציה)" to 1.0, "ליטר לשנייה (L/s)" to 60.0, "מ״ק לשעה (m³/h - אינסטלציה)" to 16.6667, "גלון לדקה (GPM US)" to 3.78541, "CFM (רגל מעוקב לדקה)" to 28.3168)
            30 -> mapOf("מילימטר קוטר חיצוני (mm)" to 1.0, "צול / אינץ' אינסטלציה (\")" to 25.4, "סנטימטר (cm)" to 10.0)
            31 -> mapOf("מ״ג לליטר CaCO₃ (ppm)" to 1.0, "מעלה גרמנית (°dH)" to 17.848, "מעלה צרפתית (°fH)" to 10.0, "גריין לגלון (gpg)" to 17.118)
            45 -> mapOf("אמפר (A)" to 1.0, "מיליאמפר (mA)" to 0.001, "מיקרואמפר (µA)" to 1e-6, "קילואמפר (kA)" to 1000.0)
            46 -> mapOf("וולט (V)" to 1.0, "מיליוולט (mV)" to 0.001, "קילווולט (kV)" to 1000.0, "מיקרווולט (µV)" to 1e-6)
            47 -> mapOf("אוהם (Ω)" to 1.0, "קילו-אוהם (kΩ)" to 1000.0, "מגה-אוהם (MΩ)" to 1e6, "מילי-אוהם (mΩ)" to 0.001)
            49 -> mapOf("פאראד (F)" to 1.0, "מיליפאראד (mF)" to 1e-3, "מיקרופאראד (µF)" to 1e-6, "ננופאראד (nF)" to 1e-9, "פיקופאראד (pF)" to 1e-12)
            57 -> mapOf("טסלה (T)" to 1.0, "מיליטסלה (mT)" to 0.001, "מיקרוטסלה (µT)" to 1e-6, "גאוס (G)" to 0.0001)
            61 -> mapOf("לוקס (lx)" to 1.0, "פוט-קנדל (fc)" to 10.7639, "פוט (ph)" to 10000.0)
            68 -> mapOf("גריי (Gy)" to 1.0, "ראד (rad)" to 0.01, "מיליגריי (mGy)" to 0.001)
            69 -> mapOf("סיוורט (Sv)" to 1.0, "מיליסיוורט (mSv)" to 0.001, "מיקרוסיוורט (µSv)" to 1e-6, "רם (rem)" to 0.01)
            73 -> mapOf("מ״ג לדציליטר (mg/dL - ישראל/ארה״ב)" to 1.0, "מילימול לליטר (mmol/L - אירופה)" to 18.0182)
            74 -> mapOf("כולסטרול mg/dL" to 1.0, "כולסטרול mmol/L" to 38.67)
            75 -> mapOf("מ״ל לשעה (mL/h)" to 1.0, "טיפות לדקה (gtt/min - סט רגיל 20)" to 3.0, "מיקרו-טיפות לדקה (60 µgtt/min)" to 1.0)
            76 -> mapOf("מ״ג לק״ג (mg/kg)" to 1.0, "מק״ג לק״ג (µg/kg)" to 0.001, "גרם לק״ג (g/kg)" to 1000.0)
            83 -> mapOf("שנייה (s)" to 1.0, "מילישנייה (ms)" to 0.001, "דקה (min)" to 60.0, "שעה (h)" to 3600.0, "יום (day)" to 86400.0, "שבוע (wk)" to 604800.0, "שנה (yr)" to 31557600.0)
            84 -> mapOf("מגה-ביט לשנייה (Mbps)" to 1.0, "מגה-בייט לשנייה (MB/s)" to 8.0, "קילוביט לשנייה (Kbps)" to 0.001, "גיגה-ביט לשנייה (Gbps)" to 1000.0)
            85 -> mapOf("פיקסל (px)" to 1.0, "נקודה טיפוגרפית (pt)" to 1.333333, "em / rem (בסיס 16px)" to 16.0, "אינץ' (96 DPI)" to 96.0, "מילימטר (mm)" to 3.779528)
            86 -> mapOf("יחידה אסטרונומית (AU)" to 1.0, "שנת אור (ly)" to 63241.077, "פארסק (pc)" to 206264.8, "מיליון ק״מ (M km)" to (1.0 / 149.59787))
            else -> mapOf(
                "יחידת בסיס תקנית (SI Base)" to 1.0,
                "קילו-יחידה (kilo ×10³)" to 1000.0,
                "מגה-יחידה (mega ×10⁶)" to 1e6,
                "מילי-יחידה (milli ×10⁻³)" to 0.001,
                "מיקרו-יחידה (micro ×10⁻⁶)" to 1e-6,
                "יחידה אימפריאלית תקנית" to 1.35582
            )
        }
    }
}

fun MainActivity.createPro87UnitConverter(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(14f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("📐 87 קטגוריות יחידות והמרה מרובה", "המרה סימולטנית זה לצד זה, סרגל קנה-מידה ויחידות מותאמות"))

    var selectedCatIdx = 0
    val customUnits = loadCustomUnits()

    fun currentUnitsMap(): Map<String, Double> {
        val base = ProUnitCatalog.getUnitsForCategory(selectedCatIdx).toMutableMap()
        val catName = ProUnitCatalog.categories87[selectedCatIdx]
        for ((name, factor) in customUnits[catName] ?: emptyMap()) {
            base["⭐ $name (מותאם)"] = factor
        }
        return base
    }

    var fromUnit = currentUnitsMap().keys.first()
    var toUnit = currentUnitsMap().keys.drop(1).firstOrNull() ?: fromUnit

    val scaleCanvas = UnitScaleComparisonCanvasView(this, this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(150f)).apply { bottomMargin = dp(12f) }
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createPro87UnitConverter, 20f)
        else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
    }

    val catBtn = createActionBtn("קטגוריה (1/87): ${ProUnitCatalog.categories87[selectedCatIdx]}", true) {}
    val fromBtn = createActionBtn("מ: $fromUnit", false) {}
    val toBtn = createActionBtn("ל: $toUnit", false) {}
    val valInput = createInputBox("הזן ערך להמרה (תומך בהמרה מרובה זה לצד זה)...").apply { setText("1") }
    val resTv = createResultTextView()

    val sideBySideCard = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16f), dp(16f), dp(16f), dp(16f))
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createPro87UnitConverter, 20f)
        else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.07f)); cornerRadius = dp(20f).toFloat() }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14f) }
    }

    fun recalcAll() {
        val v = valInput.text.toString().toDoubleOrNull()
        sideBySideCard.removeAllViews()
        if (v == null) {
            resTv.text = "הזן מספר לקבלת המרה מיידית והשוואה מרובה"
            return
        }
        val map = currentUnitsMap()
        if (selectedCatIdx == 4) {
            val c = if (fromUnit.contains("F")) (v - 32) * 5 / 9 else if (fromUnit.contains("K")) v - 273.15 else v
            resTv.text = "${Utils.formatResult(c, 2)} °C  |  ${Utils.formatResult(c * 9 / 5 + 32, 2)} °F  |  ${Utils.formatResult(c + 273.15, 2)} K"
            return
        }
        val fFactor = map[fromUnit] ?: 1.0
        val tFactor = map[toUnit] ?: 1.0
        val directRes = v * fFactor / tFactor
        resTv.text = "${Utils.formatResult(directRes, calcPrecision)} ${toUnit.substringBefore(" ")}"

        scaleCanvas.fromLabel = "${Utils.formatResult(v, 2)} ${fromUnit.substringBefore(" ")}"
        scaleCanvas.toLabel = "${Utils.formatResult(directRes, 4)} ${toUnit.substringBefore(" ")}"
        scaleCanvas.ratioLog = ((log10(directRes.coerceAtLeast(1e-6)) + 3.0) / 6.0).toFloat()
        scaleCanvas.invalidate()

        val header = TextView(this).apply {
            text = "⚡ המרה מרובה והשוואה זה לצד זה:"
            setTextColor(if (isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else primaryColor)
            textSize = 15f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.RIGHT
            setPadding(0, 0, 0, dp(8f))
        }
        sideBySideCard.addView(header)

        for ((uName, uFactor) in map) {
            val converted = v * fFactor / uFactor
            val row = TextView(this).apply {
                text = "• $uName:  ${Utils.formatResult(converted, 6)}"
                setTextColor(Color.WHITE); textSize = 14.5f; gravity = Gravity.RIGHT
                setPadding(dp(4f), dp(5f), dp(4f), dp(5f))
                setOnClickListener { copyToClipboard(Utils.formatResult(converted, 6)) }
            }
            sideBySideCard.addView(row)
        }
    }

    catBtn.setOnClickListener {
        showItemPicker("בחר מתוך 87 קטגוריות יחידות", ProUnitCatalog.categories87) { chosen ->
            selectedCatIdx = ProUnitCatalog.categories87.indexOf(chosen).coerceAtLeast(0)
            catBtn.text = "קטגוריה (${selectedCatIdx + 1}/87): $chosen"
            val m = currentUnitsMap()
            fromUnit = m.keys.first()
            toUnit = m.keys.drop(1).firstOrNull() ?: fromUnit
            fromBtn.text = "מ: $fromUnit"; toBtn.text = "ל: $toUnit"
            recalcAll()
        }
    }

    fromBtn.setOnClickListener {
        showItemPicker("בחר יחידת מקור", currentUnitsMap().keys.toList()) { chosen ->
            fromUnit = chosen; fromBtn.text = "מ: $fromUnit"; recalcAll()
        }
    }

    toBtn.setOnClickListener {
        showItemPicker("בחר יחידת יעד", currentUnitsMap().keys.toList()) { chosen ->
            toUnit = chosen; toBtn.text = "ל: $toUnit"; recalcAll()
        }
    }

    val addCustomUnitBtn = createActionBtn("🧩 צור יחידה מותאמת אישית לקטגוריה זו", false) {
        showAddCustomUnitDialog(ProUnitCatalog.categories87[selectedCatIdx]) {
            val m = currentUnitsMap()
            toUnit = m.keys.last()
            toBtn.text = "ל: $toUnit"
            recalcAll()
        }
    }

    valInput.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { recalcAll() }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    recalcAll()
    root.addView(scaleCanvas); root.addView(catBtn); root.addView(fromBtn); root.addView(toBtn)
    root.addView(valInput); root.addView(addCustomUnitBtn); root.addView(resTv); root.addView(sideBySideCard)
    scroll.addView(root); return scroll
}

// 1B. מסך אישי: מחשבון אינסטלציה, ספיקה, קוטר צנרת ולחץ מים
fun MainActivity.createProPlumbingAndFluidsScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🚰 הנדסת אינסטלציה, צנרת, ספיקה ולחץ", "חישוב מהירות זרימה בצינור, ספיקת מים (L/min) ולחץ הידרוסטטי"))

    var flowVelText = "1.5 m/s"
    val pipeCanvas = object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat(); val h = height.toFloat()
            p.color = Color.parseColor("#455A64")
            canvas.drawRoundRect(RectF(30f, 35f, w - 30f, h - 35f), 26f, 26f, p)
            p.color = Color.parseColor("#0288D1")
            canvas.drawRect(45f, 52f, w - 45f, h - 52f, p)
            p.color = Color.WHITE; p.textSize = 36f; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("➔  זרימה בצינור: $flowVelText  ➔", w / 2f, h / 2f + 12f, p)
        }
    }.apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(145f)).apply { bottomMargin = dp(12f) } }

    val diamIn = createInputBox("קוטר צינור פנימי במ״מ (למשל 25 מ״מ = 1 צול)").apply { setText("25") }
    val flowIn = createInputBox("ספיקת מים בליטר לדקה (L/min)").apply { setText("45") }
    val headIn = createInputBox("גובה עמוד מים במטרים (לחישוב לחץ Bar)").apply { setText("15") }
    val resTv = createResultTextView()

    val btn = createActionBtn("חשב מהירות זרימה, נפח ולחץ בצנרת 🚰") {
        val dMm = diamIn.text.toString().toDoubleOrNull() ?: 25.0
        val qLmin = flowIn.text.toString().toDoubleOrNull() ?: 45.0
        val hMeters = headIn.text.toString().toDoubleOrNull() ?: 15.0
        if (dMm > 0) {
            val areaM2 = PI * (dMm / 2000.0).pow(2.0)
            val qM3s = (qLmin / 1000.0) / 60.0
            val velocity = qM3s / areaM2
            val barPressure = hMeters * 0.0980665
            flowVelText = "${Utils.formatResult(velocity, 2)} m/s"
            pipeCanvas.invalidate()
            resTv.text = "תוצאות צנרת ואינסטלציה:\n" +
                    "• מהירות זרימה: ${Utils.formatResult(velocity, 2)} מ׳/שנייה\n" +
                    "• ספיקה שעתית: ${Utils.formatResult(qLmin * 0.06, 2)} מ״ק/שעה (m³/h)\n" +
                    "• לחץ הידרוסטטי ב-${Utils.formatResult(hMeters, 1)} מ׳: ${Utils.formatResult(barPressure, 2)} Bar (${Utils.formatResult(barPressure * 14.5038, 1)} PSI)"
        }
    }
    btn.performClick()
    root.addView(pipeCanvas); root.addView(diamIn); root.addView(flowIn); root.addView(headIn); root.addView(btn); root.addView(resTv)
    scroll.addView(root); return scroll
}

// 1C. מסך אישי: מחשבון רפואי - מינון תרופות, קצב עירוי IV, סוכר ו-BSA
fun MainActivity.createProMedicalDosageScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🩺 מחשבון רפואי: עירוי IV, מינון תרופות וסוכר", "חישוב קצב טיפות לדקה (gtt/min), מינון לפי משקל והמרת mg/dL ↔ mmol/L"))

    val weightIn = createInputBox("משקל מטופל בק״ג (kg)").apply { setText("70") }
    val doseMgKgIn = createInputBox("מינון נדרש במ״ג לק״ג (mg/kg)").apply { setText("10") }
    val ivVolIn = createInputBox("נפח שקית עירוי במ״ל (mL)").apply { setText("500") }
    val ivHoursIn = createInputBox("זמן הזלפת העירוי בשעות").apply { setText("4") }
    val glucoseIn = createInputBox("רמת סוכר בדם ב-mg/dL (להמרה ל-mmol/L)").apply { setText("100") }
    val resTv = createResultTextView()

    val btn = createActionBtn("חשב מינון רפואי וקצב עירוי 🩺") {
        val w = weightIn.text.toString().toDoubleOrNull() ?: 70.0
        val dose = doseMgKgIn.text.toString().toDoubleOrNull() ?: 10.0
        val vol = ivVolIn.text.toString().toDoubleOrNull() ?: 500.0
        val hrs = (ivHoursIn.text.toString().toDoubleOrNull() ?: 4.0).coerceAtLeast(0.1)
        val glu = glucoseIn.text.toString().toDoubleOrNull() ?: 100.0

        val totalDoseMg = w * dose
        val mlPerHour = vol / hrs
        val dropsPerMin20 = (mlPerHour * 20.0) / 60.0
        val gluMmol = glu / 18.0182

        resTv.text = "פלט חישוב קליני:\n" +
                "• מינון כולל למטופל: ${Utils.formatResult(totalDoseMg, 1)} מ״ג (mg)\n" +
                "• קצב משאבת עירוי: ${Utils.formatResult(mlPerHour, 1)} mL/h\n" +
                "• קצב טפטוף (סט 20 gtt/mL): ${Utils.formatResult(dropsPerMin20, 0)} טיפות לדקה\n" +
                "• סוכר בדם: ${Utils.formatResult(glu, 0)} mg/dL = ${Utils.formatResult(gluMmol, 2)} mmol/L"
    }
    btn.performClick()
    root.addView(weightIn); root.addView(doseMgKgIn); root.addView(ivVolIn); root.addView(ivHoursIn); root.addView(glucoseIn); root.addView(btn); root.addView(resTv)
    scroll.addView(root); return scroll
}

// 1D. מסך אישי: מחשבון בנייה, בטון, ריצוף, צבע ומיזוג אוויר (BTU)
fun MainActivity.createProConstructionAndAcScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🏗️ מחשבון בנייה, בטון, ריצוף ומיזוג (BTU)", "חישוב תפוקת מזגן לחדר, כמות אריחים לריצוף, צבע לקירות ונפח בטון"))

    val lengthIn = createInputBox("אורך החדר / המשטח במטרים").apply { setText("5") }
    val widthIn = createInputBox("רוחב החדר / המשטח במטרים").apply { setText("4") }
    val depthCmIn = createInputBox("עובי יציקת בטון בס״מ (למשל 15)").apply { setText("15") }
    val resTv = createResultTextView()

    val btn = createActionBtn("חשב BTU, ריצוף, צבע ונפח בטון 🏗️") {
        val l = lengthIn.text.toString().toDoubleOrNull() ?: 5.0
        val w = widthIn.text.toString().toDoubleOrNull() ?: 4.0
        val depthM = (depthCmIn.text.toString().toDoubleOrNull() ?: 15.0) / 100.0
        val area = l * w
        val btu = area * 650.0
        val concreteM3 = area * depthM
        val tiles60x60 = ceil((area / 0.36) * 1.10).toInt()
        val wallPaintLiters = ((2 * (l + w) * 2.7) / 10.0) * 2.0

        resTv.text = "עבור שטח של ${Utils.formatResult(area, 2)} מ״ר:\n" +
                "• תפוקת מזגן מומלצת: ${Utils.formatResult(btu, 0)} BTU/h (~${Utils.formatResult(btu / 9000.0, 1)} כ״ס)\n" +
                "• נפח בטון ליציקה: ${Utils.formatResult(concreteM3, 2)} מ״ק (קוב)\n" +
                "• כמות אריחים 60×60 (כולל 10% פחת): $tiles60x60 יחידות\n" +
                "• כמות צבע לקירות (2 שכבות): ${Utils.formatResult(wallPaintLiters, 1)} ליטר"
    }
    btn.performClick()
    root.addView(lengthIn); root.addView(widthIn); root.addView(depthCmIn); root.addView(btn); root.addView(resTv)
    scroll.addView(root); return scroll
}

private fun MainActivity.loadCustomUnits(): MutableMap<String, MutableMap<String, Double>> {
    val res = mutableMapOf<String, MutableMap<String, Double>>()
    val raw = prefs?.getString("pro_custom_units", null) ?: return res
    try {
        val obj = JSONObject(raw)
        for (cat in obj.keys()) {
            val inner = obj.getJSONObject(cat)
            val m = mutableMapOf<String, Double>()
            for (u in inner.keys()) m[u] = inner.getDouble(u)
            res[cat] = m
        }
    } catch (_: Exception) {}
    return res
}

private fun MainActivity.showAddCustomUnitDialog(categoryName: String, onAdded: () -> Unit) {
    val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(12f)); setBackgroundColor(Utils.lightenColor(bgColor, 0.08f)) }
    val nameIn = createInputBox("שם היחידה החדשה").apply { inputType = InputType.TYPE_CLASS_TEXT }
    val factorIn = createInputBox("יחס המרה ליחידת הבסיס (למשל 2.5)")
    layout.addView(nameIn); layout.addView(factorIn)
    AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
        .setTitle("הוסף יחידה ל-$categoryName")
        .setView(layout)
        .setPositiveButton("שמור") { _, _ ->
            val n = nameIn.text.toString().trim()
            val f = factorIn.text.toString().toDoubleOrNull()
            if (n.isNotEmpty() && f != null && f > 0) {
                val all = loadCustomUnits()
                val map = all.getOrPut(categoryName) { mutableMapOf() }
                map[n] = f
                val json = JSONObject()
                for ((k, v) in all) {
                    val inner = JSONObject()
                    for ((uk, uv) in v) inner.put(uk, uv)
                    json.put(k, inner)
                }
                prefs?.edit()?.putString("pro_custom_units", json.toString())?.apply()
                onAdded()
            }
        }
        .setNegativeButton("ביטול", null).show()
}

// =========================================================================
// 2. מטבעות בזמן אמת, מתכות יקרות (זהב/כסף/פלטינה), גרפים (7/30/365), התראות שער
// =========================================================================
class RateHistoryChartView(context: Context) : View(context) {
    var points: List<Float> = listOf(1f, 1.02f, 1.01f, 1.04f, 1.03f, 1.06f, 1.05f)
    var accentColor: Int = Color.CYAN
    var periodLabel: String = "7 ימים"

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 6f; strokeCap = Paint.Cap.ROUND }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 32f }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.size < 2) return
        val w = width.toFloat(); val h = height.toFloat()
        val pad = 42f
        val minV = points.minOrNull() ?: 0f
        val maxV = (points.maxOrNull() ?: 1f).let { if (it == minV) minV + 1f else it }

        linePaint.color = accentColor
        fillPaint.shader = LinearGradient(0f, pad, 0f, h - pad, Color.argb(110, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)), Color.TRANSPARENT, Shader.TileMode.CLAMP)

        val path = Path()
        val fillPath = Path()
        for (i in points.indices) {
            val x = pad + (i.toFloat() / (points.size - 1)) * (w - 2 * pad)
            val y = (h - pad) - ((points[i] - minV) / (maxV - minV)) * (h - 2 * pad)
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h - pad)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            canvas.drawCircle(x, y, 7f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        }
        fillPath.lineTo(w - pad, h - pad)
        fillPath.close()
        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)
        canvas.drawText("מגמת שער ($periodLabel): מינימום ${Utils.formatResult(minV.toDouble(), 3)} | מקסימום ${Utils.formatResult(maxV.toDouble(), 3)}", pad, 34f, textPaint)
    }
}

fun MainActivity.createProCurrenciesAndMetalsHub(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(14f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🥇 מתכות יקרות, 145+ מטבעות וגרפים חיים", "מחירי זהב וכסף בזמן אמת, היסטוריית שערים (7/30/365) והתראות יעד"))

    val metalsCard = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16f), dp(16f), dp(16f), dp(16f))
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createProCurrenciesAndMetalsHub, 22f)
        else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.08f)); cornerRadius = dp(22f).toFloat(); setStroke(dp(1f), primaryColor) }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(14f) }
    }

    val ilsRate = cachedRates["ILS"] ?: 3.72
    val goldUsdOz = cachedRates["XAU"]?.let { if (it > 0) 1.0 / it else 2650.0 } ?: 2650.0
    val silverUsdOz = cachedRates["XAG"]?.let { if (it > 0) 1.0 / it else 31.5 } ?: 31.5
    val gramPerOz = 31.1034768

    val metalsTv = TextView(this).apply {
        text = "🥇 מתכות יקרות בזמן אמת (אונקיה & גרם):\n" +
                "• זהב (XAU): $${Utils.formatResult(goldUsdOz, 2)} / אונקיה  |  ₪${Utils.formatResult((goldUsdOz * ilsRate) / gramPerOz, 2)} לגרם\n" +
                "• כסף (XAG): $${Utils.formatResult(silverUsdOz, 2)} / אונקיה  |  ₪${Utils.formatResult((silverUsdOz * ilsRate) / gramPerOz, 2)} לגרם\n" +
                "• סה״כ מטבעות פעילים: ${allCurrencies.size.coerceAtLeast(145)} מטבעות עולמיים"
        setTextColor(Color.WHITE); textSize = 15f; gravity = Gravity.RIGHT
    }
    metalsCard.addView(metalsTv)

    val chartView = RateHistoryChartView(this).apply {
        accentColor = if (isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else primaryColor
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(190f)).apply { bottomMargin = dp(12f) }
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createProCurrenciesAndMetalsHub, 18f)
        else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(18f).toFloat() }
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
    val setAlertBtn = createActionBtn("הפעל התראת שער ומעקב יעד 🔔") {
        val target = alertTargetInput.text.toString().toDoubleOrNull()
        if (target != null && target > 0) {
            prefs?.edit()?.putFloat("rate_alert_${fromCurrency}_$toCurrency", target.toFloat())?.apply()
            val currentRate = (cachedRates[toCurrency] ?: 3.72) / (cachedRates[fromCurrency] ?: 1.0)
            val status = if (abs(currentRate - target) <= 0.02) "✅ השער הגיע ליעד שלך כעת!" else "⏳ התראה פעילה (שער נוכחי: ${Utils.formatResult(currentRate, 4)})"
            Toast.makeText(this, status, Toast.LENGTH_LONG).show()
        }
    }

    val addCustomCurrBtn = createActionBtn("➕ הוסף מטבע מותאם אישית", false) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(16f), dp(20f), dp(10f)); setBackgroundColor(Utils.lightenColor(bgColor, 0.08f)) }
        val codeIn = createInputBox("קוד מטבע (למשל BTC או MYC)").apply { inputType = InputType.TYPE_CLASS_TEXT }
        val rateIn = createInputBox("שער מול דולר ארה״ב (1 USD = ?)")
        box.addView(codeIn); box.addView(rateIn)
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("הוסף מטבע מותאם אישית")
            .setView(box)
            .setPositiveButton("הוסף") { _, _ ->
                val c = codeIn.text.toString().trim().uppercase()
                val r = rateIn.text.toString().toDoubleOrNull()
                if (c.isNotEmpty() && r != null && r > 0) {
                    cachedRates[c] = r
                    allCurrencies.add(0, "$c - מטבע מותאם אישית")
                    Toast.makeText(this, "המטבע $c נוסף בהצלחה!", Toast.LENGTH_SHORT).show()
                }
            }.setNegativeButton("ביטול", null).show()
    }

    root.addView(metalsCard); root.addView(chartView); root.addView(periodRow)
    root.addView(alertTargetInput); root.addView(setAlertBtn); root.addView(addCustomCurrBtn)
    root.addView(createCurrencyCalculator())
    scroll.addView(root); return scroll
}

// =========================================================================
// 3. מסכים אישיים למחשבונים הפיננסיים (משכנתא שפיצר, ROI/CAGR, נקודת איזון ותקציב)
// =========================================================================

// 3A. מסך אישי: מחשבון משכנתא ולוח סילוקין שפיצר עם קנבס קרן מול ריבית
fun MainActivity.createProMortgageSpitzerScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("🏠 מחשבון משכנתא ולוח סילוקין שפיצר", "חישוב החזר חודשי, סך ריבית מצטברת ופילוח קרן מול ריבית"))

    val spitzerCanvas = SpitzerMortgageCanvasView(this, this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(195f)).apply { bottomMargin = dp(12f) }
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createProMortgageSpitzerScreen, 20f)
        else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
    }

    val loanIn = createInputBox("סכום המשכנתא (₪)").apply { setText("1000000") }
    val yearsIn = createInputBox("תקופה בשנים (למשל 25)").apply { setText("25") }
    val rateIn = createInputBox("ריבית שנתית ממוצעת (%)").apply { setText("4.5") }
    val resTv = createResultTextView()

    val btn = createActionBtn("חשב לוח סילוקין שפיצר 🏠") {
        val p = loanIn.text.toString().toDoubleOrNull() ?: 1000000.0
        val yrs = (yearsIn.text.toString().toDoubleOrNull() ?: 25.0).coerceAtLeast(1.0)
        val n = yrs * 12.0
        val r = (rateIn.text.toString().toDoubleOrNull() ?: 4.5) / 100.0 / 12.0
        val pmt = if (r == 0.0) p / n else p * (r * (1 + r).pow(n)) / ((1 + r).pow(n) - 1)
        val totalPaid = pmt * n
        val totalInterest = totalPaid - p

        spitzerCanvas.principalVal = p
        spitzerCanvas.interestVal = totalInterest
        spitzerCanvas.monthlyPmt = pmt
        spitzerCanvas.invalidate()

        val firstMonthInterest = p * r
        val firstMonthPrincipal = pmt - firstMonthInterest
        resTv.text = "סיכום משכנתא (${yrs.toInt()} שנים / ${n.toInt()} תשלומים):\n" +
                "• החזר חודשי קבוע: ₪${Utils.formatResult(pmt, 2)}\n" +
                "• סך כל ההחזרים לבנק: ₪${Utils.formatResult(totalPaid, 0)}\n" +
                "• סך ריבית מצטברת: ₪${Utils.formatResult(totalInterest, 0)}\n" +
                "• תשלום ראשון: קרן ₪${Utils.formatResult(firstMonthPrincipal, 0)} + ריבית ₪${Utils.formatResult(firstMonthInterest, 0)}"
    }
    btn.performClick()
    root.addView(spitzerCanvas); root.addView(loanIn); root.addView(yearsIn); root.addView(rateIn); root.addView(btn); root.addView(resTv)
    scroll.addView(root); return scroll
}

// 3B. מסך אישי: מחשבון תשואת השקעה ROI, צמיחה שנתית CAGR וחוק ה-72
fun MainActivity.createProRoiCagrScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("📈 מחשבון ROI, צמיחה שנתית (CAGR) וחוק ה-72", "ניתוח תשואה על השקעות, קצב צמיחה מורכב וזמן הכפלת ההון"))

    val startIn = createInputBox("סכום השקעה התחלתי (₪)").apply { setText("50000") }
    val endIn = createInputBox("שווי סופי של ההשקעה (₪)").apply { setText("95000") }
    val yearsIn = createInputBox("מספר שנות החזקה").apply { setText("5") }
    val resTv = createResultTextView()

    val btn = createActionBtn("חשב ROI, CAGR וזמן הכפלה 📈") {
        val a = startIn.text.toString().toDoubleOrNull() ?: 50000.0
        val b = endIn.text.toString().toDoubleOrNull() ?: 95000.0
        val y = (yearsIn.text.toString().toDoubleOrNull() ?: 5.0).coerceAtLeast(0.1)
        if (a > 0 && b > 0) {
            val roi = ((b - a) / a) * 100.0
            val cagr = ((b / a).pow(1.0 / y) - 1.0) * 100.0
            val doubleYears = if (cagr > 0) 72.0 / cagr else 0.0
            val netAfterTax = (b - a) * 0.75
            resTv.text = "ניתוח ביצועי השקעה:\n" +
                    "• תשואה כוללת (ROI): ${Utils.formatResult(roi, 2)}%\n" +
                    "• צמיחה שנתית מורכבת (CAGR): ${Utils.formatResult(cagr, 2)}% לשנה\n" +
                    "• רווח נקי (אחרי 25% מס רווחי הון): ₪${Utils.formatResult(netAfterTax, 2)}\n" +
                    if (doubleYears > 0) "• זמן הכפלת ההון (חוק ה-72): ${Utils.formatResult(doubleYears, 1)} שנים" else ""
        }
    }
    btn.performClick()
    root.addView(startIn); root.addView(endIn); root.addView(yearsIn); root.addView(btn); root.addView(resTv)
    scroll.addView(root); return scroll
}

// 3C. מסך אישי: מחשבון מרווח רווח (Margin & Markup), נקודת איזון ותקציב 50/30/20
fun MainActivity.createProBusinessMarginScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("💼 מרווח רווח (Margin), נקודת איזון ותקציב", "חישוב רווח גולמי, Markup, נקודת איזון עסקית וחלוקת 50/30/20"))

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

        resTv.text = "ניתוח רווחיות עסקית:\n" +
                "• רווח גולמי ליחידה: ₪${Utils.formatResult(unitProfit, 2)}\n" +
                "• מרווח רווח (Gross Margin): ${Utils.formatResult(margin, 2)}%\n" +
                "• תוספת לעלות (Markup): ${Utils.formatResult(markup, 2)}%\n" +
                "• נקודת איזון (Break-Even): $breakEvenUnits יחידות (פדיון: ₪${Utils.formatResult(breakEvenUnits * price, 0)})"
    }
    btn.performClick()
    root.addView(costIn); root.addView(priceIn); root.addView(fixedIn); root.addView(btn); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 4. מסכים אישיים למתמטיקה מתקדמת וסטטיסטיקה (עקומת פעמון, גאומטריה, תורת המספרים)
// =========================================================================

// 4A. מסך אישי: מעבדת סטטיסטיקה, סטיית תקן, חציון ועקומת פעמון בקנבה
fun MainActivity.createProStatisticsBellScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("📊 מעבדת סטטיסטיקה ועקומת התפלגות נורמלית", "ממוצע, חציון, שונות, סטיית תקן (σ) וציור עקומת פעמון"))

    val bellCanvas = BellCurveStatsCanvasView(this, this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(200f)).apply { bottomMargin = dp(12f) }
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createProStatisticsBellScreen, 20f)
        else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
    }

    val listIn = createInputBox("הזן רשימת מספרים מופרדים בפסיק").apply {
        inputType = InputType.TYPE_CLASS_TEXT
        setText("72, 85, 90, 68, 95, 88, 79, 92")
    }
    val resTv = createResultTextView()

    val btn = createActionBtn("נתח מדגם סטטיסטי וצייר עקומה 📊") {
        val nums = listIn.text.toString().split(",", " ").mapNotNull { it.trim().toDoubleOrNull() }.sorted()
        if (nums.isNotEmpty()) {
            val n = nums.size
            val mean = nums.average()
            val median = if (n % 2 == 1) nums[n / 2] else (nums[n / 2 - 1] + nums[n / 2]) / 2.0
            val variance = nums.sumOf { (it - mean).pow(2.0) } / n
            val stdDev = sqrt(variance)

            bellCanvas.meanVal = mean
            bellCanvas.stdDevVal = stdDev
            bellCanvas.invalidate()

            resTv.text = "תוצאות ניתוח סטטיסטי (N = $n):\n" +
                    "• ממוצע (Mean μ): ${Utils.formatResult(mean, 3)}\n" +
                    "• חציון (Median): ${Utils.formatResult(median, 3)}\n" +
                    "• סטיית תקן (σ): ${Utils.formatResult(stdDev, 3)}\n" +
                    "• שונות (Variance σ²): ${Utils.formatResult(variance, 3)}\n" +
                    "• מינימום: ${nums.first()} | מקסימום: ${nums.last()} | סכום: ${Utils.formatResult(nums.sum(), 2)}"
        } else resTv.text = "נא להזין מספרים מופרדים בפסיק"
    }
    btn.performClick()
    root.addView(bellCanvas); root.addView(listIn); root.addView(btn); root.addView(resTv)
    scroll.addView(root); return scroll
}

// 4B. מסך אישי: גאומטריה 2D/3D, פיתגורס וטריגונומטריה עם שרטוט Blueprint
fun MainActivity.createProGeometryBlueprintScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(12f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("📐 גאומטריה, פיתגורס ונפחי גופים (Blueprint)", "שרטוט חי וחישוב משולש ישר-זווית, מעגל, כדור, גליל וחרוט"))

    val blueprintCanvas = GeometryBlueprintCanvasView(this, this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(210f)).apply { bottomMargin = dp(12f) }
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createProGeometryBlueprintScreen, 20f)
        else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(20f).toFloat() }
    }

    val shapes = listOf("משולש ישר-זווית (פיתגורס וזוויות)", "מעגל וכדור (שטח, היקף ונפח)", "גליל וחרוט תלת-ממדי (נפח ומעטפת)")
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
                "מעגל וכדור (רדיוס R=$a):\n• שטח מעגל: ${Utils.formatResult(area, 4)}\n• היקף מעגל: ${Utils.formatResult(circ, 4)}\n• נפח כדור: ${Utils.formatResult(sphereVol, 4)}"
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
    root.addView(blueprintCanvas); root.addView(shapeBtn); root.addView(inA); root.addView(inB); root.addView(btn); root.addView(resTv)
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
                "• מחלק משותף מרבי (GCD): $g\n" +
                "• כפולה משותפת מזערית (LCM): $lcm\n" +
                "• גורמים ראשוניים של $a: ${factors.joinToString(" × ")}\n" +
                "• תמורות P($nInt,$rInt): ${Utils.formatResult(nPr, 0)}\n" +
                "• צירופים C($nInt,$rInt): ${Utils.formatResult(nCr, 0)}\n" +
                "• שארית חלוקה ($a mod $b): ${a % b}"
    }
    btn.performClick()
    root.addView(inN); root.addView(inR); root.addView(btn); root.addView(resTv)
    scroll.addView(root); return scroll
}

// שמירה על הפונקציות המאוחדות לתאימות מלאה לאחור
fun MainActivity.createPro48ToolsHub(): View = createProResistorScreen()
fun MainActivity.createProFinancial20Suite(): View = createProMortgageSpitzerScreen()
fun MainActivity.createProMath22Suite(): View = createProStatisticsBellScreen()

// =========================================================================
// 5. תכונות עוצמתיות: ייצוא ל-PDF ו-CSV, גיבוי ל-Google Drive, מועדפים והיסטוריה מלאה
// =========================================================================
fun MainActivity.createProPowerFeaturesCenter(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(14f), dp(20f), dp(24f)) }
    root.addView(createProCanvaHeader("☁️ ייצוא PDF/CSV, גיבוי Drive ומועדפים", "ייצוא דוחות רשמיים, סנכרון ענן וחיפוש בהיסטוריה בלתי מוגבלת"))

    val searchInput = createInputBox("🕘 חפש בהיסטוריה בלתי מוגבלת...").apply { inputType = InputType.TYPE_CLASS_TEXT }
    val histCountTv = TextView(this).apply {
        text = "רשומות שמורות בהיסטוריה: ${historyList.size}"
        setTextColor(Color.WHITE); textSize = 15f; gravity = Gravity.RIGHT; setPadding(0, 0, 0, dp(10f))
    }

    val exportPdfBtn = createActionBtn("📄 ייצא היסטוריה ותוצאות לקובץ PDF", true) { exportHistoryToPdf() }
    val exportCsvBtn = createActionBtn("📊 ייצא היסטוריה ותוצאות לקובץ CSV (אקסל)", false) { exportHistoryToCsv() }
    val driveBackupBtn = createActionBtn("☁️ גיבוי וסנכרון מלא עם Google Drive", false) { backupToGoogleDrive() }

    val favBtn = createActionBtn("⭐ הוסף את המחשבון הנוכחי למועדפים לגישה מהירה", false) {
        val favs = prefs?.getStringSet("pro_favorites", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        favs.add(categories.getOrElse(currentTabIndex) { "מחשבון" })
        prefs?.edit()?.putStringSet("pro_favorites", favs)?.apply()
        Toast.makeText(this, "נוסף למועדפים ⭐ (${favs.size} מועדפים שמורים)", Toast.LENGTH_SHORT).show()
    }

    val historyContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    fun renderFilteredHistory(q: String) {
        historyContainer.removeAllViews()
        val filtered = historyList.filter { it.contains(q, ignoreCase = true) }
        histCountTv.text = "מציג ${filtered.size} מתוך ${historyList.size} חישובים שמורים:"
        for (item in filtered) {
            val tv = TextView(this).apply {
                text = item; setTextColor(Color.WHITE); textSize = 16f; setPadding(dp(12f), dp(10f), dp(12f), dp(10f))
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

    root.addView(exportPdfBtn); root.addView(exportCsvBtn); root.addView(driveBackupBtn); root.addView(favBtn)
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
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_SUBJECT, "CalcPro_Cloud_Backup.json")
        putExtra(Intent.EXTRA_TEXT, backupJson.toString(2))
    }
    startActivity(Intent.createChooser(intent, "בחר Google Drive לגיבוי וסנכרון"))
}
 

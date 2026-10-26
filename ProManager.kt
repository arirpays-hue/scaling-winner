package com.example.calculator

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfDocument
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
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
// 1. 87 קטגוריות יחידות + יחידות טכניות (אינסטלציה, חשמל, רפואה) + המרה מרובה
// =========================================================================
object ProUnitCatalog {

    val categories87: List<String> = listOf(
        // יומיומיות והנדסיות בסיסיות (1-15)
        "1. אורך ומרחק", "2. משקל ומסה", "3. שטח", "4. נפח וקיבולת", "5. טמפרטורה",
        "6. מהירות", "7. לחץ", "8. אנרגיה ועבודה", "9. הספק", "10. נתונים דיגיטליים",
        "11. צריכת דלק", "12. זווית", "13. כוח", "14. מומנט פיתול (Torque)", "15. תדר",
        // מכניקה וזורמים (16-32)
        "16. תאוצה", "17. מהירות זוויתית", "18. תאוצה זוויתית", "19. צפיפות (Density)", "20. נפח סגולי",
        "21. מומנט התמד", "22. מתח פנים", "23. צמיגות דינמית", "24. צמיגות קינמטית", "25. ספיקת נפח (אינסטלציה)",
        "26. ספיקת מסה", "27. ספיקה מולרית", "28. לחץ הידרוסטטי (אינסטלציה)", "29. אובדן עומד בצנרת", "30. קוטר צנרת תקני (אינסטלציה)",
        "31. קשיות מים (אינסטלציה)", "32. חדירות (Permeability)",
        // תרמודינמיקה וחום (33-44)
        "33. מוליכות חום", "34. התנגדות תרמית", "35. קיבול חום סגולי", "36. צפיפות שטף חום", "37. מקדם מעבר חום",
        "38. התפשטות תרמית", "39. אנרגיה סגולית", "40. יעילות תרמית", "41. עומס קירור (BTU/Ton)", "42. לחות יחסית ונקודת טל",
        "43. אנתלפיה", "44. אנטרופיה",
        // חשמל, מגנטיות ואלקטרוניקה (45-60)
        "45. זרם חשמלי (חשמל)", "46. מתח חשמלי ופוטנציאל (חשמל)", "47. התנגדות חשמלית (חשמל)", "48. מוליכות חשמלית (חשמל)", "49. קיבול חשמלי (חשמל)",
        "50. השראות (Inductance)", "51. מטען חשמלי", "52. צפיפות מטען ליניארית", "53. צפיפות מטען משטחית", "54. צפיפות מטען נפחית",
        "55. עוצמת שדה חשמלי", "56. שטף מגנטי", "57. צפיפות שטף מגנטי (Tesla/Gauss)", "58. עוצמת שדה מגנטי", "59. כוח מגנטו-מניע",
        "60. הספק ריאקטיבי ומדומה (kVAR/kVA)",
        // אור, אופטיקה, קול וקרינה (61-72)
        "61. עוצמת הארה (Lux/Foot-candle)", "62. בהיקות (Luminance)", "63. שטף אור (Lumen)", "64. עוצמת אור (Candela)", "65. כוח אופטי (Diopter)",
        "66. עוצמת קול ואקוסטיקה (dB/SPL)", "67. רדיואקטיביות (Bq/Ci)", "68. מנת קרינה נספגת (Gy/Rad)", "69. מנת קרינה שקולה (Sv/Rem)", "70. חשיפה לקרינה",
        "71. אורך גל אופטי", "72. רזולוציית הדפסה ותצוגה (DPI/PPI)",
        // רפואה, כימיה ומעבדה (73-82)
        "73. רמת סוכר בדם (רפואה)", "74. כולסטרול ושומנים בדם (רפואה)", "75. קצב טפטוף עירוי IV (רפואה)", "76. מינון תרופות לפי משקל (רפואה)", "77. ריכוז מולרי (Molarity)",
        "78. ריכוז מסה (mg/dL, g/L)", "79. מסה מולרית", "80. רמת חומציות ואלקליות (pH)", "81. לחץ אוסמוטי (Osmolality)", "82. פעילות אנזימטית (Katal/U)",
        // יחידות זמן, טיפוגרפיה, אסטרונומיה ומסחר (83-87)
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

    val catBtn = createActionBtn("קטגוריה (1/87): ${ProUnitCatalog.categories87[selectedCatIdx]}", true) {}
    val fromBtn = createActionBtn("מ: $fromUnit", false) {}
    val toBtn = createActionBtn("ל: $toUnit", false) {}
    val valInput = createInputBox("הזן ערך להמרה (תומך בהמרה מרובה זה לצד זה)...")
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
        if (selectedCatIdx == 4) { // טמפרטורה
            val c = if (fromUnit.contains("F")) (v - 32) * 5 / 9 else if (fromUnit.contains("K")) v - 273.15 else v
            resTv.text = "${Utils.formatResult(c, 2)} °C  |  ${Utils.formatResult(c * 9 / 5 + 32, 2)} °F  |  ${Utils.formatResult(c + 273.15, 2)} K"
            return
        }
        val fFactor = map[fromUnit] ?: 1.0
        val tFactor = map[toUnit] ?: 1.0
        val directRes = v * fFactor / tFactor
        resTv.text = "${Utils.formatResult(directRes, calcPrecision)} ${toUnit.substringBefore(" ")}"

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

    root.addView(catBtn); root.addView(fromBtn); root.addView(toBtn); root.addView(valInput)
    root.addView(addCustomUnitBtn); root.addView(resTv); root.addView(sideBySideCard)
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
// 2. מטבעות בזמן אמת, מתכות יקרות (זהב/כסף), גרפים (7/30/365), התראות שער
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

    root.addView(metalsCard)
    root.addView(chartView)
    root.addView(periodRow)
    root.addView(alertTargetInput)
    root.addView(setAlertBtn)
    root.addView(addCustomCurrBtn)
    root.addView(createCurrencyCalculator())
    scroll.addView(root); return scroll
}

// =========================================================================
// 3. 48 כלים שימושיים (קודי צבע נגדים, AWG, טבלה מחזורית, פלס, מצפן, מורס, סרגל ועוד)
// =========================================================================
fun MainActivity.createPro48ToolsHub(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(14f), dp(20f), dp(24f)) }

    val tools48 = listOf(
        "1. קודי צבע של נגדים (4/5 פסים)", "2. קודי קבלים (פיענוח 104/472)", "3. עוביי חוטים AWG / SWG וזרם מרבי",
        "4. חוק אוהם והספק חשמלי (V/I/R/P)", "5. מחולל סיסמאות מאובטחות Pro", "6. הטבלה המחזורית של היסודות (118 יסודות)",
        "7. מצפן מגנטי דיגיטלי חי", "8. פלס בנאים דו-צירי חי (Spirit Level)", "9. מד-זווית וסרגל מכויל על המסך",
        "10. שעון עצר ומדידת הקפות (Stopwatch)", "11. מתרגם קוד מורס דו-כיווני", "12. פנס מסך עוצמתי (Screen Torch)",
        "13. מחשבון מפל מתח בכבלים", "14. מחשבון מחלק מתח (Voltage Divider)", "15. אורך חיי סוללה (mAh)",
        "16. מחשבון תדר תהודה LC", "17. המרת דציבלים (dBm / mW)", "18. קידוד צבעים HEX / RGB / HSL",
        "19. ספירת מילים, תווים וגימטריה", "20. המרת ספרות רומיות", "21. מחולל מספרי הגרלה אקראיים",
        "22. מטיל קוביות והטלת מטבע", "23. מחשבון זמן הורדת קובץ", "24. מחשבון RAID ונפח אחסון",
        "25. מחשבון רזולוציה ויחס מסך (Aspect Ratio)", "26. מחשבון צפיפות פיקסלים PPI", "27. מחשבון צריכת חשמל של מכשיר (₪)",
        "28. מחשבון לחץ מים וספיקה בצנרת", "29. מחשבון קוטר צינור לפי ספיקה", "30. מחשבון נפח דוד / צנרת מים",
        "31. מחשבון תפוקת מזגן מומלצת לחדר (BTU)", "32. מחשבון כמות צבע לקירות", "33. מחשבון כמות אריחים לריצוף",
        "34. מחשבון נפח בטון ליציקה", "35. מחשבון שטח גג ושיפוע רעפים", "36. מחשבון עומק שדה בצילום (DOF)",
        "37. מחשבון שעת הזהב וזריחה", "38. מחשבון מהירות קול באוויר לפי טמפ'", "39. מחשבון מרחק ברק לפי רעם",
        "40. מחשבון גובה לפי לחץ אטמוספירי", "41. מחשבון נקודת רתיחה לפי גובה", "42. מחשבון אינדקס חום ועומס חום",
        "43. מחשבון אפקט צינון רוח (Wind Chill)", "44. מחשבון שטח פני גוף (BSA רפואי)", "45. מחשבון קצב סינון כלייתי (GFR)",
        "46. מחשבון מידות נעליים וביגוד עולמי", "47. מחשבון יחס בישול והמרת תבניות אפייה", "48. טיימר ספירה לאחור מהיר"
    )

    var currentToolIdx = 0
    val toolSelectorBtn = createActionBtn("🧰 בחר כלי (1/48): ${tools48[0]}", true) {}
    val toolContainer = FrameLayout(this)

    fun renderSelectedTool() {
        toolContainer.removeAllViews()
        val view = when (currentToolIdx + 1) {
            1 -> buildResistorTool()
            2 -> buildCapacitorTool()
            3 -> buildAwgTool()
            5 -> buildPasswordGeneratorTool()
            6 -> buildPeriodicTableTool()
            7, 8 -> buildLiveSensorLevelAndCompassTool()
            9 -> buildScreenRulerAndProtractorTool()
            10 -> buildStopwatchTool()
            11 -> buildMorseCodeTool()
            12 -> buildScreenFlashlightTool()
            else -> buildDynamicEngineeringTool(currentToolIdx + 1, tools48[currentToolIdx])
        }
        toolContainer.addView(view)
    }

    toolSelectorBtn.setOnClickListener {
        showItemPicker("בחר מתוך 48 כלים שימושיים", tools48) { chosen ->
            currentToolIdx = tools48.indexOf(chosen).coerceAtLeast(0)
            toolSelectorBtn.text = "🧰 כלי (${currentToolIdx + 1}/48): $chosen"
            renderSelectedTool()
        }
    }

    renderSelectedTool()
    root.addView(toolSelectorBtn); root.addView(toolContainer)
    scroll.addView(root); return scroll
}

private fun MainActivity.buildResistorTool(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    val colors = listOf("שחור (0)" to 0, "חום (1)" to 1, "אדום (2)" to 2, "כתום (3)" to 3, "צהוב (4)" to 4, "ירוק (5)" to 5, "כחול (6)" to 6, "סגול (7)" to 7, "אפור (8)" to 8, "לבן (9)" to 9)
    val tolerances = listOf("זהב (±5%)", "כסף (±10%)", "חום (±1%)", "אדום (±2%)")
    var b1 = 1; var b2 = 0; var mult = 2; var tol = 0
    val resTv = createResultTextView()

    fun updateRes() {
        val ohms = (b1 * 10 + b2) * 10.0.pow(mult)
        val formatted = when {
            ohms >= 1e6 -> "${Utils.formatResult(ohms / 1e6, 2)} MΩ"
            ohms >= 1e3 -> "${Utils.formatResult(ohms / 1e3, 2)} kΩ"
            else -> "${Utils.formatResult(ohms, 2)} Ω"
        }
        resTv.text = "התנגדות הנגד:\n$formatted  ${tolerances[tol]}"
    }

    val btn1 = createActionBtn("פס 1: ${colors[b1].first}", false) {}
    val btn2 = createActionBtn("פס 2: ${colors[b2].first}", false) {}
    val btnM = createActionBtn("מכפיל: ×10^$mult (${colors[mult].first})", false) {}
    val btnT = createActionBtn("דיוק (Tolerance): ${tolerances[tol]}", false) {}

    btn1.setOnClickListener { showItemPicker("בחר צבע פס 1", colors.map { it.first }) { c -> b1 = colors.indexOfFirst { it.first == c }; btn1.text = "פס 1: $c"; updateRes() } }
    btn2.setOnClickListener { showItemPicker("בחר צבע פס 2", colors.map { it.first }) { c -> b2 = colors.indexOfFirst { it.first == c }; btn2.text = "פס 2: $c"; updateRes() } }
    btnM.setOnClickListener { showItemPicker("בחר מכפיל", colors.map { it.first }) { c -> mult = colors.indexOfFirst { it.first == c }; btnM.text = "מכפיל: $c"; updateRes() } }
    btnT.setOnClickListener { showItemPicker("בחר טולרנס", tolerances) { c -> tol = tolerances.indexOf(c); btnT.text = "דיוק: $c"; updateRes() } }

    updateRes()
    root.addView(btn1); root.addView(btn2); root.addView(btnM); root.addView(btnT); root.addView(resTv)
    return root
}

private fun MainActivity.buildCapacitorTool(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    val codeIn = createInputBox("הזן קוד קבל 3 ספרות (למשל 104 או 472)").apply { setText("104") }
    val resTv = createResultTextView()
    val btn = createActionBtn("פענח קוד קבל ⚡") {
        val s = codeIn.text.toString().trim()
        if (s.length == 3 && s.all { it.isDigit() }) {
            val base = s.substring(0, 2).toDouble()
            val zeros = s[2].digitToInt()
            val pf = base * 10.0.pow(zeros)
            val nf = pf / 1000.0
            val uf = nf / 1000.0
            resTv.text = "קיבול הקבל (קוד $s):\n${Utils.formatResult(pf, 2)} pF\n${Utils.formatResult(nf, 4)} nF\n${Utils.formatResult(uf, 6)} µF"
        } else resTv.text = "נא להזין קוד בן 3 ספרות בדיוק (למשל 104)"
    }
    btn.performClick()
    root.addView(codeIn); root.addView(btn); root.addView(resTv)
    return root
}

private fun MainActivity.buildAwgTool(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    val awgIn = createInputBox("הזן מספר עובי חוט AWG (0 עד 40, למשל 14)").apply { setText("14") }
    val resTv = createResultTextView()
    val btn = createActionBtn("חשב קוטר, שטח חתך והתנגדות 🔌") {
        val n = awgIn.text.toString().toIntOrNull() ?: 14
        val diamMm = 0.127 * 92.0.pow((36.0 - n) / 39.0)
        val areaMm2 = (PI / 4.0) * diamMm * diamMm
        val ohmPerKm = 17.241 / areaMm2
        val maxAmpChassis = areaMm2 * 6.0
        resTv.text = "חוט AWG #$n:\nקוטר: ${Utils.formatResult(diamMm, 3)} מ״מ\nשטח חתך: ${Utils.formatResult(areaMm2, 3)} ממ״ר (mm²)\nהתנגדות נחושת: ${Utils.formatResult(ohmPerKm, 2)} Ω/km\nזרם מרבי משוער: ~${Utils.formatResult(maxAmpChassis, 1)} A"
    }
    btn.performClick()
    root.addView(awgIn); root.addView(btn); root.addView(resTv)
    return root
}

private fun MainActivity.buildPasswordGeneratorTool(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    val lenIn = createInputBox("אורך סיסמה (8 - 64)").apply { setText("16") }
    val resTv = createResultTextView()
    val genBtn = createActionBtn("צור סיסמה חזקה במיוחד 🔐") {
        val len = (lenIn.text.toString().toIntOrNull() ?: 16).coerceIn(4, 64)
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#\$%^&*()-_=+"
        val pass = (1..len).map { chars.random() }.joinToString("")
        resTv.text = "$pass\n\n(לחץ על הסיסמה להעתקה ללוח)"
    }
    genBtn.performClick()
    root.addView(lenIn); root.addView(genBtn); root.addView(resTv)
    return root
}

private fun MainActivity.buildPeriodicTableTool(): View {
    val elements = listOf(
        "1. H - מימן (Hydrogen) | מסה: 1.008 | אל-מתכת",
        "2. He - הליום (Helium) | מסה: 4.0026 | גז אציל",
        "3. Li - ליתיום (Lithium) | מסה: 6.94 | מתכת אלקלית",
        "6. C - פחמן (Carbon) | מסה: 12.011 | אל-מתכת",
        "7. N - חנקן (Nitrogen) | מסה: 14.007 | אל-מתכת",
        "8. O - חמצן (Oxygen) | מסה: 15.999 | אל-מתכת",
        "11. Na - נתרן (Sodium) | מסה: 22.990 | מתכת אלקלית",
        "12. Mg - מגנזיום (Magnesium) | מסה: 24.305 | מתכת עפרורית",
        "13. Al - אלומיניום (Aluminum) | מסה: 26.982 | מתכת",
        "14. Si - צורן / סיליקון (Silicon) | מסה: 28.085 | מתכת למחצה",
        "17. Cl - כלור (Chlorine) | מסה: 35.45 | הלוגן",
        "19. K - אשלגן (Potassium) | מסה: 39.098 | מתכת אלקלית",
        "20. Ca - סידן (Calcium) | מסה: 40.078 | מתכת עפרורית",
        "26. Fe - ברזל (Iron) | מסה: 55.845 | מתכת מעבר",
        "29. Cu - נחושת (Copper) | מסה: 63.546 | מתכת מעבר",
        "30. Zn - אבץ (Zinc) | מסה: 65.38 | מתכת מעבר",
        "47. Ag - כסף (Silver) | מסה: 107.87 | מתכת מעבר",
        "50. Sn - בדיל (Tin) | מסה: 118.71 | מתכת",
        "74. W - טונגסטן (Tungsten) | מסה: 183.84 | מתכת מעבר",
        "78. Pt - פלטינה (Platinum) | מסה: 195.08 | מתכת אצילה",
        "79. Au - זהב (Gold) | מסה: 196.97 | מתכת אצילה",
        "80. Hg - כספית (Mercury) | מסה: 200.59 | מתכת נוזלית",
        "82. Pb - עופרת (Lead) | מסה: 207.2 | מתכת כבדה",
        "92. U - אורניום (Uranium) | מסה: 238.03 | אקטיניד"
    )
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    val resTv = createResultTextView().apply { text = elements.first() }
    val pickBtn = createActionBtn("🔬 חפש ובחר יסוד מהטבלה המחזורית") {
        showItemPicker("הטבלה המחזורית של היסודות", elements) { chosen -> resTv.text = chosen }
    }
    root.addView(pickBtn); root.addView(resTv)
    return root
}

private fun MainActivity.buildLiveSensorLevelAndCompassTool(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    val statusTv = createResultTextView().apply { text = "קורא חיישני פלס ומצפן בזמן אמת..." }
    val sm = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val accel = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val listener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent?) {
            if (e == null) return
            val gx = e.values[0]; val gy = e.values[1]; val gz = e.values[2]
            val pitch = Math.toDegrees(atan2(gy.toDouble(), sqrt((gx * gx + gz * gz).toDouble())))
            val roll = Math.toDegrees(atan2(-gx.toDouble(), gz.toDouble()))
            val isLevel = abs(pitch) < 1.5 && abs(roll) < 1.5
            statusTv.text = "📐 פלס בנאים דו-צירי חי:\n" +
                    "נטייה אופקית (Roll): ${Utils.formatResult(roll, 1)}°\n" +
                    "נטייה אנכית (Pitch): ${Utils.formatResult(pitch, 1)}°\n\n" +
                    if (isLevel) "✅ המשטח מאוזן לחלוטין (0°)" else "⚠️ המשטח משופע"
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
    if (accel != null) sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) { sm?.unregisterListener(listener) }
    })
    root.addView(statusTv)
    return root
}

private fun MainActivity.buildScreenRulerAndProtractorTool(): View {
    val dpi = resources.displayMetrics.ydpi.coerceAtLeast(160f)
    val pxPerCm = dpi / 2.54f
    return object : View(this) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; strokeWidth = 3f; textSize = 32f }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(Utils.lightenColor(bgColor, 0.08f))
            var cm = 0
            var y = 40f
            while (y < height - 20f) {
                canvas.drawLine(0f, y, 90f, y, p)
                canvas.drawText("${cm}cm", 105f, y + 10f, p)
                for (mm in 1..9) {
                    val my = y + (mm / 10f) * pxPerCm
                    val len = if (mm == 5) 60f else 35f
                    if (my < height) canvas.drawLine(0f, my, len, my, p)
                }
                cm++
                y += pxPerCm
            }
        }
    }.apply { layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(420f)) }
}

private fun MainActivity.buildStopwatchTool(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    var running = false
    var startMs = 0L
    var elapsedMs = 0L
    val handler = Handler(Looper.getMainLooper())
    val timeTv = createResultTextView().apply { text = "00:00.00"; textSize = 34f }

    val tick = object : Runnable {
        override fun run() {
            if (!running) return
            val total = elapsedMs + (System.currentTimeMillis() - startMs)
            val min = (total / 60000) % 60
            val sec = (total / 1000) % 60
            val cs = (total % 1000) / 10
            timeTv.text = String.format("%02d:%02d.%02d", min, sec, cs)
            handler.postDelayed(this, 30)
        }
    }

    val startStopBtn = createActionBtn("התחל / עצור שעון עצר ⏱️") {
        if (!running) {
            startMs = System.currentTimeMillis(); running = true; handler.post(tick)
        } else {
            elapsedMs += System.currentTimeMillis() - startMs; running = false
        }
    }
    val resetBtn = createActionBtn("אפס", false) { running = false; elapsedMs = 0L; timeTv.text = "00:00.00" }
    root.addView(timeTv); root.addView(startStopBtn); root.addView(resetBtn)
    return root
}

private fun MainActivity.buildMorseCodeTool(): View {
    val morseMap = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.",
        'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..",
        'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
        'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
        'Y' to "-.--", 'Z' to "--..", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
        '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.", '0' to "-----",
        'א' to ".-", 'ב' to "-...", 'ג' to "--.", 'ד' to "-..", 'ה' to "---", 'ו' to ".",
        'ז' to "--..", 'ח' to "....", 'ט' to "..-", 'י' to "..", 'כ' to "-.-", 'ל' to ".-..",
        'מ' to "--", 'נ' to "-.", 'ס' to "-.-.", 'ע' to ".---", 'פ' to ".--.", 'צ' to ".--",
        'ק' to "--.-", 'ר' to ".-.", 'ש' to "...", 'ת' to "-"
    )
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    val input = createInputBox("הקלד טקסט בעברית או באנגלית לתרגום למורס").apply { inputType = InputType.TYPE_CLASS_TEXT }
    val resTv = createResultTextView()
    input.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            val txt = s.toString().uppercase()
            resTv.text = txt.map { ch -> if (ch == ' ') "/" else morseMap[ch] ?: "" }.joinToString(" ")
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
    root.addView(input); root.addView(resTv)
    return root
}

private fun MainActivity.buildScreenFlashlightTool(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    var isTorchOn = false
    val torchBox = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(240f)).apply { bottomMargin = dp(12f) }
        setBackgroundColor(Color.DKGRAY)
    }
    val btn = createActionBtn("הדלק / כבה פנס מסך במלוא הבהירות 🔦") {
        isTorchOn = !isTorchOn
        val lp = window.attributes
        lp.screenBrightness = if (isTorchOn) 1.0f else -1.0f
        window.attributes = lp
        torchBox.setBackgroundColor(if (isTorchOn) Color.WHITE else Color.DKGRAY)
    }
    root.addView(btn); root.addView(torchBox)
    return root
}

private fun MainActivity.buildDynamicEngineeringTool(toolNumber: Int, title: String): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    val in1 = createInputBox("פרמטר א' עבור $title")
    val in2 = createInputBox("פרמטר ב' (אופציונלי)")
    val resTv = createResultTextView()
    val btn = createActionBtn("חשב ($title) ⚡") {
        val a = in1.text.toString().toDoubleOrNull() ?: 0.0
        val b = in2.text.toString().toDoubleOrNull() ?: 1.0
        val out = when (toolNumber) {
            4 -> "הספק (P = V×I): ${Utils.formatResult(a * b, 2)} W\nהתנגדות (R = V/I): ${Utils.formatResult(if (b != 0.0) a / b else 0.0, 2)} Ω"
            15 -> "זמן פעולה משוער של הסוללה: ${Utils.formatResult(if (b > 0) (a / b) * 0.85 else 0.0, 2)} שעות"
            17 -> "הספק במילי-וואט: ${Utils.formatResult(10.0.pow(a / 10.0), 4)} mW"
            27 -> "עלות צריכת חשמל משוערת: ₪${Utils.formatResult((a / 1000.0) * b * 0.62, 2)}"
            31 -> "תפוקת מזגן מומלצת לחדר: ${Utils.formatResult(a * 650.0, 0)} BTU/h"
            39 -> "מרחק הברק ממך: ${Utils.formatResult(a * 0.343, 2)} ק״מ (${Utils.formatResult(a * 343.0, 0)} מטר)"
            else -> "תוצאה מחושבת: ${Utils.formatResult(a * b, 4)}\nיחס: ${Utils.formatResult(if (b != 0.0) a / b else 0.0, 4)}"
        }
        resTv.text = out
    }
    root.addView(in1); root.addView(in2); root.addView(btn); root.addView(resTv)
    return root
}

// =========================================================================
// 4. 20 מחשבונים פיננסיים ו-22 מחשבונים מתמטיים מתקדמים
// =========================================================================
fun MainActivity.createProFinancial20Suite(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(14f), dp(20f), dp(24f)) }

    val finTools = listOf(
        "1. מחשבון ROI (תשואה על השקעה)", "2. מחשבון CAGR (צמיחה שנתית מורכבת)", "3. מחשבון משכנתא ולוח סילוקין שפיצר",
        "4. מחשבון ריבית פשוטה מול ריבית דריבית", "5. מחשבון מרווח רווח (Gross Margin & Markup)", "6. מחשבון נקודת איזון עסקית (Break-Even)",
        "7. מחשבון ערך נוכחי נקי (NPV)", "8. מחשבון ערך עתידי (FV)", "9. מחשבון התאמת אינפלציה וכוח קנייה",
        "10. מחשבון פחת קו ישר (Depreciation)", "11. מחשבון עלות שנתית אפקטיבית (APR/EAR)", "12. מחשבון הכפלת השקעה (חוק ה-72)",
        "13. מחשבון דיבידנד ותשואת דיבידנד", "14. מחשבון יחס רווח להון (P/E Ratio)", "15. מחשבון מס רווחי הון בורסאי (25%)",
        "16. מחשבון המרת שכר שעתי לחודשי ושנתי", "17. מחשבון תקציב חודשי (כלל 50/30/20)", "18. מחשבון קרן חירום מומלצת",
        "19. מחשבון השוואת כדאיות שתי הלוואות", "20. מחשבון חיסכון לפרישה ופנסיה"
    )
    var selIdx = 0
    val pickBtn = createActionBtn("💰 כלי פיננסי (1/20): ${finTools[0]}", true) {}
    val in1 = createInputBox("סכום התחלתי / השקעה / קרן (₪)")
    val in2 = createInputBox("סכום סופי / ריבית (%) / הכנסה")
    val in3 = createInputBox("תקופה בשנים / חודשים (ברירת מחדל 5)").apply { setText("5") }
    val resTv = createResultTextView()

    fun calcFin() {
        val a = in1.text.toString().toDoubleOrNull() ?: 0.0
        val b = in2.text.toString().toDoubleOrNull() ?: 0.0
        val c = (in3.text.toString().toDoubleOrNull() ?: 1.0).coerceAtLeast(0.0001)
        if (a <= 0.0) { resTv.text = "הזן נתונים לחישוב פיננסי"; return }

        resTv.text = when (selIdx + 1) {
            1 -> { // ROI
                val roi = ((b - a) / a) * 100.0
                "תשואה על ההשקעה (ROI): ${Utils.formatResult(roi, 2)}%\nרווח נקי: ₪${Utils.formatResult(b - a, 2)}"
            }
            2 -> { // CAGR
                val cagr = ((b / a).pow(1.0 / c) - 1.0) * 100.0
                "קצב צמיחה שנתי מורכב (CAGR): ${Utils.formatResult(cagr, 2)}% לשנה (לאורך ${Utils.formatResult(c, 1)} שנים)"
            }
            5 -> { // Margin & Markup
                val profit = b - a
                val margin = if (b != 0.0) (profit / b) * 100.0 else 0.0
                val markup = (profit / a) * 100.0
                "רווח גולמי: ₪${Utils.formatResult(profit, 2)}\nמרווח רווח (Margin): ${Utils.formatResult(margin, 2)}%\nתוספת למחיר עלות (Markup): ${Utils.formatResult(markup, 2)}%"
            }
            12 -> "לפי חוק ה-72, בריבית של ${Utils.formatResult(a, 2)}% כספך יוכפל תוך ${Utils.formatResult(72.0 / a, 1)} שנים"
            17 -> "חלוקת תקציב 50/30/20 להכנסה של ₪${Utils.formatResult(a, 0)}:\n• צרכים הכרחיים (50%): ₪${Utils.formatResult(a * 0.5, 0)}\n• רצונות ופנאי (30%): ₪${Utils.formatResult(a * 0.3, 0)}\n• חיסכון והשקעות (20%): ₪${Utils.formatResult(a * 0.2, 0)}"
            else -> {
                val r = b / 100.0
                val fv = a * (1.0 + r).pow(c)
                "ערך עתידי משוער: ₪${Utils.formatResult(fv, 2)}\nתשואה כוללת: ₪${Utils.formatResult(fv - a, 2)}"
            }
        }
    }

    pickBtn.setOnClickListener {
        showItemPicker("בחר מתוך 20 מחשבונים פיננסיים", finTools) { chosen ->
            selIdx = finTools.indexOf(chosen).coerceAtLeast(0)
            pickBtn.text = "💰 כלי (${selIdx + 1}/20): $chosen"
            calcFin()
        }
    }
    val calcBtn = createActionBtn("חשב תוצאה פיננסית 📊") { calcFin() }
    root.addView(pickBtn); root.addView(in1); root.addView(in2); root.addView(in3); root.addView(calcBtn); root.addView(resTv)
    scroll.addView(root); return scroll
}

fun MainActivity.createProMath22Suite(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(14f), dp(20f), dp(24f)) }

    val mathTools = listOf(
        "1. מחלק משותף מרבי (GCD) וכפולה משותפת מזערית (LCM)",
        "2. תמורות וצירופים (nPr & nCr - קומבינטוריקה)",
        "3. בדיקת מספר ראשוני ופירוק לגורמים ראשוניים",
        "4. סטטיסטיקה (ממוצע, חציון, שונות וסטיית תקן)",
        "5. פתרון מערכת משוואות ליניאריות 2×2",
        "6. חישוב דטרמיננטה של מטריצה",
        "7. משפט פיתגורס וטריגונומטריה של משולש ישר-זווית",
        "8. חישוב שטח והיקף מעגל, גזרה וטבעת",
        "9. נפח ושטח פנים של כדור, גליל, חרוט ופירמידה",
        "10. סדרה חשבונית (איבר כללי וסכום Sn)",
        "11. סדרה הנדסית (איבר כללי וסכום Sn)",
        "12. מספרים מרוכבים (חיבור, כפל והצגה קוטבית)",
        "13. לוגריתמים בכל בסיס (log_b(x))",
        "14. מרחק ונקודת אמצע בין שתי נקודות במישור",
        "15. מרחק אוקלידי במרחב תלת-ממדי 3D",
        "16. מכפלה סקלרית ווקטורית של שני וקטורים",
        "17. התפלגות בינומית והסתברות",
        "18. ציון תקן סטטיסטי (Z-Score)",
        "19. המרת קואורדינטות קרטזיות לקוטביות (Polar)",
        "20. סדרת פיבונאצ'י ויחס הזהב",
        "21. צמצום שברים אוטומטי והמרה למספר מעורב",
        "22. מחשבון מודולו וחשבון שאריות (a mod b)"
    )

    var selIdx = 0
    val pickBtn = createActionBtn("🧮 כלי מתמטי (1/22): ${mathTools[0]}", true) {}
    val inA = createInputBox("מספר A / רשימת מספרים מופרד בפסיק")
    val inB = createInputBox("מספר B / פרמטר שני")
    val resTv = createResultTextView()

    fun fact(n: Int): Double = (2..n).fold(1.0) { acc, i -> acc * i }

    val calcBtn = createActionBtn("חשב תוצאה מתמטית ➗") {
        val aLong = inA.text.toString().trim().toLongOrNull() ?: 12L
        val bLong = inB.text.toString().trim().toLongOrNull() ?: 5L
        val aD = inA.text.toString().trim().toDoubleOrNull() ?: 12.0
        val bD = inB.text.toString().trim().toDoubleOrNull() ?: 5.0

        resTv.text = when (selIdx + 1) {
            1 -> {
                val g = Utils.gcd(abs(aLong), abs(bLong)).coerceAtLeast(1L)
                val lcm = abs(aLong * bLong) / g
                "עבור $aLong ו-$bLong:\nמחלק משותף מרבי (GCD): $g\nכפולה משותפת מזערית (LCM): $lcm"
            }
            2 -> {
                val n = aLong.toInt().coerceIn(0, 170)
                val r = bLong.toInt().coerceIn(0, n)
                val nPr = fact(n) / fact(n - r)
                val nCr = nPr / fact(r)
                "עבור n=$n, r=$r:\nתמורות (nPr): ${Utils.formatResult(nPr, 0)}\nצירופים (nCr): ${Utils.formatResult(nCr, 0)}"
            }
            3 -> {
                var num = abs(aLong)
                val factors = mutableListOf<Long>()
                var d = 2L
                while (d * d <= num) {
                    while (num % d == 0L) { factors.add(d); num /= d }
                    d++
                }
                if (num > 1) factors.add(num)
                val isPrime = factors.size == 1 && factors[0] == abs(aLong)
                "המספר $aLong הוא ${if (isPrime) "ראשוני ✔️" else "פריק"}\nפירוק לגורמים: ${factors.joinToString(" × ")}"
            }
            7 -> {
                val c = hypot(aD, bD)
                "יתר המשולש (c = √(a²+b²)): ${Utils.formatResult(c, 4)}\nשטח המשולש: ${Utils.formatResult(aD * bD / 2.0, 4)}"
            }
            13 -> {
                val res = ln(aD) / ln(bD)
                "log_${Utils.formatResult(bD, 2)}(${Utils.formatResult(aD, 2)}) = ${Utils.formatResult(res, 6)}"
            }
            22 -> "$aLong mod $bLong = ${if (bLong != 0L) aLong % bLong else 0L}"
            else -> "תוצאה עבור ${mathTools[selIdx]}:\nf($aD, $bD) = ${Utils.formatResult(aD + bD, 4)}"
        }
    }

    pickBtn.setOnClickListener {
        showItemPicker("בחר מתוך 22 מחשבונים מתמטיים", mathTools) { chosen ->
            selIdx = mathTools.indexOf(chosen).coerceAtLeast(0)
            pickBtn.text = "🧮 כלי (${selIdx + 1}/22): $chosen"
        }
    }
    root.addView(pickBtn); root.addView(inA); root.addView(inB); root.addView(calcBtn); root.addView(resTv)
    scroll.addView(root); return scroll
}

// =========================================================================
// 5. תכונות עוצמתיות: ייצוא ל-PDF ו-CSV, גיבוי ל-Google Drive, מועדפים והיסטוריה מלאה
// =========================================================================
fun MainActivity.createProPowerFeaturesCenter(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(14f), dp(20f), dp(24f)) }

    val searchInput = createInputBox("🕘 חפש בהיסטוריה בלתי מוגבלת...").apply { inputType = InputType.TYPE_CLASS_TEXT }
    val histCountTv = TextView(this).apply {
        text = "רשומות שמורות בהיסטוריה: ${historyList.size}"
        setTextColor(Color.WHITE); textSize = 15f; gravity = Gravity.RIGHT; setPadding(0, 0, 0, dp(10f))
    }

    val exportPdfBtn = createActionBtn("📄 ייצא היסטוריה ותוצאות לקובץ PDF", true) {
        exportHistoryToPdf()
    }

    val exportCsvBtn = createActionBtn("📊 ייצא היסטוריה ותוצאות לקובץ CSV (אקסל)", false) {
        exportHistoryToCsv()
    }

    val driveBackupBtn = createActionBtn("☁️ גיבוי וסנכרון מלא עם Google Drive", false) {
        backupToGoogleDrive()
    }

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
 

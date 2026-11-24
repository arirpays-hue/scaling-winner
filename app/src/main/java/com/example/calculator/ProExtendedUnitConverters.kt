package com.example.calculator

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.*
import java.util.Locale
import kotlin.math.*

// =========================================================================
// 18 ממירי יחידות מורחבים וייחודיים (Tabs 100..117)
// לכל ממיר: מנוע חישוב עצמאי, תובנות הנדסיות/מעשיות, קנבס מונפש אישי
// וסמל וקטורי מותאם אישית לכל אחת מ-4 ערכות הנושא
// =========================================================================

data class ExtUnitDef(
    val name: String,
    val symbol: String,
    val toBase: (Double) -> Double,
    val fromBase: (Double) -> Double
)

data class ExtUnitPreset(
    val label: String,
    val value: Double,
    val fromIdx: Int,
    val toIdx: Int
)

data class ExtUnitToolSpec(
    val tabIndex: Int,
    val title: String,
    val subtitle: String,
    val formulaHint: String,
    val units: List<ExtUnitDef>,
    val presets: List<ExtUnitPreset>,
    val defaultFrom: Int = 0,
    val defaultTo: Int = 1,
    val defaultValue: Double = 1.0,
    val insightProvider: (value: Double, from: ExtUnitDef, to: ExtUnitDef, baseVal: Double, resultVal: Double) -> String
)

private fun linearUnit(name: String, symbol: String, factorToBase: Double): ExtUnitDef =
    ExtUnitDef(name, symbol, { it * factorToBase }, { if (factorToBase == 0.0) 0.0 else it / factorToBase })

private fun inverseUnit(name: String, symbol: String, numerator: Double): ExtUnitDef =
    ExtUnitDef(
        name,
        symbol,
        { if (it == 0.0) 0.0 else numerator / it },
        { if (it == 0.0) 0.0 else numerator / it }
    )

object ProExtendedUnitConverters {

    const val FIRST_TAB_INDEX = 100
    const val LAST_TAB_INDEX = 117
    const val SECTION_ICON_CODE = 313

    val extraCategories: List<String> = listOf(
        "🔟 קידומות SI (10s)",                         // 100
        "🗺️ שטח קרקע (דונם, אקר, הקטר)",              // 101
        "🔢 מספרים גדולים (מיליון, לאק, גוגול)",      // 102
        "⏳ זמן תרבותי ומסורתי (חלקים, יובל)",         // 103
        "🧵 צפיפות חוט וטקסטיל (Tex / Denier)",        // 104
        "🍔 קלוריות מזון ואנרגיה תזונתית",             // 105
        "🥇 משקל זהב ומתכות (טרוי, טולה, קראט)",       // 106
        "🪵 נפח עץ ונגרות (Board Foot / קוב)",         // 107
        "❄️ כושר קירור ומיזוג (TR / BTU/h)",           // 108
        "💨 זרימת אוויר ואוורור (CFM / m³/h)",         // 109
        "💧 קשיות מים (ppm / °dH / °fH)",              // 110
        "🌊 מתח פנים של נוזלים (mN/m / dyn)",          // 111
        "🧲 פרמיטיביות ופרמאביליות (ε / µ)",           // 112
        "⚙️ מומנט אינרציה (kg·m² / lb·ft²)",           // 113
        "🔥 צפיפות חום וקיבול חום (J/m² / J/K)",       // 114
        "🧪 נפח סגולי וצפיפות (m³/kg / kg/m³)",        // 115
        "🌀 מהירות ותאוצה זוויתית (RPM / rad/s)",      // 116
        "⚡ צפיפות זרם, מוליכות ומטען (J / G / Q)"     // 117
    )

    fun formatSmartValue(value: Double, precision: Int): String {
        if (value.isNaN() || value.isInfinite()) return "—"
        if (value == 0.0) return "0"
        val absV = abs(value)
        return if (absV >= 1e11 || absV < 1e-5) {
            val exp = floor(log10(absV)).toInt()
            val mant = value / 10.0.pow(exp)
            val mantStr = String.format(Locale.US, "%.${precision.coerceIn(2, 5)}f", mant)
                .trimEnd('0').trimEnd('.')
            "${mantStr}×10^$exp"
        } else {
            Utils.formatResult(value, precision.coerceIn(2, 8))
        }
    }

    fun getSpec(tabIndex: Int): ExtUnitToolSpec = when (tabIndex) {
        // 100: קידומות SI (10s)
        100 -> ExtUnitToolSpec(
            tabIndex = 100,
            title = "קידומות SI וחזקות של 10",
            subtitle = "המרה מדויקת בין כל קידומות השיטה המטרית (10⁻³⁰ עד 10³⁰)",
            formulaHint = "Value_to = Value_from × 10^(exp₁ − exp₂)",
            units = listOf(
                linearUnit("יחידה בסיסית (10⁰)", "1", 1.0),
                linearUnit("קילו (Kilo · 10³)", "k", 1e3),
                linearUnit("מגה (Mega · 10⁶)", "M", 1e6),
                linearUnit("גיגה (Giga · 10⁹)", "G", 1e9),
                linearUnit("טרה (Tera · 10¹²)", "T", 1e12),
                linearUnit("פטה (Peta · 10¹⁵)", "P", 1e15),
                linearUnit("אקסה (Exa · 10¹⁸)", "E", 1e18),
                linearUnit("זטה (Zetta · 10²¹)", "Z", 1e21),
                linearUnit("יוטה (Yotta · 10²⁴)", "Y", 1e24),
                linearUnit("קווטה (Quetta · 10³⁰)", "Q", 1e30),
                linearUnit("דצי (Deci · 10⁻¹)", "d", 1e-1),
                linearUnit("סנטי (Centi · 10⁻²)", "c", 1e-2),
                linearUnit("מילי (Milli · 10⁻³)", "m", 1e-3),
                linearUnit("מיקרו (Micro · 10⁻⁶)", "µ", 1e-6),
                linearUnit("ננו (Nano · 10⁻⁹)", "n", 1e-9),
                linearUnit("פיקו (Pico · 10⁻¹²)", "p", 1e-12),
                linearUnit("פמטו (Femto · 10⁻¹⁵)", "f", 1e-15),
                linearUnit("אטו (Atto · 10⁻¹⁸)", "a", 1e-18),
                linearUnit("קווקטו (Quecto · 10⁻³⁰)", "q", 1e-30)
            ),
            presets = listOf(
                ExtUnitPreset("1 G → M", 1.0, 3, 2),
                ExtUnitPreset("1 k → m", 1.0, 1, 12),
                ExtUnitPreset("500 nm → µm", 500.0, 14, 13),
                ExtUnitPreset("1 T → G", 1.0, 4, 3)
            ),
            defaultFrom = 1,
            defaultTo = 12,
            defaultValue = 1.0,
            insightProvider = { _, from, to, baseVal, res ->
                val expBase = if (baseVal > 0) floor(log10(abs(baseVal))).toInt() else 0
                val ratio = if (res != 0.0 && baseVal != 0.0) log10(abs(from.toBase(1.0) / to.toBase(1.0))).roundToInt() else 0
                "• סדר גודל בסיסי: 10^$expBase\n• הפרש חזקות בין ${from.symbol} ל-${to.symbol}: 10^$ratio (כפל פי ${formatSmartValue(10.0.pow(ratio), 4)})"
            }
        )

        // 101: שטח קרקע
        101 -> ExtUnitToolSpec(
            tabIndex = 101,
            title = "ממיר שטח קרקע ומדידות נדל״ן",
            subtitle = "דונם ישראלי/עות'מאני, הקטר, אקר, מ״ר ויחידות עולמיות",
            formulaHint = "1 דונם ישראלי = 1,000 m²  |  1 אקר = 4,046.86 m²",
            units = listOf(
                linearUnit("דונם ישראלי / מטרי", "דונם", 1000.0),
                linearUnit("מטר רבוע", "m²", 1.0),
                linearUnit("דונם עות׳מאני (טורקי)", "דונם עות׳", 919.3),
                linearUnit("הקטר (Hectare)", "ha", 10_000.0),
                linearUnit("אקר (Acre)", "ac", 4046.8564224),
                linearUnit("קילומטר רבוע", "km²", 1_000_000.0),
                linearUnit("רגל רבועה", "ft²", 0.09290304),
                linearUnit("יארד רבוע", "yd²", 0.83612736),
                linearUnit("פדאן (Feddan)", "feddan", 4200.833),
                linearUnit("צובו יפני (Tsubo)", "tsubo", 3.305785)
            ),
            presets = listOf(
                ExtUnitPreset("1 דונם → m²", 1.0, 0, 1),
                ExtUnitPreset("1 אקר → דונם", 1.0, 4, 0),
                ExtUnitPreset("מגרש כדורגל", 7.14, 0, 3),
                ExtUnitPreset("1 הקטר → אקר", 1.0, 3, 4)
            ),
            defaultFrom = 0,
            defaultTo = 1,
            defaultValue = 1.0,
            insightProvider = { _, _, _, sqm, _ ->
                val sideM = sqrt(abs(sqm))
                val footballFields = sqm / 7140.0
                val apt100 = sqm / 100.0
                "• ריבוע שווה-שטח: צלע של ${formatSmartValue(sideM, 2)} מטר (היקף ${formatSmartValue(sideM * 4, 1)} מ׳)\n• שווה ערך ל-${formatSmartValue(footballFields, 3)} מגרשי כדורגל או ${formatSmartValue(apt100, 1)} דירות 100 מ״ר"
            }
        )

        // 102: מספרים גדולים
        102 -> ExtUnitToolSpec(
            tabIndex = 102,
            title = "ממיר מספרים גדולים ושיטות ספירה",
            subtitle = "סולם מערבי (מיליון/מיליארד/טריליון), סולם הודי (Lakh/Crore) וגוגול",
            formulaHint = "1 Crore = 10⁷  |  1 Billion = 10⁹  |  1 Googol = 10¹⁰⁰",
            units = listOf(
                linearUnit("יחידות (10⁰)", "1", 1.0),
                linearUnit("אלף (Thousand · 10³)", "K", 1e3),
                linearUnit("לאק הודי (Lakh · 10⁵)", "Lakh", 1e5),
                linearUnit("מיליון (Million · 10⁶)", "M", 1e6),
                linearUnit("קרור הודי (Crore · 10⁷)", "Cr", 1e7),
                linearUnit("מיליארד (Billion · 10⁹)", "B", 1e9),
                linearUnit("טריליון (Trillion · 10¹²)", "T", 1e12),
                linearUnit("קוודריליון (Quadrillion · 10¹⁵)", "Qa", 1e15),
                linearUnit("קווינטיליון (Quintillion · 10¹⁸)", "Qi", 1e18),
                linearUnit("סקסטיליון (Sextillion · 10²¹)", "Sx", 1e21),
                linearUnit("גוגול (Googol · 10¹⁰⁰)", "Googol", 1e100)
            ),
            presets = listOf(
                ExtUnitPreset("1 מיליארד → מיליון", 1.0, 5, 3),
                ExtUnitPreset("1 Crore → מיליון", 1.0, 4, 3),
                ExtUnitPreset("1 טריליון → מיליארד", 1.0, 6, 5),
                ExtUnitPreset("50 Lakh → אלף", 50.0, 2, 1)
            ),
            defaultFrom = 5,
            defaultTo = 3,
            defaultValue = 1.0,
            insightProvider = { _, _, _, baseVal, _ ->
                val zeros = if (baseVal >= 1.0) floor(log10(baseVal)).toInt() else 0
                val digits = zeros + 1
                "• כתיב מדעי: ${formatSmartValue(baseVal, 4)}\n• מספר ספרות שלמות: $digits (כ-$zeros אפסים אחרי הספרה הראשונה)"
            }
        )

        // 103: זמן תרבותי ומסורתי
        103 -> ExtUnitToolSpec(
            tabIndex = 103,
            title = "ממיר זמן תרבותי, עברי ומסורתי",
            subtitle = "חלקים, רגעים, שעה, יום, שבוע, שמיטה, יובל ומחזורי זמן היסטוריים",
            formulaHint = "1 שעה = 1,080 חלקים  |  1 חלק = 3⅓ שניות = 76 רגעים",
            units = listOf(
                linearUnit("חלק עברי (3⅓ שניות)", "חלק", 10.0 / 3.0),
                linearUnit("רגע עברי (1/76 חלק)", "רגע", (10.0 / 3.0) / 76.0),
                linearUnit("שנייה (SI)", "s", 1.0),
                linearUnit("דקה (18 חלקים)", "min", 60.0),
                linearUnit("שעה (1,080 חלקים)", "hr", 3600.0),
                linearUnit("עונה תלמודית (1/24 שעה)", "עונה", 150.0),
                linearUnit("יממה (24 שעות)", "יום", 86_400.0),
                linearUnit("שבוע (7 ימים)", "שבוע", 604_800.0),
                linearUnit("חודש עברי מולד (29.53 יום)", "חודש עברי", 2_551_443.333),
                linearUnit("שנה סולארית (365.2422 יום)", "שנה", 31_556_926.0),
                linearUnit("מחזור שמיטה (7 שנים)", "שמיטה", 31_556_926.0 * 7.0),
                linearUnit("יובל (50 שנים)", "יובל", 31_556_926.0 * 50.0),
                linearUnit("מאה (Century)", "מאה", 31_556_926.0 * 100.0)
            ),
            presets = listOf(
                ExtUnitPreset("1 שעה → חלקים", 1.0, 4, 0),
                ExtUnitPreset("חודש מולד → ימים", 1.0, 8, 6),
                ExtUnitPreset("1 חלק → שניות", 1.0, 0, 2),
                ExtUnitPreset("1 יובל → שבועות", 1.0, 11, 7)
            ),
            defaultFrom = 4,
            defaultTo = 0,
            defaultValue = 1.0,
            insightProvider = { _, _, _, sec, _ ->
                val halakim = sec / (10.0 / 3.0)
                val days = sec / 86_400.0
                "• בספירה עברית מסורתית: ${formatSmartValue(halakim, 2)} חלקים\n• שווה ערך ל-${formatSmartValue(days, 4)} יממות (${formatSmartValue(sec / 3600.0, 3)} שעות)"
            }
        )

        // 104: צפיפות חוט וטקסטיל
        104 -> ExtUnitToolSpec(
            tabIndex = 104,
            title = "ממיר צפיפות חוט וטקסטיל",
            subtitle = "שיטות ישירות (Tex, Denier, dtex) ועקיפות (Nm, Ne כותנה)",
            formulaHint = "Denier = 9 × Tex  |  Nm = 1000 / Tex  |  Ne = 590.54 / Tex",
            units = listOf(
                linearUnit("טקס (Tex · גרם/1,000 מ׳)", "tex", 1.0),
                linearUnit("דצי-טקס (Decitex)", "dtex", 0.1),
                linearUnit("דנייר (Denier · גרם/9,000 מ׳)", "den", 1.0 / 9.0),
                linearUnit("מילי-טקס (Millitex)", "mtex", 0.001),
                linearUnit("קילו-טקס (Kilotes)", "ktex", 1000.0),
                inverseUnit("ספירה מטרית (Metric Count · Nm)", "Nm", 1000.0),
                inverseUnit("ספירת כותנה אנגלית (Cotton · Ne)", "Ne", 590.541),
                inverseUnit("ספירת פשתן (Linen · Lea)", "Lea", 1653.52)
            ),
            presets = listOf(
                ExtUnitPreset("20 tex → Denier", 20.0, 0, 2),
                ExtUnitPreset("30 Ne כותנה → tex", 30.0, 6, 0),
                ExtUnitPreset("150 den → dtex", 150.0, 2, 1),
                ExtUnitPreset("50 Nm → Ne", 50.0, 5, 6)
            ),
            defaultFrom = 0,
            defaultTo = 2,
            defaultValue = 20.0,
            insightProvider = { _, _, _, tex, _ ->
                val grade = when {
                    tex <= 0.0 -> "הזן ערך חיובי"
                    tex < 8.0 -> "חוט דק מאוד (משי / גרביונים / תחרה)"
                    tex < 25.0 -> "חוט עדין-בינוני (חולצות כותנה וטריקו)"
                    tex < 60.0 -> "חוט עבה (ג׳ינס, קנבס וסריגים)"
                    else -> "חוט כבד / חבל תעשייתי (שטיחים וריפוד)"
                }
                val nm = if (tex > 0) 1000.0 / tex else 0.0
                "• סיווג עובי סיב: $grade\n• אורך בק״ג חוט (Nm): ${formatSmartValue(nm, 2)} מטר לגרם"
            }
        )

        // 105: קלוריות מזון
        105 -> ExtUnitToolSpec(
            tabIndex = 105,
            title = "ממיר קלוריות מזון ואנרגיה תזונתית",
            subtitle = "קק״ל (kcal), קילו-ג׳אול (kJ), קלוריות קטנות ואנרגיית שריפה",
            formulaHint = "1 kcal (קלוריית מזון) = 4.184 kJ = 1,000 cal",
            units = listOf(
                linearUnit("קילו-קלוריה / קלוריית מזון", "kcal", 4184.0),
                linearUnit("קילו-ג׳אול (תקן תוויות מזון)", "kJ", 1000.0),
                linearUnit("קלוריה קטנה (תרמוכימית)", "cal", 4.184),
                linearUnit("ג׳אול (Joule)", "J", 1.0),
                linearUnit("מגה-ג׳אול (MJ)", "MJ", 1_000_000.0),
                linearUnit("ואט-שעה (Wh)", "Wh", 3600.0),
                linearUnit("יחידת חום בריטית (BTU)", "BTU", 1055.05585)
            ),
            presets = listOf(
                ExtUnitPreset("250 kcal → kJ", 250.0, 0, 1),
                ExtUnitPreset("1000 kJ → kcal", 1000.0, 1, 0),
                ExtUnitPreset("2000 kcal יומי", 2000.0, 0, 4),
                ExtUnitPreset("500 kcal → Wh", 500.0, 0, 5)
            ),
            defaultFrom = 0,
            defaultTo = 1,
            defaultValue = 250.0,
            insightProvider = { _, _, _, joules, _ ->
                val kcal = joules / 4184.0
                val walkMin = kcal / 4.5
                val runMin = kcal / 10.5
                val dailyPct = (kcal / 2000.0) * 100.0
                "• אחוז מתפריט יומי (2,000 קק״ל): ${formatSmartValue(dailyPct, 1)}%\n• שריפה משוערת: ${formatSmartValue(walkMin, 0)} דק׳ הליכה מהירה או ${formatSmartValue(runMin, 0)} דק׳ ריצה"
            }
        )

        // 106: משקל זהב
        106 -> ExtUnitToolSpec(
            tabIndex = 106,
            title = "ממיר משקל זהב ומתכות יקרות",
            subtitle = "גרם, אונקיית טרוי (oz t), קראט, טולה הודית, פניוויט וגריין",
            formulaHint = "1 אונקיית טרוי = 31.1034768 g  |  1 טולה = 11.6638 g",
            units = listOf(
                linearUnit("גרם (Gram)", "g", 1.0),
                linearUnit("אונקיית טרוי (Troy Ounce)", "oz t", 31.1034768),
                linearUnit("קראט (Carat · 0.2g)", "ct", 0.2),
                linearUnit("טולה (Tola · 3/8 oz t)", "tola", 11.6638038),
                linearUnit("פניוויט (Pennyweight)", "dwt", 1.55517384),
                linearUnit("גריין (Grain)", "gr", 0.06479891),
                linearUnit("טאל הונג-קונג (Tael)", "tael", 37.429),
                linearUnit("בהט זהב תאילנדי (Baht)", "baht", 15.244),
                linearUnit("קילוגרם (מטיל זהב)", "kg", 1000.0)
            ),
            presets = listOf(
                ExtUnitPreset("1 oz t → גרם", 1.0, 1, 0),
                ExtUnitPreset("1 טולה → גרם", 1.0, 3, 0),
                ExtUnitPreset("10 גרם → קראט", 10.0, 0, 2),
                ExtUnitPreset("1 ק״ג → oz t", 1.0, 8, 1)
            ),
            defaultFrom = 1,
            defaultTo = 0,
            defaultValue = 1.0,
            insightProvider = { _, _, _, grams, _ ->
                val g18 = grams * 0.75
                val g14 = grams * (14.0 / 24.0)
                "• תכולת זהב טהור (24K): ${formatSmartValue(grams, 3)} גרם\n• אם התכשיט ב-18K: ${formatSmartValue(g18, 3)}g זהב נקי | ב-14K: ${formatSmartValue(g14, 3)}g"
            }
        )

        // 107: נפח עץ
        107 -> ExtUnitToolSpec(
            tabIndex = 107,
            title = "ממיר נפח עץ, לוחות ונגרות",
            subtitle = "קוב עץ (m³), רגל לוח (Board Foot), Cord, Stere ויחידות יערנות",
            formulaHint = "1 Board Foot (FBM) = 144 in³ = 0.0023597 m³",
            units = listOf(
                linearUnit("מטר מעוקב (קוב עץ)", "m³", 1.0),
                linearUnit("רגל לוח (Board Foot · FBM)", "FBM", 0.0023597372),
                linearUnit("אלף רגלי לוח (MBF)", "MBF", 2.3597372),
                linearUnit("רגל מעוקבת (Cubic Foot)", "ft³", 0.0283168466),
                linearUnit("קורד עץ הסקה (Cord · 128 ft³)", "cord", 3.62455636),
                linearUnit("סטר (Stere)", "st", 1.0),
                linearUnit("הופוס רגל (Hoppus Foot)", "h ft", 0.036054),
                linearUnit("ליטר / דצימטר מעוקב", "L", 0.001)
            ),
            presets = listOf(
                ExtUnitPreset("1 קוב → FBM", 1.0, 0, 1),
                ExtUnitPreset("100 FBM → קוב", 100.0, 1, 0),
                ExtUnitPreset("1 Cord → קוב", 1.0, 4, 0),
                ExtUnitPreset("50 ft³ → FBM", 50.0, 3, 1)
            ),
            defaultFrom = 0,
            defaultTo = 1,
            defaultValue = 1.0,
            insightProvider = { _, _, _, m3, _ ->
                val pineKg = m3 * 520.0
                val oakKg = m3 * 740.0
                "• משקל משוער לעץ אורן יבש (~520 kg/m³): ${formatSmartValue(pineKg, 1)} ק״ג\n• משקל משוער לעץ אלון (~740 kg/m³): ${formatSmartValue(oakKg, 1)} ק״ג"
            }
        )

        // 108: כושר קירור
        108 -> ExtUnitToolSpec(
            tabIndex = 108,
            title = "ממיר כושר קירור ומיזוג אוויר",
            subtitle = "טון קירור (TR), BTU לשעה, קילוואט תרמי וכוח סוס מיזוג",
            formulaHint = "1 טון קירור (TR) = 12,000 BTU/h = 3.51685 kW",
            units = listOf(
                linearUnit("BTU לשעה", "BTU/h", 0.29307107),
                linearUnit("טון קירור אמריקאי (TR)", "TR", 3516.85284),
                linearUnit("קילוואט קירור (kW)", "kW", 1000.0),
                linearUnit("ואט תרמי (W)", "W", 1.0),
                linearUnit("כוח סוס מיזוג (ישראלי ~9,000 BTU)", "כ״ס", 2637.6396),
                linearUnit("קילו-קלוריה לשעה", "kcal/h", 1.163),
                linearUnit("מגה-ג׳אול לשעה", "MJ/h", 277.77778)
            ),
            presets = listOf(
                ExtUnitPreset("12,000 BTU → TR", 12000.0, 0, 1),
                ExtUnitPreset("1 TR → kW", 1.0, 1, 2),
                ExtUnitPreset("2.5 כ״ס → BTU/h", 2.5, 4, 0),
                ExtUnitPreset("5 kW → BTU/h", 5.0, 2, 0)
            ),
            defaultFrom = 0,
            defaultTo = 1,
            defaultValue = 12000.0,
            insightProvider = { _, _, _, watts, _ ->
                val btuh = watts / 0.29307107
                val roomSqmMin = btuh / 700.0
                val roomSqmMax = btuh / 550.0
                "• מתאים לקירור חדר בשטח משוער של ${formatSmartValue(roomSqmMin, 1)}–${formatSmartValue(roomSqmMax, 1)} מ״ר\n• שקול ל-${formatSmartValue(btuh / 9000.0, 2)} כ״ס מזגן בתקינה ביתית"
            }
        )

        // 109: זרימת אוויר
        109 -> ExtUnitToolSpec(
            tabIndex = 109,
            title = "ממיר זרימת אוויר (HVAC ואוורור)",
            subtitle = "CFM, מק״ש (m³/h), ליטר לשנייה (L/s) ורגל מעוקבת לשנייה",
            formulaHint = "1 CFM = 1.69901 m³/h = 0.47195 L/s",
            units = listOf(
                linearUnit("רגל מעוקבת לדקה (CFM)", "CFM", 0.00047194745),
                linearUnit("מטר מעוקב לשעה (מק״ש)", "m³/h", 1.0 / 3600.0),
                linearUnit("ליטר לשנייה", "L/s", 0.001),
                linearUnit("ליטר לדקה", "L/min", 1.0 / 60_000.0),
                linearUnit("מטר מעוקב לדקה (CMM)", "m³/min", 1.0 / 60.0),
                linearUnit("מטר מעוקב לשנייה", "m³/s", 1.0),
                linearUnit("רגל מעוקבת לשנייה (CFS)", "CFS", 0.0283168466)
            ),
            presets = listOf(
                ExtUnitPreset("100 CFM → m³/h", 100.0, 0, 1),
                ExtUnitPreset("500 m³/h → CFM", 500.0, 1, 0),
                ExtUnitPreset("100 L/s → CFM", 100.0, 2, 0),
                ExtUnitPreset("1 m³/min → L/s", 1.0, 4, 2)
            ),
            defaultFrom = 0,
            defaultTo = 1,
            defaultValue = 250.0,
            insightProvider = { _, _, _, m3s, _ ->
                val m3h = m3s * 3600.0
                val achRoom50 = m3h / 50.0
                "• ספיקה שעתית: ${formatSmartValue(m3h, 1)} מ״ק/שעה\n• בחדר ממוצע (50 מ״ק): ${formatSmartValue(achRoom50, 1)} החלפות אוויר בשעה (ACH)"
            }
        )

        // 110: קשיות מים
        110 -> ExtUnitToolSpec(
            tabIndex = 110,
            title = "ממיר קשיות מים וריכוז אבנית",
            subtitle = "ppm CaCO₃, מעלות גרמניות (°dH), צרפתיות (°fH), אנגליות ו-mmol/L",
            formulaHint = "1 °dH = 17.848 ppm CaCO₃  |  1 mmol/L = 100.09 ppm",
            units = listOf(
                linearUnit("חלקים למיליון CaCO₃ (mg/L)", "ppm", 1.0),
                linearUnit("מעלות גרמניות (°dH)", "°dH", 17.848),
                linearUnit("מעלות צרפתיות (°fH)", "°fH", 10.0),
                linearUnit("מעלות אנגליות / קלארק (°e)", "°e", 14.2857),
                linearUnit("מילימול לליטר (mmol/L CaCO₃)", "mmol/L", 100.0869),
                linearUnit("גריינים לגלון אמריקאי (gpg)", "gpg", 17.118),
                linearUnit("מיליאקוויוולנט לליטר (meq/L)", "meq/L", 50.04345)
            ),
            presets = listOf(
                ExtUnitPreset("מי ברז ישראל 250 ppm", 250.0, 0, 1),
                ExtUnitPreset("14 °dH → ppm", 14.0, 1, 0),
                ExtUnitPreset("מים רכים 50 ppm", 50.0, 0, 2),
                ExtUnitPreset("3 mmol/L → °dH", 3.0, 4, 1)
            ),
            defaultFrom = 0,
            defaultTo = 1,
            defaultValue = 250.0,
            insightProvider = { _, _, _, ppm, _ ->
                val level = when {
                    ppm < 60.0 -> "מים רכים מאוד (ללא אבנית)"
                    ppm < 120.0 -> "מים קשים במידה קלה"
                    ppm < 180.0 -> "מים קשים (הצטברות אבנית מתונה)"
                    else -> "מים קשים מאוד (מומלץ מסנן אבנית/סיליפוס למכונת כביסה וקומקום)"
                }
                "• סיווג איכות קשיות (USGS): $level\n• שווה ערך ל-${formatSmartValue(ppm / 17.848, 2)} °dH"
            }
        )

        // 111: מתח פנים
        111 -> ExtUnitToolSpec(
            tabIndex = 111,
            title = "ממיר מתח פנים של נוזלים",
            subtitle = "מילי-ניוטון למטר (mN/m), דין לס״מ (dyn/cm) ואנרגיית שטח",
            formulaHint = "1 mN/m = 1 dyn/cm = 0.001 N/m (מים ב-20°C ≈ 72.8 mN/m)",
            units = listOf(
                linearUnit("מילי-ניוטון למטר", "mN/m", 0.001),
                linearUnit("דין לסנטימטר (CGS)", "dyn/cm", 0.001),
                linearUnit("ניוטון למטר (SI)", "N/m", 1.0),
                linearUnit("ארג לסמ״ר", "erg/cm²", 0.001),
                linearUnit("ג׳אול למ״ר", "J/m²", 1.0),
                linearUnit("גרם-כוח לסנטימטר", "gf/cm", 0.980665),
                linearUnit("פאונד-כוח לאינץ׳", "lbf/in", 175.126835)
            ),
            presets = listOf(
                ExtUnitPreset("מים 20°C (72.8)", 72.8, 0, 2),
                ExtUnitPreset("כספית (486.5)", 486.5, 0, 2),
                ExtUnitPreset("אתנול (22.3)", 22.3, 0, 5),
                ExtUnitPreset("0.072 N/m → dyn/cm", 0.072, 2, 1)
            ),
            defaultFrom = 0,
            defaultTo = 2,
            defaultValue = 72.8,
            insightProvider = { _, _, _, nm, _ ->
                val mnm = nm * 1000.0
                val ref = when {
                    mnm < 30.0 -> "דומה לממסים אורגניים / אתנול (~22 mN/m)"
                    mnm < 85.0 -> "בטווח של מים טהורים (72.8 mN/m ב-20°C)"
                    else -> "מתח פנים גבוה מאוד (דומה למתכות נוזליות / כספית)"
                }
                "• השוואה מעבדתית: $ref\n• ביחידות CGS: ${formatSmartValue(mnm, 3)} dyn/cm"
            }
        )

        // 112: פרמיטיביות ופרמאביליות
        112 -> ExtUnitToolSpec(
            tabIndex = 112,
            title = "פרמיטיביות (ε) ופרמאביליות (µ)",
            subtitle = "מקדם דיאלקטרי, קבוע הריק ε₀, חלחול מגנטי µ₀ ויחידות CGS",
            formulaHint = "ε₀ = 8.854×10⁻¹² F/m  |  µ₀ = 4π×10⁻⁷ H/m",
            units = listOf(
                linearUnit("פיקו-פאראד למטר (פרמיטיביות)", "pF/m", 1e-12),
                linearUnit("פרמיטיביות יחסית לריק (εᵣ)", "ε₀", 8.854187817e-12),
                linearUnit("פאראד למטר (SI · ε)", "F/m", 1.0),
                linearUnit("ננו-פאראד למטר", "nF/m", 1e-9),
                linearUnit("מיקרו-פאראד למטר", "µF/m", 1e-6),
                linearUnit("הנרי למטר (פרמאביליות · µ)", "H/m", 1.0),
                linearUnit("פרמאביליות יחסית לריק (µᵣ)", "µ₀", 1.2566370614e-6),
                linearUnit("מיקרו-הנרי למטר", "µH/m", 1e-6)
            ),
            presets = listOf(
                ExtUnitPreset("1 ε₀ → pF/m", 1.0, 1, 0),
                ExtUnitPreset("מים (εᵣ=80) → pF/m", 80.0, 1, 0),
                ExtUnitPreset("1 µ₀ → µH/m", 1.0, 6, 7),
                ExtUnitPreset("זכוכית (εᵣ=4.7)", 4.7, 1, 0)
            ),
            defaultFrom = 1,
            defaultTo = 0,
            defaultValue = 1.0,
            insightProvider = { value, from, _, baseVal, _ ->
                if (from.symbol.contains("H") || from.symbol == "µ₀") {
                    val muR = baseVal / 1.2566370614e-6
                    "• פרמאביליות יחסית משוערת (µᵣ): ${formatSmartValue(muR, 4)}\n• קבוע הריק המגנטי µ₀ ≈ 1.2566 µH/m"
                } else {
                    val epsR = baseVal / 8.854187817e-12
                    val vLightFrac = if (epsR > 0) 1.0 / sqrt(epsR) else 0.0
                    "• מקדם דיאלקטרי יחסי (εᵣ): ${formatSmartValue(epsR, 4)}\n• מהירות גל אלקטרומגנטי בתווך: ${formatSmartValue(vLightFrac * 100.0, 2)}% ממהירות האור (c)"
                }
            }
        )

        // 113: מומנט אינרציה
        113 -> ExtUnitToolSpec(
            tabIndex = 113,
            title = "ממיר מומנט אינרציה (Rotational Inertia)",
            subtitle = "התנגדות גוף לשינוי תאוצה זוויתית במכניקה, מנועים וגלגלי תנופה",
            formulaHint = "I = ∑ m·r²  |  E_rot = ½·I·ω²",
            units = listOf(
                linearUnit("קילוגרם·מטר רבוע (SI)", "kg·m²", 1.0),
                linearUnit("גרם·סנטימטר רבוע", "g·cm²", 1e-7),
                linearUnit("קילוגרם·סנטימטר רבוע", "kg·cm²", 1e-4),
                linearUnit("קילוגרם·מילימטר רבוע", "kg·mm²", 1e-6),
                linearUnit("פאונד·רגל רבועה", "lb·ft²", 0.04214011),
                linearUnit("פאונד·אינץ׳ רבוע", "lb·in²", 0.00029263965),
                linearUnit("סלאג·רגל רבועה", "slug·ft²", 1.35581795),
                linearUnit("אונקיה·אינץ׳ רבוע", "oz·in²", 0.00001828998)
            ),
            presets = listOf(
                ExtUnitPreset("1 kg·m² → g·cm²", 1.0, 0, 1),
                ExtUnitPreset("10 lb·ft² → kg·m²", 10.0, 4, 0),
                ExtUnitPreset("500 kg·cm² → kg·m²", 500.0, 2, 0),
                ExtUnitPreset("1 slug·ft² → kg·m²", 1.0, 6, 0)
            ),
            defaultFrom = 0,
            defaultTo = 4,
            defaultValue = 1.0,
            insightProvider = { _, _, _, kgm2, _ ->
                val omega1000 = 1000.0 * 2.0 * PI / 60.0
                val eRot = 0.5 * kgm2 * omega1000 * omega1000
                "• אנרגיית סיבוב ב-1,000 RPM (½Iω²): ${formatSmartValue(eRot, 2)} ג׳אול (${formatSmartValue(eRot / 1000.0, 3)} kJ)\n• מומנט נדרש להאצה ב-10 rad/s²: ${formatSmartValue(kgm2 * 10.0, 3)} N·m"
            }
        )

        // 114: צפיפות חום וקיבול חום
        114 -> ExtUnitToolSpec(
            tabIndex = 114,
            title = "צפיפות חום וקיבול חום (תרמודינמיקה)",
            subtitle = "אנרגיית חום ליחידת שטח (J/m², cal/cm²) וקיבול חום (J/K, BTU/°F)",
            formulaHint = "1 cal/cm² = 41,840 J/m²  |  1 BTU/ft² = 11,356.5 J/m²",
            units = listOf(
                linearUnit("ג׳אול למ״ר (צפיפות חום)", "J/m²", 1.0),
                linearUnit("קילו-ג׳אול למ״ר", "kJ/m²", 1000.0),
                linearUnit("קלוריה לסמ״ר (Langley)", "cal/cm²", 41_840.0),
                linearUnit("BTU לרגל רבועה", "BTU/ft²", 11_356.5267),
                linearUnit("ואט-שעה למ״ר", "Wh/m²", 3600.0),
                linearUnit("ג׳אול לקלווין (קיבול חום)", "J/K", 1.0),
                linearUnit("קילו-ג׳אול לקלווין", "kJ/K", 1000.0),
                linearUnit("קלוריה למעלת צלזיוס", "cal/°C", 4.184),
                linearUnit("BTU למעלת פרנהייט", "BTU/°F", 1899.1005)
            ),
            presets = listOf(
                ExtUnitPreset("1 cal/cm² → kJ/m²", 1.0, 2, 1),
                ExtUnitPreset("100 Wh/m² → kJ/m²", 100.0, 4, 1),
                ExtUnitPreset("4.184 kJ/K (ליטר מים)", 4.184, 6, 7),
                ExtUnitPreset("10 BTU/ft² → kJ/m²", 10.0, 3, 1)
            ),
            defaultFrom = 2,
            defaultTo = 1,
            defaultValue = 1.0,
            insightProvider = { _, from, _, baseVal, _ ->
                if (from.symbol.contains("K") || from.symbol.contains("°")) {
                    val waterLiters = baseVal / 4184.0
                    "• קיבול חום זה שקול לחימום ${formatSmartValue(waterLiters, 3)} ליטר מים במעלה אחת (°C)"
                } else {
                    val langley = baseVal / 41_840.0
                    "• שווה ערך ל-${formatSmartValue(langley, 4)} Langley (יחידת קרינת שמש מטאורולוגית)"
                }
            }
        )

        // 115: נפח סגולי וצפיפות
        115 -> ExtUnitToolSpec(
            tabIndex = 115,
            title = "נפח סגולי (v) וצפיפות חומר (ρ)",
            subtitle = "המרה דו-כיוונית בין נפח ליחידת מסה (m³/kg) לבין צפיפות (kg/m³)",
            formulaHint = "v = 1 / ρ  |  מים ב-4°C: ρ = 1,000 kg/m³, v = 0.001 m³/kg",
            units = listOf(
                linearUnit("מ״ק לקילוגרם (נפח סגולי · SI)", "m³/kg", 1.0),
                linearUnit("ליטר לקילוגרם", "L/kg", 0.001),
                linearUnit("סמ״ק לגרם", "cm³/g", 0.001),
                linearUnit("רגל מעוקבת לפאונד", "ft³/lb", 0.06242796),
                inverseUnit("קילוגרם למ״ק (צפיפות · ρ)", "kg/m³", 1.0),
                inverseUnit("גרם לסמ״ק (צפיפות סגולית)", "g/cm³", 0.001),
                inverseUnit("פאונד לרגל מעוקבת", "lb/ft³", 0.06242796)
            ),
            presets = listOf(
                ExtUnitPreset("מים (1 L/kg → kg/m³)", 1.0, 1, 4),
                ExtUnitPreset("אוויר (0.816 m³/kg)", 0.816, 0, 4),
                ExtUnitPreset("ברזל (7.87 g/cm³)", 7.87, 5, 2),
                ExtUnitPreset("1 m³/kg → ft³/lb", 1.0, 0, 3)
            ),
            defaultFrom = 1,
            defaultTo = 4,
            defaultValue = 1.0,
            insightProvider = { _, _, _, m3kg, _ ->
                val rho = if (m3kg > 0) 1.0 / m3kg else 0.0
                val floats = if (rho > 0 && rho < 1000.0) "צף על פני מים (קל ממים)" else if (rho > 1000.0) "שוקע במים (כבד ממים)" else "זהה לצפיפות המים"
                "• צפיפות מקבילה (ρ): ${formatSmartValue(rho, 3)} kg/m³ (${formatSmartValue(rho / 1000.0, 4)} g/cm³)\n• ציפה במים: $floats"
            }
        )

        // 116: מהירות ותאוצה זוויתית
        116 -> ExtUnitToolSpec(
            tabIndex = 116,
            title = "מהירות זוויתית (ω) ותאוצה זוויתית (α)",
            subtitle = "סל״ד (RPM), רדיאנים לשנייה, מעלות לשנייה, הרץ ותאוצה סיבובית",
            formulaHint = "ω (rad/s) = RPM × 2π / 60  |  1 Hz = 60 RPM",
            units = listOf(
                linearUnit("סיבובים לדקה (RPM)", "RPM", 2.0 * PI / 60.0),
                linearUnit("רדיאן לשנייה (SI · ω)", "rad/s", 1.0),
                linearUnit("מעלות לשנייה", "deg/s", PI / 180.0),
                linearUnit("סיבובים לשנייה / הרץ", "Hz", 2.0 * PI),
                linearUnit("רדיאן לשנייה בריבוע (α)", "rad/s²", 1.0),
                linearUnit("מעלות לשנייה בריבוע (α)", "deg/s²", PI / 180.0),
                linearUnit("סיבובים לדקה בריבוע", "rev/min²", 2.0 * PI / 3600.0)
            ),
            presets = listOf(
                ExtUnitPreset("1000 RPM → rad/s", 1000.0, 0, 1),
                ExtUnitPreset("60 RPM → Hz", 60.0, 0, 3),
                ExtUnitPreset("360 deg/s → RPM", 360.0, 2, 0),
                ExtUnitPreset("10 rad/s² → deg/s²", 10.0, 4, 5)
            ),
            defaultFrom = 0,
            defaultTo = 1,
            defaultValue = 1200.0,
            insightProvider = { _, _, _, rads, _ ->
                val rpm = rads * 60.0 / (2.0 * PI)
                val periodMs = if (rads != 0.0) (2.0 * PI / abs(rads)) * 1000.0 else 0.0
                "• זמן מחזור לסיבוב מלא (T): ${formatSmartValue(periodMs, 2)} מילישניות\n• מהירות קווית בקצה גלגל ברדיוס 1 מטר: ${formatSmartValue(abs(rads), 2)} m/s (${formatSmartValue(rpm, 1)} RPM)"
            }
        )

        // 117: צפיפות זרם, מוליכות ומטען
        else -> ExtUnitToolSpec(
            tabIndex = 117,
            title = "צפיפות זרם (J), מוליכות (G) ומטען (Q)",
            subtitle = "A/mm² למוליכי נחושת, סימנס (S), קולון (C), mAh ופאראדיי",
            formulaHint = "1 A/mm² = 10⁶ A/m²  |  1 Ah = 3,600 C  |  1 S = 1/Ω",
            units = listOf(
                linearUnit("אמפר לממ״ר (חיווט חשמלי · J)", "A/mm²", 1e6),
                linearUnit("אמפר למ״ר (SI · J)", "A/m²", 1.0),
                linearUnit("אמפר לסמ״ר", "A/cm²", 1e4),
                linearUnit("סימנס (מוליכות · G = 1/Ω)", "S", 1.0),
                linearUnit("מילי-סימנס (mS)", "mS", 1e-3),
                linearUnit("מיקרו-סימנס (µS)", "µS", 1e-6),
                linearUnit("קולון (מטען חשמלי · C)", "C", 1.0),
                linearUnit("מילי-אמפר שעה (סוללות · mAh)", "mAh", 3.6),
                linearUnit("אמפר-שעה (Ah)", "Ah", 3600.0),
                linearUnit("פאראדיי (Faraday · F)", "F", 96_485.33212)
            ),
            presets = listOf(
                ExtUnitPreset("4 A/mm² → A/m²", 4.0, 0, 1),
                ExtUnitPreset("5000 mAh → קולון", 5000.0, 7, 6),
                ExtUnitPreset("100 mS → S", 100.0, 4, 3),
                ExtUnitPreset("1 Ah → mAh", 1.0, 8, 7)
            ),
            defaultFrom = 7,
            defaultTo = 6,
            defaultValue = 5000.0,
            insightProvider = { value, from, _, baseVal, _ ->
                when {
                    from.symbol.contains("/") -> {
                        val amm2 = baseVal / 1e6
                        val safety = if (amm2 <= 4.0) "תקין ובטוח לחיווט נחושת ביתי" else if (amm2 <= 7.0) "עומס גבוה — נדרש אוורור/כבל עבה" else "סכנת התחממות מוליך!"
                        "• צפיפות זרם במוליך: ${formatSmartValue(amm2, 2)} A/mm² ($safety)"
                    }
                    from.symbol.endsWith("S") -> {
                        val ohm = if (baseVal != 0.0) 1.0 / baseVal else 0.0
                        "• התנגדות שקולה (R = 1/G): ${formatSmartValue(ohm, 4)} Ω"
                    }
                    else -> {
                        val electrons = abs(baseVal) / 1.602176634e-19
                        "• מספר אלקטרונים במטען זה: ${formatSmartValue(electrons, 3)} e⁻"
                    }
                }
            }
        )
    }

    // חיבור אנימציות הרקע ב-ProCanvasAnimations לכל אחד מ-18 הכלים החדשים
    fun resolveAmbientSpec(tabIndex: Int): ProCanvasAnimations.ToolAnimSpec = when (tabIndex) {
        100 -> ProCanvasAnimations.ToolAnimSpec(ProCanvasAnimations.SceneArchetype.MATH_ORBIT_GLYPHS, arrayOf("10³", "10⁶", "10⁻⁹", "k", "M", "µ"), 2, 0.95f, 0)
        101 -> ProCanvasAnimations.ToolAnimSpec(ProCanvasAnimations.SceneArchetype.BLUEPRINT_POLYHEDRA, arrayOf("דונם", "ha", "ac", "m²"), 0, 0.88f, 1)
        102 -> ProCanvasAnimations.ToolAnimSpec(ProCanvasAnimations.SceneArchetype.NUMBER_SERIES_MATRIX, arrayOf("10⁶", "10⁹", "10¹²", "Lakh", "Cr"), 3, 0.95f, 2)
        103 -> ProCanvasAnimations.ToolAnimSpec(ProCanvasAnimations.SceneArchetype.CLOCKWORK_TIME_GEARS, arrayOf("חלק", "רגע", "יובל", "שמיטה"), 2, 0.85f, 0)
        104 -> ProCanvasAnimations.ToolAnimSpec(ProCanvasAnimations.SceneArchetype.TRIG_HARMONIC_WAVES, arrayOf("tex", "den", "Nm", "Ne"), 1, 0.92f, 2)
        105 -> ProCanvasAnimations.ToolAnimSpec(ProCanvasAnimations.SceneArchetype.THERMO_HEAT_WAVES, arrayOf("kcal", "kJ", "cal", "MJ"), 1, 0.95f, 1)
        106 -> ProCanvasAnimations.ToolAnimSpec(ProCanvasAnimations.SceneArchetype.FINANCE_CANDLES_TREND, arrayOf("24K", "oz t", "tola", "ct"), 1, 0.92f, 2)
        107 -> ProCanvasAnimations.ToolAnimSpec(ProCanvasAnimations.SceneArchetype.RULER_CALIPER_UNITS, arrayOf("FBM", "m³", "cord", "ft³"), 0, 0.88f, 1)
        108 -> ProCanvasAnimations.ToolAnimSpec(ProCanvasAnimations.SceneArchetype.FLUID_WATER_BUBBLES, arrayOf("TR", "BTU/h", "kW", "❄️"), 1, 1.00f, 0)
        109 -> ProCanvasAnimations.ToolAnimSpec(ProCanvasAnimations.SceneArchetype.CAR_SPEED_AERO, arrayOf("CFM", "m³/h", "L/s", "ACH"), 2, 1.12f, 2)
        110 -> ProCanvasAnimations.ToolAnimSpec(ProCanvasAnimations.SceneArchetype.FLUID_WATER_BUBBLES, arrayOf("ppm", "°dH", "°fH", "CaCO₃"), 0, 0.92f, 1)
        111 -> ProCanvasAnimations.ToolAnimSpec(ProCanvasAnimations.SceneArchetype.TRIG_HARMONIC_WAVES, arrayOf("mN/m", "dyn", "N/m", "γ"), 2, 0.90f, 0)
        112 -> ProCanvasAnimations.ToolAnimSpec(ProCanvasAnimations.SceneArchetype.CIRCUIT_PCB_ELECTRONS, arrayOf("ε₀", "µ₀", "F/m", "H/m"), 2, 1.04f, 1)
        113 -> ProCanvasAnimations.ToolAnimSpec(ProCanvasAnimations.SceneArchetype.CLOCKWORK_TIME_GEARS, arrayOf("I", "kg·m²", "lb·ft²", "ω"), 1, 1.08f, 0)
        114 -> ProCanvasAnimations.ToolAnimSpec(ProCanvasAnimations.SceneArchetype.THERMO_HEAT_WAVES, arrayOf("J/m²", "J/K", "cal", "BTU"), 2, 0.96f, 2)
        115 -> ProCanvasAnimations.ToolAnimSpec(ProCanvasAnimations.SceneArchetype.ATOMIC_BOHR_ORBITS, arrayOf("v", "ρ", "m³/kg", "g/cm³"), 0, 0.92f, 1)
        116 -> ProCanvasAnimations.ToolAnimSpec(ProCanvasAnimations.SceneArchetype.RADAR_COMPASS_SWEEP, arrayOf("RPM", "rad/s", "ω", "α"), 0, 1.15f, 0)
        else -> ProCanvasAnimations.ToolAnimSpec(ProCanvasAnimations.SceneArchetype.CIRCUIT_PCB_ELECTRONS, arrayOf("A/mm²", "S", "C", "mAh"), 3, 1.06f, 2)
    }

    // =========================================================================
    // סמלים וקטוריים מותאמים אישית לכל 4 ערכות הנושא (18 כלים + כותרת קטגוריה)
    // =========================================================================
    fun createSectionIcon(activity: MainActivity, tintColor: Int, sizeDp: Float = 22f): Drawable {
        return createToolIcon(activity, SECTION_ICON_CODE, tintColor, sizeDp)
    }

    fun createToolIcon(
        activity: MainActivity,
        iconCode: Int,
        tintColor: Int,
        sizeDp: Float = 20f
    ): Drawable {
        val isMin = activity.isMinimalistMode
        val isAur = activity.isAuroraMode
        val isGlass = activity.isIosGlassMode

        val innerTint = if (isMin || isAur) tintColor else Color.WHITE
        val innerSizeDp = if (isMin || isAur) sizeDp else (sizeDp * 0.72f)
        val rawGlyph = createRawGlyphDrawable(activity, iconCode, innerTint, innerSizeDp, isMin, isAur)

        return when {
            isMin || isAur -> rawGlyph
            isGlass -> ProUnifiedThemeIcons.wrapForLiquidGlass(activity, rawGlyph, tintColor, sizeDp + 4f)
            else -> ProUnifiedThemeIcons.wrapForClassicBadge(activity, rawGlyph, iconCode, sizeDp + 4f)
        }
    }

    private fun createRawGlyphDrawable(
        context: Context,
        iconCode: Int,
        tintColor: Int,
        sizeDp: Float,
        isMinimalist: Boolean,
        isAurora: Boolean
    ): Drawable {
        val d = context.resources.displayMetrics.density
        val sizePx = (sizeDp * d).toInt().coerceAtLeast(22)
        return object : Drawable() {
            private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.STROKE
                strokeWidth = (if (isMinimalist) 1.72f else 1.85f) * d
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                if (isAurora) {
                    setShadowLayer(4.5f * d, 0f, 0f, Color.argb(175, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor)))
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
                val r = min(bounds.width(), bounds.height()) * 0.42f

                if (isAurora) {
                    val haloP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        shader = RadialGradient(
                            cx, cy, (r * 1.15f).coerceAtLeast(1f),
                            Color.argb(65, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor)),
                            Color.TRANSPARENT,
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawCircle(cx, cy, r * 1.15f, haloP)
                }

                when (iconCode) {
                    100 -> { // קידומות SI: "10s"
                        fillP.textSize = r * 0.96f
                        canvas.drawText("10ˢ", cx, cy + r * 0.34f, fillP)
                    }
                    101 -> { // שטח קרקע: מסגרת חלקה + סמן מיקום
                        canvas.drawRoundRect(RectF(cx - r * 0.78f, cy - r * 0.62f, cx + r * 0.78f, cy + r * 0.62f), r * 0.16f, r * 0.16f, strokeP)
                        canvas.drawCircle(cx + r * 0.18f, cy - r * 0.08f, r * 0.20f, strokeP)
                        canvas.drawLine(cx - r * 0.50f, cy + r * 0.25f, cx - r * 0.05f, cy - r * 0.20f, strokeP)
                    }
                    102 -> { // מספרים גדולים: חשבונייה (Abacus)
                        canvas.drawRoundRect(RectF(cx - r * 0.76f, cy - r * 0.68f, cx + r * 0.76f, cy + r * 0.68f), r * 0.14f, r * 0.14f, strokeP)
                        canvas.drawLine(cx - r * 0.76f, cy - r * 0.22f, cx + r * 0.76f, cy - r * 0.22f, strokeP)
                        canvas.drawLine(cx - r * 0.76f, cy + r * 0.24f, cx + r * 0.76f, cy + r * 0.24f, strokeP)
                        canvas.drawCircle(cx - r * 0.32f, cy - r * 0.22f, r * 0.12f, fillP)
                        canvas.drawCircle(cx + r * 0.28f, cy + r * 0.24f, r * 0.12f, fillP)
                    }
                    103 -> { // זמן תרבותי: שעון חול
                        canvas.drawLine(cx - r * 0.62f, cy - r * 0.74f, cx + r * 0.62f, cy - r * 0.74f, strokeP)
                        canvas.drawLine(cx - r * 0.62f, cy + r * 0.74f, cx + r * 0.62f, cy + r * 0.74f, strokeP)
                        val hg = Path().apply {
                            moveTo(cx - r * 0.50f, cy - r * 0.74f)
                            lineTo(cx + r * 0.50f, cy - r * 0.74f)
                            lineTo(cx, cy)
                            lineTo(cx + r * 0.50f, cy + r * 0.74f)
                            lineTo(cx - r * 0.50f, cy + r * 0.74f)
                            lineTo(cx, cy)
                            close()
                        }
                        canvas.drawPath(hg, strokeP)
                    }
                    104 -> { // צפיפות חוט: סיב מתפתל
                        val yarn = Path().apply {
                            moveTo(cx - r * 0.78f, cy + r * 0.22f)
                            cubicTo(cx - r * 0.32f, cy - r * 0.65f, cx + r * 0.32f, cy + r * 0.65f, cx + r * 0.78f, cy - r * 0.22f)
                        }
                        canvas.drawPath(yarn, strokeP)
                        canvas.drawCircle(cx - r * 0.78f, cy + r * 0.22f, r * 0.11f, fillP)
                        canvas.drawCircle(cx + r * 0.78f, cy - r * 0.22f, r * 0.11f, fillP)
                    }
                    105 -> { // קלוריות מזון: צלחת/המבורגר + להבת אנרגיה
                        canvas.drawArc(RectF(cx - r * 0.74f, cy - r * 0.66f, cx + r * 0.74f, cy + r * 0.24f), 180f, 180f, true, strokeP)
                        canvas.drawLine(cx - r * 0.80f, cy + r * 0.10f, cx + r * 0.80f, cy + r * 0.10f, strokeP)
                        canvas.drawRoundRect(RectF(cx - r * 0.72f, cy + r * 0.32f, cx + r * 0.72f, cy + r * 0.68f), r * 0.18f, r * 0.18f, strokeP)
                    }
                    106 -> { // משקל זהב: פירמידת מטילי זהב
                        val bot = Path().apply {
                            moveTo(cx - r * 0.62f, cy + r * 0.14f)
                            lineTo(cx + r * 0.62f, cy + r * 0.14f)
                            lineTo(cx + r * 0.82f, cy + r * 0.66f)
                            lineTo(cx - r * 0.82f, cy + r * 0.66f)
                            close()
                        }
                        val top = Path().apply {
                            moveTo(cx - r * 0.36f, cy - r * 0.56f)
                            lineTo(cx + r * 0.36f, cy - r * 0.56f)
                            lineTo(cx + r * 0.54f, cy - r * 0.04f)
                            lineTo(cx - r * 0.54f, cy - r * 0.04f)
                            close()
                        }
                        canvas.drawPath(bot, strokeP)
                        canvas.drawPath(top, strokeP)
                    }
                    107 -> { // נפח עץ: גזע עץ / מסור נגרות
                        canvas.drawCircle(cx - r * 0.30f, cy, r * 0.50f, strokeP)
                        canvas.drawCircle(cx - r * 0.30f, cy, r * 0.18f, strokeP)
                        canvas.drawLine(cx - r * 0.30f, cy - r * 0.50f, cx + r * 0.74f, cy - r * 0.50f, strokeP)
                        canvas.drawLine(cx - r * 0.30f, cy + r * 0.50f, cx + r * 0.74f, cy + r * 0.50f, strokeP)
                        canvas.drawArc(RectF(cx + r * 0.44f, cy - r * 0.50f, cx + r * 0.84f, cy + r * 0.50f), -90f, 180f, false, strokeP)
                    }
                    108 -> { // כושר קירור: פתית שלג קריסטלי
                        for (i in 0 until 3) {
                            val a = Math.toRadians((i * 60).toDouble())
                            canvas.drawLine(
                                (cx - cos(a) * r * 0.78f).toFloat(), (cy - sin(a) * r * 0.78f).toFloat(),
                                (cx + cos(a) * r * 0.78f).toFloat(), (cy + sin(a) * r * 0.78f).toFloat(),
                                strokeP
                            )
                        }
                        canvas.drawCircle(cx, cy, r * 0.22f, strokeP)
                    }
                    109 -> { // זרימת אוויר: 3 קווי רוח מתעגלים
                        for (off in listOf(-0.44f, 0f, 0.44f)) {
                            val y = cy + r * off
                            canvas.drawLine(cx - r * 0.78f, y, cx + r * (0.48f - abs(off) * 0.5f), y, strokeP)
                        }
                        canvas.drawArc(RectF(cx + r * 0.20f, cy - r * 0.76f, cx + r * 0.72f, cy - r * 0.24f), -130f, 220f, false, strokeP)
                    }
                    110 -> { // קשיות מים: טיפת מים חצויה בגבישים
                        val drop = Path().apply {
                            moveTo(cx, cy - r * 0.82f)
                            quadTo(cx + r * 0.74f, cy + r * 0.14f, cx, cy + r * 0.80f)
                            quadTo(cx - r * 0.74f, cy + r * 0.14f, cx, cy - r * 0.82f)
                        }
                        canvas.drawPath(drop, strokeP)
                        canvas.drawCircle(cx - r * 0.12f, cy + r * 0.24f, r * 0.11f, fillP)
                        canvas.drawCircle(cx + r * 0.16f, cy + r * 0.04f, r * 0.09f, fillP)
                    }
                    111 -> { // מתח פנים: 3 גלים אופקיים מקבילים
                        for (off in listOf(-0.42f, 0f, 0.42f)) {
                            val y = cy + r * off
                            val wv = Path().apply {
                                moveTo(cx - r * 0.76f, y)
                                quadTo(cx - r * 0.38f, y - r * 0.22f, cx, y)
                                quadTo(cx + r * 0.38f, y + r * 0.22f, cx + r * 0.76f, y)
                            }
                            canvas.drawPath(wv, strokeP)
                        }
                    }
                    112 -> { // פרמיטיביות ופרמאביליות: קבל + קו שדה
                        canvas.drawLine(cx - r * 0.16f, cy - r * 0.68f, cx - r * 0.16f, cy + r * 0.68f, strokeP)
                        canvas.drawLine(cx + r * 0.16f, cy - r * 0.68f, cx + r * 0.16f, cy + r * 0.68f, strokeP)
                        canvas.drawLine(cx - r * 0.80f, cy, cx - r * 0.16f, cy, strokeP)
                        canvas.drawLine(cx + r * 0.16f, cy, cx + r * 0.80f, cy, strokeP)
                        canvas.drawCircle(cx - r * 0.56f, cy, r * 0.10f, fillP)
                        canvas.drawCircle(cx + r * 0.56f, cy, r * 0.10f, fillP)
                    }
                    113 -> { // אינרציה: חץ סיבוב סביב ציר
                        canvas.drawArc(RectF(cx - r * 0.68f, cy - r * 0.68f, cx + r * 0.68f, cy + r * 0.68f), -50f, 280f, false, strokeP)
                        canvas.drawCircle(cx, cy, r * 0.18f, fillP)
                        canvas.drawLine(cx + r * 0.44f, cy - r * 0.52f, cx + r * 0.76f, cy - r * 0.46f, strokeP)
                    }
                    114 -> { // צפיפות חום וקיבול חום: מדחום מעל גלי חום
                        canvas.drawLine(cx, cy - r * 0.74f, cx, cy + r * 0.18f, strokeP)
                        canvas.drawCircle(cx, cy + r * 0.26f, r * 0.22f, strokeP)
                        canvas.drawLine(cx - r * 0.68f, cy + r * 0.68f, cx + r * 0.68f, cy + r * 0.68f, strokeP)
                        canvas.drawLine(cx + r * 0.34f, cy - r * 0.46f, cx + r * 0.66f, cy - r * 0.46f, strokeP)
                        canvas.drawLine(cx + r * 0.34f, cy - r * 0.16f, cx + r * 0.58f, cy - r * 0.16f, strokeP)
                    }
                    115 -> { // נפח סגולי וצפיפות: חיצים מתכנסים לקו מרכזי
                        canvas.drawLine(cx - r * 0.74f, cy, cx + r * 0.74f, cy, strokeP)
                        canvas.drawLine(cx, cy - r * 0.78f, cx, cy - r * 0.22f, strokeP)
                        canvas.drawLine(cx - r * 0.24f, cy - r * 0.44f, cx, cy - r * 0.22f, strokeP)
                        canvas.drawLine(cx + r * 0.24f, cy - r * 0.44f, cx, cy - r * 0.22f, strokeP)
                        canvas.drawLine(cx, cy + r * 0.78f, cx, cy + r * 0.22f, strokeP)
                        canvas.drawLine(cx - r * 0.24f, cy + r * 0.44f, cx, cy + r * 0.22f, strokeP)
                        canvas.drawLine(cx + r * 0.24f, cy + r * 0.44f, cx, cy + r * 0.22f, strokeP)
                    }
                    116 -> { // מהירות ותאוצה זוויתית: קשת זווית + מחוג
                        canvas.drawArc(RectF(cx - r * 0.74f, cy - r * 0.74f, cx + r * 0.74f, cy + r * 0.74f), -140f, 220f, false, strokeP)
                        canvas.drawLine(cx, cy, cx + r * 0.56f, cy - r * 0.38f, strokeP)
                        canvas.drawCircle(cx, cy, r * 0.14f, fillP)
                    }
                    117 -> { // צפיפות זרם, מוליכות ומטען: קובייה תלת-ממדית עם פולס
                        val hex = Path().apply {
                            moveTo(cx, cy - r * 0.78f)
                            lineTo(cx + r * 0.68f, cy - r * 0.38f)
                            lineTo(cx + r * 0.68f, cy + r * 0.38f)
                            lineTo(cx, cy + r * 0.78f)
                            lineTo(cx - r * 0.68f, cy + r * 0.38f)
                            lineTo(cx - r * 0.68f, cy - r * 0.38f)
                            close()
                        }
                        canvas.drawPath(hex, strokeP)
                        canvas.drawLine(cx + r * 0.10f, cy - r * 0.34f, cx - r * 0.18f, cy + r * 0.04f, strokeP)
                        canvas.drawLine(cx - r * 0.18f, cy + r * 0.04f, cx + r * 0.16f, cy + r * 0.04f, strokeP)
                        canvas.drawLine(cx + r * 0.16f, cy + r * 0.04f, cx - r * 0.10f, cy + r * 0.38f, strokeP)
                    }
                    else -> { // 313: כותרת קטגוריית 18 הממירים המורחבים (סרגל מדורג)
                        canvas.save()
                        canvas.rotate(-40f, cx, cy)
                        canvas.drawRoundRect(RectF(cx - r * 0.86f, cy - r * 0.30f, cx + r * 0.86f, cy + r * 0.30f), r * 0.12f, r * 0.12f, strokeP)
                        for (tx in listOf(-0.48f, -0.16f, 0.16f, 0.48f)) {
                            canvas.drawLine(cx + r * tx, cy - r * 0.30f, cx + r * tx, cy + r * 0.02f, strokeP)
                        }
                        canvas.restore()
                    }
                }
            }

            override fun setAlpha(alpha: Int) { strokeP.alpha = alpha; fillP.alpha = alpha }
            override fun setColorFilter(colorFilter: ColorFilter?) { strokeP.colorFilter = colorFilter; fillP.colorFilter = colorFilter }
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }.apply { setBounds(0, 0, sizePx, sizePx) }
    }
}

// =========================================================================
// קנבס מונפש אישי לכל אחד מ-18 הממירים, מותאם ל-4 ערכות הנושא
// =========================================================================
private class ExtUnitHeroCanvasView(
    private val activity: MainActivity,
    private val spec: ExtUnitToolSpec
) : View(activity) {

    private val d = resources.displayMetrics.density
    private var phase = 0f
    private var animator: ValueAnimator? = null

    var liveInputVal: Double = spec.defaultValue
    var liveBaseVal: Double = spec.units[spec.defaultFrom].toBase(spec.defaultValue)
    var liveResultVal: Double = spec.units[spec.defaultTo].fromBase(liveBaseVal)
    var liveFromSym: String = spec.units[spec.defaultFrom].symbol
    var liveToSym: String = spec.units[spec.defaultTo].symbol

    private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val path = Path()
    private val rect = RectF()

    fun updateLiveData(inputV: Double, baseV: Double, resultV: Double, fromSym: String, toSym: String) {
        liveInputVal = inputV
        liveBaseVal = baseV
        liveResultVal = resultV
        liveFromSym = fromSym
        liveToSym = toSym
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (ProCanvasAnimations.isCanvasAnimationsEnabled) {
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 12_000L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    phase += 0.025f
                    if (phase > 6283f) phase = 0f
                    postInvalidateOnAnimation()
                }
                start()
            }
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    private fun withAlpha(color: Int, a: Int): Int =
        Color.argb(a.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 2f || h <= 2f) return

        val pal = activity.getProCanvasColors()
        val style = ProCanvasAnimations.resolveThemeStyle(activity)
        val c1 = pal.accent1
        val c2 = pal.accent2
        val c3 = pal.accent3
        val ink = if (pal.isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE

        // רקע כרטיס קנבס מותאם לכל אחת מ-4 ערכות הנושא
        rect.set(2f * d, 2f * d, w - 2f * d, h - 2f * d)
        val corner = 20f * d
        when (style) {
            ProCanvasAnimations.ThemeRenderStyle.MINIMALIST_PASTEL -> {
                fillP.shader = LinearGradient(0f, 0f, w, h, MinimalistTheme.CARD_WHITE, withAlpha(c1, 36), Shader.TileMode.CLAMP)
                canvas.drawRoundRect(rect, corner, corner, fillP)
                fillP.shader = null
                strokeP.clearShadowLayer()
                strokeP.strokeWidth = 1.2f * d
                strokeP.color = MinimalistTheme.resolveBorderStrokeColor(activity, Color.parseColor("#E3DFD9"))
                canvas.drawRoundRect(rect, corner, corner, strokeP)
            }
            ProCanvasAnimations.ThemeRenderStyle.LIQUID_GLASS -> {
                fillP.shader = LinearGradient(0f, 0f, w, h, Color.argb(70, 255, 255, 255), withAlpha(c1, 38), Shader.TileMode.CLAMP)
                canvas.drawRoundRect(rect, corner, corner, fillP)
                fillP.shader = null
                strokeP.clearShadowLayer()
                strokeP.strokeWidth = 1.3f * d
                strokeP.color = Color.argb(120, 255, 255, 255)
                canvas.drawRoundRect(rect, corner, corner, strokeP)
            }
            ProCanvasAnimations.ThemeRenderStyle.AURORA_NEON -> {
                fillP.shader = LinearGradient(0f, 0f, w, h, withAlpha(AuroraBorealisTheme.EMERALD_NIGHT, 235), withAlpha(c1, 45), Shader.TileMode.CLAMP)
                canvas.drawRoundRect(rect, corner, corner, fillP)
                fillP.shader = null
                strokeP.strokeWidth = 1.4f * d
                strokeP.color = withAlpha(c1, 185)
                strokeP.setShadowLayer(6f * d, 0f, 0f, withAlpha(c1, 160))
                canvas.drawRoundRect(rect, corner, corner, strokeP)
            }
            ProCanvasAnimations.ThemeRenderStyle.CLASSIC_VECTOR -> {
                fillP.shader = null
                fillP.color = Utils.lightenColor(activity.bgColor, 0.08f)
                canvas.drawRoundRect(rect, corner, corner, fillP)
                // רשת הנדסית עדינה
                strokeP.clearShadowLayer()
                strokeP.strokeWidth = 0.7f * d
                strokeP.color = withAlpha(c1, 24)
                var gx = 20f * d
                while (gx < w - 10f * d) { canvas.drawLine(gx, 8f * d, gx, h - 8f * d, strokeP); gx += 20f * d }
                strokeP.strokeWidth = 1.3f * d
                strokeP.color = withAlpha(c1, 130)
                canvas.drawRoundRect(rect, corner, corner, strokeP)
            }
        }

        val cx = w * 0.5f
        val cy = h * 0.45f
        val animGlow = (style == ProCanvasAnimations.ThemeRenderStyle.AURORA_NEON)
        if (animGlow) strokeP.setShadowLayer(5f * d, 0f, 0f, withAlpha(c1, 165)) else strokeP.clearShadowLayer()

        // ציור ייעודי מונפש לכל אחד מ-18 הכלים
        when (spec.tabIndex) {
            100 -> { // סקאלת חזקות SI מונפשת
                val yBar = cy
                strokeP.strokeWidth = 2.4f * d; strokeP.color = withAlpha(c1, 165)
                canvas.drawLine(w * 0.10f, yBar, w * 0.90f, yBar, strokeP)
                val labels = arrayOf("10⁻⁹", "10⁻⁶", "10⁻³", "10⁰", "10³", "10⁶", "10⁹")
                textP.textSize = 10f * d; textP.color = withAlpha(ink, 180)
                for (i in labels.indices) {
                    val tx = w * (0.12f + i * (0.76f / (labels.size - 1)))
                    canvas.drawLine(tx, yBar - 7f * d, tx, yBar + 7f * d, strokeP)
                    canvas.drawText(labels[i], tx, yBar - 12f * d, textP)
                }
                val expClamped = if (liveBaseVal > 0) log10(abs(liveBaseVal)).toFloat().coerceIn(-9f, 9f) else 0f
                val px = w * (0.50f + (expClamped / 9f) * 0.38f) + sin(phase * 1.4f) * 3f * d
                fillP.color = c2
                canvas.drawCircle(px, yBar, 7.5f * d, fillP)
            }

            101 -> { // חלקת קרקע טופוגרפית פרופורציונלית
                val ratio = (sqrt(abs(liveBaseVal).coerceIn(10.0, 50_000.0)) / sqrt(50_000.0)).toFloat().coerceIn(0.22f, 0.92f)
                val pw = w * 0.56f * ratio
                val ph = h * 0.50f * ratio
                rect.set(cx - pw * 0.5f, cy - ph * 0.5f, cx + pw * 0.5f, cy + ph * 0.5f)
                fillP.color = withAlpha(pal.positive, 55)
                canvas.drawRoundRect(rect, 8f * d, 8f * d, fillP)
                strokeP.strokeWidth = 2.2f * d; strokeP.color = pal.positive
                canvas.drawRoundRect(rect, 8f * d, 8f * d, strokeP)
                // קו סריקת מדידה
                val scanX = rect.left + ((sin(phase) * 0.5f + 0.5f) * rect.width())
                strokeP.strokeWidth = 1.4f * d; strokeP.color = withAlpha(c1, 160)
                canvas.drawLine(scanX, rect.top, scanX, rect.bottom, strokeP)
            }

            102 -> { // מגדלי סדרי גודל (10^3 .. 10^15)
                val bars = 6
                val bw = w * 0.08f
                val startX = w * 0.18f
                for (i in 0 until bars) {
                    val bh = (12f + i * 10f + sin(phase * 1.3f + i) * 4f) * d
                    val bx = startX + i * w * 0.115f
                    rect.set(bx, cy + 28f * d - bh, bx + bw, cy + 28f * d)
                    fillP.color = withAlpha(if (i % 2 == 0) c1 else c2, 85 + i * 22)
                    canvas.drawRoundRect(rect, 4f * d, 4f * d, fillP)
                }
            }

            103 -> { // שעון חול + גלגל חלקים עברי מונפש
                val r = min(w, h) * 0.28f
                strokeP.strokeWidth = 2f * d; strokeP.color = c1
                canvas.drawCircle(cx, cy, r, strokeP)
                val a1 = phase * 1.1f
                val a2 = phase * 0.25f
                canvas.drawLine(cx, cy, cx + cos(a1) * r * 0.78f, cy + sin(a1) * r * 0.78f, strokeP)
                strokeP.color = c2
                canvas.drawLine(cx, cy, cx + cos(a2) * r * 0.52f, cy + sin(a2) * r * 0.52f, strokeP)
                fillP.color = c3
                canvas.drawCircle(cx, cy, 4f * d, fillP)
            }

            104 -> { // סיב טקסטיל מוגדל שעוביו מגיב ל-Tex
                val texNorm = (abs(liveBaseVal).coerceIn(1.0, 100.0) / 100.0).toFloat()
                val fiberW = (3.5f + texNorm * 14f) * d
                path.reset()
                var x = w * 0.10f
                while (x <= w * 0.90f) {
                    val y = cy + sin(x * 0.045f + phase * 1.8f) * 14f * d
                    if (x == w * 0.10f) path.moveTo(x, y) else path.lineTo(x, y)
                    x += 6f * d
                }
                strokeP.strokeWidth = fiberW; strokeP.color = withAlpha(c1, 85)
                canvas.drawPath(path, strokeP)
                strokeP.strokeWidth = 2f * d; strokeP.color = c2
                canvas.drawPath(path, strokeP)
            }

            105 -> { // טבעת אנרגיה תזונתית (kcal)
                val kcal = abs(liveBaseVal) / 4184.0
                val pct = (kcal / 2000.0).toFloat().coerceIn(0.06f, 1.0f)
                val r = min(w, h) * 0.30f
                rect.set(cx - r, cy - r, cx + r, cy + r)
                strokeP.strokeWidth = 8f * d; strokeP.color = withAlpha(ink, 28)
                canvas.drawCircle(cx, cy, r, strokeP)
                strokeP.color = pal.warning
                canvas.drawArc(rect, -90f, 360f * pct, false, strokeP)
                val dotA = Math.toRadians((-90.0 + 360.0 * pct))
                fillP.color = c1
                canvas.drawCircle(cx + (cos(dotA) * r).toFloat(), cy + (sin(dotA) * r).toFloat(), 5.5f * d, fillP)
            }

            106 -> { // מטיל זהב מבריק תלת-ממדי מונפש
                val gold = Color.parseColor("#FFD54F")
                val goldDark = Color.parseColor("#FFB300")
                path.reset()
                path.moveTo(cx - 54f * d, cy + 20f * d)
                path.lineTo(cx + 54f * d, cy + 20f * d)
                path.lineTo(cx + 38f * d, cy - 22f * d)
                path.lineTo(cx - 38f * d, cy - 22f * d)
                path.close()
                val shX = cx + sin(phase * 1.4f) * 45f * d
                fillP.shader = LinearGradient(shX - 30f * d, cy - 22f * d, shX + 30f * d, cy + 20f * d, intArrayOf(goldDark, Color.WHITE, gold), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
                canvas.drawPath(path, fillP)
                fillP.shader = null
                strokeP.strokeWidth = 1.8f * d; strokeP.color = goldDark
                canvas.drawPath(path, strokeP)
                textP.textSize = 11f * d; textP.color = Color.parseColor("#4E342E")
                canvas.drawText("999.9 FINE GOLD", cx, cy + 6f * d, textP)
            }

            107 -> { // פרוסת גזע עץ + טבעות שנתיות + קורות חתוכות
                val r = 28f * d
                val lx = cx - 36f * d
                fillP.color = withAlpha(Color.parseColor("#D7A86E"), 95)
                canvas.drawCircle(lx, cy, r, fillP)
                strokeP.strokeWidth = 1.6f * d; strokeP.color = Color.parseColor("#8D6E63")
                for (k in 1..4) canvas.drawCircle(lx, cy, r * (k / 4f), strokeP)
                // 3 לוחות עץ מימין
                for (b in 0 until 3) {
                    val ox = sin(phase * 1.2f + b) * 4f * d
                    rect.set(cx + 6f * d + ox, cy - 24f * d + b * 17f * d, cx + 82f * d + ox, cy - 11f * d + b * 17f * d)
                    fillP.color = withAlpha(c2, 130)
                    canvas.drawRoundRect(rect, 3f * d, 3f * d, fillP)
                    canvas.drawRoundRect(rect, 3f * d, 3f * d, strokeP)
                }
            }

            108 -> { // מאוורר קירור ופתית שלג מסתובב + גלי קור
                val r = 26f * d
                canvas.save()
                canvas.rotate(phase * 45f, cx, cy)
                strokeP.strokeWidth = 2.4f * d; strokeP.color = c3
                for (i in 0 until 6) {
                    val a = Math.toRadians(i * 60.0)
                    canvas.drawLine(cx, cy, cx + (cos(a) * r).toFloat(), cy + (sin(a) * r).toFloat(), strokeP)
                }
                canvas.restore()
                for (s in -1..1) {
                    val y = cy + s * 16f * d
                    val off = ((phase * 30f * d) % (40f * d))
                    strokeP.strokeWidth = 1.5f * d; strokeP.color = withAlpha(c1, 140)
                    canvas.drawLine(cx + 38f * d + off * 0.5f, y, cx + 74f * d + off * 0.5f, y, strokeP)
                }
            }

            109 -> { // מנהרת רוח עם חלקיקי אוויר זורמים
                val speed = (1f + log10(abs(liveBaseVal * 3600.0).coerceAtLeast(1.0)).toFloat() * 0.5f).coerceIn(0.8f, 3.2f)
                strokeP.strokeWidth = 2f * d
                for (i in 0 until 5) {
                    val y = cy - 26f * d + i * 13f * d
                    val x = w * 0.10f + ((phase * 48f * speed * d + i * 42f * d) % (w * 0.72f))
                    strokeP.color = withAlpha(if (i % 2 == 0) c1 else c3, 185)
                    canvas.drawLine(x, y, x + 36f * d, y, strokeP)
                }
            }

            110 -> { // טיפת מים עם יוני סידן/מגנזיום לפי דרגת הקשיות
                val ppm = abs(liveBaseVal)
                val dropCol = when {
                    ppm < 60.0 -> Color.parseColor("#4FC3F7")
                    ppm < 180.0 -> Color.parseColor("#FFB74D")
                    else -> Color.parseColor("#EF5350")
                }
                path.reset()
                path.moveTo(cx, cy - 32f * d)
                path.quadTo(cx + 34f * d, cy + 8f * d, cx, cy + 32f * d)
                path.quadTo(cx - 34f * d, cy + 8f * d, cx, cy - 32f * d)
                fillP.color = withAlpha(dropCol, 70)
                canvas.drawPath(path, fillP)
                strokeP.strokeWidth = 2.2f * d; strokeP.color = dropCol
                canvas.drawPath(path, strokeP)
                val ions = (ppm / 45.0).toInt().coerceIn(1, 9)
                fillP.color = dropCol
                for (k in 0 until ions) {
                    val ix = cx + sin(phase * 1.4f + k * 1.9f) * 11f * d
                    val iy = cy + 6f * d + cos(phase * 1.1f + k * 2.3f) * 12f * d
                    canvas.drawCircle(ix, iy, 2.6f * d, fillP)
                }
            }

            111 -> { // מניסקוס נוזל ווקטורי מתח פנים
                path.reset()
                path.moveTo(w * 0.14f, cy - 16f * d)
                path.quadTo(cx, cy + 16f * d + sin(phase * 1.8f) * 7f * d, w * 0.86f, cy - 16f * d)
                strokeP.strokeWidth = 3f * d; strokeP.color = c3
                canvas.drawPath(path, strokeP)
                // חצי מתח פנים בצדדים
                strokeP.strokeWidth = 2f * d; strokeP.color = c1
                canvas.drawLine(w * 0.24f, cy - 10f * d, w * 0.40f, cy - 10f * d, strokeP)
                canvas.drawLine(w * 0.76f, cy - 10f * d, w * 0.60f, cy - 10f * d, strokeP)
            }

            112 -> { // לוחות קבל וקווי שדה חשמלי/מגנטי נעים
                val lx = cx - 44f * d; val rx = cx + 44f * d
                strokeP.strokeWidth = 4f * d; strokeP.color = c1
                canvas.drawLine(lx, cy - 28f * d, lx, cy + 28f * d, strokeP)
                strokeP.color = c2
                canvas.drawLine(rx, cy - 28f * d, rx, cy + 28f * d, strokeP)
                strokeP.strokeWidth = 1.6f * d; strokeP.color = withAlpha(c3, 185)
                for (k in -2..2) {
                    val y = cy + k * 11f * d
                    canvas.drawLine(lx + 6f * d, y, rx - 6f * d, y, strokeP)
                    val px = lx + 8f * d + ((phase * 36f * d + (k + 2) * 14f * d) % (rx - lx - 16f * d))
                    fillP.color = c3
                    canvas.drawCircle(px, y, 2.8f * d, fillP)
                }
            }

            113 -> { // גלגל תנופה (Flywheel) מסתובב עם משקולות
                val r = 30f * d
                canvas.save()
                canvas.rotate(phase * 52f, cx, cy)
                strokeP.strokeWidth = 3f * d; strokeP.color = c1
                canvas.drawCircle(cx, cy, r, strokeP)
                canvas.drawLine(cx - r, cy, cx + r, cy, strokeP)
                canvas.drawLine(cx, cy - r, cx, cy + r, strokeP)
                fillP.color = c2
                canvas.drawCircle(cx - r, cy, 5.5f * d, fillP)
                canvas.drawCircle(cx + r, cy, 5.5f * d, fillP)
                canvas.restore()
            }

            114 -> { // גלי חום תרמיים עולים מפלטה לוהטת
                strokeP.strokeWidth = 3.5f * d; strokeP.color = pal.warning
                canvas.drawLine(w * 0.22f, cy + 24f * d, w * 0.78f, cy + 24f * d, strokeP)
                strokeP.strokeWidth = 2f * d
                for (i in -2..2) {
                    val bx = cx + i * 24f * d
                    path.reset()
                    var y = cy + 18f * d
                    var first = true
                    while (y >= cy - 28f * d) {
                        val x = bx + sin(y * 0.14f + phase * 2.2f + i) * 6f * d
                        if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
                        y -= 5f * d
                    }
                    strokeP.color = withAlpha(if (i % 2 == 0) pal.warning else c1, 185)
                    canvas.drawPath(path, strokeP)
                }
            }

            115 -> { // תא מולקולות (צפיפות מול נפח סגולי)
                rect.set(cx - 56f * d, cy - 26f * d, cx + 56f * d, cy + 26f * d)
                strokeP.strokeWidth = 1.8f * d; strokeP.color = c1
                canvas.drawRoundRect(rect, 10f * d, 10f * d, strokeP)
                val rho = if (liveBaseVal > 0) 1.0 / liveBaseVal else 1000.0
                val count = (log10(rho.coerceAtLeast(1.0)) * 5.0).toInt().coerceIn(5, 22)
                for (k in 0 until count) {
                    val px = cx + sin(phase * 1.1f + k * 2.1f) * 44f * d
                    val py = cy + cos(phase * 1.3f + k * 1.7f) * 18f * d
                    fillP.color = if (k % 2 == 0) c1 else c3
                    canvas.drawCircle(px, py, 3.2f * d, fillP)
                }
            }

            116 -> { // רוטור מד-סיבובים (Tachometer) מונפש
                val r = 30f * d
                val spd = (1f + log10(abs(liveBaseVal).coerceAtLeast(1.0)).toFloat()).coerceIn(1f, 4f)
                strokeP.strokeWidth = 2.4f * d; strokeP.color = withAlpha(c1, 130)
                rect.set(cx - r, cy - r, cx + r, cy + r)
                canvas.drawArc(rect, 140f, 260f, false, strokeP)
                val ang = phase * 2.2f * spd
                strokeP.color = c2; strokeP.strokeWidth = 2.8f * d
                canvas.drawLine(cx, cy, cx + cos(ang) * r * 0.85f, cy + sin(ang) * r * 0.85f, strokeP)
                fillP.color = c1
                canvas.drawCircle(cx, cy, 4.5f * d, fillP)
            }

            else -> { // 117: חתך מוליך נחושת עם מטענים זורמים
                val r = 28f * d
                strokeP.strokeWidth = 3f * d; strokeP.color = pal.warning
                fillP.color = withAlpha(pal.warning, 42)
                canvas.drawCircle(cx, cy, r, fillP)
                canvas.drawCircle(cx, cy, r, strokeP)
                for (k in 0 until 7) {
                    val a = phase * 1.6f + k * (2f * PI.toFloat() / 7f)
                    val rr = (10f + (k % 2) * 10f) * d
                    fillP.color = c1
                    canvas.drawCircle(cx + cos(a) * rr, cy + sin(a) * rr, 3.3f * d, fillP)
                }
            }
        }

        // שורת תוצאה חיה בתחתית הקנבס
        strokeP.clearShadowLayer()
        val inStr = ProExtendedUnitConverters.formatSmartValue(liveInputVal, 4)
        val outStr = ProExtendedUnitConverters.formatSmartValue(liveResultVal, 5)
        val summary = "\u200E$inStr $liveFromSym  =  $outStr $liveToSym"
        textP.textSize = 12.5f * d
        textP.color = ink
        canvas.drawText(summary, cx, h - 11f * d, textP)
    }
}

// =========================================================================
// בונה מסך הממיר הייעודי (Extension על MainActivity עבור Tabs 100..117)
// =========================================================================
fun MainActivity.createProExtendedUnitScreen(tabIndex: Int): View {
    val spec = ProExtendedUnitConverters.getSpec(tabIndex)
    val isMin = isMinimalistMode
    val textPrimary = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
    val textSecondary = if (isMin) MinimalistTheme.TEXT_MUTED_GREY else Color.parseColor("#B0BEC5")
    val accentCol = when {
        isMin -> MinimalistTheme.ACCENT_SAGE_GREEN
        isAuroraMode -> AuroraBorealisTheme.AURORA_PRIMARY
        else -> primaryColor
    }

    val scroll = ScrollView(this).apply { isFillViewport = true }
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16f), dp(8f), dp(16f), dp(26f))
    }
    scroll.addView(root)

    // 1. כותרת ותיאור תחום
    val headerCard = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
        background = when {
            isMin -> MinimalistTheme.createMinimalistCardDrawable(this@createProExtendedUnitScreen, 20f, 1)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProExtendedUnitScreen, 20f)
            else -> GradientDrawable().apply {
                cornerRadius = dp(20f).toFloat()
                setColor(Utils.lightenColor(bgColor, 0.08f))
                setStroke(dp(1.2f), MinimalistTheme.resolveBorderStrokeColor(this@createProExtendedUnitScreen, accentCol))
            }
        }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(12f)
        }
    }

    headerCard.addView(TextView(this).apply {
        text = "\u200F${spec.title}"
        setTextColor(textPrimary)
        textSize = 18.5f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.RIGHT
    })
    headerCard.addView(TextView(this).apply {
        text = "\u200F${spec.subtitle}"
        setTextColor(textSecondary)
        textSize = 13.2f
        gravity = Gravity.RIGHT
        setPadding(0, dp(3f), 0, dp(4f))
    })
    headerCard.addView(TextView(this).apply {
        text = "\u200E${spec.formulaHint}"
        setTextColor(if (isMin) MinimalistTheme.TEXT_CHARCOAL else accentCol)
        textSize = 12.2f
        typeface = Typeface.MONOSPACE
        gravity = Gravity.RIGHT
    })
    root.addView(headerCard)

    // 2. קנבס אינטראקטיבי מונפש אישי לכלי
    val heroCanvas = ExtUnitHeroCanvasView(this, spec).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(136f)
        ).apply { bottomMargin = dp(12f) }
    }
    root.addView(heroCanvas)

    var fromIdx = spec.defaultFrom.coerceIn(0, spec.units.lastIndex)
    var toIdx = spec.defaultTo.coerceIn(0, spec.units.lastIndex)

    // 3. שורת דוגמאות מהירות (Presets)
    val presetScroll = HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(10f)
        }
    }
    val presetRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    presetScroll.addView(presetRow)
    root.addView(presetScroll)

    // 4. תיבת קלט ובחירת יחידות מקור/יעד
    val inputBox = createInputBox("הזן ערך להמרה (${spec.units[fromIdx].symbol})").apply {
        tag = "HAS_CUSTOM_LIVE_WATCHER"
        setText(ProExtendedUnitConverters.formatSmartValue(spec.defaultValue, 4))
    }
    root.addView(inputBox)

    lateinit var fromBtn: TextView
    lateinit var toBtn: TextView
    val mainResultTv = createResultTextView().apply {
        setPadding(dp(14f), dp(14f), dp(14f), dp(14f))
        background = when {
            isMin -> MinimalistTheme.createMinimalistCardDrawable(this@createProExtendedUnitScreen, 20f, 0)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProExtendedUnitScreen, 20f)
            else -> GradientDrawable().apply {
                cornerRadius = dp(20f).toFloat()
                setColor(Utils.lightenColor(bgColor, 0.07f))
                setStroke(dp(1.2f), MinimalistTheme.resolveBorderStrokeColor(this@createProExtendedUnitScreen, accentCol))
            }
        }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(6f); bottomMargin = dp(12f)
        }
    }

    val allUnitsContainer = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    fun recalculateAll() {
        val fromU = spec.units[fromIdx]
        val toU = spec.units[toIdx]
        fromBtn.text = "מיחידה: ${fromU.name} (${fromU.symbol})"
        toBtn.text = "ליחידה: ${toU.name} (${toU.symbol})"
        inputBox.hint = "\u200Fהזן ערך להמרה (${fromU.symbol})"

        val rawVal = inputBox.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
        val baseVal = fromU.toBase(rawVal)
        val resVal = toU.fromBase(baseVal)

        heroCanvas.updateLiveData(rawVal, baseVal, resVal, fromU.symbol, toU.symbol)

        val formattedIn = ProExtendedUnitConverters.formatSmartValue(rawVal, calcPrecision)
        val formattedOut = ProExtendedUnitConverters.formatSmartValue(resVal, calcPrecision)
        val insight = spec.insightProvider(rawVal, fromU, toU, baseVal, resVal)

        mainResultTv.text = BidiFmt.rtlParagraph(
            "תוצאה: ${BidiFmt.ltr("$formattedOut ${toU.symbol}")}",
            "המרה: ${BidiFmt.ltr("$formattedIn ${fromU.symbol}  =  $formattedOut ${toU.symbol}")}",
            "",
            "💡 תובנה מקצועית:",
            insight
        )

        // טבלת השוואה חיה לכל היחידות בקטגוריה
        allUnitsContainer.removeAllViews()
        allUnitsContainer.addView(TextView(this).apply {
            text = "📊 המרה מקבילה לכל ${spec.units.size} היחידות (לחץ להעתקה):"
            setTextColor(textSecondary)
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(dp(4f), dp(4f), dp(4f), dp(8f))
        })

        for ((idx, unit) in spec.units.withIndex()) {
            val convVal = unit.fromBase(baseVal)
            val convStr = ProExtendedUnitConverters.formatSmartValue(convVal, calcPrecision.coerceAtMost(6))
            val isTarget = (idx == toIdx)
            val rowBg = when {
                isMin && isTarget -> MinimalistTheme.ACCENT_MUTED_GREEN
                isMin -> MinimalistTheme.CARD_WHITE
                isTarget -> Color.argb(65, Color.red(accentCol), Color.green(accentCol), Color.blue(accentCol))
                else -> Color.argb(30, 255, 255, 255)
            }

            val rowCard = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14f), dp(10f), dp(14f), dp(10f))
                background = GradientDrawable().apply {
                    cornerRadius = dp(14f).toFloat()
                    setColor(rowBg)
                    setStroke(dp(if (isTarget) 1.4f else 1f), if (isTarget) accentCol else Color.argb(30, 128, 128, 128))
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(6f)
                }
                setOnClickListener {
                    feedback(it)
                    copyToClipboard("$convStr ${unit.symbol}")
                }
            }

            val valTv = TextView(this).apply {
                text = "\u200E$convStr ${unit.symbol}"
                setTextColor(MinimalistTheme.ensureReadableTextOnBg(if (isTarget && !isMin) accentCol else textPrimary, rowBg, 2.2))
                textSize = 14.5f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.LEFT
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val nameTv = TextView(this).apply {
                text = "\u200F${unit.name}"
                setTextColor(MinimalistTheme.ensureReadableTextOnBg(textSecondary, rowBg, 2.0))
                textSize = 13.5f
                gravity = Gravity.RIGHT
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.1f)
            }
            rowCard.addView(valTv)
            rowCard.addView(nameTv)
            allUnitsContainer.addView(rowCard)
        }
    }

    val unitLabels = spec.units.map { "${it.name} (${it.symbol})" }

    fromBtn = createActionBtn("מיחידה: ${unitLabels[fromIdx]}", false) {
        showItemPicker("בחר יחידת מקור", unitLabels) { picked ->
            val idx = unitLabels.indexOf(picked)
            if (idx >= 0) { fromIdx = idx; recalculateAll() }
        }
    }

    val swapBtn = createActionBtn("⇅ החלף כיוון", false) {
        val tmp = fromIdx; fromIdx = toIdx; toIdx = tmp
        recalculateAll()
    }

    toBtn = createActionBtn("ליחידה: ${unitLabels[toIdx]}", false) {
        showItemPicker("בחר יחידת יעד", unitLabels) { picked ->
            val idx = unitLabels.indexOf(picked)
            if (idx >= 0) { toIdx = idx; recalculateAll() }
        }
    }

    // כפתורי Presets מהירים
    for (p in spec.presets) {
        val chipBg = if (isMin) MinimalistTheme.NEUTRAL_SECONDARY_BG else Color.argb(45, Color.red(accentCol), Color.green(accentCol), Color.blue(accentCol))
        val chip = TextView(this).apply {
            tag = "PRO_STAR_CHIP"
            text = p.label
            setTextColor(MinimalistTheme.ensureReadableTextOnBg(textPrimary, chipBg, 2.3))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(14f), dp(8f), dp(14f), dp(8f))
            background = GradientDrawable().apply {
                cornerRadius = dp(999f).toFloat()
                setColor(chipBg)
                setStroke(dp(1f), accentCol)
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(4f), 0, dp(4f), 0)
            }
            setOnClickListener {
                feedback(it)
                fromIdx = p.fromIdx.coerceIn(0, spec.units.lastIndex)
                toIdx = p.toIdx.coerceIn(0, spec.units.lastIndex)
                inputBox.setText(ProExtendedUnitConverters.formatSmartValue(p.value, 4))
                recalculateAll()
            }
        }
        presetRow.addView(chip)
    }

    root.addView(fromBtn)
    root.addView(swapBtn)
    root.addView(toBtn)
    root.addView(mainResultTv)
    root.addView(allUnitsContainer)

    inputBox.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { recalculateAll() }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    recalculateAll()
    return scroll
}

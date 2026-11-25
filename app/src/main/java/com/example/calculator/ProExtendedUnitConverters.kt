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
// לכל ממיר: תרשים הנדסי/פיזיקלי חי עם צירים, תוויות וקנה מידה ייחוס,
// תת-קטגוריות מופרדות פיזיקלית, Superscript אחיד וסמלים ל-4 ערכות הנושא
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

data class ExtUnitGroup(
    val label: String,
    val formulaHint: String,
    val units: List<ExtUnitDef>,
    val presets: List<ExtUnitPreset>,
    val defaultFrom: Int = 0,
    val defaultTo: Int = 1,
    val defaultValue: Double = 1.0,
    val insightProvider: (value: Double, from: ExtUnitDef, to: ExtUnitDef, baseVal: Double, resultVal: Double) -> String
)

data class ExtUnitToolSpec(
    val tabIndex: Int,
    val title: String,
    val subtitle: String,
    val groups: List<ExtUnitGroup>
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

private fun singleGroupSpec(
    tabIndex: Int,
    title: String,
    subtitle: String,
    formulaHint: String,
    units: List<ExtUnitDef>,
    presets: List<ExtUnitPreset>,
    defaultFrom: Int = 0,
    defaultTo: Int = 1,
    defaultValue: Double = 1.0,
    insightProvider: (Double, ExtUnitDef, ExtUnitDef, Double, Double) -> String
): ExtUnitToolSpec = ExtUnitToolSpec(
    tabIndex = tabIndex,
    title = title,
    subtitle = subtitle,
    groups = listOf(
        ExtUnitGroup(
            label = title,
            formulaHint = formulaHint,
            units = units,
            presets = presets,
            defaultFrom = defaultFrom,
            defaultTo = defaultTo,
            defaultValue = defaultValue,
            insightProvider = insightProvider
        )
    )
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

    fun toSuperscript(value: Int): String {
        val raw = value.toString()
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            sb.append(
                when (ch) {
                    '-' -> '⁻'
                    '+' -> '⁺'
                    '0' -> '⁰'
                    '1' -> '¹'
                    '2' -> '²'
                    '3' -> '³'
                    '4' -> '⁴'
                    '5' -> '⁵'
                    '6' -> '⁶'
                    '7' -> '⁷'
                    '8' -> '⁸'
                    '9' -> '⁹'
                    else -> ch
                }
            )
        }
        return sb.toString()
    }

    fun formatSmartValue(value: Double, precision: Int): String {
        if (value.isNaN() || value.isInfinite()) return "—"
        if (value == 0.0) return "0"
        val absV = abs(value)
        return if (absV >= 1e11 || absV < 1e-5) {
            val exp = floor(log10(absV)).toInt()
            val mant = value / 10.0.pow(exp)
            val mantStr = String.format(Locale.US, "%.${precision.coerceIn(2, 5)}f", mant)
                .trimEnd('0').trimEnd('.')
            "${mantStr}×10${toSuperscript(exp)}"
        } else {
            Utils.formatResult(value, precision.coerceIn(2, 8))
        }
    }

    fun formatPlainInput(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        val roundedLong = value.roundToLong()
        if (abs(value - roundedLong.toDouble()) < 1e-9 && abs(value) < 1e15) {
            return roundedLong.toString()
        }
        return String.format(Locale.US, "%.6f", value).trimEnd('0').trimEnd('.')
    }

    fun formatValueWithUnit(valStr: String, symbol: String): String {
        return if (symbol.isBlank()) valStr else "$valStr $symbol"
    }

    fun formatUnitDisplayLabel(u: ExtUnitDef): String {
        if (u.symbol.isBlank()) return u.name
        return if (u.name.contains("(${u.symbol})") || u.name.contains(" ${u.symbol})") || u.name == u.symbol) {
            u.name
        } else {
            "${u.name} (${u.symbol})"
        }
    }

    fun getSpec(tabIndex: Int): ExtUnitToolSpec = when (tabIndex) {
        // 100: קידומות SI (10s)
        100 -> singleGroupSpec(
            tabIndex = 100,
            title = "קידומות SI וחזקות של 10",
            subtitle = "המרה מדויקת בין כל קידומות השיטה המטרית (10⁻³⁰ עד 10³⁰)",
            formulaHint = "Value_to = Value_from × 10⁽ⁿ¹⁻ⁿ²⁾",
            units = listOf(
                linearUnit("יחידה בסיסית — 10⁰", "", 1.0),
                linearUnit("קילו — Kilo 10³", "k", 1e3),
                linearUnit("מגה — Mega 10⁶", "M", 1e6),
                linearUnit("גיגה — Giga 10⁹", "G", 1e9),
                linearUnit("טרה — Tera 10¹²", "T", 1e12),
                linearUnit("פטה — Peta 10¹⁵", "P", 1e15),
                linearUnit("אקסה — Exa 10¹⁸", "E", 1e18),
                linearUnit("זטה — Zetta 10²¹", "Z", 1e21),
                linearUnit("יוטה — Yotta 10²⁴", "Y", 1e24),
                linearUnit("קווטה — Quetta 10³⁰", "Q", 1e30),
                linearUnit("דצי — Deci 10⁻¹", "d", 1e-1),
                linearUnit("סנטי — Centi 10⁻²", "c", 1e-2),
                linearUnit("מילי — Milli 10⁻³", "m", 1e-3),
                linearUnit("מיקרו — Micro 10⁻⁶", "µ", 1e-6),
                linearUnit("ננו — Nano 10⁻⁹", "n", 1e-9),
                linearUnit("פיקו — Pico 10⁻¹²", "p", 1e-12),
                linearUnit("פמטו — Femto 10⁻¹⁵", "f", 1e-15),
                linearUnit("אטו — Atto 10⁻¹⁸", "a", 1e-18),
                linearUnit("קווקטו — Quecto 10⁻³⁰", "q", 1e-30)
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
                val expBase = if (baseVal != 0.0) floor(log10(abs(baseVal))).toInt() else 0
                val ratio = if (res != 0.0 && baseVal != 0.0) log10(abs(from.toBase(1.0) / to.toBase(1.0))).roundToInt() else 0
                val fromLbl = if (from.symbol.isBlank()) "יחידה בסיסית" else BidiFmt.ltr(from.symbol)
                val toLbl = if (to.symbol.isBlank()) "יחידה בסיסית" else BidiFmt.ltr(to.symbol)
                "• סדר גודל בסיסי: ${BidiFmt.ltr("10${toSuperscript(expBase)}")}\n• הפרש חזקות בין $fromLbl ל-$toLbl: ${BidiFmt.ltr("10${toSuperscript(ratio)}")} (כפל פי ${BidiFmt.ltr(formatSmartValue(10.0.pow(ratio), 4))})"
            }
        )

        // 101: שטח קרקע
        101 -> singleGroupSpec(
            tabIndex = 101,
            title = "ממיר שטח קרקע ומדידות נדל״ן",
            subtitle = "דונם ישראלי/עות'מאני, הקטר, אקר, מ״ר ויחידות עולמיות",
            formulaHint = "1 דונם ישראלי = 1,000 m²  |  1 אקר = 4,046.86 m²",
            units = listOf(
                linearUnit("דונם ישראלי / מטרי", "דונם", 1000.0),
                linearUnit("מטר רבוע", "m²", 1.0),
                linearUnit("דונם עות׳מאני (טורקי)", "דונם עות׳", 919.3),
                linearUnit("הקטר — Hectare", "ha", 10_000.0),
                linearUnit("אקר — Acre", "ac", 4046.8564224),
                linearUnit("קילומטר רבוע", "km²", 1_000_000.0),
                linearUnit("רגל רבועה", "ft²", 0.09290304),
                linearUnit("יארד רבוע", "yd²", 0.83612736),
                linearUnit("פדאן — Feddan", "feddan", 4200.833),
                linearUnit("צובו יפני — Tsubo", "tsubo", 3.305785)
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
                "• ריבוע שווה-שטח: צלע של ${BidiFmt.ltr("${formatSmartValue(sideM, 2)} m")} (היקף ${BidiFmt.ltr("${formatSmartValue(sideM * 4, 1)} m")})\n• שווה ערך ל-${BidiFmt.ltr(formatSmartValue(footballFields, 3))} מגרשי כדורגל או ${BidiFmt.ltr(formatSmartValue(apt100, 1))} דירות 100 מ״ר"
            }
        )

        // 102: מספרים גדולים
        102 -> singleGroupSpec(
            tabIndex = 102,
            title = "ממיר מספרים גדולים ושיטות ספירה",
            subtitle = "סולם מערבי (מיליון/מיליארד/טריליון), סולם הודי (Lakh/Crore) וגוגול",
            formulaHint = "1 Crore = 10⁷  |  1 Billion = 10⁹  |  1 Googol = 10¹⁰⁰",
            units = listOf(
                linearUnit("יחידות בודדות — 10⁰", "", 1.0),
                linearUnit("אלף — Thousand 10³", "K", 1e3),
                linearUnit("לאק הודי — Lakh 10⁵", "Lakh", 1e5),
                linearUnit("מיליון — Million 10⁶", "M", 1e6),
                linearUnit("קרור הודי — Crore 10⁷", "Cr", 1e7),
                linearUnit("מיליארד — Billion 10⁹", "B", 1e9),
                linearUnit("טריליון — Trillion 10¹²", "T", 1e12),
                linearUnit("קוודריליון — Quadrillion 10¹⁵", "Qa", 1e15),
                linearUnit("קווינטיליון — Quintillion 10¹⁸", "Qi", 1e18),
                linearUnit("סקסטיליון — Sextillion 10²¹", "Sx", 1e21),
                linearUnit("גוגול — Googol 10¹⁰⁰", "Googol", 1e100)
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
                val zeros = if (abs(baseVal) >= 1.0) floor(log10(abs(baseVal))).toInt() else 0
                val digits = zeros + 1
                "• כתיב מדעי: ${BidiFmt.ltr(formatSmartValue(baseVal, 4))}\n• מספר ספרות שלמות: ${BidiFmt.ltr(digits.toString())} (כ-${BidiFmt.ltr(zeros.toString())} אפסים אחרי הספרה הראשונה)"
            }
        )

        // 103: זמן תרבותי ומסורתי
        103 -> singleGroupSpec(
            tabIndex = 103,
            title = "ממיר זמן תרבותי, עברי ומסורתי",
            subtitle = "חלקים, רגעים, שעה, יום, שבוע, שמיטה, יובל ומחזורי זמן היסטוריים",
            formulaHint = "1 שעה = 1,080 חלקים  |  1 חלק = 3⅓ שניות = 76 רגעים",
            units = listOf(
                linearUnit("חלק עברי — 3⅓ שניות", "חלק", 10.0 / 3.0),
                linearUnit("רגע עברי — 1/76 חלק", "רגע", (10.0 / 3.0) / 76.0),
                linearUnit("שנייה תקנית", "s", 1.0),
                linearUnit("דקה — 18 חלקים", "min", 60.0),
                linearUnit("שעה — 1,080 חלקים", "hr", 3600.0),
                linearUnit("עונה תלמודית — 1/24 שעה", "עונה", 150.0),
                linearUnit("יממה — 24 שעות", "יום", 86_400.0),
                linearUnit("שבוע — 7 ימים", "שבוע", 604_800.0),
                linearUnit("חודש עברי מולד — 29.53 יום", "חודש עברי", 2_551_443.333),
                linearUnit("שנה סולארית — 365.24 יום", "שנה", 31_556_926.0),
                linearUnit("מחזור שמיטה — 7 שנים", "שמיטה", 31_556_926.0 * 7.0),
                linearUnit("יובל — 50 שנים", "יובל", 31_556_926.0 * 50.0),
                linearUnit("מאה — Century", "מאה", 31_556_926.0 * 100.0)
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
                "• בספירה עברית מסורתית: ${BidiFmt.ltr(formatSmartValue(halakim, 2))} חלקים\n• שווה ערך ל-${BidiFmt.ltr(formatSmartValue(days, 4))} יממות (${BidiFmt.ltr(formatSmartValue(sec / 3600.0, 3))} שעות)"
            }
        )

        // 104: צפיפות חוט וטקסטיל
        104 -> singleGroupSpec(
            tabIndex = 104,
            title = "ממיר צפיפות חוט וטקסטיל",
            subtitle = "שיטות ישירות (Tex, Denier, dtex) ועקיפות (Nm, Ne כותנה)",
            formulaHint = "Denier = 9 × Tex  |  Nm = 1000 / Tex  |  Ne = 590.54 / Tex",
            units = listOf(
                linearUnit("טקס — גרם ל-1,000 מ׳", "tex", 1.0),
                linearUnit("דצי-טקס — Decitex", "dtex", 0.1),
                linearUnit("דנייר — גרם ל-9,000 מ׳", "den", 1.0 / 9.0),
                linearUnit("מילי-טקס — Millitex", "mtex", 0.001),
                linearUnit("קילו-טקס — Kilotex", "ktex", 1000.0),
                inverseUnit("ספירה מטרית — Metric Count", "Nm", 1000.0),
                inverseUnit("ספירת כותנה אנגלית — Cotton", "Ne", 590.541),
                inverseUnit("ספירת פשתן — Linen", "Lea", 1653.52)
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
                "• סיווג עובי סיב: $grade\n• אורך בגרם חוט (${BidiFmt.ltr("Nm")}): ${BidiFmt.ltr("${formatSmartValue(nm, 2)} m/g")}"
            }
        )

        // 105: קלוריות מזון
        105 -> singleGroupSpec(
            tabIndex = 105,
            title = "ממיר קלוריות מזון ואנרגיה תזונתית",
            subtitle = "קק״ל (kcal), קילו-ג׳אול (kJ), קלוריות קטנות ואנרגיית שריפה",
            formulaHint = "1 kcal (קלוריית מזון) = 4.184 kJ = 1,000 cal",
            units = listOf(
                linearUnit("קילו-קלוריה — קלוריית מזון", "kcal", 4184.0),
                linearUnit("קילו-ג׳אול — תקן תוויות מזון", "kJ", 1000.0),
                linearUnit("קלוריה קטנה — תרמוכימית", "cal", 4.184),
                linearUnit("ג׳אול — Joule", "J", 1.0),
                linearUnit("מגה-ג׳אול", "MJ", 1_000_000.0),
                linearUnit("ואט-שעה", "Wh", 3600.0),
                linearUnit("יחידת חום בריטית", "BTU", 1055.05585)
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
                "• אחוז מתפריט יומי (2,000 קק״ל): ${BidiFmt.ltr("${formatSmartValue(dailyPct, 1)}%")}\n• שריפה משוערת: ${BidiFmt.ltr(formatSmartValue(walkMin, 0))} דק׳ הליכה מהירה או ${BidiFmt.ltr(formatSmartValue(runMin, 0))} דק׳ ריצה"
            }
        )

        // 106: משקל זהב
        106 -> singleGroupSpec(
            tabIndex = 106,
            title = "ממיר משקל זהב ומתכות יקרות",
            subtitle = "גרם, אונקיית טרוי (oz t), קראט, טולה הודית, פניוויט וגריין",
            formulaHint = "1 אונקיית טרוי = 31.1034768 g  |  1 טולה = 11.6638 g",
            units = listOf(
                linearUnit("גרם — Gram", "g", 1.0),
                linearUnit("אונקיית טרוי — Troy Ounce", "oz t", 31.1034768),
                linearUnit("קראט — Carat 0.2g", "ct", 0.2),
                linearUnit("טולה — Tola", "tola", 11.6638038),
                linearUnit("פניוויט — Pennyweight", "dwt", 1.55517384),
                linearUnit("גריין — Grain", "gr", 0.06479891),
                linearUnit("טאל הונג-קונג — Tael", "tael", 37.429),
                linearUnit("בהט זהב תאילנדי — Baht", "baht", 15.244),
                linearUnit("קילוגרם — מטיל זהב", "kg", 1000.0)
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
                "• תכולת זהב טהור (${BidiFmt.ltr("24K")}): ${BidiFmt.ltr("${formatSmartValue(grams, 3)} g")}\n• אם התכשיט ב-${BidiFmt.ltr("18K")}: ${BidiFmt.ltr("${formatSmartValue(g18, 3)} g")} זהב נקי | ב-${BidiFmt.ltr("14K")}: ${BidiFmt.ltr("${formatSmartValue(g14, 3)} g")}"
            }
        )

        // 107: נפח עץ
        107 -> singleGroupSpec(
            tabIndex = 107,
            title = "ממיר נפח עץ, לוחות ונגרות",
            subtitle = "קוב עץ (m³), רגל לוח (Board Foot), Cord, Stere ויחידות יערנות",
            formulaHint = "1 Board Foot (FBM) = 144 in³ = 0.0023597 m³",
            units = listOf(
                linearUnit("מטר מעוקב — קוב עץ", "m³", 1.0),
                linearUnit("רגל לוח — Board Foot", "FBM", 0.0023597372),
                linearUnit("אלף רגלי לוח", "MBF", 2.3597372),
                linearUnit("רגל מעוקבת — Cubic Foot", "ft³", 0.0283168466),
                linearUnit("קורד עץ הסקה — 128 ft³", "cord", 3.62455636),
                linearUnit("סטר — Stere", "st", 1.0),
                linearUnit("הופוס רגל — Hoppus Foot", "h ft", 0.036054),
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
                "• משקל משוער לעץ אורן יבש (~520 kg/m³): ${BidiFmt.ltr("${formatSmartValue(pineKg, 1)} kg")}\n• משקל משוער לעץ אלון (~740 kg/m³): ${BidiFmt.ltr("${formatSmartValue(oakKg, 1)} kg")}"
            }
        )

        // 108: כושר קירור
        108 -> singleGroupSpec(
            tabIndex = 108,
            title = "ממיר כושר קירור ומיזוג אוויר",
            subtitle = "טון קירור (TR), BTU לשעה, קילוואט תרמי וכוח סוס מיזוג",
            formulaHint = "1 טון קירור (TR) = 12,000 BTU/h = 3.51685 kW",
            units = listOf(
                linearUnit("BTU לשעה", "BTU/h", 0.29307107),
                linearUnit("טון קירור אמריקאי", "TR", 3516.85284),
                linearUnit("קילוואט קירור", "kW", 1000.0),
                linearUnit("ואט תרמי", "W", 1.0),
                linearUnit("כוח סוס מיזוג — תקן ישראלי", "כ״ס", 2637.6396),
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
                "• מתאים לקירור חדר בשטח משוער של ${BidiFmt.ltr("${formatSmartValue(roomSqmMin, 1)}–${formatSmartValue(roomSqmMax, 1)} m²")}\n• שקול ל-${BidiFmt.ltr(formatSmartValue(btuh / 9000.0, 2))} כ״ס מזגן בתקינה ביתית"
            }
        )

        // 109: זרימת אוויר
        109 -> singleGroupSpec(
            tabIndex = 109,
            title = "ממיר זרימת אוויר (HVAC ואוורור)",
            subtitle = "CFM, מק״ש (m³/h), ליטר לשנייה (L/s) ורגל מעוקבת לשנייה",
            formulaHint = "1 CFM = 1.69901 m³/h = 0.47195 L/s",
            units = listOf(
                linearUnit("רגל מעוקבת לדקה", "CFM", 0.00047194745),
                linearUnit("מטר מעוקב לשעה — מק״ש", "m³/h", 1.0 / 3600.0),
                linearUnit("ליטר לשנייה", "L/s", 0.001),
                linearUnit("ליטר לדקה", "L/min", 1.0 / 60_000.0),
                linearUnit("מטר מעוקב לדקה", "m³/min", 1.0 / 60.0),
                linearUnit("מטר מעוקב לשנייה", "m³/s", 1.0),
                linearUnit("רגל מעוקבת לשנייה", "CFS", 0.0283168466)
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
                "• ספיקה שעתית: ${BidiFmt.ltr("${formatSmartValue(m3h, 1)} m³/h")}\n• בחדר ממוצע (50 מ״ק): ${BidiFmt.ltr("${formatSmartValue(achRoom50, 1)} ACH")} החלפות אוויר בשעה"
            }
        )

        // 110: קשיות מים
        110 -> singleGroupSpec(
            tabIndex = 110,
            title = "ממיר קשיות מים וריכוז אבנית",
            subtitle = "ppm CaCO₃, מעלות גרמניות (°dH), צרפתיות (°fH), אנגליות ו-mmol/L",
            formulaHint = "1 °dH = 17.848 ppm CaCO₃  |  1 mmol/L = 100.09 ppm",
            units = listOf(
                linearUnit("חלקים למיליון CaCO₃", "ppm", 1.0),
                linearUnit("מעלות גרמניות", "°dH", 17.848),
                linearUnit("מעלות צרפתיות", "°fH", 10.0),
                linearUnit("מעלות אנגליות / קלארק", "°e", 14.2857),
                linearUnit("מילימול לליטר CaCO₃", "mmol/L", 100.0869),
                linearUnit("גריינים לגלון אמריקאי", "gpg", 17.118),
                linearUnit("מיליאקוויוולנט לליטר", "meq/L", 50.04345)
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
                "• סיווג איכות קשיות (USGS): $level\n• שווה ערך ל-${BidiFmt.ltr("${formatSmartValue(ppm / 17.848, 2)} °dH")}"
            }
        )

        // 111: מתח פנים
        111 -> singleGroupSpec(
            tabIndex = 111,
            title = "ממיר מתח פנים של נוזלים",
            subtitle = "מילי-ניוטון למטר (mN/m), דין לס״מ (dyn/cm) ואנרגיית שטח",
            formulaHint = "1 mN/m = 1 dyn/cm = 0.001 N/m (מים ב-20°C ≈ 72.8 mN/m)",
            units = listOf(
                linearUnit("מילי-ניוטון למטר", "mN/m", 0.001),
                linearUnit("דין לסנטימטר — CGS", "dyn/cm", 0.001),
                linearUnit("ניוטון למטר — SI", "N/m", 1.0),
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
                "• השוואה מעבדתית: $ref\n• ביחידות CGS: ${BidiFmt.ltr("${formatSmartValue(mnm, 3)} dyn/cm")}"
            }
        )

        // 112: פרמיטיביות ופרמאביליות
        112 -> ExtUnitToolSpec(
            tabIndex = 112,
            title = "פרמיטיביות (ε) ופרמאביליות (µ)",
            subtitle = "בחר בין פרמיטיביות דיאלקטרית (ε) לבין פרמאביליות מגנטית (µ)",
            groups = listOf(
                ExtUnitGroup(
                    label = "פרמיטיביות (ε)",
                    formulaHint = "ε₀ = 8.8541878×10⁻¹² F/m  |  ε = εᵣ · ε₀",
                    units = listOf(
                        linearUnit("פרמיטיביות יחסית לריק — εᵣ", "ε₀", 8.854187817e-12),
                        linearUnit("פיקו-פאראד למטר", "pF/m", 1e-12),
                        linearUnit("ננו-פאראד למטר", "nF/m", 1e-9),
                        linearUnit("מיקרו-פאראד למטר", "µF/m", 1e-6),
                        linearUnit("פאראד למטר — SI", "F/m", 1.0)
                    ),
                    presets = listOf(
                        ExtUnitPreset("ריק 1 ε₀ → pF/m", 1.0, 0, 1),
                        ExtUnitPreset("מים εᵣ=80", 80.0, 0, 1),
                        ExtUnitPreset("זכוכית εᵣ=4.7", 4.7, 0, 1),
                        ExtUnitPreset("100 pF/m → ε₀", 100.0, 1, 0)
                    ),
                    defaultFrom = 0,
                    defaultTo = 1,
                    defaultValue = 1.0,
                    insightProvider = { _, _, _, baseVal, _ ->
                        val epsR = baseVal / 8.854187817e-12
                        val vLightPct = if (epsR > 0) (1.0 / sqrt(epsR)) * 100.0 else 0.0
                        "• מקדם דיאלקטרי יחסי (${BidiFmt.ltr("εᵣ")}): ${BidiFmt.ltr(formatSmartValue(epsR, 4))}\n• מהירות גל אלקטרומגנטי בתווך: ${BidiFmt.ltr("${formatSmartValue(vLightPct, 2)}%")} ממהירות האור"
                    }
                ),
                ExtUnitGroup(
                    label = "פרמאביליות (µ)",
                    formulaHint = "µ₀ = 4π×10⁻⁷ H/m ≈ 1.2566 µH/m  |  µ = µᵣ · µ₀",
                    units = listOf(
                        linearUnit("פרמאביליות יחסית לריק — µᵣ", "µ₀", 1.2566370614e-6),
                        linearUnit("מיקרו-הנרי למטר", "µH/m", 1e-6),
                        linearUnit("ננו-הנרי למטר", "nH/m", 1e-9),
                        linearUnit("מילי-הנרי למטר", "mH/m", 1e-3),
                        linearUnit("הנרי למטר — SI", "H/m", 1.0)
                    ),
                    presets = listOf(
                        ExtUnitPreset("ריק 1 µ₀ → µH/m", 1.0, 0, 1),
                        ExtUnitPreset("ברזל µᵣ=5000", 5000.0, 0, 3),
                        ExtUnitPreset("ניקל µᵣ=600", 600.0, 0, 1),
                        ExtUnitPreset("10 µH/m → µ₀", 10.0, 1, 0)
                    ),
                    defaultFrom = 0,
                    defaultTo = 1,
                    defaultValue = 1.0,
                    insightProvider = { _, _, _, baseVal, _ ->
                        val muR = baseVal / 1.2566370614e-6
                        "• פרמאביליות מגנטית יחסית (${BidiFmt.ltr("µᵣ")}): ${BidiFmt.ltr(formatSmartValue(muR, 4))}\n• הגברת שטף מגנטי לעומת ריק: פי ${BidiFmt.ltr(formatSmartValue(muR, 2))}"
                    }
                )
            )
        )

        // 113: מומנט אינרציה
        113 -> singleGroupSpec(
            tabIndex = 113,
            title = "ממיר מומנט אינרציה (Rotational Inertia)",
            subtitle = "התנגדות גוף לשינוי תאוצה זוויתית במכניקה, מנועים וגלגלי תנופה",
            formulaHint = "I = ∑ m·r²  |  E_rot = ½·I·ω²",
            units = listOf(
                linearUnit("קילוגרם·מטר רבוע — SI", "kg·m²", 1.0),
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
                "• אנרגיית סיבוב ב-${BidiFmt.ltr("1,000 RPM")}: ${BidiFmt.ltr("${formatSmartValue(eRot, 2)} J")} (${BidiFmt.ltr("${formatSmartValue(eRot / 1000.0, 3)} kJ")})\n• מומנט נדרש להאצה ב-${BidiFmt.ltr("10 rad/s²")}: ${BidiFmt.ltr("${formatSmartValue(kgm2 * 10.0, 3)} N·m")}"
            }
        )

        // 114: צפיפות חום וקיבול חום
        114 -> ExtUnitToolSpec(
            tabIndex = 114,
            title = "צפיפות חום וקיבול חום (תרמודינמיקה)",
            subtitle = "בחר בין צפיפות אנרגיית חום לשטח (J/m²) לבין קיבול חום (J/K)",
            groups = listOf(
                ExtUnitGroup(
                    label = "צפיפות חום (J/m²)",
                    formulaHint = "1 cal/cm² (Langley) = 41,840 J/m²  |  1 Wh/m² = 3,600 J/m²",
                    units = listOf(
                        linearUnit("קלוריה לסמ״ר — Langley", "cal/cm²", 41_840.0),
                        linearUnit("קילו-ג׳אול למ״ר", "kJ/m²", 1000.0),
                        linearUnit("ג׳אול למ״ר — SI", "J/m²", 1.0),
                        linearUnit("ואט-שעה למ״ר", "Wh/m²", 3600.0),
                        linearUnit("BTU לרגל רבועה", "BTU/ft²", 11_356.5267),
                        linearUnit("מגה-ג׳אול למ״ר", "MJ/m²", 1_000_000.0)
                    ),
                    presets = listOf(
                        ExtUnitPreset("1 cal/cm² → kJ/m²", 1.0, 0, 1),
                        ExtUnitPreset("100 Wh/m² → kJ/m²", 100.0, 3, 1),
                        ExtUnitPreset("10 BTU/ft² → kJ/m²", 10.0, 4, 1),
                        ExtUnitPreset("1 MJ/m² → Wh/m²", 1.0, 5, 3)
                    ),
                    defaultFrom = 0,
                    defaultTo = 1,
                    defaultValue = 1.0,
                    insightProvider = { _, _, _, jm2, _ ->
                        val langley = jm2 / 41_840.0
                        "• שווה ערך ל-${BidiFmt.ltr("${formatSmartValue(langley, 4)} Langley")} (יחידת קרינת שמש מטאורולוגית)\n• ביחידות פאנל סולארי: ${BidiFmt.ltr("${formatSmartValue(jm2 / 3600.0, 3)} Wh/m²")}"
                    }
                ),
                ExtUnitGroup(
                    label = "קיבול חום (J/K)",
                    formulaHint = "1 kJ/K = 1,000 J/K  |  ליטר מים ≈ 4.184 kJ/K",
                    units = listOf(
                        linearUnit("קילו-ג׳אול לקלווין", "kJ/K", 1000.0),
                        linearUnit("ג׳אול לקלווין — SI", "J/K", 1.0),
                        linearUnit("קלוריה למעלת צלזיוס", "cal/°C", 4.184),
                        linearUnit("קילו-קלוריה למעלת צלזיוס", "kcal/°C", 4184.0),
                        linearUnit("BTU למעלת פרנהייט", "BTU/°F", 1899.1005),
                        linearUnit("ואט-שעה לקלווין", "Wh/K", 3600.0)
                    ),
                    presets = listOf(
                        ExtUnitPreset("ליטר מים 4.184 kJ/K", 4.184, 0, 3),
                        ExtUnitPreset("1000 J/K → cal/°C", 1000.0, 1, 2),
                        ExtUnitPreset("1 BTU/°F → kJ/K", 1.0, 4, 0),
                        ExtUnitPreset("10 kcal/°C → Wh/K", 10.0, 3, 5)
                    ),
                    defaultFrom = 0,
                    defaultTo = 3,
                    defaultValue = 4.184,
                    insightProvider = { _, _, _, jk, _ ->
                        val waterLiters = jk / 4184.0
                        "• קיבול חום זה שקול לחימום ${BidiFmt.ltr("${formatSmartValue(waterLiters, 3)} L")} מים במעלה אחת (${BidiFmt.ltr("1°C")})"
                    }
                )
            )
        )

        // 115: נפח סגולי וצפיפות
        115 -> singleGroupSpec(
            tabIndex = 115,
            title = "נפח סגולי (v) וצפיפות חומר (ρ)",
            subtitle = "המרה דו-כיוונית אמיתית בין נפח סגולי (m³/kg) לבין צפיפות (kg/m³)",
            formulaHint = "v = 1 / ρ  |  מים ב-4°C: ρ = 1,000 kg/m³, v = 0.001 m³/kg",
            units = listOf(
                linearUnit("מ״ק לקילוגרם — נפח סגולי", "m³/kg", 1.0),
                linearUnit("ליטר לקילוגרם — נפח סגולי", "L/kg", 0.001),
                linearUnit("סמ״ק לגרם — נפח סגולי", "cm³/g", 0.001),
                linearUnit("רגל מעוקבת לפאונד", "ft³/lb", 0.06242796),
                inverseUnit("קילוגרם למ״ק — צפיפות ρ", "kg/m³", 1.0),
                inverseUnit("גרם לסמ״ק — צפיפות סגולית", "g/cm³", 0.001),
                inverseUnit("פאונד לרגל מעוקבת", "lb/ft³", 0.06242796)
            ),
            presets = listOf(
                ExtUnitPreset("מים 1 L/kg → kg/m³", 1.0, 1, 4),
                ExtUnitPreset("אוויר 0.816 m³/kg", 0.816, 0, 4),
                ExtUnitPreset("ברזל 7.87 g/cm³", 7.87, 5, 2),
                ExtUnitPreset("1 m³/kg → ft³/lb", 1.0, 0, 3)
            ),
            defaultFrom = 1,
            defaultTo = 4,
            defaultValue = 1.0,
            insightProvider = { _, _, _, m3kg, _ ->
                val rho = if (m3kg > 0) 1.0 / m3kg else 0.0
                val floats = if (rho > 0 && rho < 1000.0) "צף על פני מים (קל ממים)" else if (rho > 1000.0) "שוקע במים (כבד ממים)" else "זהה לצפיפות המים"
                "• צפיפות מקבילה (${BidiFmt.ltr("ρ")}): ${BidiFmt.ltr("${formatSmartValue(rho, 3)} kg/m³")} (${BidiFmt.ltr("${formatSmartValue(rho / 1000.0, 4)} g/cm³")})\n• ציפה במים: $floats"
            }
        )

        // 116: מהירות ותאוצה זוויתית
        116 -> ExtUnitToolSpec(
            tabIndex = 116,
            title = "מהירות זוויתית (ω) ותאוצה זוויתית (α)",
            subtitle = "בחר בין מהירות סיבוב (RPM / rad/s) לבין תאוצה סיבובית (rad/s²)",
            groups = listOf(
                ExtUnitGroup(
                    label = "מהירות זוויתית (ω)",
                    formulaHint = "ω (rad/s) = RPM × 2π / 60  |  1 Hz = 60 RPM",
                    units = listOf(
                        linearUnit("סיבובים לדקה", "RPM", 2.0 * PI / 60.0),
                        linearUnit("רדיאן לשנייה — SI", "rad/s", 1.0),
                        linearUnit("מעלות לשנייה", "deg/s", PI / 180.0),
                        linearUnit("סיבובים לשנייה / הרץ", "Hz", 2.0 * PI),
                        linearUnit("רדיאן לדקה", "rad/min", 1.0 / 60.0),
                        linearUnit("מעלות לדקה", "deg/min", PI / 10800.0)
                    ),
                    presets = listOf(
                        ExtUnitPreset("1000 RPM → rad/s", 1000.0, 0, 1),
                        ExtUnitPreset("60 RPM → Hz", 60.0, 0, 3),
                        ExtUnitPreset("360 deg/s → RPM", 360.0, 2, 0),
                        ExtUnitPreset("50 Hz → RPM", 50.0, 3, 0)
                    ),
                    defaultFrom = 0,
                    defaultTo = 1,
                    defaultValue = 1200.0,
                    insightProvider = { _, _, _, rads, _ ->
                        val rpm = rads * 60.0 / (2.0 * PI)
                        val periodMs = if (rads != 0.0) (2.0 * PI / abs(rads)) * 1000.0 else 0.0
                        "• זמן מחזור לסיבוב מלא: ${BidiFmt.ltr("${formatSmartValue(periodMs, 2)} ms")}\n• מהירות קווית בקצה גלגל ברדיוס 1 מ׳: ${BidiFmt.ltr("${formatSmartValue(abs(rads), 2)} m/s")} (${BidiFmt.ltr("${formatSmartValue(rpm, 1)} RPM")})"
                    }
                ),
                ExtUnitGroup(
                    label = "תאוצה זוויתית (α)",
                    formulaHint = "α = dω / dt  |  1 rev/s² = 2π rad/s²",
                    units = listOf(
                        linearUnit("רדיאן לשנייה בריבוע — SI", "rad/s²", 1.0),
                        linearUnit("מעלות לשנייה בריבוע", "deg/s²", PI / 180.0),
                        linearUnit("סיבובים לשנייה בריבוע", "rev/s²", 2.0 * PI),
                        linearUnit("סל״ד לשנייה", "RPM/s", 2.0 * PI / 60.0),
                        linearUnit("סיבובים לדקה בריבוע", "rev/min²", 2.0 * PI / 3600.0)
                    ),
                    presets = listOf(
                        ExtUnitPreset("10 rad/s² → deg/s²", 10.0, 0, 1),
                        ExtUnitPreset("100 RPM/s → rad/s²", 100.0, 3, 0),
                        ExtUnitPreset("1 rev/s² → rad/s²", 1.0, 2, 0),
                        ExtUnitPreset("360 deg/s² → rev/s²", 360.0, 1, 2)
                    ),
                    defaultFrom = 0,
                    defaultTo = 1,
                    defaultValue = 10.0,
                    insightProvider = { _, _, _, rads2, _ ->
                        val rpmAfter5s = abs(rads2) * 5.0 * 60.0 / (2.0 * PI)
                        "• בתאוצה קבועה זו, לאחר 5 שניות ממנוחה תגיע ל-${BidiFmt.ltr("${formatSmartValue(rpmAfter5s, 1)} RPM")}"
                    }
                )
            )
        )

        // 117: צפיפות זרם, מוליכות ומטען
        else -> ExtUnitToolSpec(
            tabIndex = 117,
            title = "צפיפות זרם, מוליכות ומטען (J / G / Q)",
            subtitle = "בחר את הגודל החשמלי להמרה נקייה בין יחידות תואמות בלבד",
            groups = listOf(
                ExtUnitGroup(
                    label = "מטען (Q)",
                    formulaHint = "1 Ah = 1,000 mAh = 3,600 C  |  1 Faraday = 96,485 C",
                    units = listOf(
                        linearUnit("מילי-אמפר שעה — סוללות", "mAh", 3.6),
                        linearUnit("קולון — SI", "C", 1.0),
                        linearUnit("אמפר-שעה", "Ah", 3600.0),
                        linearUnit("מילי-קולון", "mC", 1e-3),
                        linearUnit("מיקרו-קולון", "µC", 1e-6),
                        linearUnit("קילו-קולון", "kC", 1000.0),
                        linearUnit("פאראדיי — כימיה חשמלית", "F", 96_485.33212)
                    ),
                    presets = listOf(
                        ExtUnitPreset("5000 mAh → קולון", 5000.0, 0, 1),
                        ExtUnitPreset("1 Ah → mAh", 1.0, 2, 0),
                        ExtUnitPreset("1 Faraday → C", 1.0, 6, 1),
                        ExtUnitPreset("3600 C → Ah", 3600.0, 1, 2)
                    ),
                    defaultFrom = 0,
                    defaultTo = 1,
                    defaultValue = 5000.0,
                    insightProvider = { _, _, _, coulombs, _ ->
                        val electrons = abs(coulombs) / 1.602176634e-19
                        val whAt37V = (abs(coulombs) / 3600.0) * 3.7
                        "• מספר אלקטרונים במטען זה: ${BidiFmt.ltr(formatSmartValue(electrons, 3))} אלקטרונים\n• אנרגיית סוללת ליתיום (${BidiFmt.ltr("3.7V")}): ${BidiFmt.ltr("${formatSmartValue(whAt37V, 2)} Wh")}"
                    }
                ),
                ExtUnitGroup(
                    label = "צפיפות זרם (J)",
                    formulaHint = "J = I / A  |  1 A/mm² = 100 A/cm² = 10⁶ A/m²",
                    units = listOf(
                        linearUnit("אמפר לממ״ר — חיווט חשמלי", "A/mm²", 1e6),
                        linearUnit("אמפר למ״ר — SI", "A/m²", 1.0),
                        linearUnit("אמפר לסמ״ר", "A/cm²", 1e4),
                        linearUnit("מילי-אמפר לממ״ר", "mA/mm²", 1e3),
                        linearUnit("קילו-אמפר למ״ר", "kA/m²", 1e3),
                        linearUnit("אמפר לאינץ׳ רבוע", "A/in²", 1550.0031)
                    ),
                    presets = listOf(
                        ExtUnitPreset("4 A/mm² → A/m²", 4.0, 0, 1),
                        ExtUnitPreset("6 A/mm² → A/cm²", 6.0, 0, 2),
                        ExtUnitPreset("100 A/cm² → A/mm²", 100.0, 2, 0),
                        ExtUnitPreset("1000 A/in² → A/mm²", 1000.0, 5, 0)
                    ),
                    defaultFrom = 0,
                    defaultTo = 1,
                    defaultValue = 4.0,
                    insightProvider = { _, _, _, am2, _ ->
                        val amm2 = abs(am2) / 1e6
                        val safety = when {
                            amm2 <= 4.0 -> "תקין ובטוח לחיווט נחושת ביתי"
                            amm2 <= 7.0 -> "עומס גבוה — מומלץ כבל עבה יותר או אוורור"
                            else -> "סכנת התחממות מוליך נחושת!"
                        }
                        "• צפיפות זרם במוליך: ${BidiFmt.ltr("${formatSmartValue(amm2, 2)} A/mm²")}\n• הערכת בטיחות: $safety"
                    }
                ),
                ExtUnitGroup(
                    label = "מוליכות (G)",
                    formulaHint = "G = 1 / R  |  1 S (סימנס) = 1,000 mS = 1 ℧ (mho)",
                    units = listOf(
                        linearUnit("סימנס — SI", "S", 1.0),
                        linearUnit("מילי-סימנס", "mS", 1e-3),
                        linearUnit("מיקרו-סימנס", "µS", 1e-6),
                        linearUnit("ננו-סימנס", "nS", 1e-9),
                        linearUnit("קילו-סימנס", "kS", 1e3),
                        linearUnit("מהו — mho", "℧", 1.0)
                    ),
                    presets = listOf(
                        ExtUnitPreset("100 mS → S", 100.0, 1, 0),
                        ExtUnitPreset("500 µS → mS", 500.0, 2, 1),
                        ExtUnitPreset("1 S → mS", 1.0, 0, 1),
                        ExtUnitPreset("2.5 kS → S", 2.5, 4, 0)
                    ),
                    defaultFrom = 1,
                    defaultTo = 0,
                    defaultValue = 100.0,
                    insightProvider = { _, _, _, siemens, _ ->
                        val ohm = if (siemens != 0.0) 1.0 / abs(siemens) else 0.0
                        "• התנגדות שקולה (${BidiFmt.ltr("R = 1/G")}): ${BidiFmt.ltr("${formatSmartValue(ohm, 4)} Ω")}"
                    }
                )
            )
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
                    100 -> {
                        fillP.textSize = r * 0.96f
                        canvas.drawText("10ˢ", cx, cy + r * 0.34f, fillP)
                    }
                    101 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.78f, cy - r * 0.62f, cx + r * 0.78f, cy + r * 0.62f), r * 0.16f, r * 0.16f, strokeP)
                        canvas.drawCircle(cx + r * 0.18f, cy - r * 0.08f, r * 0.20f, strokeP)
                        canvas.drawLine(cx - r * 0.50f, cy + r * 0.25f, cx - r * 0.05f, cy - r * 0.20f, strokeP)
                    }
                    102 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.76f, cy - r * 0.68f, cx + r * 0.76f, cy + r * 0.68f), r * 0.14f, r * 0.14f, strokeP)
                        canvas.drawLine(cx - r * 0.76f, cy - r * 0.22f, cx + r * 0.76f, cy - r * 0.22f, strokeP)
                        canvas.drawLine(cx - r * 0.76f, cy + r * 0.24f, cx + r * 0.76f, cy + r * 0.24f, strokeP)
                        canvas.drawCircle(cx - r * 0.32f, cy - r * 0.22f, r * 0.12f, fillP)
                        canvas.drawCircle(cx + r * 0.28f, cy + r * 0.24f, r * 0.12f, fillP)
                    }
                    103 -> {
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
                    104 -> {
                        val yarn = Path().apply {
                            moveTo(cx - r * 0.78f, cy + r * 0.22f)
                            cubicTo(cx - r * 0.32f, cy - r * 0.65f, cx + r * 0.32f, cy + r * 0.65f, cx + r * 0.78f, cy - r * 0.22f)
                        }
                        canvas.drawPath(yarn, strokeP)
                        canvas.drawCircle(cx - r * 0.78f, cy + r * 0.22f, r * 0.11f, fillP)
                        canvas.drawCircle(cx + r * 0.78f, cy - r * 0.22f, r * 0.11f, fillP)
                    }
                    105 -> {
                        canvas.drawArc(RectF(cx - r * 0.74f, cy - r * 0.66f, cx + r * 0.74f, cy + r * 0.24f), 180f, 180f, true, strokeP)
                        canvas.drawLine(cx - r * 0.80f, cy + r * 0.10f, cx + r * 0.80f, cy + r * 0.10f, strokeP)
                        canvas.drawRoundRect(RectF(cx - r * 0.72f, cy + r * 0.32f, cx + r * 0.72f, cy + r * 0.68f), r * 0.18f, r * 0.18f, strokeP)
                    }
                    106 -> {
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
                    107 -> {
                        canvas.drawCircle(cx - r * 0.30f, cy, r * 0.50f, strokeP)
                        canvas.drawCircle(cx - r * 0.30f, cy, r * 0.18f, strokeP)
                        canvas.drawLine(cx - r * 0.30f, cy - r * 0.50f, cx + r * 0.74f, cy - r * 0.50f, strokeP)
                        canvas.drawLine(cx - r * 0.30f, cy + r * 0.50f, cx + r * 0.74f, cy + r * 0.50f, strokeP)
                        canvas.drawArc(RectF(cx + r * 0.44f, cy - r * 0.50f, cx + r * 0.84f, cy + r * 0.50f), -90f, 180f, false, strokeP)
                    }
                    108 -> {
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
                    109 -> {
                        for (off in listOf(-0.44f, 0f, 0.44f)) {
                            val y = cy + r * off
                            canvas.drawLine(cx - r * 0.78f, y, cx + r * (0.48f - abs(off) * 0.5f), y, strokeP)
                        }
                        canvas.drawArc(RectF(cx + r * 0.20f, cy - r * 0.76f, cx + r * 0.72f, cy - r * 0.24f), -130f, 220f, false, strokeP)
                    }
                    110 -> {
                        val drop = Path().apply {
                            moveTo(cx, cy - r * 0.82f)
                            quadTo(cx + r * 0.74f, cy + r * 0.14f, cx, cy + r * 0.80f)
                            quadTo(cx - r * 0.74f, cy + r * 0.14f, cx, cy - r * 0.82f)
                        }
                        canvas.drawPath(drop, strokeP)
                        canvas.drawCircle(cx - r * 0.12f, cy + r * 0.24f, r * 0.11f, fillP)
                        canvas.drawCircle(cx + r * 0.16f, cy + r * 0.04f, r * 0.09f, fillP)
                    }
                    111 -> {
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
                    112 -> {
                        canvas.drawLine(cx - r * 0.16f, cy - r * 0.68f, cx - r * 0.16f, cy + r * 0.68f, strokeP)
                        canvas.drawLine(cx + r * 0.16f, cy - r * 0.68f, cx + r * 0.16f, cy + r * 0.68f, strokeP)
                        canvas.drawLine(cx - r * 0.80f, cy, cx - r * 0.16f, cy, strokeP)
                        canvas.drawLine(cx + r * 0.16f, cy, cx + r * 0.80f, cy, strokeP)
                        canvas.drawCircle(cx - r * 0.56f, cy, r * 0.10f, fillP)
                        canvas.drawCircle(cx + r * 0.56f, cy, r * 0.10f, fillP)
                    }
                    113 -> {
                        canvas.drawArc(RectF(cx - r * 0.68f, cy - r * 0.68f, cx + r * 0.68f, cy + r * 0.68f), -50f, 280f, false, strokeP)
                        canvas.drawCircle(cx, cy, r * 0.18f, fillP)
                        canvas.drawLine(cx + r * 0.44f, cy - r * 0.52f, cx + r * 0.76f, cy - r * 0.46f, strokeP)
                    }
                    114 -> {
                        canvas.drawLine(cx, cy - r * 0.74f, cx, cy + r * 0.18f, strokeP)
                        canvas.drawCircle(cx, cy + r * 0.26f, r * 0.22f, strokeP)
                        canvas.drawLine(cx - r * 0.68f, cy + r * 0.68f, cx + r * 0.68f, cy + r * 0.68f, strokeP)
                        canvas.drawLine(cx + r * 0.34f, cy - r * 0.46f, cx + r * 0.66f, cy - r * 0.46f, strokeP)
                        canvas.drawLine(cx + r * 0.34f, cy - r * 0.16f, cx + r * 0.58f, cy - r * 0.16f, strokeP)
                    }
                    115 -> {
                        canvas.drawLine(cx - r * 0.74f, cy, cx + r * 0.74f, cy, strokeP)
                        canvas.drawLine(cx, cy - r * 0.78f, cx, cy - r * 0.22f, strokeP)
                        canvas.drawLine(cx - r * 0.24f, cy - r * 0.44f, cx, cy - r * 0.22f, strokeP)
                        canvas.drawLine(cx + r * 0.24f, cy - r * 0.44f, cx, cy - r * 0.22f, strokeP)
                        canvas.drawLine(cx, cy + r * 0.78f, cx, cy + r * 0.22f, strokeP)
                        canvas.drawLine(cx - r * 0.24f, cy + r * 0.44f, cx, cy + r * 0.22f, strokeP)
                        canvas.drawLine(cx + r * 0.24f, cy + r * 0.44f, cx, cy + r * 0.22f, strokeP)
                    }
                    116 -> {
                        canvas.drawArc(RectF(cx - r * 0.74f, cy - r * 0.74f, cx + r * 0.74f, cy + r * 0.74f), -140f, 220f, false, strokeP)
                        canvas.drawLine(cx, cy, cx + r * 0.56f, cy - r * 0.38f, strokeP)
                        canvas.drawCircle(cx, cy, r * 0.14f, fillP)
                    }
                    117 -> {
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
                    else -> {
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
// קנבס מדעי/הנדסי אינטראקטיבי עם צירים, שנתות ותוויות חיות לכל 18 הממירים
// =========================================================================
private class ExtUnitHeroCanvasView(
    private val activity: MainActivity,
    private val tabIndex: Int,
    initialGroup: ExtUnitGroup
) : View(activity) {

    private val d = resources.displayMetrics.density
    private var phase = 0f
    private var reactPulse = 0f
    private var animator: ValueAnimator? = null

    var liveInputVal: Double = initialGroup.defaultValue
    var liveBaseVal: Double = initialGroup.units[initialGroup.defaultFrom].toBase(initialGroup.defaultValue)
    var liveResultVal: Double = initialGroup.units[initialGroup.defaultTo].fromBase(liveBaseVal)
    var liveFromSym: String = initialGroup.units[initialGroup.defaultFrom].symbol
    var liveToSym: String = initialGroup.units[initialGroup.defaultTo].symbol
    var activeGroupIdx: Int = 0

    private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val dashedP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * d
        pathEffect = DashPathEffect(floatArrayOf(4f * d, 3f * d), 0f)
    }
    private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val subTextP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    private val path = Path()
    private val rect = RectF()

    fun updateLiveData(
        inputV: Double,
        baseV: Double,
        resultV: Double,
        fromSym: String,
        toSym: String,
        groupIdx: Int
    ) {
        val changed = (inputV != liveInputVal || baseV != liveBaseVal || groupIdx != activeGroupIdx)
        liveInputVal = inputV
        liveBaseVal = baseV
        liveResultVal = resultV
        liveFromSym = fromSym
        liveToSym = toSym
        activeGroupIdx = groupIdx
        if (changed) reactPulse = 1f
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
                    if (reactPulse > 0f) reactPulse = (reactPulse - 0.08f).coerceAtLeast(0f)
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

    private fun logNorm(v: Double, minExp: Double, maxExp: Double): Float {
        val a = abs(v)
        if (a <= 0.0 || a.isNaN() || a.isInfinite()) return 0f
        val e = log10(a)
        return ((e - minExp) / (maxExp - minExp)).toFloat().coerceIn(0f, 1f)
    }

    private fun mantissaFrac(v: Double): Float {
        val a = abs(v)
        if (a <= 0.0 || a.isNaN() || a.isInfinite()) return 0f
        val lg = log10(a)
        return (lg - floor(lg)).toFloat().coerceIn(0f, 1f)
    }

    private fun drawArrowRight(canvas: Canvas, x: Float, y: Float, size: Float, paint: Paint) {
        canvas.drawLine(x, y, x - size, y - size * 0.6f, paint)
        canvas.drawLine(x, y, x - size, y + size * 0.6f, paint)
    }

    private fun drawArrowLeft(canvas: Canvas, x: Float, y: Float, size: Float, paint: Paint) {
        canvas.drawLine(x, y, x + size, y - size * 0.6f, paint)
        canvas.drawLine(x, y, x + size, y + size * 0.6f, paint)
    }

    private fun drawArrowUp(canvas: Canvas, x: Float, y: Float, size: Float, paint: Paint) {
        canvas.drawLine(x, y, x - size * 0.6f, y + size, paint)
        canvas.drawLine(x, y, x + size * 0.6f, y + size, paint)
    }

    // סרגל קנה מידה מדעי אופקי עם תווית כותרת, שנתות ייחוס וסמן ערך חי
    private fun drawReferenceScaleBar(
        canvas: Canvas,
        left: Float,
        right: Float,
        y: Float,
        title: String,
        valueBadge: String,
        progress: Float,
        ticks: List<Pair<Float, String>>,
        accent: Int,
        markerColor: Int,
        ink: Int
    ) {
        val span = (right - left).coerceAtLeast(10f)
        val cxBar = (left + right) * 0.5f

        textP.textAlign = Paint.Align.CENTER
        textP.textSize = 9.5f * d
        textP.color = withAlpha(ink, 210)
        canvas.drawText(title, cxBar, y - 22f * d, textP)

        textP.textSize = 10.5f * d
        textP.color = accent
        canvas.drawText(valueBadge, cxBar, y - 9f * d, textP)

        strokeP.strokeWidth = 3.2f * d
        strokeP.color = withAlpha(ink, 38)
        canvas.drawLine(left, y, right, y, strokeP)

        val clamped = progress.coerceIn(0f, 1f)
        val markerX = left + span * clamped
        strokeP.color = accent
        canvas.drawLine(left, y, markerX, y, strokeP)

        strokeP.strokeWidth = 1.2f * d
        strokeP.color = withAlpha(ink, 110)
        subTextP.textAlign = Paint.Align.CENTER
        subTextP.textSize = 8.2f * d
        subTextP.color = withAlpha(ink, 175)
        for ((pos, lbl) in ticks) {
            val tx = left + span * pos.coerceIn(0f, 1f)
            canvas.drawLine(tx, y - 4f * d, tx, y + 4f * d, strokeP)
            canvas.drawText(lbl, tx, y + 14f * d, subTextP)
        }

        fillP.color = markerColor
        canvas.drawCircle(markerX, y, (4.6f + reactPulse * 1.8f) * d, fillP)
        strokeP.strokeWidth = 1.2f * d
        strokeP.color = Color.WHITE
        canvas.drawCircle(markerX, y, (4.6f + reactPulse * 1.8f) * d, strokeP)
    }

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

        // רקע כרטיס מותאם לערכת הנושא + פולס גבול עדין בהקלדה
        rect.set(2f * d, 2f * d, w - 2f * d, h - 2f * d)
        val corner = 20f * d
        val pulseStrokeExtra = reactPulse * 1.3f * d
        when (style) {
            ProCanvasAnimations.ThemeRenderStyle.MINIMALIST_PASTEL -> {
                fillP.shader = LinearGradient(0f, 0f, w, h, MinimalistTheme.CARD_WHITE, withAlpha(c1, 36 + (reactPulse * 24).toInt()), Shader.TileMode.CLAMP)
                canvas.drawRoundRect(rect, corner, corner, fillP)
                fillP.shader = null
                strokeP.clearShadowLayer()
                strokeP.strokeWidth = 1.2f * d + pulseStrokeExtra
                strokeP.color = if (reactPulse > 0.1f) withAlpha(c1, 165) else MinimalistTheme.resolveBorderStrokeColor(activity, Color.parseColor("#E3DFD9"))
                canvas.drawRoundRect(rect, corner, corner, strokeP)
            }
            ProCanvasAnimations.ThemeRenderStyle.LIQUID_GLASS -> {
                fillP.shader = LinearGradient(0f, 0f, w, h, Color.argb(70, 255, 255, 255), withAlpha(c1, 38 + (reactPulse * 30).toInt()), Shader.TileMode.CLAMP)
                canvas.drawRoundRect(rect, corner, corner, fillP)
                fillP.shader = null
                strokeP.clearShadowLayer()
                strokeP.strokeWidth = 1.3f * d + pulseStrokeExtra
                strokeP.color = Color.argb((120 + reactPulse * 80).toInt().coerceAtMost(255), 255, 255, 255)
                canvas.drawRoundRect(rect, corner, corner, strokeP)
            }
            ProCanvasAnimations.ThemeRenderStyle.AURORA_NEON -> {
                fillP.shader = LinearGradient(0f, 0f, w, h, withAlpha(AuroraBorealisTheme.EMERALD_NIGHT, 235), withAlpha(c1, 45 + (reactPulse * 35).toInt()), Shader.TileMode.CLAMP)
                canvas.drawRoundRect(rect, corner, corner, fillP)
                fillP.shader = null
                strokeP.strokeWidth = 1.4f * d + pulseStrokeExtra
                strokeP.color = withAlpha(c1, 185)
                strokeP.setShadowLayer((6f + reactPulse * 4f) * d, 0f, 0f, withAlpha(c1, 175))
                canvas.drawRoundRect(rect, corner, corner, strokeP)
            }
            ProCanvasAnimations.ThemeRenderStyle.CLASSIC_VECTOR -> {
                fillP.shader = null
                fillP.color = Utils.lightenColor(activity.bgColor, 0.08f)
                canvas.drawRoundRect(rect, corner, corner, fillP)
                strokeP.clearShadowLayer()
                strokeP.strokeWidth = 0.7f * d
                strokeP.color = withAlpha(c1, 24)
                var gx = 20f * d
                while (gx < w - 10f * d) { canvas.drawLine(gx, 8f * d, gx, h - 8f * d, strokeP); gx += 20f * d }
                strokeP.strokeWidth = 1.3f * d + pulseStrokeExtra
                strokeP.color = withAlpha(c1, (130 + reactPulse * 95).toInt())
                canvas.drawRoundRect(rect, corner, corner, strokeP)
            }
        }

        val cx = w * 0.5f
        val cy = h * 0.46f
        val leftCx = w * 0.28f
        val rightL = w * 0.55f
        val rightR = w * 0.92f
        val rightCx = (rightL + rightR) * 0.5f
        val animGlow = (style == ProCanvasAnimations.ThemeRenderStyle.AURORA_NEON)
        if (animGlow) strokeP.setShadowLayer(5f * d, 0f, 0f, withAlpha(c1, 165)) else strokeP.clearShadowLayer()
        textP.textAlign = Paint.Align.CENTER
        subTextP.textAlign = Paint.Align.CENTER

        when (tabIndex) {
            // 100: קידומות SI — ציר חזקות מלא 10⁻³⁰ עד 10³⁰ + תגית סדר גודל
            100 -> {
                val yBar = cy + 6f * d
                val leftX = w * 0.10f
                val rightX = w * 0.90f
                val spanW = rightX - leftX

                subTextP.textSize = 9.2f * d; subTextP.color = withAlpha(ink, 170)
                canvas.drawText("ציר חזקות לוגריתמי SI (10ⁿ)", cx, 20f * d, subTextP)

                strokeP.strokeWidth = 2.6f * d; strokeP.color = withAlpha(ink, 45)
                canvas.drawLine(leftX, yBar, rightX, yBar, strokeP)

                val expVal = if (liveBaseVal != 0.0 && liveBaseVal.isFinite()) log10(abs(liveBaseVal)).toFloat() else 0f
                val expClamped = expVal.coerceIn(-30f, 30f)
                val zeroX = leftX + spanW * 0.5f
                val px = leftX + ((expClamped + 30f) / 60f) * spanW

                strokeP.strokeWidth = 4.2f * d; strokeP.color = c1
                canvas.drawLine(zeroX, yBar, px, yBar, strokeP)

                val ticks = listOf(-30, -18, -9, 0, 9, 18, 30)
                textP.textSize = 9.0f * d; textP.color = withAlpha(ink, 175)
                strokeP.strokeWidth = 1.5f * d; strokeP.color = withAlpha(ink, 95)
                for (t in ticks) {
                    val tx = leftX + ((t + 30f) / 60f) * spanW
                    canvas.drawLine(tx, yBar - 6f * d, tx, yBar + 6f * d, strokeP)
                    canvas.drawText("10${ProExtendedUnitConverters.toSuperscript(t)}", tx, yBar + 17f * d, textP)
                }

                fillP.color = c2
                canvas.drawCircle(px, yBar, (6.5f + reactPulse * 2.4f) * d, fillP)
                textP.textSize = 11f * d; textP.color = ink
                canvas.drawText("10${ProExtendedUnitConverters.toSuperscript(expVal.roundToInt())}", px.coerceIn(w * 0.14f, w * 0.86f), yBar - 13f * d, textP)
            }

            // 101: שטח קרקע — חלקה ממוספרת עם צלע a×a וסרגל ייחוס (דירה / דונם / הקטר)
            101 -> {
                val n = logNorm(liveBaseVal, 1.0, 7.0)
                val sidePx = (32f + n * 38f) * d
                val sqCx = leftCx
                val sqCy = cy + 2f * d
                rect.set(sqCx - sidePx * 0.5f, sqCy - sidePx * 0.5f, sqCx + sidePx * 0.5f, sqCy + sidePx * 0.5f)
                fillP.color = withAlpha(pal.positive, 50 + (n * 35).toInt())
                canvas.drawRoundRect(rect, 6f * d, 6f * d, fillP)
                strokeP.strokeWidth = 2.0f * d; strokeP.color = pal.positive
                canvas.drawRoundRect(rect, 6f * d, 6f * d, strokeP)

                val cols = (2 + (n * 4).toInt()).coerceIn(2, 6)
                strokeP.strokeWidth = 0.8f * d; strokeP.color = withAlpha(pal.positive, 90)
                for (i in 1 until cols) {
                    val lx = rect.left + (rect.width() * i / cols)
                    val ly = rect.top + (rect.height() * i / cols)
                    canvas.drawLine(lx, rect.top, lx, rect.bottom, strokeP)
                    canvas.drawLine(rect.left, ly, rect.right, ly, strokeP)
                }
                val sideStr = ProExtendedUnitConverters.formatSmartValue(sqrt(abs(liveBaseVal)), 2)
                textP.textSize = 9.5f * d; textP.color = ink
                canvas.drawText("\u200Ea = $sideStr m", sqCx, rect.top - 6f * d, textP)
                subTextP.textSize = 9f * d; subTextP.color = withAlpha(ink, 195)
                canvas.drawText("S = a²", sqCx, sqCy + 3.5f * d, subTextP)

                drawReferenceScaleBar(
                    canvas, rightL, rightR, cy + 8f * d,
                    title = "סולם שטח (m²)",
                    valueBadge = "\u200E${ProExtendedUnitConverters.formatSmartValue(abs(liveBaseVal) / 1000.0, 2)} דונם",
                    progress = n,
                    ticks = listOf(0.17f to "דירה", 0.33f to "דונם", 0.50f to "הקטר", 0.83f to "km²"),
                    accent = pal.positive,
                    markerColor = c2,
                    ink = ink
                )
            }

            // 102: מספרים גדולים — 6 עמודות סדרי גודל + תווית ספרות וחזקה
            102 -> {
                val exp = if (abs(liveBaseVal) >= 1.0 && liveBaseVal.isFinite()) log10(abs(liveBaseVal)) else 0.0
                val digitsCount = if (abs(liveBaseVal) >= 1.0 && liveBaseVal.isFinite()) floor(exp).toInt() + 1 else if (liveBaseVal == 0.0) 1 else 0
                val baseTier = if (exp >= 18.0) ((floor(exp / 3.0).toInt() - 5) * 3).coerceAtLeast(0) else 0
                val bars = 6
                val bw = w * 0.095f
                val gap = w * 0.125f
                val startX = cx - ((bars - 1) * gap + bw) * 0.5f
                val bottomY = cy + 32f * d
                val maxBarH = 56f * d

                for (i in 0 until bars) {
                    val tierExp = (baseTier + i * 3).toDouble()
                    val fillFrac = when {
                        abs(liveBaseVal) <= 0.0 -> 0.08f
                        exp >= tierExp + 3.0 -> 1.0f
                        exp <= tierExp -> 0.12f
                        else -> (0.12f + ((exp - tierExp) / 3.0).toFloat() * 0.88f)
                    }
                    val targetMax = maxBarH * (0.30f + i * 0.14f)
                    val wobble = if (fillFrac > 0.15f) sin(phase * 1.4f + i) * 2.0f * d else 0f
                    val bh = (targetMax * fillFrac + wobble).coerceAtLeast(6f * d)
                    val bx = startX + i * gap
                    rect.set(bx, bottomY - bh, bx + bw, bottomY)

                    val isActiveBar = (exp >= tierExp && exp < tierExp + 3.0) || (i == bars - 1 && exp >= tierExp + 3.0)
                    val barCol = when {
                        isActiveBar -> c1
                        exp >= tierExp + 3.0 -> pal.positive
                        else -> ink
                    }
                    fillP.color = withAlpha(barCol, if (isActiveBar) 225 else if (exp >= tierExp) 140 else 36)
                    canvas.drawRoundRect(rect, 5f * d, 5f * d, fillP)

                    textP.textSize = 9f * d
                    textP.color = withAlpha(ink, if (isActiveBar) 235 else 135)
                    canvas.drawText("10${ProExtendedUnitConverters.toSuperscript(tierExp.toInt())}", bx + bw * 0.5f, bottomY + 11f * d, textP)
                }

                textP.textSize = 10.5f * d; textP.color = c1
                canvas.drawText("\u200F$digitsCount ספרות  •  סדר גודל 10${ProExtendedUnitConverters.toSuperscript(floor(exp).toInt())}", cx, 19f * d, textP)
            }

            // 103: זמן תרבותי ומסורתי — שעון מחוגים + מונה חלקים וסרגל תקופות
            103 -> {
                val r = 34f * d
                val clockX = leftCx
                val sec = abs(liveBaseVal)
                val halakim = sec / (10.0 / 3.0)
                val fracDay = ((sec % 86_400.0) / 86_400.0).toFloat()
                val logCycle = logNorm(sec, 0.0, 10.0)

                strokeP.strokeWidth = 2.0f * d; strokeP.color = withAlpha(ink, 45)
                canvas.drawCircle(clockX, cy, r, strokeP)
                for (t in 0 until 12) {
                    val a = Math.toRadians(t * 30.0)
                    canvas.drawLine(
                        clockX + (cos(a) * r * 0.84f).toFloat(), cy + (sin(a) * r * 0.84f).toFloat(),
                        clockX + (cos(a) * r).toFloat(), cy + (sin(a) * r).toFloat(),
                        strokeP
                    )
                }
                rect.set(clockX - r, cy - r, clockX + r, cy + r)
                strokeP.strokeWidth = 3.8f * d; strokeP.color = pal.positive
                canvas.drawArc(rect, -90f, 15f + logCycle * 345f, false, strokeP)

                val bigDeg = -90.0 + ((halakim % 1080.0) / 1080.0) * 360.0 + mantissaFrac(sec) * 360.0 + sin(phase) * 2.5
                val bigRad = Math.toRadians(bigDeg)
                strokeP.strokeWidth = 2.3f * d; strokeP.color = c1
                canvas.drawLine(clockX, cy, clockX + (cos(bigRad) * r * 0.76f).toFloat(), cy + (sin(bigRad) * r * 0.76f).toFloat(), strokeP)

                val smallRad = Math.toRadians(-90.0 + fracDay * 360.0 + logCycle * 720.0)
                strokeP.strokeWidth = 2.8f * d; strokeP.color = c2
                canvas.drawLine(clockX, cy, clockX + (cos(smallRad) * r * 0.48f).toFloat(), cy + (sin(smallRad) * r * 0.48f).toFloat(), strokeP)

                fillP.color = c3
                canvas.drawCircle(clockX, cy, 3.8f * d, fillP)

                val eraLabel = when {
                    sec < 60.0 -> "שניות / חלקים"
                    sec < 86_400.0 -> "שעות"
                    sec < 31_556_926.0 -> "ימים / חודשים"
                    sec < 31_556_926.0 * 50.0 -> "שנים / שמיטה"
                    else -> "יובלות ומאות"
                }
                drawReferenceScaleBar(
                    canvas, rightL, rightR, cy + 8f * d,
                    title = "סולם זמן ($eraLabel)",
                    valueBadge = "\u200E${ProExtendedUnitConverters.formatSmartValue(halakim, 2)} חלקים",
                    progress = logCycle,
                    ticks = listOf(0.05f to "חלק", 0.36f to "שעה", 0.64f to "חודש", 0.92f to "יובל"),
                    accent = c1,
                    markerColor = c2,
                    ink = ink
                )
            }

            // 104: צפיפות חוט וטקסטיל — חתך סיב + גל חוט + סרגל עובי ב-Tex
            104 -> {
                val tex = abs(liveBaseVal)
                val n = logNorm(tex, 0.0, 3.0)
                val crossR = (9f + n * 16f) * d
                val fiberCx = w * 0.14f
                fillP.color = withAlpha(c1, 100)
                canvas.drawCircle(fiberCx, cy, crossR, fillP)
                strokeP.strokeWidth = 1.8f * d; strokeP.color = c2
                canvas.drawCircle(fiberCx, cy, crossR, strokeP)
                subTextP.textSize = 8.5f * d; subTextP.color = withAlpha(ink, 185)
                canvas.drawText("חתך Ø", fiberCx, cy + crossR + 11f * d, subTextP)

                // סיב גלי במרכז-שמאל
                val fiberW = (2.6f + n * 12f) * d
                path.reset()
                var x = w * 0.25f
                while (x <= w * 0.50f) {
                    val y = cy + sin(x * (0.07f - n * 0.03f) + phase * 1.5f) * (8f + n * 7f) * d
                    if (x == w * 0.25f) path.moveTo(x, y) else path.lineTo(x, y)
                    x += 4f * d
                }
                strokeP.strokeWidth = fiberW; strokeP.color = withAlpha(c1, 80)
                canvas.drawPath(path, strokeP)
                strokeP.strokeWidth = (1.5f + n * 1.6f) * d; strokeP.color = c2
                canvas.drawPath(path, strokeP)

                val grade = when {
                    tex < 8.0 -> "חוט דק (משי)"
                    tex < 25.0 -> "עדין (כותנה)"
                    tex < 60.0 -> "עבה (ג׳ינס)"
                    else -> "כבד (חבל)"
                }
                drawReferenceScaleBar(
                    canvas, rightL, rightR, cy + 8f * d,
                    title = "סיווג סיב: $grade",
                    valueBadge = "\u200E${ProExtendedUnitConverters.formatSmartValue(tex, 2)} tex",
                    progress = n,
                    ticks = listOf(0.10f to "2", 0.38f to "15", 0.60f to "60", 0.90f to "500"),
                    accent = c2,
                    markerColor = c1,
                    ink = ink
                )
            }

            // 105: קלוריות מזון — טבעת תפריט יומי + מד שריפת אנרגיה בהליכה/ריצה
            105 -> {
                val kcal = abs(liveBaseVal) / 4184.0
                val dailyFrac = (kcal / 2000.0).toFloat()
                val pct = dailyFrac.coerceIn(0f, 1.0f)
                val r = 34f * d
                val ringCol = when {
                    dailyFrac < 0.35f -> pal.positive
                    dailyFrac <= 1.0f -> pal.warning
                    else -> Color.parseColor("#EF5350")
                }
                rect.set(leftCx - r, cy - r, leftCx + r, cy + r)
                strokeP.strokeWidth = 7.5f * d; strokeP.color = withAlpha(ink, 28)
                canvas.drawCircle(leftCx, cy, r, strokeP)
                strokeP.color = ringCol
                canvas.drawArc(rect, -90f, 360f * pct, false, strokeP)

                textP.textSize = 11.5f * d; textP.color = ink
                val centerTxt = if (dailyFrac >= 10f) "×${ProExtendedUnitConverters.formatSmartValue(dailyFrac.toDouble(), 1)}" else "${(dailyFrac * 100f).roundToInt()}%"
                canvas.drawText(centerTxt, leftCx, cy + 2f * d, textP)
                subTextP.textSize = 7.8f * d; subTextP.color = withAlpha(ink, 165)
                canvas.drawText("מתפריט יומי", leftCx, cy + 13f * d, subTextP)

                val walkMin = ProExtendedUnitConverters.formatSmartValue(kcal / 4.5, 0)
                val runMin = ProExtendedUnitConverters.formatSmartValue(kcal / 10.5, 0)
                drawReferenceScaleBar(
                    canvas, rightL, rightR, cy + 8f * d,
                    title = "תפריט 2,000 kcal",
                    valueBadge = "הליכה $walkMin דק׳ | ריצה $runMin דק׳",
                    progress = pct,
                    ticks = listOf(0.0f to "0", 0.25f to "500", 0.50f to "1k", 1.0f to "2k"),
                    accent = ringCol,
                    markerColor = c1,
                    ink = ink
                )
            }

            // 106: משקל זהב ומתכות — פירמידת מטילים + פירוק קראט חי (24K / 18K / 14K)
            106 -> {
                val grams = abs(liveBaseVal)
                val n = logNorm(grams, 0.0, 6.0)
                val barCount = if (grams <= 0.0) 1 else (1 + floor(n * 5.99f).toInt()).coerceIn(1, 6)
                val gold = Color.parseColor("#FFD54F")
                val goldDark = Color.parseColor("#F59E0B")

                val barW = 42f * d
                val barH = 16f * d
                val positions = when (barCount) {
                    1 -> listOf(leftCx to cy)
                    2 -> listOf((leftCx - barW * 0.52f) to (cy + 4f * d), (leftCx + barW * 0.52f) to (cy + 4f * d))
                    3 -> listOf((leftCx - barW * 0.52f) to (cy + 10f * d), (leftCx + barW * 0.52f) to (cy + 10f * d), leftCx to (cy - 8f * d))
                    4 -> listOf((leftCx - barW * 0.85f) to (cy + 12f * d), leftCx to (cy + 12f * d), (leftCx + barW * 0.85f) to (cy + 12f * d), leftCx to (cy - 7f * d))
                    5 -> listOf((leftCx - barW * 0.85f) to (cy + 12f * d), leftCx to (cy + 12f * d), (leftCx + barW * 0.85f) to (cy + 12f * d), (leftCx - barW * 0.42f) to (cy - 7f * d), (leftCx + barW * 0.42f) to (cy - 7f * d))
                    else -> listOf((leftCx - barW * 0.85f) to (cy + 16f * d), leftCx to (cy + 16f * d), (leftCx + barW * 0.85f) to (cy + 16f * d), (leftCx - barW * 0.42f) to (cy - 2f * d), (leftCx + barW * 0.42f) to (cy - 2f * d), leftCx to (cy - 20f * d))
                }

                val shOffset = sin(phase * 1.4f + n * 4f) * 24f * d
                for ((idx, pos) in positions.withIndex()) {
                    val bx = pos.first; val by = pos.second
                    path.reset()
                    path.moveTo(bx - barW * 0.5f, by + barH * 0.5f)
                    path.lineTo(bx + barW * 0.5f, by + barH * 0.5f)
                    path.lineTo(bx + barW * 0.36f, by - barH * 0.5f)
                    path.lineTo(bx - barW * 0.36f, by - barH * 0.5f)
                    path.close()
                    fillP.shader = LinearGradient(bx - barW * 0.4f + shOffset, by - barH * 0.5f, bx + barW * 0.4f + shOffset, by + barH * 0.5f, intArrayOf(goldDark, Color.WHITE, gold), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
                    canvas.drawPath(path, fillP)
                    fillP.shader = null
                    strokeP.strokeWidth = 1.4f * d; strokeP.color = goldDark
                    canvas.drawPath(path, strokeP)
                    if (idx == positions.lastIndex) {
                        textP.textSize = 7.8f * d; textP.color = Color.parseColor("#4E342E")
                        canvas.drawText("24K", bx, by + 3f * d, textP)
                    }
                }

                val ozT = ProExtendedUnitConverters.formatSmartValue(grams / 31.1034768, 2)
                drawReferenceScaleBar(
                    canvas, rightL, rightR, cy + 8f * d,
                    title = "סולם מתכות יקרות (g)",
                    valueBadge = "\u200E$ozT oz t  •  18K: ${ProExtendedUnitConverters.formatSmartValue(grams * 0.75, 1)}g",
                    progress = n,
                    ticks = listOf(0.0f to "1g", 0.25f to "1 oz", 0.50f to "1 kg", 0.85f to "100kg"),
                    accent = goldDark,
                    markerColor = c1,
                    ink = ink
                )
            }

            // 107: נפח עץ ונגרות — חתך גזע + קורות ממוספרות + סרגל נפח ומשקל אורן
            107 -> {
                val m3 = abs(liveBaseVal)
                val n = logNorm(m3, -3.0, 4.0)
                val r = (14f + n * 14f) * d
                val logX = w * 0.16f
                fillP.color = withAlpha(Color.parseColor("#D7A86E"), 95 + (n * 55).toInt())
                canvas.drawCircle(logX, cy, r, fillP)
                strokeP.strokeWidth = 1.4f * d; strokeP.color = Color.parseColor("#8D6E63")
                val rings = (2 + (n * 4).toInt()).coerceIn(2, 6)
                for (k in 1..rings) canvas.drawCircle(logX, cy, r * (k.toFloat() / rings), strokeP)
                subTextP.textSize = 8.2f * d; subTextP.color = withAlpha(ink, 180)
                canvas.drawText("גזע", logX, cy + r + 10f * d, subTextP)

                val planks = if (m3 <= 0.0) 1 else (1 + floor(n * 4.99f).toInt()).coerceIn(1, 5)
                val plankH = 8f * d
                val gap = 11f * d
                val topY = cy - ((planks - 1) * gap) * 0.5f
                val maxPlankW = (28f + n * 34f) * d
                val plankX = w * 0.29f
                for (b in 0 until planks) {
                    val py = topY + b * gap
                    rect.set(plankX, py - plankH * 0.5f, plankX + maxPlankW, py + plankH * 0.5f)
                    fillP.color = withAlpha(if (b % 2 == 0) c2 else c1, 130)
                    canvas.drawRoundRect(rect, 3f * d, 3f * d, fillP)
                    canvas.drawRoundRect(rect, 3f * d, 3f * d, strokeP)
                }
                canvas.drawText("קורות", plankX + maxPlankW * 0.5f, topY + (planks - 1) * gap + 15f * d, subTextP)

                val pineKg = ProExtendedUnitConverters.formatSmartValue(m3 * 520.0, 1)
                drawReferenceScaleBar(
                    canvas, rightL, rightR, cy + 8f * d,
                    title = "נפח עץ (משקל אורן ≈ ${pineKg}kg)",
                    valueBadge = "\u200E${ProExtendedUnitConverters.formatSmartValue(m3 / 0.0023597372, 1)} FBM",
                    progress = n,
                    ticks = listOf(0.05f to "FBM", 0.43f to "1 m³", 0.52f to "Cord", 0.90f to "10³m³"),
                    accent = Color.parseColor("#8D6E63"),
                    markerColor = c2,
                    ink = ink
                )
            }

            // 108: כושר קירור ומיזוג — מאוורר + חצי זרם קר + סרגל BTU/TR וגודל חדר
            108 -> {
                val watts = abs(liveBaseVal)
                val btuh = watts / 0.29307107
                val tr = watts / 3516.85284
                val n = logNorm(btuh, 3.0, 7.0)
                val fanX = w * 0.18f
                val r = (18f + n * 10f) * d
                val rotDeg = phase * (32f + n * 100f) + mantissaFrac(watts) * 360f
                val coolCol = if (n > 0.65f) Color.parseColor("#00B0FF") else c3

                canvas.save()
                canvas.rotate(rotDeg, fanX, cy)
                strokeP.strokeWidth = (2.0f + n * 1.1f) * d; strokeP.color = coolCol
                val blades = if (n > 0.5f) 8 else 6
                for (i in 0 until blades) {
                    val a = Math.toRadians(i * (360.0 / blades))
                    canvas.drawLine(fanX, cy, fanX + (cos(a) * r).toFloat(), cy + (sin(a) * r).toFloat(), strokeP)
                }
                canvas.restore()
                fillP.color = c1
                canvas.drawCircle(fanX, cy, 4f * d, fillP)

                val streams = (3 + floor(n * 2.99f).toInt()).coerceIn(3, 5)
                val sGap = 11f * d
                val topS = cy - ((streams - 1) * sGap) * 0.5f
                val sStart = fanX + r + 8f * d
                val sEnd = w * 0.49f
                for (s in 0 until streams) {
                    val sy = topS + s * sGap
                    strokeP.strokeWidth = 1.6f * d
                    strokeP.color = withAlpha(if (s % 2 == 0) coolCol else c1, 175)
                    canvas.drawLine(sStart, sy, sEnd, sy, strokeP)
                    drawArrowRight(canvas, sEnd, sy, 4f * d, strokeP)
                }

                val roomM2 = ProExtendedUnitConverters.formatSmartValue(btuh / 620.0, 1)
                drawReferenceScaleBar(
                    canvas, rightL, rightR, cy + 8f * d,
                    title = "תפוקת קירור (חדר ≈ ${roomM2}m²)",
                    valueBadge = "\u200E${ProExtendedUnitConverters.formatSmartValue(tr, 2)} TR",
                    progress = n,
                    ticks = listOf(0.05f to "1k", 0.27f to "12k", 0.50f to "100k", 0.90f to "10M"),
                    accent = coolCol,
                    markerColor = c1,
                    ink = ink
                )
            }

            // 109: זרימת אוויר (HVAC) — מנהרת רוח עם חצים + ציר ספיקה ו-ACH
            109 -> {
                val m3h = abs(liveBaseVal * 3600.0)
                val ach = m3h / 50.0
                val n = logNorm(m3h, 1.0, 7.0)
                val lineCount = (3 + floor(n * 4.99f).toInt()).coerceIn(3, 7)
                val gap = (54f / (lineCount - 1).coerceAtLeast(1)) * d
                val startY = cy - 27f * d
                val pipeL = w * 0.08f
                val pipeR = w * 0.49f

                // קירות תעלת אוורור
                strokeP.strokeWidth = 1.5f * d; strokeP.color = withAlpha(ink, 85)
                canvas.drawLine(pipeL, cy - 33f * d, pipeR, cy - 33f * d, strokeP)
                canvas.drawLine(pipeL, cy + 33f * d, pipeR, cy + 33f * d, strokeP)

                val lineLen = (24f + n * 30f) * d
                val travel = (pipeR - pipeL - lineLen).coerceAtLeast(8f)
                for (i in 0 until lineCount) {
                    val y = startY + i * gap
                    val x = pipeL + 3f * d + ((phase * (26f + n * 50f) * d + i * 19f * d + mantissaFrac(m3h) * travel) % travel)
                    val x2 = (x + lineLen).coerceAtMost(pipeR - 3f * d)
                    strokeP.strokeWidth = (1.5f + n * 1.1f) * d
                    strokeP.color = withAlpha(if (i % 2 == 0) c1 else c3, 185)
                    canvas.drawLine(x, y, x2, y, strokeP)
                    drawArrowRight(canvas, x2, y, 3.6f * d, strokeP)
                }

                drawReferenceScaleBar(
                    canvas, rightL, rightR, cy + 8f * d,
                    title = "ספיקת אוויר (חדר 50m³)",
                    valueBadge = "\u200E${ProExtendedUnitConverters.formatSmartValue(ach, 1)} ACH",
                    progress = n,
                    ticks = listOf(0.05f to "10", 0.33f to "1k", 0.66f to "100k", 0.95f to "10M"),
                    accent = c1,
                    markerColor = c3,
                    ink = ink
                )
            }

            // 110: קשיות מים — טיפה עם יוני Ca²⁺/Mg²⁺ + סרגל 4 דרגות קשיות צבעוני
            110 -> {
                val ppm = abs(liveBaseVal)
                val dh = ppm / 17.848
                val n = (ppm / 300.0).toFloat().coerceIn(0f, 1f)
                val (dropCol, statusLbl) = when {
                    ppm < 60.0 -> Color.parseColor("#4FC3F7") to "מים רכים"
                    ppm < 120.0 -> Color.parseColor("#81C784") to "קשיות קלה"
                    ppm < 180.0 -> Color.parseColor("#FFB74D") to "מים קשים"
                    else -> Color.parseColor("#EF5350") to "קשים מאוד"
                }

                path.reset()
                path.moveTo(leftCx, cy - 30f * d)
                path.quadTo(leftCx + 30f * d, cy + 8f * d, leftCx, cy + 30f * d)
                path.quadTo(leftCx - 30f * d, cy + 8f * d, leftCx, cy - 30f * d)
                fillP.color = withAlpha(dropCol, 60 + (n * 60).toInt())
                canvas.drawPath(path, fillP)
                strokeP.strokeWidth = 2.0f * d; strokeP.color = dropCol
                canvas.drawPath(path, strokeP)

                val ions = if (ppm <= 0.0) 0 else (2 + floor(n * 11f).toInt()).coerceIn(2, 13)
                fillP.color = dropCol
                for (k in 0 until ions) {
                    val ix = leftCx + sin(phase * 1.2f + k * 1.9f + mantissaFrac(ppm) * 6f) * 11f * d
                    val iy = cy + 6f * d + cos(phase * 1.0f + k * 2.3f) * 11f * d
                    canvas.drawCircle(ix, iy, 2.2f * d, fillP)
                }
                subTextP.textSize = 8f * d; subTextP.color = withAlpha(ink, 180)
                canvas.drawText("Ca²⁺/Mg²⁺", leftCx, cy + 40f * d, subTextP)

                drawReferenceScaleBar(
                    canvas, rightL, rightR, cy + 8f * d,
                    title = "סיווג קשיות: $statusLbl",
                    valueBadge = "\u200E${ProExtendedUnitConverters.formatSmartValue(dh, 2)} °dH",
                    progress = n,
                    ticks = listOf(0.0f to "0", 0.20f to "60", 0.60f to "180", 1.0f to "300+"),
                    accent = dropCol,
                    markerColor = ink,
                    ink = ink
                )
            }

            // 111: מתח פנים — מניסקוס נוזל + חצי כוח γ + סרגל ייחוס (אתנול / מים / כספית)
            111 -> {
                val mnm = abs(liveBaseVal * 1000.0)
                val n = logNorm(mnm, 0.8, 3.0)
                val curveDepth = (-8f + n * 38f) * d
                val yBase = cy - 10f * d
                val mL = w * 0.08f
                val mR = w * 0.49f
                val mCx = (mL + mR) * 0.5f

                // דפנות כלי קפילרי
                strokeP.strokeWidth = 1.6f * d; strokeP.color = withAlpha(ink, 85)
                canvas.drawLine(mL, cy - 28f * d, mL, cy + 26f * d, strokeP)
                canvas.drawLine(mR, cy - 28f * d, mR, cy + 26f * d, strokeP)

                path.reset()
                path.moveTo(mL, yBase)
                path.quadTo(mCx, yBase + curveDepth + sin(phase * 1.5f) * 2.5f * d, mR, yBase)
                fillP.color = withAlpha(c3, 45 + (n * 50).toInt())
                canvas.drawPath(path, fillP)
                strokeP.strokeWidth = (2.2f + n * 1.8f) * d; strokeP.color = c3
                canvas.drawPath(path, strokeP)

                // חצי מתח פנים ← γ →
                val arrowLen = (14f + n * 24f) * d
                val ay = yBase - 12f * d
                strokeP.strokeWidth = 1.8f * d; strokeP.color = c1
                canvas.drawLine(mCx - 8f * d, ay, mCx - 8f * d - arrowLen, ay, strokeP)
                drawArrowLeft(canvas, mCx - 8f * d - arrowLen, ay, 4f * d, strokeP)
                canvas.drawLine(mCx + 8f * d, ay, mCx + 8f * d + arrowLen, ay, strokeP)
                drawArrowRight(canvas, mCx + 8f * d + arrowLen, ay, 4f * d, strokeP)
                textP.textSize = 10.5f * d; textP.color = c1
                canvas.drawText("γ", mCx, ay + 3.5f * d, textP)

                drawReferenceScaleBar(
                    canvas, rightL, rightR, cy + 8f * d,
                    title = "סולם מתח פנים (mN/m)",
                    valueBadge = "\u200Eγ = ${ProExtendedUnitConverters.formatSmartValue(mnm, 2)} mN/m",
                    progress = n,
                    ticks = listOf(0.25f to "אתנול", 0.48f to "מים", 0.86f to "כספית"),
                    accent = c3,
                    markerColor = c1,
                    ink = ink
                )
            }

            // 112: פרמיטיביות (ε) ופרמאביליות (µ) — איור פיזיקלי מסומן (+/−, E→, N/S, B) + סרגל ייחוס חומרים!
            112 -> {
                if (activeGroupIdx == 0) {
                    val epsR = abs(liveBaseVal / 8.854187817e-12)
                    val n = logNorm(epsR, 0.0, 4.0) // 1 (ריק) .. 10⁴
                    val plateHalf = (34f + n * 12f) * d
                    val lx = leftCx - plateHalf
                    val rx = leftCx + plateHalf
                    val topY = cy - 28f * d
                    val botY = cy + 28f * d

                    // תווך דיאלקטרי בין לוחות הקבל
                    rect.set(lx + 4f * d, topY + 2f * d, rx - 4f * d, botY - 2f * d)
                    fillP.color = withAlpha(c3, (26 + n * 74).toInt())
                    canvas.drawRoundRect(rect, 6f * d, 6f * d, fillP)

                    // לוחות קבל + סימוני קטבים (+ / −)
                    strokeP.strokeWidth = 3.2f * d; strokeP.color = c1
                    canvas.drawLine(lx, topY, lx, botY, strokeP)
                    strokeP.color = c2
                    canvas.drawLine(rx, topY, rx, botY, strokeP)

                    textP.textSize = 11f * d
                    textP.color = c1; canvas.drawText("+", lx, topY - 6f * d, textP)
                    textP.color = c2; canvas.drawText("−", rx, topY - 6f * d, textP)
                    subTextP.textSize = 9f * d; subTextP.color = withAlpha(ink, 190)
                    canvas.drawText("E →", leftCx, topY - 6f * d, subTextP)

                    // תמיד לפחות 3 קווי שדה עם חצי כיוון + חלקיקים נעים (לא נראה ריק גם ב-1 ε₀!)
                    val lines = (3 + floor(n * 4.99f).toInt()).coerceIn(3, 7)
                    val innerTop = topY + 9f * d
                    val innerBot = botY - 9f * d
                    val gap = (innerBot - innerTop) / (lines - 1).coerceAtLeast(1)
                    strokeP.strokeWidth = 1.3f * d
                    for (k in 0 until lines) {
                        val y = innerTop + k * gap
                        strokeP.color = withAlpha(c3, 130 + (n * 95).toInt())
                        canvas.drawLine(lx + 7f * d, y, rx - 8f * d, y, strokeP)
                        drawArrowRight(canvas, rx - 8f * d, y, 3.4f * d, strokeP)
                        val span = (rx - lx - 20f * d).coerceAtLeast(4f)
                        val px = lx + 8f * d + ((phase * (26f + n * 35f) * d + k * 13f * d + mantissaFrac(epsR) * span) % span)
                        fillP.color = c1
                        canvas.drawCircle(px, y, 2.3f * d, fillP)
                    }

                    // תווית חיה של εᵣ בתחתית התווך
                    val matName = when {
                        abs(epsR - 1.0) < 0.05 -> "ריק / אוויר"
                        epsR in 3.5..7.5 -> "זכוכית"
                        epsR in 70.0..90.0 -> "מים"
                        epsR > 500.0 -> "קרמיקה גבוהה"
                        else -> "תווך דיאלקטרי"
                    }
                    drawReferenceScaleBar(
                        canvas, rightL, rightR, cy + 8f * d,
                        title = "קנה מידה ε / ε₀ ($matName)",
                        valueBadge = "\u200Eεᵣ = ${ProExtendedUnitConverters.formatSmartValue(epsR, 3)}",
                        progress = n,
                        ticks = listOf(0.0f to "ריק 1", 0.17f to "זכוכית", 0.48f to "מים 80", 0.92f to "10⁴"),
                        accent = c1,
                        markerColor = c2,
                        ink = ink
                    )
                } else {
                    val muR = abs(liveBaseVal / 1.2566370614e-6)
                    val n = logNorm(muR, 0.0, 5.0) // 1 (ריק) .. 10⁵
                    val loops = (3 + floor(n * 3.99f).toInt()).coerceIn(3, 6)
                    val coreW = 46f * d; val coreH = 15f * d

                    for (k in 1..loops) {
                        val rx = coreW * 0.52f + k * (6.2f + n * 3.2f) * d
                        val ry = coreH * 0.65f + k * (4.4f + n * 2.2f) * d
                        rect.set(leftCx - rx, cy - ry, leftCx + rx, cy + ry)
                        strokeP.strokeWidth = 1.3f * d
                        strokeP.color = withAlpha(c3, (190 - k * 16).coerceIn(70, 220))
                        canvas.drawOval(rect, strokeP)
                    }

                    rect.set(leftCx - coreW * 0.5f, cy - coreH * 0.5f, leftCx + coreW * 0.5f, cy + coreH * 0.5f)
                    fillP.color = withAlpha(c1, 140)
                    canvas.drawRoundRect(rect, 4f * d, 4f * d, fillP)
                    strokeP.strokeWidth = 1.5f * d; strokeP.color = c2
                    canvas.drawRoundRect(rect, 4f * d, 4f * d, strokeP)

                    textP.textSize = 9.5f * d; textP.color = Color.WHITE
                    canvas.drawText("N", leftCx - coreW * 0.28f, cy + 3.5f * d, textP)
                    canvas.drawText("S", leftCx + coreW * 0.28f, cy + 3.5f * d, textP)
                    subTextP.textSize = 8.5f * d; subTextP.color = withAlpha(ink, 185)
                    canvas.drawText("שטף מגנטי B", leftCx, cy - 33f * d, subTextP)

                    val matName = when {
                        abs(muR - 1.0) < 0.05 -> "ריק"
                        muR in 100.0..1200.0 -> "ניקל"
                        muR in 2000.0..10000.0 -> "ברזל"
                        muR > 10000.0 -> "פרומגנט על"
                        else -> "חומר מגנטי"
                    }
                    drawReferenceScaleBar(
                        canvas, rightL, rightR, cy + 8f * d,
                        title = "קנה מידה µ / µ₀ ($matName)",
                        valueBadge = "\u200Eµᵣ = ${ProExtendedUnitConverters.formatSmartValue(muR, 3)}",
                        progress = n,
                        ticks = listOf(0.0f to "ריק 1", 0.55f to "ניקל", 0.74f to "ברזל", 0.96f to "10⁵"),
                        accent = c2,
                        markerColor = c1,
                        ink = ink
                    )
                }
            }

            // 113: מומנט אינרציה — גלגל תנופה מסומן (I, r, m) + סרגל אינרציה ואנרגיית סיבוב
            113 -> {
                val kgm2 = abs(liveBaseVal)
                val n = logNorm(kgm2, -4.0, 5.0)
                val r = (18f + n * 13f) * d
                val rimW = (2.0f + n * 4.2f) * d
                val weightR = (3.4f + n * 3.8f) * d
                val rotDeg = phase * (68f - n * 42f).coerceAtLeast(16f) + mantissaFrac(kgm2) * 360f

                val outerR = r + 6f * d
                rect.set(leftCx - outerR, cy - outerR, leftCx + outerR, cy + outerR)
                strokeP.strokeWidth = 2.2f * d; strokeP.color = withAlpha(c3, 165)
                canvas.drawArc(rect, -90f, 30f + n * 310f, false, strokeP)

                canvas.save()
                canvas.rotate(rotDeg, leftCx, cy)
                strokeP.strokeWidth = rimW; strokeP.color = c1
                canvas.drawCircle(leftCx, cy, r, strokeP)
                strokeP.strokeWidth = 1.6f * d
                canvas.drawLine(leftCx - r, cy, leftCx + r, cy, strokeP)
                canvas.drawLine(leftCx, cy - r, leftCx, cy + r, strokeP)
                fillP.color = c2
                canvas.drawCircle(leftCx - r, cy, weightR, fillP)
                canvas.drawCircle(leftCx + r, cy, weightR, fillP)
                canvas.drawCircle(leftCx, cy - r, weightR, fillP)
                canvas.drawCircle(leftCx, cy + r, weightR, fillP)
                canvas.restore()

                subTextP.textSize = 8.5f * d; subTextP.color = withAlpha(ink, 195)
                canvas.drawText("I = ∑mr²", leftCx, cy - r - 9f * d, subTextP)

                val omega1000 = 1000.0 * 2.0 * PI / 60.0
                val eRot = 0.5 * kgm2 * omega1000 * omega1000
                drawReferenceScaleBar(
                    canvas, rightL, rightR, cy + 8f * d,
                    title = "אינרציה סיבובית (kg·m²)",
                    valueBadge = "\u200EE(1k RPM) ≈ ${ProExtendedUnitConverters.formatSmartValue(eRot, 2)} J",
                    progress = n,
                    ticks = listOf(0.05f to "10⁻⁴", 0.44f to "1", 0.72f to "10²", 0.95f to "10⁵"),
                    accent = c1,
                    markerColor = c2,
                    ink = ink
                )
            }

            // 114: צפיפות חום (J/m²) וקיבול חום (J/K) — תרשים מסומן + סרגל ייחוס לכל תת-קטגוריה
            114 -> {
                if (activeGroupIdx == 0) {
                    val jm2 = abs(liveBaseVal)
                    val n = logNorm(jm2, 1.0, 8.0)
                    val heatCol = when {
                        n < 0.35f -> pal.warning
                        n < 0.70f -> Color.parseColor("#FF7043")
                        else -> Color.parseColor("#EF5350")
                    }
                    val pL = w * 0.09f; val pR = w * 0.47f
                    strokeP.strokeWidth = (3f + n * 2.5f) * d; strokeP.color = heatCol
                    canvas.drawLine(pL, cy + 25f * d, pR, cy + 25f * d, strokeP)

                    val waves = (3 + floor(n * 4.99f).toInt()).coerceIn(3, 7)
                    val plumeH = (22f + n * 28f) * d
                    strokeP.strokeWidth = 1.6f * d
                    for (i in 0 until waves) {
                        val bx = pL + 6f * d + ((pR - pL - 12f * d) * i / (waves - 1).coerceAtLeast(1))
                        path.reset()
                        var y = cy + 20f * d
                        val topY = cy + 20f * d - plumeH
                        var first = true
                        while (y >= topY) {
                            val x = bx + sin(y * 0.16f + phase * (1.8f + n * 1.8f) + i) * 3.8f * d
                            if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
                            y -= 4f * d
                        }
                        strokeP.color = withAlpha(if (i % 2 == 0) heatCol else c1, 175)
                        canvas.drawPath(path, strokeP)
                        drawArrowUp(canvas, bx, topY, 3.2f * d, strokeP)
                    }
                    subTextP.textSize = 8.5f * d; subTextP.color = withAlpha(ink, 185)
                    canvas.drawText("פליטת חום Q/A", (pL + pR) * 0.5f, cy + 37f * d, subTextP)

                    drawReferenceScaleBar(
                        canvas, rightL, rightR, cy + 8f * d,
                        title = "צפיפות אנרגיה (J/m²)",
                        valueBadge = "\u200E${ProExtendedUnitConverters.formatSmartValue(jm2 / 41_840.0, 2)} Langley",
                        progress = n,
                        ticks = listOf(0.05f to "10", 0.35f to "1k", 0.65f to "10⁵", 0.95f to "10⁸"),
                        accent = heatCol,
                        markerColor = c1,
                        ink = ink
                    )
                } else {
                    val jk = abs(liveBaseVal)
                    val n = logNorm(jk, 0.0, 7.0)
                    val tankW = 66f * d; val tankH = 54f * d
                    rect.set(leftCx - tankW * 0.5f, cy - tankH * 0.5f, leftCx + tankW * 0.5f, cy + tankH * 0.5f)
                    strokeP.strokeWidth = 2f * d; strokeP.color = c1
                    canvas.drawRoundRect(rect, 9f * d, 9f * d, strokeP)

                    val fillH = (tankH - 6f * d) * n.coerceAtLeast(0.10f)
                    val fillRect = RectF(rect.left + 3f * d, rect.bottom - 3f * d - fillH, rect.right - 3f * d, rect.bottom - 3f * d)
                    fillP.color = withAlpha(pal.warning, 145 + (n * 80).toInt())
                    canvas.drawRoundRect(fillRect, 6f * d, 6f * d, fillP)

                    textP.textSize = 9.5f * d; textP.color = ink
                    canvas.drawText("C = Q/ΔT", leftCx, rect.top - 6f * d, textP)

                    val liters = ProExtendedUnitConverters.formatSmartValue(jk / 4184.0, 2)
                    drawReferenceScaleBar(
                        canvas, rightL, rightR, cy + 8f * d,
                        title = "קיבול חום תרמי (J/K)",
                        valueBadge = "≈ $liters ליטר מים",
                        progress = n,
                        ticks = listOf(0.05f to "1", 0.45f to "1k", 0.68f to "100k", 0.95f to "10⁷"),
                        accent = pal.warning,
                        markerColor = c1,
                        ink = ink
                    )
                }
            }

            // 115: נפח סגולי וצפיפות — תא מולקולות עם קו מים מסומן + סרגל צפיפות (אוויר / מים / ברזל)
            115 -> {
                val rho = if (liveBaseVal > 0.0) 1.0 / liveBaseVal else 0.0
                val n = logNorm(rho, 0.0, 4.4) // 1 (אוויר) .. 25000 (אוסמיום)
                val boxW = 86f * d; val boxH = 54f * d
                rect.set(leftCx - boxW * 0.5f, cy - boxH * 0.5f, leftCx + boxW * 0.5f, cy + boxH * 0.5f)
                strokeP.strokeWidth = 1.8f * d; strokeP.color = c1
                canvas.drawRoundRect(rect, 9f * d, 9f * d, strokeP)

                // קו ייחוס מים מקווקו בתוך התא
                dashedP.color = withAlpha(c3, 180)
                canvas.drawLine(rect.left + 4f * d, cy, rect.right - 4f * d, cy, dashedP)

                val count = if (rho <= 0.0) 3 else (3 + floor(n * 22f).toInt()).coerceIn(3, 25)
                val yBias = if (rho in 0.0001..999.9) -9f * d else if (rho > 1000.1) 9f * d else 0f
                for (k in 0 until count) {
                    val px = leftCx + sin(phase * 1.1f + k * 2.1f + mantissaFrac(rho) * 6f) * 34f * d
                    val py = (cy + yBias + cos(phase * 1.3f + k * 1.7f) * 12f * d).coerceIn(rect.top + 5f * d, rect.bottom - 5f * d)
                    fillP.color = if (k % 2 == 0) c1 else c3
                    canvas.drawCircle(px, py, 2.7f * d, fillP)
                }
                subTextP.textSize = 8f * d; subTextP.color = withAlpha(ink, 185)
                canvas.drawText("קו ייחוס: מים 1000", leftCx, rect.top - 5f * d, subTextP)

                val floatLbl = when {
                    rho <= 0.0 -> "—"
                    rho < 999.5 -> "צף במים"
                    rho > 1000.5 -> "שוקע במים"
                    else -> "צפיפות המים"
                }
                drawReferenceScaleBar(
                    canvas, rightL, rightR, cy + 8f * d,
                    title = "סולם צפיפות ρ ($floatLbl)",
                    valueBadge = "\u200Eρ = ${ProExtendedUnitConverters.formatSmartValue(rho, 2)} kg/m³",
                    progress = n,
                    ticks = listOf(0.02f to "אוויר", 0.68f to "מים", 0.89f to "ברזל"),
                    accent = c3,
                    markerColor = c1,
                    ink = ink
                )
            }

            // 116: מהירות ותאוצה זוויתית — טכומטר עם שנתות וסימון ω/α + סרגל RPM
            116 -> {
                val n = logNorm(liveBaseVal, -1.0, 6.0)
                val r = 34f * d
                val sweepMax = 240f
                val startAng = 150f
                val activeSweep = (8f + n * (sweepMax - 8f)).coerceIn(8f, sweepMax)

                rect.set(leftCx - r, cy - r, leftCx + r, cy + r)
                strokeP.strokeWidth = 5f * d; strokeP.color = withAlpha(ink, 32)
                canvas.drawArc(rect, startAng, sweepMax, false, strokeP)

                strokeP.color = if (activeGroupIdx == 0) c1 else c2
                canvas.drawArc(rect, startAng, activeSweep, false, strokeP)

                // שנתות על הטכומטר
                strokeP.strokeWidth = 1.3f * d; strokeP.color = withAlpha(ink, 120)
                for (t in 0..4) {
                    val a = Math.toRadians((startAng + t * (sweepMax / 4f)).toDouble())
                    canvas.drawLine(
                        leftCx + (cos(a) * (r - 6f * d)).toFloat(), cy + (sin(a) * (r - 6f * d)).toFloat(),
                        leftCx + (cos(a) * (r + 1f * d)).toFloat(), cy + (sin(a) * (r + 1f * d)).toFloat(),
                        strokeP
                    )
                }

                val needleDeg = startAng + activeSweep + sin(phase * (2f + n * 3f)) * 2.0
                val needleRad = Math.toRadians(needleDeg)
                strokeP.color = c2; strokeP.strokeWidth = 2.6f * d
                canvas.drawLine(leftCx, cy, leftCx + (cos(needleRad) * r * 0.80f).toFloat(), cy + (sin(needleRad) * r * 0.80f).toFloat(), strokeP)
                fillP.color = c1
                canvas.drawCircle(leftCx, cy, 4.2f * d, fillP)

                textP.textSize = 10f * d; textP.color = ink
                canvas.drawText(if (activeGroupIdx == 0) "ω" else "α", leftCx, cy + 20f * d, textP)

                val badge = if (activeGroupIdx == 0) {
                    "\u200E${ProExtendedUnitConverters.formatSmartValue(abs(liveBaseVal) * 60.0 / (2.0 * PI), 1)} RPM"
                } else {
                    "\u200E${ProExtendedUnitConverters.formatSmartValue(abs(liveBaseVal), 2)} rad/s²"
                }
                drawReferenceScaleBar(
                    canvas, rightL, rightR, cy + 8f * d,
                    title = if (activeGroupIdx == 0) "מהירות סיבוב (ω)" else "תאוצה סיבובית (α)",
                    valueBadge = badge,
                    progress = n,
                    ticks = listOf(0.05f to "10⁻¹", 0.35f to "10¹", 0.65f to "10³", 0.95f to "10⁶"),
                    accent = if (activeGroupIdx == 0) c1 else c2,
                    markerColor = c3,
                    ink = ink
                )
            }

            // 117: מטען (Q), צפיפות זרם (J) ומוליכות (G) — 3 תצוגות מדעיות עם סרגלי ייחוס ובטיחות
            else -> {
                when (activeGroupIdx) {
                    0 -> { // מטען Q
                        val coulombs = abs(liveBaseVal)
                        val n = logNorm(coulombs, -2.0, 7.0)
                        val bw = 72f * d; val bh = 38f * d
                        rect.set(leftCx - bw * 0.5f, cy - bh * 0.5f, leftCx + bw * 0.5f, cy + bh * 0.5f)
                        strokeP.strokeWidth = 2.0f * d; strokeP.color = c1
                        canvas.drawRoundRect(rect, 8f * d, 8f * d, strokeP)
                        fillP.color = c1
                        canvas.drawRoundRect(RectF(rect.right, cy - 8f * d, rect.right + 5f * d, cy + 8f * d), 2.5f * d, 2.5f * d, fillP)

                        val fillW = (bw - 8f * d) * n.coerceAtLeast(0.08f)
                        val innerRect = RectF(rect.left + 4f * d, rect.top + 4f * d, rect.left + 4f * d + fillW, rect.bottom - 4f * d)
                        fillP.color = withAlpha(pal.positive, 125 + (n * 90).toInt())
                        canvas.drawRoundRect(innerRect, 5f * d, 5f * d, fillP)

                        textP.textSize = 10f * d; textP.color = ink
                        canvas.drawText("−", rect.left + 9f * d, rect.top - 5f * d, textP)
                        canvas.drawText("+", rect.right - 9f * d, rect.top - 5f * d, textP)
                        subTextP.textSize = 8.5f * d; subTextP.color = withAlpha(ink, 190)
                        canvas.drawText("מטען Q", leftCx, rect.bottom + 12f * d, subTextP)

                        val wh = ProExtendedUnitConverters.formatSmartValue((coulombs / 3600.0) * 3.7, 2)
                        drawReferenceScaleBar(
                            canvas, rightL, rightR, cy + 8f * d,
                            title = "אנרגיית סוללה (3.7V)",
                            valueBadge = "\u200E≈ $wh Wh",
                            progress = n,
                            ticks = listOf(0.05f to "10mC", 0.35f to "10C", 0.60f to "1Ah", 0.92f to "10⁷C"),
                            accent = pal.positive,
                            markerColor = c2,
                            ink = ink
                        )
                    }
                    1 -> { // צפיפות זרם J
                        val amm2 = abs(liveBaseVal) / 1e6
                        val n = (amm2 / 10.0).toFloat().coerceIn(0f, 1f)
                        val (wireCol, safetyLbl) = when {
                            amm2 <= 4.0 -> pal.positive to "תקין לנחושת"
                            amm2 <= 7.0 -> pal.warning to "עומס גבוה"
                            else -> Color.parseColor("#EF5350") to "סכנת חימום!"
                        }
                        val r = (22f + n * 9f) * d
                        fillP.color = withAlpha(wireCol, 45 + (n * 65).toInt())
                        canvas.drawCircle(leftCx, cy, r, fillP)
                        strokeP.strokeWidth = 2.8f * d; strokeP.color = wireCol
                        canvas.drawCircle(leftCx, cy, r, strokeP)

                        val dots = (4 + floor(n * 18f).toInt()).coerceIn(4, 22)
                        fillP.color = wireCol
                        for (k in 0 until dots) {
                            val a = phase * (1.2f + n * 2.2f) + k * (2f * PI.toFloat() / dots) + mantissaFrac(amm2) * 6f
                            val rr = r * (0.28f + ((k % 3) * 0.23f))
                            canvas.drawCircle(leftCx + cos(a) * rr, cy + sin(a) * rr, 2.3f * d, fillP)
                        }
                        subTextP.textSize = 8.5f * d; subTextP.color = withAlpha(ink, 190)
                        canvas.drawText("חתך מוליך J=I/A", leftCx, cy - r - 6f * d, subTextP)

                        drawReferenceScaleBar(
                            canvas, rightL, rightR, cy + 8f * d,
                            title = "בטיחות מוליך: $safetyLbl",
                            valueBadge = "\u200E${ProExtendedUnitConverters.formatSmartValue(amm2, 2)} A/mm²",
                            progress = n,
                            ticks = listOf(0.0f to "0", 0.40f to "4 תקין", 0.70f to "7", 1.0f to "10+"),
                            accent = wireCol,
                            markerColor = ink,
                            ink = ink
                        )
                    }
                    else -> { // מוליכות G
                        val siemens = abs(liveBaseVal)
                        val n = logNorm(siemens, -6.0, 4.0)
                        val pipeH = (14f + n * 28f) * d
                        val pL = w * 0.08f; val pR = w * 0.49f
                        rect.set(pL, cy - pipeH * 0.5f, pR, cy + pipeH * 0.5f)
                        fillP.color = withAlpha(c1, 35 + (n * 65).toInt())
                        canvas.drawRoundRect(rect, pipeH * 0.45f, pipeH * 0.45f, fillP)
                        strokeP.strokeWidth = 1.8f * d; strokeP.color = c1
                        canvas.drawRoundRect(rect, pipeH * 0.45f, pipeH * 0.45f, strokeP)

                        val carriers = (3 + floor(n * 9f).toInt()).coerceIn(3, 12)
                        val span = (rect.width() - 16f * d).coerceAtLeast(8f)
                        fillP.color = c2
                        for (k in 0 until carriers) {
                            val px = rect.left + 8f * d + ((phase * (32f + n * 48f) * d + k * (span / carriers) + mantissaFrac(siemens) * span) % span)
                            val py = cy + sin(k * 2.1f) * (pipeH * 0.26f)
                            canvas.drawCircle(px, py, 2.7f * d, fillP)
                        }
                        subTextP.textSize = 8.5f * d; subTextP.color = withAlpha(ink, 190)
                        canvas.drawText("תעלת מוליכות G = 1/R", (pL + pR) * 0.5f, rect.top - 6f * d, subTextP)

                        val ohm = if (siemens > 0.0) 1.0 / siemens else 0.0
                        drawReferenceScaleBar(
                            canvas, rightL, rightR, cy + 8f * d,
                            title = "מוליכות (Siemens)",
                            valueBadge = "\u200ER ≈ ${ProExtendedUnitConverters.formatSmartValue(ohm, 3)} Ω",
                            progress = n,
                            ticks = listOf(0.05f to "1µS", 0.30f to "1mS", 0.60f to "1S", 0.95f to "10kS"),
                            accent = c1,
                            markerColor = c2,
                            ink = ink
                        )
                    }
                }
            }
        }

        // שורת תוצאה חיה בתחתית הקנבס
        strokeP.clearShadowLayer()
        textP.textAlign = Paint.Align.CENTER
        val inStr = ProExtendedUnitConverters.formatSmartValue(liveInputVal, 4)
        val outStr = ProExtendedUnitConverters.formatSmartValue(liveResultVal, 5)
        val lhs = ProExtendedUnitConverters.formatValueWithUnit(inStr, liveFromSym)
        val rhs = ProExtendedUnitConverters.formatValueWithUnit(outStr, liveToSym)
        val summary = "\u200E$lhs  =  $rhs"
        textP.textSize = 12.2f * d
        textP.color = ink
        canvas.drawText(summary, cx, h - 10f * d, textP)
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

    var activeGroupIdx = 0
    fun currentGroup(): ExtUnitGroup = spec.groups[activeGroupIdx.coerceIn(0, spec.groups.lastIndex)]

    // 1. כרטיס כותרת + נוסחה דינמית לפי תת-הקטגוריה הפעילה
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
            bottomMargin = dp(10f)
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
    val formulaTv = TextView(this).apply {
        text = "\u200E${currentGroup().formulaHint}"
        setTextColor(if (isMin) MinimalistTheme.TEXT_CHARCOAL else accentCol)
        textSize = 12.2f
        typeface = Typeface.MONOSPACE
        gravity = Gravity.RIGHT
    }
    headerCard.addView(formulaTv)
    root.addView(headerCard)

    // 2. סרגל תת-קטגוריות (מופיע בכלים משולבים: 112, 114, 116, 117)
    val subGroupTabsRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        visibility = if (spec.groups.size > 1) View.VISIBLE else View.GONE
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(10f)
        }
    }
    root.addView(subGroupTabsRow)

    // 3. קנבס מדעי אינטראקטיבי עם צירים, תוויות וקנה מידה חי
    val heroCanvas = ExtUnitHeroCanvasView(this, spec.tabIndex, currentGroup()).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(156f)
        ).apply { bottomMargin = dp(12f) }
    }
    root.addView(heroCanvas)

    var fromIdx = currentGroup().defaultFrom.coerceIn(0, currentGroup().units.lastIndex)
    var toIdx = currentGroup().defaultTo.coerceIn(0, currentGroup().units.lastIndex)
    var isProgrammaticText = false

    // 4. שורת דוגמאות מהירות (Presets)
    val presetScroll = HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(10f)
        }
    }
    val presetRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    presetScroll.addView(presetRow)
    root.addView(presetScroll)

    // 5. תיבת קלט ובחירת יחידות
    val initFromU = currentGroup().units[fromIdx]
    val initHintLabel = if (initFromU.symbol.isBlank()) initFromU.name.substringBefore(" —") else initFromU.symbol
    val inputBox = createInputBox("הזן ערך להמרה ($initHintLabel)").apply {
        tag = "HAS_CUSTOM_LIVE_WATCHER"
        setText(ProExtendedUnitConverters.formatPlainInput(currentGroup().defaultValue))
    }
    root.addView(inputBox)

    lateinit var fromBtn: TextView
    lateinit var toBtn: TextView

    val resultCard = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
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

    val mainResultTv = createResultTextView().apply {
        background = null
        setPadding(0, 0, 0, 0)
        textSize = 16.5f
        setLineSpacing(dp(3f).toFloat(), 1.06f)
    }
    resultCard.addView(mainResultTv)
    resultCard.setOnClickListener {
        if (mainResultTv.text.isNotEmpty()) {
            feedback(it)
            copyToClipboard(mainResultTv.text.toString())
        }
    }

    val allUnitsContainer = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    fun parseNumericOrExpression(raw: String): Double {
        val cleaned = raw.trim().replace(",", ".")
        if (cleaned.isEmpty()) return 0.0
        cleaned.toDoubleOrNull()?.let { return it }
        return try {
            Utils.evalMathExpression(cleaned)
        } catch (_: Throwable) {
            0.0
        }
    }

    fun recalculateAll() {
        val grp = currentGroup()
        fromIdx = fromIdx.coerceIn(0, grp.units.lastIndex)
        toIdx = toIdx.coerceIn(0, grp.units.lastIndex)
        val fromU = grp.units[fromIdx]
        val toU = grp.units[toIdx]

        val hintLbl = if (fromU.symbol.isBlank()) fromU.name.substringBefore(" —") else fromU.symbol
        formulaTv.text = "\u200E${grp.formulaHint}"
        fromBtn.text = "מיחידה: ${ProExtendedUnitConverters.formatUnitDisplayLabel(fromU)}"
        toBtn.text = "ליחידה: ${ProExtendedUnitConverters.formatUnitDisplayLabel(toU)}"
        inputBox.hint = "\u200Fהזן ערך להמרה ($hintLbl)"

        val rawVal = parseNumericOrExpression(inputBox.text.toString())
        val baseVal = fromU.toBase(rawVal)
        val resVal = toU.fromBase(baseVal)

        heroCanvas.updateLiveData(rawVal, baseVal, resVal, fromU.symbol, toU.symbol, activeGroupIdx)

        val formattedIn = ProExtendedUnitConverters.formatSmartValue(rawVal, calcPrecision)
        val formattedOut = ProExtendedUnitConverters.formatSmartValue(resVal, calcPrecision)
        val inWithSym = ProExtendedUnitConverters.formatValueWithUnit(formattedIn, fromU.symbol)
        val outWithSym = ProExtendedUnitConverters.formatValueWithUnit(formattedOut, toU.symbol)
        val outHeadline = if (toU.symbol.isBlank()) {
            "${BidiFmt.ltr(formattedOut)} (${toU.name.substringBefore(" —")})"
        } else {
            BidiFmt.ltr(outWithSym)
        }
        val insight = grp.insightProvider(rawVal, fromU, toU, baseVal, resVal)

        mainResultTv.text = BidiFmt.rtlParagraph(
            "תוצאה: $outHeadline",
            "המרה: ${BidiFmt.ltr("$inWithSym  =  $outWithSym")}",
            "",
            "תובנה מקצועית:",
            insight
        )

        allUnitsContainer.removeAllViews()
        allUnitsContainer.addView(TextView(this).apply {
            text = "המרה מקבילה לכל ${grp.units.size} היחידות (לחץ להעתקה):"
            setTextColor(textSecondary)
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(dp(4f), dp(4f), dp(4f), dp(8f))
        })

        for ((idx, unit) in grp.units.withIndex()) {
            val convVal = unit.fromBase(baseVal)
            val convStr = ProExtendedUnitConverters.formatSmartValue(convVal, calcPrecision.coerceAtMost(6))
            val convWithUnit = ProExtendedUnitConverters.formatValueWithUnit(convStr, unit.symbol)
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
                    copyToClipboard(convWithUnit)
                }
            }

            val valTv = TextView(this).apply {
                text = "\u200E$convWithUnit"
                textDirection = View.TEXT_DIRECTION_LTR
                setTextColor(MinimalistTheme.ensureReadableTextOnBg(if (isTarget && !isMin) accentCol else textPrimary, rowBg, 2.2))
                textSize = 14.5f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.LEFT
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val nameTv = TextView(this).apply {
                text = "\u200F${ProExtendedUnitConverters.formatUnitDisplayLabel(unit)}"
                setTextColor(MinimalistTheme.ensureReadableTextOnBg(textSecondary, rowBg, 2.0))
                textSize = 13.5f
                gravity = Gravity.RIGHT
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.15f)
            }
            rowCard.addView(valTv)
            rowCard.addView(nameTv)
            allUnitsContainer.addView(rowCard)
        }
    }

    fun rebuildPresetsBar() {
        presetRow.removeAllViews()
        val grp = currentGroup()
        for (p in grp.presets) {
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
                    fromIdx = p.fromIdx.coerceIn(0, grp.units.lastIndex)
                    toIdx = p.toIdx.coerceIn(0, grp.units.lastIndex)
                    isProgrammaticText = true
                    inputBox.setText(ProExtendedUnitConverters.formatPlainInput(p.value))
                    isProgrammaticText = false
                    recalculateAll()
                }
            }
            presetRow.addView(chip)
        }
    }

    fun rebuildSubGroupTabs() {
        if (spec.groups.size <= 1) return
        subGroupTabsRow.removeAllViews()
        for ((idx, grp) in spec.groups.withIndex()) {
            val isSel = (idx == activeGroupIdx)
            val tabBg = when {
                isSel -> accentCol
                isMin -> MinimalistTheme.NEUTRAL_SECONDARY_BG
                else -> Color.argb(38, 255, 255, 255)
            }
            val tabTxt = MinimalistTheme.ensureReadableTextOnBg(
                if (isSel && !isMin && !isAuroraMode) Color.parseColor("#08090C") else textPrimary,
                tabBg,
                2.3
            )
            val tabChip = TextView(this).apply {
                tag = "PRO_STAR_CHIP"
                text = grp.label
                setTextColor(tabTxt)
                textSize = 13.5f
                typeface = if (isSel) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                gravity = Gravity.CENTER
                setPadding(dp(10f), dp(10f), dp(10f), dp(10f))
                background = GradientDrawable().apply {
                    cornerRadius = dp(999f).toFloat()
                    setColor(tabBg)
                    setStroke(dp(if (isSel) 1.5f else 1f), if (isSel) accentCol else Color.argb(55, 128, 128, 128))
                }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dp(3f), 0, dp(3f), 0)
                }
                setOnClickListener {
                    if (activeGroupIdx == idx) return@setOnClickListener
                    feedback(it)
                    activeGroupIdx = idx
                    val nextGrp = currentGroup()
                    fromIdx = nextGrp.defaultFrom.coerceIn(0, nextGrp.units.lastIndex)
                    toIdx = nextGrp.defaultTo.coerceIn(0, nextGrp.units.lastIndex)
                    isProgrammaticText = true
                    inputBox.setText(ProExtendedUnitConverters.formatPlainInput(nextGrp.defaultValue))
                    isProgrammaticText = false
                    rebuildSubGroupTabs()
                    rebuildPresetsBar()
                    recalculateAll()
                }
            }
            subGroupTabsRow.addView(tabChip)
        }
    }

    fromBtn = createActionBtn("מיחידה: ${ProExtendedUnitConverters.formatUnitDisplayLabel(currentGroup().units[fromIdx])}", false) {
        val labels = currentGroup().units.map { ProExtendedUnitConverters.formatUnitDisplayLabel(it) }
        showItemPicker("בחר יחידת מקור (${currentGroup().label})", labels) { picked ->
            val idx = labels.indexOf(picked)
            if (idx >= 0) { fromIdx = idx; recalculateAll() }
        }
    }

    val swapBtn = createActionBtn("⇅ החלף כיוון", false) {
        val tmp = fromIdx; fromIdx = toIdx; toIdx = tmp
        recalculateAll()
    }

    toBtn = createActionBtn("ליחידה: ${ProExtendedUnitConverters.formatUnitDisplayLabel(currentGroup().units[toIdx])}", false) {
        val labels = currentGroup().units.map { ProExtendedUnitConverters.formatUnitDisplayLabel(it) }
        showItemPicker("בחר יחידת יעד (${currentGroup().label})", labels) { picked ->
            val idx = labels.indexOf(picked)
            if (idx >= 0) { toIdx = idx; recalculateAll() }
        }
    }

    root.addView(fromBtn)
    root.addView(swapBtn)
    root.addView(toBtn)
    root.addView(resultCard)
    root.addView(allUnitsContainer)

    inputBox.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            if (!isProgrammaticText) recalculateAll()
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    rebuildSubGroupTabs()
    rebuildPresetsBar()
    recalculateAll()
    return scroll
}

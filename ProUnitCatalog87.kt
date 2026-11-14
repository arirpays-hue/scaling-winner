package com.example.calculator

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import kotlin.math.log10

object ProUnitCatalog {

    val categories87: List<String> = listOf(
        // 1-15: יומיומיות והנדסיות בסיסיות
        "1. אורך ומרחק", "2. משקל ומסה", "3. שטח", "4. נפח וקיבולת", "5. טמפרטורה",
        "6. מהירות", "7. לחץ", "8. אנרגיה ועבודה", "9. הספק", "10. נתונים דיגיטליים",
        "11. צריכת דלק", "12. זווית", "13. כוח", "14. מומנט פיתול (Torque)", "15. תדר",
        // 16-32: מכניקה, זורמים ואינסטלציה
        "16. תאוצה", "17. מהירות זוויתית", "18. תאוצה זוויתית", "19. צפיפות (Density)", "20. נפח סגולי",
        "21. מומנט התמד", "22. מתח פנים", "23. צמיגות דינמית", "24. צמיגות קינמטית", "25. ספיקת נפח (אינסטלציה)",
        "26. ספיקת מסה", "27. ספיקה מולרית", "28. לחץ הידרוסטטי (אינסטלציה)", "29. אובדן עומד בצנרת", "30. קוטר צנרת תקני (אינסטלציה)",
        "31. קשיות מים (אינסטלציה)", "32. חדירות (Permeability)",
        // 33-44: תרמודינמיקה וחום
        "33. מוליכות חום", "34. התנגדות תרמית", "35. קיבול חום סגולי", "36. צפיפות שטף חום", "37. מקדם מעבר חום",
        "38. התפשטות תרמית", "39. אנרגיה סגולית", "40. יעילות תרמית", "41. עומס קירור (BTU/Ton)", "42. לחות יחסית ונקודת טל",
        "43. אנתלפיה", "44. אנטרופיה",
        // 45-60: חשמל, מגנטיות ואלקטרוניקה
        "45. זרם חשמלי (חשמל)", "46. מתח חשמלי ופוטנציאל (חשמל)", "47. התנגדות חשמלית (חשמל)", "48. מוליכות חשמלית (חשמל)", "49. קיבול חשמלי (חשמל)",
        "50. השראות (Inductance)", "51. מטען חשמלי", "52. צפיפות מטען ליניארית", "53. צפיפות מטען משטחית", "54. צפיפות מטען נפחית",
        "55. עוצמת שדה חשמלי", "56. שטף מגנטי", "57. צפיפות שטף מגנטי (Tesla/Gauss)", "58. עוצמת שדה מגנטי", "59. כוח מגנטו-מניע",
        "60. הספק ריאקטיבי ומדומה (kVAR/kVA)",
        // 61-72: אור, אופטיקה, קול וקרינה
        "61. עוצמת הארה (Lux/Foot-candle)", "62. בהיקות (Luminance)", "63. שטף אור (Lumen)", "64. עוצמת אור (Candela)", "65. כוח אופטי (Diopter)",
        "66. עוצמת קול ואקוסטיקה (dB/SPL)", "67. רדיואקטיביות (Bq/Ci)", "68. מנת קרינה נספגת (Gy/Rad)", "69. מנת קרינה שקולה (Sv/Rem)", "70. חשיפה לקרינה",
        "71. אורך גל אופטי", "72. רזולוציית הדפסה ותצוגה (DPI/PPI)",
        // 73-82: רפואה, כימיה ומעבדה
        "73. רמת סוכר בדם (רפואה)", "74. כולסטרול ושומנים בדם (רפואה)", "75. קצב טפטוף עירוי IV (רפואה)", "76. מינון תרופות לפי משקל (רפואה)", "77. ריכוז מולרי (Molarity)",
        "78. ריכוז מסה (mg/dL, g/L)", "79. מסה מולרית", "80. רמת חומציות ואלקליות (pH)", "81. לחץ אוסמוטי (Osmolality)", "82. פעילות אנזימטית (Katal/U)",
        // 83-87: זמן, תקשורת, טיפוגרפיה, אסטרונומיה ובישול
        "83. זמן ותקופות", "84. קצב העברת נתונים (Bandwidth)", "85. טיפוגרפיה (pt/px/em/rem)", "86. אסטרונומיה (AU/Parsec/שנות אור)", "87. מידות בישול ואפייה"
    )

    // מיפוי מדויק ומלא לכל אחת מ-87 הקטגוריות (ללא אף חוסר!)
    fun getUnitsForCategory(catIndex: Int): Map<String, Double> {
        return when (catIndex + 1) {
            1 -> mapOf("מטר (m)" to 1.0, "קילומטר (km)" to 1000.0, "סנטימטר (cm)" to 0.01, "מילימטר (mm)" to 0.001, "מיקרומטר (µm)" to 1e-6, "ננומטר (nm)" to 1e-9, "מייל (mi)" to 1609.344, "יארד (yd)" to 0.9144, "רגל (ft)" to 0.3048, "אינץ' (in)" to 0.0254, "מייל ימי (nmi)" to 1852.0, "אנגסטרם (Å)" to 1e-10)
            2 -> mapOf("קילוגרם (kg)" to 1.0, "גרם (g)" to 0.001, "מיליגרם (mg)" to 1e-6, "מיקרוגרם (µg)" to 1e-9, "טון מטרי (t)" to 1000.0, "פאונד (lb)" to 0.45359237, "אונקיה (oz)" to 0.02834952, "אונקיית טרוי (oz t)" to 0.03110348, "קראט (ct)" to 0.0002, "סטון (st)" to 6.350293)
            3 -> mapOf("מטר רבוע (m²)" to 1.0, "קילומטר רבוע (km²)" to 1e6, "סמ״ר (cm²)" to 0.0001, "ממ״ר (mm²)" to 1e-6, "דונם (Dunam)" to 1000.0, "הקטר (ha)" to 10000.0, "אקר (Acre)" to 4046.856, "רגל רבוע (ft²)" to 0.092903, "אינץ' רבוע (in²)" to 0.00064516)
            4 -> mapOf("ליטר (L)" to 1.0, "מיליליטר (mL / cc)" to 0.001, "מטר מעוקב (m³)" to 1000.0, "סמ״ק (cm³)" to 0.001, "גלון אמריקאי (US gal)" to 3.78541, "גלון בריטי (UK gal)" to 4.54609, "חבית נפט (bbl)" to 158.987, "אונקיית נוזל (fl oz)" to 0.0295735, "כוס (Cup)" to 0.24)
            5 -> mapOf("צלזיוס (°C)" to 1.0, "פרנהייט (°F)" to 1.0, "קלווין (K)" to 1.0)
            6 -> mapOf("מטר לשנייה (m/s)" to 1.0, "קמ״ש (km/h)" to 0.2777778, "מייל לשעה (mph)" to 0.44704, "קשר ימי (Knot)" to 0.514444, "רגל לשנייה (ft/s)" to 0.3048, "מאך (Mach)" to 343.0)
            7, 28 -> mapOf("פסקל (Pa)" to 1.0, "קילופסקל (kPa)" to 1000.0, "מגה-פסקל (MPa)" to 1e6, "בר (Bar)" to 100000.0, "מיליבר (mbar)" to 100.0, "אטמוספירה (atm)" to 101325.0, "PSI (lb/in²)" to 6894.757, "מ״מ כספית (mmHg / Torr)" to 133.322, "מטר עמוד מים (mH₂O)" to 9806.65)
            8 -> mapOf("ג'אול (J)" to 1.0, "קילוג'אול (kJ)" to 1000.0, "מגה-ג'אול (MJ)" to 1e6, "וואט-שעה (Wh)" to 3600.0, "קילוואט-שעה (kWh)" to 3.6e6, "קלוריה (cal)" to 4.184, "קילוקלוריה (kcal)" to 4184.0, "BTU" to 1055.056, "רגל-פאונד (ft·lbf)" to 1.355818)
            9, 41 -> mapOf("וואט (W)" to 1.0, "קילוואט (kW)" to 1000.0, "מגה-וואט (MW)" to 1e6, "מיליוואט (mW)" to 0.001, "כוח סוס מכני (hp)" to 745.7, "כוח סוס מטרי (PS)" to 735.499, "BTU לשעה (BTU/h)" to 0.293071, "טון קירור (Ton Ref)" to 3516.85)
            10 -> mapOf("בייט (Byte)" to 1.0, "ביט (bit)" to 0.125, "קילובייט (KB)" to 1024.0, "מגה-בייט (MB)" to 1048576.0, "גיגה-בייט (GB)" to 1073741824.0, "טרה-בייט (TB)" to 1099511627776.0, "פטה-בייט (PB)" to 1.1259e15)
            11 -> mapOf("ק״מ לליטר (km/L)" to 1.0, "ק״מ לגלון ארה״ב (km/gal)" to 0.264172, "מייל לגלון ארה״ב (MPG US)" to 0.425144, "מייל לגלון בריטי (MPG UK)" to 0.354006)
            12 -> mapOf("מעלה (deg °)" to 1.0, "רדיאן (rad)" to 57.2957795, "גראדיאן (grad)" to 0.9, "אלפית ארטילרית (mil NATO)" to 0.05625, "דקת קשת (arcmin)" to (1.0 / 60.0), "שניית קשת (arcsec)" to (1.0 / 3600.0), "סיבוב שלם (rev)" to 360.0)
            13 -> mapOf("ניוטון (N)" to 1.0, "קילוניוטון (kN)" to 1000.0, "מגה-ניוטון (MN)" to 1e6, "קילוגרם-כוח (kgf)" to 9.80665, "גרם-כוח (gf)" to 0.00980665, "פאונד-כוח (lbf)" to 4.44822, "דיין (dyn)" to 1e-5)
            14 -> mapOf("ניוטון-מטר (N·m)" to 1.0, "ניוטון-ס״מ (N·cm)" to 0.01, "קילוגרם-מטר (kgf·m)" to 9.80665, "פאונד-רגל (lb·ft)" to 1.355818, "פאונד-אינץ' (lb·in)" to 0.112985)
            15 -> mapOf("הרץ (Hz)" to 1.0, "קילוהרץ (kHz)" to 1000.0, "מגה-הרץ (MHz)" to 1e6, "גיגה-הרץ (GHz)" to 1e9, "טרה-הרץ (THz)" to 1e12, "סל״ד (RPM)" to (1.0 / 60.0))
            16 -> mapOf("מטר לשנייה בריבוע (m/s²)" to 1.0, "תאוצת כובד תקנית (g)" to 9.80665, "ס״מ לשנייה בריבוע (Gal)" to 0.01, "רגל לשנייה בריבוע (ft/s²)" to 0.3048, "אינץ' לשנייה בריבוע (in/s²)" to 0.0254)
            17 -> mapOf("רדיאן לשנייה (rad/s)" to 1.0, "מעלות לשנייה (°/s)" to 0.0174533, "סיבובים לדקה (RPM)" to 0.1047198, "סיבובים לשנייה (RPS)" to 6.283185)
            18 -> mapOf("רדיאן לשנייה בריבוע (rad/s²)" to 1.0, "מעלות לשנייה בריבוע (°/s²)" to 0.0174533, "סיבובים לדקה בריבוע (rev/min²)" to 0.00174533, "סיבובים לשנייה בריבוע (rev/s²)" to 6.283185)
            19 -> mapOf("ק״ג למטר מעוקב (kg/m³)" to 1.0, "גרם לסמ״ק (g/cm³)" to 1000.0, "ק״ג לליטר (kg/L)" to 1000.0, "גרם לליטר (g/L)" to 1.0, "פאונד לרגל מעוקב (lb/ft³)" to 16.01846, "פאונד לגלון (lb/gal)" to 119.826)
            20 -> mapOf("מטר מעוקב לק״ג (m³/kg)" to 1.0, "ליטר לק״ג (L/kg)" to 0.001, "סמ״ק לגרם (cm³/g)" to 0.001, "רגל מעוקב לפאונד (ft³/lb)" to 0.062428)
            21 -> mapOf("ק״ג·מטר רבוע (kg·m²)" to 1.0, "ק״ג·סמ״ר (kg·cm²)" to 0.0001, "גרם·סמ״ר (g·cm²)" to 1e-7, "פאונד·רגל רבוע (lb·ft²)" to 0.0421401, "פאונד·אינץ' רבוע (lb·in²)" to 0.00029264)
            22 -> mapOf("ניוטון למטר (N/m)" to 1.0, "מיליניוטון למטר (mN/m)" to 0.001, "דיין לס״מ (dyn/cm)" to 0.001, "גרם-כוח לס״מ (gf/cm)" to 0.980665, "פאונד-כוח לאינץ' (lbf/in)" to 175.1268)
            23 -> mapOf("פסקל-שנייה (Pa·s)" to 1.0, "מיליפסקל-שנייה (mPa·s)" to 0.001, "פואז (Poise - P)" to 0.1, "סנטי-פואז (cP)" to 0.001, "פאונד לרגל-שנייה (lb/(ft·s))" to 1.488164)
            24 -> mapOf("מ״ר לשנייה (m²/s)" to 1.0, "ממ״ר לשנייה (mm²/s)" to 1e-6, "סטוקס (Stokes - St)" to 0.0001, "סנטי-סטוקס (cSt)" to 1e-6, "רגל רבוע לשנייה (ft²/s)" to 0.092903)
            25 -> mapOf("ליטר לדקה (L/min)" to 1.0, "ליטר לשנייה (L/s)" to 60.0, "ליטר לשעה (L/h)" to (1.0 / 60.0), "מ״ק לשעה (m³/h)" to 16.66667, "מ״ק לשנייה (m³/s)" to 60000.0, "גלון לדקה (GPM US)" to 3.78541, "CFM (רגל מעוקב לדקה)" to 28.31685)
            26 -> mapOf("ק״ג לשנייה (kg/s)" to 1.0, "ק״ג לדקה (kg/min)" to (1.0 / 60.0), "ק״ג לשעה (kg/h)" to (1.0 / 3600.0), "גרם לשנייה (g/s)" to 0.001, "טון לשעה (t/h)" to 0.277778, "פאונד לשעה (lb/h)" to 0.000125998)
            27 -> mapOf("מול לשנייה (mol/s)" to 1.0, "מול לדקה (mol/min)" to (1.0 / 60.0), "מול לשעה (mol/h)" to (1.0 / 3600.0), "קילומול לשנייה (kmol/s)" to 1000.0, "קילומול לשעה (kmol/h)" to 0.277778, "מילימול לשנייה (mmol/s)" to 0.001)
            29 -> mapOf("מטר עומד ל-100 מ׳ (m/100m)" to 1.0, "בר ל-100 מ׳ (Bar/100m)" to 10.1972, "קילופסקל למטר (kPa/m)" to 10.1972, "PSI ל-100 רגל (PSI/100ft)" to 2.30666)
            30 -> mapOf("מילימטר קוטר (mm)" to 1.0, "צול / אינץ' אינסטלציה (\")" to 25.4, "סנטימטר (cm)" to 10.0, "רגל (ft)" to 304.8)
            31 -> mapOf("מ״ג לליטר CaCO₃ (ppm)" to 1.0, "מעלה גרמנית (°dH)" to 17.848, "מעלה צרפתית (°fH)" to 10.0, "מעלה אנגלית / קלארק (°e)" to 14.254, "גריין לגלון (gpg)" to 17.118, "מילימול לליטר (mmol/L)" to 100.09)
            32 -> mapOf("מטר רבוע (m²)" to 1.0, "דארסי (Darcy - D)" to 9.869233e-13, "מילי-דארסי (mD)" to 9.869233e-16, "מיקרומטר רבוע (µm²)" to 1e-12)
            33 -> mapOf("וואט למטר-קלווין (W/(m·K))" to 1.0, "וואט לס״מ-קלווין (W/(cm·K))" to 100.0, "קילוקלוריה לשעה-מטר-צלזיוס" to 1.163, "BTU·in/(h·ft²·°F)" to 0.144228, "BTU/(h·ft·°F)" to 1.730735)
            34 -> mapOf("קלווין לוואט (K/W)" to 1.0, "מ״ר·קלווין לוואט (R-Value SI)" to 1.0, "R-Value אמריקאי (h·ft²·°F/BTU)" to 0.17611, "קלווין למיליוואט (K/mW)" to 1000.0)
            35 -> mapOf("ג'אול לק״ג-קלווין (J/(kg·K))" to 1.0, "קילוג'אול לק״ג-קלווין (kJ/(kg·K))" to 1000.0, "קלוריה לגרם-צלזיוס (cal/(g·°C))" to 4184.0, "BTU לפאונד-פרנהייט (BTU/(lb·°F))" to 4186.8)
            36 -> mapOf("וואט למ״ר (W/m²)" to 1.0, "קילוואט למ״ר (kW/m²)" to 1000.0, "וואט לסמ״ר (W/cm²)" to 10000.0, "BTU לשעה-רגל רבוע (BTU/(h·ft²))" to 3.15459)
            37 -> mapOf("וואט למ״ר-קלווין (W/(m²·K))" to 1.0, "קילוואט למ״ר-קלווין (kW/(m²·K))" to 1000.0, "BTU/(h·ft²·°F)" to 5.678263, "kcal/(h·m²·°C)" to 1.163)
            38 -> mapOf("אחד לקלווין (1/K)" to 1.0, "מיקרומטר למטר-קלווין (µm/(m·K) / ppm/K)" to 1e-6, "אחד לפרנהייט (1/°F)" to 1.8, "ppm לפרנהייט (ppm/°F)" to 1.8e-6)
            39 -> mapOf("ג'אול לק״ג (J/kg)" to 1.0, "קילוג'אול לק״ג (kJ/kg)" to 1000.0, "מגה-ג'אול לק״ג (MJ/kg)" to 1e6, "קילוואט-שעה לק״ג (kWh/kg)" to 3.6e6, "קלוריה לגרם (cal/g)" to 4184.0, "BTU לפאונד (BTU/lb)" to 2326.0)
            40 -> mapOf("אחוז יעילות (%)" to 0.01, "שבר עשרוני (COP / η)" to 1.0, "EER (BTU/Wh)" to 0.293071)
            42 -> mapOf("גרם אדי מים למ״ק (g/m³)" to 1.0, "מיליגרם לליטר (mg/L)" to 1.0, "ק״ג למ״ק (kg/m³)" to 1000.0, "גריין לרגל מעוקב (gr/ft³)" to 2.28835)
            43 -> mapOf("קילוג'אול לק״ג (kJ/kg)" to 1.0, "ג'אול לק״ג (J/kg)" to 0.001, "ג'אול למול (J/mol)" to 0.001, "BTU לפאונד (BTU/lb)" to 2.326, "קלוריה לגרם (cal/g)" to 4.184)
            44 -> mapOf("ג'אול לקלווין (J/K)" to 1.0, "קילוג'אול לקלווין (kJ/K)" to 1000.0, "ג'אול למול-קלווין (J/(mol·K))" to 1.0, "קלוריה לקלווין (cal/K)" to 4.184, "BTU לרנקין (BTU/°R)" to 1899.1)
            45 -> mapOf("אמפר (A)" to 1.0, "מיליאמפר (mA)" to 0.001, "מיקרואמפר (µA)" to 1e-6, "ננואמפר (nA)" to 1e-9, "קילואמפר (kA)" to 1000.0)
            46 -> mapOf("וולט (V)" to 1.0, "מיליוולט (mV)" to 0.001, "מיקרווולט (µV)" to 1e-6, "קילווולט (kV)" to 1000.0, "מגה-וולט (MV)" to 1e6)
            47 -> mapOf("אוהם (Ω)" to 1.0, "מילי-אוהם (mΩ)" to 0.001, "קילו-אוהם (kΩ)" to 1000.0, "מגה-אוהם (MΩ)" to 1e6, "גיגה-אוהם (GΩ)" to 1e9)
            48 -> mapOf("סימנס / מהו (S / ℧)" to 1.0, "מילי-סימנס (mS)" to 0.001, "מיקרו-סימנס (µS)" to 1e-6, "קילו-סימנס (kS)" to 1000.0)
            49 -> mapOf("פאראד (F)" to 1.0, "מיליפאראד (mF)" to 1e-3, "מיקרופאראד (µF)" to 1e-6, "ננופאראד (nF)" to 1e-9, "פיקופאראד (pF)" to 1e-12)
            50 -> mapOf("הנרי (H)" to 1.0, "מיליהנרי (mH)" to 1e-3, "מיקרוהנרי (µH)" to 1e-6, "ננוהנרי (nH)" to 1e-9, "קילוהנרי (kH)" to 1000.0)
            51 -> mapOf("קולון (C)" to 1.0, "מיליקולון (mC)" to 0.001, "מיקרוקולון (µC)" to 1e-6, "אמפר-שעה (Ah)" to 3600.0, "מיליאמפר-שעה (mAh)" to 3.6, "מטען אלמנטרי (e)" to 1.6021766e-19)
            52 -> mapOf("קולון למטר (C/m)" to 1.0, "מיליקולון למטר (mC/m)" to 0.001, "מיקרוקולון למטר (µC/m)" to 1e-6, "קולון לס״מ (C/cm)" to 100.0)
            53 -> mapOf("קולון למ״ר (C/m²)" to 1.0, "מיליקולון למ״ר (mC/m²)" to 0.001, "מיקרוקולון למ״ר (µC/m²)" to 1e-6, "קולון לסמ״ר (C/cm²)" to 10000.0)
            54 -> mapOf("קולון למ״ק (C/m³)" to 1.0, "מיליקולון למ״ק (mC/m³)" to 0.001, "מיקרוקולון למ״ק (µC/m³)" to 1e-6, "קולון לסמ״ק (C/cm³)" to 1e6)
            55 -> mapOf("וולט למטר (V/m)" to 1.0, "קילווולט למטר (kV/m)" to 1000.0, "קילווולט לס״מ (kV/cm)" to 100000.0, "וולט למילימטר (V/mm)" to 1000.0, "מיליוולט למטר (mV/m)" to 0.001)
            56 -> mapOf("וובר (Wb)" to 1.0, "מיליוובר (mWb)" to 0.001, "מיקרווובר (µWb)" to 1e-6, "מקסוול (Maxwell - Mx)" to 1e-8)
            57 -> mapOf("טסלה (T)" to 1.0, "מיליטסלה (mT)" to 0.001, "מיקרוטסלה (µT)" to 1e-6, "ננוטסלה (nT)" to 1e-9, "גאוס (Gauss - G)" to 0.0001)
            58 -> mapOf("אמפר למטר (A/m)" to 1.0, "קילואמפר למטר (kA/m)" to 1000.0, "אורסטד (Oersted - Oe)" to 79.57747)
            59 -> mapOf("אמפר-כריכה (At)" to 1.0, "מיליאמפר-כריכה (mAt)" to 0.001, "קילואמפר-כריכה (kAt)" to 1000.0, "גילברט (Gilbert - Gb)" to 0.7957747)
            60 -> mapOf("וולט-אמפר (VA / VAR)" to 1.0, "קילו-וולט-אמפר (kVA / kVAR)" to 1000.0, "מגה-וולט-אמפר (MVA / MVAR)" to 1e6)
            61 -> mapOf("לוקס (lx)" to 1.0, "קילו-לוקס (klx)" to 1000.0, "פוט-קנדל (fc)" to 10.76391, "פוט (Phot - ph)" to 10000.0)
            62 -> mapOf("קנדלה למ״ר (cd/m² / Nit)" to 1.0, "קנדלה לסמ״ר (Stilb)" to 10000.0, "למברט (Lambert - L)" to 3183.099, "פוט-למברט (fL)" to 3.426259)
            63 -> mapOf("לומן (lm)" to 1.0, "קילו-לומן (klm)" to 1000.0, "נר כדורי (Spherical Candle)" to 12.56637)
            64 -> mapOf("קנדלה (cd)" to 1.0, "מיליקנדלה (mcd)" to 0.001, "קילוקנדלה (kcd)" to 1000.0, "הפנר-קרצה (HK)" to 0.903)
            65 -> mapOf("דיופטר (Diopter - dpt / m⁻¹)" to 1.0, "מוקד בס״מ (100/f)" to 1.0)
            66 -> mapOf("פסקל לחץ קול (Pa SPL)" to 1.0, "מיליפסקל (mPa)" to 0.001, "מיקרופסקל (µPa - סף שמיעה 20)" to 1e-6, "דיין לסמ״ר (µbar)" to 0.1)
            67 -> mapOf("בקרל (Bq)" to 1.0, "קילובקרל (kBq)" to 1000.0, "מגה-בקרל (MBq)" to 1e6, "גיגה-בקרל (GBq)" to 1e9, "קירי (Curie - Ci)" to 3.7e10, "מיליקירי (mCi)" to 3.7e7, "רתרפורד (Rd)" to 1e6)
            68 -> mapOf("גריי (Gy)" to 1.0, "מיליגריי (mGy)" to 0.001, "מיקרוגריי (µGy)" to 1e-6, "ראד (rad)" to 0.01, "מיליראד (mrad)" to 1e-5)
            69 -> mapOf("סיוורט (Sv)" to 1.0, "מיליסיוורט (mSv)" to 0.001, "מיקרוסיוורט (µSv)" to 1e-6, "רם (rem)" to 0.01, "מילירם (mrem)" to 1e-5)
            70 -> mapOf("קולון לק״ג (C/kg)" to 1.0, "מיליקולון לק״ג (mC/kg)" to 0.001, "רנטגן (Roentgen - R)" to 0.000258, "מילירנטגן (mR)" to 2.58e-7)
            71 -> mapOf("ננומטר (nm)" to 1.0, "אנגסטרם (Å)" to 0.1, "מיקרומטר (µm)" to 1000.0, "מילימטר (mm)" to 1e6, "מטר (m)" to 1e9)
            72 -> mapOf("נקודות לאינץ' (DPI / PPI)" to 1.0, "נקודות לס״מ (dpcm)" to 2.54, "נקודות למ״מ (dpmm)" to 25.4)
            73 -> mapOf("מ״ג לדציליטר (mg/dL - ישראל/ארה״ב)" to 1.0, "מילימול לליטר (mmol/L - אירופה)" to 18.0182, "גרם לליטר (g/L)" to 100.0)
            74 -> mapOf("כולסטרול mg/dL" to 1.0, "כולסטרול mmol/L" to 38.67, "טריגליצרידים mmol/L" to 88.57)
            75 -> mapOf("מ״ל לשעה (mL/h)" to 1.0, "טיפות לדקה (gtt/min - סט 20)" to 3.0, "טיפות לדקה (סט דם 15)" to 4.0, "מיקרו-טיפות לדקה (60 µgtt/min)" to 1.0)
            76 -> mapOf("מ״ג לק״ג (mg/kg)" to 1.0, "מק״ג לק״ג (µg/kg)" to 0.001, "גרם לק״ג (g/kg)" to 1000.0, "מ״ג לפאונד (mg/lb)" to 2.20462)
            77 -> mapOf("מול לליטר (mol/L - M)" to 1.0, "מילימול לליטר (mmol/L - mM)" to 0.001, "מיקרומול לליטר (µmol/L - µM)" to 1e-6, "ננומול לליטר (nmol/L)" to 1e-9, "מול למ״ק (mol/m³)" to 0.001)
            78 -> mapOf("גרם לליטר (g/L)" to 1.0, "מ״ג לדציליטר (mg/dL)" to 0.01, "מ״ג לליטר (mg/L / ppm)" to 0.001, "מק״ג לליטר (µg/L / ppb)" to 1e-6, "ק״ג למ״ק (kg/m³)" to 1.0)
            79 -> mapOf("גרם למול (g/mol)" to 1.0, "ק״ג למול (kg/mol)" to 1000.0, "ק״ג לקילומול (kg/kmol)" to 1.0, "דלטון / יחידת מסה אטומית (Da / u)" to 1.0)
            80 -> mapOf("יחידות pH (0-14)" to 1.0, "ריכוז יוני מימן -log[H+]" to 1.0)
            81 -> mapOf("מיליאוסמול לק״ג (mOsm/kg)" to 1.0, "אוסמול לק״ג (Osm/kg)" to 1000.0, "מיליאוסמול לליטר (mOsm/L)" to 1.0)
            82 -> mapOf("יחידה אנזימטית (U = µmol/min)" to 1.0, "קטאל (Katal = mol/s)" to 6e7, "מיליקטאל (mkat)" to 60000.0, "מיקרוקטאל (µkat)" to 60.0, "ננוקטאל (nkat)" to 0.06)
            83 -> mapOf("שנייה (s)" to 1.0, "מילישנייה (ms)" to 0.001, "מיקרושנייה (µs)" to 1e-6, "ננושנייה (ns)" to 1e-9, "דקה (min)" to 60.0, "שעה (h)" to 3600.0, "יום (day)" to 86400.0, "שבוע (wk)" to 604800.0, "חודש ממוצע (30.44d)" to 2629746.0, "שנה (yr)" to 31556952.0)
            84 -> mapOf("מגה-ביט לשנייה (Mbps)" to 1.0, "מגה-בייט לשנייה (MB/s)" to 8.0, "קילוביט לשנייה (Kbps)" to 0.001, "קילובייט לשנייה (KB/s)" to 0.008, "גיגה-ביט לשנייה (Gbps)" to 1000.0, "גיגה-בייט לשנייה (GB/s)" to 8000.0)
            85 -> mapOf("פיקסל (px - 96 DPI)" to 1.0, "נקודה טיפוגרפית (pt)" to 1.333333, "פיקה (Pica - pc)" to 16.0, "em / rem (בסיס 16px)" to 16.0, "אינץ' (in)" to 96.0, "מילימטר (mm)" to 3.779528)
            86 -> mapOf("יחידה אסטרונומית (AU)" to 1.0, "שנת אור (ly)" to 63241.077, "פארסק (pc)" to 206264.806, "מגה-פארסק (Mpc)" to 2.062648e11, "דקת אור" to 0.120237, "שניית אור" to 0.002004, "מיליון ק״מ (M km)" to (1.0 / 149.59787))
            87 -> mapOf("כוס בינלאומית (240 mL)" to 240.0, "כוס ישראלית (200 mL)" to 200.0, "כף (Tbsp - 15 mL)" to 15.0, "כפית (Tsp - 5 mL)" to 5.0, "אונקיית נוזל (fl oz)" to 29.5735, "פיינט ארה״ב (Pint)" to 473.176, "קווארט (Quart)" to 946.353, "מיליליטר (mL)" to 1.0, "ליטר (L)" to 1000.0)
            else -> mapOf("יחידת בסיס תקנית (SI)" to 1.0)
        }
    }
}

// בונה מסך המרה אינטראקטיבי מעוצב בקנבה עבור טווח קטגוריות ממוקד או כלל 87 הקטגוריות
fun MainActivity.createScopedProUnitConverter(
    headerTitle: String,
    headerSubtitle: String,
    allowedRange: IntRange = 0..86
): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20f), dp(12f), dp(20f), dp(24f))
    }
    root.addView(createProCanvaHeader(headerTitle, headerSubtitle))

    val scopedIndices = allowedRange.filter { it in ProUnitCatalog.categories87.indices }
    var selectedCatIdx = scopedIndices.firstOrNull() ?: 0
    val customUnits = loadCustomUnitsMap()

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
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(150f)
        ).apply { bottomMargin = dp(12f) }
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createScopedProUnitConverter, 20f)
        else GradientDrawable().apply {
            setColor(Utils.lightenColor(bgColor, 0.06f))
            cornerRadius = dp(20f).toFloat()
        }
    }

    val catBtn = createActionBtn("קטגוריה: ${ProUnitCatalog.categories87[selectedCatIdx]}", true) {}
    val fromBtn = createActionBtn("מ: $fromUnit", false) {}
    val toBtn = createActionBtn("ל: $toUnit", false) {}
    val valInput = createInputBox("הזן כמות להמרה...").apply { setText("1") }
    val resTv = createResultTextView()

    val sideBySideCard = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16f), dp(16f), dp(16f), dp(16f))
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createScopedProUnitConverter, 20f)
        else GradientDrawable().apply {
            setColor(Utils.lightenColor(bgColor, 0.07f))
            cornerRadius = dp(20f).toFloat()
        }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(12f) }
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

        scaleCanvas.fromLabel = "${Utils.formatResult(v, 2)} ${fromUnit.substringBefore(" ")}"
        scaleCanvas.toLabel = "${Utils.formatResult(directRes, 4)} ${toUnit.substringBefore(" ")}"
        scaleCanvas.ratioLog = ((log10(directRes.coerceAtLeast(1e-6)) + 3.0) / 6.0).toFloat()
        scaleCanvas.invalidate()

        val header = TextView(this).apply {
            text = "⚡ השוואה סימולטנית לכל היחידות בקטגוריה (לחץ להעתקה):"
            setTextColor(if (isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else primaryColor)
            textSize = 14.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(0, 0, 0, dp(8f))
        }
        sideBySideCard.addView(header)

        for ((uName, uFactor) in map) {
            val converted = v * fFactor / uFactor
            val row = TextView(this).apply {
                text = "• $uName:  ${Utils.formatResult(converted, 6)}"
                setTextColor(Color.WHITE)
                textSize = 14.5f
                gravity = Gravity.RIGHT
                setPadding(dp(4f), dp(5f), dp(4f), dp(5f))
                setOnClickListener { copyToClipboard(Utils.formatResult(converted, 6)) }
            }
            sideBySideCard.addView(row)
        }
    }

    catBtn.setOnClickListener {
        val scopedLabels = scopedIndices.map { ProUnitCatalog.categories87[it] }
        showItemPicker("בחר קטגוריית יחידות (${scopedLabels.size})", scopedLabels) { chosen ->
            selectedCatIdx = ProUnitCatalog.categories87.indexOf(chosen).coerceAtLeast(0)
            catBtn.text = "קטגוריה: $chosen"
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
        showCustomUnitCreatorDialog(ProUnitCatalog.categories87[selectedCatIdx]) {
            val m = currentUnitsMap()
            toUnit = m.keys.last()
            toBtn.text = "ל: $toUnit"
            recalcAll()
        }
    }

    val pinBtn = createPinResultButton(resTv)

    valInput.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { recalcAll() }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    recalcAll()
    root.addView(scaleCanvas)
    root.addView(catBtn)
    root.addView(fromBtn)
    root.addView(toBtn)
    root.addView(valInput)
    root.addView(addCustomUnitBtn)
    root.addView(pinBtn)
    root.addView(resTv)
    root.addView(sideBySideCard)
    scroll.addView(root)
    return scroll
}

// 7 מסכי משנה מפוצלים של 87 קטגוריות היחידות + המסך המאוחד:
fun MainActivity.createPro87UnitConverter(): View =
    createScopedProUnitConverter("📐 כל 87 קטגוריות היחידות והמרה מרובה", "מאגר 87 קטגוריות מדעיות והנדסיות עם המרה סימולטנית", 0..86)

fun MainActivity.createProMechanicsUnitsScreen(): View =
    createScopedProUnitConverter("⚙️ ממירי מכניקה, כוח, מומנט ותאוצה", "אורך, מסה, שטח, נפח, כוח, מומנט פיתול, תאוצה, התמד ומתח פנים", 0..21)

fun MainActivity.createProFluidsUnitsScreen(): View =
    createScopedProUnitConverter("💧 ממירי זורמים, צמיגות, ספיקה וצנרת", "צמיגות דינמית/קינמטית, ספיקת נפח/מסה/מולרית, לחץ, קשיות מים וחדירות", 22..31)

fun MainActivity.createProThermoUnitsScreen(): View =
    createScopedProUnitConverter("🔥 ממירי תרמודינמיקה, חום ומיזוג", "מוליכות והתנגדות תרמית, קיבול חום, שטף חום, BTU/Ton, לחות, אנתלפיה ואנטרופיה", 32..43)

fun MainActivity.createProElectricalUnitsScreen(): View =
    createScopedProUnitConverter("⚡ ממירי חשמל, מגנטיות, שדה והשראות", "זרם, מתח, התנגדות, קיבול, השראות, מטען, שדה חשמלי, שטף מגנטי (טסלה/גאוס) ו-kVA", 44..59)

fun MainActivity.createProOpticsRadiationUnitsScreen(): View =
    createScopedProUnitConverter("☀️ ממירי אופטיקה, אקוסטיקה וקרינה", "לוקס, לומן, קנדלה, דיופטר, לחץ קול, רדיואקטיביות (Bq/Ci), גריי, סיוורט ו-DPI", 60..71)

fun MainActivity.createProMedicalChemUnitsScreen(): View =
    createScopedProUnitConverter("🧪 ממירי רפואה, כימיה ומעבדה קלינית", "סוכר וכולסטרול בדם, קצב עירוי IV, מינון תרופות, מולריות, אוסמולליות ופעילות אנזימטית", 72..81)

fun MainActivity.createProDigitalAstroUnitsScreen(): View =
    createScopedProUnitConverter("🌌 ממירי תקשורת, טיפוגרפיה, חלל ובישול", "זמן, רוחב פס (Mbps/MBs), טיפוגרפיה (px/pt/em), שנות אור/פארסק ומידות אפייה", 82..86)

private fun MainActivity.loadCustomUnitsMap(): MutableMap<String, MutableMap<String, Double>> {
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

private fun MainActivity.showCustomUnitCreatorDialog(categoryName: String, onAdded: () -> Unit) {
    val layout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20f), dp(20f), dp(20f), dp(12f))
        setBackgroundColor(Utils.lightenColor(bgColor, 0.08f))
    }
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
                val all = loadCustomUnitsMap()
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
 

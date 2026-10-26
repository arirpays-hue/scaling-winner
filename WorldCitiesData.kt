package com.example.calculator

import java.util.TimeZone

object WorldCitiesData {

    private val regionNames = mapOf(
        "Africa" to "אפריקה",
        "America" to "אמריקה",
        "Antarctica" to "אנטארקטיקה",
        "Arctic" to "האזור הארקטי",
        "Asia" to "אסיה",
        "Atlantic" to "האוקיינוס האטלנטי",
        "Australia" to "אוסטרליה",
        "Europe" to "אירופה",
        "Indian" to "האוקיינוס ההודי",
        "Pacific" to "האוקיינוס השקט"
    )

    // מיפוי מקיף של ערים ואזורי זמן בעולם לעברית
    private val hebrewCityNames = mapOf(
        // ישראל והמזרח התיכון
        "Asia/Jerusalem" to "ירושלים / תל אביב (ישראל)",
        "Asia/Tel_Aviv" to "תל אביב (ישראל)",
        "Asia/Hebron" to "חברון",
        "Asia/Gaza" to "עזה",
        "Asia/Amman" to "עמאן (ירדן)",
        "Asia/Beirut" to "ביירות (לבנון)",
        "Asia/Damascus" to "דמשק (סוריה)",
        "Asia/Baghdad" to "בגדאד (עיראק)",
        "Asia/Kuwait" to "כווית סיטי (כווית)",
        "Asia/Riyadh" to "ריאד (ערב הסעודית)",
        "Asia/Dubai" to "דובאי (איחוד האמירויות)",
        "Asia/Qatar" to "דוחה (קטאר)",
        "Asia/Bahrain" to "מנאמה (בחריין)",
        "Asia/Muscat" to "מסקט (עומאן)",
        "Asia/Aden" to "עדן (תימן)",
        "Asia/Tehran" to "טהרן (איראן)",
        "Asia/Nicosia" to "ניקוסיה (קפריסין)",

        // אירופה
        "Europe/London" to "לונדון (בריטניה)",
        "Europe/Dublin" to "דבלין (אירלנד)",
        "Europe/Paris" to "פריז (צרפת)",
        "Europe/Berlin" to "ברלין (גרמניה)",
        "Europe/Rome" to "רומא (איטליה)",
        "Europe/Madrid" to "מדריד (ספרד)",
        "Europe/Lisbon" to "ליסבון (פורטוגל)",
        "Europe/Amsterdam" to "אמסטרדם (הולנד)",
        "Europe/Brussels" to "בריסל (בלגיה)",
        "Europe/Luxembourg" to "לוקסמבורג",
        "Europe/Zurich" to "ציריך (שווייץ)",
        "Europe/Vienna" to "וינה (אוסטריה)",
        "Europe/Prague" to "פראג (צ׳כיה)",
        "Europe/Warsaw" to "ורשה (פולין)",
        "Europe/Budapest" to "בודפשט (הונגריה)",
        "Europe/Bratislava" to "ברטיסלבה (סלובקיה)",
        "Europe/Ljubljana" to "לובליאנה (סלובניה)",
        "Europe/Zagreb" to "זאגרב (קרואטיה)",
        "Europe/Sarajevo" to "סרייבו (בוסניה)",
        "Europe/Belgrade" to "בלגרד (סרביה)",
        "Europe/Podgorica" to "פודגוריצה (מונטנגרו)",
        "Europe/Tirane" to "טירנה (אלבניה)",
        "Europe/Skopje" to "סקופיה (מקדוניה הצפונית)",
        "Europe/Sofia" to "סופיה (בולגריה)",
        "Europe/Bucharest" to "בוקרשט (רומניה)",
        "Europe/Chisinau" to "קישינב (מולדובה)",
        "Europe/Athens" to "אתונה (יוון)",
        "Europe/Istanbul" to "איסטנבול (טורקיה)",
        "Europe/Kyiv" to "קייב (אוקראינה)",
        "Europe/Kiev" to "קייב (אוקראינה)",
        "Europe/Minsk" to "מינסק (בלארוס)",
        "Europe/Moscow" to "מוסקבה (רוסיה)",
        "Europe/Kaliningrad" to "קלינינגרד (רוסיה)",
        "Europe/Samara" to "סמרה (רוסיה)",
        "Europe/Volgograd" to "וולגוגרד (רוסיה)",
        "Europe/Saratov" to "סרטוב (רוסיה)",
        "Europe/Kirov" to "קירוב (רוסיה)",
        "Europe/Astrakhan" to "אסטרחן (רוסיה)",
        "Europe/Ulyanovsk" to "אוליאנובסק (רוסיה)",
        "Europe/Copenhagen" to "קופנהגן (דנמרק)",
        "Europe/Stockholm" to "סטוקהולם (שוודיה)",
        "Europe/Oslo" to "אוסלו (נורווגיה)",
        "Europe/Helsinki" to "הלסינקי (פינלנד)",
        "Europe/Tallinn" to "טאלין (אסטוניה)",
        "Europe/Riga" to "ריגה (לטביה)",
        "Europe/Vilnius" to "וילנה (ליטא)",
        "Europe/Monaco" to "מונקו",
        "Europe/Andorra" to "אנדורה",
        "Europe/Malta" to "ולטה (מלטה)",
        "Europe/Gibraltar" to "גיברלטר",
        "Europe/Vatican" to "קריית הוותיקן",
        "Europe/San_Marino" to "סן מרינו",
        "Europe/Jersey" to "ג׳רזי",
        "Europe/Guernsey" to "גרנזי",
        "Europe/Isle_of_Man" to "האי מאן",
        "Atlantic/Reykjavik" to "רייקיאוויק (איסלנד)",
        "Atlantic/Azores" to "האיים האזוריים (פורטוגל)",
        "Atlantic/Canary" to "האיים הקנריים (ספרד)",
        "Atlantic/Madeira" to "מדיירה (פורטוגל)",
        "Atlantic/Faroe" to "איי פארו",

        // צפון ומרכז אמריקה
        "America/New_York" to "ניו יורק (ארה״ב - מזרח)",
        "America/Chicago" to "שיקגו (ארה״ב - מרכז)",
        "America/Denver" to "דנבר (ארה״ב - הרים)",
        "America/Phoenix" to "פיניקס (אריזונה, ארה״ב)",
        "America/Los_Angeles" to "לוס אנג׳לס (ארה״ב - מערב)",
        "America/Anchorage" to "אנקורג׳ (אלסקה, ארה״ב)",
        "America/Detroit" to "דטרויט (מישיגן, ארה״ב)",
        "America/Indianapolis" to "אינדיאנפוליס (ארה״ב)",
        "America/Boise" to "בויסי (איידהו, ארה״ב)",
        "America/Juneau" to "ג׳ונו (אלסקה, ארה״ב)",
        "America/Adak" to "אדאק (אלסקה, ארה״ב)",
        "Pacific/Honolulu" to "הונולולו (הוואי, ארה״ב)",
        "America/Toronto" to "טורונטו (קנדה)",
        "America/Vancouver" to "ונקובר (קנדה)",
        "America/Montreal" to "מונטריאול (קנדה)",
        "America/Winnipeg" to "וויניפג (קנדה)",
        "America/Edmonton" to "אדמונטון (קנדה)",
        "America/Halifax" to "הליפקס (קנדה)",
        "America/St_Johns" to "סנט ג׳ונס (ניופאונדלנד, קנדה)",
        "America/Regina" to "רג׳יינה (קנדה)",
        "America/Whitehorse" to "ווייטהורס (יוקון, קנדה)",
        "America/Yellowknife" to "ילונייף (קנדה)",
        "America/Mexico_City" to "מקסיקו סיטי (מקסיקו)",
        "America/Cancun" to "קנקון (מקסיקו)",
        "America/Tijuana" to "טיחואנה (מקסיקו)",
        "America/Monterrey" to "מונטריי (מקסיקו)",
        "America/Merida" to "מרידה (מקסיקו)",
        "America/Chihuahua" to "צ׳יוואווה (מקסיקו)",
        "America/Mazatlan" to "מזטלן (מקסיקו)",
        "America/Hermosillo" to "הרמוסיו (מקסיקו)",
        "America/Guatemala" to "גואטמלה סיטי (גואטמלה)",
        "America/Belize" to "בליז סיטי (בליז)",
        "America/El_Salvador" to "סן סלבדור (אל סלבדור)",
        "America/Tegucigalpa" to "טגוסיגלפה (הונדורס)",
        "America/Managua" to "מנגואה (ניקרגואה)",
        "America/Costa_Rica" to "סן חוסה (קוסטה ריקה)",
        "America/Panama" to "פנמה סיטי (פנמה)",
        "America/Havana" to "הוואנה (קובה)",
        "America/Jamaica" to "קינגסטון (ג׳מייקה)",
        "America/Port-au-Prince" to "פורט-או-פרנס (האיטי)",
        "America/Santo_Domingo" to "סנטו דומינגו (הרפובליקה הדומיניקנית)",
        "America/Puerto_Rico" to "סן חואן (פוארטו ריקו)",
        "America/Nassau" to "נסאו (איי בהאמה)",
        "America/Barbados" to "ברידג׳טאון (ברבדוס)",
        "America/Port_of_Spain" to "פורט אוף ספיין (טרינידד וטובגו)",
        "America/Curacao" to "קוראסאו",
        "America/Aruba" to "ארובה",
        "America/Cayman" to "איי קיימן",
        "Atlantic/Bermuda" to "ברמודה",

        // דרום אמריקה
        "America/Sao_Paulo" to "סאו פאולו / ריו דה ז׳ניירו (ברזיל)",
        "America/Manaus" to "מנאוס (אמזונס, ברזיל)",
        "America/Belem" to "בלם (ברזיל)",
        "America/Fortaleza" to "פורטלזה (ברזיל)",
        "America/Recife" to "רסיפה (ברזיל)",
        "America/Bahia" to "סלבדור דה באהיה (ברזיל)",
        "America/Cuiaba" to "קויאבה (ברזיל)",
        "America/Rio_Branco" to "ריו ברנקו (ברזיל)",
        "America/Buenos_Aires" to "בואנוס איירס (ארגנטינה)",
        "America/Argentina/Buenos_Aires" to "בואנוס איירס (ארגנטינה)",
        "America/Argentina/Cordoba" to "קורדובה (ארגנטינה)",
        "America/Argentina/Mendoza" to "מנדוסה (ארגנטינה)",
        "America/Argentina/Salta" to "סלטה (ארגנטינה)",
        "America/Argentina/Ushuaia" to "אושואיה (ארגנטינה)",
        "America/Santiago" to "סנטיאגו (צ׳ילה)",
        "America/Lima" to "לימה (פרו)",
        "America/Bogota" to "בוגוטה (קולומביה)",
        "America/Caracas" to "קראקס (ונצואלה)",
        "America/La_Paz" to "לה פאס (בוליביה)",
        "America/Guayaquil" to "גואיאקיל / קיטו (אקוודור)",
        "America/Asuncion" to "אסונסיון (פרגוואי)",
        "America/Montevideo" to "מונטווידאו (אורוגוואי)",
        "America/Guyana" to "ג׳ורג׳טאון (גיאנה)",
        "America/Paramaribo" to "פרמריבו (סורינאם)",
        "America/Cayenne" to "קאיין (גיאנה הצרפתית)",
        "Atlantic/Stanley" to "סטנלי (איי פוקלנד)",

        // אסיה והמזרח הרחוק
        "Asia/Tokyo" to "טוקיו (יפן)",
        "Asia/Seoul" to "סיאול (קוריאה הדרומית)",
        "Asia/Pyongyang" to "פיונגיאנג (קוריאה הצפונית)",
        "Asia/Shanghai" to "שנגחאי / בייג׳ינג (סין)",
        "Asia/Urumqi" to "אורומצ׳י (סין)",
        "Asia/Hong_Kong" to "הונג קונג",
        "Asia/Macau" to "מקאו",
        "Asia/Taipei" to "טאיפיי (טייוואן)",
        "Asia/Singapore" to "סינגפור",
        "Asia/Bangkok" to "בנגקוק (תאילנד)",
        "Asia/Ho_Chi_Minh" to "הו צ׳י מין סיטי / האנוי (וייטנאם)",
        "Asia/Saigon" to "סייגון / הו צ׳י מין (וייטנאם)",
        "Asia/Phnom_Penh" to "פנום פן (קמבודיה)",
        "Asia/Vientiane" to "ויינטיאן (לאוס)",
        "Asia/Yangon" to "יאנגון (מיאנמר)",
        "Asia/Rangoon" to "רנגון (מיאנמר)",
        "Asia/Kuala_Lumpur" to "קואלה לומפור (מלזיה)",
        "Asia/Jakarta" to "ג׳קרטה (אינדונזיה)",
        "Asia/Makassar" to "מקאסאר / באלי (אינדונזיה)",
        "Asia/Jayapura" to "ג׳איאפורה (אינדונזיה)",
        "Asia/Manila" to "מנילה (הפיליפינים)",
        "Asia/Brunei" to "בנדר סרי בגוואן (ברוניי)",
        "Asia/Dili" to "דילי (מזרח טימור)",
        "Asia/Kolkata" to "מומבאי / ניו דלהי (הודו)",
        "Asia/Calcutta" to "כלכותה / ניו דלהי (הודו)",
        "Asia/Colombo" to "קולומבו (סרי לנקה)",
        "Asia/Kathmandu" to "קטמנדו (נפאל)",
        "Asia/Katmandu" to "קטמנדו (נפאל)",
        "Asia/Dhaka" to "דאקה (בנגלדש)",
        "Asia/Thimphu" to "טהימפהו (בהוטן)",
        "Asia/Karachi" to "קראצ׳י / אסלאמאבאד (פקיסטן)",
        "Asia/Kabul" to "קאבול (אפגניסטן)",
        "Asia/Tashkent" to "טשקנט (אוזבקיסטן)",
        "Asia/Samarkand" to "סמרקנד (אוזבקיסטן)",
        "Asia/Almaty" to "אלמטי / אסטנה (קזחסטן)",
        "Asia/Bishkek" to "בישקק (קירגיזסטן)",
        "Asia/Dushanbe" to "דושנבה (טג׳יקיסטן)",
        "Asia/Ashgabat" to "אשגבאט (טורקמניסטן)",
        "Asia/Baku" to "באקו (אזרבייג׳ן)",
        "Asia/Tbilisi" to "טביליסי (גיאורגיה)",
        "Asia/Yerevan" to "ירוואן (ארמניה)",
        "Asia/Yekaterinburg" to "יקטרינבורג (רוסיה)",
        "Asia/Omsk" to "אומסק (רוסיה)",
        "Asia/Novosibirsk" to "נובוסיבירסק (רוסיה)",
        "Asia/Krasnoyarsk" to "קרסנויארסק (רוסיה)",
        "Asia/Irkutsk" to "אירקוטסק (רוסיה)",
        "Asia/Yakutsk" to "יקוטסק (רוסיה)",
        "Asia/Vladivostok" to "ולדיווסטוק (רוסיה)",
        "Asia/Magadan" to "מגדן (רוסיה)",
        "Asia/Kamchatka" to "קמצ׳טקה (רוסיה)",
        "Asia/Sakhalin" to "סחלין (רוסיה)",
        "Asia/Ulaanbaatar" to "אולן בטור (מונגוליה)",
        "Indian/Maldives" to "מאלה (האיים המלדיביים)",

        // אוסטרליה והאוקיינוס השקט
        "Australia/Sydney" to "סידני (אוסטרליה)",
        "Australia/Melbourne" to "מלבורן (אוסטרליה)",
        "Australia/Brisbane" to "בריזבן (אוסטרליה)",
        "Australia/Perth" to "פרת׳ (אוסטרליה)",
        "Australia/Adelaide" to "אדלייד (אוסטרליה)",
        "Australia/Darwin" to "דרווין (אוסטרליה)",
        "Australia/Hobart" to "הובארט (טסמניה, אוסטרליה)",
        "Australia/Canberra" to "קנברה (אוסטרליה)",
        "Pacific/Auckland" to "אוקלנד / ולינגטון (ניו זילנד)",
        "Pacific/Fiji" to "סובה (פיג׳י)",
        "Pacific/Guam" to "גואם",
        "Pacific/Port_Moresby" to "פורט מורסבי (פפואה גינאה החדשה)",
        "Pacific/Tahiti" to "טהיטי (פולינזיה הצרפתית)",
        "Pacific/Samoa" to "סמואה",
        "Pacific/Apia" to "אפיה (סמואה)",
        "Pacific/Tongatapu" to "נוקואלופה (טונגה)",
        "Pacific/Noumea" to "נומאה (קלדוניה החדשה)",
        "Pacific/Efate" to "פורט וילה (ונואטו)",
        "Pacific/Guadalcanal" to "הוניארה (איי שלמה)",
        "Pacific/Palau" to "פלאו",
        "Pacific/Galapagos" to "איי גלאפגוס (אקוודור)",
        "Pacific/Easter" to "אי הפסחא (צ׳ילה)",

        // אפריקה
        "Africa/Cairo" to "קהיר (מצרים)",
        "Africa/Johannesburg" to "יוהנסבורג / קייפטאון (דרום אפריקה)",
        "Africa/Casablanca" to "קזבלנקה / רבאט (מרוקו)",
        "Africa/Algiers" to "אלג׳יר (אלג׳יריה)",
        "Africa/Tunis" to "תוניס (תוניסיה)",
        "Africa/Tripoli" to "טריפולי (לוב)",
        "Africa/Khartoum" to "חרטום (סודאן)",
        "Africa/Juba" to "ג׳ובה (דרום סודאן)",
        "Africa/Addis_Ababa" to "אדיס אבבה (אתיופיה)",
        "Africa/Asmara" to "אסמרה (אריתריאה)",
        "Africa/Asmera" to "אסמרה (אריתריאה)",
        "Africa/Djibouti" to "ג׳יבוטי",
        "Africa/Mogadishu" to "מוגדישו (סומליה)",
        "Africa/Nairobi" to "ניירובי (קניה)",
        "Africa/Kampala" to "קמפלה (אוגנדה)",
        "Africa/Dar_es_Salaam" to "דאר א-סלאם (טנזניה)",
        "Africa/Kigali" to "קיגאלי (רואנדה)",
        "Africa/Bujumbura" to "בוג׳ומבורה (בורונדי)",
        "Africa/Lagos" to "לאגוס / אבוג׳ה (ניגריה)",
        "Africa/Accra" to "אקרה (גאנה)",
        "Africa/Abidjan" to "אביג׳אן (חוף השנהב)",
        "Africa/Dakar" to "דקאר (סנגל)",
        "Africa/Bamako" to "במקו (מאלי)",
        "Africa/Conakry" to "קונאקרי (גינאה)",
        "Africa/Freetown" to "פריטאון (סיירה לאון)",
        "Africa/Monrovia" to "מונרוביה (ליבריה)",
        "Africa/Banjul" to "בנג׳ול (גמביה)",
        "Africa/Bissau" to "ביסאו (גינאה ביסאו)",
        "Africa/Nouakchott" to "נואקשוט (מאוריטניה)",
        "Africa/Ouagadougou" to "ואגאדוגו (בורקינה פאסו)",
        "Africa/Niamey" to "ניאמיי (ניז׳ר)",
        "Africa/Lome" to "לומה (טוגו)",
        "Africa/Porto-Novo" to "פורטו-נובו (בנין)",
        "Africa/Douala" to "דואלה / יאונדה (קמרון)",
        "Africa/Bangui" to "בנגי (הרפובליקה המרכז-אפריקאית)",
        "Africa/Ndjamena" to "נג׳מנה (צ׳אד)",
        "Africa/Kinshasa" to "קינשאסה (קונגו)",
        "Africa/Lubumbashi" to "לובומבאשי (קונגו)",
        "Africa/Brazzaville" to "ברזוויל (קונגו)",
        "Africa/Libreville" to "ליברוויל (גבון)",
        "Africa/Malabo" to "מלאבו (גינאה המשוונית)",
        "Africa/Luanda" to "לואנדה (אנגולה)",
        "Africa/Windhoek" to "וינדהוק (נמיביה)",
        "Africa/Gaborone" to "גבורונה (בוצואנה)",
        "Africa/Harare" to "הארארה (זימבבואה)",
        "Africa/Lusaka" to "לוסקה (זמביה)",
        "Africa/Blantyre" to "בלנטייר (מלאווי)",
        "Africa/Maputo" to "מפוטו (מוזמביק)",
        "Africa/Maseru" to "מסרו (לסוטו)",
        "Africa/Mbabane" to "מבבנה (אסוואטיני)",
        "Indian/Antananarivo" to "אנטננריבו (מדגסקר)",
        "Indian/Mauritius" to "פורט לואי (מאוריציוס)",
        "Indian/Mahe" to "ויקטוריה (סיישל)"
    )

    // תעתיק אוטומטי לעברית עבור ערים משניות שלא מופיעות במילון הראשי
    private fun transliterateToHebrew(englishName: String): String {
        val clean = englishName.replace("_", " ")
        val sb = StringBuilder()
        var i = 0
        val lower = clean.lowercase()
        while (i < lower.length) {
            if (i + 1 < lower.length) {
                val two = lower.substring(i, i + 2)
                val mappedTwo = when (two) {
                    "sh" -> "ש"; "ch" -> "צ׳"; "th" -> "ת"; "ph" -> "פ"; "kh" -> "ח"
                    "zh" -> "ז׳"; "oo" -> "ו"; "ee" -> "י"; "ai", "ay" -> "יי"; else -> null
                }
                if (mappedTwo != null) {
                    sb.append(mappedTwo)
                    i += 2
                    continue
                }
            }
            val ch = lower[i]
            val hebChar = when (ch) {
                'a' -> if (i == 0 || lower[i - 1] == ' ') "א" else "ה"
                'b' -> "ב"; 'c' -> "ק"; 'd' -> "ד"; 'e' -> if (i == 0) "א" else ""
                'f' -> "פ"; 'g' -> "ג"; 'h' -> "ה"; 'i' -> "י"; 'j' -> "ג׳"
                'k' -> "ק"; 'l' -> "ל"; 'm' -> "מ"; 'n' -> if (i == lower.lastIndex || (i + 1 < lower.length && lower[i + 1] == ' ')) "ן" else "נ"
                'o' -> "ו"; 'p' -> "פ"; 'q' -> "ק"; 'r' -> "ר"; 's' -> "ס"
                't' -> "ט"; 'u' -> "ו"; 'v' -> "ו"; 'w' -> "וו"; 'x' -> "קס"
                'y' -> "י"; 'z' -> "ז"; ' ', '-' -> " "; else -> ch.toString()
            }
            sb.append(hebChar)
            i++
        }
        return sb.toString()
    }

    fun getHebrewDisplay(zoneId: String): String {
        hebrewCityNames[zoneId]?.let { return "$it - $zoneId" }
        val regionEng = zoneId.substringBefore("/")
        val cityEng = zoneId.substringAfterLast("/")
        val regionHeb = regionNames[regionEng] ?: regionEng
        val cityHeb = hebrewCityNames.entries.find { it.key.endsWith("/$cityEng") }?.value?.substringBefore(" (")
            ?: transliterateToHebrew(cityEng)
        return "$cityHeb ($regionHeb) - $zoneId"
    }

    fun getAllWorldCitiesHebrew(): List<String> {
        val result = mutableListOf<String>()
        val seenIds = mutableSetOf<String>()

        // הוספת הערים המרכזיות בראש הרשימה
        val priorityIds = listOf(
            "Asia/Jerusalem", "America/New_York", "Europe/London", "Europe/Paris",
            "Europe/Berlin", "Asia/Tokyo", "Australia/Sydney", "America/Los_Angeles",
            "Asia/Dubai", "Asia/Bangkok", "America/Toronto", "Europe/Rome",
            "Europe/Madrid", "Europe/Moscow", "Asia/Shanghai", "Asia/Singapore",
            "Asia/Hong_Kong", "America/Sao_Paulo", "America/Buenos_Aires",
            "Europe/Zurich", "Europe/Amsterdam", "Africa/Cairo", "Africa/Johannesburg"
        )

        for (id in priorityIds) {
            result.add(getHebrewDisplay(id))
            seenIds.add(id)
        }

        val allRemaining = TimeZone.getAvailableIDs()
            .filter { it.contains("/") && !it.startsWith("Etc/") && !it.startsWith("SystemV/") && !seenIds.contains(it) }
            .map { getHebrewDisplay(it) }
            .sorted()

        result.addAll(allRemaining)
        return result
    }
}

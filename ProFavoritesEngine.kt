package com.example.calculator

import android.app.AlertDialog
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class PinnedCalculation(
    val id: Long,
    val tabIndex: Int,
    val calculatorTitle: String,
    val note: String,
    val resultText: String,
    val timestamp: String
)

object ProFavoritesEngine {

    private const val KEY_FAV_TABS = "pro_fav_tab_indices"
    private const val KEY_PINNED_CALCS = "pro_pinned_calculations_json"

    fun getFavoriteTabIndices(prefs: SharedPreferences?): MutableSet<Int> {
        val raw = prefs?.getString(KEY_FAV_TABS, "") ?: ""
        if (raw.isBlank()) return mutableSetOf()
        return raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toMutableSet()
    }

    fun isFavoriteTab(prefs: SharedPreferences?, tabIndex: Int): Boolean {
        return getFavoriteTabIndices(prefs).contains(tabIndex)
    }

    fun toggleFavoriteTab(activity: MainActivity, tabIndex: Int): Boolean {
        val set = getFavoriteTabIndices(activity.prefs)
        val nowFav = if (set.contains(tabIndex)) {
            set.remove(tabIndex)
            false
        } else {
            set.add(tabIndex)
            true
        }
        activity.prefs?.edit()?.putString(KEY_FAV_TABS, set.joinToString(","))?.apply()
        val title = IosGlassTheme.cleanCategoryTitle(
            activity.categories.getOrElse(tabIndex) { "מחשבון" },
            true
        )
        Toast.makeText(
            activity,
            if (nowFav) "⭐ '$title' נוסף למועדפים!" else "הוסר מהמועדפים: $title",
            Toast.LENGTH_SHORT
        ).show()
        return nowFav
    }

    fun getPinnedCalculations(prefs: SharedPreferences?): MutableList<PinnedCalculation> {
        val list = mutableListOf<PinnedCalculation>()
        val raw = prefs?.getString(KEY_PINNED_CALCS, null) ?: return list
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    PinnedCalculation(
                        id = o.optLong("id", System.currentTimeMillis()),
                        tabIndex = o.optInt("tabIndex", 0),
                        calculatorTitle = o.optString("calculatorTitle", ""),
                        note = o.optString("note", ""),
                        resultText = o.optString("resultText", ""),
                        timestamp = o.optString("timestamp", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun savePinnedCalculation(
        activity: MainActivity,
        tabIndex: Int,
        note: String,
        resultText: String
    ) {
        val list = getPinnedCalculations(activity.prefs)
        val calcTitle = IosGlassTheme.cleanCategoryTitle(
            activity.categories.getOrElse(tabIndex) { "מחשבון" },
            true
        )
        val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        list.add(0, PinnedCalculation(System.currentTimeMillis(), tabIndex, calcTitle, note, resultText, nowStr))
        persistPinnedList(activity.prefs, list)
        Toast.makeText(activity, "📌 החישוב נשמר במועדפים!", Toast.LENGTH_SHORT).show()
    }

    fun deletePinnedCalculation(prefs: SharedPreferences?, id: Long) {
        val list = getPinnedCalculations(prefs).filterNot { it.id == id }
        persistPinnedList(prefs, list)
    }

    private fun persistPinnedList(prefs: SharedPreferences?, list: List<PinnedCalculation>) {
        val arr = JSONArray()
        for (item in list.take(100)) {
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("tabIndex", item.tabIndex)
                put("calculatorTitle", item.calculatorTitle)
                put("note", item.note)
                put("resultText", item.resultText)
                put("timestamp", item.timestamp)
            })
        }
        prefs?.edit()?.putString(KEY_PINNED_CALCS, arr.toString())?.apply()
    }

    fun showPinResultDialog(activity: MainActivity, resultToPin: String) {
        if (resultToPin.isBlank()) {
            Toast.makeText(activity, "אין תוצאה לשמירה כרגע", Toast.LENGTH_SHORT).show()
            return
        }
        val box = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(activity.dp(20f), activity.dp(16f), activity.dp(20f), activity.dp(10f))
            setBackgroundColor(Utils.lightenColor(activity.bgColor, 0.08f))
        }
        val noteInput = activity.createInputBox("הוסף שם או הערה לחישוב זה (למשל: משכנתא דירה)...").apply {
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val previewTv = TextView(activity).apply {
            text = resultToPin
            setTextColor(Color.LTGRAY)
            textSize = 14f
            gravity = Gravity.RIGHT
            setPadding(0, activity.dp(6f), 0, 0)
        }
        box.addView(noteInput)
        box.addView(previewTv)

        AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("📌 שמירת תוצאה במועדפים")
            .setView(box)
            .setPositiveButton("שמור במועדפים") { _, _ ->
                val note = noteInput.text.toString().trim().ifEmpty { "חישוב שמור" }
                savePinnedCalculation(activity, activity.currentTabIndex, note, resultToPin)
            }
            .setNegativeButton("ביטול", null)
            .show()
    }
}

// סרגל שבבים אופקי לגישה מהירה לכל המחשבונים המועדפים
fun MainActivity.createFavoritesQuickChipsBar(): View {
    val favIndices = ProFavoritesEngine.getFavoriteTabIndices(prefs).sorted()
    val wrapper = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(10f) }
    }
    if (favIndices.isEmpty()) return wrapper

    val accent = if (isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else primaryColor
    val hScroll = HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
    }
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(2f), dp(2f), dp(2f), dp(2f))
    }

    for (idx in favIndices) {
        if (idx !in categories.indices) continue
        val isCurrent = (idx == currentTabIndex)
        val chip = TextView(this).apply {
            text = "⭐ ${IosGlassTheme.cleanCategoryTitle(categories[idx], true)}"
            setTextColor(if (isCurrent) Color.parseColor("#08090C") else Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(14f), dp(8f), dp(14f), dp(8f))
            background = GradientDrawable().apply {
                setColor(if (isCurrent) accent else Utils.lightenColor(bgColor, 0.10f))
                cornerRadius = dp(50f).toFloat()
                setStroke(dp(1f), accent)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(4f), 0, dp(4f), 0) }
            setOnClickListener {
                feedback(it)
                switchTab(idx)
            }
        }
        row.addView(chip)
    }
    hScroll.addView(row)
    wrapper.addView(hScroll)
    return wrapper
}

// מסך מרכז המועדפים והחישובים השמורים
fun MainActivity.createProFavoritesCenterScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20f), dp(12f), dp(20f), dp(24f))
    }
    root.addView(
        createProCanvaHeader(
            "⭐ מרכז המועדפים והחישובים השמורים",
            "גישה מהירה למחשבונים המועדפים שלך ולחישובים שנעצת עם הערות"
        )
    )

    val accent = if (isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else primaryColor
    val favSet = ProFavoritesEngine.getFavoriteTabIndices(prefs)

    val favSectionTitle = TextView(this).apply {
        text = "⭐ מחשבונים מועדפים (${favSet.size}):"
        setTextColor(accent)
        textSize = 16f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.RIGHT
        setPadding(0, dp(6f), 0, dp(8f))
    }
    root.addView(favSectionTitle)

    if (favSet.isEmpty()) {
        root.addView(TextView(this).apply {
            text = "טרם סימנת מחשבונים במועדפים.\nלחץ על סמל הכוכב (☆) בכותרת של כל מחשבון כדי להוסיף אותו לכאן!"
            setTextColor(Color.LTGRAY)
            textSize = 14.5f
            gravity = Gravity.RIGHT
            setPadding(0, 0, 0, dp(14f))
        })
    } else {
        for (idx in favSet.sorted()) {
            if (idx !in categories.indices) continue
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14f), dp(12f), dp(14f), dp(12f))
                background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createProFavoritesCenterScreen, 18f)
                else GradientDrawable().apply {
                    setColor(Utils.lightenColor(bgColor, 0.07f))
                    cornerRadius = dp(18f).toFloat()
                    setStroke(dp(1f), Utils.lightenColor(bgColor, 0.16f))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(8f) }
            }
            val removeBtn = TextView(this).apply {
                text = "🗑️ הסר"
                setTextColor(Color.parseColor("#FF5252"))
                textSize = 13.5f
                setPadding(dp(10f), dp(6f), dp(10f), dp(6f))
                setOnClickListener {
                    feedback(it)
                    ProFavoritesEngine.toggleFavoriteTab(this@createProFavoritesCenterScreen, idx)
                    switchTab(currentTabIndex)
                }
            }
            val openTv = TextView(this).apply {
                text = categories[idx]
                setTextColor(Color.WHITE)
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    feedback(it)
                    switchTab(idx)
                }
            }
            row.addView(removeBtn)
            row.addView(openTv)
            root.addView(row)
        }
    }

    val pinnedList = ProFavoritesEngine.getPinnedCalculations(prefs)
    val pinnedTitle = TextView(this).apply {
        text = "📌 תוצאות וחישובים נעוצים (${pinnedList.size}):"
        setTextColor(accent)
        textSize = 16f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.RIGHT
        setPadding(0, dp(16f), 0, dp(8f))
    }
    root.addView(pinnedTitle)

    if (pinnedList.isEmpty()) {
        root.addView(TextView(this).apply {
            text = "אין חישובים נעוצים עדיין. בכל מחשבון תוכל ללחוץ על '📌 נעץ תוצאה במועדפים' כדי לשמור חישוב חשוב להמשך."
            setTextColor(Color.LTGRAY)
            textSize = 14.5f
            gravity = Gravity.RIGHT
        })
    } else {
        for (item in pinnedList) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
                background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@createProFavoritesCenterScreen, 18f)
                else GradientDrawable().apply {
                    setColor(Utils.lightenColor(bgColor, 0.08f))
                    cornerRadius = dp(18f).toFloat()
                    setStroke(dp(1f), accent)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(10f) }
            }
            val topBar = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val delBtn = TextView(this).apply {
                text = "מחק ✕"
                setTextColor(Color.parseColor("#FF5252"))
                textSize = 13f
                setOnClickListener {
                    feedback(it)
                    ProFavoritesEngine.deletePinnedCalculation(prefs, item.id)
                    switchTab(currentTabIndex)
                }
            }
            val noteTv = TextView(this).apply {
                text = "📌 ${item.note} (${item.calculatorTitle})"
                setTextColor(accent)
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.RIGHT
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            topBar.addView(delBtn)
            topBar.addView(noteTv)

            val bodyTv = TextView(this).apply {
                text = "${item.resultText}\n🕒 ${item.timestamp}"
                setTextColor(Color.WHITE)
                textSize = 14.5f
                gravity = Gravity.RIGHT
                setPadding(0, dp(6f), 0, 0)
                setOnClickListener { copyToClipboard(item.resultText) }
            }
            card.addView(topBar)
            card.addView(bodyTv)
            root.addView(card)
        }
    }

    scroll.addView(root)
    return scroll
}
 

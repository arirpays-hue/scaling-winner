package com.example.calculator

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

// חלון היסטוריית חישובים מעוצב בזכוכית מלאה עם קווי הפרדה ואייקון וקטורי
fun MainActivity.showHistoryDialog() {
    if (historyList.isEmpty()) {
        Toast.makeText(this, "היסטוריית החישובים ריקה", Toast.LENGTH_SHORT).show()
        return
    }
    val activeAccent = if (isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else primaryColor
    if (isCleanGlassActive) IosGlassTheme.applyRealtimeGlassBlur(mainContent, true)

    val dialogView = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20f), dp(20f), dp(20f), dp(18f))
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@showHistoryDialog, 28f)
        else GradientDrawable().apply {
            setColor(Utils.lightenColor(bgColor, 0.06f))
            cornerRadius = dp(24f).toFloat()
            setStroke(dp(1f), Utils.lightenColor(bgColor, 0.16f))
        }
    }

    val headerRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
        setPadding(dp(4f), 0, dp(4f), dp(14f))
    }
    val titleTv = TextView(this).apply {
        text = if (isCleanGlassActive) "היסטוריית חישובים" else "🕒 היסטוריית חישובים"
        setTextColor(Color.WHITE)
        textSize = 20f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        compoundDrawablePadding = dp(10f)
        if (isCleanGlassActive) {
            val histIcon = AuroraBorealisTheme.createVectorIcon(this@showHistoryDialog, 101, activeAccent, 22f, isAuroraMode)
            setCompoundDrawables(null, null, histIcon, null)
        }
    }
    headerRow.addView(titleTv)
    dialogView.addView(headerRow)

    val scrollView = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(320f)) }
    val listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    scrollView.addView(listContainer)

    val dialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert).setView(dialogView).create()
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    dialog.setOnDismissListener { if (!isDrawerOpen) IosGlassTheme.applyRealtimeGlassBlur(mainContent, false) }

    for (entry in historyList) {
        val rowTv = TextView(this).apply {
            text = entry
            setTextColor(Color.WHITE)
            textSize = 17f
            gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
            setPadding(dp(14f), dp(14f), dp(14f), dp(14f))
            if (isAuroraMode) setShadowLayer(8f, 0f, 0f, Color.argb(90, 0, 245, 212))
            setOnClickListener {
                feedback(it)
                val selected = entry.substringAfter("=").trim()
                currentInput = selected
                justCalculated = true
                if (currentTabIndex != 0 && currentTabIndex != 1) switchTab(0) else currentView?.text = currentInput
                dialog.dismiss()
            }
        }
        listContainer.addView(rowTv)
        listContainer.addView(View(this).apply {
            setBackgroundColor(if (isAuroraMode) Color.argb(40, 0, 245, 212) else Color.argb(35, 255, 255, 255))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1f))
        })
    }

    val actionsRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(14f), 0, 0)
    }
    val clearBtn = createActionBtn("נקה הכל", false) {
        historyList.clear(); savePreferences()
        Toast.makeText(this, "נמחק", Toast.LENGTH_SHORT).show()
        dialog.dismiss()
    }.apply {
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(6f) }
    }
    val closeBtn = createActionBtn("סגור", true) { dialog.dismiss() }.apply {
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(6f) }
    }
    actionsRow.addView(clearBtn)
    actionsRow.addView(closeBtn)

    dialogView.addView(scrollView)
    dialogView.addView(actionsRow)
    dialog.show()
}

// חלון בחירת מטבע / יחידות בעיצוב זכוכית מלא עם אייקון חיפוש וקטורי
fun MainActivity.showItemPicker(title: String, items: List<String>, onSelected: (String) -> Unit) {
    val activeAccent = if (isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else primaryColor
    if (isCleanGlassActive) IosGlassTheme.applyRealtimeGlassBlur(mainContent, true)

    val dialogView = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18f), dp(20f), dp(18f), dp(18f))
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@showItemPicker, 28f)
        else GradientDrawable().apply {
            setColor(Utils.lightenColor(bgColor, 0.06f))
            cornerRadius = dp(24f).toFloat()
        }
    }

    val titleView = TextView(this).apply {
        text = IosGlassTheme.cleanCategoryTitle(title, isCleanGlassActive)
        setTextColor(Color.WHITE)
        textSize = 20f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.RIGHT
        setPadding(dp(4f), 0, dp(4f), dp(14f))
    }
    dialogView.addView(titleView)

    val searchInput = EditText(this).apply {
        hint = "חפש..."
        setHintTextColor(if (isAuroraMode) AuroraBorealisTheme.EXPRESSION_CYAN_GREY else Color.parseColor("#8A94A8"))
        setTextColor(Color.WHITE)
        gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        compoundDrawablePadding = dp(10f)
        setPadding(dp(14f), dp(12f), dp(14f), dp(12f))
        if (isCleanGlassActive) {
            val searchIcon = AuroraBorealisTheme.createVectorIcon(this@showItemPicker, 200, activeAccent, 18f, isAuroraMode)
            setCompoundDrawables(searchIcon, null, null, null)
        }
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@showItemPicker, 16f)
        else GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = dp(12f).toFloat()
            setStroke(dp(1f), Utils.lightenColor(bgColor, 0.15f))
        }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12f) }
    }

    val scrollView = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(400f)) }
    val listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    scrollView.addView(listContainer)

    val dialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert).setView(dialogView).create()
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    dialog.setOnDismissListener { if (!isDrawerOpen) IosGlassTheme.applyRealtimeGlassBlur(mainContent, false) }

    fun populateList(query: String) {
        listContainer.removeAllViews(); val filtered = items.filter { it.contains(query, ignoreCase = true) }
        for (curr in filtered) {
            val itemBtn = TextView(this).apply {
                text = curr
                setTextColor(Color.WHITE)
                textSize = 16f
                gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                setPadding(dp(16f), dp(15f), dp(16f), dp(15f))
                setOnClickListener { feedback(it); onSelected(curr); dialog.dismiss() }
            }
            listContainer.addView(itemBtn)
            listContainer.addView(View(this).apply {
                setBackgroundColor(if (isAuroraMode) Color.argb(38, 0, 245, 212) else if (isIosGlassMode) Color.argb(30, 255, 255, 255) else Utils.lightenColor(bgColor, 0.15f))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1f))
            })
        }
    }
    searchInput.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { populateList(s.toString()) }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
    populateList(""); dialogView.addView(searchInput); dialogView.addView(scrollView); dialog.show()
}

// חלון בחירת צליל בעיצוב זכוכית עם אייקוני גלי-קול וקטוריים מוארים (ללא אימוג'ים)
fun MainActivity.showGlassSoundPickerDialog(onSoundChanged: () -> Unit = {}) {
    val activeAccent = if (isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else primaryColor
    if (isCleanGlassActive) IosGlassTheme.applyRealtimeGlassBlur(mainContent, true)

    val dialogView = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20f), dp(20f), dp(20f), dp(18f))
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@showGlassSoundPickerDialog, 28f)
        else GradientDrawable().apply {
            setColor(Utils.lightenColor(bgColor, 0.06f))
            cornerRadius = dp(24f).toFloat()
        }
    }

    val titleTv = TextView(this).apply {
        text = "בחר צליל"
        setTextColor(Color.WHITE)
        textSize = 20f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.RIGHT
        setPadding(dp(4f), 0, dp(4f), dp(14f))
    }
    dialogView.addView(titleTv)

    val scrollView = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(340f)) }
    val listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    scrollView.addView(listContainer)

    val dialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert).setView(dialogView).create()
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    dialog.setOnDismissListener { if (!isDrawerOpen) IosGlassTheme.applyRealtimeGlassBlur(mainContent, false) }

    val options = mutableListOf("מערכת")
    for (i in 1..12) options.add("צליל $i")

    for ((idx, label) in options.withIndex()) {
        val isSelected = (soundType == idx)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14f), dp(14f), dp(14f), dp(14f))
            if (isSelected && isCleanGlassActive) {
                background = AuroraBorealisTheme.createActiveDrawerCapsule(this@showGlassSoundPickerDialog, isAuroraMode)
            }
            setOnClickListener {
                soundType = idx
                savePreferences()
                feedback(it)
                onSoundChanged()
                dialog.dismiss()
            }
        }

        val radioIndicator = RadioButton(this).apply {
            isChecked = isSelected
            isClickable = false
            buttonTintList = ColorStateList.valueOf(if (isSelected) activeAccent else Color.WHITE)
        }

        val itemTv = TextView(this).apply {
            text = if (isCleanGlassActive) label else if (idx == 0) "📱 מערכת" else "🎵 $label"
            setTextColor(if (isSelected) activeAccent else Color.WHITE)
            textSize = 17f
            typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            compoundDrawablePadding = dp(10f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            if (isCleanGlassActive) {
                val soundVec = AuroraBorealisTheme.createVectorIcon(this@showGlassSoundPickerDialog, 201, if (isSelected) activeAccent else Color.WHITE, 20f, isAuroraMode)
                setCompoundDrawables(null, null, soundVec, null)
            }
        }

        row.addView(radioIndicator)
        row.addView(itemTv)
        listContainer.addView(row)
        listContainer.addView(View(this).apply {
            setBackgroundColor(if (isAuroraMode) Color.argb(35, 0, 245, 212) else Color.argb(28, 255, 255, 255))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1f))
        })
    }

    dialogView.addView(scrollView)
    dialog.show()
}

// תפריט לבחירת מצב תצוגה (עיצוב רגיל, עיצוב הזכוכית, או הזוהר הצפוני) זהה במראה לתפריט הצלילים
fun MainActivity.showGlassDisplayModePickerDialog() {
    val activeAccent = if (isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else primaryColor
    if (isCleanGlassActive) IosGlassTheme.applyRealtimeGlassBlur(mainContent, true)

    val dialogView = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20f), dp(20f), dp(20f), dp(18f))
        background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@showGlassDisplayModePickerDialog, 28f)
        else GradientDrawable().apply {
            setColor(Utils.lightenColor(bgColor, 0.06f))
            cornerRadius = dp(24f).toFloat()
            setStroke(dp(1f), Utils.lightenColor(bgColor, 0.16f))
        }
    }

    val titleTv = TextView(this).apply {
        text = "בחר מצב תצוגה"
        setTextColor(Color.WHITE)
        textSize = 20f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.RIGHT
        setPadding(dp(4f), 0, dp(4f), dp(14f))
    }
    dialogView.addView(titleTv)

    val listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

    val dialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert).setView(dialogView).create()
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    dialog.setOnDismissListener {
        if (!isDrawerOpen || !isCleanGlassActive) {
            IosGlassTheme.applyRealtimeGlassBlur(mainContent, false)
        }
    }

    val currentModeIndex = when {
        isAuroraMode -> 2
        isIosGlassMode -> 1
        else -> 0
    }

    val modes = listOf(
        Triple("עיצוב רגיל (קלאסי)", "🎨 עיצוב רגיל (קלאסי)", 19),
        Triple("עיצוב הזכוכית (${IosGlassTheme.LIQUID_GLASS_THEME_NAME})", "💎 עיצוב הזכוכית (${IosGlassTheme.LIQUID_GLASS_THEME_NAME})", 203),
        Triple("הזוהר הצפוני (${IosGlassTheme.AURORA_THEME_NAME})", "🌌 הזוהר הצפוני (${IosGlassTheme.AURORA_THEME_NAME})", 202)
    )

    for ((idx, modeData) in modes.withIndex()) {
        val isSelected = (currentModeIndex == idx)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14f), dp(14f), dp(14f), dp(14f))
            if (isSelected && isCleanGlassActive) {
                background = AuroraBorealisTheme.createActiveDrawerCapsule(this@showGlassDisplayModePickerDialog, isAuroraMode)
            } else if (isSelected) {
                setBackgroundColor(Color.argb(45, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)))
            }
            setOnClickListener {
                feedback(it)
                IosGlassTheme.applyRealtimeGlassBlur(mainContent, false)
                when (idx) {
                    0 -> {
                        isIosGlassMode = false
                        isAuroraMode = false
                    }
                    1 -> {
                        isIosGlassMode = true
                        isAuroraMode = false
                        if (primaryColor == AuroraBorealisTheme.AURORA_PRIMARY || (primaryColor == Color.parseColor("#64B5F6") && bgColor == Color.parseColor("#08090C"))) {
                            primaryColor = Color.parseColor("#FF9F0A")
                            bgColor = Color.parseColor("#181920")
                        }
                    }
                    2 -> {
                        isAuroraMode = true
                        isIosGlassMode = false
                        primaryColor = AuroraBorealisTheme.AURORA_PRIMARY
                        bgColor = AuroraBorealisTheme.ARCTIC_BLUE
                    }
                }
                savePreferences()
                dialog.dismiss()
                applyTheme()
            }
        }

        val radioIndicator = RadioButton(this).apply {
            isChecked = isSelected
            isClickable = false
            buttonTintList = ColorStateList.valueOf(if (isSelected) activeAccent else Color.WHITE)
        }

        val itemTv = TextView(this).apply {
            text = if (isCleanGlassActive) modeData.first else modeData.second
            setTextColor(if (isSelected) activeAccent else Color.WHITE)
            textSize = 17f
            typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            compoundDrawablePadding = dp(10f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            if (isCleanGlassActive) {
                val modeIcon = AuroraBorealisTheme.createVectorIcon(this@showGlassDisplayModePickerDialog, modeData.third, if (isSelected) activeAccent else Color.WHITE, 20f, isAuroraMode)
                setCompoundDrawables(null, null, modeIcon, null)
            }
        }

        row.addView(radioIndicator)
        row.addView(itemTv)
        listContainer.addView(row)
        if (idx < modes.lastIndex) {
            listContainer.addView(View(this).apply {
                setBackgroundColor(if (isAuroraMode) Color.argb(35, 0, 245, 212) else Color.argb(28, 255, 255, 255))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1f))
            })
        }
    }

    dialogView.addView(listContainer)
    dialog.show()
}

// מעדכן באופן אוטומטי את השמות במסך ההגדרות ומוסיף אייקונים ודיאלוג צלילים ותצוגה מזכוכית
fun MainActivity.enhanceSettingsHierarchy(root: View) {
    when (root) {
        is TextView -> {
            val raw = root.text?.toString() ?: ""
            var updated = raw
                .replace("iOS 26 Liquid Glass", IosGlassTheme.LIQUID_GLASS_THEME_NAME)
                .replace("iOS Glass", "Liquid Glass")
            if (isCleanGlassActive) {
                updated = IosGlassTheme.cleanCategoryTitle(updated, true)
            }
            if (updated != raw) root.text = updated

            if (raw.contains("בחר צליל") || raw.contains("צליל מקשים") || raw.contains("סוג צליל")) {
                root.setOnClickListener {
                    feedback(it)
                    showGlassSoundPickerDialog { switchTab(19) }
                }
            }

            if (raw.contains("סגנון עיצוב:") || raw.contains("מצב תצוגה:")) {
                val activeModeTitle = when {
                    isAuroraMode -> IosGlassTheme.AURORA_THEME_NAME
                    isIosGlassMode -> "עיצוב הזכוכית (${IosGlassTheme.LIQUID_GLASS_THEME_NAME})"
                    else -> "עיצוב רגיל"
                }
                root.text = if (isCleanGlassActive) "מצב תצוגה: $activeModeTitle" else "✨ מצב תצוגה: $activeModeTitle"
                root.setOnClickListener {
                    feedback(it)
                    showGlassDisplayModePickerDialog()
                }
            }
        }
        is ViewGroup -> {
            for (i in 0 until root.childCount) {
                enhanceSettingsHierarchy(root.getChildAt(i))
            }
        }
    }
}
 

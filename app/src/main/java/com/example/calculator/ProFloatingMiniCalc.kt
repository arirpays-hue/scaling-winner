package com.example.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.min

// =========================================================================
// מחשבון מיני צף אוניברסלי התומך בכל 120 המחשבונים והכלים (ProFloatingMiniCalc)
// • Tab 0: מחשבון מיני רגיל עם מקלדת 4×5 מלאה
// • Tab 1: מחשבון מיני מדעי עם מקלדת 5×6 מלאה
// • Tabs 2..120: מארח חי של המסך הפעיל (תוצאות, קנבס, Presets, החלפת כיוון וטיימרים)
//   תוך הסתרה אוטומטית של שדות מקלדת מערכת + בורר החלפת סוג מחשבון צף מובנה
// =========================================================================
object ProFloatingMiniCalc {

    private data class MiniPalette(
        val cardBg: Int,
        val cardBorder: Int,
        val displayBg: Int,
        val displayBorder: Int,
        val displayText: Int,
        val exprText: Int,
        val headerIcon: Int,
        val chipBg: Int,
        val chipActiveBg: Int,
        val chipActiveText: Int,
        val bubbleBg: Int,
        val bubbleText: Int,
        val keyBgMap: Map<String, Int>,
        val keyTextMap: Map<String, Int>,
        val keyBorder: Int,
        val isMinimalist: Boolean
    )

    private class MaxHeightFrameLayout(
        context: Context,
        private val maxHeightPx: Int
    ) : FrameLayout(context) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val hMode = MeasureSpec.getMode(heightMeasureSpec)
            val hSize = MeasureSpec.getSize(heightMeasureSpec)
            val cappedSpec = when (hMode) {
                MeasureSpec.UNSPECIFIED -> MeasureSpec.makeMeasureSpec(maxHeightPx, MeasureSpec.AT_MOST)
                MeasureSpec.AT_MOST -> MeasureSpec.makeMeasureSpec(min(hSize, maxHeightPx), MeasureSpec.AT_MOST)
                else -> MeasureSpec.makeMeasureSpec(min(hSize, maxHeightPx), MeasureSpec.EXACTLY)
            }
            super.onMeasure(widthMeasureSpec, cappedSpec)
        }
    }

    private var windowManager: WindowManager? = null
    private var rootOverlay: FrameLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var hostActivityRef: WeakReference<MainActivity>? = null

    private var activeTabIndex: Int = 0
    private var currentInput: String = ""
    private var currentExpr: String = ""
    private var justCalculated: Boolean = false
    private var calcPrecision: Int = 8
    private var useHaptic: Boolean = true
    private var isCollapsedBubble: Boolean = false
    private var waitingForOverlayPermission: Boolean = false

    // ניהול השאלת מסך הכלי הפעיל מ-MainActivity והחזרתו בשלמותו
    private var hostedToolView: View? = null
    private var hostedOriginalLayoutParams: ViewGroup.LayoutParams? = null
    private val hiddenViewsOriginalVis = LinkedHashMap<View, Int>()

    // שכבת בורר פנימית (להחלפת סוג מחשבון או בחירת יחידה בלי Dialog של Activity)
    private var inlineModalContainer: FrameLayout? = null
    private var inlineBodyHolder: FrameLayout? = null
    private var headerTitleTv: TextView? = null
    private var miniCardRef: LinearLayout? = null

    val isShowing: Boolean
        get() = rootOverlay != null

    fun launchOrRequestPermission(activity: MainActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(activity)) {
            waitingForOverlayPermission = true
            Toast.makeText(
                activity,
                "יש לאשר 'הצגה מעל אפליקציות אחרות' כדי להפעיל את המחשבון המיני הצף",
                Toast.LENGTH_LONG
            ).show()
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${activity.packageName}")
                )
                activity.startActivity(intent)
            } catch (_: Throwable) {
                Toast.makeText(activity, "לא ניתן לפתוח את מסך ההרשאה במכשיר זה", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val startTab = if (activity.currentTabIndex == 19) 0 else activity.currentTabIndex
        showFloatingWindow(activity, startTab)
        activity.moveTaskToBack(true)
    }

    fun onHostActivityResumed(activity: MainActivity) {
        hostActivityRef = WeakReference(activity)

        if (waitingForOverlayPermission) {
            waitingForOverlayPermission = false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(activity)) {
                Toast.makeText(
                    activity,
                    "✓ ההרשאה אושרה! לחץ שוב על כפתור ההקטנה להפעלת החלון הצף",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        if (isShowing) {
            syncAndRestoreBeforeClose(activity, syncSwitchIfBasicOrSci = true)
            dismissInternal()
        }
    }

    fun dismiss() {
        val host = hostActivityRef?.get()
        if (host != null) {
            syncAndRestoreBeforeClose(host, syncSwitchIfBasicOrSci = false)
        } else {
            restoreHiddenViewsVisibility()
            hostedToolView = null
            hostedOriginalLayoutParams = null
        }
        dismissInternal()
    }

    private fun dismissInternal() {
        val view = rootOverlay ?: return
        try {
            windowManager?.removeViewImmediate(view)
        } catch (_: Throwable) {
            try {
                windowManager?.removeView(view)
            } catch (_: Throwable) {
            }
        }
        rootOverlay = null
        layoutParams = null
        inlineModalContainer = null
        inlineBodyHolder = null
        headerTitleTv = null
        miniCardRef = null
        isCollapsedBubble = false
    }

    private fun syncAndRestoreBeforeClose(activity: MainActivity, syncSwitchIfBasicOrSci: Boolean) {
        restoreHostedToolToActivity(activity)
        if (activeTabIndex == 0 || activeTabIndex == 1) {
            activity.currentInput = currentInput
            activity.justCalculated = justCalculated
            if (syncSwitchIfBasicOrSci && activity.currentTabIndex != activeTabIndex) {
                activity.switchTab(activeTabIndex)
            }
            activity.currentView?.text = if (currentInput.isEmpty()) "0" else currentInput
            if (currentExpr.isNotEmpty()) {
                activity.expressionView?.text = currentExpr
            }
        }
    }

    private fun showFloatingWindow(activity: MainActivity, initialTab: Int) {
        dismiss()

        val appCtx = activity.applicationContext
        val wm = appCtx.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        windowManager = wm
        hostActivityRef = WeakReference(activity)

        activeTabIndex = if (initialTab == 19) 0 else initialTab
        currentInput = activity.currentInput
        currentExpr = activity.expressionView?.text?.toString() ?: ""
        justCalculated = activity.justCalculated
        calcPrecision = activity.calcPrecision
        useHaptic = activity.useHaptic
        isCollapsedBubble = false

        val palette = captureThemePalette(activity)
        val dm = appCtx.resources.displayMetrics
        fun dp(v: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, dm).toInt()

        val cardWidthPx = computeCardWidthPx(dm, activeTabIndex)
        val approxHeightPx = dp(350f)
        val marginPx = dp(10f)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (activity.isRightHanded) (dm.widthPixels - cardWidthPx - marginPx).coerceAtLeast(0) else marginPx
            y = (dm.heightPixels - approxHeightPx - dp(92f)).coerceAtLeast(dp(44f))
        }
        layoutParams = params

        val outerWrapper = FrameLayout(appCtx).apply {
            setPadding(dp(4f), dp(4f), dp(4f), dp(4f))
        }

        // =========================================================================
        // 1. הכרטיס הצף הראשי (סרגל עליון + מעבר מהיר + גוף המחשבון/הכלי + שכבת בורר)
        // =========================================================================
        val miniCard = LinearLayout(appCtx).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(dp(8f), dp(6f), dp(8f), dp(8f))
            background = GradientDrawable().apply {
                cornerRadius = dp(20f).toFloat()
                setColor(palette.cardBg)
                setStroke(dp(1.4f), palette.cardBorder)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                elevation = dp(10f).toFloat()
            }
            layoutParams = FrameLayout.LayoutParams(cardWidthPx, FrameLayout.LayoutParams.WRAP_CONTENT)
        }
        miniCardRef = miniCard

        // =========================================================================
        // 2. בועה עגולה מוקטנת בקצה המסך
        // =========================================================================
        val collapsedBubble = TextView(appCtx).apply {
            text = if (activity.isCleanGlassActive) "⊞" else "🧮"
            textSize = 22f
            setTextColor(palette.bubbleText)
            gravity = Gravity.CENTER
            visibility = View.GONE
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(palette.bubbleBg)
                setStroke(dp(1.8f), palette.cardBorder)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                elevation = dp(10f).toFloat()
            }
            layoutParams = FrameLayout.LayoutParams(dp(54f), dp(54f))
        }

        // --- סרגל כותרת וגרירה עליון ---
        val headerBar = LinearLayout(appCtx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2f), 0, dp(2f), dp(4f))
        }

        fun makeHeaderBtn(label: String, onTap: () -> Unit): TextView = TextView(appCtx).apply {
            text = label
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(palette.headerIcon)
            gravity = Gravity.CENTER
            setPadding(dp(6f), dp(3f), dp(6f), dp(3f))
            setOnClickListener {
                if (useHaptic) it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onTap()
            }
        }

        val closeBtn = makeHeaderBtn("✕") {
            dismiss()
        }

        val collapseBtn = makeHeaderBtn("—") {
            isCollapsedBubble = true
            miniCard.visibility = View.GONE
            collapsedBubble.visibility = View.VISIBLE
            val bSize = dp(62f)
            val lp = layoutParams ?: return@makeHeaderBtn
            val curCardW = miniCard.width.takeIf { it > 0 } ?: cardWidthPx
            lp.x = if (lp.x + curCardW / 2 > dm.widthPixels / 2) {
                (dm.widthPixels - bSize - dp(8f)).coerceAtLeast(0)
            } else {
                dp(8f)
            }
            try {
                wm.updateViewLayout(outerWrapper, lp)
            } catch (_: Throwable) {
            }
        }

        val titleDragTv = TextView(appCtx).apply {
            text = shortToolTitle(activity, activeTabIndex)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(palette.headerIcon)
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(dp(4f), dp(4f), dp(4f), dp(4f))
            layoutParams = LinearLayout.LayoutParams(0, dp(26f), 1f)
        }
        headerTitleTv = titleDragTv

        val switchCalcBtn = makeHeaderBtn("⇄") {
            hostActivityRef?.get()?.let { showCalculatorTypeSwitcher(it, palette) }
        }

        val expandFullBtn = makeHeaderBtn("⤢") {
            val host = hostActivityRef?.get()
            if (host != null) {
                syncAndRestoreBeforeClose(host, syncSwitchIfBasicOrSci = true)
            }
            dismissInternal()
            val launchIntent = Intent(appCtx, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
            }
            appCtx.startActivity(launchIntent)
        }

        attachDragBehavior(titleDragTv, outerWrapper, wm, dm, dp(6f)) {
            hostActivityRef?.get()?.let { showCalculatorTypeSwitcher(it, palette) }
        }
        attachDragBehavior(collapsedBubble, outerWrapper, wm, dm, dp(6f)) {
            isCollapsedBubble = false
            collapsedBubble.visibility = View.GONE
            miniCard.visibility = View.VISIBLE
            val lp = layoutParams ?: return@attachDragBehavior
            val wNow = computeCardWidthPx(dm, activeTabIndex)
            lp.x = lp.x.coerceIn(0, (dm.widthPixels - wNow - dp(8f)).coerceAtLeast(0))
            lp.y = lp.y.coerceIn(dp(28f), (dm.heightPixels - approxHeightPx - dp(32f)).coerceAtLeast(dp(28f)))
            try {
                wm.updateViewLayout(outerWrapper, lp)
            } catch (_: Throwable) {
            }
        }

        headerBar.addView(closeBtn)
        headerBar.addView(collapseBtn)
        headerBar.addView(titleDragTv)
        headerBar.addView(switchCalcBtn)
        headerBar.addView(expandFullBtn)
        miniCard.addView(headerBar)

        // --- סרגל הפיכת סוג מחשבון מהיר (רגיל / מדעי / כל הכלים) ---
        val quickSwitchRow = LinearLayout(appCtx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(0, 0, 0, dp(6f))
        }
        miniCard.addView(quickSwitchRow)

        // --- גוף החלון הצף (מארח מחשבון רגיל / מדעי / כלי חי + שכבת בורר פנימית) ---
        val bodyStack = FrameLayout(appCtx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val bodyHolder = FrameLayout(appCtx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val modalLayer = FrameLayout(appCtx).apply {
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        inlineBodyHolder = bodyHolder
        inlineModalContainer = modalLayer
        bodyStack.addView(bodyHolder)
        bodyStack.addView(modalLayer)
        miniCard.addView(bodyStack)

        fun rebuildQuickSwitchStrip() {
            quickSwitchRow.removeAllViews()
            fun makeModeChip(lbl: String, isSelected: Boolean, onTap: () -> Unit): TextView {
                val bg = if (isSelected) palette.chipActiveBg else palette.chipBg
                val fg = if (isSelected) palette.chipActiveText else palette.headerIcon
                return TextView(appCtx).apply {
                    text = lbl
                    textSize = 11f
                    typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    setTextColor(fg)
                    gravity = Gravity.CENTER
                    setPadding(dp(8f), dp(4f), dp(8f), dp(4f))
                    background = GradientDrawable().apply {
                        cornerRadius = dp(999f).toFloat()
                        setColor(bg)
                        setStroke(dp(0.9f), if (isSelected) palette.chipActiveBg else palette.keyBorder)
                    }
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(dp(2f), 0, dp(2f), 0)
                    }
                    setOnClickListener {
                        if (useHaptic) it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onTap()
                    }
                }
            }

            quickSwitchRow.addView(makeModeChip("רגיל", activeTabIndex == 0) {
                hostActivityRef?.get()?.let { switchFloatingTab(it, 0, palette, dm, wm, outerWrapper, ::rebuildQuickSwitchStrip) }
            })
            quickSwitchRow.addView(makeModeChip("מדעי", activeTabIndex == 1) {
                hostActivityRef?.get()?.let { switchFloatingTab(it, 1, palette, dm, wm, outerWrapper, ::rebuildQuickSwitchStrip) }
            })
            val toolChipLabel = if (activeTabIndex >= 2) "⇄ החלף כלי" else "⇄ כלים"
            quickSwitchRow.addView(makeModeChip(toolChipLabel, activeTabIndex >= 2) {
                hostActivityRef?.get()?.let { showCalculatorTypeSwitcher(it, palette, ::rebuildQuickSwitchStrip) }
            })
        }

        outerWrapper.addView(miniCard)
        outerWrapper.addView(collapsedBubble)

        try {
            wm.addView(outerWrapper, params)
            rootOverlay = outerWrapper
            rebuildQuickSwitchStrip()
            renderFloatingBodyForTab(activity, activeTabIndex, palette, dm)
        } catch (_: Throwable) {
            Toast.makeText(activity, "לא ניתן להציג חלון צף — בדוק הרשאת 'הצגה מעל אפליקציות'", Toast.LENGTH_LONG).show()
        }
    }

    private fun computeCardWidthPx(dm: DisplayMetrics, tabIndex: Int): Int {
        fun dp(v: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, dm).toInt()
        val desiredDp = when (tabIndex) {
            0 -> 238f
            1 -> 256f
            else -> 282f
        }
        val maxFrac = if (tabIndex >= 2) 0.78f else 0.68f
        return min(dp(desiredDp), (dm.widthPixels * maxFrac).toInt().coerceAtLeast(dp(208f)))
    }

    private fun shortToolTitle(activity: MainActivity, tabIndex: Int): String {
        val raw = activity.categories.getOrElse(tabIndex) { "מחשבון" }
        return IosGlassTheme.cleanCategoryTitle(raw, true)
    }

    private fun switchFloatingTab(
        activity: MainActivity,
        newTabIndex: Int,
        palette: MiniPalette,
        dm: DisplayMetrics,
        wm: WindowManager,
        outerWrapper: View,
        onStripUpdated: (() -> Unit)? = null
    ) {
        if (newTabIndex == 19) return
        hideInlineModal()

        // השבת הכלי הקודם ל-Activity לפני החלפה
        restoreHostedToolToActivity(activity)

        activeTabIndex = newTabIndex
        if (newTabIndex >= 2 && activity.currentTabIndex != newTabIndex) {
            activity.switchTab(newTabIndex)
        }

        headerTitleTv?.text = shortToolTitle(activity, activeTabIndex)

        // התאמת רוחב החלון הצף לסוג הכלי (רגיל / מדעי / כלי תוצאות)
        val newW = computeCardWidthPx(dm, activeTabIndex)
        miniCardRef?.layoutParams = FrameLayout.LayoutParams(newW, FrameLayout.LayoutParams.WRAP_CONTENT)
        layoutParams?.let { lp ->
            lp.x = lp.x.coerceIn(0, (dm.widthPixels - newW - 12).coerceAtLeast(0))
            try {
                wm.updateViewLayout(outerWrapper, lp)
            } catch (_: Throwable) {
            }
        }

        onStripUpdated?.invoke()
        renderFloatingBodyForTab(activity, activeTabIndex, palette, dm)
    }

    private fun renderFloatingBodyForTab(
        activity: MainActivity,
        tabIndex: Int,
        palette: MiniPalette,
        dm: DisplayMetrics
    ) {
        val holder = inlineBodyHolder ?: return
        holder.removeAllViews()

        when (tabIndex) {
            0 -> holder.addView(buildKeypadCalculatorBody(activity.applicationContext, palette, dm, isScientific = false))
            1 -> holder.addView(buildKeypadCalculatorBody(activity.applicationContext, palette, dm, isScientific = true))
            else -> holder.addView(buildHostedToolResultsBody(activity, tabIndex, palette, dm))
        }
    }

    // =========================================================================
    // גוף א׳ + ב׳: מחשבון רגיל (4×5) ומחשבון מדעי (5×6) אינטראקטיביים מלאים
    // =========================================================================
    private fun buildKeypadCalculatorBody(
        appCtx: Context,
        palette: MiniPalette,
        dm: DisplayMetrics,
        isScientific: Boolean
    ): View {
        fun dp(v: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, dm).toInt()

        val container = LinearLayout(appCtx).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val displayCard = LinearLayout(appCtx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(dp(10f), dp(5f), dp(10f), dp(5f))
            background = GradientDrawable().apply {
                cornerRadius = dp(14f).toFloat()
                setColor(palette.displayBg)
                setStroke(dp(1f), palette.displayBorder)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(if (isScientific) 54f else 58f)
            ).apply { bottomMargin = dp(6f) }
        }

        val exprTv = TextView(appCtx).apply {
            text = currentExpr
            setTextColor(palette.exprText)
            textSize = 10.5f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.START
            gravity = Gravity.END
        }

        val inputTv = TextView(appCtx).apply {
            text = if (currentInput.isEmpty()) "0" else currentInput
            setTextColor(palette.displayText)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.START
            gravity = Gravity.END
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setAutoSizeTextTypeUniformWithConfiguration(12, 23, 1, TypedValue.COMPLEX_UNIT_SP)
            } else {
                textSize = 20f
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        displayCard.setOnClickListener {
            val txt = if (currentInput.isEmpty()) "0" else currentInput
            val cb = appCtx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cb?.setPrimaryClip(ClipData.newPlainText("MiniCalcResult", txt))
            if (useHaptic) it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            Toast.makeText(appCtx, "הועתק: $txt", Toast.LENGTH_SHORT).show()
        }

        displayCard.addView(exprTv)
        displayCard.addView(inputTv)
        container.addView(displayCard)

        fun refreshDisplay() {
            exprTv.text = currentExpr
            inputTv.text = if (currentInput.isEmpty()) "0" else currentInput
        }

        fun handleKey(sym: String) {
            if (justCalculated && sym !in listOf("÷", "×", "−", "+", "%", "^", "x²", "!", "=")) {
                currentInput = ""
                justCalculated = false
            }
            justCalculated = false

            when (sym) {
                "AC" -> {
                    currentInput = ""
                    currentExpr = ""
                }
                "⌫" -> {
                    if (currentInput.isNotEmpty()) currentInput = currentInput.dropLast(1)
                }
                "( )" -> {
                    val opens = currentInput.count { it == '(' }
                    val closes = currentInput.count { it == ')' }
                    val last = currentInput.lastOrNull()
                    currentInput += if (opens > closes && last != null && (last.isDigit() || last == ')' || last == 'π')) ")" else "("
                }
                "sin", "cos", "tan", "log", "ln" -> currentInput += "$sym("
                "√" -> currentInput += "√("
                "x²" -> currentInput += "^2"
                "=" -> {
                    if (currentInput.isNotEmpty()) {
                        try {
                            val res = Utils.evalMathExpression(currentInput)
                            val formatted = Utils.formatResult(res, calcPrecision)
                            currentExpr = "$currentInput ="
                            saveHistoryEntry(appCtx, "$currentInput = $formatted")
                            currentInput = formatted
                            justCalculated = true
                        } catch (_: Throwable) {
                            currentInput = "Error"
                            justCalculated = true
                        }
                    }
                }
                else -> currentInput += sym
            }
            refreshDisplay()
        }

        val rows = if (isScientific) {
            listOf(
                listOf("sin", "cos", "tan", "log", "ln"),
                listOf("√", "x²", "^", "π", "( )"),
                listOf("7", "8", "9", "⌫", "AC"),
                listOf("4", "5", "6", "×", "÷"),
                listOf("1", "2", "3", "+", "−"),
                listOf("0", ".", "%", "!", "=")
            )
        } else {
            listOf(
                listOf("AC", "⌫", "%", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "−"),
                listOf("1", "2", "3", "+"),
                listOf("( )", "0", ".", "=")
            )
        }

        val btnHeightPx = dp(if (isScientific) 34f else 38f)
        val btnMarginPx = dp(if (isScientific) 2f else 2.5f)
        val btnCornerPx = dp(if (isScientific) 10f else 12f).toFloat()

        for (rowSymbols in rows) {
            val rowLayout = LinearLayout(appCtx).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_LTR
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            for (sym in rowSymbols) {
                val lookupSym = when (sym) {
                    "( )" -> "("
                    "sin", "cos", "tan", "log", "ln", "√", "x²", "^", "π", "!" -> "sin"
                    else -> sym
                }
                val bgCol = palette.keyBgMap[lookupSym] ?: palette.displayBg
                val txtCol = palette.keyTextMap[lookupSym] ?: palette.displayText

                val bgShape = GradientDrawable().apply {
                    cornerRadius = btnCornerPx
                    setColor(bgCol)
                    if (sym != "=" && sym != "AC") {
                        setStroke(dp(0.9f), palette.keyBorder)
                    }
                }
                val ripple = RippleDrawable(
                    ColorStateList.valueOf(if (palette.isMinimalist) Color.parseColor("#1A000000") else Color.parseColor("#33FFFFFF")),
                    bgShape,
                    null
                )

                val btn = TextView(appCtx).apply {
                    text = sym
                    setTextColor(txtCol)
                    textSize = when {
                        sym.length >= 3 -> 11.5f
                        sym.length == 2 -> 12.5f
                        isScientific -> 14.5f
                        else -> 16.5f
                    }
                    typeface = if (sym in listOf("÷", "×", "−", "+", "=", "AC")) Typeface.DEFAULT_BOLD else Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    gravity = Gravity.CENTER
                    background = ripple
                    isClickable = true
                    layoutParams = LinearLayout.LayoutParams(0, btnHeightPx, 1f).apply {
                        setMargins(btnMarginPx, btnMarginPx, btnMarginPx, btnMarginPx)
                    }
                    setOnClickListener {
                        if (useHaptic) it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        handleKey(sym)
                    }
                }
                rowLayout.addView(btn)
            }
            container.addView(rowLayout)
        }

        return container
    }

    // =========================================================================
    // גוף ג׳: אירוח חי של כל אחד מ-118 הכלים והמחשבונים האחרים (Tabs 2..120)
    // במחשבונים שדורשים מקלדת מערכת: מסתיר EditText ומציג תוצאות + פקדים חיים
    // =========================================================================
    private fun buildHostedToolResultsBody(
        activity: MainActivity,
        tabIndex: Int,
        palette: MiniPalette,
        dm: DisplayMetrics
    ): View {
        val appCtx = activity.applicationContext
        fun dp(v: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, dm).toInt()

        val wrapper = LinearLayout(appCtx).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val toolView = detachActiveToolViewFromActivity(activity)
        if (toolView == null) {
            wrapper.addView(TextView(appCtx).apply {
                text = "לא נמצאה תצוגה פעילה לכלי זה — בחר מחשבון אחר למעלה"
                setTextColor(palette.displayText)
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(dp(12f), dp(24f), dp(12f), dp(24f))
            })
            return wrapper
        }

        // איפוס אנימציות כניסה כדי ששום רכיב לא יישאר שקוף (alpha=0) בחלון הצף
        resetEntranceAnimationsRecursively(toolView)

        // זיהוי והסתרת שדות מקלדת מערכת (EditText) תוך איסוף ערכי הקלט לתגית סיכום נקייה
        val collectedInputs = mutableListOf<String>()
        val hadKeyboardInputs = adaptKeyboardFieldsForMiniMode(toolView, collectedInputs)

        if (hadKeyboardInputs) {
            val summaryText = if (collectedInputs.isNotEmpty()) {
                "מצב תוצאות צף  •  קלט: ${collectedInputs.take(3).joinToString(" | ")}"
            } else {
                "מצב תוצאות צף (ללא מקלדת מערכת)"
            }
            val bannerTv = TextView(appCtx).apply {
                text = "\u200F$summaryText"
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(palette.exprText)
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setPadding(dp(8f), dp(4f), dp(8f), dp(4f))
                background = GradientDrawable().apply {
                    cornerRadius = dp(10f).toFloat()
                    setColor(palette.displayBg)
                    setStroke(dp(0.8f), palette.displayBorder)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(6f) }
            }
            wrapper.addView(bannerTv)
        }

        val maxToolHeightPx = min(dp(336f), (dm.heightPixels * 0.48f).toInt().coerceAtLeast(dp(220f)))
        val boundedHost = MaxHeightFrameLayout(appCtx, maxToolHeightPx).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(14f).toFloat()
                setColor(palette.displayBg)
                setStroke(dp(1f), palette.displayBorder)
            }
            setPadding(dp(2f), dp(2f), dp(2f), dp(2f))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        if (toolView is ScrollView) {
            toolView.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            boundedHost.addView(toolView)
        } else {
            val tempScroll = ScrollView(appCtx).apply {
                isFillViewport = true
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }
            toolView.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            tempScroll.addView(toolView)
            boundedHost.addView(tempScroll)
        }

        wrapper.addView(boundedHost)

        if (hadKeyboardInputs) {
            wrapper.addView(TextView(appCtx).apply {
                text = "להקלדה במקלדת לחץ ⤢ להרחבה • להחלפת מחשבון לחץ ⇄"
                textSize = 10f
                setTextColor(palette.exprText)
                gravity = Gravity.CENTER
                setPadding(dp(4f), dp(5f), dp(4f), 0)
            })
        }

        return wrapper
    }

    private fun detachActiveToolViewFromActivity(activity: MainActivity): View? {
        restoreHostedToolToActivity(activity)
        val cf = activity.contentFrame
        var candidate: View? = null
        for (i in cf.childCount - 1 downTo 0) {
            val ch = cf.getChildAt(i)
            if (ch.tag != "PRO_CANVAS_AMBIENT_VIEW") {
                candidate = ch
                break
            }
        }
        val target = candidate ?: return null
        hostedOriginalLayoutParams = target.layoutParams
        (target.parent as? ViewGroup)?.removeView(target)
        hostedToolView = target
        return target
    }

    private fun restoreHostedToolToActivity(activity: MainActivity) {
        restoreHiddenViewsVisibility()
        val tool = hostedToolView ?: return
        (tool.parent as? ViewGroup)?.removeView(tool)
        val cf = activity.contentFrame
        val restoreLp = hostedOriginalLayoutParams ?: FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        tool.layoutParams = restoreLp
        // מוסיפים בחזרה רק אם המסך עדיין תואם ולא הוחלף למסך אחר
        val alreadyHasTool = (0 until cf.childCount).any { cf.getChildAt(it).tag != "PRO_CANVAS_AMBIENT_VIEW" }
        if (!alreadyHasTool) {
            cf.addView(tool)
        }
        hostedToolView = null
        hostedOriginalLayoutParams = null
    }

    private fun restoreHiddenViewsVisibility() {
        for ((v, origVis) in hiddenViewsOriginalVis) {
            v.visibility = origVis
        }
        hiddenViewsOriginalVis.clear()
    }

    private fun resetEntranceAnimationsRecursively(v: View) {
        v.animate().cancel()
        v.alpha = 1f
        v.translationX = 0f
        v.translationY = 0f
        v.scaleX = 1f
        v.scaleY = 1f
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) {
                resetEntranceAnimationsRecursively(v.getChildAt(i))
            }
        }
    }

    private fun adaptKeyboardFieldsForMiniMode(root: View, outValues: MutableList<String>): Boolean {
        hiddenViewsOriginalVis.clear()
        var foundEditText = false

        fun hideAndTrack(v: View) {
            if (!hiddenViewsOriginalVis.containsKey(v)) {
                hiddenViewsOriginalVis[v] = v.visibility
            }
            v.visibility = View.GONE
        }

        fun walk(v: View) {
            if (v is EditText) {
                foundEditText = true
                val txt = v.text?.toString()?.trim().orEmpty()
                if (txt.isNotEmpty()) outValues.add(txt)
                hideAndTrack(v)
                return
            }
            if (v is ViewGroup) {
                var childEditCount = 0
                var visibleChildCount = 0
                for (i in 0 until v.childCount) {
                    val ch = v.getChildAt(i)
                    if (ch.visibility == View.VISIBLE) {
                        visibleChildCount++
                        if (ch is EditText) childEditCount++
                    }
                    walk(ch)
                }
                // אם שורה שלמה הכילה אך ורק שדות EditText (למשל שעות/דקות/שניות) — מסתירים גם אותה למניעת רווח ריק
                if (visibleChildCount > 0 && childEditCount == visibleChildCount && v !is ScrollView) {
                    hideAndTrack(v)
                }
            }
        }

        walk(root)
        return foundEditText
    }

    // =========================================================================
    // בורר החלפת סוג מחשבון + בורר יחידות פנימי (עובד מעל כל אפליקציה בלי Dialog)
    // =========================================================================
    fun showInlinePicker(
        activity: MainActivity,
        title: String,
        items: List<String>,
        onSelected: (String) -> Unit
    ) {
        val palette = captureThemePalette(activity)
        showInlineModalList(
            appCtx = activity.applicationContext,
            title = title,
            items = items,
            palette = palette
        ) { picked ->
            hideInlineModal()
            onSelected(picked)
        }
    }

    private fun showCalculatorTypeSwitcher(
        activity: MainActivity,
        palette: MiniPalette,
        onStripUpdated: (() -> Unit)? = null
    ) {
        val proActive = ProState.isProEnabled(activity.prefs)
        val entries = mutableListOf<Pair<Int, String>>()

        // 1. מחשבוני בסיס ומדעי תמיד בראש
        entries.add(0 to "🧮 מחשבון רגיל (מקלדת מובנית)")
        entries.add(1 to "🔬 מחשבון מדעי (מקלדת מובנית)")

        // 2. מועדפים (אם קיימים)
        for (idx in activity.categories.indices) {
            if (idx in listOf(0, 1, 19)) continue
            if (ProFavoritesEngine.isFavoriteTab(activity.prefs, idx) && activity.isCalculatorVisibleInDrawer(idx)) {
                entries.add(idx to "⭐ ${shortToolTitle(activity, idx)}")
            }
        }

        // 3. כל שאר המחשבונים והכלים הזמינים לפי קטגוריות
        val addedIndices = entries.map { it.first }.toMutableSet()
        for ((secCode, _, itemIndices) in activity.drawerSections) {
            if (secCode in 305..314 && !proActive) continue
            for (idx in itemIndices) {
                if (idx == 19 || idx in addedIndices) continue
                if (!activity.isCalculatorVisibleInDrawer(idx)) continue
                addedIndices.add(idx)
                entries.add(idx to shortToolTitle(activity, idx))
            }
        }

        val labels = entries.map { it.second }
        showInlineModalList(
            appCtx = activity.applicationContext,
            title = "החלף סוג מחשבון (${entries.size})",
            items = labels,
            palette = palette
        ) { pickedLabel ->
            val matchIdx = labels.indexOf(pickedLabel)
            if (matchIdx >= 0) {
                val targetTab = entries[matchIdx].first
                val wm = windowManager
                val ov = rootOverlay
                if (wm != null && ov != null) {
                    switchFloatingTab(
                        activity,
                        targetTab,
                        palette,
                        activity.applicationContext.resources.displayMetrics,
                        wm,
                        ov,
                        onStripUpdated
                    )
                }
            } else {
                hideInlineModal()
            }
        }
    }

    private fun showInlineModalList(
        appCtx: Context,
        title: String,
        items: List<String>,
        palette: MiniPalette,
        onPicked: (String) -> Unit
    ) {
        val modal = inlineModalContainer ?: return
        val dm = appCtx.resources.displayMetrics
        fun dp(v: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, dm).toInt()

        modal.removeAllViews()
        modal.visibility = View.VISIBLE

        val maxH = min(dp(340f), (dm.heightPixels * 0.48f).toInt().coerceAtLeast(dp(220f)))
        val sheet = LinearLayout(appCtx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8f), dp(8f), dp(8f), dp(8f))
            background = GradientDrawable().apply {
                cornerRadius = dp(16f).toFloat()
                setColor(palette.cardBg)
                setStroke(dp(1.2f), palette.cardBorder)
            }
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                maxH
            )
            isClickable = true
        }

        val topBar = LinearLayout(appCtx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(dp(2f), 0, dp(2f), dp(6f))
        }
        val backBtn = TextView(appCtx).apply {
            text = "✕"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(palette.headerIcon)
            setPadding(dp(6f), dp(2f), dp(8f), dp(2f))
            setOnClickListener {
                if (useHaptic) it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                hideInlineModal()
            }
        }
        val titleTv = TextView(appCtx).apply {
            text = "\u200F$title"
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(palette.displayText)
            gravity = Gravity.RIGHT
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        topBar.addView(backBtn)
        topBar.addView(titleTv)
        sheet.addView(topBar)

        val scroll = ScrollView(appCtx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val listCol = LinearLayout(appCtx).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(listCol)

        for (item in items) {
            val row = TextView(appCtx).apply {
                text = "\u200F$item"
                textSize = 13f
                setTextColor(palette.displayText)
                gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                setPadding(dp(10f), dp(9f), dp(10f), dp(9f))
                background = GradientDrawable().apply {
                    cornerRadius = dp(11f).toFloat()
                    setColor(palette.displayBg)
                    setStroke(dp(0.8f), palette.displayBorder)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(5f) }
                setOnClickListener {
                    if (useHaptic) it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onPicked(item)
                }
            }
            listCol.addView(row)
        }

        sheet.addView(scroll)
        modal.addView(sheet)
    }

    private fun hideInlineModal() {
        inlineModalContainer?.apply {
            removeAllViews()
            visibility = View.GONE
        }
    }

    // =========================================================================
    // גרירה חופשית של החלון הצף / הבועה
    // =========================================================================
    private fun attachDragBehavior(
        dragTarget: View,
        rootView: View,
        wm: WindowManager,
        dm: DisplayMetrics,
        touchSlopPx: Int,
        onTap: (() -> Unit)? = null
    ) {
        var initialX = 0
        var initialY = 0
        var downRawX = 0f
        var downRawY = 0f
        var moved = false

        dragTarget.setOnTouchListener { _, event ->
            val lp = layoutParams ?: return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = lp.x
                    initialY = lp.y
                    downRawX = event.rawX
                    downRawY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downRawX).toInt()
                    val dy = (event.rawY - downRawY).toInt()
                    if (abs(dx) > touchSlopPx || abs(dy) > touchSlopPx) {
                        moved = true
                    }
                    val curW = rootView.width.takeIf { it > 0 } ?: 220
                    val curH = rootView.height.takeIf { it > 0 } ?: 220
                    val maxX = (dm.widthPixels - curW).coerceAtLeast(0)
                    val maxY = (dm.heightPixels - curH).coerceAtLeast(0)
                    lp.x = (initialX + dx).coerceIn(0, maxX)
                    lp.y = (initialY + dy).coerceIn(0, maxY)
                    try {
                        wm.updateViewLayout(rootView, lp)
                    } catch (_: Throwable) {
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved && onTap != null) {
                        onTap()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun saveHistoryEntry(appCtx: Context, entry: String) {
        val host = hostActivityRef?.get()
        if (host != null) {
            host.historyList.add(0, entry)
            host.savePreferences()
            return
        }
        val prefs = appCtx.getSharedPreferences("CalcProPrefs", Context.MODE_PRIVATE)
        val prev = prefs.getString("history", "") ?: ""
        val list = if (prev.isNotEmpty()) prev.split("||").toMutableList() else mutableListOf()
        list.add(0, entry)
        val maxHist = if (ProState.isProEnabled(prefs)) 500 else 30
        prefs.edit().putString("history", list.take(maxHist).joinToString("||")).apply()
    }

    private fun captureThemePalette(activity: MainActivity): MiniPalette {
        val isMin = activity.isMinimalistMode
        val isAur = activity.isAuroraMode
        val isGlass = activity.isIosGlassMode

        val cardBg = when {
            isMin -> MinimalistTheme.getBackgroundColor(activity)
            isAur -> Color.argb(242, Color.red(AuroraBorealisTheme.ARCTIC_BLUE), Color.green(AuroraBorealisTheme.ARCTIC_BLUE), Color.blue(AuroraBorealisTheme.ARCTIC_BLUE))
            isGlass -> Color.argb(232, 22, 29, 44)
            else -> Color.argb(246, Color.red(activity.bgColor), Color.green(activity.bgColor), Color.blue(activity.bgColor))
        }
        val cardBorder = when {
            isMin -> MinimalistTheme.resolveBorderStrokeColor(activity, Color.parseColor("#DCD6CE"))
            isAur -> Color.argb(180, Color.red(AuroraBorealisTheme.AURORA_PRIMARY), Color.green(AuroraBorealisTheme.AURORA_PRIMARY), Color.blue(AuroraBorealisTheme.AURORA_PRIMARY))
            isGlass -> Color.argb(135, 255, 255, 255)
            else -> MinimalistTheme.resolveBorderStrokeColor(activity, activity.primaryColor)
        }
        val displayBg = when {
            isMin -> MinimalistTheme.CARD_WHITE
            isAur -> Color.argb(195, Color.red(AuroraBorealisTheme.EMERALD_NIGHT), Color.green(AuroraBorealisTheme.EMERALD_NIGHT), Color.blue(AuroraBorealisTheme.EMERALD_NIGHT))
            isGlass -> Color.argb(66, 255, 255, 255)
            else -> Utils.lightenColor(activity.bgColor, 0.08f)
        }
        val displayBorder = when {
            isMin -> Color.parseColor("#EAE5DE")
            isGlass -> Color.argb(90, 255, 255, 255)
            else -> Color.argb(75, Color.red(activity.primaryColor), Color.green(activity.primaryColor), Color.blue(activity.primaryColor))
        }
        val displayText = MinimalistTheme.ensureReadableTextOnBg(
            MinimalistTheme.resolveDisplayTextColor(activity),
            displayBg,
            2.3
        )
        val exprText = if (isMin) MinimalistTheme.TEXT_MUTED_GREY else Color.parseColor("#B0BEC5")
        val headerIcon = MinimalistTheme.ensureReadableTextOnBg(
            if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE,
            cardBg,
            2.3
        )
        val chipBg = when {
            isMin -> MinimalistTheme.CARD_WHITE
            isGlass -> Color.argb(55, 255, 255, 255)
            else -> Color.argb(38, 255, 255, 255)
        }
        val chipActiveBg = when {
            isMin -> MinimalistTheme.ACCENT_SAGE_GREEN
            isAur -> AuroraBorealisTheme.AURORA_PRIMARY
            else -> activity.primaryColor
        }
        val chipActiveText = MinimalistTheme.ensureReadableTextOnBg(
            if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.parseColor("#08090C"),
            chipActiveBg,
            2.3
        )
        val bubbleBg = chipActiveBg
        val bubbleText = MinimalistTheme.ensureReadableTextOnBg(Color.WHITE, bubbleBg, 2.3)

        val keys = listOf("AC", "⌫", "%", "÷", "×", "−", "+", "=", "(", ".", "sin", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
        val bgMap = mutableMapOf<String, Int>()
        val txtMap = mutableMapOf<String, Int>()
        for (k in keys) {
            val bg = when {
                isGlass -> when (k) {
                    "=", "+", "−", "×", "÷" -> Color.argb(220, Color.red(activity.primaryColor), Color.green(activity.primaryColor), Color.blue(activity.primaryColor))
                    "AC", "⌫", "%", "sin" -> Color.argb(95, 255, 255, 255)
                    else -> Color.argb(58, 255, 255, 255)
                }
                isAur -> when (k) {
                    "=", "+", "−", "×", "÷" -> AuroraBorealisTheme.AURORA_PRIMARY
                    "AC", "⌫", "%", "sin" -> Color.argb(90, Color.red(AuroraBorealisTheme.AURORA_PRIMARY), Color.green(AuroraBorealisTheme.AURORA_PRIMARY), Color.blue(AuroraBorealisTheme.AURORA_PRIMARY))
                    else -> Color.argb(175, 21, 38, 50)
                }
                else -> MinimalistTheme.resolveKeypadButtonBgColor(activity, k)
            }
            val fg = MinimalistTheme.resolveKeypadSymbolTextColor(activity, k, bg)
            bgMap[k] = bg
            txtMap[k] = fg
        }

        val keyBorder = when {
            isMin -> Color.argb(22, 44, 44, 44)
            isGlass -> Color.argb(65, 255, 255, 255)
            else -> Color.argb(40, 255, 255, 255)
        }

        return MiniPalette(
            cardBg = cardBg,
            cardBorder = cardBorder,
            displayBg = displayBg,
            displayBorder = displayBorder,
            displayText = displayText,
            exprText = exprText,
            headerIcon = headerIcon,
            chipBg = chipBg,
            chipActiveBg = chipActiveBg,
            chipActiveText = chipActiveText,
            bubbleBg = bubbleBg,
            bubbleText = bubbleText,
            keyBgMap = bgMap,
            keyTextMap = txtMap,
            keyBorder = keyBorder,
            isMinimalist = isMin
        )
    }
}

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
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.min

// =========================================================================
// מחשבון מיני צף ואינטראקטיבי בקצה המסך מעל כל אפליקציה (ProFloatingMiniCalc)
// מחליף את מצב ה-PiP הלא-לחיץ בחלון עבודה מלא המותאם לכל 4 ערכות הנושא
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
        val bubbleBg: Int,
        val bubbleText: Int,
        val keyBgMap: Map<String, Int>,
        val keyTextMap: Map<String, Int>,
        val keyBorder: Int,
        val isMinimalist: Boolean
    )

    private var windowManager: WindowManager? = null
    private var rootOverlay: FrameLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var hostActivityRef: WeakReference<MainActivity>? = null

    private var currentInput: String = ""
    private var currentExpr: String = ""
    private var justCalculated: Boolean = false
    private var calcPrecision: Int = 8
    private var useHaptic: Boolean = true
    private var isCollapsedBubble: Boolean = false
    private var waitingForOverlayPermission: Boolean = false

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

        showFloatingCalculator(activity)
        activity.moveTaskToBack(true)
    }

    fun onHostActivityResumed(activity: MainActivity) {
        hostActivityRef = WeakReference(activity)

        if (waitingForOverlayPermission) {
            waitingForOverlayPermission = false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(activity)) {
                Toast.makeText(
                    activity,
                    "✓ ההרשאה אושרה! לחץ שוב על כפתור ההקטנה להפעלת המחשבון הצף",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // כשחוזרים למחשבון המלא — סוגרים את המיני ומעבירים את התוצאה האחרונה למסך הראשי
        if (isShowing) {
            syncStateBackToActivity(activity)
            dismiss()
        }
    }

    fun dismiss() {
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
        isCollapsedBubble = false
    }

    private fun showFloatingCalculator(activity: MainActivity) {
        dismiss()

        val appCtx = activity.applicationContext
        val wm = appCtx.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        windowManager = wm
        hostActivityRef = WeakReference(activity)

        currentInput = activity.currentInput
        currentExpr = activity.expressionView?.text?.toString() ?: ""
        justCalculated = activity.justCalculated
        calcPrecision = activity.calcPrecision
        useHaptic = activity.useHaptic
        isCollapsedBubble = false

        val palette = captureThemePalette(activity)
        val dm = appCtx.resources.displayMetrics
        val d = dm.density
        fun dp(v: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, dm).toInt()

        val cardWidthPx = min(dp(236f), (dm.widthPixels * 0.64f).toInt().coerceAtLeast(dp(196f)))
        val approxHeightPx = dp(312f)
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
            y = (dm.heightPixels - approxHeightPx - dp(96f)).coerceAtLeast(dp(48f))
        }
        layoutParams = params

        val outerWrapper = FrameLayout(appCtx).apply {
            setPadding(dp(4f), dp(4f), dp(4f), dp(4f))
        }

        // =========================================================================
        // 1. מצב מורחב: מחשבון מיני מלא, קריא ולחיץ
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

        // =========================================================================
        // 2. מצב מכווץ: בועה עגולה קטנה בקצה המסך (נפתחת בלחיצה)
        // =========================================================================
        val collapsedBubble = TextView(appCtx).apply {
            text = "🧮"
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

        // --- סרגל עליון לגרירה, הרחבה למסך מלא, מזעור לבועה וסגירה ---
        val headerBar = LinearLayout(appCtx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2f), 0, dp(2f), dp(4f))
        }

        fun makeHeaderIconBtn(label: String, onTap: () -> Unit): TextView = TextView(appCtx).apply {
            text = label
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(palette.headerIcon)
            gravity = Gravity.CENTER
            setPadding(dp(7f), dp(3f), dp(7f), dp(3f))
            setOnClickListener {
                if (useHaptic) it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onTap()
            }
        }

        val closeBtn = makeHeaderIconBtn("✕") {
            hostActivityRef?.get()?.let { syncStateBackToActivity(it) }
            dismiss()
        }

        val expandFullBtn = makeHeaderIconBtn("⤢") {
            hostActivityRef?.get()?.let { syncStateBackToActivity(it) }
            dismiss()
            val launchIntent = Intent(appCtx, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
            }
            appCtx.startActivity(launchIntent)
        }

        val collapseBtn = makeHeaderIconBtn("—") {
            isCollapsedBubble = true
            miniCard.visibility = View.GONE
            collapsedBubble.visibility = View.VISIBLE
            val bSize = dp(62f)
            val lp = layoutParams ?: return@makeHeaderIconBtn
            lp.x = if (lp.x + cardWidthPx / 2 > dm.widthPixels / 2) {
                (dm.widthPixels - bSize - dp(8f)).coerceAtLeast(0)
            } else {
                dp(8f)
            }
            try {
                wm.updateViewLayout(outerWrapper, lp)
            } catch (_: Throwable) {
            }
        }

        val dragHandle = LinearLayout(appCtx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, dp(26f), 1f)
            addView(View(appCtx).apply {
                background = GradientDrawable().apply {
                    cornerRadius = dp(4f).toFloat()
                    setColor(Color.argb(95, Color.red(palette.headerIcon), Color.green(palette.headerIcon), Color.blue(palette.headerIcon)))
                }
                layoutParams = LinearLayout.LayoutParams(dp(34f), dp(4f))
            })
        }

        attachDragBehavior(dragHandle, outerWrapper, wm, dm, cardWidthPx, approxHeightPx, dp(6f))
        attachDragBehavior(collapsedBubble, outerWrapper, wm, dm, dp(62f), dp(62f), dp(6f)) {
            // לחיצה על הבועה פותחת בחזרה את המחשבון המיני
            isCollapsedBubble = false
            collapsedBubble.visibility = View.GONE
            miniCard.visibility = View.VISIBLE
            val lp = layoutParams ?: return@attachDragBehavior
            lp.x = lp.x.coerceIn(0, (dm.widthPixels - cardWidthPx - dp(8f)).coerceAtLeast(0))
            lp.y = lp.y.coerceIn(dp(28f), (dm.heightPixels - approxHeightPx - dp(32f)).coerceAtLeast(dp(28f)))
            try {
                wm.updateViewLayout(outerWrapper, lp)
            } catch (_: Throwable) {
            }
        }

        headerBar.addView(closeBtn)
        headerBar.addView(collapseBtn)
        headerBar.addView(dragHandle)
        headerBar.addView(expandFullBtn)
        miniCard.addView(headerBar)

        // --- צג התוצאה והתרגיל (חד, ברור, לחיץ להעתקה) ---
        val displayCard = LinearLayout(appCtx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(dp(10f), dp(6f), dp(10f), dp(6f))
            background = GradientDrawable().apply {
                cornerRadius = dp(14f).toFloat()
                setColor(palette.displayBg)
                setStroke(dp(1f), palette.displayBorder)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58f)
            ).apply { bottomMargin = dp(6f) }
        }

        val exprTv = TextView(appCtx).apply {
            text = currentExpr
            setTextColor(palette.exprText)
            textSize = 11f
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
                setAutoSizeTextTypeUniformWithConfiguration(13, 24, 1, TypedValue.COMPLEX_UNIT_SP)
            } else {
                textSize = 21f
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
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
        miniCard.addView(displayCard)

        fun refreshDisplay() {
            exprTv.text = currentExpr
            inputTv.text = if (currentInput.isEmpty()) "0" else currentInput
        }

        fun handleKey(sym: String) {
            if (justCalculated && sym !in listOf("÷", "×", "−", "+", "%", "=")) {
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
                    currentInput += if (opens > closes && last != null && (last.isDigit() || last == ')')) ")" else "("
                }
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

        // --- לוח מקשים מיני 4×5 ---
        val rows = listOf(
            listOf("AC", "⌫", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("( )", "0", ".", "=")
        )

        val btnHeightPx = dp(38f)
        val btnMarginPx = dp(2.5f)
        val btnCornerPx = dp(12f).toFloat()

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
                val lookupSym = if (sym == "( )") "(" else sym
                val bgCol = palette.keyBgMap[lookupSym] ?: palette.displayBg
                val txtCol = palette.keyTextMap[lookupSym] ?: palette.displayText

                val bgShape = GradientDrawable().apply {
                    cornerRadius = btnCornerPx
                    setColor(bgCol)
                    if (sym != "=" && sym != "AC") {
                        setStroke(dp(1f), palette.keyBorder)
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
                    textSize = if (sym.length > 1) 13.5f else 16.5f
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
            miniCard.addView(rowLayout)
        }

        outerWrapper.addView(miniCard)
        outerWrapper.addView(collapsedBubble)

        try {
            wm.addView(outerWrapper, params)
            rootOverlay = outerWrapper
        } catch (_: Throwable) {
            Toast.makeText(activity, "לא ניתן להציג חלון צף — בדוק הרשאת 'הצגה מעל אפליקציות'", Toast.LENGTH_LONG).show()
        }
    }

    private fun attachDragBehavior(
        dragTarget: View,
        rootView: View,
        wm: WindowManager,
        dm: android.util.DisplayMetrics,
        widthPx: Int,
        heightPx: Int,
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
                    val maxX = (dm.widthPixels - widthPx).coerceAtLeast(0)
                    val maxY = (dm.heightPixels - heightPx).coerceAtLeast(0)
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

    private fun syncStateBackToActivity(activity: MainActivity) {
        activity.currentInput = currentInput
        activity.justCalculated = justCalculated
        activity.currentView?.text = if (currentInput.isEmpty()) "0" else currentInput
        if (currentExpr.isNotEmpty()) {
            activity.expressionView?.text = currentExpr
        }
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
            isAur -> Color.argb(190, Color.red(AuroraBorealisTheme.EMERALD_NIGHT), Color.green(AuroraBorealisTheme.EMERALD_NIGHT), Color.blue(AuroraBorealisTheme.EMERALD_NIGHT))
            isGlass -> Color.argb(72, 255, 255, 255)
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
        val bubbleBg = when {
            isMin -> MinimalistTheme.ACCENT_SAGE_GREEN
            isAur -> AuroraBorealisTheme.AURORA_PRIMARY
            else -> activity.primaryColor
        }
        val bubbleText = MinimalistTheme.ensureReadableTextOnBg(Color.WHITE, bubbleBg, 2.3)

        val keys = listOf("AC", "⌫", "%", "÷", "×", "−", "+", "=", "(", ".", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
        val bgMap = mutableMapOf<String, Int>()
        val txtMap = mutableMapOf<String, Int>()
        for (k in keys) {
            val bg = when {
                isGlass -> when (k) {
                    "=", "+", "−", "×", "÷" -> Color.argb(220, Color.red(activity.primaryColor), Color.green(activity.primaryColor), Color.blue(activity.primaryColor))
                    "AC", "⌫", "%" -> Color.argb(95, 255, 255, 255)
                    else -> Color.argb(58, 255, 255, 255)
                }
                isAur -> when (k) {
                    "=", "+", "−", "×", "÷" -> AuroraBorealisTheme.AURORA_PRIMARY
                    "AC", "⌫", "%" -> Color.argb(90, Color.red(AuroraBorealisTheme.AURORA_PRIMARY), Color.green(AuroraBorealisTheme.AURORA_PRIMARY), Color.blue(AuroraBorealisTheme.AURORA_PRIMARY))
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
            bubbleBg = bubbleBg,
            bubbleText = bubbleText,
            keyBgMap = bgMap,
            keyTextMap = txtMap,
            keyBorder = keyBorder,
            isMinimalist = isMin
        )
    }
}

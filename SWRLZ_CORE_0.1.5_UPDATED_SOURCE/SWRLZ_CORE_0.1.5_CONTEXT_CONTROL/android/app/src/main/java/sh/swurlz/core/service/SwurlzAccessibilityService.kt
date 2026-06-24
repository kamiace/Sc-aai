package sh.swurlz.core.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import sh.swurlz.core.model.UiNode
import java.util.concurrent.atomic.AtomicReference

/**
 * The Local Operator's eyes + hands.
 *
 * - Snapshots the active window's accessibility node tree (text/desc/class/bounds/clickable/editable)
 * - Performs atomic actions: tap, scroll, type_text, back, home, wait, open_app
 * - Each visible node is fingerprinted as nX so the planner can address them by stable id.
 *
 * Important: the AccessibilityService is bound by the system once the user enables it
 * in Settings → Accessibility → Installed apps → SWRLZ-CORE Operator.
 */
class SwurlzAccessibilityService : AccessibilityService() {

    companion object {
        const val TAG = "SwurlzA11y"
        private val INSTANCE = AtomicReference<SwurlzAccessibilityService?>(null)
        fun get(): SwurlzAccessibilityService? = INSTANCE.get()
    }

    /** Map from synthetic node id -> live AccessibilityNodeInfo (only valid for current snapshot). */
    private val nodeIndex = mutableMapOf<String, AccessibilityNodeInfo>()
    private var lastPackage: String? = null
    private var lastWindowClass: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        INSTANCE.set(this)
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onDestroy() {
        INSTANCE.set(null)
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.packageName?.toString()?.let { lastPackage = it }
        event?.className?.toString()?.let { lastWindowClass = it }
    }

    override fun onInterrupt() {}

    /** Capture the current screen as a compact list of UiNode + meta. */
    fun snapshot(maxNodes: Int = 80): Snapshot {
        nodeIndex.clear()
        val root = rootInActiveWindow ?: return Snapshot(emptyList(), null, lastWindowClass, "no root window")
        val nodes = mutableListOf<UiNode>()
        var counter = 0
        fun walk(n: AccessibilityNodeInfo?, depth: Int) {
            if (n == null) return
            if (nodes.size >= maxNodes) return
            val text = n.text?.toString()
            val desc = n.contentDescription?.toString()
            val isInteractive = n.isClickable || n.isLongClickable || n.isEditable || (text != null) || (desc != null)
            if (isInteractive) {
                val id = "n${counter++}"
                val rect = Rect()
                n.getBoundsInScreen(rect)
                nodeIndex[id] = AccessibilityNodeInfo.obtain(n)
                nodes += UiNode(
                    id = id,
                    cls = n.className?.toString()?.substringAfterLast('.'),
                    text = text,
                    desc = desc,
                    pkg = n.packageName?.toString(),
                    clickable = n.isClickable,
                    editable = n.isEditable,
                    password = n.isPassword,
                    enabled = n.isEnabled,
                    checked = if (n.isCheckable) n.isChecked else null,
                    selected = n.isSelected,
                    scrollable = n.isScrollable,
                    bounds = listOf(rect.left, rect.top, rect.right, rect.bottom),
                )
            }
            for (i in 0 until n.childCount) walk(n.getChild(i), depth + 1)
        }
        walk(root, 0)
        val summary = "pkg=${root.packageName ?: "?"} nodes=${nodes.size}"
        return Snapshot(nodes, root.packageName?.toString(), lastWindowClass, summary)
    }

    data class Snapshot(
        val nodes: List<UiNode>,
        val pkg: String?,
        val windowClass: String?,
        val summary: String,
    )

    /** Tap a specific node by the id from the last snapshot. */
    fun tapNode(nodeId: String): Boolean {
        val n = nodeIndex[nodeId] ?: return false
        // climb to the nearest clickable ancestor if the leaf isn't clickable
        var cur: AccessibilityNodeInfo? = n
        while (cur != null && !cur.isClickable) cur = cur.parent
        val target = cur ?: n
        val ok = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        if (ok) return true
        // fall back to gesture at center of bounds
        val rect = Rect()
        n.getBoundsInScreen(rect)
        if (rect.width() == 0 || rect.height() == 0) return false
        return tapPoint(rect.centerX().toFloat(), rect.centerY().toFloat())
    }

    fun tapPoint(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 60)
        val g = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(g, null, null)
    }

    fun typeText(nodeId: String, text: String): Boolean {
        val n = nodeIndex[nodeId] ?: return false
        n.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun scrollScreen(direction: String): Boolean {
        val root = rootInActiveWindow ?: return false
        // Find first scrollable descendant
        var found: AccessibilityNodeInfo? = null
        fun walk(n: AccessibilityNodeInfo?) {
            if (n == null || found != null) return
            if (n.isScrollable) { found = n; return }
            for (i in 0 until n.childCount) walk(n.getChild(i))
        }
        walk(root)
        val node = found
        if (node != null) {
            val action = when (direction.lowercase()) {
                "up" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                "left" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                else -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            }
            if (node.performAction(action)) return true
        }
        // Fallback: synthesize swipe gesture
        val dm = resources.displayMetrics
        val cx = dm.widthPixels / 2f
        val cy = dm.heightPixels / 2f
        val d = dm.heightPixels * 0.35f
        val x1: Float; val y1: Float; val x2: Float; val y2: Float
        when (direction.lowercase()) {
            "up"    -> { x1 = cx; y1 = cy - d; x2 = cx; y2 = cy + d }
            "left"  -> { x1 = cx - d; y1 = cy; x2 = cx + d; y2 = cy }
            "right" -> { x1 = cx + d; y1 = cy; x2 = cx - d; y2 = cy }
            else    -> { x1 = cx; y1 = cy + d; x2 = cx; y2 = cy - d }
        }
        val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 280)
        val g = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(g, null, null)
    }

    fun pressBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun pressHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
}

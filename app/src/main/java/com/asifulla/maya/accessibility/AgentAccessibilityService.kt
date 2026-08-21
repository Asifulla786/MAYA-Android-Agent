package com.asifulla.maya.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AgentAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    companion object {
        @Volatile var instance: AgentAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        instance = null
        scope.cancel()
        super.onDestroy()
    }

    fun clickText(text: String, exact: Boolean = false): Boolean =
        findNode { node ->
            val value = node.text?.toString()
                ?: node.contentDescription?.toString()
                ?: return@findNode false
            if (exact) value == text else value.contains(text, ignoreCase = true)
        }?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true

    fun clickViewId(viewId: String): Boolean =
        findNode { it.viewIdResourceName == viewId }
            ?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true

    fun clickDescription(description: String): Boolean =
        findNode {
            it.contentDescription?.toString()?.contains(description, ignoreCase = true) == true
        }?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true

    fun typeText(text: String, humanized: Boolean = false): Boolean {
        val node = findNode { it.isFocused && it.isEditable }
            ?: findNode { it.isEditable }
            ?: return false

        if (!humanized) {
            return node.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        text
                    )
                }
            )
        }

        scope.launch {
            var current = node.text?.toString().orEmpty()
            for (character in text) {
                current += character
                node.performAction(
                    AccessibilityNodeInfo.ACTION_SET_TEXT,
                    Bundle().apply {
                        putCharSequence(
                            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                            current
                        )
                    }
                )
                delay(25)
            }
        }
        return true
    }

    fun tap(x: Float, y: Float, durationMs: Long = 80): Boolean {
        val path = Path().apply { moveTo(x, y) }
        return dispatchGesture(
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
                .build(),
            null,
            null
        )
    }

    fun swipe(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        durationMs: Long = 350
    ): Boolean {
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        return dispatchGesture(
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
                .build(),
            null,
            null
        )
    }

    fun global(action: Int): Boolean = performGlobalAction(action)

    private fun findNode(
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? = rootInActiveWindow?.let { walk(it, predicate) }

    private fun walk(
        node: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (predicate(node)) return node
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { child ->
                walk(child, predicate)?.let { return it }
            }
        }
        return null
    }
}

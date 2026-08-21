package com.asifulla.maya.agent

import com.asifulla.maya.accessibility.AgentAccessibilityService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Maps validated LLM tool calls to explicit native capabilities. Unknown tools fail closed. */
class ToolOrchestrator(private val accessibility: () -> AgentAccessibilityService?) {
    suspend fun execute(rawJson: String): ToolResult = runCatching {
        val obj = Json.parseToJsonElement(rawJson).jsonObject
        val name = obj["name"]?.jsonPrimitive?.content ?: error("missing tool name")
        val args = obj["arguments"]?.jsonObject ?: Json.parseToJsonElement("{}").jsonObject
        val service = accessibility() ?: return@runCatching ToolResult(false, "Accessibility service is not enabled")
        when (name) {
            "click_text" -> ToolResult(service.clickText(args["text"]?.jsonPrimitive?.content ?: error("text")), "click_text")
            "click_view_id" -> ToolResult(service.clickViewId(args["view_id"]?.jsonPrimitive?.content ?: error("view_id")), "click_view_id")
            "click_description" -> ToolResult(service.clickDescription(args["description"]?.jsonPrimitive?.content ?: error("description")), "click_description")
            "type_text" -> ToolResult(service.typeText(args["text"]?.jsonPrimitive?.content ?: error("text"), args["humanized"]?.jsonPrimitive?.content?.toBoolean() == true), "type_text")
            "tap" -> ToolResult(service.tap(args["x"]!!.jsonPrimitive.float, args["y"]!!.jsonPrimitive.float), "tap")
            "swipe" -> ToolResult(service.swipe(args["x1"]!!.jsonPrimitive.float, args["y1"]!!.jsonPrimitive.float, args["x2"]!!.jsonPrimitive.float, args["y2"]!!.jsonPrimitive.float), "swipe")
            "home" -> ToolResult(service.global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME), "home")
            "back" -> ToolResult(service.global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK), "back")
            "recents" -> ToolResult(service.global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS), "recents")
            "notifications" -> ToolResult(service.global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS), "notifications")
            else -> ToolResult(false, "Blocked/unknown tool: $name")
        }
    }.getOrElse { ToolResult(false, "Tool error: ${it.message}") }
}

data class ToolResult(val success: Boolean, val message: String)

package com.asifulla.maya.automation

import com.asifulla.maya.agent.ToolOrchestrator
import org.json.JSONArray
import org.json.JSONObject

class MacroExecutor(private val orchestrator: ToolOrchestrator) {
    suspend fun run(stepsJson: String): List<String> {
        val steps = JSONArray(stepsJson)
        val results = mutableListOf<String>()
        for (i in 0 until steps.length()) {
            val step = steps.getJSONObject(i)
            val tool = step.getString("tool")
            val args = step.optJSONObject("arguments") ?: JSONObject()
            val raw = JSONObject().put("name", tool).put("arguments", args).toString()
            val result = orchestrator.execute(raw)
            results += "$tool: ${result.message}"
            if (!result.success) break
        }
        return results
    }

    fun step(tool: String, arguments: JSONObject = JSONObject()): JSONObject =
        JSONObject().put("tool", tool).put("arguments", arguments)
}

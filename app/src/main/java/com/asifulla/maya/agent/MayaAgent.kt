package com.asifulla.maya.agent

import com.asifulla.maya.ai.OpenAIProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MayaAgent(
    private val provider: OpenAIProvider,
    private val orchestrator: ToolOrchestrator
) {
    suspend fun execute(command: String): String = withContext(Dispatchers.Default) {
        var prompt = """
            You are MAYA, a precise Android agent. You may use native UI tools only when needed.
            Never claim an action succeeded unless the native tool reports success.
            High-impact actions such as sending messages, calls, purchases, deletion, or SOS require explicit user confirmation outside this agent.
            User command: $command
        """.trimIndent()

        repeat(3) {
            val response = provider.runAgent(prompt)
            if (response.toolCalls.isEmpty()) return@withContext response.text.ifBlank { "I couldn't produce a response." }

            val results = response.toolCalls.map { call ->
                val safe = "{\"name\":${quote(call.name)},\"arguments\":${call.arguments}}"
                val result = orchestrator.execute(safe)
                "${call.name}: ${if (result.success) "SUCCESS" else "FAILED"} — ${result.message}"
            }

            prompt = """
                Original user request: $command
                Native tool execution results:
                ${results.joinToString("\n")}
                Continue the task if another safe UI action is genuinely required. Otherwise give the user a concise final result.
            """.trimIndent()
        }

        "I stopped after the safety execution limit. Please continue with a more specific command."
    }

    private fun quote(value: String): String = org.json.JSONObject.quote(value)
}

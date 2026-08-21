package com.asifulla.maya.ai

import com.asifulla.maya.agent.ToolSchemas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenAIProvider(private val apiKeyProvider: () -> String?) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun runAgent(userText: String, model: String = "gpt-5.6-luna"): AgentResponse = withContext(Dispatchers.IO) {
        val key = apiKeyProvider()?.trim().orEmpty()
        require(key.isNotEmpty()) { "OpenAI API key is not configured. Open Settings → AI Provider." }

        val tools = JSONArray(ToolSchemas.NATIVE_TOOLS)
        val normalizedTools = JSONArray()
        for (i in 0 until tools.length()) {
            val tool = tools.getJSONObject(i)
            normalizedTools.put(JSONObject()
                .put("type", "function")
                .put("name", tool.getString("name"))
                .put("description", tool.getString("description"))
                .put("parameters", tool.getJSONObject("parameters"))
                .put("strict", false))
        }

        val body = JSONObject()
            .put("model", model)
            .put("input", userText)
            .put("tools", normalizedTools)
            .put("tool_choice", "auto")
            .toString()

        val request = Request.Builder()
            .url("https://api.openai.com/v1/responses")
            .header("Authorization", "Bearer $key")
            .header("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("OpenAI ${response.code}: ${extractError(raw)}")
            }
            parseResponse(raw)
        }
    }

    private fun parseResponse(raw: String): AgentResponse {
        val root = JSONObject(raw)
        val output = root.optJSONArray("output") ?: JSONArray()
        val calls = mutableListOf<ToolCall>()
        val text = StringBuilder()

        for (i in 0 until output.length()) {
            val item = output.optJSONObject(i) ?: continue
            when (item.optString("type")) {
                "function_call" -> calls += ToolCall(
                    callId = item.optString("call_id"),
                    name = item.optString("name"),
                    arguments = item.optString("arguments", "{}")
                )
                "message" -> {
                    val content = item.optJSONArray("content") ?: JSONArray()
                    for (j in 0 until content.length()) {
                        val part = content.optJSONObject(j) ?: continue
                        if (part.optString("type") == "output_text") text.append(part.optString("text"))
                    }
                }
            }
        }
        return AgentResponse(text.toString().trim(), calls)
    }

    private fun extractError(raw: String): String = runCatching {
        JSONObject(raw).optJSONObject("error")?.optString("message") ?: raw.take(500)
    }.getOrDefault(raw.take(500))
}

data class AgentResponse(val text: String, val toolCalls: List<ToolCall>)
data class ToolCall(val callId: String, val name: String, val arguments: String)

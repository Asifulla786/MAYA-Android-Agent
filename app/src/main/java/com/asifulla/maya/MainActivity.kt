package com.asifulla.maya

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.asifulla.maya.accessibility.AgentAccessibilityService
import com.asifulla.maya.agent.MayaAgent
import com.asifulla.maya.agent.ToolOrchestrator
import com.asifulla.maya.ai.OpenAIProvider
import com.asifulla.maya.engine.MayaForegroundService
import com.asifulla.maya.overlay.MayaOrbController
import com.asifulla.maya.security.SecureConfig
import com.asifulla.maya.voice.VoiceGuardian
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )

        setContent {
            MaterialTheme {
                MayaScreen()
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun MayaScreen() {
        val scope = rememberCoroutineScope()
        val config = remember { SecureConfig(applicationContext) }
        var apiKey by remember { mutableStateOf(config.getProviderKey("openai").orEmpty()) }
        var command by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("Ready") }
        val messages = remember { mutableStateListOf<Pair<Boolean, String>>() }
        val orb = remember { MayaOrbController(this@MainActivity) }
        val provider = remember { OpenAIProvider { config.getProviderKey("openai") } }
        val orchestrator = remember {
            ToolOrchestrator(applicationContext) { AgentAccessibilityService.instance }
        }
        val agent = remember { MayaAgent(provider, orchestrator) }
        val voice = remember {
            VoiceGuardian(applicationContext) { spoken ->
                command = spoken
                messages.add(true to spoken)
                scope.launch {
                    status = "Thinking…"
                    runCatching { agent.execute(spoken) }
                        .onSuccess { reply ->
                            messages.add(false to reply)
                            status = "Ready"
                        }
                        .onFailure { error ->
                            val msg = error.message ?: "Unknown error"
                            messages.add(false to msg)
                            status = "Error"
                        }
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                voice.destroy()
                orb.hide()
            }
        }

        Column(
            Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("MAYA", style = MaterialTheme.typography.headlineLarge)
            Text("Agentic Android Assistant", style = MaterialTheme.typography.titleMedium)
            Text(status, color = MaterialTheme.colorScheme.primary)

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("AI Provider", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("OpenAI API key") },
                        placeholder = { Text("sk-…") }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            config.putProviderKey("openai", apiKey.trim())
                            status = if (apiKey.isBlank()) "API key removed" else "API key saved securely"
                        }) { Text("Save key") }
                        TextButton(onClick = {
                            config.removeProviderKey("openai")
                            apiKey = ""
                            status = "API key removed"
                        }) { Text("Clear") }
                    }
                    Text(
                        "The key is stored locally using Android encrypted preferences. Do not put it in GitHub source code.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages) { (user, text) ->
                    Card(Modifier.fillMaxWidth()) {
                        Text(
                            text = if (user) "You: $text" else "MAYA: $text",
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tell MAYA what to do") },
                minLines = 2
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = command.isNotBlank(),
                    onClick = {
                        val text = command.trim()
                        messages.add(true to text)
                        command = ""
                        scope.launch {
                            status = "Thinking…"
                            runCatching { agent.execute(text) }
                                .onSuccess { reply -> messages.add(false to reply); status = "Ready" }
                                .onFailure { messages.add(false to (it.message ?: "Failed")); status = "Error" }
                        }
                    }
                ) { Text("Run") }

                Button(onClick = {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        voice.startListening()
                        status = "Listening…"
                    } else {
                        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                    }
                }) { Text("Voice") }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, modifier = Modifier.weight(1f)) { Text("Accessibility") }
                OutlinedButton(onClick = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, modifier = Modifier.weight(1f)) { Text("Notifications") }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                }, modifier = Modifier.weight(1f)) { Text("Overlay") }
                OutlinedButton(onClick = {
                    ContextCompat.startForegroundService(this@MainActivity, Intent(this@MainActivity, MayaForegroundService::class.java))
                }, modifier = Modifier.weight(1f)) { Text("Start Agent") }
            }

            Button(onClick = {
                if (Settings.canDrawOverlays(this@MainActivity)) orb.show()
                else startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }, modifier = Modifier.fillMaxWidth()) { Text("Show Floating MAYA Orb") }

            Spacer(Modifier.height(2.dp))
        }
    }
}

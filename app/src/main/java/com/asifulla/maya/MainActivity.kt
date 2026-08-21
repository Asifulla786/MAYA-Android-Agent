package com.asifulla.maya

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.asifulla.maya.accessibility.AgentAccessibilityService
import com.asifulla.maya.agent.MayaAgent
import com.asifulla.maya.agent.ToolOrchestrator
import com.asifulla.maya.ai.OpenAIProvider
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
        requestRuntimePermissions()
        setContent { MaterialTheme { MayaApp() } }
    }

    private fun requestRuntimePermissions() {
        val permissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (android.os.Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    @Composable
    private fun MayaApp() {
        var page by remember { mutableStateOf("home") }
        if (page == "settings") SettingsScreen(onBack = { page = "home" })
        else HomeScreen(onSettings = { page = "settings" })
    }

    @Composable
    private fun HomeScreen(onSettings: () -> Unit) {
        val scope = rememberCoroutineScope()
        val config = remember { SecureConfig(applicationContext) }
        var command by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("Ready") }
        val messages = remember { mutableStateListOf<Pair<Boolean, String>>() }
        val orb = remember { MayaOrbController(this@MainActivity) }
        val provider = remember { OpenAIProvider { config.getProviderKey("openai") } }
        val orchestrator = remember { ToolOrchestrator(applicationContext) { AgentAccessibilityService.instance } }
        val agent = remember { MayaAgent(provider, orchestrator) }
        val voice = remember {
            VoiceGuardian(applicationContext) { spoken ->
                command = spoken
                messages.add(true to spoken)
                scope.launch {
                    status = "Thinking…"
                    runCatching { agent.execute(spoken) }
                        .onSuccess { reply -> messages.add(false to reply); status = "Ready" }
                        .onFailure { error -> messages.add(false to (error.message ?: "MAYA could not complete the task")); status = "Needs attention" }
                }
            }
        }

        DisposableEffect(Unit) { onDispose { voice.destroy(); orb.hide() } }

        Column(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant))).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("MAYA", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("Your agentic Android assistant", style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = onSettings) { Text("Settings") }
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(128.dp).background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)), CircleShape), contentAlignment = Alignment.Center) {
                        Text("M", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(status, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Tap Voice or type a command", style = MaterialTheme.typography.bodySmall)
                }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(messages) { (user, text) -> Card(Modifier.fillMaxWidth()) { Text(if (user) "You: $text" else "MAYA: $text", Modifier.padding(12.dp)) } }
            }
            OutlinedTextField(value = command, onValueChange = { command = it }, Modifier.fillMaxWidth(), label = { Text("Ask MAYA anything") }, placeholder = { Text("Hindi, Hinglish, Kannada, Urdu, English…") }, minLines = 2)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = command.isNotBlank(),
                    onClick = {
                        val text = command.trim()
                        command = ""
                        messages.add(true to text)
                        scope.launch {
                            status = "Thinking…"
                            runCatching { agent.execute(text) }
                                .onSuccess { messages.add(false to it); status = "Ready" }
                                .onFailure { messages.add(false to (it.message ?: "Failed")); status = "Needs attention" }
                        }
                    }
                ) { Text("Run") }
                Button(onClick = {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        voice.startListening()
                        status = "Listening…"
                    } else requestRuntimePermissions()
                }) { Text("Voice") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSettings, Modifier.weight(1f)) { Text("Permissions & AI") }
                OutlinedButton(onClick = { if (Settings.canDrawOverlays(this@MainActivity)) orb.show() else startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }, Modifier.weight(1f)) { Text("Show Orb") }
            }
        }
    }

    @Composable
    private fun SettingsScreen(onBack: () -> Unit) {
        val config = remember { SecureConfig(applicationContext) }
        val providers = listOf("openai", "gemini", "groq", "openrouter")
        val labels = mapOf("openai" to "OpenAI", "gemini" to "Google Gemini", "groq" to "Groq", "openrouter" to "OpenRouter")
        val input = remember { mutableStateMapOf<String, String>() }
        var status by remember { mutableStateOf("API keys are encrypted on this device") }
        var permissionRefresh by remember { mutableStateOf(0) }

        DisposableEffect(Unit) {
            val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) permissionRefresh++ }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }
        @Suppress("UNUSED_VARIABLE") val refresh = permissionRefresh

        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("MAYA Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = onBack) { Text("Home") }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Device readiness", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    PermissionRow("Microphone", ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                    PermissionRow("Floating window", Settings.canDrawOverlays(this@MainActivity))
                    PermissionRow("Accessibility", isAccessibilityEnabled())
                    PermissionRow("Notification access", isNotificationAccessEnabled())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { requestRuntimePermissions() }) { Text("Request") }
                        OutlinedButton(onClick = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("Accessibility") }
                        OutlinedButton(onClick = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) { Text("Notifications") }
                    }
                }
            }
            Text("AI Providers", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Saving a key does not call the AI API. Quota/data-limit errors appear only when MAYA sends a request.", style = MaterialTheme.typography.bodySmall)
            Text(status, style = MaterialTheme.typography.bodySmall)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(providers) { provider ->
                    val saved = config.hasProviderKey(provider)
                    val value = input[provider].orEmpty()
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(labels[provider].orEmpty(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(if (saved) "Saved ••••••••" else "Not configured", style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedTextField(value = value, onValueChange = { input[provider] = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("API key") }, placeholder = { Text(if (saved) "Enter a new key to replace it" else "Paste key here") }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(enabled = value.isNotBlank(), onClick = { config.putProviderKey(provider, value); input[provider] = ""; status = "${labels[provider]} key saved securely and hidden" }) { Text("Save") }
                                TextButton(enabled = saved, onClick = { config.removeProviderKey(provider); input[provider] = ""; status = "${labels[provider]} key removed" }) { Text("Remove") }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun PermissionRow(name: String, allowed: Boolean) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name)
            Text(if (allowed) "✓ Allowed" else "⚠ Required", color = if (allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        val expected = ComponentName(this, AgentAccessibilityService::class.java).flattenToString()
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun isNotificationAccessEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners").orEmpty()
        return enabled.contains(packageName, ignoreCase = true)
    }
}

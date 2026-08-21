package com.asifulla.maya.notifications

import android.app.RemoteInput
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MayaNotificationListener : NotificationListenerService() {
    data class IncomingMessage(val packageName: String, val title: String?, val text: String?, val replyAction: android.app.Notification.Action?)
    private val supported = setOf("com.whatsapp", "org.telegram.messenger", "com.google.android.apps.messaging")

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in supported) return
        val extras = sbn.notification.extras
        val title = extras.getString("android.title")
        val text = extras.getCharSequence("android.text")?.toString()
        val action = sbn.notification.actions?.firstOrNull { it.remoteInputs?.isNotEmpty() == true }
        latest = IncomingMessage(sbn.packageName, title, text, action)
    }

    fun reply(action: android.app.Notification.Action, message: String): Boolean {
        val input = action.remoteInputs?.firstOrNull() ?: return false
        val fill = android.os.Bundle().apply { putCharSequence(input.resultKey, message) }
        val intent = Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        RemoteInput.addResultsToIntent(arrayOf(input), intent, fill)
        return runCatching { action.actionIntent.send(this, 0, intent) }.isSuccess
    }

    companion object { @Volatile var latest: IncomingMessage? = null }
}

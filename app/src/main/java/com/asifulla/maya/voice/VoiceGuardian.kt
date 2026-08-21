package com.asifulla.maya.voice

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceGuardian(context: Context, private val onCommand: (String) -> Unit) : RecognitionListener {
    private val recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { it.setRecognitionListener(this) }
    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
    }
    private val tts = TextToSpeech(context) { if (it == TextToSpeech.SUCCESS) tts.language = Locale.getDefault() }
    @Volatile private var speaking = false

    fun startListening() { if (!speaking && SpeechRecognizer.isRecognitionAvailable(context)) recognizer.startListening(intent) }
    fun stopListening() = recognizer.stopListening()
    fun speak(text: String) { speaking = true; tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "maya_reply") }
    fun destroy() { recognizer.destroy(); tts.shutdown() }

    override fun onResults(results: android.os.Bundle?) { val text=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull(); if(!text.isNullOrBlank()) onCommand(text); startListening() }
    override fun onError(error: Int) { startListening() }
    override fun onBeginningOfSpeech() = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() { if(!speaking) startListening() }
    override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit
    override fun onPartialResults(partialResults: android.os.Bundle?) = Unit
    override fun onReadyForSpeech(params: android.os.Bundle?) = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
}

package com.asifulla.maya.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Voice front-end. Manual Voice mode is always available; background wake-word engines can be plugged in later. */
class VoiceGuardian(context: Context, private val onCommand: (String) -> Unit) : RecognitionListener {
    private val appContext = context.applicationContext
    private val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).also { it.setRecognitionListener(this) }
    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
    }
    private lateinit var tts: TextToSpeech
    @Volatile private var speaking = false
    @Volatile private var listening = false

    init {
        tts = TextToSpeech(appContext) { result ->
            if (result == TextToSpeech.SUCCESS) {
                tts.language = Locale.getDefault()
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { speaking = true }
                    override fun onDone(utteranceId: String?) { speaking = false }
                    override fun onError(utteranceId: String?) { speaking = false }
                })
            }
        }
    }

    fun startListening() {
        if (speaking || listening || !SpeechRecognizer.isRecognitionAvailable(appContext)) return
        listening = true
        recognizer.startListening(intent)
    }

    fun stopListening() {
        listening = false
        recognizer.stopListening()
    }

    fun speak(text: String) {
        speaking = true
        listening = false
        recognizer.cancel()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "maya_reply")
    }

    fun destroy() {
        recognizer.destroy()
        tts.shutdown()
    }

    override fun onResults(results: Bundle?) {
        listening = false
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim()
        if (text.isNotBlank()) onCommand(text)
    }

    override fun onError(error: Int) {
        listening = false
    }

    override fun onBeginningOfSpeech() = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() { listening = false }
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
    override fun onPartialResults(partialResults: Bundle?) = Unit
    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
}

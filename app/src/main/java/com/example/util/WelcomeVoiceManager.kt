package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class WelcomeVoiceManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = try {
        TextToSpeech(context.applicationContext, this)
    } catch (e: Exception) {
        Log.e("WelcomeVoiceManager", "TextToSpeech engine not available on this device", e)
        null
    }
    private var isInitialized = false

    override fun onInit(status: Int) {
        try {
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w("WelcomeVoiceManager", "Language US is not supported or missing data")
                } else {
                    isInitialized = true
                    tts?.setSpeechRate(0.95f)
                    tts?.setPitch(1.05f)
                }
            } else {
                Log.w("WelcomeVoiceManager", "TTS Initialization failed with status $status")
            }
        } catch (e: Exception) {
            Log.e("WelcomeVoiceManager", "Exception during onInit", e)
        }
    }

    fun speakAppWelcome() {
        val welcomeMessage = "Welcome to 24/7 SHOPPING! Shopping with ease and comfort. Explore our 3D spatial catalog and enjoy custom price bargaining."
        speakText(welcomeMessage)
    }

    fun speakCustomerSignIn(customerName: String) {
        val message = "Welcome to 24/7 SHOPPING, $customerName! Enjoy shopping with ease and comfort."
        speakText(message)
    }

    fun speakNewOrderAlert(customerName: String, totalAmountText: String, orderId: String) {
        val message = "Thank you $customerName! Order $orderId for $totalAmountText has been submitted for confirmation."
        speakText(message)
    }

    fun speakLowStockAlert(productTitle: String, remainingQuantity: Int) {
        val message = "Inventory alert! Low stock warning for $productTitle. Only $remainingQuantity units remaining."
        speakText(message)
    }

    fun speakExpiredProductAlert(productTitle: String) {
        val message = "Critical health and safety warning! Product $productTitle is expired. Immediate admin action required."
        speakText(message)
    }

    fun speakText(text: String) {
        try {
            if (isInitialized && tts != null) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "DEPROS_VOICE_ID")
            }
        } catch (e: Exception) {
            Log.e("WelcomeVoiceManager", "Error speaking text", e)
        }
    }

    fun stop() {
        try {
            if (isInitialized) {
                tts?.stop()
            }
        } catch (e: Exception) {
            Log.e("WelcomeVoiceManager", "Error stopping TTS", e)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("WelcomeVoiceManager", "Error shutting down TTS", e)
        }
    }
}

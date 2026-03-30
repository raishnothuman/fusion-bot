package com.fusionbot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FusionBotService : AccessibilityService() {

    companion object {
        var instance: FusionBotService? = null
        var isRunning: Boolean = false

        // Tap grid for 1080x1920 — covers the Fusion Blocks game board
        val tapPoints = listOf(
            180f to 420f,  540f to 420f,  900f to 420f,
            180f to 780f,  540f to 780f,  900f to 780f,
            180f to 1140f, 540f to 1140f, 900f to 1140f,
            180f to 1500f, 540f to 1500f, 900f to 1500f
        )
    }

    private var botJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { stop() }

    override fun onDestroy() {
        instance = null
        isRunning = false
        scope.cancel()
        super.onDestroy()
    }

    fun start(delayMs: Long, mode: String) {
        botJob?.cancel()
        isRunning = true
        botJob = scope.launch {
            while (isActive && isRunning) {
                when (mode) {
                    "tap" -> {
                        for ((x, y) in tapPoints) {
                            if (!isActive || !isRunning) break
                            tap(x, y)
                            delay(delayMs)
                        }
                    }
                    "swipe" -> {
                        swipe(180f, 1500f, 900f, 420f, 120)
                        delay(delayMs)
                        swipe(900f, 420f, 180f, 1500f, 120)
                        delay(delayMs)
                        swipe(180f, 420f, 900f, 1500f, 120)
                        delay(delayMs)
                        swipe(540f, 200f, 540f, 1700f, 120)
                        delay(delayMs)
                    }
                    else -> { // combo
                        for ((x, y) in tapPoints.take(6)) {
                            if (!isActive || !isRunning) break
                            tap(x, y)
                            delay(maxOf(delayMs / 2, 30L))
                        }
                        swipe(180f, 1500f, 900f, 420f, 100)
                        delay(delayMs)
                        swipe(180f, 420f, 900f, 1500f, 100)
                        delay(delayMs)
                    }
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        botJob?.cancel()
        botJob = null
    }

    private suspend fun tap(x: Float, y: Float) = suspendCoroutine<Unit> { cont ->
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 50L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(g: GestureDescription?) = cont.resume(Unit)
            override fun onCancelled(g: GestureDescription?) = cont.resume(Unit)
        }, null)
    }

    private fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long) {
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }
}

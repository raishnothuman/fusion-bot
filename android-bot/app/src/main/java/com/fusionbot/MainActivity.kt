package com.fusionbot

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.fusionbot.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    private val speedOptions = listOf(
        "Slow" to 800L,
        "Medium" to 400L,
        "Fast" to 150L,
        "Turbo" to 60L,
        "MAX" to 20L
    )
    private var selectedSpeed = 2 // Fast default
    private var selectedMode = "combo"

    private val statusChecker = object : Runnable {
        override fun run() {
            updateUI()
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpeedButtons()
        setupModeButtons()

        binding.btnStartStop.setOnClickListener {
            if (!isAccessibilityEnabled()) {
                openAccessibilitySettings()
                return@setOnClickListener
            }
            if (running) stopBot() else startBot()
        }

        binding.btnAccessibility.setOnClickListener {
            openAccessibilitySettings()
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(statusChecker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(statusChecker)
    }

    private fun setupSpeedButtons() {
        val buttons = listOf(
            binding.btnSlow, binding.btnMedium,
            binding.btnFast, binding.btnTurbo, binding.btnMax
        )
        buttons.forEachIndexed { i, btn ->
            btn.text = speedOptions[i].first
            btn.setOnClickListener {
                selectedSpeed = i
                buttons.forEach { b -> b.isSelected = false }
                btn.isSelected = true
            }
        }
        buttons[selectedSpeed].isSelected = true
    }

    private fun setupModeButtons() {
        listOf(
            binding.btnModeTap to "tap",
            binding.btnModeSwipe to "swipe",
            binding.btnModeCombo to "combo"
        ).forEach { (btn, mode) ->
            btn.setOnClickListener {
                selectedMode = mode
                binding.btnModeTap.isSelected = false
                binding.btnModeSwipe.isSelected = false
                binding.btnModeCombo.isSelected = false
                btn.isSelected = true
            }
        }
        binding.btnModeCombo.isSelected = true
    }

    private fun startBot() {
        val delay = speedOptions[selectedSpeed].second
        FusionBotService.instance?.start(delay, selectedMode)
        running = true
        updateUI()
    }

    private fun stopBot() {
        FusionBotService.instance?.stop()
        running = false
        updateUI()
    }

    private fun updateUI() {
        val serviceEnabled = isAccessibilityEnabled()
        val serviceConnected = FusionBotService.instance != null

        running = FusionBotService.isRunning

        if (!serviceEnabled) {
            binding.tvStatus.text = "⚠ Enable Accessibility Service below"
            binding.tvStatus.setTextColor(0xFFFFB300.toInt())
            binding.btnAccessibility.visibility = View.VISIBLE
            binding.btnStartStop.text = "ENABLE SERVICE FIRST"
            binding.btnStartStop.isSelected = false
        } else if (!serviceConnected) {
            binding.tvStatus.text = "Service enabled — reopen this app"
            binding.tvStatus.setTextColor(0xFF00E5FF.toInt())
            binding.btnAccessibility.visibility = View.GONE
            binding.btnStartStop.text = "WAITING..."
            binding.btnStartStop.isSelected = false
        } else if (running) {
            binding.tvStatus.text = "● BOT RUNNING — switch to Fusion Blocks"
            binding.tvStatus.setTextColor(0xFF00E676.toInt())
            binding.btnAccessibility.visibility = View.GONE
            binding.btnStartStop.text = "■  STOP"
            binding.btnStartStop.isSelected = true
        } else {
            binding.tvStatus.text = "Ready — tap START, then switch to the game"
            binding.tvStatus.setTextColor(0xFF00E5FF.toInt())
            binding.btnAccessibility.visibility = View.GONE
            binding.btnStartStop.text = "▶  START"
            binding.btnStartStop.isSelected = false
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expectedComp = ComponentName(this, FusionBotService::class.java)
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.equals(expectedComp.flattenToString(), ignoreCase = true) }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}

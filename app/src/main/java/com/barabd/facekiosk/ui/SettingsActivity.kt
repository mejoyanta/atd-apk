package com.barabd.facekiosk.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.barabd.facekiosk.FaceKioskApp
import com.barabd.facekiosk.databinding.ActivitySettingsBinding
import com.barabd.facekiosk.data.PunchCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private val app by lazy { application as FaceKioskApp }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.inputServerUrl.setText(app.prefs.serverBaseUrl)
        binding.inputDeviceId.setText(app.prefs.deviceId)
        binding.inputTerminalSn.setText(app.prefs.terminalSn)
        binding.switchCheckIn.isChecked = app.prefs.punchAsCheckIn

        binding.btnSave.setOnClickListener {
            savePrefs()
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        }

        binding.btnChangePin.setOnClickListener {
            PinDialog.requirePin(this, forceCreate = true) {
                Toast.makeText(this, "PIN updated", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnTest.setOnClickListener {
            savePrefs()
            binding.statusText.text = "Testing…"
            lifecycleScope.launch {
                val health = withContext(Dispatchers.IO) { app.apiClient.health() }
                if (!health.ok) {
                    binding.statusText.text = "Health failed: ${health.message}\n${health.body}"
                    return@launch
                }
                val punch = withContext(Dispatchers.IO) {
                    app.apiClient.punch(
                        personId = "TEST",
                        time = PunchCoordinator.formatNow(),
                        similarity = 0.99
                    )
                }
                binding.statusText.text = buildString {
                    append("Health: ${health.message}\n")
                    append(health.body.take(200))
                    append("\n\nTest punch: ${punch.message} (HTTP ${punch.code})")
                    if (punch.body.isNotBlank()) {
                        append("\n")
                        append(punch.body.take(200))
                    }
                }
            }
        }
    }

    private fun savePrefs() {
        app.prefs.serverBaseUrl = binding.inputServerUrl.text?.toString().orEmpty()
        app.prefs.deviceId = binding.inputDeviceId.text?.toString().orEmpty()
        app.prefs.terminalSn = binding.inputTerminalSn.text?.toString().orEmpty()
        app.prefs.punchAsCheckIn = binding.switchCheckIn.isChecked
    }
}

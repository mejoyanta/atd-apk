package com.barabd.facekiosk.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.barabd.facekiosk.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private var pendingCameraTarget: Class<*>? = null

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val target = pendingCameraTarget
        pendingCameraTarget = null
        if (granted && target != null) {
            startActivity(Intent(this, target))
        } else if (!granted) {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSettings.setOnClickListener {
            PinDialog.requirePin(this) {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }

        binding.btnEnroll.setOnClickListener {
            PinDialog.requirePin(this) {
                ensureCamera(EnrollActivity::class.java)
            }
        }

        binding.btnPeople.setOnClickListener {
            PinDialog.requirePin(this) {
                startActivity(Intent(this, PeopleActivity::class.java))
            }
        }

        binding.btnStart.setOnClickListener {
            ensureCamera(IdentifyActivity::class.java)
        }
    }

    private fun ensureCamera(target: Class<*>) {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED -> {
                startActivity(Intent(this, target))
            }
            else -> {
                pendingCameraTarget = target
                cameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }
}

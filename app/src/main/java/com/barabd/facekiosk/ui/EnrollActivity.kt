package com.barabd.facekiosk.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.barabd.facekiosk.FaceKioskApp
import com.barabd.facekiosk.camera.CameraController
import com.barabd.facekiosk.databinding.ActivityEnrollBinding
import com.barabd.facekiosk.settings.KioskPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EnrollActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEnrollBinding
    private val app by lazy { application as FaceKioskApp }
    private var camera: CameraController? = null
    private val captured = mutableListOf<FloatArray>()
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnrollBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!app.facePipeline.modelsReady) {
            binding.statusText.text = getString(com.barabd.facekiosk.R.string.models_missing)
        } else {
            binding.statusText.text = "Shots 0/${KioskPreferences.ENROLL_SHOTS} — face the camera"
        }

        binding.btnCapture.setOnClickListener {
            if (busy) return@setOnClickListener
            val code = binding.inputCode.text?.toString()?.trim().orEmpty()
            val name = binding.inputName.text?.toString()?.trim().orEmpty()
            if (code.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, "Enter attendance code and name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!app.facePipeline.modelsReady) {
                Toast.makeText(this, getString(com.barabd.facekiosk.R.string.models_missing), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            // Trigger one-shot: next frame will be processed
            pendingCapture = true
            binding.statusText.text = "Hold still…"
        }
    }

    private var pendingCapture = false

    override fun onStart() {
        super.onStart()
        camera = CameraController(this, this, binding.previewView).also { cam ->
            cam.onFrame = { bitmap ->
                if (!pendingCapture || busy) return@onFrame
                pendingCapture = false
                busy = true
                lifecycleScope.launch {
                    val result = withContext(Dispatchers.Default) {
                        app.facePipeline.extractForEnroll(bitmap)
                    }
                    busy = false
                    result.onSuccess { embedding ->
                        captured.add(embedding)
                        val n = captured.size
                        binding.statusText.text = "Shots $n/${KioskPreferences.ENROLL_SHOTS}"
                        if (n >= KioskPreferences.ENROLL_SHOTS) {
                            finishEnroll()
                        } else {
                            Toast.makeText(this@EnrollActivity, "Captured $n", Toast.LENGTH_SHORT).show()
                        }
                    }.onFailure {
                        binding.statusText.text = it.message ?: "Capture failed"
                    }
                }
            }
            lifecycleScope.launch {
                cam.start(useFront = true)
            }
        }
    }

    private fun finishEnroll() {
        val code = binding.inputCode.text?.toString()?.trim().orEmpty()
        val name = binding.inputName.text?.toString()?.trim().orEmpty()
        val avg = app.facePipeline.matcher.averageEmbeddings(captured.toList())
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                app.personRepository.save(code, name, avg, null)
            }
            Toast.makeText(this@EnrollActivity, "Enrolled $name ($code)", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onStop() {
        camera?.shutdown()
        camera = null
        super.onStop()
    }
}

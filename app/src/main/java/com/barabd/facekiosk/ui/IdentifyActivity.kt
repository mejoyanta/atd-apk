package com.barabd.facekiosk.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.barabd.facekiosk.FaceKioskApp
import com.barabd.facekiosk.R
import com.barabd.facekiosk.camera.CameraController
import com.barabd.facekiosk.data.PunchCoordinator
import com.barabd.facekiosk.data.PunchSubmitResult
import com.barabd.facekiosk.databinding.ActivityIdentifyBinding
import com.barabd.facekiosk.ml.PipelineResult
import com.barabd.facekiosk.ml.RejectReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IdentifyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIdentifyBinding
    private val app by lazy { application as FaceKioskApp }
    private var camera: CameraController? = null
    private lateinit var punchCoordinator: PunchCoordinator
    private val mainHandler = Handler(Looper.getMainLooper())
    private var busy = false
    private var lastUiMessageAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityIdentifyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        punchCoordinator = PunchCoordinator(app.prefs, app.apiClient, app.outboxRepository)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                requestExit()
            }
        })

        binding.btnExit.setOnClickListener { requestExit() }

        if (!app.facePipeline.modelsReady) {
            showBanner(getString(R.string.models_missing), R.color.warning)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            punchCoordinator.flushPending()
        }
    }

    private fun requestExit() {
        PinDialog.requirePin(this, title = getString(R.string.exit_kiosk), onSuccess = {
            finish()
        })
    }

    override fun onStart() {
        super.onStart()
        camera = CameraController(this, this, binding.previewView).also { cam ->
            cam.onFrame = frame@{ bitmap ->
                if (busy || !app.facePipeline.modelsReady) return@frame
                busy = true
                lifecycleScope.launch {
                    val people = withContext(Dispatchers.IO) { app.personRepository.all() }
                    val result = withContext(Dispatchers.Default) {
                        app.facePipeline.identify(bitmap, people, requireLiveness = true)
                    }
                    handleResult(result)
                    // Brief pause between attempts
                    withContext(Dispatchers.IO) { Thread.sleep(800) }
                    busy = false
                }
            }
            lifecycleScope.launch { cam.start(useFront = true) }
        }
    }

    private suspend fun handleResult(result: PipelineResult) {
        when (result) {
            is PipelineResult.Matched -> {
                val submit = withContext(Dispatchers.IO) {
                    punchCoordinator.submitPunch(
                        result.person.attendanceCode,
                        result.similarity.toDouble()
                    )
                }
                when (submit) {
                    is PunchSubmitResult.Accepted -> {
                        showBanner(
                            "${getString(R.string.identified)} ${result.person.displayName}",
                            R.color.success
                        )
                    }
                    PunchSubmitResult.Cooldown -> {
                        showBanner(getString(R.string.cooldown), R.color.warning)
                    }
                }
            }
            is PipelineResult.Rejected -> {
                val (msg, color) = when (result.reason) {
                    RejectReason.SPOOF -> getString(R.string.spoof_detected) to R.color.danger
                    RejectReason.UNKNOWN -> getString(R.string.unknown_face) to R.color.danger
                    RejectReason.MODELS_MISSING -> getString(R.string.models_missing) to R.color.warning
                    RejectReason.QUALITY -> (result.detail.ifBlank { "Adjust position" }) to R.color.warning
                    RejectReason.NO_FACE -> return
                    RejectReason.MULTI_FACE -> "One face only" to R.color.warning
                }
                // Throttle soft quality messages
                val now = System.currentTimeMillis()
                if (result.reason == RejectReason.QUALITY && now - lastUiMessageAt < 1500) return
                lastUiMessageAt = now
                showBanner(msg, color)
            }
        }
    }

    private fun showBanner(text: String, colorRes: Int) {
        runOnUiThread {
            binding.statusText.text = text
            binding.resultBanner.text = text
            binding.resultBanner.setBackgroundColor(ContextCompat.getColor(this, colorRes))
            binding.resultBanner.visibility = View.VISIBLE
            mainHandler.removeCallbacksAndMessages(null)
            mainHandler.postDelayed({
                binding.resultBanner.visibility = View.GONE
                binding.statusText.text = getString(R.string.looking_for_face)
            }, 2500)
        }
    }

    override fun onStop() {
        camera?.shutdown()
        camera = null
        super.onStop()
    }
}

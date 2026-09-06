package com.barabd.facekiosk.ui

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.barabd.facekiosk.FaceKioskApp
import com.barabd.facekiosk.R
import com.barabd.facekiosk.databinding.DialogPinBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object PinDialog {
    fun requirePin(
        context: Context,
        title: String = context.getString(R.string.enter_pin),
        forceCreate: Boolean = false,
        onCancel: (() -> Unit)? = null,
        onSuccess: () -> Unit
    ) {
        // #region agent log
        try {
            Thread {
                try {
                    val body = """{"sessionId":"a427e0","hypothesisId":"H1","location":"PinDialog.kt:requirePin","message":"requirePin invoked","data":{"forceCreate":$forceCreate,"hasOnSuccess":true},"timestamp":${System.currentTimeMillis()},"runId":"post-fix"}"""
                    val conn = java.net.URL("http://127.0.0.1:7243/ingest/0f7cc482-4d93-4f47-a1c1-15ee26198b7e").openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("X-Debug-Session-Id", "a427e0")
                    conn.doOutput = true
                    conn.connectTimeout = 500
                    conn.readTimeout = 500
                    conn.outputStream.use { os -> os.write(body.toByteArray()) }
                    conn.responseCode
                    conn.disconnect()
                } catch (_: Exception) {
                }
            }.start()
        } catch (_: Exception) {
        }
        // #endregion
        val app = context.applicationContext as FaceKioskApp
        val createMode = forceCreate || !app.pinStore.hasPin()
        val binding = DialogPinBinding.inflate(LayoutInflater.from(context))
        if (createMode) {
            binding.layoutConfirm.visibility = android.view.View.VISIBLE
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(if (createMode) context.getString(R.string.create_pin) else title)
            .setView(binding.root)
            .setCancelable(true)
            .setNegativeButton(R.string.cancel) { d, _ ->
                d.dismiss()
                onCancel?.invoke()
            }
            .setPositiveButton(R.string.ok, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val pin = binding.inputPin.text?.toString().orEmpty()
                if (pin.length !in 4..8) {
                    binding.inputPin.error = "4–8 digits"
                    return@setOnClickListener
                }
                if (createMode) {
                    val confirm = binding.inputPinConfirm.text?.toString().orEmpty()
                    if (pin != confirm) {
                        binding.inputPinConfirm.error = "Does not match"
                        return@setOnClickListener
                    }
                    app.pinStore.setPin(pin)
                    dialog.dismiss()
                    onSuccess()
                } else {
                    if (!app.pinStore.verify(pin)) {
                        binding.inputPin.error = "Wrong PIN"
                        return@setOnClickListener
                    }
                    dialog.dismiss()
                    onSuccess()
                }
            }
        }
        dialog.setOnCancelListener { onCancel?.invoke() }
        dialog.show()
    }
}

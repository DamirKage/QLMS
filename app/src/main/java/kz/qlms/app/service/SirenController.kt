package kz.qlms.app.service

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * "Disorient and draw attention" tool during an active SOS, the way Hollie
 * Guard's panic alarm works: a loud siren plus a strobing flashlight. Torch
 * control needs no extra runtime permission (Android's torch API is
 * permission-free by design — only opening the camera for capture needs one).
 */
object SirenController {
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun start(context: Context) {
        if (job?.isActive == true) return
        job = scope.launch {
            val toneGenerator = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME) }.getOrNull()
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val torchCameraId = cameraManager?.let { findTorchCapableCamera(it) }
            var torchOn = false
            try {
                while (isActive) {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK_TONE_ON, 400)
                    if (cameraManager != null && torchCameraId != null) {
                        torchOn = !torchOn
                        runCatching { cameraManager.setTorchMode(torchCameraId, torchOn) }
                    }
                    delay(180)
                }
            } finally {
                runCatching { toneGenerator?.release() }
                if (cameraManager != null && torchCameraId != null) {
                    runCatching { cameraManager.setTorchMode(torchCameraId, false) }
                }
            }
        }
    }

    fun stop(context: Context) {
        job?.cancel()
        job = null
        // Belt-and-suspenders: make sure the torch doesn't get stuck on if cancellation raced the loop.
        runCatching {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            cameraManager?.let { findTorchCapableCamera(it) }?.let { cameraManager?.setTorchMode(it, false) }
        }
    }

    val isActive: Boolean get() = job?.isActive == true

    private fun findTorchCapableCamera(manager: CameraManager): String? =
        manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
}

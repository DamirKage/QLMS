package kz.qlms.app.util

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * "Silent SOS" trigger: three hard shakes within ~1.2s. Active only while the
 * app process is alive (registered/unregistered from MainActivity's lifecycle)
 * — this is a deliberately honest scope-cut versus promising a true always-on
 * background daemon, which would need a justified special-use foreground
 * service and is easy to get wrong without a device to test on. A shake still
 * only *arms* SOS; the existing notification's Cancel action is the safety net.
 */
class ShakeDetector(
    private val sensorManager: SensorManager,
    private val onShakeDetected: () -> Unit,
) : SensorEventListener {

    private val shakeTimestamps = ArrayDeque<Long>()
    private var lastShakeTriggeredAt = 0L

    fun start() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        shakeTimestamps.clear()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val gX = event.values[0] / SensorManager.GRAVITY_EARTH
        val gY = event.values[1] / SensorManager.GRAVITY_EARTH
        val gZ = event.values[2] / SensorManager.GRAVITY_EARTH
        val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

        if (gForce < SHAKE_THRESHOLD_G) return

        val now = System.currentTimeMillis()
        shakeTimestamps.addLast(now)
        while (shakeTimestamps.isNotEmpty() && now - shakeTimestamps.first() > SHAKE_WINDOW_MS) {
            shakeTimestamps.removeFirst()
        }

        if (shakeTimestamps.size >= REQUIRED_SHAKES && now - lastShakeTriggeredAt > COOLDOWN_MS) {
            lastShakeTriggeredAt = now
            shakeTimestamps.clear()
            onShakeDetected()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val SHAKE_THRESHOLD_G = 2.7
        private const val SHAKE_WINDOW_MS = 1200L
        private const val REQUIRED_SHAKES = 3
        private const val COOLDOWN_MS = 10_000L
    }
}

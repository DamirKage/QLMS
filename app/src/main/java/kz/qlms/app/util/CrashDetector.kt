package kz.qlms.app.util

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Opt-in crash/fall detection, Life360/Apple-style, without the always-on
 * location tracking those products build it on top of. Heuristic: a hard
 * impact (a g-force spike well past anything normal handling produces)
 * followed by several seconds of near-total stillness — consistent with the
 * phone (and possibly its owner) not moving afterward. Normal driving,
 * dropping the phone once and picking it back up, or a shake never produces
 * that specific sequence, which keeps false positives rare without needing
 * to know the phone's location or speed at all.
 */
class CrashDetector(
    private val sensorManager: SensorManager,
    private val onCrashDetected: () -> Unit,
) : SensorEventListener {

    private enum class State { IDLE, WATCHING_STILLNESS }

    private var state = State.IDLE
    private var watchStartMs = 0L
    private var maxDeviationSinceImpact = 0f

    fun start() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        state = State.IDLE
    }

    override fun onSensorChanged(event: SensorEvent) {
        val gX = event.values[0] / SensorManager.GRAVITY_EARTH
        val gY = event.values[1] / SensorManager.GRAVITY_EARTH
        val gZ = event.values[2] / SensorManager.GRAVITY_EARTH
        val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)
        val now = System.currentTimeMillis()

        when (state) {
            State.IDLE -> {
                if (gForce >= IMPACT_THRESHOLD_G) {
                    state = State.WATCHING_STILLNESS
                    watchStartMs = now
                    maxDeviationSinceImpact = 0f
                }
            }
            State.WATCHING_STILLNESS -> {
                val deviationFromRest = abs(gForce - 1f)
                if (deviationFromRest > maxDeviationSinceImpact) maxDeviationSinceImpact = deviationFromRest

                if (now - watchStartMs >= STILLNESS_WINDOW_MS) {
                    val wasStillTheWholeTime = maxDeviationSinceImpact < STILLNESS_THRESHOLD_G
                    state = State.IDLE
                    if (wasStillTheWholeTime) onCrashDetected()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val IMPACT_THRESHOLD_G = 3.5
        private const val STILLNESS_WINDOW_MS = 8_000L
        private const val STILLNESS_THRESHOLD_G = 0.35f
    }
}

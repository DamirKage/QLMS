package kz.qlms.app.util

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Opt-in crash/fall detection, Life360/Apple-style, without the always-on
 * location tracking those products build it on top of. Heuristic: a hard
 * impact (a g-force spike well past anything normal handling produces)
 * followed by several seconds of near-total stillness — consistent with the
 * phone (and possibly its owner) not moving afterward. Normal driving,
 * dropping the phone once and picking it back up, or a shake never produces
 * that specific sequence, which keeps false positives rare without needing
 * to know the phone's location or speed at all. The actual state machine is
 * [CrashHeuristic] — kept separate so it can be unit-tested without a real
 * SensorManager.
 */
class CrashDetector(
    private val sensorManager: SensorManager,
    private val onCrashDetected: (peakImpactG: Float) -> Unit,
) : SensorEventListener {

    private val heuristic = CrashHeuristic()

    fun start() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        heuristic.reset()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val gX = event.values[0] / SensorManager.GRAVITY_EARTH
        val gY = event.values[1] / SensorManager.GRAVITY_EARTH
        val gZ = event.values[2] / SensorManager.GRAVITY_EARTH
        val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

        heuristic.onReading(gForce, System.currentTimeMillis())?.let { onCrashDetected(it.peakImpactG) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

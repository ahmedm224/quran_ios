package com.quranmedia.player.wear.presentation.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.getSystemService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Detects Samsung Air Gestures and general wrist gestures using accelerometer and gyroscope sensors.
 * Optimized for Samsung Galaxy Watch air gestures with low sensitivity thresholds.
 *
 * Supported gestures:
 * - Pinch (index finger and thumb touch) - detected via subtle wrist micro-movements
 * - Double pinch - two quick pinches
 * - Clench fist - detected via rapid muscle tension movement
 * - Flick/Knock wrist - quick wrist movement
 * - Shake gesture - vigorous shaking
 * - Wrist rotation/twist
 */
class AirGestureDetector(context: Context) {

    private val sensorManager: SensorManager? = context.getSystemService()
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val linearAccelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    // Very low thresholds for Samsung Air Gesture sensitivity
    // Pinch gestures create very subtle movements
    private val pinchThreshold = 0.8f          // Very subtle movement for pinch
    private val microMovementThreshold = 0.4f  // Micro movements from finger gestures
    private val flickThreshold = 3.0f          // Lower threshold for flick detection
    private val shakeThreshold = 8.0f          // Shake detection
    private val gyroThreshold = 0.5f           // Lower gyroscope threshold
    private val clenchThreshold = 1.5f         // Clench fist detection

    // Timing for gesture detection
    private val gestureTimeout = 200L          // Faster response between gestures
    private val doublePinchWindow = 400L       // Window for double pinch detection
    private val pinchPatternWindow = 150L      // Window for pinch pattern detection

    private var lastGestureTime = 0L
    private var lastPinchTime = 0L
    private var pinchCount = 0

    // Sensor value history for pattern detection
    private val accelHistory = ArrayDeque<FloatArray>(10)
    private val gyroHistory = ArrayDeque<FloatArray>(10)
    private val linearAccelHistory = ArrayDeque<FloatArray>(10)

    private var lastAccelValues = floatArrayOf(0f, 0f, 0f)
    private var lastGyroValues = floatArrayOf(0f, 0f, 0f)
    private var lastLinearAccelValues = floatArrayOf(0f, 0f, 0f)

    // Baseline calibration
    private var isCalibrated = false
    private var baselineAccel = floatArrayOf(0f, 0f, 0f)
    private var calibrationSamples = mutableListOf<FloatArray>()
    private val calibrationCount = 20

    sealed class AirGesture {
        data object Pinch : AirGesture()           // Index + thumb pinch
        data object DoublePinch : AirGesture()     // Two quick pinches
        data object Clench : AirGesture()          // Clench fist
        data object FlickUp : AirGesture()
        data object FlickDown : AirGesture()
        data object FlickLeft : AirGesture()
        data object FlickRight : AirGesture()
        data object Knock : AirGesture()           // Quick knock/tap motion
        data object Shake : AirGesture()
        data object WristRotate : AirGesture()
        data object MicroMovement : AirGesture()   // Any subtle detected movement
    }

    /**
     * Returns a Flow that emits detected air gestures.
     */
    fun gestureFlow(): Flow<AirGesture> = callbackFlow {
        if (sensorManager == null) {
            Timber.w("SensorManager not available")
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val currentTime = System.currentTimeMillis()

                when (event.sensor.type) {
                    Sensor.TYPE_LINEAR_ACCELERATION -> {
                        // Linear acceleration (gravity removed) - best for gesture detection
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]

                        // Add to history
                        linearAccelHistory.addLast(floatArrayOf(x, y, z))
                        if (linearAccelHistory.size > 10) linearAccelHistory.removeFirst()

                        // Detect pinch gesture pattern
                        // Pinch creates a very subtle, quick spike in linear acceleration
                        val magnitude = sqrt(x * x + y * y + z * z)

                        if (currentTime - lastGestureTime >= gestureTimeout) {
                            // Check for pinch pattern (subtle quick movement)
                            if (magnitude > pinchThreshold && magnitude < flickThreshold) {
                                val isPinchPattern = detectPinchPattern()
                                if (isPinchPattern) {
                                    // Check for double pinch
                                    if (currentTime - lastPinchTime < doublePinchWindow) {
                                        pinchCount++
                                        if (pinchCount >= 2) {
                                            lastGestureTime = currentTime
                                            pinchCount = 0
                                            trySend(AirGesture.DoublePinch)
                                            Timber.d("Detected: Double Pinch")
                                        }
                                    } else {
                                        pinchCount = 1
                                        lastGestureTime = currentTime
                                        lastPinchTime = currentTime
                                        trySend(AirGesture.Pinch)
                                        Timber.d("Detected: Pinch (magnitude: $magnitude)")
                                    }
                                }
                            }

                            // Detect micro-movements (any subtle gesture)
                            if (magnitude > microMovementThreshold && magnitude < pinchThreshold) {
                                if (detectMicroMovementPattern()) {
                                    lastGestureTime = currentTime
                                    trySend(AirGesture.MicroMovement)
                                    Timber.d("Detected: Micro Movement")
                                }
                            }
                        }

                        lastLinearAccelValues = floatArrayOf(x, y, z)
                    }

                    Sensor.TYPE_ACCELEROMETER -> {
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]

                        // Calibration
                        if (!isCalibrated) {
                            calibrationSamples.add(floatArrayOf(x, y, z))
                            if (calibrationSamples.size >= calibrationCount) {
                                calibrate()
                            }
                            return
                        }

                        // Add to history
                        accelHistory.addLast(floatArrayOf(x, y, z))
                        if (accelHistory.size > 10) accelHistory.removeFirst()

                        // Calculate delta from baseline
                        val deltaX = abs(x - baselineAccel[0])
                        val deltaY = abs(y - baselineAccel[1])
                        val deltaZ = abs(z - baselineAccel[2])
                        val deltaMagnitude = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)

                        if (currentTime - lastGestureTime >= gestureTimeout) {
                            // Detect shake gesture
                            if (deltaMagnitude > shakeThreshold) {
                                lastGestureTime = currentTime
                                trySend(AirGesture.Shake)
                                Timber.d("Detected: Shake")
                            }
                            // Detect flick/knock gestures
                            else if (deltaMagnitude > flickThreshold) {
                                val gesture = when {
                                    deltaX > deltaY && deltaX > deltaZ -> {
                                        if (x - lastAccelValues[0] > 0) AirGesture.FlickRight
                                        else AirGesture.FlickLeft
                                    }
                                    deltaY > deltaX && deltaY > deltaZ -> {
                                        if (y - lastAccelValues[1] > 0) AirGesture.FlickUp
                                        else AirGesture.FlickDown
                                    }
                                    else -> AirGesture.Knock
                                }
                                lastGestureTime = currentTime
                                trySend(gesture)
                                Timber.d("Detected: $gesture")
                            }
                            // Detect clench fist (specific acceleration pattern)
                            else if (deltaMagnitude > clenchThreshold && detectClenchPattern()) {
                                lastGestureTime = currentTime
                                trySend(AirGesture.Clench)
                                Timber.d("Detected: Clench")
                            }
                        }

                        lastAccelValues = floatArrayOf(x, y, z)
                    }

                    Sensor.TYPE_GYROSCOPE -> {
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]

                        // Add to history
                        gyroHistory.addLast(floatArrayOf(x, y, z))
                        if (gyroHistory.size > 10) gyroHistory.removeFirst()

                        val rotationMagnitude = sqrt(x * x + y * y + z * z)

                        if (currentTime - lastGestureTime >= gestureTimeout) {
                            // Detect wrist rotation
                            if (rotationMagnitude > gyroThreshold) {
                                // Primary rotation around X axis (wrist twist)
                                if (abs(x) > gyroThreshold && abs(x) > abs(y) && abs(x) > abs(z)) {
                                    lastGestureTime = currentTime
                                    trySend(AirGesture.WristRotate)
                                    Timber.d("Detected: Wrist Rotate")
                                }
                            }
                        }

                        lastGyroValues = floatArrayOf(x, y, z)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not needed
            }
        }

        // Register all available sensors with fastest sampling rate
        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_FASTEST)
            Timber.d("Accelerometer registered")
        }

        gyroscope?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_FASTEST)
            Timber.d("Gyroscope registered")
        }

        linearAccelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_FASTEST)
            Timber.d("Linear accelerometer registered")
        }

        Timber.d("Air gesture detection started with low sensitivity thresholds")

        awaitClose {
            sensorManager.unregisterListener(listener)
            Timber.d("Air gesture detection stopped")
        }
    }

    /**
     * Calibrate baseline sensor values when wrist is still.
     */
    private fun calibrate() {
        if (calibrationSamples.isEmpty()) return

        val sumX = calibrationSamples.sumOf { it[0].toDouble() }.toFloat()
        val sumY = calibrationSamples.sumOf { it[1].toDouble() }.toFloat()
        val sumZ = calibrationSamples.sumOf { it[2].toDouble() }.toFloat()
        val count = calibrationSamples.size

        baselineAccel = floatArrayOf(sumX / count, sumY / count, sumZ / count)
        isCalibrated = true
        calibrationSamples.clear()

        Timber.d("Calibrated baseline: ${baselineAccel.contentToString()}")
    }

    /**
     * Detect pinch gesture pattern from sensor history.
     * Pinch creates a quick spike followed by return to baseline.
     */
    private fun detectPinchPattern(): Boolean {
        if (linearAccelHistory.size < 3) return true // Not enough data, allow gesture

        val recent = linearAccelHistory.takeLast(3)
        if (recent.size < 3) return true

        // Check for spike pattern: low -> high -> low
        val mag0 = magnitude(recent[0])
        val mag1 = magnitude(recent[1])
        val mag2 = magnitude(recent[2])

        // Pattern: magnitude increases then decreases (spike)
        return (mag1 > mag0 * 1.2f) || (mag2 > mag1 * 0.5f)
    }

    /**
     * Detect micro-movement pattern for very subtle gestures.
     */
    private fun detectMicroMovementPattern(): Boolean {
        if (linearAccelHistory.size < 2) return false

        val recent = linearAccelHistory.takeLast(2)
        val mag0 = magnitude(recent[0])
        val mag1 = magnitude(recent[1])

        // Any detectable change in movement
        return abs(mag1 - mag0) > microMovementThreshold * 0.5f
    }

    /**
     * Detect clench fist pattern.
     * Clenching creates a specific multi-axis movement pattern.
     */
    private fun detectClenchPattern(): Boolean {
        if (accelHistory.size < 4) return false

        val recent = accelHistory.takeLast(4)

        // Clench typically shows sustained tension (less variation after initial movement)
        val variations = recent.windowed(2).map { (a, b) ->
            magnitude(floatArrayOf(b[0] - a[0], b[1] - a[1], b[2] - a[2]))
        }

        // Pattern: initial movement followed by steadier state
        return variations.isNotEmpty() && variations.first() > variations.last() * 1.5f
    }

    private fun magnitude(values: FloatArray): Float {
        return sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
    }

    /**
     * Check if air gestures are supported on this device.
     */
    fun isSupported(): Boolean {
        return sensorManager != null && (accelerometer != null || linearAccelerometer != null)
    }
}

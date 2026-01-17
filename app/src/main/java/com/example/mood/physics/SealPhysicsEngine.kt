package com.example.mood.physics

import kotlin.math.abs
import com.example.mood.util.angularDistance
import com.example.mood.util.normalizeAngle

class SealPhysicsEngine(
    private val minSeparationDeg: Float
) {

    fun step(
        angles: MutableList<Float>,
        velocities: MutableList<Float>,
        deltaSeconds: Float,
        onImpact: (strength: Float) -> Unit
    ) {
        if (angles.isEmpty()) return

        // --- 1️⃣ Integrate motion ---
        for (i in angles.indices) {
            angles[i] =
                normalizeAngle(
                    angles[i] + velocities[i] * deltaSeconds
                )
        }

        // --- 2️⃣ Resolve collisions ---
        for (i in 0 until angles.size) {
            for (j in i + 1 until angles.size) {

                val a = angles[i]
                val b = angles[j]

                val dist = angularDistance(a, b)

                if (dist < minSeparationDeg) {

                    val overlap = minSeparationDeg - dist

                    val direction =
                        if (((b - a + 540f) % 360f) - 180f > 0) 1f else -1f

                    angles[i] =
                        normalizeAngle(
                            angles[i] - direction * overlap / 2f
                        )
                    angles[j] =
                        normalizeAngle(
                            angles[j] + direction * overlap / 2f
                        )

                    val vi = velocities[i]
                    val vj = velocities[j]

                    velocities[i] = vj * 0.9f
                    velocities[j] = vi * 0.9f

                    val relativeVelocity = abs(vi - vj)
                    val normalizedImpact =
                        (relativeVelocity / 720f).coerceIn(0f, 1f)

                    if (normalizedImpact > 0.15f) {
                        onImpact(normalizedImpact)
                    }
                }
            }
        }

        // --- 3️⃣ Friction ---
        for (i in velocities.indices) {
            velocities[i] *= 0.97f
            if (abs(velocities[i]) < 0.05f) {
                velocities[i] = 0f
            }
        }
    }
}



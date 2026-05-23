package com.example.pruebasdk

import kotlin.math.hypot

data class HeatmapPoint(val x: Float, val y: Float, var weight: Int = 1)

class HeatmapProcessor {

    companion object {
        // Radio en píxeles para considerar que dos clics pertenecen al mismo clúster
        private const val CLUSTER_RADIUS = 100f
    }

    /**
     * Algoritmo simple de agrupación (clustering) para calcular densidad.
     */
    fun processDensity(events: List<ClickEvent>): List<HeatmapPoint> {
        val clusters = mutableListOf<HeatmapPoint>()

        for (event in events) {
            var addedToCluster = false

            // Buscar si el clic actual cae dentro del radio de un clúster existente
            for (cluster in clusters) {
                val distance = hypot((cluster.x - event.rawX).toDouble(), (cluster.y - event.rawY).toDouble())
                if (distance <= CLUSTER_RADIUS) {
                    cluster.weight += 1
                    addedToCluster = true
                    break
                }
            }

            // Si no está cerca de ningún clúster, creamos uno nuevo
            if (!addedToCluster) {
                clusters.add(HeatmapPoint(event.rawX, event.rawY, 1))
            }
        }

        return clusters
    }
}

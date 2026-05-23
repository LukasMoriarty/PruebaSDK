package com.example.pruebasdk

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View

@SuppressLint("ViewConstructor")
class HeatmapView(context: Context, private val points: List<HeatmapPoint>) : View(context) {

    private val paint = Paint().apply {
        isAntiAlias = true
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Encontrar el peso máximo para normalizar la intensidad
        val maxWeight = points.maxOfOrNull { it.weight } ?: 1

        for (point in points) {
            val radius = 150f + (point.weight * 10f) // El radio crece ligeramente con la densidad
            
            // Determinar color basado en peso relativo al máximo
            val intensity = point.weight.toFloat() / maxWeight.toFloat()
            val centerColor = getColorForIntensity(intensity)
            val edgeColor = Color.TRANSPARENT

            paint.shader = RadialGradient(
                point.x, point.y, radius,
                centerColor, edgeColor, Shader.TileMode.CLAMP
            )

            canvas.drawCircle(point.x, point.y, radius, paint)
        }
    }

    /**
     * Interpola el color dependiendo de la densidad:
     * Baja intensidad (pocos clics) -> Azul / Verde
     * Alta intensidad (muchos clics) -> Rojo
     */
    private fun getColorForIntensity(intensity: Float): Int {
        // Colores base del Proyecto.tex (adaptados con transparencia para superposición)
        return when {
            intensity > 0.7f -> Color.argb(180, 255, 0, 0)     // Rojo (ET)
            intensity > 0.4f -> Color.argb(180, 0, 255, 0)     // Verde (Edema)
            else -> Color.argb(150, 0, 0, 255)                 // Azul (NCR)
        }
    }
}

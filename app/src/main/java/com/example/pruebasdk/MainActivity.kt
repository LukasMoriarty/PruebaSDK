package com.example.pruebasdk

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pruebasdk.SoimSDK

// =============================================================================
// MainActivity.kt
// SOIM SDK - Aplicación de Prueba
//
// DÍA 1 + DÍA 2: Demostración de la integración del SDK en una Activity.
//
// En producción, SoimSDK.init() debería estar en la clase Application del
// proyecto (MyApp : Application) para que se inicialice UNA VEZ para toda
// la app. Lo colocamos aquí en MainActivity solo para efectos de la demo.
//
// INTEGRACIÓN COMPLETA EN 2 PASOS:
//   1. SoimSDK.init(application)   ← DÍA 1: Activa el interceptor global
//   2. SoimSDK.setTrackingEnabled(true) ← Después de aceptar privacidad
// =============================================================================

/**
 * Activity principal de la app de prueba del SDK SOIM.
 *
 * Esta pantalla sirve como banco de pruebas para verificar que:
 *  - El SDK intercepta correctamente los toques (DÍA 1)
 *  - Los eventos registran el View ID, X, Y, Timestamp y screenName (DÍA 2)
 *  - El interceptor NO rompe la funcionalidad normal de los botones
 *
 * Para verificar el funcionamiento, revisar el Logcat con el filtro:
 *   "SOIM" → muestra todos los logs del SDK
 */
class MainActivity : AppCompatActivity() {

    private var heatmapView: HeatmapView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ── INTEGRACIÓN DEL SDK (DÍA 1) ──────────────────────────────────────
        //
        // Una sola línea inicializa todo el sistema:
        //  Registra SoimActivityTracker para todas las Activities
        //  Instala SoimWindowCallback al entrar en onResume
        //  Comienza a rastrear la pantalla actual
        //
        // En un proyecto real, esto va en:
        //   class MyApplication : Application() {
        //       override fun onCreate() {
        //           super.onCreate()
        //           SoimSDK.init(this) // ← Aquí, una sola vez
        //       }
        //   }
        //
        SoimSDK.init(application)

        // ── CONSENTIMIENTO DE PRIVACIDAD ──────────────────────────────────────
        //
        // REGLA DE CONTROL (Proceso A):
        // "Solo se envían datos si el usuario aceptó el aviso de privacidad"
        //
        // En producción, este valor vendría de SharedPreferences o de un
        // diálogo de consentimiento que el usuario ya haya respondido.
        //
        // Para la demo activamos directamente:
        SoimSDK.setTrackingEnabled(true)

        // ── NOTA PARA EL DESARROLLADOR ────────────────────────────────────────
        // A partir de aquí, el SDK ya está activo. Cada toque en cualquier
        // View de esta Activity será capturado automáticamente por
        // SoimWindowCallback.dispatchTouchEvent() y se registrará en Logcat
        // con el formato:
        //
        //   SOIM_WindowCallback: Clic capturado → viewId='btn_flush'
        //                           | (542.0, 1203.5) | pantalla='MainActivity'
        //
        // No se requiere ningún código adicional en los Views o Fragments.

        // ── CONFIGURACIÓN DE BOTONES DE PRUEBA ───────────────────────────────
        setupTestButtons()
    }

    private fun setupTestButtons() {
        val showToast = { msg: String -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }

        findViewById<Button>(R.id.btn_top_left).setOnClickListener { showToast("Clic en Perfil") }
        findViewById<Button>(R.id.btn_top_right).setOnClickListener { showToast("Clic en Ajustes") }
        findViewById<Button>(R.id.btn_center).setOnClickListener { showToast("Clic en Comprar") }
        findViewById<Button>(R.id.btn_bottom_left).setOnClickListener { showToast("Clic en Favoritos") }
        findViewById<Button>(R.id.btn_bottom_right).setOnClickListener { showToast("Clic en Carrito") }

        findViewById<Button>(R.id.btn_admin).setOnClickListener {
            toggleAdminMode()
        }

        findViewById<Button>(R.id.btn_export).setOnClickListener {
            CsvExporter.exportAndShareCsv(this)
        }

        findViewById<Button>(R.id.btn_clear).setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("Borrar datos")
                .setMessage("¿Quiere borrar todos los datos guardados?")
                .setPositiveButton("Confirmar") { _, _ ->
                    SoimSDK.dbHelper?.clearAllEvents()
                    Toast.makeText(this, "Datos borrados", Toast.LENGTH_SHORT).show()
                    
                    // Si el heatmap está activo, lo quitamos para limpiar la pantalla
                    if (heatmapView != null) {
                        findViewById<ConstraintLayout>(R.id.main).removeView(heatmapView)
                        heatmapView = null
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun toggleAdminMode() {
        try {
            val rootLayout = findViewById<ConstraintLayout>(R.id.main)

            if (heatmapView != null) {
                // Si ya está activo, lo quitamos
                rootLayout.removeView(heatmapView)
                heatmapView = null
                Toast.makeText(this, "Modo Admin desactivado", Toast.LENGTH_SHORT).show()
                return
            }

            // Forzamos flush para tener los últimos eventos
            SoimSDK.flushBuffer()

            // Obtenemos los eventos de la base de datos para esta pantalla
            val events = SoimSDK.dbHelper?.getEventsByScreen(this.javaClass.simpleName) ?: emptyList()

            if (events.isEmpty()) {
                Toast.makeText(this, "No hay datos para mostrar en esta pantalla", Toast.LENGTH_SHORT).show()
                return
            }

            // Procesamos la densidad
            val processor = HeatmapProcessor()
            val heatmapPoints = processor.processDensity(events)

            // Creamos y añadimos la vista
            heatmapView = HeatmapView(this, heatmapPoints).apply {
                // Evitamos que el Heatmap consuma toques (para poder seguir interactuando con botones de abajo)
                isClickable = false
                isFocusable = false
            }
            
            // Lo añadimos por encima de todo
            rootLayout.addView(heatmapView, ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            ))

            Toast.makeText(this, "Modo Admin activado: Mostrando " + events.size + " clics", Toast.LENGTH_SHORT).show()
        } catch (e: Throwable) {
            e.printStackTrace()
            android.app.AlertDialog.Builder(this)
                .setTitle("Crash Detectado")
                .setMessage(e.stackTraceToString())
                .setPositiveButton("OK", null)
                .show()
        }
    }

    override fun onStop() {
        super.onStop()
        // ── FLUSH AL IR A BACKGROUND ──────────────────────────────────────────
        //
        // Cuando la app va a background, forzamos el envío del buffer para
        // no perder eventos pendientes si el proceso es terminado por Android.
        //
        // En Días 3+, este flush enviará los datos reales al backend.
        // Por ahora, muestra el resumen en Logcat.
        SoimSDK.flushBuffer()
    }
}
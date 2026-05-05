package com.example.pruebasdk

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
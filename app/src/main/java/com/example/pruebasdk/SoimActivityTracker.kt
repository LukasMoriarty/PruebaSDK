package com.example.pruebasdk

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import java.lang.ref.WeakReference

// =============================================================================
// SoimActivityTracker.kt
// SOIM SDK - Sistema de Optimización de Interfaces Móviles
//
// DÍA 2: Rastrea en qué pantalla (Activity o Fragment) se encuentra el usuario
// en todo momento, e instala el interceptor de toques (SoimWindowCallback)
// en cada Activity de forma automática.
//
// Implementa dos interfaces de callback de Android:
//
//  1. Application.ActivityLifecycleCallbacks
//     ──────────────────────────────────────
//     Android notifica automáticamente cuando cualquier Activity del proceso
//     cambia de estado (onCreate, onResume, onPause, onDestroy...).
//     Lo registramos en Application para cubrir TODAS las Activities,
//     sin necesidad de modificar ninguna de ellas.
//
//  2. FragmentManager.FragmentLifecycleCallbacks
//     ───────────────────────────────────────────
//     Para apps con arquitectura Fragment-based (Navigation Component,
//     ViewPager, etc.), necesitamos saber también qué Fragment está visible,
//     ya que múltiples Fragments pueden compartir la misma Activity.
//
// Regla de negocio (Proceso A del proyecto):
//   "El análisis debe filtrarse por versión de la aplicación y tipo de
//    dispositivo para no mezclar datos de interfaces distintas."
//   → screenName garantiza que los clics en pantalla X no se mezclan con
//     los de pantalla Y en los Heatmaps de ClickHouse/Grafana.
// =============================================================================

/**
 * Observador del ciclo de vida de Activities y Fragments.
 *
 * Responsabilidades:
 *  - Mantener actualizado [currentScreenName] con la pantalla visible
 *  - Instalar [SoimWindowCallback] en cada Activity al entrar en Resumed
 *  - Desinstalar el callback al abandonar la Activity (buenas prácticas)
 *  - Registrar callbacks de Fragment para apps con navegación por Fragments
 *
 * Se registra una sola vez en [SoimSDK.init] y funciona de forma invisible
 * para toda la vida de la aplicación.
 */
class SoimActivityTracker : Application.ActivityLifecycleCallbacks {

    companion object {
        private const val TAG = "SOIM_ActivityTracker"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Estado interno del tracker
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Nombre de la pantalla actualmente visible para el usuario.
     * Es leído por [SoimWindowCallback] al construir cada [ClickEvent].
     *
     * Formato: "NombreActivity" o "NombreActivity > NombreFragment"
     * Ejemplos: "LoginActivity", "HomeActivity > ProfileFragment"
     */
    var currentScreenName: String = "Unknown"
        private set // Solo modificable internamente

    /**
     * Referencia débil a la Activity actualmente en primer plano.
     * Usamos WeakReference para no impedir que el GC libere la Activity
     * cuando se destruya, evitando memory leaks.
     */
    private var currentActivityRef: WeakReference<Activity?> = WeakReference(null)

    // ─────────────────────────────────────────────────────────────────────────
    // DÍA 2: Implementación de ActivityLifecycleCallbacks
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Llamado cuando una Activity entra en estado RESUMED (visible y activa).
     * Es el momento correcto para instalar nuestro interceptor porque:
     *  - La ventana (Window) ya está completamente inicializada
     *  - La Activity está recibiendo input del usuario
     *  - Es el estado "estable" de una Activity en primer plano
     */
    override fun onActivityResumed(activity: Activity) {
        // Actualizamos la referencia a la Activity actual
        currentActivityRef = WeakReference(activity)

        // Actualizamos el nombre de pantalla con el nombre simple de la clase
        // getSimpleName() devuelve "LoginActivity" en lugar de
        // "com.example.myapp.ui.auth.LoginActivity"
        currentScreenName = activity.javaClass.simpleName

        Log.d(TAG, "▶ Pantalla activa: $currentScreenName")

        // DÍA 1: Instalamos el interceptor de toques en esta Activity
        installWindowCallbackInterceptor(activity)

        // DÍA 2: Si la Activity soporta Fragments (FragmentActivity),
        // registramos también el observador de Fragments
        if (activity is FragmentActivity) {
            registerFragmentCallbacks(activity)
        }
    }

    /**
     * Llamado cuando una Activity entra en PAUSED (parcialmente oculta).
     * Actualizamos el nombre de pantalla a "Background" para no registrar
     * eventos de toques que pudieran llegar durante transiciones.
     */
    override fun onActivityPaused(activity: Activity) {
        // Solo limpiamos si la Activity que se pausa es la que estábamos trackeando
        if (currentActivityRef.get() == activity) {
            Log.d(TAG, "⏸ Pantalla en pausa: ${activity.javaClass.simpleName}")
            currentScreenName = "${activity.javaClass.simpleName}[paused]"
        }
    }

    /**
     * Llamado cuando una Activity es destruida.
     * Desinstalamos el interceptor para liberar recursos y evitar retener
     * referencias a la Activity destruida.
     */
    override fun onActivityDestroyed(activity: Activity) {
        uninstallWindowCallbackInterceptor(activity)
        Log.d(TAG, "🗑 Activity destruida: ${activity.javaClass.simpleName}")
    }

    // Callbacks obligatorios de la interfaz que no necesitamos implementar
    // Los dejamos vacíos pero comentados para documentar por qué no se usan.

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // No instalamos el interceptor aquí porque la Window puede no estar
        // completamente lista. Esperamos a onActivityResumed.
    }

    override fun onActivityStarted(activity: Activity) {
        // onStarted ocurre antes de que la Activity sea completamente visible.
        // Preferimos onResumed para mayor seguridad.
    }

    override fun onActivityStopped(activity: Activity) {
        // La Activity está en background. No necesitamos hacer nada aquí.
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // No guardamos estado del SDK en el Bundle de la Activity.
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DÍA 1: Instalación y desinstalación del interceptor de Window.Callback
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Reemplaza el Window.Callback de la Activity con nuestro [SoimWindowCallback],
     * que envuelve (decora) el callback original sin romper ninguna funcionalidad.
     *
     * Verificamos que no esté ya instalado para evitar doble-wrapping en caso
     * de que onResumed sea llamado múltiples veces para la misma Activity
     * (lo cual puede ocurrir en algunos flujos de navegación).
     *
     * @param activity La Activity en la que instalar el interceptor
     */
    private fun installWindowCallbackInterceptor(activity: Activity) {
        val existingCallback = activity.window.callback

        // Verificamos que no hayamos ya instalado nuestro interceptor
        // (evita el caso: SoimWindowCallback wrapping SoimWindowCallback)
        if (existingCallback !is SoimWindowCallback) {
            val soimCallback = SoimWindowCallback(
                originalCallback = existingCallback,
                activity         = activity
            )
            activity.window.callback = soimCallback
            Log.d(TAG, "🔌 Interceptor instalado en: ${activity.javaClass.simpleName}")
        } else {
            Log.d(TAG, "ℹ Interceptor ya instalado en: ${activity.javaClass.simpleName}")
        }
    }

    /**
     * Restaura el Window.Callback original de la Activity, eliminando
     * nuestro interceptor. Se llama en onActivityDestroyed para evitar
     * retener referencias a objetos destruidos.
     *
     * @param activity La Activity de la que desinstalar el interceptor
     */
    private fun uninstallWindowCallbackInterceptor(activity: Activity) {
        val currentCallback = activity.window.callback
        if (currentCallback is SoimWindowCallback) {
            // Restauramos el callback original que estaba antes de nosotros
            activity.window.callback = currentCallback.originalCallback
            Log.d(TAG, "🔌 Interceptor removido de: ${activity.javaClass.simpleName}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DÍA 2: Tracking de Fragments para identificación precisa de pantalla
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Registra un observador de ciclo de vida de Fragments en la Activity.
     *
     * Esto es fundamental para apps que usan Navigation Component o múltiples
     * Fragments en una Activity (patrón Single-Activity Architecture), ya que
     * en esos casos la Activity siempre es la misma (ej: "MainActivity") pero
     * el Fragment visible cambia (ej: HomeFragment → ProductDetailFragment).
     *
     * El parámetro `recursive = true` permite también detectar Fragments
     * anidados (Fragments dentro de Fragments).
     *
     * @param activity FragmentActivity en la que registrar el observador
     */
    private fun registerFragmentCallbacks(activity: FragmentActivity) {
        activity.supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {

                /**
                 * Llamado cuando un Fragment entra en estado RESUMED (visible y activo).
                 * Actualizamos screenName para incluir tanto la Activity como el Fragment.
                 */
                override fun onFragmentResumed(fm: FragmentManager, fragment: Fragment) {
                    super.onFragmentResumed(fm, fragment)

                    // Solo consideramos Fragments "reales" de navegación,
                    // ignoramos DialogFragments y otros Fragments auxiliares
                    // que no representan una "pantalla" completa.
                    if (fragment.isVisible && !fragment.isHidden) {
                        val activityName  = activity.javaClass.simpleName
                        val fragmentName  = fragment.javaClass.simpleName

                        // Formato: "MainActivity > HomeFragment"
                        currentScreenName = "$activityName > $fragmentName"
                        Log.d(TAG, "▶ Fragment activo: $currentScreenName")
                    }
                }

                /**
                 * Llamado cuando un Fragment entra en PAUSED.
                 * Volvemos a mostrar solo la Activity como pantalla base.
                 */
                override fun onFragmentPaused(fm: FragmentManager, fragment: Fragment) {
                    super.onFragmentPaused(fm, fragment)
                    // Revertimos al nombre base de la Activity
                    currentScreenName = activity.javaClass.simpleName
                }
            },
            true // recursive = true → también detecta Fragments anidados
        )
    }
}

// =============================================================================
// NOTA: Acceso al originalCallback desde SoimSDK
// SoimWindowCallback expone originalCallback como internal val para que
// SoimActivityTracker pueda restaurarlo en uninstallWindowCallbackInterceptor.
// Esto es necesario porque Window.Callback no tiene método para obtener
// el callback anterior.
// =============================================================================
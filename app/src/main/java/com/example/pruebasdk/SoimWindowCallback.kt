package com.example.pruebasdk

import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.app.Activity
import android.util.Log

// =============================================================================
// SoimWindowCallback.kt
// SOIM SDK - Sistema de Optimización de Interfaces Móviles
//
// DÍA 1: Interceptor global de eventos táctiles usando el patrón Decorator
// sobre Window.Callback de Android.
//
// ¿POR QUÉ Window.Callback y no OnTouchListener?
// ─────────────────────────────────────────────────
// Un OnTouchListener solo funciona sobre vistas específicas. Para capturar
// TODOS los eventos en TODA la pantalla sin tocar el código de la app
// anfitriona, necesitamos interceptar en el nivel más alto: la Window.
//
// Window.Callback es la interfaz que Android usa internamente para despachar
// eventos de entrada a la Activity. Al envolverla (patrón Decorator), nuestro
// SDK se "inyecta" de forma no intrusiva: primero registra el evento, luego
// lo pasa al callback original para que la app funcione con normalidad.
//
// Flujo de un evento táctil:
//   [Dedo toca pantalla]
//       ↓
//   Android OS genera MotionEvent
//       ↓
//   SoimWindowCallback.dispatchTouchEvent()  ← AQUÍ entra nuestro SDK
//       ↓                    ↓
//   [Registro del evento]   [originalCallback.dispatchTouchEvent()]
//                                   ↓
//                          [La app responde normalmente]
// =============================================================================

/**
 * Decorator sobre [Window.Callback] que intercepta eventos táctiles
 * sin interferir con el comportamiento normal de la aplicación.
 *
 * Se instala automáticamente en cada Activity cuando ésta entra en
 * estado Resumed (ver [SoimActivityTracker]).
 *
 * Implementa la interfaz completa [Window.Callback] delegando todas las
 * funciones al [originalCallback], excepto [dispatchTouchEvent] donde
 * añadimos nuestra lógica de captura.
 *
 * @param originalCallback El Window.Callback original de la Activity,
 *                         que recibirá todos los eventos después de
 *                         que el SDK los haya registrado.
 * @param activity         Referencia a la Activity actual, necesaria para
 *                         recorrer la jerarquía de vistas y encontrar
 *                         el View presionado.
 */
class SoimWindowCallback(
        val originalCallback: Window.Callback,
        private val activity: Activity
) : Window.Callback by originalCallback {
    // "by originalCallback" es la delegación de Kotlin:
    // todos los métodos de Window.Callback que no sobreescribamos aquí
    // se redirigen automáticamente al originalCallback. Esto nos garantiza
    // que NO rompemos ninguna funcionalidad existente de la Activity.

    companion object {
        private const val TAG = "SOIM_WindowCallback"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DÍA 1: Punto de entrada principal del SDK.
    // Este método es llamado por Android para CADA evento de toque en la app.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Intercepta todos los eventos de toque de la pantalla.
     *
     * Solo procesamos ACTION_DOWN porque:
     *  - ACTION_DOWN = inicio del toque → el "clic" intencional del usuario
     *  - ACTION_MOVE = arrastre → no queremos spamear el buffer con movimientos
     *  - ACTION_UP   = fin del toque → redundante si ya capturamos ACTION_DOWN
     *
     * Regla de control (Proceso A del proyecto):
     *  "Solo se envían datos si el usuario aceptó el aviso de privacidad"
     *  → Se verifica mediante SoimSDK.isTrackingEnabled()
     *
     * @return El resultado del originalCallback (true si el evento fue consumido
     *         por alguna vista, false si no). Nunca modificamos este valor.
     */
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {

        // Capturamos solo el inicio del toque (ACTION_DOWN)
        // y solo si el tracking está habilitado (privacidad del usuario)
        if (event?.action == MotionEvent.ACTION_DOWN && SoimSDK.isTrackingEnabled()) {
            try {
                captureClickEvent(event)
            } catch (e: Exception) {
                // El SDK NUNCA debe crashear la app anfitriona.
                // Si algo falla en la captura, lo ignoramos silenciosamente.
                Log.e(TAG, "Error al capturar evento: ${e.message}")
            }
        }

        // Siempre delegamos al callback original para que la app funcione normal
        return originalCallback.dispatchTouchEvent(event)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DÍA 2: Captura de variables definidas en el Proceso A del proyecto
    // Variables: View ID, X, Y, Timestamp, Screen Name
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Extrae toda la información relevante del evento táctil y construye
     * un [ClickEvent] que será enviado al buffer local del SDK.
     *
     * @param motionEvent El evento de toque interceptado (garantizado ACTION_DOWN)
     */
    private fun captureClickEvent(motionEvent: MotionEvent) {

        // ── VARIABLE 1 & 2: Coordenadas X, Y ─────────────────────────────────
        // rawX/rawY son coordenadas absolutas en pantalla (no relativas a la vista).
        // Son las que necesitamos para el Heatmap, ya que se comparan contra
        // capturas de pantalla completas de la app.
        val x = motionEvent.rawX
        val y = motionEvent.rawY

        // ── VARIABLE 3: Timestamp ─────────────────────────────────────────────
        // Milisegundos desde epoch Unix. Usamos el momento del evento del sistema
        // (no del MotionEvent) para mayor consistencia entre dispositivos.
        val timestamp = System.currentTimeMillis()

        // ── VARIABLE 4: Screen Name (DÍA 2) ──────────────────────────────────
        // Obtenemos el nombre de la pantalla actual desde el tracker de Activities.
        // Este dato es crítico para el filtro mencionado en el Proceso B:
        // "El análisis debe filtrarse por versión de la aplicación y tipo de
        //  dispositivo para no mezclar datos de interfaces distintas."
        val screenName = SoimSDK.getCurrentScreenName()

        // ── VARIABLE 5: View ID ───────────────────────────────────────────────
        // Buscamos cuál View estaba exactamente en las coordenadas del toque.
        // Recorremos la jerarquía de vistas de afuera hacia adentro para
        // encontrar el View más específico (hijo más profundo) en esa posición.
        val decorView = activity.window.decorView as? ViewGroup
        val touchedView = decorView?.let { findDeepestViewAt(it, x.toInt(), y.toInt()) }

        val viewId = resolveViewId(touchedView)

        // ── Construcción del evento ───────────────────────────────────────────
        val clickEvent = ClickEvent(
                viewId     = viewId,
                rawX       = x,
                rawY       = y,
                timestamp  = timestamp,
                screenName = screenName,
                sessionId  = SoimSDK.getSessionId()
        )

        // Enviamos al SDK para que lo procese (buffer → batch → envío)
        SoimSDK.onEventCaptured(clickEvent)

        Log.d(TAG, "✅ Clic capturado → viewId='$viewId' | ($x, $y) | pantalla='$screenName'")
    }

    /**
     * Recorre recursivamente la jerarquía de vistas para encontrar el View
     * más profundo (más específico) que contiene las coordenadas dadas.
     *
     * Algoritmo: busca de hijo al padre, priorizando los hijos más al frente
     * (mayor índice Z) para simular el orden de dibujado de Android.
     *
     * @param viewGroup ViewGroup raíz desde donde iniciar la búsqueda
     * @param x         Coordenada X en píxeles de pantalla
     * @param y         Coordenada Y en píxeles de pantalla
     * @return El View más específico en esa posición, o null si no se encontró
     */
    private fun findDeepestViewAt(viewGroup: ViewGroup, x: Int, y: Int): View? {

        // Recorremos los hijos en orden inverso (el último dibujado es el que
        // aparece "encima" visualmente, por lo que lo revisamos primero)
        for (i in viewGroup.childCount - 1 downTo 0) {
            val child = viewGroup.getChildAt(i)

            // Saltamos vistas invisibles o que no reciben eventos
            if (child.visibility != View.VISIBLE) continue

                    // Obtenemos la posición absoluta del hijo en pantalla
            val location = IntArray(2)
            child.getLocationOnScreen(location)

            val rect = Rect(
                    location[0],                    // left
                    location[1],                    // top
                    location[0] + child.width,      // right
                    location[1] + child.height      // bottom
            )

            // Verificamos si el toque cayó dentro de este hijo
            if (rect.contains(x, y)) {
                // Si el hijo es un ViewGroup, buscamos recursivamente dentro de él
                if (child is ViewGroup && child.childCount > 0) {
                    val deeperView = findDeepestViewAt(child, x, y)
                    if (deeperView != null) return deeperView
                }
                // Si llegamos aquí, este hijo es el más profundo que contiene el toque
                return child
            }
        }
    // Ningún hijo coincide; el toque fue sobre el ViewGroup en sí
    return null
    }

    /**
     * Resuelve el identificador legible de una vista Android.
     *
     * Prioridad:
     *  1. Nombre del recurso XML (ej: "btn_confirmar") → más descriptivo
     *  2. Hash del objeto View (ej: "view_1a2b3c4d") → identificable pero no semántico
     *  3. "unknown" → si la vista es null
     *
     * @param view La vista cuyo ID queremos resolver
     * @return String con el identificador de la vista
     */
    private fun resolveViewId(view: View?): String {
        if (view == null) return "unknown"

        // Intentamos obtener el nombre del recurso del ID
        // (el nombre que se definió en XML como android:id="@+id/NombreAquí")
        if (view.id != View.NO_ID) {
            return try {
                activity.resources.getResourceEntryName(view.id)
                // Retorna el nombre del recurso, ej: "btn_login", "tv_titulo"
            } catch (e: Exception) {
                // El ID existe pero no tiene nombre de recurso (ID generado en código)
                "view_${Integer.toHexString(view.id)}"
            }
        }

        // La vista no tiene ID asignado; usamos su hashCode como identificador único
        return "view_${Integer.toHexString(view.hashCode())}"
    }
}
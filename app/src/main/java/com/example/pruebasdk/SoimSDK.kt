package com.example.pruebasdk

import android.app.Application
import android.util.Log
import java.util.UUID

// =============================================================================
// SoimSDK.kt
// SOIM SDK - Sistema de Optimización de Interfaces Móviles
//
// DÍA 1: Módulo principal del SDK. Es el punto de entrada único para la
// app anfitriona y el cerebro del sistema.
//
// FILOSOFÍA DE DISEÑO:
// ─────────────────────
// Del documento del proyecto (Objetivo 1):
//   "Se desarrollará un SDK ligero y no intrusivo utilizando Kotlin."
//   "con una o dos líneas de código de inicialización"
//
// Por eso usamos el patrón Singleton (object en Kotlin), que permite
// inicializar el SDK con exactamente UNA línea en Application.onCreate():
//
//   SoimSDK.init(this)
//
// Nada más. El SDK se encarga del resto automáticamente.
//
// ARQUITECTURA INTERNA (Días 1 y 2):
// ────────────────────────────────────
//
//   [App anfitriona]
//        │ SoimSDK.init(application)
//        ▼
//   [SoimSDK] ──── registra ──▶ [SoimActivityTracker]
//                                        │
//                          instala en cada Activity
//                                        │
//                                        ▼
//                              [SoimWindowCallback]
//                                        │
//                           captura ACTION_DOWN events
//                                        │
//                                        ▼
//                              [SoimSDK.onEventCaptured()]
//                                        │
//                              [buffer local en memoria]
//                                        │
//                             (Días 3+: batching → API REST
//                              → Kafka → ClickHouse → Grafana)
// =============================================================================

/**
 * Singleton principal del SDK SOIM.
 *
 * Coordinador central que mantiene el estado global del SDK y expone
 * la API pública hacia la app anfitriona y la API interna hacia los
 * demás componentes del SDK (SoimWindowCallback, SoimActivityTracker).
 *
 * Uso mínimo desde la app anfitriona:
 * ```kotlin
 * // En Application.onCreate() o MainActivity.onCreate():
 * SoimSDK.init(this)
 * ```
 *
 * Uso con configuración de privacidad:
 * ```kotlin
 * SoimSDK.init(this)
 * SoimSDK.setTrackingEnabled(userAcceptedPrivacyPolicy)
 * ```
 */
object SoimSDK {

    private const val TAG = "SOIM_SDK"

    // ─────────────────────────────────────────────────────────────────────────
    // Estado interno del SDK
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Flag que indica si el SDK ya fue inicializado.
     * Evita dobles inicializaciones si init() es llamado más de una vez.
     */
    private var isInitialized: Boolean = false

    /**
     * Controla si el tracking está activo.
     * REGLA DE CONTROL (Proceso A del proyecto):
     * "Solo se envían datos si el usuario aceptó el aviso de privacidad."
     *
     * Por defecto está en FALSE para respetar la privacidad del usuario.
     * La app anfitriona DEBE llamar setTrackingEnabled(true) solo después
     * de que el usuario haya aceptado explícitamente la política de privacidad.
     */
    private var trackingEnabled: Boolean = false

    /**
     * Referencia al tracker de Activities/Fragments.
     * Nullable porque solo existe después de llamar a init().
     */
    private var activityTracker: SoimActivityTracker? = null

    /**
     * Identificador único de la sesión actual.
     * Se genera al inicializar el SDK y persiste durante toda la vida
     * del proceso de la app. Agrupa todos los eventos de una misma
     * apertura de la aplicación para reconstruir el flujo del usuario.
     *
     * Formato: UUID v4, ej: "550e8400-e29b-41d4-a716-446655440000"
     */
    private var sessionId: String = ""

    /**
     * Buffer local de eventos capturados, en espera de ser enviados al backend.
     *
     * ESTRATEGIA DE BATCHING (Objetivo 2 del proyecto):
     * Los eventos se acumulan aquí y se envían en lotes para:
     *  - Reducir el número de peticiones HTTP (eficiencia de red)
     *  - Permitir envío solo bajo WiFi (regla opcional de ahorro de datos)
     *  - Garantizar que no se pierden eventos si no hay conexión temporal
     *
     * En Días 3+, este buffer se procesa con corrutinas de Kotlin y se
     * transmite vía Retrofit → API REST → Kafka → ClickHouse.
     */
    private val eventBuffer = mutableListOf<ClickEvent>()

    // Límite máximo de eventos en el buffer antes de forzar un flush
    private const val MAX_BUFFER_SIZE = 50

    // ─────────────────────────────────────────────────────────────────────────
    // API PÚBLICA: métodos que usa la app anfitriona
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Inicializa el SDK. Debe llamarse UNA VEZ, idealmente en Application.onCreate()
     * o en el onCreate() de la primera Activity.
     *
     * Proceso de inicialización:
     *  1. Genera un sessionId único para esta sesión
     *  2. Crea el SoimActivityTracker
     *  3. Lo registra en Application para recibir callbacks de ALL Activities
     *
     * @param application Contexto de Application, necesario para registrar
     *                    ActivityLifecycleCallbacks a nivel de proceso.
     *                    NO usar Activity o Context para evitar memory leaks.
     */
    fun init(application: Application) {
        // Protección contra doble inicialización
        if (isInitialized) {
            Log.w(TAG, "⚠ SDK ya inicializado. Ignorando llamada duplicada a init().")
            return
        }

        // Generamos el identificador único de sesión
        sessionId = UUID.randomUUID().toString()

        // Creamos el tracker de pantallas (DÍA 2)
        activityTracker = SoimActivityTracker()

        // Registramos el tracker para recibir eventos de TODAS las Activities
        // del proceso, sin necesidad de modificar ninguna Activity individual
        application.registerActivityLifecycleCallbacks(activityTracker)

        isInitialized = true

        Log.i(TAG, "✅ SOIM SDK inicializado. SessionID: $sessionId")
        Log.i(TAG, "ℹ Tracking: ${if (trackingEnabled) "ACTIVO" else "INACTIVO (esperando consentimiento)"}")
    }

    /**
     * Activa o desactiva la captura de eventos.
     *
     * Debe llamarse DESPUÉS de que el usuario acepte la política de privacidad.
     * Si el usuario retira su consentimiento, llamar con false para detener
     * inmediatamente toda captura de datos.
     *
     * @param enabled true para activar tracking, false para desactivarlo
     */
    fun setTrackingEnabled(enabled: Boolean) {
        trackingEnabled = enabled
        Log.i(TAG, "Tracking ${if (enabled) "ACTIVADO ✅" else "DESACTIVADO 🚫"}")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // API INTERNA: métodos usados por SoimWindowCallback y SoimActivityTracker
    // Marcados como "internal" para que solo sean accesibles desde el mismo módulo
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Indica si el tracking está habilitado por el usuario.
     * Consultado por [SoimWindowCallback] antes de procesar cada evento.
     *
     * @return true si se debe capturar el evento, false si se debe ignorar
     */
    internal fun isTrackingEnabled(): Boolean = trackingEnabled && isInitialized

    /**
     * Retorna el nombre de la pantalla actualmente activa.
     * Consultado por [SoimWindowCallback] al construir cada [ClickEvent].
     *
     * @return Nombre legible de la pantalla, ej: "HomeActivity > DashboardFragment"
     */
    internal fun getCurrentScreenName(): String {
        return activityTracker?.currentScreenName ?: "Unknown"
    }

    /**
     * Retorna el identificador de la sesión actual.
     *
     * @return UUID de la sesión, generado en init()
     */
    internal fun getSessionId(): String = sessionId

    /**
     * Recibe un [ClickEvent] capturado por [SoimWindowCallback] y lo agrega
     * al buffer local para su posterior envío al backend.
     *
     * En Días 3+, aquí también se activará la lógica de batching:
     *  - Si el buffer llega a MAX_BUFFER_SIZE → flush inmediato
     *  - Si hay WiFi disponible → puede hacer flush preventivo
     *  - Si no hay conexión → el buffer se persiste en disco (Room/SharedPrefs)
     *
     * @param event El evento de clic capturado
     */
    internal fun onEventCaptured(event: ClickEvent) {
        // Agregamos al buffer en memoria
        synchronized(eventBuffer) {
            eventBuffer.add(event)

            Log.d(TAG, "📥 Evento en buffer [${eventBuffer.size}/$MAX_BUFFER_SIZE]: " +
                    "view='${event.viewId}' | (${event.rawX}, ${event.rawY}) | " +
                    "pantalla='${event.screenName}'")

            // Si el buffer está lleno, hacemos flush (Días 3+: envío real)
            if (eventBuffer.size >= MAX_BUFFER_SIZE) {
                flushBuffer()
            }
        }
    }

    /**
     * Fuerza el envío de todos los eventos en el buffer al backend.
     *
     * Por ahora (Días 1 y 2) solo imprime un resumen en Logcat.
     * En Días 3+, aquí se implementará:
     *  - Serialización a JSON (Gson/Moshi)
     *  - Envío vía Retrofit a la API REST
     *  - Manejo de reintentos con backoff exponencial
     *  - Regla opcional: solo enviar bajo WiFi
     *
     * Puede llamarse manualmente desde la app anfitriona, por ejemplo
     * cuando la app pasa a background (onStop) para no perder eventos.
     */
    fun flushBuffer() {
        synchronized(eventBuffer) {
            if (eventBuffer.isEmpty()) return

            Log.i(TAG, "📤 Flush: enviando ${eventBuffer.size} eventos al backend...")
            Log.i(TAG, "━━━━━━ Eventos capturados (sesión: $sessionId) ━━━━━━")

            eventBuffer.forEachIndexed { index, event ->
                Log.i(TAG, "[$index] view='${event.viewId}' | " +
                        "(${String.format("%.0f", event.rawX)}, ${String.format("%.0f", event.rawY)}) | " +
                        "ts=${event.timestamp} | pantalla='${event.screenName}'")
            }

            Log.i(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            // TODO (Día 3): Aquí va el envío real vía Retrofit
            // val request = BatchEventRequest(sessionId, eventBuffer.toList())
            // apiService.sendEvents(request)

            eventBuffer.clear()
        }
    }

    /**
     * Retorna una copia inmutable del buffer actual de eventos.
     * Útil para debugging y tests.
     *
     * @return Lista de eventos pendientes de envío
     */
    fun getBufferedEvents(): List<ClickEvent> {
        return synchronized(eventBuffer) { eventBuffer.toList() }
    }
}
package com.example.pruebasdk

// =============================================================================
// ClickEvent.kt
// SOIM SDK - Sistema de Optimización de Interfaces Móviles
//
// DÍA 2: Modelo de datos que representa un evento de interacción capturado.
// Este es el "paquete de información" que viaja desde el dispositivo del
// usuario hasta el backend (Go/NodeJS) → Kafka → ClickHouse.
//
// Cada campo mapea directamente a las variables definidas en el Proceso A
// del documento del proyecto:
//   - viewId    → "View ID (el botón presionado)"
//   - rawX, rawY → "Coordenadas X,Y"
//   - timestamp  → "Marca de tiempo (Timestamp)"
//   - screenName → identifica en qué pantalla (Activity/Fragment) ocurrió
//   - sessionId  → agrupa eventos de una misma sesión de uso
// =============================================================================

/**
 * Representa un único evento de clic/toque registrado por el SDK.
 *
 * Es una data class inmutable; una vez creado el evento, no se modifica.
 * Será serializada a JSON (formato definido en el Proceso A) antes de
 * ser enviada al broker de mensajería.
 *
 * Ejemplo de JSON resultante:
 * {
 *   "viewId":     "btn_login",
 *   "rawX":       542.0,
 *   "rawY":       1203.5,
 *   "timestamp":  1746500000000,
 *   "screenName": "LoginActivity",
 *   "sessionId":  "a1b2c3d4"
 * }
 */
data class ClickEvent(

    /**
     * Identificador de la vista tocada.
     * Si la vista tiene un android:id definido en XML, se usa el nombre del
     * recurso (ej: "btn_login"). Si no tiene id, se usa "view_<hashCode>".
     * Si no se identifica ninguna vista, se registra "unknown".
     */
    val viewId: String,

    /**
     * Coordenada X absoluta en píxeles, relativa a la pantalla completa.
     * "rawX" es la posición real en pantalla, independiente del scroll.
     * Usamos Float para máxima precisión en pantallas de alta densidad (xxhdpi).
     */
    val rawX: Float,

    /**
     * Coordenada Y absoluta en píxeles, relativa a la pantalla completa.
     * Junto con rawX forman el par de coordenadas que alimentará
     * el algoritmo de densidad de puntos para generar los Heatmaps.
     */
    val rawY: Float,

    /**
     * Marca de tiempo Unix en milisegundos (epoch ms) del momento exacto
     * en que ocurrió el toque (ACTION_DOWN). Permite:
     *  - Ordenar eventos cronológicamente en ClickHouse
     *  - Calcular el flujo de usuario entre pantallas
     *  - Filtrar eventos por ventanas de tiempo en Grafana/Superset
     */
    val timestamp: Long,

    /**
     * Nombre de la pantalla (Activity o Fragment) donde ocurrió el evento.
     * DÍA 2: Este campo es clave para filtrar el análisis por pantalla,
     * ya que la misma coordenada (X,Y) puede representar botones distintos
     * en pantallas diferentes.
     * Ejemplo: "MainActivity", "LoginActivity", "CheckoutFragment"
     */
    val screenName: String,

    /**
     * Identificador único de la sesión de uso actual.
     * Agrupa todos los eventos de una misma apertura de la app.
     * Se genera en SoimSDK.init() y persiste hasta que la app se cierra.
     * Permite reconstruir el flujo completo de navegación de un usuario.
     */
    val sessionId: String
)
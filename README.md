# Sistema de Optimización de Interfaces Móviles)

Este documento resume el progreso completo y todos los cambios técnicos implementados en el proyecto, documentados cronológicamente según las peticiones realizadas.

---

### Petición 1: Implementación del cronograma base basado en Proyecto.tex
**Objetivo:** Desarrollar el SDK de análisis de interacción y su persistencia local.

**Cambios realizados:**
*   **Análisis inicial:** Se revisó el documento LaTeX para extraer el cronograma y los requisitos (Días 1 al 7).
*   **Días 1 y 2 (Captura de Eventos):** Se desarrolló la infraestructura central del SDK (`SoimSDK.kt`, `SoimWindowCallback.kt` y `SoimActivityTracker.kt`) para interceptar clics globales usando el patrón Decorator, extrayendo las coordenadas reales `(X, Y)` y validando las pantallas (`View ID`).
*   **Día 3 (Persistencia Local):** Se construyó `SoimDatabaseHelper.kt` configurando una base de datos SQLite. Se configuró un buffer que guarda los registros en disco cada 5 interacciones para no saturar los hilos de red/I-O.
*   **Día 4 (App de Prueba):** Se modificó `activity_main.xml` colocando botones dispersos (esquinas y centro) para emular el uso real de una aplicación y probar la recolección de métricas.
*   **Día 5 (Procesamiento de Densidad):** Se programó `HeatmapProcessor.kt` utilizando una fórmula de distancia Euclidiana para agrupar clics cercanos (clústeres) y calcular el "peso" o densidad de una misma zona.
*   **Día 6 (Visualización):** Se implementó `HeatmapView.kt`, el cual inyecta gráficamente círculos con gradientes de color (Azul, Verde, Rojo) sobre la pantalla (Modo Administrador) sin bloquear la interacción con la UI que se encuentra debajo.

---

### Petición 2: Implementación de Exportación (Día 7)
**Objetivo:** Permitir la exportación de los clics recolectados.

**Cambios realizados:**
*   **Día 7 (Exportación a CSV):** Se programó el script `CsvExporter.kt` que recupera toda la base de datos de SQLite y la transforma a un documento en texto plano `.csv`.
*   **Configuración del sistema:** Se añadió un bloque `<provider>` en `AndroidManifest.xml` (apoyado por un nuevo archivo de reglas `file_paths.xml`) para permitir la funcionalidad de compartir archivos a aplicaciones externas (correo, drive, etc.) usando el selector nativo de Android.

---

### Petición 3: Ajustes de Sistema y Archivo APK
**Objetivo:** Evitar que los botones choquen con el UI del sistema Android y exportar el entregable para pruebas grupales.

**Cambios realizados:**
*   **Edge-to-Edge Padding:** Se integró la funcionalidad de márgenes en `MainActivity.kt` (`ViewCompat.setOnApplyWindowInsetsListener`) para que el SDK respete la barra de estado superior (reloj) y la barra de navegación inferior.
*   **Aclaración de Entregable:** Se le proporcionó al usuario la ruta exacta del APK ya compilado (`app/build/outputs/apk/debug/app-debug.apk`) con instrucciones detalladas de cómo el resto del equipo lo podía instalar y probar en sus dispositivos físicos.

---

### Petición 4: Llenado del documento LaTeX (Proyecto.tex)
**Objetivo:** Redactar y estructurar el formato académico del documento basándose en la metodología y todo el código generado.

**Cambios realizados:**
*   **Reestructuración académica:** Se eliminó la estructura antigua de "Etapas" y se implementó un índice formal (Introducción, Marco Teórico, Objetivos, Procedimiento y Metodología Experimental).
*   **Redacción técnica:** Se documentaron detalladamente los procedimientos de los 7 días de trabajo.
*   **Inyección de Código:** Se aislaron e inyectaron las partes clave del código fuente de Kotlin y se añadió la fórmula matemática de la distancia euclidiana en LaTeX (`\begin{equation}`) para enriquecer el documento y prepararlo para captura de pantallas.

---

### Petición 5: Reversión de cambios que rompieron la compilación
**Objetivo:** Arreglar el código que crasheaba después de que el usuario insertó cambios manuales incompatibles.

**Cambios realizados:**
*   **Diagnóstico Git:** Se utilizó `git diff` para rastrear cambios ajenos al proceso de desarrollo actual.
*   **Reversión (Rollback):** Se restauraron los archivos `SoimWindowCallback.kt` y `gradle/libs.versions.toml`, quitando métodos mal implementados (`onPointerCaptureChanged`), la variable de extensión sin contexto (`isNotEmpty()`) y un salto de versión incompatible de Android Gradle Plugin.
*   **Compilación exitosa:** Se devolvió el repositorio a su estado óptimo de compilación.

---

### Petición 6: Solución de crash SQLITE_BUSY en "Modo Admin"
**Objetivo:** Solventar un error en tiempo de ejecución de la base de datos local al intentar leer el mapa de calor.

**Cambios realizados:**
*   **Diagnóstico de Concurrencia:** Se identificó que el sistema operativo arrojaba un error `code 5 (SQLITE_BUSY)`.
*   **Parche SQLite:** Se detectó que el comando de cierre `db.close()` forzaba un bloqueo de hilos asíncrono. Se eliminaron todas las declaraciones `db.close()` manuales de las consultas dentro de `SoimDatabaseHelper.kt` dejando que el objeto `SQLiteOpenHelper` maneje eficientemente el grupo de conexiones, resolviendo el crash en la ventana de administrador por completo.

---

### Petición 7: Botón para Limpiar la Base de Datos
**Objetivo:** Añadir control visual para purgar datos y reiniciar las pruebas, acompañado de una alerta de confirmación.

**Cambios realizados:**
*   **Modificación de Vista:** Se integró un nuevo botón rojo "Clear" en el `LinearLayout` del archivo `activity_main.xml`.
*   **Actualización del Helper:** Se añadió la función `clearAllEvents()` dentro de la base de datos SQLite.
*   **Lógica de Confirmación:** En `MainActivity.kt`, se programó un `AlertDialog` nativo de Android. Si el usuario selecciona "Confirmar", la tabla se vacía, se limpia la vista gráfica del Heatmap (si estaba activa), y se lanza una alerta de confirmación ("Datos borrados").

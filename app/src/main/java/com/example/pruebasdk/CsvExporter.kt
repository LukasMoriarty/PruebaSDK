package com.example.pruebasdk

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter

class CsvExporter {
    companion object {
        fun exportAndShareCsv(context: Context) {
            val events = SoimSDK.dbHelper?.getAllEvents() ?: emptyList()

            if (events.isEmpty()) {
                Toast.makeText(context, "No hay eventos para exportar", Toast.LENGTH_SHORT).show()
                return
            }

            try {
                // Crear archivo CSV
                val fileName = "soim_events_export.csv"
                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
                val writer = FileWriter(file)

                // Escribir cabecera
                writer.append("SessionID,ScreenName,ViewID,Timestamp,RawX,RawY\n")

                // Escribir datos
                for (event in events) {
                    writer.append("${event.sessionId},")
                    writer.append("${event.screenName},")
                    writer.append("${event.viewId},")
                    writer.append("${event.timestamp},")
                    writer.append("${event.rawX},")
                    writer.append("${event.rawY}\n")
                }

                writer.flush()
                writer.close()

                Toast.makeText(context, "CSV Exportado a Documentos", Toast.LENGTH_SHORT).show()

                // Intentar compartir el archivo
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "Exportación de Datos SOIM")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(Intent.createChooser(intent, "Compartir CSV..."))

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error al exportar CSV: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

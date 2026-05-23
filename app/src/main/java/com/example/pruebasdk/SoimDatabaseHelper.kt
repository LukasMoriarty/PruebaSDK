package com.example.pruebasdk

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.core.database.sqlite.transaction

class SoimDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "SOIM_Database"
        private const val DATABASE_NAME = "soim_events.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_EVENTS = "click_events"
        const val COLUMN_ID = "id"
        const val COLUMN_VIEW_ID = "view_id"
        const val COLUMN_RAW_X = "raw_x"
        const val COLUMN_RAW_Y = "raw_y"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_SCREEN_NAME = "screen_name"
        const val COLUMN_SESSION_ID = "session_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = ("CREATE TABLE " + TABLE_EVENTS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_VIEW_ID + " TEXT,"
                + COLUMN_RAW_X + " REAL,"
                + COLUMN_RAW_Y + " REAL,"
                + COLUMN_TIMESTAMP + " INTEGER,"
                + COLUMN_SCREEN_NAME + " TEXT,"
                + COLUMN_SESSION_ID + " TEXT" + ")")
        db.execSQL(createTableQuery)
        Log.i(TAG, "Base de datos creada exitosamente.")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EVENTS")
        onCreate(db)
    }

    fun insertEvents(events: List<ClickEvent>) {
        val db = this.writableDatabase
        db.transaction {
            try {
                for (event in events) {
                    val values = ContentValues().apply {
                        put(COLUMN_VIEW_ID, event.viewId)
                        put(COLUMN_RAW_X, event.rawX)
                        put(COLUMN_RAW_Y, event.rawY)
                        put(COLUMN_TIMESTAMP, event.timestamp)
                        put(COLUMN_SCREEN_NAME, event.screenName)
                        put(COLUMN_SESSION_ID, event.sessionId)
                    }
                    insert(TABLE_EVENTS, null, values)
                }
                Log.i(TAG, "Se insertaron ${events.size} eventos en SQLite.")
            } catch (e: Exception) {
                Log.e(TAG, "Error insertando eventos: ${e.message}")
            }
        }
    }

    fun getAllEvents(): List<ClickEvent> {
        val eventsList = mutableListOf<ClickEvent>()
        val selectQuery = "SELECT * FROM $TABLE_EVENTS"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val viewId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VIEW_ID))
                val rawX = cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_RAW_X))
                val rawY = cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_RAW_Y))
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                val screenName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SCREEN_NAME))
                val sessionId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SESSION_ID))

                val event = ClickEvent(viewId, rawX, rawY, timestamp, screenName, sessionId)
                eventsList.add(event)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return eventsList
    }

    fun getEventsByScreen(screenNameFilter: String): List<ClickEvent> {
        val eventsList = mutableListOf<ClickEvent>()
        val selectQuery = "SELECT * FROM $TABLE_EVENTS WHERE $COLUMN_SCREEN_NAME = ?"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, arrayOf(screenNameFilter))

        if (cursor.moveToFirst()) {
            do {
                val viewId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VIEW_ID))
                val rawX = cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_RAW_X))
                val rawY = cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_RAW_Y))
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                val screenName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SCREEN_NAME))
                val sessionId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SESSION_ID))

                val event = ClickEvent(viewId, rawX, rawY, timestamp, screenName, sessionId)
                eventsList.add(event)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return eventsList
    }

    fun clearAllEvents() {
        val db = this.writableDatabase
        db.delete(TABLE_EVENTS, null, null)
    }
}

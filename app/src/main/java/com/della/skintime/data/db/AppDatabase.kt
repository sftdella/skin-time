package com.della.skintime.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AppDatabase(context: Context) : SQLiteOpenHelper(context, "skintime.db", null, 3) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE checklist_steps (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                order_index INTEGER NOT NULL,
                is_optional INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE activity_schedule (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                days TEXT NOT NULL DEFAULT ''
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE routine_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                time TEXT NOT NULL,
                session_type TEXT NOT NULL,
                is_completed INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE routine_session_details (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id INTEGER NOT NULL,
                step_name TEXT NOT NULL,
                is_done INTEGER NOT NULL DEFAULT 0,
                is_optional INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        insertDefaultSteps(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS checklist_steps")
        db.execSQL("DROP TABLE IF EXISTS routine_records")
        db.execSQL("DROP TABLE IF EXISTS activity_schedule")
        db.execSQL("DROP TABLE IF EXISTS routine_sessions")
        db.execSQL("DROP TABLE IF EXISTS routine_session_details")
        onCreate(db)
    }

    private fun insertDefaultSteps(db: SQLiteDatabase) {
        val defaults = listOf(
            ChecklistStep(name = "Facial Wash",  description = "Bersihkan wajah dari kotoran",     orderIndex = 0, isOptional = false),
            ChecklistStep(name = "Toner",        description = "Seimbangkan pH kulit",             orderIndex = 1, isOptional = false),
            ChecklistStep(name = "Serum",        description = "Nutrisi untuk kulit wajah",        orderIndex = 2, isOptional = false),
            ChecklistStep(name = "Moisturizer",  description = "Kunci kelembapan kulit",           orderIndex = 3, isOptional = false),
            ChecklistStep(name = "Eye Cream",    description = "Rawat area mata agar tetap sehat", orderIndex = 4, isOptional = true),
            ChecklistStep(name = "Body Care",    description = "Merawat kulit tubuh agar lembap",  orderIndex = 5, isOptional = true)
        )
        defaults.forEach { step ->
            db.insert("checklist_steps", null, stepToValues(step))
        }
    }

    // ── Checklist Steps ──────────────────────────────────────────────────────

    fun getAllSteps(): List<ChecklistStep> {
        val list = mutableListOf<ChecklistStep>()
        val cursor = readableDatabase.query(
            "checklist_steps", null, null, null, null, null, "order_index ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list += ChecklistStep(
                    id          = it.getInt(it.getColumnIndexOrThrow("id")),
                    name        = it.getString(it.getColumnIndexOrThrow("name")),
                    description = it.getString(it.getColumnIndexOrThrow("description")),
                    orderIndex  = it.getInt(it.getColumnIndexOrThrow("order_index")),
                    isOptional  = it.getInt(it.getColumnIndexOrThrow("is_optional")) == 1
                )
            }
        }
        return list
    }

    fun insertStep(step: ChecklistStep): Long =
        writableDatabase.insert("checklist_steps", null, stepToValues(step))

    fun updateStep(step: ChecklistStep) {
        writableDatabase.update(
            "checklist_steps", stepToValues(step),
            "id = ?", arrayOf(step.id.toString())
        )
    }

    fun deleteStep(id: Int) {
        writableDatabase.delete("checklist_steps", "id = ?", arrayOf(id.toString()))
    }

    private fun stepToValues(step: ChecklistStep) = ContentValues().apply {
        put("name",        step.name)
        put("description", step.description)
        put("order_index", step.orderIndex)
        put("is_optional", if (step.isOptional) 1 else 0)
    }

    // ── Activity Schedule ────────────────────────────────────────────────────

    fun getAllActivities(): List<ActivitySchedule> {
        val list = mutableListOf<ActivitySchedule>()
        val cursor = readableDatabase.query(
            "activity_schedule", null, null, null, null, null, "id ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val daysStr = it.getString(it.getColumnIndexOrThrow("days"))
                list += ActivitySchedule(
                    id   = it.getInt(it.getColumnIndexOrThrow("id")),
                    name = it.getString(it.getColumnIndexOrThrow("name")),
                    days = if (daysStr.isBlank()) emptyList()
                           else daysStr.split(",").map { d -> d.toInt() }
                )
            }
        }
        return list
    }

    fun getActivitiesForDay(dayOfWeek: Int): List<ActivitySchedule> =
        getAllActivities().filter { dayOfWeek in it.days }

    fun insertActivity(activity: ActivitySchedule): Long =
        writableDatabase.insert("activity_schedule", null, activityToValues(activity))

    fun updateActivity(activity: ActivitySchedule) {
        writableDatabase.update(
            "activity_schedule", activityToValues(activity),
            "id = ?", arrayOf(activity.id.toString())
        )
    }

    fun deleteActivity(id: Int) {
        writableDatabase.delete("activity_schedule", "id = ?", arrayOf(id.toString()))
    }

    private fun activityToValues(a: ActivitySchedule) = ContentValues().apply {
        put("name", a.name)
        put("days", a.days.joinToString(","))
    }

    // ── Routine Sessions ─────────────────────────────────────────────────────

    fun createSession(session: RoutineSession): Long {
        val values = ContentValues().apply {
            put("date",         session.date)
            put("time",         session.time)
            put("session_type", session.sessionType)
            put("is_completed", if (session.isCompleted) 1 else 0)
        }
        return writableDatabase.insert("routine_sessions", null, values)
    }

    fun getSessionsByDate(date: String): List<RoutineSession> {
        val list = mutableListOf<RoutineSession>()
        val cursor = readableDatabase.query(
            "routine_sessions", null, "date = ?", arrayOf(date), null, null, "id ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list += rowToSession(it)
            }
        }
        return list
    }

    fun getActiveSession(date: String): RoutineSession? {
        val cursor = readableDatabase.query(
            "routine_sessions", null,
            "date = ? AND is_completed = 0", arrayOf(date),
            null, null, "id ASC", "1"
        )
        return cursor.use { if (it.moveToFirst()) rowToSession(it) else null }
    }

    fun completeSession(sessionId: Int) {
        writableDatabase.update(
            "routine_sessions",
            ContentValues().apply { put("is_completed", 1) },
            "id = ?", arrayOf(sessionId.toString())
        )
    }

    fun deleteSession(sessionId: Int) {
        writableDatabase.delete("routine_sessions", "id = ?", arrayOf(sessionId.toString()))
        writableDatabase.delete("routine_session_details", "session_id = ?", arrayOf(sessionId.toString()))
    }

    fun getAllSessions(): List<RoutineSession> {
        val list = mutableListOf<RoutineSession>()
        val cursor = readableDatabase.query(
            "routine_sessions", null, null, null, null, null, "date DESC, id ASC"
        )
        cursor.use { while (it.moveToNext()) list += rowToSession(it) }
        return list
    }

    fun getCompletedSessionDates(): Set<String> {
        val set = mutableSetOf<String>()
        val cursor = readableDatabase.rawQuery(
            "SELECT DISTINCT date FROM routine_sessions WHERE is_completed = 1", null
        )
        cursor.use { while (it.moveToNext()) set += it.getString(0) }
        return set
    }

    fun getMostUsedSteps(limit: Int = 5): List<Pair<String, Int>> {
        val list = mutableListOf<Pair<String, Int>>()
        val cursor = readableDatabase.rawQuery(
            """SELECT step_name, COUNT(*) as cnt
               FROM routine_session_details
               WHERE is_done = 1
               GROUP BY step_name
               ORDER BY cnt DESC
               LIMIT $limit""",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                list += it.getString(0) to it.getInt(1)
            }
        }
        return list
    }

    fun getSessionCountsByDate(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        val cursor = readableDatabase.rawQuery(
            """SELECT date, COUNT(*) as cnt
               FROM routine_sessions
               WHERE is_completed = 1
               GROUP BY date""",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                map[it.getString(0)] = it.getInt(1)
            }
        }
        return map
    }

    private fun rowToSession(cursor: android.database.Cursor) = RoutineSession(
        id          = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
        date        = cursor.getString(cursor.getColumnIndexOrThrow("date")),
        time        = cursor.getString(cursor.getColumnIndexOrThrow("time")),
        sessionType = cursor.getString(cursor.getColumnIndexOrThrow("session_type")),
        isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow("is_completed")) == 1
    )

    // ── Routine Session Details ──────────────────────────────────────────────

    fun insertSessionDetail(detail: RoutineSessionDetail): Long {
        val values = ContentValues().apply {
            put("session_id",  detail.sessionId)
            put("step_name",   detail.stepName)
            put("is_done",     if (detail.isDone) 1 else 0)
            put("is_optional", if (detail.isOptional) 1 else 0)
        }
        return writableDatabase.insert("routine_session_details", null, values)
    }

    fun getSessionDetails(sessionId: Int): List<RoutineSessionDetail> {
        val list = mutableListOf<RoutineSessionDetail>()
        val cursor = readableDatabase.query(
            "routine_session_details", null,
            "session_id = ?", arrayOf(sessionId.toString()),
            null, null, "id ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list += RoutineSessionDetail(
                    id         = it.getInt(it.getColumnIndexOrThrow("id")),
                    sessionId  = it.getInt(it.getColumnIndexOrThrow("session_id")),
                    stepName   = it.getString(it.getColumnIndexOrThrow("step_name")),
                    isDone     = it.getInt(it.getColumnIndexOrThrow("is_done")) == 1,
                    isOptional = it.getInt(it.getColumnIndexOrThrow("is_optional")) == 1
                )
            }
        }
        return list
    }

    fun updateDetailDone(detailId: Int, isDone: Boolean) {
        writableDatabase.update(
            "routine_session_details",
            ContentValues().apply { put("is_done", if (isDone) 1 else 0) },
            "id = ?", arrayOf(detailId.toString())
        )
    }
}

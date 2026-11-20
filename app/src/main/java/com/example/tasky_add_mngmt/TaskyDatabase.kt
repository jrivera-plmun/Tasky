package com.example.tasky_add_mngmt

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File


class TaskyDatabase(context: Context) : SQLiteOpenHelper(context, "tasky.db", null, 1) {
    private val appContext = context.applicationContext
    companion object {
        private fun prepare(context: Context): String {
            val target = context.getDatabasePath("tasky.db")
            if (!target.exists()) {
                target.parentFile?.mkdirs()
                context.assets.open("tasky.db").use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
            return target.absolutePath
        }
    }
    private val dbPath = prepare(context.applicationContext)
    override fun onCreate(db: SQLiteDatabase) = Unit
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    override fun getWritableDatabase(): SQLiteDatabase {
        val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)
        try {
            db.setForeignKeyConstraintsEnabled(true)
            migrateDatabase(db)
            assignLegacyTasks(db)
            return db
        } catch (error: Exception) {
            db.close()
            throw error
        }
    }

    private fun migrateDatabase(db: SQLiteDatabase) {
        if (db.version >= 2) return
        db.beginTransaction()
        try {
            db.execSQL("""CREATE TABLE IF NOT EXISTS focus_sessions (
                session_id INTEGER PRIMARY KEY AUTOINCREMENT,
                task_id INTEGER NOT NULL,
                started_at INTEGER NOT NULL,
                ended_at INTEGER,
                focused_seconds INTEGER NOT NULL DEFAULT 0,
                break_seconds INTEGER NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'Paused',
                FOREIGN KEY(task_id) REFERENCES tasks(task_id) ON DELETE CASCADE
            )""")
            db.execSQL("""CREATE TABLE IF NOT EXISTS focus_distractions (
                distraction_id INTEGER PRIMARY KEY AUTOINCREMENT,
                task_id INTEGER NOT NULL,
                session_id INTEGER,
                occurred_at INTEGER NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                FOREIGN KEY(task_id) REFERENCES tasks(task_id) ON DELETE CASCADE
            )""")
            db.execSQL("""CREATE TABLE IF NOT EXISTS focus_break_state (
                session_id INTEGER PRIMARY KEY,
                break_started_at INTEGER NOT NULL,
                FOREIGN KEY(session_id) REFERENCES focus_sessions(session_id) ON DELETE CASCADE
            )""")
            val columns = mutableSetOf<String>()
            db.rawQuery("PRAGMA table_info(tasks)", null).use { cursor ->
                while (cursor.moveToNext()) columns.add(cursor.getString(1))
            }
            if ("owner_user_id" !in columns) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN owner_user_id INTEGER")
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_tasks_owner ON tasks(owner_user_id)")
            db.version = 2
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun assignLegacyTasks(db: SQLiteDatabase) {
        val hasUnassignedTasks = db.rawQuery(
            "SELECT 1 FROM tasks WHERE owner_user_id IS NULL LIMIT 1", null
        ).use { it.moveToFirst() }
        if (!hasUnassignedTasks) return
        val accountTableExists = db.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type='table' AND name='local_accounts'", null
        ).use { it.moveToFirst() }
        if (!accountTableExists) return
        val firstId = db.rawQuery("SELECT MIN(user_id) FROM local_accounts", null).use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        }
        if (firstId != null) {
            db.execSQL("UPDATE tasks SET owner_user_id=? WHERE owner_user_id IS NULL", arrayOf(firstId))
        }
    }

    private fun ownerId(): Long = AccountStore(appContext).currentId
    private fun requireOwner(): Long = ownerId().also { require(it > 0L) { "Please sign in first" } }
    private fun ownedSession(db: SQLiteDatabase, sessionId: Long): Boolean = db.rawQuery(
        "SELECT 1 FROM focus_sessions f JOIN tasks t ON t.task_id=f.task_id WHERE f.session_id=? AND t.owner_user_id=?",
        arrayOf(sessionId.toString(), ownerId().toString())
    ).use { it.moveToFirst() }
    private fun ownedTask(db: SQLiteDatabase, taskId: Long): Boolean = db.rawQuery(
        "SELECT 1 FROM tasks WHERE task_id=? AND owner_user_id=?", arrayOf(taskId.toString(), ownerId().toString())
    ).use { it.moveToFirst() }

    override fun getReadableDatabase(): SQLiteDatabase = writableDatabase

    fun saveTask(id: Long?, title: String, description: String, category: String, priority: String,
                 deadline: Long, duration: Int, subtasks: List<String>, difficulty: String = "Moderate"): Long {
        val owner = requireOwner()
        val db = writableDatabase
        db.beginTransaction()
        try {
            if (id != null) require(ownedTask(db, id)) { "Task not found" }
            val categoryId = db.rawQuery("SELECT category_id FROM categories WHERE category_name=?", arrayOf(category)).use {
                if (!it.moveToFirst()) throw IllegalArgumentException("Choose a valid category")
                it.getLong(0)
            }
            val now = System.currentTimeMillis()
            val values = ContentValues().apply {
                put("owner_user_id", owner); put("category_id", categoryId); put("title", title); put("description", description)
                put("priority", priority); put("difficulty", difficulty)
                put("estimated_minutes", duration); put("deadline", deadline); put("updated_at", now)
                if (id == null) {
                    put("remaining_minutes", duration); put("status", "Pending"); put("created_at", now)
                }
            }
            val taskId = if (id == null) db.insertOrThrow("tasks", null, values) else {
                if (db.update("tasks", values, "task_id=? AND owner_user_id=?", arrayOf(id.toString(), owner.toString())) != 1) throw IllegalArgumentException("Task not found")
                id
            }
            if (id != null) db.delete("subtasks", "task_id=?", arrayOf(taskId.toString()))
            subtasks.filter { it.isNotBlank() }.forEach { sub ->
                db.insertOrThrow("subtasks", null, ContentValues().apply { put("task_id", taskId); put("title", sub.trim()) })
            }
            db.setTransactionSuccessful()
            return taskId
        } finally { db.endTransaction(); db.close() }
    }
    
    
    data class FocusSession(val id: Long, val taskId: Long, val startedAt: Long,
                            val focusedSeconds: Long, val status: String)

    
    fun activeFocusSessionAny(): FocusSession? = readableDatabase.use { db ->
        db.rawQuery("SELECT session_id,task_id,started_at,focused_seconds,status FROM focus_sessions WHERE status='Active' AND task_id IN (SELECT task_id FROM tasks WHERE owner_user_id=?) ORDER BY started_at DESC LIMIT 1", arrayOf(ownerId().toString())).use { c ->
            if (c.moveToFirst()) FocusSession(c.getLong(0), c.getLong(1), c.getLong(2), c.getLong(3), c.getString(4)) else null
        }
    }

    
    fun runningFocusOrBreakAny(): FocusSession? = readableDatabase.use { db ->
        db.rawQuery("""SELECT f.session_id,f.task_id,f.started_at,f.focused_seconds,f.status
            FROM focus_sessions f LEFT JOIN focus_break_state b ON b.session_id=f.session_id
            WHERE (f.status='Active' OR (f.status='Paused' AND b.session_id IS NOT NULL)) AND f.task_id IN (SELECT task_id FROM tasks WHERE owner_user_id=?)
            ORDER BY f.session_id DESC LIMIT 1""", arrayOf(ownerId().toString())).use { c ->
            if (c.moveToFirst()) FocusSession(c.getLong(0),c.getLong(1),c.getLong(2),c.getLong(3),c.getString(4)) else null
        }
    }

    fun activeFocusSession(taskId: Long): FocusSession? = readableDatabase.use { db ->
        db.rawQuery("SELECT session_id,task_id,started_at,focused_seconds,status FROM focus_sessions WHERE task_id=? AND status='Active' AND task_id IN (SELECT task_id FROM tasks WHERE owner_user_id=?) ORDER BY session_id DESC LIMIT 1", arrayOf(taskId.toString(), ownerId().toString())).use { c ->
            if (c.moveToFirst()) FocusSession(c.getLong(0),c.getLong(1),c.getLong(2),c.getLong(3),c.getString(4)) else null
        }
    }

    fun startFocus(taskId: Long): Long = writableDatabase.use { db ->
        require(ownedTask(db, taskId)) { "Task not found" }
        db.beginTransaction()
        try {
            val status = db.rawQuery("SELECT status FROM tasks WHERE task_id=?", arrayOf(taskId.toString())).use { c ->
                if (!c.moveToFirst()) error("Task not found")
                c.getString(0)
            }
            require(status != "Completed") { "Completed tasks cannot start focus sessions" }
            
            val active = db.rawQuery("SELECT session_id FROM focus_sessions WHERE task_id=? AND status='Active' LIMIT 1", arrayOf(taskId.toString())).use { c -> if (c.moveToFirst()) c.getLong(0) else null }
            val sessionId = active ?: db.insertOrThrow("focus_sessions", null, ContentValues().apply {
                put("task_id", taskId); put("started_at", System.currentTimeMillis())
                put("focused_seconds", 0); put("break_seconds", 0); put("status", "Active")
            })
            db.execSQL("UPDATE tasks SET status='Ongoing', updated_at=? WHERE task_id=?", arrayOf(System.currentTimeMillis(), taskId))
            db.setTransactionSuccessful()
            sessionId
        } finally { db.endTransaction() }
    }

    fun pauseFocus(sessionId: Long, additionalSeconds: Long = 0L, finished: Boolean = false) {
        writableDatabase.use { db ->
            if (!ownedSession(db, sessionId)) return@use
            db.beginTransaction()
            try {
                val session = db.rawQuery(
                    "SELECT task_id,started_at FROM focus_sessions WHERE session_id=? AND status='Active'",
                    arrayOf(sessionId.toString())
                ).use { c -> if (c.moveToFirst()) Pair(c.getLong(0), c.getLong(1)) else null }
                if (session != null) {
                    val (taskId, startedAt) = session
                    val now = System.currentTimeMillis()
                    val elapsed = ((now - startedAt) / 1000L).coerceAtLeast(0L)
                    db.execSQL("UPDATE focus_sessions SET focused_seconds=focused_seconds+?,status=?,ended_at=? WHERE session_id=?",
                        arrayOf(elapsed, if (finished) "Completed" else "Paused", now, sessionId))
                    val seconds = db.rawQuery("SELECT COALESCE(SUM(focused_seconds),0) FROM focus_sessions WHERE task_id=?",
                        arrayOf(taskId.toString())).use { c -> c.moveToFirst(); c.getLong(0) }
                    db.execSQL("UPDATE tasks SET remaining_minutes=MAX(0,estimated_minutes-?),updated_at=? WHERE task_id=? AND status!='Completed'",
                        arrayOf(seconds / 60L, now, taskId))
                }
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }
    }

    
    fun startBreak(sessionId: Long) {
        writableDatabase.use { db ->
            if (!ownedSession(db, sessionId)) return@use
            db.beginTransaction()
            try {
                val now = System.currentTimeMillis()
                val changed = db.compileStatement("""UPDATE focus_sessions SET
                    focused_seconds=focused_seconds+MAX(0,(?-started_at)/1000),
                    started_at=?, ended_at=NULL, status='Paused'
                    WHERE session_id=? AND status='Active'""").apply {
                    bindLong(1, now); bindLong(2, now); bindLong(3, sessionId)
                }.executeUpdateDelete()
                if (changed > 0) {
                    db.execSQL("INSERT OR REPLACE INTO focus_break_state(session_id,break_started_at) VALUES (?,?)", arrayOf(sessionId, now))
                }
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }
    }

    fun endBreak(sessionId: Long, resumeFocus: Boolean) {
        writableDatabase.use { db ->
            if (!ownedSession(db, sessionId)) return@use
            db.beginTransaction()
            try {
                val now = System.currentTimeMillis()
                val breakStart = db.rawQuery("SELECT break_started_at FROM focus_break_state WHERE session_id=?", arrayOf(sessionId.toString())).use { c ->
                    if (c.moveToFirst()) c.getLong(0) else null
                }
                if (breakStart != null) {
                    db.execSQL("""UPDATE focus_sessions SET
                        break_seconds=break_seconds+?, started_at=?, status=?, ended_at=?
                        WHERE session_id=? AND status='Paused'""", arrayOf(
                        ((now-breakStart)/1000L).coerceAtLeast(0L), now,
                        if (resumeFocus) "Active" else "Paused",
                        if (resumeFocus) null else now, sessionId
                    ))
                    db.execSQL("DELETE FROM focus_break_state WHERE session_id=?", arrayOf(sessionId))
                }
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }
    }

    fun breakSession(taskId: Long): FocusSession? = readableDatabase.use { db ->
        if (!ownedTask(db, taskId)) return@use null
        db.rawQuery("""SELECT f.session_id,f.task_id,f.started_at,f.focused_seconds,f.status
            FROM focus_sessions f JOIN focus_break_state b ON b.session_id=f.session_id
            WHERE f.task_id=? AND f.status='Paused' ORDER BY f.session_id DESC LIMIT 1""", arrayOf(taskId.toString())).use { c ->
            if (c.moveToFirst()) FocusSession(c.getLong(0),c.getLong(1),c.getLong(2),c.getLong(3),c.getString(4)) else null
        }
    }

    fun breakSeconds(taskId: Long): Long = readableDatabase.use { db ->
        if (!ownedTask(db, taskId)) return@use 0L
        db.rawQuery("""SELECT COALESCE(SUM(f.break_seconds +
            CASE WHEN b.session_id IS NOT NULL THEN MAX(0,(?-b.break_started_at)/1000) ELSE 0 END),0)
            FROM focus_sessions f LEFT JOIN focus_break_state b ON b.session_id=f.session_id
            WHERE f.task_id=?""", arrayOf(System.currentTimeMillis().toString(),taskId.toString())).use { c -> c.moveToFirst(); c.getLong(0) }
    }

    fun logDistraction(taskId: Long, sessionId: Long?, note: String) {
        writableDatabase.use { db ->
            require(ownedTask(db, taskId)) { "Task not found" }
            if (sessionId != null) require(ownedSession(db, sessionId)) { "Session not found" }
            db.insertOrThrow("focus_distractions", null, ContentValues().apply {
                put("task_id",taskId); if (sessionId != null) put("session_id",sessionId)
                put("occurred_at",System.currentTimeMillis()); put("note",note.take(250))
            })
        }
    }
    fun distractionCount(taskId: Long? = null): Int = readableDatabase.use { db ->
        val where = if (taskId == null) "" else " AND task_id=?"
        db.rawQuery("SELECT COUNT(*) FROM focus_distractions WHERE task_id IN (SELECT task_id FROM tasks WHERE owner_user_id=?)$where", if (taskId == null) arrayOf(ownerId().toString()) else arrayOf(ownerId().toString(),taskId.toString())).use { c -> c.moveToFirst(); c.getInt(0) }
    }
    fun totalBreakSeconds(): Long = readableDatabase.use { db ->
        db.rawQuery("""SELECT COALESCE(SUM(f.break_seconds + CASE WHEN b.session_id IS NOT NULL THEN MAX(0,(?-b.break_started_at)/1000) ELSE 0 END),0) FROM focus_sessions f LEFT JOIN focus_break_state b ON b.session_id=f.session_id WHERE f.task_id IN (SELECT task_id FROM tasks WHERE owner_user_id=?)""",arrayOf(System.currentTimeMillis().toString(),ownerId().toString())).use { c -> c.moveToFirst(); c.getLong(0) }
    }

    fun focusedSeconds(taskId: Long): Long = readableDatabase.use { db ->
        if (!ownedTask(db, taskId)) return@use 0L
        db.rawQuery("""SELECT COALESCE(SUM(focused_seconds + CASE WHEN status='Active'
            THEN MAX(0,(? - started_at)/1000) ELSE 0 END),0)
            FROM focus_sessions WHERE task_id=?""",
            arrayOf(System.currentTimeMillis().toString(), taskId.toString())).use { c ->
            c.moveToFirst(); c.getLong(0)
        }
    }

    data class FocusHistoryRow(val taskTitle: String, val startedAt: Long, val seconds: Long, val status: String)
    data class FocusSummary(val totalSeconds: Long, val completedSessions: Int, val sessions: List<FocusHistoryRow>, val dailySeconds: List<Long>)

    
    fun focusSummary(): FocusSummary = readableDatabase.use { db ->
        val now = System.currentTimeMillis()
        val records = mutableListOf<FocusHistoryRow>()
        db.rawQuery("""SELECT COALESCE(t.title,'Deleted task'),f.started_at,
            f.focused_seconds + CASE WHEN f.status='Active' THEN MAX(0,(? - f.started_at)/1000) ELSE 0 END,
            f.status FROM focus_sessions f LEFT JOIN tasks t ON t.task_id=f.task_id
            WHERE f.task_id IN (SELECT task_id FROM tasks WHERE owner_user_id=?) ORDER BY f.started_at DESC LIMIT 100""", arrayOf(now.toString(),ownerId().toString())).use { c ->
            while (c.moveToNext()) records.add(FocusHistoryRow(c.getString(0),c.getLong(1),c.getLong(2),c.getString(3)))
        }
        val totals = db.rawQuery("""SELECT COALESCE(SUM(focused_seconds + CASE WHEN status='Active'
            THEN MAX(0,(? - started_at)/1000) ELSE 0 END),0),
            COALESCE(SUM(CASE WHEN status='Completed' THEN 1 ELSE 0 END),0)
            FROM focus_sessions WHERE task_id IN (SELECT task_id FROM tasks WHERE owner_user_id=?)""", arrayOf(now.toString(),ownerId().toString())).use { c ->
            c.moveToFirst(); Pair(c.getLong(0),c.getInt(1))
        }
        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY,0); set(java.util.Calendar.MINUTE,0)
            set(java.util.Calendar.SECOND,0); set(java.util.Calendar.MILLISECOND,0)
        }
        val days = (6 downTo 0).map { offset ->
            val start = (today.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR,-offset) }.timeInMillis
            val end = start + 86400000L
            db.rawQuery("""SELECT COALESCE(SUM(focused_seconds + CASE WHEN status='Active'
                THEN MAX(0,(? - started_at)/1000) ELSE 0 END),0)
                FROM focus_sessions WHERE started_at>=? AND started_at<? AND task_id IN (SELECT task_id FROM tasks WHERE owner_user_id=?)""",
                arrayOf(now.toString(),start.toString(),end.toString(),ownerId().toString())).use { c -> c.moveToFirst(); c.getLong(0) }
        }
        FocusSummary(totals.first,totals.second,records,days)
    }

    fun deleteTask(id: Long) { writableDatabase.use { it.delete("tasks", "task_id=? AND owner_user_id=?", arrayOf(id.toString(),ownerId().toString())) }; TaskReminderScheduler.cancel(appContext, id) }
    fun completeTask(id: Long) {
        writableDatabase.use { db ->
            db.update("tasks", ContentValues().apply {
                put("status", "Completed"); put("remaining_minutes", 0)
                put("completed_at", System.currentTimeMillis()); put("updated_at", System.currentTimeMillis())
            }, "task_id=? AND owner_user_id=?", arrayOf(id.toString(),ownerId().toString()))
        }
        TaskReminderScheduler.cancel(appContext, id)
    }

    data class Task(val id: Long, val title: String, val description: String, val category: String,
                    val priority: String, val deadline: Long, val estimated: Int, val remaining: Int, val status: String, val focusedSeconds: Long = 0L, val difficulty: String = "Moderate") {
        val overdue get() = status != "Completed" && deadline < System.currentTimeMillis()
        val percent get() = if (status == "Completed") 100 else ((focusedSeconds * 100L) / (estimated.toLong() * 60L).coerceAtLeast(1L)).toInt().coerceIn(0, 100)
    }
    fun tasks(): List<Task> = readableDatabase.use { db ->
        db.rawQuery("""SELECT t.task_id,t.title,COALESCE(t.description,''),c.category_name,
            t.priority,t.deadline,t.estimated_minutes,t.remaining_minutes,t.status,
            COALESCE((SELECT SUM(fs.focused_seconds + CASE WHEN fs.status='Active'
                THEN MAX(0,(CAST(strftime('%s','now') AS INTEGER)*1000-fs.started_at)/1000)
                ELSE 0 END) FROM focus_sessions fs WHERE fs.task_id=t.task_id),0), COALESCE(t.difficulty,'Moderate')
            FROM tasks t JOIN categories c ON c.category_id=t.category_id WHERE t.owner_user_id=? ORDER BY t.deadline""", arrayOf(ownerId().toString())).use { c ->
            buildList { while(c.moveToNext()) add(Task(c.getLong(0),c.getString(1),c.getString(2),
                c.getString(3),c.getString(4),c.getLong(5),c.getInt(6),c.getInt(7),c.getString(8),c.getLong(9),c.getString(10))) }
        }
    }
    fun task(id: Long): Task? = tasks().firstOrNull { it.id == id }
    fun reopenTask(id: Long) {
        writableDatabase.use { db ->
            db.execSQL("UPDATE tasks SET status='Pending', remaining_minutes=estimated_minutes, completed_at=NULL, updated_at=? WHERE task_id=? AND owner_user_id=?",
                arrayOf(System.currentTimeMillis(), id, ownerId()))
        }
        TaskReminderScheduler.refresh(appContext)
    }
}

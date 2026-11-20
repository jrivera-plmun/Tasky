package com.example.tasky_add_mngmt

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec


class AccountStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("tasky_session", Context.MODE_PRIVATE)
    private fun open(): SQLiteDatabase {
        val db = TaskyDatabase(context).writableDatabase
        db.execSQL("""CREATE TABLE IF NOT EXISTS local_accounts (
            user_id INTEGER PRIMARY KEY AUTOINCREMENT,
            full_name TEXT NOT NULL, email TEXT NOT NULL UNIQUE COLLATE NOCASE,
            password_salt TEXT NOT NULL, password_hash TEXT NOT NULL,
            bio TEXT NOT NULL DEFAULT '', birthday TEXT NOT NULL DEFAULT ''
        )""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS local_preferences (
            user_id INTEGER PRIMARY KEY, theme TEXT NOT NULL DEFAULT 'system',
            reminders INTEGER NOT NULL DEFAULT 1, overdue INTEGER NOT NULL DEFAULT 1,
            weekly INTEGER NOT NULL DEFAULT 0,
            FOREIGN KEY(user_id) REFERENCES local_accounts(user_id) ON DELETE CASCADE
        )""")
        return db
    }
    data class Account(val id: Long, val name: String, val email: String, val bio: String, val birthday: String)
    val currentId: Long get() = prefs.getLong("user_id", -1L)
    private fun hash(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, 120000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded.joinToString("") { "%02x".format(it.toInt() and 255) } }
        finally { spec.clearPassword() }
    }
    fun register(name: String, email: String, password: String): Boolean {
        require(name.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) { "Enter a valid name and email" }
        require(password.length >= 8) { "Password must contain at least 8 characters" }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val db = open()
        return try {
            val id = db.insertOrThrow("local_accounts", null, ContentValues().apply {
                put("full_name", name.trim()); put("email", email.trim().lowercase())
                put("password_salt", salt.joinToString("") { "%02x".format(it.toInt() and 255) })
                put("password_hash", hash(password, salt))
            })
            prefs.edit().putLong("user_id", id).apply()
            TaskyDatabase(context).writableDatabase.close()
            TaskReminderScheduler.refresh(context)
            true
        } catch (_: android.database.sqlite.SQLiteConstraintException) { false }
        finally { db.close() }
    }
    fun login(email: String, password: String): Boolean {
        val db = open()
        return try {
            db.rawQuery("SELECT user_id,password_salt,password_hash FROM local_accounts WHERE email=? COLLATE NOCASE", arrayOf(email.trim())).use { c ->
                if (!c.moveToFirst()) return false
                val salt = c.getString(1).chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                val matched = java.security.MessageDigest.isEqual(hash(password, salt).toByteArray(), c.getString(2).toByteArray())
                if (matched) { prefs.edit().putLong("user_id", c.getLong(0)).apply(); TaskReminderScheduler.refresh(context) }
                matched
            }
        } finally { db.close() }
    }
    fun changePassword(oldPassword: String, newPassword: String): Boolean {
        if (currentId < 0 || newPassword.length < 8) return false
        val db = open()
        return try {
            db.rawQuery("SELECT password_salt,password_hash FROM local_accounts WHERE user_id=?", arrayOf(currentId.toString())).use { c ->
                if (!c.moveToFirst()) return false
                val salt = c.getString(0).chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                if (!java.security.MessageDigest.isEqual(hash(oldPassword, salt).toByteArray(), c.getString(1).toByteArray())) return false
                val newSalt = ByteArray(16).also { SecureRandom().nextBytes(it) }
                db.update("local_accounts", ContentValues().apply {
                    put("password_salt", newSalt.joinToString("") { "%02x".format(it.toInt() and 255) })
                    put("password_hash", hash(newPassword, newSalt))
                }, "user_id=?", arrayOf(currentId.toString())) == 1
            }
        } finally { db.close() }
    }
    fun logout() {
        val ids = TaskyDatabase(context).tasks().map { it.id }
        ids.forEach { TaskReminderScheduler.cancel(context, it) }
        prefs.edit().remove("user_id").apply()
    }
    fun current(): Account? {
        if (currentId < 0) return null
        val db = open()
        return try { db.rawQuery("SELECT user_id,full_name,email,bio,birthday FROM local_accounts WHERE user_id=?", arrayOf(currentId.toString())).use { c ->
            if (c.moveToFirst()) Account(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4)) else null
        } } finally { db.close() }
    }
    fun update(name: String, email: String, bio: String, birthday: String): Boolean {
        if (currentId < 0 || name.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) return false
        val db = open()
        return try { db.update("local_accounts", ContentValues().apply {
            put("full_name",name.trim());put("email",email.trim().lowercase());put("bio",bio);put("birthday",birthday)
        }, "user_id=?", arrayOf(currentId.toString())) == 1 }
        catch (_: android.database.sqlite.SQLiteConstraintException) { false }
        finally { db.close() }
    }
    fun preference(key: String, default: String): String {
        if (currentId < 0 || key !in setOf("theme","reminders","overdue","weekly")) return default
        val db = open()
        return try { db.rawQuery("SELECT $key FROM local_preferences WHERE user_id=?", arrayOf(currentId.toString())).use { c -> if (c.moveToFirst()) c.getString(0) else default } }
        finally { db.close() }
    }
    fun setPreference(key: String, value: String) {
        if (currentId < 0 || key !in setOf("theme","reminders","overdue","weekly")) return
        val db = open()
        try {
            db.execSQL("INSERT OR IGNORE INTO local_preferences(user_id) VALUES(?)", arrayOf(currentId))
            db.update("local_preferences", ContentValues().apply { put(key,value) }, "user_id=?", arrayOf(currentId.toString()))
        } finally { db.close() }
    }
}

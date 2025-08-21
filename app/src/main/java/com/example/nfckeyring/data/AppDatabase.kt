package com.example.nfckeyring.data

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.nfckeyring.util.SecurePrefs
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom

@Database(entities = [TagEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context): AppDatabase {
            val prefs = SecurePrefs.getPrefs(context)
            val passphrase = prefs.getString("db_passphrase", null) ?: generatePassphrase(prefs)
            val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase.toCharArray()))
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "tag_db"
            ).openHelperFactory(factory).build()
        }

        private fun generatePassphrase(prefs: SharedPreferences): String {
            val random = ByteArray(32)
            SecureRandom().nextBytes(random)
            val passphrase = random.joinToString("") { "%02x".format(it) }
            prefs.edit().putString("db_passphrase", passphrase).apply()
            return passphrase
        }
    }
}

package com.portscanpro.data.database

import android.content.Context
import androidx.room.*

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val scanType: String,
    val target: String,
    val duration: Long = 0,
    val hostsFound: Int = 0,
    val portsFound: Int = 0,
    val resultsJson: String = ""
)

@Dao
interface ScanDao {
    @Query("SELECT * FROM scans ORDER BY timestamp DESC")
    suspend fun getAllScans(): List<ScanEntity>

    @Query("SELECT * FROM scans WHERE id = :id")
    suspend fun getScanById(id: Long): ScanEntity?

    @Insert
    suspend fun insertScan(scan: ScanEntity): Long

    @Delete
    suspend fun deleteScan(scan: ScanEntity)

    @Query("DELETE FROM scans")
    suspend fun deleteAll()
}

@Database(entities = [ScanEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "portscanpro_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

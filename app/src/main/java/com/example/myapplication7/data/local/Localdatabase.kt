package com.example.myapplication7.data.local

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

// ЛР №9 Завдання 5: Кешування — Room Entity
// ЛР №12 Завдання 3: Додано поле imagePath для збереження шляху до фото
@Entity(tableName = "concerts")
data class ConcertEntity(
    @PrimaryKey val id: Int,
    val artist: String,
    val title: String,
    val date: String,
    val genre: String,
    val venue: String?,
    val description: String?,
    // ЛР №12 Завдання 3: Шлях до фото у filesDir (не бінарний вміст!)
    val imagePath: String? = null
)

// ЛР №9 Завдання 5: DAO для роботи з кешем
@Dao
@JvmSuppressWildcards
interface ConcertDao {
    @Query("SELECT * FROM concerts ORDER BY id ASC")
    fun getAllConcerts(): Flow<List<ConcertEntity>>

    @Query("SELECT * FROM concerts WHERE id = :id")
    suspend fun getConcertById(id: Int): ConcertEntity?

    @Upsert
    suspend fun upsertAll(concerts: List<ConcertEntity>): List<Long>

    @Query("DELETE FROM concerts WHERE id = :id")
    suspend fun deleteConcertById(id: Int): Int

    @Query("DELETE FROM concerts")
    suspend fun clearAll(): Int

    // ЛР №12 Завдання 3: Оновлення шляху до фото для конкретного концерту
    @Query("UPDATE concerts SET imagePath = :imagePath WHERE id = :id")
    suspend fun updateImagePath(id: Int, imagePath: String?): Int
}

// ЛР №12: Міграція 1→2 — додаємо колонку imagePath
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE concerts ADD COLUMN imagePath TEXT")
    }
}

// ЛР №9 Завдання 5: Room Database
// ЛР №12: version = 2 з міграцією
@Database(entities = [ConcertEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun concertDao(): ConcertDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "beatstream_db"
                )
                    .addMigrations(MIGRATION_1_2)  // ЛР №12: міграція схеми
                    .build().also { INSTANCE = it }
            }
        }
    }
}
package com.example.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// --- Room Entities ---

@Entity(tableName = "favorite_restaurants")
data class FavoriteRestaurantEntity(
    @PrimaryKey val restaurantId: Int,
    val restaurantName: String
)

@Entity(tableName = "savings_records")
data class SavingsRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val restaurantName: String,
    val cheaperPlatform: String,
    val savedAmount: Double,
    val timestamp: Long = System.currentTimeMillis()
)

// --- Daos ---

@Dao
interface FoodCompareDao {
    @Query("SELECT * FROM favorite_restaurants")
    fun getFavoriteRestaurants(): Flow<List<FavoriteRestaurantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteRestaurantEntity)

    @Query("DELETE FROM favorite_restaurants WHERE restaurantId = :id")
    suspend fun deleteFavorite(id: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_restaurants WHERE restaurantId = :id LIMIT 1)")
    suspend fun isFavorite(id: Int): Boolean

    // Savings CRUD
    @Query("SELECT * FROM savings_records ORDER BY timestamp DESC")
    fun getAllSavingsRecords(): Flow<List<SavingsRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsRecord(record: SavingsRecordEntity)

    @Query("SELECT SUM(savedAmount) FROM savings_records")
    fun getTotalLifetimeSavings(): Flow<Double?>

    @Query("DELETE FROM savings_records")
    suspend fun clearHistory()
}

// --- App Database ---

@Database(
    entities = [FavoriteRestaurantEntity::class, SavingsRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): FoodCompareDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "food_compare_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- Repository Pattern ---

class FoodCompareRepository(private val dao: FoodCompareDao) {
    val favoriteRestaurants: Flow<List<FavoriteRestaurantEntity>> = dao.getFavoriteRestaurants()
    val savingsRecords: Flow<List<SavingsRecordEntity>> = dao.getAllSavingsRecords()
    val totalLifetimeSavings: Flow<Double?> = dao.getTotalLifetimeSavings()

    suspend fun toggleFavorite(id: Int, name: String) {
        if (dao.isFavorite(id)) {
            dao.deleteFavorite(id)
        } else {
            dao.insertFavorite(FavoriteRestaurantEntity(id, name))
        }
    }

    suspend fun isFavorite(id: Int): Boolean {
        return dao.isFavorite(id)
    }

    suspend fun saveSavingsRecord(restaurantName: String, cheaperPlatform: String, amount: Double) {
        if (amount > 0) {
            dao.insertSavingsRecord(
                SavingsRecordEntity(
                    restaurantName = restaurantName,
                    cheaperPlatform = cheaperPlatform,
                    savedAmount = amount
                )
            )
        }
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }
}

package com.asifulla.maya.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "macros")
data class MacroEntity(@PrimaryKey val name: String, val stepsJson: String, val updatedAt: Long = System.currentTimeMillis())

@Dao
interface MacroDao {
    @Query("SELECT * FROM macros ORDER BY updatedAt DESC") fun observe(): Flow<List<MacroEntity>>
    @Query("SELECT * FROM macros WHERE name = :name LIMIT 1") suspend fun get(name: String): MacroEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(macro: MacroEntity)
    @Delete suspend fun delete(macro: MacroEntity)
}

@Database(entities = [MacroEntity::class], version = 1, exportSchema = false)
abstract class MayaDatabase : RoomDatabase() { abstract fun macroDao(): MacroDao }

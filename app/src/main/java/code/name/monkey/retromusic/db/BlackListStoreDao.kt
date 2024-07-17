package code.name.monkey.retromusic.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BlackListStoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBlacklistPath(blackListStoreEntity: BlackListStoreEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlacklistPath(blackListStoreEntities: List<BlackListStoreEntity>)

    @Delete
    suspend fun deleteBlacklistPath(blackListStoreEntity: BlackListStoreEntity)

    @Query("DELETE FROM BlackListStoreEntity")
    suspend fun clearBlacklist()

    @Query("SELECT * FROM BlackListStoreEntity")
    fun blackListPaths(): List<BlackListStoreEntity>
}
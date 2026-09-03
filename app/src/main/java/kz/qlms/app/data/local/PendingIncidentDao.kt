package kz.qlms.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingIncidentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingIncidentEntity)

    @Update
    suspend fun update(entity: PendingIncidentEntity)

    @Delete
    suspend fun delete(entity: PendingIncidentEntity)

    @Query("SELECT * FROM pending_incidents ORDER BY createdAtEpochMs ASC")
    suspend fun getAll(): List<PendingIncidentEntity>

    @Query("SELECT * FROM pending_incidents ORDER BY createdAtEpochMs ASC")
    fun observeAll(): Flow<List<PendingIncidentEntity>>

    @Query("SELECT COUNT(*) FROM pending_incidents")
    fun observeCount(): Flow<Int>
}

package com.ignaciovalero.saludario.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ignaciovalero.saludario.data.local.entity.HealthRecord
import com.ignaciovalero.saludario.data.local.entity.HealthRecordType
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthRecordDao {

    @Query("SELECT * FROM health_records ORDER BY recorded_at DESC")
    fun observeAll(): Flow<List<HealthRecord>>

    @Query("SELECT * FROM health_records WHERE type = :type ORDER BY recorded_at DESC")
    fun observeByType(type: HealthRecordType): Flow<List<HealthRecord>>

    @Query("SELECT * FROM health_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): HealthRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: HealthRecord): Long

    @Update
    suspend fun update(record: HealthRecord)

    @Delete
    suspend fun delete(record: HealthRecord)

    @Query("DELETE FROM health_records")
    suspend fun deleteAll()
}

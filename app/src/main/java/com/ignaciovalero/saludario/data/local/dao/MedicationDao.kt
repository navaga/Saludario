package com.ignaciovalero.saludario.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ignaciovalero.saludario.data.local.entity.MedicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    @Query("SELECT * FROM medications ORDER BY name ASC")
    fun observeAll(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: Long): MedicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medication: MedicationEntity): Long

    @Update
    suspend fun update(medication: MedicationEntity)

    @Query("UPDATE medications SET stock_remaining = :stockRemaining WHERE id = :medicationId")
    suspend fun updateStockRemaining(medicationId: Long, stockRemaining: Double)

    @Query(
        "UPDATE medications SET stock_total = :stockTotal, stock_remaining = :stockRemaining, low_stock_threshold = :lowStockThreshold WHERE id = :medicationId"
    )
    suspend fun updateStockFields(
        medicationId: Long,
        stockTotal: Double,
        stockRemaining: Double,
        lowStockThreshold: Double
    )

    @Delete
    suspend fun delete(medication: MedicationEntity)

    @Query("SELECT * FROM medications WHERE start_date <= :date AND (end_date IS NULL OR end_date >= :date)")
    suspend fun getActiveForDate(date: String): List<MedicationEntity>

    @Query("DELETE FROM medications")
    suspend fun deleteAll()
}
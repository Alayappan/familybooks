package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FamilyInvoiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM family_invoices WHERE isDeleted = 0 ORDER BY date DESC, createdAt DESC")
    fun getAllActiveInvoices(): Flow<List<FamilyInvoiceEntity>>

    @Query("SELECT * FROM family_invoices WHERE isDeleted = 0 ORDER BY date DESC, createdAt DESC")
    suspend fun getAllActiveInvoicesSnapshot(): List<FamilyInvoiceEntity>

    @Query("SELECT * FROM family_invoices WHERE id = :id")
    suspend fun getInvoiceById(id: String): FamilyInvoiceEntity?

    @Query("SELECT * FROM family_invoices WHERE updatedAt > :sinceTimestamp")
    suspend fun getInvoicesModifiedSince(sinceTimestamp: Long): List<FamilyInvoiceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(invoice: FamilyInvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(invoices: List<FamilyInvoiceEntity>)

    @Update
    suspend fun update(invoice: FamilyInvoiceEntity)

    @Query("UPDATE family_invoices SET status = 'SETTLED', settledAt = :settledAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markAsSettled(
        id: String,
        settledAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE family_invoices SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long = System.currentTimeMillis())
}

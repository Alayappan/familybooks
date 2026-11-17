package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY date DESC, createdAt DESC")
    fun getAllActiveTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY date DESC, createdAt DESC")
    suspend fun getAllActiveTransactionsSnapshot(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun hardDelete(id: String)
}

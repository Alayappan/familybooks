package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.FamilyInvoiceEntity
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.CoroutineScope

@Database(
    entities = [
        TransactionEntity::class,
        FamilyInvoiceEntity::class,
        FamilyMemberEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class FamilyBooksDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun familyMemberDao(): FamilyMemberDao

    companion object {
        @Volatile
        private var INSTANCE: FamilyBooksDatabase? = null

        fun getDatabase(context: Context, @Suppress("UNUSED_PARAMETER") scope: CoroutineScope): FamilyBooksDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FamilyBooksDatabase::class.java,
                    "familybooks_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

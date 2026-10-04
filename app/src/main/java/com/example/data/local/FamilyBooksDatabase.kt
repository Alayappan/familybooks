package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.FamilyInvoiceEntity
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.SyncLogEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        TransactionEntity::class,
        FamilyInvoiceEntity::class,
        FamilyMemberEntity::class,
        SyncLogEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class FamilyBooksDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun syncLogDao(): SyncLogDao

    companion object {
        @Volatile
        private var INSTANCE: FamilyBooksDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): FamilyBooksDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FamilyBooksDatabase::class.java,
                    "familybooks_db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        prepopulateDatabase(database)
                    }
                }
            }
        }

        private suspend fun prepopulateDatabase(db: FamilyBooksDatabase) {
            val now = System.currentTimeMillis()
            val dayMs = 24L * 60L * 60L * 1000L
            val monthMs = 30L * dayMs

            // 1. Current Device User (Self)
            val selfMember = FamilyMemberEntity(
                id = UUID.randomUUID().toString(),
                deviceId = "self_device",
                name = "Mom",
                deviceName = "Pixel 8",
                macAddress = "A4:C3:F1:00:23:45",
                role = "This Device",
                avatarColorHex = "#00D4B2",
                hostAddress = "127.0.0.1",
                port = 8976,
                isSelf = true,
                connectionStatus = "ONLINE",
                lastConnectedAt = now,
                createdAt = now,
                updatedAt = now
            )

            // 2. Connected P2P Family Member Device
            val dadPeerMember = FamilyMemberEntity(
                id = UUID.randomUUID().toString(),
                deviceId = "peer_dad_phone",
                name = "Dad",
                deviceName = "Galaxy S24",
                macAddress = "B8:27:EB:4A:89:12",
                role = "Galaxy S24",
                avatarColorHex = "#0A2540",
                hostAddress = "192.168.1.15",
                port = 8976,
                isSelf = false,
                connectionStatus = "SYNCED",
                lastConnectedAt = now - (dayMs * 1),
                createdAt = now,
                updatedAt = now
            )

            db.familyMemberDao().insertOrUpdateAll(listOf(selfMember, dadPeerMember))

            // Initial transactions across Current Month and Previous Month for MoM comparison
            val initialTransactions = listOf(
                // Current Month Transactions
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    type = "EXPENSE",
                    amount = 142.50,
                    date = now - (dayMs * 1),
                    category = "Groceries",
                    paymentMethod = "Cash",
                    paidBy = "Mom",
                    notes = "Weekly supermarket shopping at Trader Joe's",
                    createdAt = now - (dayMs * 1),
                    updatedAt = now - (dayMs * 1)
                ),
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    type = "EXPENSE",
                    amount = 85.00,
                    date = now - (dayMs * 2),
                    category = "Utilities",
                    paymentMethod = "UPI / Bank Transfer",
                    paidBy = "Dad",
                    notes = "High-speed home fiber internet monthly bill",
                    createdAt = now - (dayMs * 2),
                    updatedAt = now - (dayMs * 2)
                ),
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    type = "EXPENSE",
                    amount = 45.00,
                    date = now - (dayMs * 3),
                    category = "Dining",
                    paymentMethod = "Cash",
                    paidBy = "Mom",
                    notes = "Family pizza dinner",
                    createdAt = now - (dayMs * 3),
                    updatedAt = now - (dayMs * 3)
                ),
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    type = "INCOME",
                    amount = 3500.00,
                    date = now - (dayMs * 4),
                    category = "Salary",
                    paymentMethod = "UPI / Bank Transfer",
                    paidBy = "Dad",
                    notes = "Monthly family primary income",
                    createdAt = now - (dayMs * 4),
                    updatedAt = now - (dayMs * 4)
                ),
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    type = "EXPENSE",
                    amount = 60.00,
                    date = now - (dayMs * 5),
                    category = "Transport",
                    paymentMethod = "Card",
                    paidBy = "Dad",
                    notes = "Gas station fuel refill",
                    createdAt = now - (dayMs * 5),
                    updatedAt = now - (dayMs * 5)
                ),

                // Previous Month Transactions (for Month-on-Month comparison)
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    type = "EXPENSE",
                    amount = 185.00,
                    date = now - monthMs - (dayMs * 2),
                    category = "Groceries",
                    paymentMethod = "Cash",
                    paidBy = "Mom",
                    notes = "Last month supermarket run",
                    createdAt = now - monthMs - (dayMs * 2),
                    updatedAt = now - monthMs - (dayMs * 2)
                ),
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    type = "EXPENSE",
                    amount = 90.00,
                    date = now - monthMs - (dayMs * 3),
                    category = "Utilities",
                    paymentMethod = "UPI / Bank Transfer",
                    paidBy = "Dad",
                    notes = "Last month internet and electricity",
                    createdAt = now - monthMs - (dayMs * 3),
                    updatedAt = now - monthMs - (dayMs * 3)
                ),
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    type = "EXPENSE",
                    amount = 75.00,
                    date = now - monthMs - (dayMs * 4),
                    category = "Dining",
                    paymentMethod = "Card",
                    paidBy = "Dad",
                    notes = "Last month weekend restaurant dinner",
                    createdAt = now - monthMs - (dayMs * 4),
                    updatedAt = now - monthMs - (dayMs * 4)
                ),
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    type = "EXPENSE",
                    amount = 80.00,
                    date = now - monthMs - (dayMs * 6),
                    category = "Transport",
                    paymentMethod = "Card",
                    paidBy = "Dad",
                    notes = "Last month gas station fuel",
                    createdAt = now - monthMs - (dayMs * 6),
                    updatedAt = now - monthMs - (dayMs * 6)
                ),
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    type = "INCOME",
                    amount = 3400.00,
                    date = now - monthMs - (dayMs * 5),
                    category = "Salary",
                    paymentMethod = "UPI / Bank Transfer",
                    paidBy = "Dad",
                    notes = "Last month primary income",
                    createdAt = now - monthMs - (dayMs * 5),
                    updatedAt = now - monthMs - (dayMs * 5)
                )
            )
            db.transactionDao().insertOrUpdateAll(initialTransactions)

            // Informal invoice between Mom and Dad
            val initialInvoices = listOf(
                FamilyInvoiceEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Electricity bill 50% split",
                    fromMember = "Dad",
                    toMember = "Mom",
                    amount = 65.00,
                    date = now - (dayMs * 2),
                    dueDate = now + (dayMs * 5),
                    notes = "Mom paid utility invoice directly online",
                    status = "UNPAID",
                    createdAt = now - (dayMs * 2),
                    updatedAt = now - (dayMs * 2)
                )
            )
            db.invoiceDao().insertOrUpdateAll(initialInvoices)

            // Initial sync log
            db.syncLogDao().insertLog(
                SyncLogEntity(
                    id = UUID.randomUUID().toString(),
                    timestamp = now,
                    peerName = "Dad's Phone",
                    peerIp = "192.168.1.15",
                    direction = "INCOMING",
                    status = "SUCCESS",
                    message = "Connected to Dad's device. Stored profile as Family Member and synchronized ledger.",
                    recordsProcessed = 3
                )
            )
        }
    }
}

package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FamilyMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyMemberDao {
    @Query("SELECT * FROM family_members WHERE isDeleted = 0 ORDER BY isSelf DESC, updatedAt DESC, name ASC")
    fun getAllActiveMembers(): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE isDeleted = 0 ORDER BY isSelf DESC, updatedAt DESC, name ASC")
    suspend fun getAllActiveMembersSnapshot(): List<FamilyMemberEntity>

    @Query("SELECT * FROM family_members WHERE id = :id")
    suspend fun getMemberById(id: String): FamilyMemberEntity?

    @Query("SELECT * FROM family_members WHERE deviceId = :deviceId AND isDeleted = 0 LIMIT 1")
    suspend fun getMemberByDeviceId(deviceId: String): FamilyMemberEntity?

    @Query("SELECT * FROM family_members WHERE macAddress = :macAddress AND isDeleted = 0 LIMIT 1")
    suspend fun getMemberByMacAddress(macAddress: String): FamilyMemberEntity?

    @Query("SELECT * FROM family_members WHERE name = :name AND isDeleted = 0 LIMIT 1")
    suspend fun getMemberByName(name: String): FamilyMemberEntity?

    @Query("SELECT * FROM family_members WHERE isSelf = 1 AND isDeleted = 0 LIMIT 1")
    suspend fun getSelfMember(): FamilyMemberEntity?

    @Query("SELECT * FROM family_members WHERE updatedAt > :sinceTimestamp")
    suspend fun getMembersModifiedSince(sinceTimestamp: Long): List<FamilyMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(member: FamilyMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(members: List<FamilyMemberEntity>)

    @Update
    suspend fun update(member: FamilyMemberEntity)

    @Query("UPDATE family_members SET connectionStatus = :status, lastConnectedAt = :connectedAt, hostAddress = :ip, port = :port, updatedAt = :updatedAt WHERE deviceId = :deviceId")
    suspend fun updateDeviceStatus(
        deviceId: String,
        status: String,
        connectedAt: Long = System.currentTimeMillis(),
        ip: String,
        port: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE family_members SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long = System.currentTimeMillis())
}

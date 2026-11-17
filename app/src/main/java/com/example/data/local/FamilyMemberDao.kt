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
    @Query("SELECT * FROM family_members WHERE isDeleted = 0 ORDER BY isPrimary DESC, name ASC")
    fun getAllActiveMembers(): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE isDeleted = 0 ORDER BY isPrimary DESC, name ASC")
    suspend fun getAllActiveMembersSnapshot(): List<FamilyMemberEntity>

    @Query("SELECT * FROM family_members WHERE id = :id")
    suspend fun getMemberById(id: String): FamilyMemberEntity?

    @Query("SELECT * FROM family_members WHERE LOWER(name) = LOWER(:name) AND isDeleted = 0 LIMIT 1")
    suspend fun getMemberByName(name: String): FamilyMemberEntity?

    @Query("UPDATE family_members SET isPrimary = 0")
    suspend fun clearPrimaryFlags()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(member: FamilyMemberEntity)

    @Update
    suspend fun update(member: FamilyMemberEntity)

    @Query("UPDATE family_members SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long = System.currentTimeMillis())
}

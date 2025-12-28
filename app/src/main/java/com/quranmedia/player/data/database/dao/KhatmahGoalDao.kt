package com.quranmedia.player.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranmedia.player.data.database.entity.KhatmahGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KhatmahGoalDao {
    @Query("SELECT * FROM khatmah_goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<KhatmahGoalEntity>>

    @Query("SELECT * FROM khatmah_goals WHERE isActive = 1 LIMIT 1")
    fun getActiveGoal(): Flow<KhatmahGoalEntity?>

    @Query("SELECT * FROM khatmah_goals WHERE id = :id")
    suspend fun getGoalById(id: String): KhatmahGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: KhatmahGoalEntity)

    @Query("UPDATE khatmah_goals SET isActive = 0")
    suspend fun deactivateAllGoals()

    @Query("DELETE FROM khatmah_goals WHERE id = :id")
    suspend fun deleteGoal(id: String)
}

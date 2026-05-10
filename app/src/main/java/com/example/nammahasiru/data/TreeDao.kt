package com.example.nammahasiru.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TreeDao {
    @Insert
    suspend fun insertTree(tree: TreeEntity): Long

    @Update
    suspend fun updateTree(tree: TreeEntity)

    @Query("SELECT * FROM trees")
    fun getAllTrees(): Flow<List<TreeEntity>>

    @Query("SELECT COUNT(*) FROM trees")
    fun getTotalTreesPlanted(): Flow<Int>

    @Query("SELECT COUNT(*) FROM trees WHERE status = 'Survived'")
    fun getSurvivedTreesCount(): Flow<Int>
}

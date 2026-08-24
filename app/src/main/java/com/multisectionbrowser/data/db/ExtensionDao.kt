package com.multisectionbrowser.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ExtensionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(extension: ExtensionEntity)

    @Update
    suspend fun update(extension: ExtensionEntity)

    @Query("DELETE FROM extensions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM extensions ORDER BY createdAt DESC")
    fun getAllExtensions(): LiveData<List<ExtensionEntity>>

    @Query("SELECT * FROM extensions ORDER BY createdAt DESC")
    suspend fun getAllExtensionsSync(): List<ExtensionEntity>

    @Query("SELECT * FROM extensions WHERE id = :id")
    suspend fun getExtension(id: String): ExtensionEntity?

    @Query("SELECT * FROM extensions WHERE sourceUrl = :url")
    suspend fun getExtensionBySourceUrl(url: String): ExtensionEntity?
}
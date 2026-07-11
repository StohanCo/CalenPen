package com.calenpen.data.database.dao

import androidx.room.*
import com.calenpen.data.database.entities.Template
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: Template): Long

    @Update
    suspend fun updateTemplate(template: Template)

    @Delete
    suspend fun deleteTemplate(template: Template)

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getTemplateById(id: Long): Template?

    @Query("SELECT * FROM templates ORDER BY isBuiltIn DESC, name ASC")
    fun observeAllTemplates(): Flow<List<Template>>

    @Query("SELECT * FROM templates WHERE isBuiltIn = 1 ORDER BY name ASC")
    fun observeBuiltInTemplates(): Flow<List<Template>>

    @Query("SELECT * FROM templates WHERE isBuiltIn = 0 ORDER BY createdAt DESC")
    fun observeCustomTemplates(): Flow<List<Template>>

    @Query("SELECT * FROM templates WHERE type = :type ORDER BY isBuiltIn DESC")
    fun observeTemplatesByType(type: String): Flow<List<Template>>
}

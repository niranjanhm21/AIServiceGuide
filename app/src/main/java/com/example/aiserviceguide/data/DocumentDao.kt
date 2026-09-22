package com.example.aiserviceguide.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface DocumentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: Document)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<Document>)

    @Query("SELECT * FROM documents")
    suspend fun getAllDocuments(): List<Document>

    @Query("SELECT * FROM documents WHERE documentId = :documentId")
    suspend fun getDocumentById(documentId: String): Document?
}
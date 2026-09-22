package com.example.aiserviceguide.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface ServiceDocumentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceDocument(
        serviceDocument: ServiceDocument
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceDocuments(
        serviceDocuments: List<ServiceDocument>
    )

    @Query("""
        SELECT * FROM service_documents
        WHERE serviceId = :serviceId
    """)
    suspend fun getDocumentsForService(
        serviceId: String
    ): List<ServiceDocument>
}
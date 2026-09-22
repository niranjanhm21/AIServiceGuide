package com.example.aiserviceguide.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface ServiceProcedureDao {

    @Query("""
        SELECT * FROM service_procedures
        WHERE serviceId = :serviceId
        LIMIT 1
    """)
    suspend fun getProcedureForService(
        serviceId: String
    ): ServiceProcedure?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProcedures(
        procedures: List<ServiceProcedure>
    )
}
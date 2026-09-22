package com.example.aiserviceguide.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface ServiceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: Service)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServices(services: List<Service>)

    @Query("SELECT * FROM services")
    suspend fun getAllServices(): List<Service>

    @Query("SELECT * FROM services WHERE serviceId = :serviceId")
    suspend fun getServiceById(serviceId: String): Service?

    @Query("""
        SELECT * FROM services
        WHERE serviceName LIKE '%' || :query || '%'
    """)
    suspend fun searchServices(query: String): List<Service>
}
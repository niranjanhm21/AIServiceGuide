package com.example.aiserviceguide.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface ProblemDao {

    @Query("SELECT * FROM problems WHERE serviceId = :serviceId ORDER BY problemType")
    suspend fun getProblemsForService(serviceId: String): List<Problem>

    @Query("SELECT * FROM problems WHERE serviceId = :serviceId AND problemType = :problemType LIMIT 1")
    suspend fun getProblem(
        serviceId: String,
        problemType: String
    ): Problem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblems(problems: List<Problem>)
}

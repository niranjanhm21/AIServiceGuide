package com.example.aiserviceguide.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "services")
data class Service(
    @PrimaryKey
    val serviceId: String,

    val serviceName: String,

    val description: String,

    val jurisdiction: String,

    val department: String,

    val officialSource: String,

    val sourceConfidence: String
)
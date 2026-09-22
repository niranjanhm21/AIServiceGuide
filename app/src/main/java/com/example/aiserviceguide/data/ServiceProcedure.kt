package com.example.aiserviceguide.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "service_procedures")
data class ServiceProcedure(
    @PrimaryKey
    val serviceId: String,
    val applicationProcedure: String,
    val whereToApply: String,
    val sourceUrl: String,
    val officialHelpline: String = "",
    val verificationNote: String
)

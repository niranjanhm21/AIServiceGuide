package com.example.aiserviceguide.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "problems")
data class Problem(
    @PrimaryKey
    val problemId: String,
    val serviceId: String,
    val problemType: String,
    val title: String,
    val solution: String,
    val nextStep: String,
    val sourceUrl: String,
    val verificationNote: String
)

package com.example.aiserviceguide.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey
    val documentId: String,
    val documentName: String,
    val purpose: String,
    val sourceAuthority: String,
    val verificationNote: String,
    val howToObtain: String,
    val whereToObtain: String,
    val officialUrl: String = ""
)

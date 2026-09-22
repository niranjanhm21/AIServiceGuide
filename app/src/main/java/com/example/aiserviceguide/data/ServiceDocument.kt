package com.example.aiserviceguide.data

import androidx.room3.Entity

/**
 * Requirement status is service-specific.
 *
 * REQUIRED      = verified as mandatory for this service
 * OPTIONAL      = verified but not mandatory
 * NOT_REQUIRED  = verified as not required
 * VERIFY        = relationship exists, but requirement is not verified yet
 *
 * OFFICIAL is kept only as a legacy value and is treated as REQUIRED
 * by ServiceAssistant so older seeded data does not break the app.
 */
object RequirementStatus {
    const val REQUIRED = "REQUIRED"
    const val OPTIONAL = "OPTIONAL"
    const val NOT_REQUIRED = "NOT_REQUIRED"
    const val VERIFY = "VERIFY"
}

@Entity(
    tableName = "service_documents",
    primaryKeys = ["serviceId", "documentId"]
)
data class ServiceDocument(
    val serviceId: String,
    val documentId: String,
    val status: String,
    val notes: String,
    val verifiedAlternative: String,
    val sourceUrl: String = ""
)

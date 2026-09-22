package com.example.aiserviceguide.data

import androidx.room3.Database
import androidx.room3.RoomDatabase

@Database(
    entities = [
        Service::class,
        Document::class,
        ServiceDocument::class,
        ServiceProcedure::class,
        Problem::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun serviceDao(): ServiceDao

    abstract fun documentDao(): DocumentDao

    abstract fun serviceDocumentDao(): ServiceDocumentDao

    abstract fun serviceProcedureDao(): ServiceProcedureDao

    abstract fun problemDao(): ProblemDao
}

package com.example.aiserviceguide.data

import android.content.Context

object DatabaseTest {

    suspend fun testDatabase(context: Context): String {

        val database = DatabaseProvider.getDatabase(context)

        DatabaseSeeder.seedDatabase(database)

        val services = database.serviceDao().getAllServices()

        return "Database contains ${services.size} services"
    }
}
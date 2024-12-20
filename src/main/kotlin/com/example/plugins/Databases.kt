package com.example.plugins

import com.example.models.*
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/MyRent",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "postgres"
    )

    transaction(database) {
        SchemaUtils.create(Users, Vehicles)
    }
}
package com.example

import com.example.models.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.example.plugins.*
import com.example.routes.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureAuthentication()
    configureSwagger()
    configureDatabases()
    configureRouting()
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
}

fun Application.configureAuthentication() {
    authentication {
        jwt {
            // Configure your JWT authentication here
        }
    }
}

fun Application.configureSwagger() {
    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
    }
}

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        authenticate {
            userRoutes()
            // Uncomment these as you implement them
            // vehicleRoutes()
            // routeRoutes()
            // photoRoutes()
            // drivingBehaviorRoutes()
        }
    }
}

fun Application.configureDatabases() {
    val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/MyRent",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "postgres"
    )

    try {
        transaction(database) {
            exec("SELECT 1")
            println("Database connection successful!")
        }
    } catch (e: Exception) {
        println("Database connection failed: ${e.message}")
    }

    transaction(database) {
        SchemaUtils.create(Users, Vehicles, Routes, Photos, DrivingBehaviors)
        seedDatabase()
    }
}

fun seedDatabase() {
    transaction {
        try {
            // Insert random data into the tables
            repeat(10) {
                Users.insert {
                    it[name] = "User ${Random.nextInt(1, 100)}"
                    it[email] = "user${Random.nextInt(1, 100)}@example.com"
                    // Set other fields as needed
                }
            }
            repeat(5) {
                Vehicles.insert {
                    it[make] = "Make ${Random.nextInt(1, 10)}"
                    it[model] = "Model ${Random.nextInt(1, 10)}"
                    // Set other fields as needed
                }
            }
            // Seed other tables with random data as needed
        } catch (e: Exception) {
            rollback()
            println("Data seeding failed: ${e.message}")
        }
    }
}

// Define your table objects here
object Users : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50)
    val email = varchar("email", 100)
    // Define other columns as needed

    override val primaryKey = PrimaryKey(id)
}

object Vehicles : Table() {
    val id = integer("id").autoIncrement()
    val make = varchar("make", 50)
    val model = varchar("model", 50)
    // Define other columns as needed

    override val primaryKey = PrimaryKey(id)
}

// Define other table objects (Routes, Photos, DrivingBehaviors) similarly
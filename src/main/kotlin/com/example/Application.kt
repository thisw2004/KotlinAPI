package com.example

import com.example.models.Users.email
import com.example.plugins.UserService.Users.name
import com.example.plugins.configureSwagger
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.javatime.datetime
import kotlin.random.Random
import java.time.LocalDateTime
import io.ktor.server.plugins.openapi.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.swagger.*
//import io.swagger.v3.oas.models.info.Info
import io.ktor.server.application.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.openapi.*
//import jdk.jpackage.internal.Log.info

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
//
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml") {
            version = "4.15.5"
        }
    }
}

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Default endpoint from MyRent API.")
        }
        get("/seed-database") {
            seedDatabase()
            call.respondText("Database seeded successfully")
            //TODO: add error in case something goes wrong
        }
        authenticate {
            // userRoutes()
            // Uncomment these as you implement them
            // vehicleRoutes()
            // routeRoutes()
            // photoRoutes()
        }
    }
}


fun Application.configureOpenAPI() {
    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
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

    transaction {
        SchemaUtils.createMissingTablesAndColumns(Users, Vehicles, Photos)
    }
}

fun seedDatabase() {
    transaction {
        // Clear existing data



        // Seed Users
        val userIds = List(10) { index ->
            Users.insert {
                it[username] = "user${index + 1}"
                it[pw] = "password${index + 1}"
                it[mail] = "user${index + 1}@example.com"
            } get Users.id
        }

        // Seed Vehicles
        val vehicleIds = List(20) { index ->
            Vehicles.insert {
                it[rented] = Random.nextBoolean()
                it[userId] = userIds.random()
                it[brand] = listOf("Toyota", "Honda", "Ford", "BMW", "Mercedes").random()
                it[model] = "Model${index + 1}"
                it[buildYear] = Random.nextInt(2000, 2024)
                it[kenteken] = "ABC${Random.nextInt(100, 999)}"
                it[brandstof] = listOf("Petrol", "Diesel", "Electric", "Hybrid").random()
                it[verbruik] = Random.nextInt(5, 20)
                it[kmStand] = Random.nextInt(0, 200000)
                it[location] = listOf("New York", "Los Angeles", "Chicago", "Houston", "Phoenix").random()
            } get Vehicles.id
        }

        // Seed Photos
        val photoIds = vehicleIds.map { vehicleId ->
            Photos.insert {
                it[photoUrl] = "https://example.com/vehicle$vehicleId.jpg"
                it[carId] = vehicleId
            } get Photos.id
        }

        // Update vehicles with photo ids
        vehicleIds.zip(photoIds).forEach { (vehicleId, photoId) ->
            Vehicles.update({ Vehicles.id eq vehicleId }) {
                it[Vehicles.photoId] = photoId
            }
        }


    }
}

// Table definitions
object Users : Table() {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 255)
    val pw = varchar("pw", 255)
    val mail = varchar("mail", 255)

    override val primaryKey = PrimaryKey(id)
}

object Vehicles : Table() {
    val id = integer("id").autoIncrement()
    val rented = bool("rented")
    val userId = (integer("user_id").references(Users.id)).nullable()
    val brand = varchar("brand", 255)
    val model = varchar("model", 255)
    val buildYear = integer("buildyear")
    val kenteken = varchar("kenteken", 255)
    val brandstof = varchar("brandstof", 255)
    val verbruik = integer("verbruik")
    val kmStand = integer("km_stand")
    val photoId = (integer("photo_id").references(Photos.id)).nullable()
    val location = varchar("location", 255)

    override val primaryKey = PrimaryKey(id)
}

object Photos : Table() {
    val id = integer("id").autoIncrement()
    val photoUrl = varchar("photo_url", 255)
    val carId = (integer("car_id").references(Vehicles.id)).nullable()

    override val primaryKey = PrimaryKey(id)
}


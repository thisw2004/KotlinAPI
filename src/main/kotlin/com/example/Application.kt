package com.example

import com.example.plugins.configureSwagger
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random
//import io.swagger.v3.oas.models.info.Info
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

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


fun Application.configureDatabases() {
    val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/MyRent",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "postgres"
    )

    transaction(database) {
        SchemaUtils.createMissingTablesAndColumns(Users, Vehicles, Photos)
        exec("""
            DO $$ 
            BEGIN 
                ALTER TABLE vehicles ALTER COLUMN photo_id DROP NOT NULL;
            EXCEPTION 
                WHEN others THEN null; 
            END $$;
        """)
    }

    // Test the database connection
    testDatabaseConnection(database)
}

fun testDatabaseConnection(database: Database) {
    try {
        transaction(database) {
            exec("SELECT 1") { rs ->
                if (rs.next()) {
                    println("Database connection test successful!")
                } else {
                    throw Exception("Database connection test failed: No result returned")
                }
            }
        }
    } catch (e: Exception) {
        println("Database connection test failed: ${e.message}")
        throw e  // Re-throw the exception to halt the application if the connection fails
    }
}

fun seedDatabase() {
    transaction {
        // Seed Users
        val userIds = List(10) { index ->
            Users.insert {
                it[username] = "user${index + 1}"
                val (salt, hash) = hashPassword("password${index + 1}")
                it[password] = "$salt:$hash"
                it[email] = "user${index + 1}@example.com"
            } get Users.id
        }

        // Seed Vehicles without photo_id
        val vehicleIds = List(20) { index ->
            Vehicles.insert {
                it[rented] = Random.nextBoolean()
                it[userId] = userIds.random()
                it[brand] = listOf("Toyota", "Honda", "Ford", "BMW", "Mercedes").random()
                it[model] = "Model${index + 1}"
                it[buildyear] = Random.nextInt(2000, 2024)
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
    println("Database seeded successfully")
}

fun hashPassword(password: String): Pair<String, String> {
    val salt = SecureRandom.getInstanceStrong().generateSeed(16)
    val saltedPassword = salt + password.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val hash = md.digest(saltedPassword)
    val saltBase64 = Base64.getEncoder().encodeToString(salt)
    val hashBase64 = Base64.getEncoder().encodeToString(hash)
    return Pair(saltBase64, hashBase64)
}

fun verifyPassword(password: String, storedHash: String): Boolean {
    val (saltBase64, hashBase64) = storedHash.split(":")
    val salt = Base64.getDecoder().decode(saltBase64)
    val saltedPassword = salt + password.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val computedHash = md.digest(saltedPassword)
    val computedHashBase64 = Base64.getEncoder().encodeToString(computedHash)
    return computedHashBase64 == hashBase64
}

// Table definitions
object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 255)
    val password = varchar("password", 255)
    val email = varchar("email", 255)

    override val primaryKey = PrimaryKey(id)
}

object Vehicles : Table("vehicles") {
    val id = integer("id").autoIncrement()
    val rented = bool("rented")
    val userId = integer("user_id").references(Users.id)
    val brand = varchar("brand", 255)
    val model = varchar("model", 255)
    val buildyear = integer("buildyear")
    val kenteken = varchar("kenteken", 255)
    val brandstof = varchar("brandstof", 255)
    val verbruik = integer("verbruik")
    val kmStand = integer("km_stand")
    val photoId = integer("photo_id").references(Photos.id).nullable() // Make this nullable
    val location = varchar("location", 255)

    override val primaryKey = PrimaryKey(id)
}

object Photos : Table("photos") {
    val id = integer("id").autoIncrement()
    val photoUrl = varchar("photo_url", 255)
    val carId = integer("car_id").references(Vehicles.id)

    override val primaryKey = PrimaryKey(id)
}


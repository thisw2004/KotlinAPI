package com.example

import com.example.plugins.configureSwagger
import io.ktor.http.*
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
            try {
                seedDatabase()
                call.respondText("Database seeded successfully", status = HttpStatusCode.OK)
            } catch (e: Exception) {
                application.log.error("Error seeding database", e)
                call.respondText(
                    "Error seeding database: ${e.message}",
                    status = HttpStatusCode.InternalServerError
                )
            }
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
    val names = listOf("Emma", "Liam", "Olivia", "Noah", "Ava", "Ethan", "Sophia", "Mason", "Isabella", "William",
        "Mia", "James", "Charlotte", "Benjamin", "Amelia", "Lucas", "Harper", "Henry", "Evelyn", "Alexander")
    val surnames = listOf("Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez",
        "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin")
    val domains = listOf("gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "icloud.com")

    transaction {
        // Seed Users
        val newUserIds = names.map { name ->
            val surname = surnames.random()
            var username: String
            var email: String

            // Ensure username and email are unique
            do {
                username = "${name.toLowerCase()}${Random.nextInt(100, 9999)}"
                email = "${name.toLowerCase()}.${surname.toLowerCase()}${Random.nextInt(100, 9999)}@${domains.random()}"
            } while (Users.select { (Users.username eq username) or (Users.email eq email) }.count() > 0)

            Users.insert {
                it[this.username] = username
                val (salt, hash) = hashPassword(UUID.randomUUID().toString()) // Use UUID for unique passwords
                it[password] = "$salt:$hash"
                it[this.email] = email
            } get Users.id
        }

        // Seed Vehicles
        val vehicleBrands = listOf("Toyota", "Honda", "Ford", "BMW", "Mercedes", "Audi", "Volkswagen", "Nissan", "Hyundai", "Kia")
        val vehicleModels = listOf("Sedan", "SUV", "Hatchback", "Coupe", "Convertible", "Pickup", "Van", "Wagon", "Crossover", "Sports Car")

        val newVehicleIds = List(30) {
            var kenteken: String
            do {
                kenteken = "${('A'..'Z').random()}${('A'..'Z').random()}${Random.nextInt(10, 99)}-${('A'..'Z').random()}${('A'..'Z').random()}${('A'..'Z').random()}"
            } while (Vehicles.select { Vehicles.kenteken eq kenteken }.count() > 0)

            Vehicles.insert {
                it[rented] = Random.nextBoolean()
                it[userId] = newUserIds.random()
                it[brand] = vehicleBrands.random()
                it[model] = "${vehicleModels.random()} ${Random.nextInt(1, 5)}"
                it[buildyear] = Random.nextInt(2010, 2024)
                it[this.kenteken] = kenteken
                it[brandstof] = listOf("Petrol", "Diesel", "Electric", "Hybrid").random()
                it[verbruik] = Random.nextInt(3, 20)
                it[kmStand] = Random.nextInt(0, 150000)
                it[location] = listOf("Amsterdam", "Rotterdam", "The Hague", "Utrecht", "Eindhoven", "Groningen", "Tilburg", "Almere", "Breda", "Nijmegen").random()
            } get Vehicles.id
        }

        // Seed Photos and update Vehicles
        newVehicleIds.forEach { vehicleId ->
            val photoId = Photos.insert {
                it[photoUrl] = "https://example.com/vehicle${vehicleId}_${Random.nextInt(1000, 9999)}.jpg"
                it[carId] = vehicleId
            } get Photos.id

            Vehicles.update({ Vehicles.id eq vehicleId }) {
                it[Vehicles.photoId] = photoId
            }
        }

        println("Added ${newUserIds.size} new users and ${newVehicleIds.size} new vehicles to the database.")
    }
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


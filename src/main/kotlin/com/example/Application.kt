package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.routes.loginRoute
import com.example.routes.registerRoute
import com.example.routes.vehicleRoutes
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
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.swagger.*
import org.jetbrains.exposed.sql.javatime.*
import java.time.LocalDateTime
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.security.SecurityScheme
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import java.util.Base64

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureAuthentication()
    configureOpenAPI()
    configureDatabases()
    configureRouting()
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
}

fun Application.configureAuthentication() {
    val secret = System.getenv("JWT_SECRET") ?: "your-secret-key" // Use environment variable in production
    val issuer = "http://0.0.0.0:8080/"
    val audience = "http://0.0.0.0:8080/hello"

    authentication {
        jwt {
            realm = "Ktor Server"
            verifier(
                JWT
                    .require(Algorithm.HMAC256(secret))
                    .withAudience(audience)
                    .withIssuer(issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(audience)) JWTPrincipal(credential.payload) else null
            }
        }
    }
}

fun Application.configureOpenAPI() {
    routing {
        openAPI(path = "openapi", swaggerFile = "openapi/documentation.yaml")
        swaggerUI(path = "swagger-ui", swaggerFile = "openapi/documentation.yaml")
    }
}

fun Application.configureRouting() {
    routing {
        // Always accessible routes
        get("/") {
            call.respondText("Default endpoint from MyRent API.")
        }
        get("/seed-database") {
            val seedUsers = call.request.queryParameters["users"]?.toBoolean() ?: true
            val seedVehicles = call.request.queryParameters["vehicles"]?.toBoolean() ?: true

            try {
                seedDatabase(seedUsers, seedVehicles)
                call.respondText("Database seeded successfully", status = HttpStatusCode.OK)
            } catch (e: Exception) {
                application.log.error("Error seeding database", e)
                call.respondText(
                    "Error seeding database: ${e.message}",
                    status = HttpStatusCode.InternalServerError
                )
            }
        }

        loginRoute() // Auth routes
        registerRoute()
        authenticate { // Routes accessible after register/login
            vehicleRoutes()
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
        SchemaUtils.createMissingTablesAndColumns(Users, Vehicles)
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

fun seedDatabase(seedUsers: Boolean = true, seedVehicles: Boolean = true) {
    if (seedUsers) {
        seedUsers()
    }
    if (seedVehicles) {
        seedVehicles()
    }
}

fun seedUsers() {
    val names = listOf("Emma", "Liam", "Olivia", "Noah", "Ava", "Ethan", "Sophia", "Mason", "Isabella", "William",
        "Mia", "James", "Charlotte", "Benjamin", "Amelia", "Lucas", "Harper", "Henry", "Evelyn", "Alexander")
    val surnames = listOf("Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez",
        "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin")
    val domains = listOf("gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "icloud.com")

    transaction {
        names.forEach { name ->
            val surname = surnames.random()
            var username: String
            var email: String

            // Ensure username and email are unique
            do {
                username = "${name.lowercase(Locale.getDefault())}${Random.nextInt(100, 9999)}"
                email = "${name.lowercase(Locale.getDefault())}.${surname.lowercase(Locale.getDefault())}${Random.nextInt(100, 9999)}@${domains.random()}"
            } while (Users.select { (Users.username eq username) or (Users.email eq email) }.count() > 0)

            Users.insert {
                it[this.username] = username
                val (salt, hash) = hashPassword(UUID.randomUUID().toString()) // Use UUID for unique passwords
                it[password] = "$salt:$hash"
                it[this.email] = email
                it[createdAt] = LocalDateTime.now()
            }
        }
        println("Users seeded successfully.")
    }
}

fun seedVehicles() {
    val vehicleBrands = listOf("Toyota", "Honda", "Ford", "BMW", "Mercedes", "Audi", "Volkswagen", "Nissan", "Hyundai", "Kia")
    val vehicleModels = listOf("Sedan", "SUV", "Hatchback", "Coupe", "Convertible", "Pickup", "Van", "Wagon", "Crossover", "Sports Car")

    transaction {
        repeat(30) {
            var kenteken: String
            do {
                kenteken = "${('A'..'Z').random()}${('A'..'Z').random()}${Random.nextInt(10, 99)}-${('A'..'Z').random()}${('A'..'Z').random()}${('A'..'Z').random()}"
            } while (Vehicles.select { Vehicles.kenteken eq kenteken }.count() > 0)

            val photoId: String? = generateBase64Image() // This returns a Base64 string or null

            Vehicles.insert {
                it[rented] = Random.nextBoolean()
                it[userId] = null
                it[brand] = vehicleBrands.random()
                it[model] = "${vehicleModels.random()} ${Random.nextInt(1, 5)}"
                it[buildYear] = Random.nextInt(2010, 2024)  // Correct field reference
                it[this.kenteken] = kenteken
                it[brandstof] = listOf("Petrol", "Diesel", "Electric", "Hybrid").random()
                it[verbruik] = Random.nextInt(3, 20)
                it[kmstand] = Random.nextInt(0, 150000)  // Correct field reference
                it[this.photoId] = photoId  // Ensure this is a nullable String?
                it[location] = listOf("Amsterdam", "Rotterdam", "The Hague", "Utrecht", "Eindhoven", "Groningen", "Tilburg", "Almere", "Breda", "Nijmegen").random()
                it[createdAt] = LocalDateTime.now()
            }
        }
        println("Vehicles seeded successfully.")
    }
}

fun generateBase64Image(): String? {
    val width = 150
    val height = 150
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

    // Generate a simple colored square image
    val graphics = image.createGraphics()
    graphics.color = Color(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
    graphics.fillRect(0, 0, width, height)
    graphics.dispose()

    val baos = ByteArrayOutputStream()
    return try {
        ImageIO.write(image, "png", baos)
        baos.flush()
        val imageBytes = baos.toByteArray()
        Base64.getEncoder().encodeToString(imageBytes)
    } catch (e: Exception) {
        null // If something goes wrong, return null
    } finally {
        baos.close()
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
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}

object Vehicles : Table("vehicles") {
    val id = integer("id").autoIncrement()
    val rented = bool("rented")
    val userId = integer("user_id").references(Users.id).nullable()  // Nullable user ID
    val brand = varchar("brand", 255)
    val model = varchar("model", 255)
    val buildYear = integer("buildyear")
    val kenteken = varchar("kenteken", 255)
    val brandstof = varchar("brandstof", 255)
    val verbruik = integer("verbruik")
    val kmstand = integer("km_stand")
    val photoId = varchar("photo_id", 100000).nullable()  // Stores base64-encoded image
    val location = varchar("location", 255)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}

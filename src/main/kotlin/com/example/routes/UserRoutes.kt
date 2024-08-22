package com.example.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.models.Users
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class RegistrationRequest(val username: String, val password: String, val email: String)

fun Route.userRoutes() {
    loginRoute()
    registerRoute()
}

fun Route.loginRoute() {
    post("/login") {
        try {
            val loginRequest = call.receive<LoginRequest>()
            println("Login attempt for username: ${loginRequest.username}") // Debug log

            val user = transaction {
                val result = Users.select { Users.username eq loginRequest.username }.singleOrNull()
                println("Query result: $result") // Debug log
                result
            }

            if (user != null) {
                println("User found: ${user[Users.username]}") // Debug log
                val storedPassword = user[Users.password]
                println("Stored password: $storedPassword") // Debug log
                if (verifyPassword(loginRequest.password, storedPassword)) {
                    val token = createJwtToken(user[Users.id])
                    call.respond(HttpStatusCode.OK, mapOf("token" to token))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
                }
            } else {
                println("User not found in database") // Debug log
                call.respond(HttpStatusCode.Unauthorized, "User not found")
            }
        } catch (e: Exception) {
            call.application.log.error("Error in login route", e)
            call.respond(HttpStatusCode.InternalServerError, "An error occurred during login: ${e.message}")
        }
    }
}

fun Route.registerRoute() {
    post("/register") {
        try {
            val registration = call.receive<RegistrationRequest>()

            // Check if user already exists
            val existingUser = transaction {
                Users.select { Users.username eq registration.username }.singleOrNull()
            }

            if (existingUser != null) {
                call.respond(HttpStatusCode.Conflict, "Username already exists")
                return@post
            }

            // Hash the password
            val (salt, hash) = hashPassword(registration.password)
            val hashedPassword = "${Base64.getEncoder().encodeToString(salt)}:${Base64.getEncoder().encodeToString(hash)}"


                    transaction {
                        Users.insert { stmt ->
                            stmt[Users.username] = registration.username
                            stmt[Users.password] = hashedPassword
                            stmt[Users.email] = registration.email
                            stmt[Users.createdAt] = LocalDateTime.now()
                        }
                    }

            call.respond(HttpStatusCode.Created, "User registered successfully")
        } catch (e: Exception) {
            call.application.log.error("Error in register route", e)
            call.respond(HttpStatusCode.InternalServerError, "An error occurred during registration: ${e.message}")
        }
    }
}



fun createJwtToken(userId: Int): String {
    val secret = System.getenv("JWT_SECRET") ?: "your-secret-key" // Use environment variable in production
    val issuer = "http://0.0.0.0:8080/"
    val audience = "http://0.0.0.0:8080/hello"
    val expirationTime = 3600000 * 24 // 24 hours

    return JWT.create()
        .withAudience(audience)
        .withIssuer(issuer)
        .withClaim("userId", userId)
        .withExpiresAt(Date(System.currentTimeMillis() + expirationTime))
        .sign(Algorithm.HMAC256(secret))
}

fun verifyPassword(inputPassword: String, storedPassword: String): Boolean {
    val parts = storedPassword.split(":")
    if (parts.size != 2) {
        println("Stored password is not in the correct format: $storedPassword")
        return false
    }
    val (saltBase64, hashBase64) = parts
    val salt = Base64.getDecoder().decode(saltBase64)
    val hash = Base64.getDecoder().decode(hashBase64)

    val pb = PBEKeySpec(inputPassword.toCharArray(), salt, 10000, 256)
    val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val computedHash = skf.generateSecret(pb).encoded

    return computedHash.contentEquals(hash)
}

fun hashPassword(password: String): Pair<ByteArray, ByteArray> {
    val random = SecureRandom()
    val salt = ByteArray(16)
    random.nextBytes(salt)

    val pb = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
    val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val hash = skf.generateSecret(pb).encoded

    return Pair(salt, hash)
}
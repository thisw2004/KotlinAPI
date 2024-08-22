package com.example.routes

import com.example.Vehicles.rented
import com.example.models.Vehicles
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class VehicleResponse(
    val id: Int,
    val rented: Boolean,
    val userId: Int?,
    val brand: String,
    val model: String,
    val buildYear: Int,
    val kenteken: String,
    val brandstof: String,
    val verbruik: Int,
    val kmstand: Int,
    val photoId: Int?,
    val location: String
)

@Serializable
data class AddCarRequest(
    val brand: String,
    val model: String,
    val buildYear: Int,
    val kenteken: String,
    val brandstof: String,
    val verbruik: Int,
    val kmstand: Int,
    val photoId: Int?,
    val location: String
)

@Serializable
data class HireCarRequest(
    val carId: Int
)


fun Route.vehicleRoutes() {
    getVehiclesRoute()
    getMyRentedVehiclesRoute()
    addCarRoute()
    hireCarRoute()


}

fun Route.getVehiclesRoute() {
    authenticate {
        get("/vehicles") {
            try {
                call.application.log.info("Entering /vehicles route")

                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.getClaim("userId", String::class)?.toIntOrNull()

                call.application.log.info("User ID from token: $userId")

                if (userId == null) {
                    call.application.log.warn("Invalid token: userId is null")
                    call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    return@get
                }

                call.application.log.info("Querying database for vehicles")
                val vehicles = transaction {
                    Vehicles.selectAll().map { row ->
                        VehicleResponse(
                            id = row[Vehicles.id],
                            rented = row[Vehicles.rented],
                            userId = row[Vehicles.userId],
                            brand = row[Vehicles.brand],
                            model = row[Vehicles.model],
                            buildYear = row[Vehicles.buildYear],
                            kenteken = row[Vehicles.kenteken],
                            brandstof = row[Vehicles.brandstof],
                            verbruik = row[Vehicles.verbruik],
                            kmstand = row[Vehicles.kmstand],
                            photoId = row[Vehicles.photoId],
                            location = row[Vehicles.location]
                        )
                    }
                }

                call.application.log.info("Retrieved ${vehicles.size} vehicles from database")

                call.respond(HttpStatusCode.OK, vehicles)
                call.application.log.info("Response sent successfully")
            } catch (e: Exception) {
                call.application.log.error("Error in get all vehicles route", e)
                call.respond(HttpStatusCode.InternalServerError, "An error occurred while fetching vehicles: ${e.message}")
            }
        }
    }
}



fun Route.getMyRentedVehiclesRoute() {
    authenticate {
        get("/vehicles/myrentedvehicles") {
            try {
                call.application.log.info("Entering /vehicles/rented route")

                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.getClaim("userId", String::class)?.toIntOrNull()

                call.application.log.info("User ID from token: $userId")

                if (userId == null) {
                    call.application.log.warn("Invalid token: userId is null")
                    call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    return@get
                }

                call.application.log.info("Querying database for rented vehicles for user $userId")
                val rentedVehicles = transaction {
                    Vehicles.select {
                        (Vehicles.userId eq userId) and (Vehicles.rented eq true)
                    }.map { row ->
                        VehicleResponse(
                            id = row[Vehicles.id],
                            rented = row[Vehicles.rented],
                            userId = row[Vehicles.userId],
                            brand = row[Vehicles.brand],
                            model = row[Vehicles.model],
                            buildYear = row[Vehicles.buildYear],
                            kenteken = row[Vehicles.kenteken],
                            brandstof = row[Vehicles.brandstof],
                            verbruik = row[Vehicles.verbruik],
                            kmstand = row[Vehicles.kmstand],
                            photoId = row[Vehicles.photoId],
                            location = row[Vehicles.location]
                        )
                    }
                }

                call.application.log.info("Retrieved ${rentedVehicles.size} rented vehicles for user $userId")

                call.respond(HttpStatusCode.OK, rentedVehicles)
                call.application.log.info("Response sent successfully")
            } catch (e: Exception) {
                call.application.log.error("Error in get rented vehicles route", e)
                call.respond(HttpStatusCode.InternalServerError, "An error occurred while fetching rented vehicles: ${e.message}")
            }
        }
    }
}

fun Route.addCarRoute() {
    authenticate {
        post("/vehicles/add") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.getClaim("userId", String::class)?.toIntOrNull()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    return@post
                }

                val request = call.receive<AddCarRequest>()

                val newCarId = transaction {
                    Vehicles.insert {
                        it[Vehicles.rented] = false
                        it[Vehicles.userId] = userId
                        it[Vehicles.brand] = request.brand
                        it[Vehicles.model] = request.model
                        it[Vehicles.buildYear] = request.buildYear
                        it[Vehicles.kenteken] = request.kenteken
                        it[Vehicles.brandstof] = request.brandstof
                        it[Vehicles.verbruik] = request.verbruik
                        it[Vehicles.kmstand] = request.kmstand
                        it[Vehicles.photoId] = request.photoId
                        it[Vehicles.location] = request.location
                    } get Vehicles.id
                }

                call.respond(HttpStatusCode.Created, mapOf("id" to newCarId))
            } catch (e: Exception) {
                call.application.log.error("Error in add car route", e)
                call.respond(HttpStatusCode.InternalServerError, "An error occurred while adding the car: ${e.message}")
            }
        }
    }
}

fun Route.hireCarRoute() {
    authenticate {
        post("/vehicles/hire") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.getClaim("userId", String::class)?.toIntOrNull()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    return@post
                }

                val request = call.receive<HireCarRequest>()

                val result = transaction {
                    val car = Vehicles.select { Vehicles.id eq request.carId }.singleOrNull()

                    if (car == null) {
                        return@transaction "Car not found"
                    }

                    if (car[Vehicles.rented]) {
                        return@transaction "Car is already rented"
                    }

                    Vehicles.update({ Vehicles.id eq request.carId }) {
                        it[rented] = true
                        it[Vehicles.userId] = userId
                    }

                    "Car hired successfully"
                }

                when (result) {
                    "Car hired successfully" -> call.respond(HttpStatusCode.OK, mapOf("message" to result))
                    "Car not found" -> call.respond(HttpStatusCode.NotFound, mapOf("error" to result))
                    "Car is already rented" -> call.respond(HttpStatusCode.Conflict, mapOf("error" to result))
                    else -> call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "An unexpected error occurred"))
                }

            } catch (e: Exception) {
                call.application.log.error("Error in hire car route", e)
                call.respond(HttpStatusCode.InternalServerError, "An error occurred while hiring the car: ${e.message}")
            }
        }
    }
}

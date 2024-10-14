package com.example.routes

import com.example.models.Vehicles
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import io.ktor.server.request.*
import org.jetbrains.exposed.sql.*

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
    val photoId: String?,
    val location: String  // Location as GPS coordinates
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
    val photoId: String?,  // Server expects this exact field name
    val location: String   // GPS coordinates
)

@Serializable
data class HireCarRequest(
    val carId: Int
)

@Serializable
data class CarIdRequest(val carId: Int)

fun Route.vehicleRoutes() {
    getVehiclesRoute()
    getMyRentedVehiclesRoute()
    addCarRoute()
    hireCarRoute()
    getAvailableVehiclesRoute()
    getVehicleByIdRoute()
}

fun Route.getVehiclesRoute() {
    authenticate {
        get("/vehicles") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.getClaim("userId", String::class)?.toIntOrNull()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    return@get
                }

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
                            location = row[Vehicles.location]  // Return location
                        )
                    }
                }

                call.respond(HttpStatusCode.OK, vehicles)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error fetching vehicles: ${e.message}")
            }
        }
    }
}

fun Route.getMyRentedVehiclesRoute() {
    authenticate {
        get("/vehicles/myrentedvehicles") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.getClaim("userId", String::class)?.toIntOrNull()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    return@get
                }

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
                            location = row[Vehicles.location]  // Return location
                        )
                    }
                }

                call.respond(HttpStatusCode.OK, rentedVehicles)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error fetching rented vehicles: ${e.message}")
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
                        it[rented] = false
                       //userid no need,bc is null by default.
                        it[brand] = request.brand
                        it[model] = request.model
                        it[buildYear] = request.buildYear
                        it[kenteken] = request.kenteken
                        it[brandstof] = request.brandstof
                        it[verbruik] = request.verbruik
                        it[kmstand] = request.kmstand
                        it[photoId] = request.photoId  // Insert the base64-encoded image
                        it[location] = request.location  // GPS coordinates
                    } get Vehicles.id
                }

                call.respond(HttpStatusCode.Created, mapOf("id" to newCarId))
            } catch (e: Exception) {
                call.application.log.error("Error in add car route", e)
                call.respond(HttpStatusCode.InternalServerError, "Error adding car: ${e.message}")
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
                    "Car is already hired" -> call.respond(HttpStatusCode.Conflict, mapOf("error" to result))
                    else -> call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Unexpected error occurred"))
                }

            } catch (e: Exception) {
                call.application.log.error("Error in hire car route", e)
                call.respond(HttpStatusCode.InternalServerError, "Error hiring car: ${e.message}")
            }
        }
    }
}

fun Route.getAvailableVehiclesRoute() {
    authenticate {
        get("/vehicles/available") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.getClaim("userId", String::class)?.toIntOrNull()

                if (userId == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    return@get
                }

                val availableVehicles = transaction {
                    Vehicles.select { Vehicles.rented eq false }.map { row ->
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
                            location = row[Vehicles.location]  // Return location
                        )
                    }
                }

                call.respond(HttpStatusCode.OK, availableVehicles)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error fetching available vehicles: ${e.message}")
            }
        }
    }
}

fun Route.getVehicleByIdRoute() {
    authenticate {
        post("/vehicles/getbyid") {
            try {
                val request = call.receive<CarIdRequest>()
                val carId = request.carId

                val vehicle = transaction {
                    Vehicles.select { Vehicles.id eq carId }
                        .mapNotNull { row ->
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
                                location = row[Vehicles.location]  // Return location
                            )
                        }
                        .singleOrNull()
                }

                if (vehicle == null) {
                    call.respond(HttpStatusCode.NotFound, "Car not found")
                } else {
                    call.respond(HttpStatusCode.OK, vehicle)
                }
            } catch (e: Exception) {
                call.application.log.error("Error in get vehicle by ID route", e)
                call.respond(HttpStatusCode.InternalServerError, "Error fetching vehicle: ${e.message}")
            }
        }
    }
}

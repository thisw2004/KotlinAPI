package com.example.routes

import com.example.models.Vehicles
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.selectAll
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

fun Route.vehicleRoutes() {
    getVehiclesRoute()
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
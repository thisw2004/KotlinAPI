package com.example

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
            // For example:
            // verifier(...)
            // validate { credential ->
            //     // Perform credential validation
            // }
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

// Assuming you have this function defined elsewhere
fun Application.configureDatabases() {
    // Database configuration code
}
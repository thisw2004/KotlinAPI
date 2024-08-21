package com.example.plugins

import io.ktor.server.routing.*
import io.ktor.server.application.*
import com.example.routes.*   //o.a userRoutes etc.


fun Application.configureRouting() {
    routing {
        loginRoute()
        registerRoute()
        //userRoutes()
//        vehicleRoutes()
//        routeRoutes()
//        photoRoutes()
//        drivingBehaviorRoutes()
    }
}
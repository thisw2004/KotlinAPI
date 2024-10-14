package com.example.models

import org.jetbrains.exposed.sql.*

object Vehicles : Table("vehicles") {  // Explicitly naming the table "vehicles"
    val id = integer("id").autoIncrement()
    val rented = bool("rented")
    val userId = integer("user_id").nullable()  // Nullable user ID
    val brand = varchar("brand", 255)
    val model = varchar("model", 255)
    val buildYear = integer("buildyear")
    val kenteken = varchar("kenteken", 255)
    val brandstof = varchar("brandstof", 255)
    val verbruik = integer("verbruik")
    val kmstand = integer("km_stand")
    val photoId = varchar("photo_id", 100000).nullable()  // Base64 encoded image string
    val location = varchar("location", 255)

    override val primaryKey = PrimaryKey(id)
}

// TODO: Make sure the vehicles in the DB are only linked to the user ID of the current logged-in user if the vehicle is rented.
// TODO: Otherwise, the userId in the vehicle should be empty or null.

package com.example.models

import org.jetbrains.exposed.sql.*

object Vehicles : Table() {
    val id = integer("id").autoIncrement()
    val rented = bool("rented")
    val userId = (integer("user_id").references(Users.id)).nullable()
    val brand = varchar("brand", 255)
    val model = varchar("model", 255)
    val buildYear = integer("buildyear")
    val kenteken = varchar("kenteken", 255)
    val brandstof = varchar("brandstof", 255)
    val verbruik = integer("verbruik")
    val kmstand = integer("km_stand")
    val photoId = (integer("photo_id").references(Photos.id)).nullable()
    val location = varchar("location", 255)


    override val primaryKey = PrimaryKey(id)
}
//TODO: make sure the vehicles in the db are only linked to the user id of the current logged in user if the vehicle is verhuurd
//TODO: otherwise the userid in the vehicle should be empty or null or something.
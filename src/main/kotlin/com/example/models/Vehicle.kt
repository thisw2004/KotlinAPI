package com.example.models

import org.jetbrains.exposed.sql.*

object Vehicles : Table() {
    val id = integer("id").autoIncrement()
    val rented = bool("rented")
    val userId = (integer("user_id").references(Users.id)).nullable()
    val brand = varchar("brand", 255)
    val model = varchar("model", 255)
    val buildYear = integer("buildyear")
    val licensePlate = varchar("license_plate", 255)
    val fuelType = varchar("fuel_type", 255)
    val consumption = integer("consumption")
    val mileage = integer("mileage")
    val photoId = (integer("photo_id").references(Photos.id)).nullable()
    val location = varchar("location", 255)
    val type = varchar("type", 50)

    override val primaryKey = PrimaryKey(id)
}
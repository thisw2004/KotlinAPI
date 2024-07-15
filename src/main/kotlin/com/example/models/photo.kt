package com.example.models

import org.jetbrains.exposed.sql.*

object Photos : Table() {
    val id = integer("id").autoIncrement()
    val photoUrl = varchar("photo_url", 255)
    val vehicleId = (integer("vehicle_id").references(Vehicles.id)).nullable()

    override val primaryKey = PrimaryKey(id)
}
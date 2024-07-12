package com.example.models

import org.jetbrains.exposed.sql.*

object Vehicles : Table() {
    val id = integer("id").autoIncrement()
    val ownerId = reference("owner_id", Users.id)
    val model = varchar("model", 50)
    val plateNumber = varchar("plate_number", 20).uniqueIndex()
    val isAvailable = bool("is_available")

    override val primaryKey = PrimaryKey(id)
}
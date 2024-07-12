package com.example.models

import org.jetbrains.exposed.sql.*

object Routes : Table() {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users.id)
    val startPoint = varchar("start_point", 100)
    val endPoint = varchar("end_point", 100)
    val distance = double("distance")

    override val primaryKey = PrimaryKey(id)
}
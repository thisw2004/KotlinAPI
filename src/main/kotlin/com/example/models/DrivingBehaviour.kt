package com.example.models

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object DrivingBehaviors : Table() {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users.id)
    val score = integer("score")
    val date = datetime("date")
    val reward = integer("reward")

    override val primaryKey = PrimaryKey(id)
}
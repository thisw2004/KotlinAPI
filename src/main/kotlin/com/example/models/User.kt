package com.example.models

import org.jetbrains.exposed.sql.*

object Users : Table() {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 255)
    val password = varchar("password", 255)
    val email = varchar("email", 255)

    override val primaryKey = PrimaryKey(id)
}
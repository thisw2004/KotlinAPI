package com.example.models

import org.jetbrains.exposed.sql.*

object Users : Table() {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 50).uniqueIndex()
    val password = varchar("password", 100)
    val email = varchar("email", 100).uniqueIndex()

    override val primaryKey = PrimaryKey(id)
}
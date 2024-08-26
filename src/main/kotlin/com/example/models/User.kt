package com.example.models

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object Users : Table() {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 50).uniqueIndex()
    val password = varchar("password", 255)
    val email = varchar("email", 100).uniqueIndex()
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}
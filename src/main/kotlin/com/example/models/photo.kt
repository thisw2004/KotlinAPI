package com.example.models

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime

object Photos : Table() {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users.id)
    val url = varchar("url", 255)
    val uploadDate = datetime("upload_date")

    override val primaryKey = PrimaryKey(id)
}
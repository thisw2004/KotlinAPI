package com.example.services

import com.example.models.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class UserService {
    fun createUser(username: String, password: String, email: String): Int = transaction {
        Users.insert {
            it[Users.username] = username
            it[Users.password] = password // Remember to hash the password
            it[Users.email] = email
        } get Users.id
    }

    // Implement other user-related operations
}
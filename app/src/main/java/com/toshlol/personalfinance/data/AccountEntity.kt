package com.toshlol.personalfinance.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val createdAt: Long,
    val updatedAt: Long,
)

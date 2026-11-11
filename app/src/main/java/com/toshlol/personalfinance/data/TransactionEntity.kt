package com.toshlol.personalfinance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("accountId"), Index("date")],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val amountMinor: Long,
    val direction: String,
    val date: Long,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class TransactionDirection { INCOME, EXPENSE }

data class FinanceTransaction(
    val id: String,
    val accountId: String,
    val accountName: String,
    val amountMinor: Long,
    val direction: TransactionDirection,
    val date: Long,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class TransactionDraft(
    val id: String? = null,
    val accountId: String,
    val amountMinor: Long,
    val direction: TransactionDirection,
    val date: Long,
    val note: String?,
)

package com.toshlol.personalfinance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories", indices = [Index("direction"), Index("archived")])
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val direction: String,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "tags",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("categoryId"), Index("archived")],
)
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val categoryId: String?,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "transaction_tags",
    primaryKeys = ["transactionId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("tagId")],
)
data class TransactionTagCrossRef(
    val transactionId: String,
    val tagId: String,
)

data class FinanceCategory(
    val id: String,
    val name: String,
    val direction: TransactionDirection,
    val archived: Boolean,
)

data class FinanceTag(
    val id: String,
    val name: String,
    val categoryId: String?,
    val categoryName: String?,
    val archived: Boolean,
)

data class CategoryTagSnapshot(
    val categories: List<FinanceCategory>,
    val tags: List<FinanceTag>,
)

data class CategoryDraft(
    val id: String? = null,
    val name: String,
    val direction: TransactionDirection,
)

data class TagDraft(
    val id: String? = null,
    val name: String,
    val categoryId: String? = null,
)

data class CategoryTagDraft(
    val id: String? = null,
    val name: String,
    val direction: TransactionDirection? = null,
    val categoryId: String? = null,
)

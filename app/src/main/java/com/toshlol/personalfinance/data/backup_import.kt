@file:Suppress("ktlint:standard:filename", "ktlint:standard:no-unit-return")

package com.toshlol.personalfinance.data

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharsetDecoder
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

enum class BackupImportMode { MERGE, OVERWRITE }

enum class BackupEntityType { ACCOUNT, CATEGORY, TAG, TRANSACTION, TRANSFER, BUDGET, LEGACY_BUDGET }

interface BackupEntityRecord {
    val id: String

    val updatedAt: Long
}

data class BackupEntityCounts(
    val inserted: Int = 0,
    val updated: Int = 0,
    val unchanged: Int = 0,
)

data class BackupImportReport(
    val counts: Map<BackupEntityType, BackupEntityCounts>,
)

data class BackupImportFailure(
    val code: String,
    val message: String,
)

sealed interface BackupImportOutcome {
    data class Success(
        val report: BackupImportReport,
    ) : BackupImportOutcome

    data class Failure(
        val failures: List<BackupImportFailure>,
    ) : BackupImportOutcome
}

fun interface BackupDocumentReader {
    suspend fun read(source: Uri): ByteArray
}

class ContentResolverBackupReader(
    private val contentResolver: ContentResolver,
) : BackupDocumentReader {
    override suspend fun read(source: Uri): ByteArray =
        withContext(Dispatchers.IO) {
            val input: InputStream = requireNotNull(contentResolver.openInputStream(source)) { "The selected Backup could not be opened." }
            input.use(::readLimited)
        }
}

private fun readLimited(stream: InputStream): ByteArray {
    val output: ByteArrayOutputStream = ByteArrayOutputStream()
    val buffer: ByteArray = ByteArray(8_192)
    var totalBytes: Int = 0
    while (totalBytes < MAX_BACKUP_BYTES + 1) {
        val bytesToRead: Int = minOf(buffer.size, MAX_BACKUP_BYTES + 1 - totalBytes)
        val bytesRead: Int = stream.read(buffer, 0, bytesToRead)
        if (bytesRead <= 0) break
        output.write(buffer, 0, bytesRead)
        totalBytes += bytesRead
    }
    val bytes: ByteArray = output.toByteArray()
    require(bytes.size <= MAX_BACKUP_BYTES) { "Backup exceeds the maximum file size." }
    return bytes
}

class RoomBackupImporter(
    private val database: AppDatabase,
    private val beforeTransaction: suspend () -> Unit = {},
    private val beforeCommit: suspend () -> Unit = {},
) {
    private val backupStore: RoomBackupStore = RoomBackupStore(database)
    private val reconciler: BackupReconciler = BackupReconciler()

    suspend fun import(
        contents: ByteArray,
        mode: BackupImportMode = BackupImportMode.MERGE,
    ): BackupImportOutcome {
        if (contents.size > MAX_BACKUP_BYTES) return createFailure("file_too_large", "Backup exceeds the maximum file size.")
        val backup: FinanceBackup =
            decodeBackup(contents) ?: return createFailure("invalid_backup", "Backup is malformed, unsupported, or invalid.")
        return executeImport(backup, mode)
    }

    private fun decodeBackup(contents: ByteArray): FinanceBackup? =
        try {
            val decoder: CharsetDecoder =
                StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
            FinanceBackupCodec.decode(decoder.decode(ByteBuffer.wrap(contents)).toString()).also(BackupValidator::validate)
        } catch (_: Exception) {
            null
        }

    private suspend fun executeImport(
        backup: FinanceBackup,
        mode: BackupImportMode,
    ): BackupImportOutcome =
        try {
            currentCoroutineContext().ensureActive()
            beforeTransaction()
            val report: BackupImportReport = executeTransaction(backup, mode)
            BackupImportOutcome.Success(report)
        } catch (exception: BackupImportRejected) {
            createFailure(exception.code, exception.message ?: "Backup could not be imported.")
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            createFailure("storage_failure", "Backup could not be imported. Local data was not changed.")
        }

    private suspend fun executeTransaction(
        backup: FinanceBackup,
        mode: BackupImportMode,
    ): BackupImportReport =
        withContext(NonCancellable) {
            database.withTransaction {
                val current: FinanceBackup? = backupStore.load()
                validateCurrency(current, backup, mode)
                val merged: FinanceBackup = reconcile(current, backup, mode)
                BackupValidator.validate(merged)
                backupStore.save(merged, mode)
                beforeCommit()
                reconciler.createReport(current, backup, mode)
            }
        }

    private fun validateCurrency(
        current: FinanceBackup?,
        imported: FinanceBackup,
        mode: BackupImportMode,
    ): Unit {
        if (mode == BackupImportMode.MERGE && current != null && current.settings.currency != imported.settings.currency) {
            throw BackupImportRejected("currency_mismatch", "Backup currency does not match the local ledger.")
        }
    }

    private fun reconcile(
        current: FinanceBackup?,
        imported: FinanceBackup,
        mode: BackupImportMode,
    ): FinanceBackup = if (mode == BackupImportMode.OVERWRITE || current == null) imported else reconciler.merge(current, imported)

    private fun createFailure(
        code: String,
        message: String,
    ): BackupImportOutcome.Failure = BackupImportOutcome.Failure(listOf(BackupImportFailure(code, message)))
}

internal class BackupImportRejected(
    val code: String,
    override val message: String,
) : IllegalArgumentException(message)

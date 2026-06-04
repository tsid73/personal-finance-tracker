package com.example.finance.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.finance.data.repository.BackupSnapshot
import com.example.finance.data.repository.LocalBackupDocument
import com.google.gson.GsonBuilder
import java.io.File

object BackupManager {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun exportTransactionsToCsv(context: Context, rows: List<Map<String, String>>): File {
        val headers = listOf("Date", "Title", "Kind", "Amount", "Category", "Account", "Merchant", "Notes")
        return exportCsv(context, "transactions", headers, rows)
    }

    fun exportCsv(
        context: Context,
        prefix: String,
        headers: List<String>,
        rows: List<Map<String, String>>
    ): File {
        val file = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.csv")
        val lines = buildList {
            add(headers.joinToString(","))
            rows.forEach { row ->
                add(
                    headers.joinToString(",") { header ->
                        "\"${(row[header] ?: "").replace("\"", "\"\"")}\""
                    }
                )
            }
        }
        file.writeText(lines.joinToString("\n"))
        return file
    }

    fun exportSnapshotToJson(context: Context, snapshot: BackupSnapshot): File {
        val file = File(context.cacheDir, "finance_backup_${System.currentTimeMillis()}.json")
        file.writeText(gson.toJson(snapshot))
        return file
    }

    fun writeBackupDocument(context: Context, uri: Uri, document: LocalBackupDocument) {
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
            writer.write(gson.toJson(document))
        } ?: error("Unable to open backup destination.")
    }

    fun readBackupDocument(context: Context, uri: Uri): LocalBackupDocument {
        val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Unable to read backup file.")
        return gson.fromJson(json, LocalBackupDocument::class.java)
            ?: error("Backup file is empty or invalid.")
    }

    fun createShareIntent(context: Context, file: File, mimeType: String, title: String): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

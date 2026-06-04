package com.example.finance.ui.screens

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.finance.data.repository.FinanceRepository
import com.example.finance.data.repository.LocalBackupDocument
import com.example.finance.data.repository.PreferenceManager
import com.example.finance.data.repository.RemoteSyncManager
import com.example.finance.data.repository.RestoreValidationResult
import com.example.finance.data.repository.SyncResult
import com.example.finance.util.BackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

enum class SettingsOperation {
    BACKUP,
    RESTORE,
    SYNC
}

data class RestorePreview(
    val document: LocalBackupDocument,
    val validation: RestoreValidationResult
)

class SettingsViewModel(
    private val repository: FinanceRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {
    private val remoteSyncManager = RemoteSyncManager()
    private val operationMutex = Mutex()

    private val _syncEnabled = MutableStateFlow(false)
    val syncEnabled: StateFlow<Boolean> = _syncEnabled.asStateFlow()

    private val _syncBaseUrl = MutableStateFlow("")
    val syncBaseUrl: StateFlow<String> = _syncBaseUrl.asStateFlow()

    private val _lastSyncResult = MutableStateFlow<SyncResult?>(null)
    val lastSyncResult: StateFlow<SyncResult?> = _lastSyncResult.asStateFlow()

    private val _activeOperation = MutableStateFlow<SettingsOperation?>(null)
    val activeOperation: StateFlow<SettingsOperation?> = _activeOperation.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDemoUserAndSeeds()
        }
        viewModelScope.launch {
            preferenceManager.syncEnabled.collectLatest { _syncEnabled.value = it }
        }
        viewModelScope.launch {
            preferenceManager.syncBaseUrl.collectLatest { _syncBaseUrl.value = it }
        }
    }

    fun clearStatus() {
        _statusMessage.value = null
    }

    fun showStatus(message: String) {
        _statusMessage.value = message
    }

    fun updateSyncSettings(enabled: Boolean, baseUrl: String) {
        viewModelScope.launch {
            preferenceManager.setSyncEnabled(enabled)
            preferenceManager.setSyncBaseUrl(baseUrl)
            _statusMessage.value = "Sync settings saved."
        }
    }

    suspend fun prepareRestore(context: Context, uri: Uri): RestorePreview {
        val document = BackupManager.readBackupDocument(context, uri)
        val validation = com.example.finance.data.repository.BackupValidator.validate(document)
        return RestorePreview(document, validation)
    }

    fun backupToUri(context: Context, uri: Uri) {
        runExclusive(SettingsOperation.BACKUP) {
            repository.ensureDemoUserAndSeeds()
            val settings = preferenceManager.getSnapshot()
            val document = repository.getLocalBackupDocument(settings)
            BackupManager.writeBackupDocument(context, uri, document)
            _statusMessage.value = "Backup completed."
        }
    }

    fun restoreDocument(document: LocalBackupDocument) {
        runExclusive(SettingsOperation.RESTORE) {
            val validation = repository.restoreLocalBackup(document)
            preferenceManager.restoreSnapshot(document.appSettings)
            _statusMessage.value = "Restore completed. ${validation.transactionCount} transactions restored."
        }
    }

    fun syncNow() {
        runExclusive(SettingsOperation.SYNC) {
            val snapshot = repository.getBackupSnapshot()
            _lastSyncResult.value = remoteSyncManager.sync(_syncBaseUrl.value, snapshot)
            _statusMessage.value = if (_lastSyncResult.value?.healthy == true) {
                "Sync completed."
            } else {
                _lastSyncResult.value?.errors?.firstOrNull() ?: "Sync failed."
            }
        }
    }

    private fun runExclusive(operation: SettingsOperation, block: suspend () -> Unit) {
        viewModelScope.launch {
            if (!operationMutex.tryLock()) {
                _statusMessage.value = "Another settings operation is already running."
                return@launch
            }

            _activeOperation.value = operation
            try {
                block()
            } catch (error: Exception) {
                _statusMessage.value = error.localizedMessage ?: "Operation failed."
            } finally {
                _activeOperation.value = null
                operationMutex.unlock()
            }
        }
    }
}

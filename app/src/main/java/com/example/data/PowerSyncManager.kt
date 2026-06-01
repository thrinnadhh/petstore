package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PowerSyncManager {
    sealed class SyncState {
        object Connected : SyncState()
        object Syncing : SyncState()
        object Paused : SyncState()
        object Offline : SyncState()
    }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Connected)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    fun triggerSync() {
        _syncState.value = SyncState.Syncing
        // Simulate local SQLite (Room) syncing dirty rows with Supabase Postgres
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            _syncState.value = SyncState.Connected
            println("PowerSync: Offline-first synchronization successfully reconciled.")
        }, 1200)
    }

    fun setOffline() {
        _syncState.value = SyncState.Offline
    }

    fun setConnected() {
        _syncState.value = SyncState.Connected
    }
}

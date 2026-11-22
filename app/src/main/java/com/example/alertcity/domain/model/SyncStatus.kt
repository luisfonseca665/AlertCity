package com.example.alertcity.domain.model

enum class SyncStatus(val etiqueta: String) {
    SYNC_PENDING("Pendiente de sincronizar"),
    SYNCED("Sincronizado"),
    SYNC_ERROR("Error de sincronización")
}

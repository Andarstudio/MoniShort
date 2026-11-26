package app.monishort.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class ChargingSettings(
    val enabled: Boolean = false,
    val connectSoundUri: String? = null,
    val connectSoundName: String? = null,
    val disconnectSoundEnabled: Boolean = false,
    val disconnectSoundUri: String? = null,
    val disconnectSoundName: String? = null,
    val notifyOnConnect: Boolean = false,
    val notifyOnDisconnect: Boolean = false,
    val volumePercent: Int = 100,
)

private val Context.moniStore: DataStore<Preferences> by preferencesDataStore(name = "monishort_settings")

private object K {
    val ENGINE = booleanPreferencesKey("engine_enabled")
    val C_ENABLED = booleanPreferencesKey("charging_enabled")
    val C_URI = stringPreferencesKey("charging_connect_uri")
    val C_NAME = stringPreferencesKey("charging_connect_name")
    val D_ENABLED = booleanPreferencesKey("charging_disconnect_enabled")
    val D_URI = stringPreferencesKey("charging_disconnect_uri")
    val D_NAME = stringPreferencesKey("charging_disconnect_name")
    val N_CONNECT = booleanPreferencesKey("charging_notify_connect")
    val N_DISCONNECT = booleanPreferencesKey("charging_notify_disconnect")
    val VOLUME = intPreferencesKey("charging_volume")
}

private fun Preferences.toCharging() = ChargingSettings(
    enabled = this[K.C_ENABLED] ?: false,
    connectSoundUri = this[K.C_URI],
    connectSoundName = this[K.C_NAME],
    disconnectSoundEnabled = this[K.D_ENABLED] ?: false,
    disconnectSoundUri = this[K.D_URI],
    disconnectSoundName = this[K.D_NAME],
    notifyOnConnect = this[K.N_CONNECT] ?: false,
    notifyOnDisconnect = this[K.N_DISCONNECT] ?: false,
    volumePercent = this[K.VOLUME] ?: 100,
)

private fun androidx.datastore.preferences.core.MutablePreferences.writeCharging(s: ChargingSettings) {
    this[K.C_ENABLED] = s.enabled
    if (s.connectSoundUri != null) this[K.C_URI] = s.connectSoundUri else remove(K.C_URI)
    if (s.connectSoundName != null) this[K.C_NAME] = s.connectSoundName else remove(K.C_NAME)
    this[K.D_ENABLED] = s.disconnectSoundEnabled
    if (s.disconnectSoundUri != null) this[K.D_URI] = s.disconnectSoundUri else remove(K.D_URI)
    if (s.disconnectSoundName != null) this[K.D_NAME] = s.disconnectSoundName else remove(K.D_NAME)
    this[K.N_CONNECT] = s.notifyOnConnect
    this[K.N_DISCONNECT] = s.notifyOnDisconnect
    this[K.VOLUME] = s.volumePercent.coerceIn(0, 100)
}

class SettingsRepository(private val context: Context) {
    val engineEnabled: Flow<Boolean> = context.moniStore.data.map { it[K.ENGINE] ?: true }
    val charging: Flow<ChargingSettings> = context.moniStore.data.map { it.toCharging() }

    suspend fun setEngineEnabled(value: Boolean) {
        context.moniStore.edit { it[K.ENGINE] = value }
    }

    suspend fun currentCharging(): ChargingSettings = charging.first()

    suspend fun updateCharging(transform: (ChargingSettings) -> ChargingSettings) {
        context.moniStore.edit { p -> p.writeCharging(transform(p.toCharging())) }
    }
}

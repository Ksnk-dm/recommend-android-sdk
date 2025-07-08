package com.recommend.sdk.core.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.recommend.sdk.core.data.model.CurrentState
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

/**
 * Current state repository
 *
 * @constructor
 *
 * @param context
 */
class CurrentStateRepository(private val context: Context) {
    companion object {
        const val CURRENT_STATE_PREFERENCES_NAME = "RECOMMEND_CURRENT_STATE"
        const val DEVICE_OLD_SDK_PREFERENCES_KEY = "PREF_UNIQUE_ID"
        private const val SALT = "salt"

        val Context.recommendCurrentStateDataStore: DataStore<Preferences> by preferencesDataStore(
            name = CURRENT_STATE_PREFERENCES_NAME,
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() }
            )
        )
    }

    private val dataStore: DataStore<Preferences> = context.recommendCurrentStateDataStore

    private val deviceIdKey = stringPreferencesKey("device_id")
    private val isFirstLaunchKey = booleanPreferencesKey("is_first_launch")
    private val isSubscribedToPushKey = booleanPreferencesKey("is_subscribed_to_push")
    private val pushToken = stringPreferencesKey("push_token")
    private val lastSentIsSubscribedToPushStatusKey = booleanPreferencesKey("last_sent_is_subscribed_to_push_status")
    private val subscriptionStatusChangeDateKey = intPreferencesKey("subscription_status_change_date")
    private val firstSubscribedDateKey = intPreferencesKey("first_subscribed_date")

    suspend fun getCurrentState(): CurrentState {
        val preferences = dataStore.data.first()

        val currentState = if (preferences[deviceIdKey] == null) {
            // Support for old SDK
            val deviceIdFromOldSDK = getDeviceIdFromOldSDK()
            val newCurrentState = CurrentState(
                deviceIdFromOldSDK ?: getSecretKey(context)
            )
            saveCurrentState(newCurrentState)
            newCurrentState
        } else {
            CurrentState(
                preferences[deviceIdKey]!!,
                preferences[isFirstLaunchKey] ?: true,
                preferences[isSubscribedToPushKey],
                preferences[pushToken],
                preferences[lastSentIsSubscribedToPushStatusKey],
                preferences[subscriptionStatusChangeDateKey],
                preferences[firstSubscribedDateKey]
            )
        }

        return currentState
    }

    suspend fun saveCurrentState(currentState: CurrentState) {
        dataStore.edit { state ->
            state[deviceIdKey] = currentState.deviceId
            state[isFirstLaunchKey] = currentState.isFirstLaunch
            currentState.isSubscribedToPush?.let { state[isSubscribedToPushKey] = it }
            currentState.pushToken?.let { state[pushToken] = it }
            currentState.lastSentManuallySubscribedToPushStatus?.let {
                state[lastSentIsSubscribedToPushStatusKey] = it
            }
            currentState.subscriptionStatusChangeDate?.let {
                state[subscriptionStatusChangeDateKey] = it
            }
            currentState.firstSubscribedDate?.let {
                state[firstSubscribedDateKey] = it
            }
        }
    }

    private fun getDeviceIdFromOldSDK(): String? {
        val sharedPrefs: SharedPreferences = context.getSharedPreferences(
            DEVICE_OLD_SDK_PREFERENCES_KEY,
            Context.MODE_PRIVATE
        )
        return sharedPrefs.getString(DEVICE_OLD_SDK_PREFERENCES_KEY, null)
    }

    @SuppressLint("HardwareIds")
    private fun getSecretKey(context: Context): String {
        val short = "35" +
                Build.BOARD.length % 10 +
                Build.BRAND.length % 10 +
                Build.CPU_ABI.length % 10 +
                Build.DEVICE.length % 10 +
                Build.DISPLAY.length % 10 +
                Build.HOST.length % 10 +
                Build.ID.length % 10 +
                Build.MANUFACTURER.length % 10 +
                Build.MODEL.length % 10 +
                Build.PRODUCT.length % 10 +
                Build.TAGS.length % 10 +
                Build.TYPE.length % 10 +
                Build.USER.length % 10

        val advId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val macAddress = wm.connectionInfo.macAddress

        var longID = short + advId + macAddress
        longID += SALT + longID.length * SALT.length

        val m: MessageDigest = try {
            MessageDigest.getInstance("MD5")
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            return UUID.randomUUID().toString()
        }

        m.update(longID.toByteArray(), 0, longID.length)
        val digest = m.digest()
        return buildString {
            for (i in digest.indices) {
                val b = 0xFF and digest[i].toInt()
                if (b <= 0xF) append("0")
                append(b.toString(16))
            }
        }.uppercase(Locale.getDefault())
    }
}

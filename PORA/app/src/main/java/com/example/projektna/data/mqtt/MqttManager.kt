package com.example.projektna.data.mqtt

import android.content.Context
import android.util.Log
import com.example.projektna.data.PreferencesManager
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID

/**
 * Singleton za upravljanje MQTT povezave.
 * Uporablja Eclipse Paho MQTT klient za objavljanje podatkov na broker.
 */
object MqttManager {

    private const val TAG = "MqttManager"

    // Default MQTT nastavitve
    const val DEFAULT_BROKER_URL = "tcp://10.0.2.2:1883"  // Za Android emulator
    const val DEFAULT_CLIENT_ID_PREFIX = "projektna_"

    // MQTT Topics
    object Topics {
        const val GPS = "sensors/simulation/gps"
        const val ACCELEROMETER = "sensors/simulation/accelerometer"
    }

    // QoS levels
    const val QOS_AT_MOST_ONCE = 0   // Fire and forget
    const val QOS_AT_LEAST_ONCE = 1  // Guaranteed delivery
    const val QOS_EXACTLY_ONCE = 2   // Exactly once

    private var mqttClient: MqttAsyncClient? = null
    private var isConnecting = false

    // Callback za obveščanje o statusu povezave
    var connectionCallback: MqttConnectionCallback? = null

    interface MqttConnectionCallback {
        fun onConnected()
        fun onDisconnected(cause: Throwable?)
        fun onConnectionFailed(error: String)
        fun onMessagePublished(topic: String)
        fun onPublishFailed(topic: String, error: String)
    }

    /**
     * Pridobi URL MQTT brokerja iz nastavitev
     */
    fun getBrokerUrl(context: Context): String {
        val prefs = PreferencesManager(context)
        return prefs.mqttBrokerUrl.ifBlank { DEFAULT_BROKER_URL }
    }

    /**
     * Vzpostavi povezavo z MQTT brokerjem
     */
    @Synchronized
    fun connect(context: Context, callback: MqttConnectionCallback? = null) {
        if (isConnecting) {
            Log.d(TAG, "Že se povezujem...")
            return
        }

        connectionCallback = callback
        val brokerUrl = getBrokerUrl(context)
        val prefs = PreferencesManager(context)

        // Ustvari unikatni client ID
        val clientId = DEFAULT_CLIENT_ID_PREFIX + UUID.randomUUID().toString().take(8)

        Log.d(TAG, "Povezujem na MQTT broker: $brokerUrl")

        try {
            isConnecting = true

            // Ustvari novega MQTT klienta
            mqttClient = MqttAsyncClient(brokerUrl, clientId, MemoryPersistence())

            mqttClient?.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    Log.d(TAG, "Povezava vzpostavljena: $serverURI (reconnect: $reconnect)")
                    isConnecting = false
                    connectionCallback?.onConnected()
                }

                override fun connectionLost(cause: Throwable?) {
                    Log.w(TAG, "Povezava izgubljena: ${cause?.message}")
                    isConnecting = false
                    connectionCallback?.onDisconnected(cause)
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    // Trenutno ne naročamo na teme, samo objavljamo
                    Log.d(TAG, "Sporočilo prejeto: $topic")
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d(TAG, "Sporočilo dostavljeno")
                }
            })

            // Nastavitve povezave
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 10  // 10 sekund timeout
                keepAliveInterval = 60  // 60 sekund keep alive
                isAutomaticReconnect = true

                // Če so nastavljene poverilnice
                val username = prefs.mqttUsername
                val password = prefs.mqttPassword
                if (username.isNotBlank()) {
                    userName = username
                }
                if (password.isNotBlank()) {
                    this.password = password.toCharArray()
                }
            }

            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Povezava uspešna")
                    isConnecting = false
                    connectionCallback?.onConnected()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    val errorMsg = exception?.message ?: "Neznana napaka"
                    Log.e(TAG, "Povezava neuspešna: $errorMsg", exception)
                    isConnecting = false
                    connectionCallback?.onConnectionFailed(errorMsg)
                }
            })

        } catch (e: MqttException) {
            Log.e(TAG, "MQTT napaka: ${e.message}", e)
            isConnecting = false
            connectionCallback?.onConnectionFailed(e.message ?: "MQTT Exception")
        }
    }

    /**
     * Prekini povezavo z MQTT brokerjem
     */
    @Synchronized
    fun disconnect() {
        try {
            mqttClient?.let { client ->
                if (client.isConnected) {
                    Log.d(TAG, "Prekinjam MQTT povezavo...")
                    client.disconnect(null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.d(TAG, "Povezava prekinjena")
                            connectionCallback?.onDisconnected(null)
                        }

                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            Log.w(TAG, "Napaka pri prekinitvi: ${exception?.message}")
                        }
                    })
                }
                client.close()
            }
        } catch (e: MqttException) {
            Log.e(TAG, "Napaka pri prekinitvi: ${e.message}", e)
        } finally {
            mqttClient = null
        }
    }

    /**
     * Preveri ali je povezava vzpostavljena
     */
    fun isConnected(): Boolean {
        return mqttClient?.isConnected == true
    }

    /**
     * Objavi sporočilo na MQTT topic
     */
    fun publish(
        topic: String,
        payload: String,
        qos: Int = QOS_AT_LEAST_ONCE,
        retained: Boolean = false
    ): Boolean {
        val client = mqttClient

        if (client == null || !client.isConnected) {
            Log.w(TAG, "MQTT ni povezan, sporočilo ni poslano")
            connectionCallback?.onPublishFailed(topic, "MQTT ni povezan")
            return false
        }

        return try {
            val message = MqttMessage(payload.toByteArray(Charsets.UTF_8)).apply {
                this.qos = qos
                this.isRetained = retained
            }

            client.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Objavljeno na $topic: ${payload.take(100)}...")
                    connectionCallback?.onMessagePublished(topic)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Napaka pri objavi na $topic: ${exception?.message}")
                    connectionCallback?.onPublishFailed(topic, exception?.message ?: "Neznana napaka")
                }
            })

            true
        } catch (e: MqttException) {
            Log.e(TAG, "MQTT publish napaka: ${e.message}", e)
            connectionCallback?.onPublishFailed(topic, e.message ?: "MQTT Exception")
            false
        }
    }

    /**
     * Objavi GPS podatke na MQTT
     */
    fun publishGps(
        latitude: Double,
        longitude: Double,
        timestamp: Long,
        isSimulated: Boolean = true
    ): Boolean {
        val payload = """
            {
                "latitude": $latitude,
                "longitude": $longitude,
                "timestamp": $timestamp,
                "isSimulated": $isSimulated
            }
        """.trimIndent()

        return publish(Topics.GPS, payload)
    }

    /**
     * Objavi Accelerometer podatke na MQTT
     */
    fun publishAccelerometer(
        x: Float,
        y: Float,
        z: Float,
        magnitude: Float,
        latitude: Double,
        longitude: Double,
        timestamp: Long,
        isSimulated: Boolean = true
    ): Boolean {
        val payload = """
            {
                "x": $x,
                "y": $y,
                "z": $z,
                "magnitude": $magnitude,
                "latitude": $latitude,
                "longitude": $longitude,
                "timestamp": $timestamp,
                "isSimulated": $isSimulated
            }
        """.trimIndent()

        return publish(Topics.ACCELEROMETER, payload)
    }
}

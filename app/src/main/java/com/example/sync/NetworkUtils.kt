package com.example.sync

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {

    fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: ""
                        if (host.isNotEmpty()) {
                            return host
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return "127.0.0.1"
    }

    fun getDeviceId(context: Context): String {
        return try {
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            if (!androidId.isNullOrEmpty()) {
                androidId.take(8)
            } else {
                Build.MODEL.replace(" ", "_").lowercase().take(8)
            }
        } catch (_: Exception) {
            "dev_" + (Build.BOARD.hashCode() % 10000)
        }
    }

    fun getDeviceModelName(): String {
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        if (model.contains("sdk_gphone", ignoreCase = true) ||
            model.contains("emulator", ignoreCase = true) ||
            model.contains("generic", ignoreCase = true) ||
            Build.FINGERPRINT.contains("generic", ignoreCase = true)
        ) {
            return "Pixel 8"
        }
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }
    }

    /**
     * Obtains or generates a stable hardware MAC address used strictly internally
     * to persistently track peer devices across dynamic Wi-Fi IP changes.
     */
    fun getDeviceMacAddress(context: Context): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (nif in interfaces) {
                if (nif.name.equals("wlan0", ignoreCase = true) || nif.name.equals("eth0", ignoreCase = true)) {
                    val macBytes = nif.hardwareAddress
                    if (macBytes != null && macBytes.isNotEmpty()) {
                        val sb = StringBuilder()
                        for (b in macBytes) {
                            sb.append(String.format("%02X:", b))
                        }
                        if (sb.isNotEmpty()) {
                            sb.deleteCharAt(sb.length - 1)
                        }
                        val mac = sb.toString()
                        if (mac != "02:00:00:00:00:00") {
                            return mac
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback: Generate consistent, stable MAC address format based on hardware ANDROID_ID
        val rawId = getDeviceId(context).padStart(12, 'a')
        val clean = rawId.filter { it.isLetterOrDigit() }.padEnd(12, '0').take(12).uppercase()
        return clean.chunked(2).joinToString(":")
    }

    /**
     * Generates a deterministic internal MAC address format for a given identifier
     */
    fun generateDeterministicMac(seed: String): String {
        val hash = kotlin.math.abs(seed.hashCode().toLong())
        val hex = hash.toString(16).padStart(12, 'a').takeLast(12).uppercase()
        return hex.chunked(2).joinToString(":")
    }
}

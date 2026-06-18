package com.streamhek.tv

import java.net.NetworkInterface

fun getLocalIpAddress(): String {
    try {
        NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { iface ->
            if (iface.isLoopback || !iface.isUp) return@forEach
            iface.inetAddresses.toList().forEach { addr ->
                val host = addr.hostAddress ?: return@forEach
                if (!host.contains(":") && !host.startsWith("127.") && !host.startsWith("169.254.")) return host
            }
        }
    } catch (e: Exception) {
        android.util.Log.w("StreamHek", "getLocalIp failed", e)
    }
    return "127.0.0.1"
}

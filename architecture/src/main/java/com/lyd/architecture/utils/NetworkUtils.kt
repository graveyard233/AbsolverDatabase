package com.lyd.architecture.utils

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.format.Formatter
import android.util.Log
import androidx.annotation.RequiresPermission
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.UnknownHostException
import java.util.*

/**
 * <pre>
 * author:
 * blog  :
 * time  :
 * desc  : utils derry
</pre> *
 */
class NetworkUtils private constructor() {
    enum class NetworkType {
        NETWORK_ETHERNET, NETWORK_WIFI, NETWORK_4G, NETWORK_3G, NETWORK_2G, NETWORK_UNKNOWN, NETWORK_NO
    }

    interface Callback {
        fun call(isSuccess: Boolean)
    }

    companion object {
        /**
         * Open the settings of wireless.
         */
        fun openWirelessSettings() {
            Utils.getApp().startActivity(
                Intent(Settings.ACTION_WIRELESS_SETTINGS)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

        /**
         * Return whether network is connected.
         *
         * Must hold `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />`
         *
         * @return `true`: connected<br></br>`false`: disconnected
         */
        @get:RequiresPermission(permission.ACCESS_NETWORK_STATE)
        val isConnected: Boolean
            get() {
                val info = activeNetworkInfo
                return info != null && info.isConnected
            }

        /**
         * Return whether network is available using ping.
         *
         * Must hold `<uses-permission android:name="android.permission.INTERNET" />`
         *
         * The default ping ip: 223.5.5.5
         *
         * @return `true`: yes<br></br>`false`: no
         */
        @get:RequiresPermission(permission.INTERNET)
        val isAvailableByPing: Boolean
            get() = isAvailableByPing(null)

        /**
         * Return whether network is available using ping.
         *
         * Must hold `<uses-permission android:name="android.permission.INTERNET" />`
         *
         * @param ip The ip address.
         * @return `true`: yes<br></br>`false`: no
         */
        @RequiresPermission(permission.INTERNET)
        fun isAvailableByPing(ip: String?): Boolean {
            var ip = ip
            if (ip == null || ip.length <= 0) {
                // default ping ip
                ip = "223.5.5.5"
            }
            val result = ShellUtils.execCmd(String.format("ping -c 1 %s", ip), false)
            val ret = result.result == 0
            if (result.errorMsg != null) {
                Log.d("NetworkUtils", "isAvailableByPing() called" + result.errorMsg)
            }
            if (result.successMsg != null) {
                Log.d("NetworkUtils", "isAvailableByPing() called" + result.successMsg)
            }
            return ret
        }

        @RequiresPermission(permission.INTERNET)
        fun isAvailableByDns(ip: String?) {
        }

        /**
         * Return whether mobile data is enabled.
         *
         * @return `true`: enabled<br></br>`false`: disabled
         */
        val mobileDataEnabled: Boolean
            get() {
                try {
                    val tm = Utils.getApp()
                        .getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                        ?: return false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        return tm.isDataEnabled
                    }
                    @SuppressLint("PrivateApi") val getMobileDataEnabledMethod =
                        tm.javaClass.getDeclaredMethod("getDataEnabled")
                    if (null != getMobileDataEnabledMethod) {
                        return getMobileDataEnabledMethod.invoke(tm) as Boolean
                    }
                } catch (e: Exception) {
                    Log.e("NetworkUtils", "getMobileDataEnabled: ", e)
                }
                return false
            }

        /**
         * Enable or disable mobile data.
         *
         * Must hold `android:sharedUserId="android.uid.system"`,
         * `<uses-permission android:name="android.permission.MODIFY_PHONE_STATE" />`
         *
         * @param enabled True to enabled, false otherwise.
         * @return `true`: success<br></br>`false`: fail
         */
        @RequiresPermission(permission.MODIFY_PHONE_STATE)
        fun setMobileDataEnabled(enabled: Boolean): Boolean {
            try {
                val tm =
                    Utils.getApp().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                        ?: return false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    tm.isDataEnabled = enabled
                    return false
                }
                val setDataEnabledMethod = tm.javaClass.getDeclaredMethod(
                    "setDataEnabled",
                    Boolean::class.javaPrimitiveType
                )
                if (null != setDataEnabledMethod) {
                    setDataEnabledMethod.invoke(tm, enabled)
                    return true
                }
            } catch (e: Exception) {
                Log.e("NetworkUtils", "setMobileDataEnabled: ", e)
            }
            return false
        }

        /**
         * Return whether using mobile data.
         *
         * Must hold `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />`
         *
         * @return `true`: yes<br></br>`false`: no
         */
        @get:RequiresPermission(permission.ACCESS_NETWORK_STATE)
        val isMobileData: Boolean
            get() {
                val info = activeNetworkInfo
                return (null != info && info.isAvailable
                        && info.type == ConnectivityManager.TYPE_MOBILE)
            }

        /**
         * Return whether using 4G.
         *
         * Must hold `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />`
         *
         * @return `true`: yes<br></br>`false`: no
         */
        @RequiresPermission(permission.ACCESS_NETWORK_STATE)
        fun is4G(): Boolean {
            val info = activeNetworkInfo
            return (info != null && info.isAvailable
                    && info.subtype == TelephonyManager.NETWORK_TYPE_LTE)
        }
        /**
         * Return whether wifi is enabled.
         *
         * Must hold `<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />`
         *
         * @return `true`: enabled<br></br>`false`: disabled
         */
        /**
         * Enable or disable wifi.
         *
         * Must hold `<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />`
         *
         * @param enabled True to enabled, false otherwise.
         */
        @get:RequiresPermission(permission.ACCESS_WIFI_STATE)
        @set:RequiresPermission(permission.CHANGE_WIFI_STATE)
        var wifiEnabled: Boolean
            get() {
                @SuppressLint("WifiManagerLeak") val manager =
                    Utils.getApp().getSystemService(Context.WIFI_SERVICE) as WifiManager
                        ?: return false
                return manager.isWifiEnabled
            }
            set(enabled) {
                @SuppressLint("WifiManagerLeak") val manager =
                    Utils.getApp().getSystemService(Context.WIFI_SERVICE) as WifiManager
                        ?: return
                if (enabled == manager.isWifiEnabled) {
                    return
                }
                manager.isWifiEnabled = enabled
            }

        /**
         * Return whether wifi is connected.
         *
         * Must hold `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />`
         *
         * @return `true`: connected<br></br>`false`: disconnected
         */
        @get:RequiresPermission(permission.ACCESS_NETWORK_STATE)
        val isWifiConnected: Boolean
            get() {
                val cm = Utils.getApp()
                    .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    ?: return false
                val ni = cm.activeNetworkInfo
                return ni != null && ni.type == ConnectivityManager.TYPE_WIFI
            }

        /**
         * Return whether wifi is available.
         *
         * Must hold `<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />`,
         * `<uses-permission android:name="android.permission.INTERNET" />`
         *
         * @return `true`: available<br></br>`false`: unavailable
         */
        @get:RequiresPermission(allOf = [permission.ACCESS_WIFI_STATE, permission.INTERNET])
        val isWifiAvailable: Boolean
            get() = wifiEnabled && isAvailableByPing

        /**
         * Return the name of network operate.
         *
         * @return the name of network operate
         */
        val networkOperatorName: String
            get() {
                val tm =
                    Utils.getApp().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                        ?: return ""
                return tm.networkOperatorName
            }

        /**
         * Return type of network.
         *
         * Must hold `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />`
         *
         * @return type of network
         *
         *  * [NetworkType.NETWORK_ETHERNET]
         *  * [NetworkType.NETWORK_WIFI]
         *  * [NetworkType.NETWORK_4G]
         *  * [NetworkType.NETWORK_3G]
         *  * [NetworkType.NETWORK_2G]
         *  * [NetworkType.NETWORK_UNKNOWN]
         *  * [NetworkType.NETWORK_NO]
         *
         */
        @get:RequiresPermission(permission.ACCESS_NETWORK_STATE)
        val networkType: NetworkType
            get() {
                if (isEthernet) {
                    return NetworkType.NETWORK_ETHERNET
                }
                val info = activeNetworkInfo
                if (info != null && info.isAvailable) {
                    if (info.type == ConnectivityManager.TYPE_WIFI) {
                        return NetworkType.NETWORK_WIFI
                    } else if (info.type == ConnectivityManager.TYPE_MOBILE) {
                        when (info.subtype) {
                            TelephonyManager.NETWORK_TYPE_GSM, TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_IDEN -> return NetworkType.NETWORK_2G
                            TelephonyManager.NETWORK_TYPE_TD_SCDMA, TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPAP -> return NetworkType.NETWORK_3G
                            TelephonyManager.NETWORK_TYPE_IWLAN, TelephonyManager.NETWORK_TYPE_LTE -> return NetworkType.NETWORK_4G
                            else -> {
                                val subtypeName = info.subtypeName
                                if ("TD-SCDMA".equals(subtypeName, ignoreCase = true)
                                    || "WCDMA".equals(subtypeName, ignoreCase = true)
                                    || "CDMA2000".equals(subtypeName, ignoreCase = true)
                                ) {
                                    return NetworkType.NETWORK_3G
                                }
                            }
                        }
                    }
                }
                return NetworkType.NETWORK_UNKNOWN
            }

        /**
         * Return whether using ethernet.
         *
         * Must hold
         * `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />`
         *
         * @return `true`: yes<br></br>`false`: no
         */
        @get:RequiresPermission(permission.ACCESS_NETWORK_STATE)
        private val isEthernet: Boolean
            private get() {
                val cm = Utils.getApp()
                    .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    ?: return false
                val info = cm.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET) ?: return false
                val state = info.state ?: return false
                return state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.CONNECTING
            }

        @get:RequiresPermission(permission.ACCESS_NETWORK_STATE)
        private val activeNetworkInfo: NetworkInfo?
            private get() {
                val cm = Utils.getApp()
                    .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    ?: return null
                return cm.activeNetworkInfo
            }

        /**
         * Return the ip address.
         *
         * Must hold `<uses-permission android:name="android.permission.INTERNET" />`
         *
         * @param useIPv4 True to use ipv4, false otherwise.
         * @return the ip address
         */
        @RequiresPermission(permission.INTERNET)
        fun getIPAddress(useIPv4: Boolean): String {
            try {
                val nis = NetworkInterface.getNetworkInterfaces()
                val adds = LinkedList<InetAddress>()
                while (nis.hasMoreElements()) {
                    val ni = nis.nextElement()
                    // To prevent phone of xiaomi return "10.0.2.15"
                    if (!ni.isUp || ni.isLoopback) {
                        continue
                    }
                    val addresses = ni.inetAddresses
                    while (addresses.hasMoreElements()) {
                        adds.addFirst(addresses.nextElement())
                    }
                }
                for (add in adds) {
                    if (!add.isLoopbackAddress) {
                        val hostAddress = add.hostAddress
                        val isIPv4 = hostAddress.indexOf(':') < 0
                        if (useIPv4) {
                            if (isIPv4) {
                                return hostAddress
                            }
                        } else {
                            if (!isIPv4) {
                                val index = hostAddress.indexOf('%')
                                return if (index < 0) hostAddress.toUpperCase() else hostAddress.substring(
                                    0,
                                    index
                                ).toUpperCase()
                            }
                        }
                    }
                }
            } catch (e: SocketException) {
                e.printStackTrace()
            }
            return ""
        }

        /**
         * Return the ip address of broadcast.
         *
         * @return the ip address of broadcast
         */
        val broadcastIpAddress: String
            get() {
                try {
                    val nis = NetworkInterface.getNetworkInterfaces()
                    val adds = LinkedList<InetAddress>()
                    while (nis.hasMoreElements()) {
                        val ni = nis.nextElement()
                        if (!ni.isUp || ni.isLoopback) {
                            continue
                        }
                        val ias = ni.interfaceAddresses
                        var i = 0
                        val size = ias.size
                        while (i < size) {
                            val ia = ias[i]
                            val broadcast = ia.broadcast
                            if (broadcast != null) {
                                return broadcast.hostAddress
                            }
                            i++
                        }
                    }
                } catch (e: SocketException) {
                    e.printStackTrace()
                }
                return ""
            }

        /**
         * Return the domain address.
         *
         * Must hold `<uses-permission android:name="android.permission.INTERNET" />`
         *
         * @param domain The name of domain.
         * @return the domain address
         */
        @RequiresPermission(permission.INTERNET)
        fun getDomainAddress(domain: String?): String {
            val inetAddress: InetAddress
            return try {
                inetAddress = InetAddress.getByName(domain)
                inetAddress.hostAddress
            } catch (e: UnknownHostException) {
                e.printStackTrace()
                ""
            }
        }

        /**
         * Return the ip address by wifi.
         *
         * @return the ip address by wifi
         */
        @get:RequiresPermission(permission.ACCESS_WIFI_STATE)
        val ipAddressByWifi: String
            get() {
                @SuppressLint("WifiManagerLeak") val wm = Utils.getApp()
                    .getSystemService(Context.WIFI_SERVICE) as WifiManager
                    ?: return ""
                return Formatter.formatIpAddress(wm.dhcpInfo.ipAddress)
            }

        /**
         * Return the gate way by wifi.
         *
         * @return the gate way by wifi
         */
        @get:RequiresPermission(permission.ACCESS_WIFI_STATE)
        val gatewayByWifi: String
            get() {
                @SuppressLint("WifiManagerLeak") val wm = Utils.getApp()
                    .getSystemService(Context.WIFI_SERVICE) as WifiManager
                    ?: return ""
                return Formatter.formatIpAddress(wm.dhcpInfo.gateway)
            }

        /**
         * Return the net mask by wifi.
         *
         * @return the net mask by wifi
         */
        @get:RequiresPermission(permission.ACCESS_WIFI_STATE)
        val netMaskByWifi: String
            get() {
                @SuppressLint("WifiManagerLeak") val wm = Utils.getApp()
                    .getSystemService(Context.WIFI_SERVICE) as WifiManager
                    ?: return ""
                return Formatter.formatIpAddress(wm.dhcpInfo.netmask)
            }

        /**
         * Return the server address by wifi.
         *
         * @return the server address by wifi
         */
        @get:RequiresPermission(permission.ACCESS_WIFI_STATE)
        val serverAddressByWifi: String
            get() {
                @SuppressLint("WifiManagerLeak") val wm = Utils.getApp()
                    .getSystemService(Context.WIFI_SERVICE) as WifiManager
                    ?: return ""
                return Formatter.formatIpAddress(wm.dhcpInfo.serverAddress)
            }
    }

    init {
        throw UnsupportedOperationException("u can't instantiate me...")
    }
}
package cn.sharinghub.headscale.tailscale_root.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.DhcpInfo
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import cn.sharinghub.headscale.tailscale_root.core.RootShell
import cn.sharinghub.headscale.tailscale_root.util.LogCollector

class WifiChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WifiManager.NETWORK_STATE_CHANGED_ACTION) return

        val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
        if (networkInfo != null && networkInfo.isConnected) {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo: WifiInfo = wifiManager.connectionInfo
            val dhcpInfo: DhcpInfo = wifiManager.dhcpInfo

            val localIp = formatIp(wifiInfo.ipAddress)
            val gatewayIp = formatIp(dhcpInfo.gateway)

            LogCollector.log("WLAN 已连接, ip=$localIp, gw=$gatewayIp")

            // 延迟 1 秒执行默认路由检查与添加逻辑
            Handler(Looper.getMainLooper()).postDelayed({
                val expectedRoute = "default via $gatewayIp dev wlan0"
                val routeCheck = RootShell.exec("ip route show")

                if (!routeCheck.output.contains(expectedRoute)) {
                    LogCollector.log("未发现默认路由，尝试添加: $expectedRoute")
                    val addResult = RootShell.exec("ip route add default via $gatewayIp dev wlan0")
                    if (addResult.success) {
                        LogCollector.log("默认路由添加成功")
                    } else {
                        LogCollector.log("添加默认路由失败: ${addResult.output}")
                    }
                } else {
                    LogCollector.log("默认路由已存在: $expectedRoute")
                }
            }, 1000)

        } else {
            LogCollector.log("WLAN 已断开")
        }
    }

    private fun formatIp(ip: Int): String {
        return "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    }
}

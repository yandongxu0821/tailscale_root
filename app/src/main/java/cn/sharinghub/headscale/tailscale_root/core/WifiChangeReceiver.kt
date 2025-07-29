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
                val expectedGateway = gatewayIp
                val expectedInterface = "wlan0"

                val routeCheck = RootShell.exec("ip route show")
                val currentDefaultRoutes = routeCheck.output.lines()
                    .filter { it.startsWith("default") }

                val shouldReplace = currentDefaultRoutes.any { route ->
                    !route.contains("via $expectedGateway") || !route.contains("dev $expectedInterface")
                }

                if (shouldReplace || currentDefaultRoutes.isEmpty()) {
                    LogCollector.log("默认路由不匹配或不存在，尝试替换为: default via $expectedGateway dev $expectedInterface")

                    val commands = mutableListOf<String>()
                    currentDefaultRoutes.forEach { _ ->
                        commands.add("ip route delete default")
                    }
                    commands.add("ip route add default via $expectedGateway dev $expectedInterface")

                    val result = RootShell.exec(commands)
                    if (result.success) {
                        LogCollector.log("默认路由替换成功")
                    } else {
                        LogCollector.log("默认路由替换失败: ${result.output}")
                    }
                } else {
                    LogCollector.log("默认路由已正确设置: default via $expectedGateway dev $expectedInterface")
                }
            }, 1000)


        } else {
            LogCollector.log("WLAN 已断开")
            Handler(Looper.getMainLooper()).post {
                val routeCheck = RootShell.exec("ip route show")
                if (routeCheck.output.contains("default via")) {
                    LogCollector.log("检测到默认路由，尝试删除")
                    val delResult = RootShell.exec("ip route delete default")
                    if (delResult.success) {
                        LogCollector.log("默认路由删除成功")
                    } else {
                        LogCollector.log("默认路由删除失败: ${delResult.output}")
                    }
                }
            }
        }
    }

    private fun formatIp(ip: Int): String {
        return "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    }
}

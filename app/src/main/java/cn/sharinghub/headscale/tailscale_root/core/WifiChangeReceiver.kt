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

    companion object {
        private var lastDetailedState: NetworkInfo.DetailedState? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WifiManager.NETWORK_STATE_CHANGED_ACTION) return

        val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO) ?: return
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo

        val state = networkInfo.state
        val detailed = networkInfo.detailedState
        val ssid = wifiInfo.ssid ?: "UK"
        val extra = networkInfo.extraInfo ?: "无"

        if (detailed != lastDetailedState) {
            lastDetailedState = detailed
            LogCollector.log("WLAN 状态变化: \nstate=$state, \ndetailed=$detailed, \nssid=$ssid, \nextra=$extra")
        }

        if (state == NetworkInfo.State.CONNECTED) {
            Handler(Looper.getMainLooper()).postDelayed({

                val dhcpInfo: DhcpInfo = wifiManager.dhcpInfo
                val localIp = formatIp(wifiInfo.ipAddress)
                val gatewayIp = formatIp(dhcpInfo.gateway)
                LogCollector.log("WLAN 已连接, \nIPv4=$localIp, GWv4=$gatewayIp")
                val expectedInterface = "wlan0"

                // ---------- IPv4 路由检测 ----------
                val routeCheckV4 = RootShell.exec("ip route show")
                val currentDefaultRoutesV4 = routeCheckV4.output.lines().filter { it.startsWith("default") }

                val shouldReplaceV4 = currentDefaultRoutesV4.any { route ->
                    !route.contains("via $gatewayIp") || !route.contains("dev $expectedInterface")
                }

                if (shouldReplaceV4 || currentDefaultRoutesV4.isEmpty()) {
                    LogCollector.log("IPv4 默认路由不匹配或不存在，替换为: default via $gatewayIp dev $expectedInterface")
                    val commands = mutableListOf<String>()
                    currentDefaultRoutesV4.forEach { _ -> commands.add("ip route delete default") }
                    commands.add("ip route add default via $gatewayIp dev $expectedInterface")
                    val result = RootShell.exec(commands)
                    if (result.success)
                        LogCollector.log("IPv4 默认路由替换成功")
                    else
                        LogCollector.log("IPv4 默认路由替换失败: ${result.output}")
                } else {
                    LogCollector.log("IPv4 默认路由已正确设置: default via $gatewayIp dev $expectedInterface")
                }

                // ---------- IPv6 路由检测 ----------
                val v6AddrCheck = RootShell.exec("ip -6 addr show dev $expectedInterface")
                val hasV6 = v6AddrCheck.output.contains("inet6 ")
                if (hasV6) {
                    val routeCheckV6 = RootShell.exec("ip -6 route show")
                    val currentDefaultRoutesV6 = routeCheckV6.output.lines().filter { it.startsWith("default") }

                    // 从邻居表提取 router 地址作为网关
                    var gwV6: String? = null
                    val neighCheck = RootShell.exec("ip -6 neigh show dev $expectedInterface")
                    gwV6 = neighCheck.output.lines().firstNotNullOfOrNull { line ->
                        if (line.contains("router")) {
                            line.substringBefore(" ").trim()
                        } else null
                    }

                    // 若无法识别，则放弃 IPv6 处理
                    if (gwV6.isNullOrEmpty()) {
                        LogCollector.log("⚠ 未能自动识别 IPv6 网关地址，将跳过 IPv6 路由配置")
                    } else {
                        val validGwV6 = gwV6
                        val shouldReplaceV6 = currentDefaultRoutesV6.any { route ->
                            !route.contains("via $validGwV6") || !route.contains("dev $expectedInterface")
                        }

                        if (shouldReplaceV6 || currentDefaultRoutesV6.isEmpty()) {
                            LogCollector.log("IPv6 默认路由将被替换: default via $validGwV6 dev $expectedInterface")
                            val commands = mutableListOf<String>()
                            currentDefaultRoutesV6.forEach { _ -> commands.add("ip -6 route delete default") }
                            commands.add("ip -6 route add default via $validGwV6 dev $expectedInterface")
                            val result6 = RootShell.exec(commands)
                            if (result6.success)
                                LogCollector.log("IPv6 默认路由替换成功")
                            else
                                LogCollector.log("IPv6 默认路由替换失败: ${result6.output}")
                        } else {
                            LogCollector.log("IPv6 默认路由已正确设置: default via $validGwV6 dev $expectedInterface")
                        }
                    }
                } else {
                    LogCollector.log("未检测到 IPv6 地址，跳过 IPv6 路由检查")
                }
            }, 1000)

        } else if (state == NetworkInfo.State.DISCONNECTED) {
            LogCollector.log("WLAN 已断开 (ssid=$ssid)")
            Handler(Looper.getMainLooper()).post {
                val routeCheckV4 = RootShell.exec("ip route show")
                if (routeCheckV4.output.contains("default via")) {
                    LogCollector.log("检测到 IPv4 默认路由，尝试删除")
                    val delResult = RootShell.exec("ip route delete default")
                    if (delResult.success)
                        LogCollector.log("IPv4 默认路由删除成功")
                    else
                        LogCollector.log("IPv4 默认路由删除失败: ${delResult.output}")
                }

                val routeCheckV6 = RootShell.exec("ip -6 route show")
                if (routeCheckV6.output.contains("default via")) {
                    LogCollector.log("检测到 IPv6 默认路由，尝试删除")
                    val delResult6 = RootShell.exec("ip -6 route delete default")
                    if (delResult6.success)
                        LogCollector.log("IPv6 默认路由删除成功")
                    else
                        LogCollector.log("IPv6 默认路由删除失败: ${delResult6.output}")
                }
            }
        }
    }

    private fun formatIp(ip: Int): String {
        return "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    }
}

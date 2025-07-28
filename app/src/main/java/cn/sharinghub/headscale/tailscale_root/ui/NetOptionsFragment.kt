package cn.sharinghub.headscale.tailscale_root.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import cn.sharinghub.headscale.tailscale_root.R
import cn.sharinghub.headscale.tailscale_root.core.RootShell
import cn.sharinghub.headscale.tailscale_root.util.LogCollector

class NetOptionsFragment : Fragment() {

    private lateinit var editGatewayIp:   EditText
    private lateinit var btnApplyGateway: Button
    private lateinit var checkboxIpv4:    CheckBox
    private lateinit var checkboxIpv6:    CheckBox
    private lateinit var btnAddDns:       Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_net_options, container, false)

        editGatewayIp   = view.findViewById(R.id.edit_login_server)
        btnApplyGateway = view.findViewById(R.id.gateway_confirm)
        checkboxIpv4    = view.findViewById(R.id.ipv4_forwarding_checkbox)
        checkboxIpv6    = view.findViewById(R.id.ipv6_forwarding_checkbox)
        btnAddDns       = view.findViewById(R.id.add_dns_nameserver)

        setupListeners()
        return view
    }

    private fun setupListeners() {
        btnApplyGateway.setOnClickListener {
            val ip = editGatewayIp.text.toString().trim()
            val ipv4Regex = Regex("^((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)$")
            if (ip.isNotEmpty() && ipv4Regex.matches(ip)) {
                val cmd = "ip route add default via $ip dev wlan0"
                RootShell.exec(cmd)
                LogCollector.log("默认网关设置成 $ip")
            } else {
                LogCollector.log("输入的网关地址无效")
                return@setOnClickListener
            }
        }

        checkboxIpv4.setOnCheckedChangeListener { _, isChecked ->
            val value = if (isChecked) "1" else "0"
            RootShell.exec("sysctl -w net.ipv4.ip_forward=$value")
            LogCollector.log("IPv4 转发 ${if (isChecked) "开启" else "关闭"}")
        }

        checkboxIpv6.setOnCheckedChangeListener { _, isChecked ->
            val value = if (isChecked) "1" else "0"
            RootShell.exec("sysctl -w net.ipv6.conf.all.forwarding=$value")
            LogCollector.log("IPv6 转发 ${if (isChecked) "开启" else "关闭"}")
        }

        btnAddDns.setOnClickListener {
            RootShell.exec("echo 'nameserver 223.5.5.5' >> /etc/resolv.conf")
            LogCollector.log("DNS 名称服务器 223.5.5.5 已添加")
        }
    }
}

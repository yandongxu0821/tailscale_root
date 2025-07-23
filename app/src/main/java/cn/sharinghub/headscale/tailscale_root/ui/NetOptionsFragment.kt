package cn.sharinghub.headscale.tailscale_root.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import cn.sharinghub.headscale.tailscale_root.R
import cn.sharinghub.headscale.tailscale_root.util.LogCollector
import cn.sharinghub.headscale.tailscale_root.core.RootShell

class NetOptionsFragment : Fragment() {

    private lateinit var editGatewayIp:   EditText
    private lateinit var btnApplyGateway: Button
    private lateinit var checkboxIpv4:    CheckBox
    private lateinit var checkboxIpv6:    CheckBox
    private lateinit var btnAddDns:       Button
//    private lateinit var btnBack:         Button

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
//        btnBack         = view.findViewById(R.id.btn_back_control)

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
                LogCollector.log("Default gateway set to $ip")
            } else {
                LogCollector.log("Please enter a valid IP")
                return@setOnClickListener
            }
        }

        checkboxIpv4.setOnCheckedChangeListener { _, isChecked ->
            val value = if (isChecked) "1" else "0"
            RootShell.exec("sysctl -w net.ipv4.ip_forward=$value")
            LogCollector.log("IPv4 forwarding ${if (isChecked) "enabled" else "disabled"}")
        }

        checkboxIpv6.setOnCheckedChangeListener { _, isChecked ->
            val value = if (isChecked) "1" else "0"
            RootShell.exec("sysctl -w net.ipv6.conf.all.forwarding=$value")
            LogCollector.log("IPv6 forwarding ${if (isChecked) "enabled" else "disabled"}")
        }

        btnAddDns.setOnClickListener {
            val cmd = """echo "nameserver 223.5.5.5" >> /etc/resolv.conf"""
            RootShell.exec(cmd)
            LogCollector.log("DNS added: 223.5.5.5")
        }

//        btnBack.setOnClickListener {
//            requireActivity().onBackPressedDispatcher.onBackPressed()
//        }
    }
}

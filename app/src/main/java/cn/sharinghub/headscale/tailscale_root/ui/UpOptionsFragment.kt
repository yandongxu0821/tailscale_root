package cn.sharinghub.headscale.tailscale_root.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import cn.sharinghub.headscale.tailscale_root.R
import cn.sharinghub.headscale.tailscale_root.core.BinaryInstaller
import cn.sharinghub.headscale.tailscale_root.util.LogCollector
import cn.sharinghub.headscale.tailscale_root.core.RootShell
import cn.sharinghub.headscale.tailscale_root.service.TailscaleService

class UpOptionsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_up_options, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<Button>(R.id.btn_execute_up)?.setOnClickListener {
            val result = tailscaleUp(view)
            LogCollector.log("tailscale up 输出：\n${result.output}")
            Toast.makeText(requireContext(), "tailscale up 已执行", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.btn_back_control)?.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun tailscaleUp(view: View): RootShell.CommandResult {
        val base = BinaryInstaller.getTailscalePath() +
                " up" +
                " --socket=" +
                BinaryInstaller.getTailscaleSockPath()
        val args = mutableListOf<String>()

        fun isChecked(id: Int) = view.findViewById<CheckBox>(id)?.isChecked == true
        fun getText(id: Int): String = view.findViewById<EditText>(id)?.text?.toString()?.trim().orEmpty()

        // 布尔型参数
        if (isChecked(R.id.check_accept_dns)) args += "--accept-dns" else args += "--accept-dns=false"
        if (isChecked(R.id.check_accept_risk)) args += "--accept-risk"
        if (isChecked(R.id.check_accept_routes)) args += "--accept-routes"
        if (isChecked(R.id.check_advertise_connector)) args += "--advertise-connector"
        if (isChecked(R.id.check_advertise_exit_node)) args += "--advertise-exit-node"
        if (isChecked(R.id.check_exit_node_lan)) args += "--exit-node-allow-lan-access"
        if (isChecked(R.id.check_force_reauth)) args += "--force-reauth"
        if (isChecked(R.id.check_json)) args += "--json"
        if (isChecked(R.id.check_qr)) args += "--qr"
        if (isChecked(R.id.check_reset)) args += "--reset"
        if (isChecked(R.id.check_shields_up)) args += "--shields-up"
        if (isChecked(R.id.check_ssh)) args += "--ssh"

        // 字符串参数
        val map = mapOf(
            "--advertise-routes" to getText(R.id.edit_advertise_routes),
            "--advertise-tags" to getText(R.id.edit_advertise_tags),
            "--authkey" to getText(R.id.edit_auth_key),
            "--exit-node" to getText(R.id.edit_exit_node),
            "--hostname" to getText(R.id.edit_hostname),
            "--login-server" to getText(R.id.edit_login_server),
            "--timeout" to getText(R.id.edit_timeout)
        )
        for ((k, v) in map) {
            if (v.isNotEmpty()) args += "$k=$v"
        }

        val fullCmd = "$base ${args.joinToString(" ")}"
        LogCollector.log("执行 tailscale up:\n$fullCmd")
        return RootShell.exec(fullCmd)
    }
}

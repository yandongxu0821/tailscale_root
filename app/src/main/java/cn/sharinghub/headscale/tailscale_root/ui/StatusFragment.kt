package cn.sharinghub.headscale.tailscale_root.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import cn.sharinghub.headscale.tailscale_root.R
import cn.sharinghub.headscale.tailscale_root.core.DaemonManager

class StatusFragment : Fragment(R.layout.fragment_status) {

    private lateinit var textDaemonStatus: TextView
    private lateinit var textIp: TextView
    private lateinit var textStatus: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_status, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textDaemonStatus = view.findViewById(R.id.text_daemon_status)
        textIp = view.findViewById(R.id.text_ip)
        textStatus = view.findViewById(R.id.text_status)

        loadStatus()
    }

    @SuppressLint("SetTextI18n")
    private fun loadStatus() {
        textDaemonStatus.text = "守护进程状态：查询中..."
        textIp.text = "Tailscale IP：查询中..."
        textStatus.text = "Status 输出：加载中..."

        Thread {
            val daemonRunning = DaemonManager.isRunning()
            val ip = DaemonManager.getTailscaleIP()
            val status = DaemonManager.getStatus()

            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread  // 防止 fragment 已销毁

                textDaemonStatus.text = "守护进程状态：" + if (daemonRunning) "已运行" else "未运行"
                textIp.text = "Tailscale IP：" + (ip ?: "未分配")
                textStatus.text = status.output.ifBlank { status.error }
            }
        }.start()
    }

}

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
    private lateinit var textOnline: TextView
    private lateinit var textIp: TextView
    private lateinit var textStatus: TextView
    private lateinit var buttonRefresh: View

    private var cachedDaemonStatus: String? = null
    private var cachedOnline: String? = null
    private var cachedIp: String? = null
    private var cachedStatus: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_status, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textDaemonStatus = view.findViewById(R.id.text_daemon_status)
        textOnline = view.findViewById(R.id.text_online)
        textIp = view.findViewById(R.id.text_ip)
        textStatus = view.findViewById(R.id.text_status)
        buttonRefresh = view.findViewById(R.id.button_refresh)

        textDaemonStatus.text = cachedDaemonStatus ?: "守护进程状态："
        textOnline.text = cachedOnline ?: "在线状态：      "
        textIp.text = cachedIp ?: "Tailscale IP：  "
        textStatus.text = cachedStatus ?: "Status 输出将在这里显示"

        buttonRefresh.setOnClickListener {
            loadStatus()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadStatus() {
        textDaemonStatus.text = "守护进程状态：查询中..."
        textOnline.text = "在线状态：        查询中...      "
        textIp.text = "Tailscale IP：    查询中..."
        textStatus.text = "......"

        Thread {
            val daemonRunning = DaemonManager.isRunning()
            val online = DaemonManager.isOnline()
            val ip = DaemonManager.getTailscaleIP()
            val status = DaemonManager.getStatus()

            val newDaemonStatus = "守护进程状态：" + if (daemonRunning) "正在运行" else "已停止"
            val newOnline = "在线状态：        " + if (online) "在线" else "离线"
            val newIp = "Tailscale IP：    " + (ip ?: "未分配")
            val newStatus = status.output.ifBlank { status.error }

            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread

                cachedDaemonStatus = newDaemonStatus
                cachedOnline = newOnline
                cachedIp = newIp
                cachedStatus = newStatus

                textDaemonStatus.text = newDaemonStatus
                textOnline.text = newOnline
                textIp.text = newIp
                textStatus.text = newStatus
            }
        }.start()
    }
}

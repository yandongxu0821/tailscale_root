package cn.sharinghub.headscale.tailscale_root.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import cn.sharinghub.headscale.tailscale_root.R
import cn.sharinghub.headscale.tailscale_root.core.BinaryInstaller
import cn.sharinghub.headscale.tailscale_root.core.DaemonManager

class ControlFragment : Fragment(R.layout.fragment_control) {

    private lateinit var btnInstall: Button
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnUp: Button
    private lateinit var btnDown: Button
    private lateinit var authKeyEdit: EditText
    private lateinit var resultText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_control, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnInstall = view.findViewById(R.id.btn_install)
        btnStart = view.findViewById(R.id.btn_start)
        btnStop = view.findViewById(R.id.btn_stop)
        btnUp = view.findViewById(R.id.btn_up)
        btnDown = view.findViewById(R.id.btn_down)
        resultText = view.findViewById(R.id.text_result)

        btnInstall.setOnClickListener {
            threadWithFeedback("安装二进制") {
                val ok = BinaryInstaller.installAllBinaries(requireContext())
                ok to "安装二进制 ${if (ok) "成功" else "失败"}"
            }
        }

        btnStart.setOnClickListener {
            threadWithFeedback("启动守护进程") {
                val ok = DaemonManager.startDaemon(requireContext())
                ok to "启动 tailscaled ${if (ok) "成功" else "失败"}"
            }
        }

        btnStop.setOnClickListener {
            threadWithFeedback("停止守护进程") {
                val ok = DaemonManager.stopDaemon()
                ok to "停止 tailscaled ${if (ok) "成功" else "失败"}"
            }
        }

        btnUp.setOnClickListener {
//            val key = authKeyEdit.text.toString().trim().ifEmpty { null }
//            threadWithFeedback("执行 tailscale up") {
//                val result = DaemonManager.tailscaleUp(key)
//                result.success to result.output.ifEmpty { result.error }
//            }
            findNavController().navigate(R.id.navigation_up_options)
        }

        btnDown.setOnClickListener {
            threadWithFeedback("执行 tailscale down") {
                val result = DaemonManager.tailscaleDown()
                result.success to result.output.ifEmpty { result.error }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun threadWithFeedback(title: String, action: () -> Pair<Boolean, String>) {
        resultText.text = "$title 中..."
        Thread {
            val (ok, msg) = action()
            requireActivity().runOnUiThread {
                resultText.text = "$title：${if (ok) "成功" else "失败"}\n$msg"
            }
        }.start()
    }
}

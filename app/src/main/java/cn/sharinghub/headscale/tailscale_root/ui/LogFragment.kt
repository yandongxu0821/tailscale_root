package cn.sharinghub.headscale.tailscale_root.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import cn.sharinghub.headscale.tailscale_root.R
import cn.sharinghub.headscale.tailscale_root.util.LogCollector

class LogFragment : Fragment(R.layout.fragment_log) {

    private lateinit var logText: TextView
    private lateinit var refreshButton: Button
    private lateinit var clearButton: Button
    private lateinit var tagSelect: TextView

    // 当前选中的 tag，默认显示 general
    private var currentTag = "general"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logText = view.findViewById(R.id.text_log)
        refreshButton = view.findViewById(R.id.btn_refresh)
        clearButton = view.findViewById(R.id.btn_clear)
        tagSelect = view.findViewById(R.id.tag_select)

        refreshButton.setOnClickListener {
            refreshLogs()
        }

        clearButton.setOnClickListener {
            LogCollector.clear(currentTag)
            refreshLogs()
        }

        tagSelect.setOnClickListener {
            toggleTag()
        }

        updateTagDisplay()
        refreshLogs()
    }

    private fun refreshLogs() {
        logText.text = LogCollector.getLogs(currentTag).ifBlank { "暂无日志。" }
    }

    private fun toggleTag() {
        currentTag = if (currentTag == "general") "daemon" else "general"
        updateTagDisplay()
        refreshLogs()
    }

    @SuppressLint("SetTextI18n")
    private fun updateTagDisplay() {
        tagSelect.text = "$currentTag  ⇋"
    }
}

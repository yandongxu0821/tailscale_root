package cn.sharinghub.headscale.tailscale_root.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import cn.sharinghub.headscale.tailscale_root.R
import cn.sharinghub.headscale.tailscale_root.core.TaildropManager
import cn.sharinghub.headscale.tailscale_root.core.DaemonManager
import cn.sharinghub.headscale.tailscale_root.util.LogCollector
import org.json.JSONObject

class ShareReceiverActivity : AppCompatActivity() {
    private lateinit var textInfo: TextView
    private lateinit var btnSend: Button
    private lateinit var btnCancel: Button
    private lateinit var spinnerPeers: Spinner

    private var sharedUris: List<Uri> = emptyList()
    private var peerMap: Map<String, String> = emptyMap()
    private var selectedPeerDns: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_receiver)

        textInfo = findViewById(R.id.text_shared_content)
        btnSend = findViewById(R.id.btn_send)
        btnCancel = findViewById(R.id.btn_cancel)
        spinnerPeers = findViewById(R.id.spinner_peers)

        handleIntent(intent)
        loadPeers()

        spinnerPeers.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long
            ) {
                val name = parent.getItemAtPosition(position) as String
                selectedPeerDns = peerMap[name]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedPeerDns = null
            }
        }

        btnSend.setOnClickListener {
            if (sharedUris.isNotEmpty() && selectedPeerDns != null) {
                val success = TaildropManager.sendFilesToPeer(this, sharedUris, selectedPeerDns!!)
                Toast.makeText(this, if (success) "发送成功" else "发送失败", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "未选择文件或目标设备", Toast.LENGTH_SHORT).show()
            }
            finish()
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let {
                sharedUris = listOf(it)
            }
            val fileNames = sharedUris.map { getFileNameFromUri(it) ?: it.toString() }
            textInfo.text = "即将分享的文件：\n\n" + fileNames.joinToString("\n")

        } else if (Intent.ACTION_SEND_MULTIPLE == action && type != null) {
            sharedUris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: emptyList()
            val fileNames = sharedUris.map { getFileNameFromUri(it) ?: it.toString() }
            textInfo.text = "即将分享的文件（共 ${sharedUris.size} 个）：\n\n" +
                    fileNames.joinToString("\n")

        } else {
            textInfo.text = "不支持的分享类型"
            btnSend.isEnabled = false
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
        return name
    }

    private fun loadPeers() {
        val status = DaemonManager.getStatusJson()
        val peers = mutableMapOf<String, String>() // HostName -> DNSName

        try {
            val json = JSONObject(status.output)
            val peerObj = json.getJSONObject("Peer")
            val keys = peerObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val peer = peerObj.getJSONObject(key)
                val hostName = peer.optString("HostName")
                val dnsName = peer.optString("DNSName")
                val online = peer.optBoolean("Online", false)
                if (online && hostName.isNotBlank() && dnsName.isNotBlank()) {
                    peers[hostName] = dnsName
                }
            }
        } catch (e: Exception) {
            e.message?.let { LogCollector.log(it) }
            e.printStackTrace()
        }

        if (peers.isNotEmpty()) {
            peerMap = peers
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, peers.keys.toList())
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerPeers.adapter = adapter
        } else {
            Toast.makeText(this, "未发现在线设备", Toast.LENGTH_SHORT).show()
            btnSend.isEnabled = false
        }
    }
}

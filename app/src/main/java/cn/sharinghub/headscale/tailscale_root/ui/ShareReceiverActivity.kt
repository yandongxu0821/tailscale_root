package cn.sharinghub.headscale.tailscale_root.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import cn.sharinghub.headscale.tailscale_root.R
import cn.sharinghub.headscale.tailscale_root.core.TaildropManager
import cn.sharinghub.headscale.tailscale_root.core.DaemonManager
import org.json.JSONObject

class ShareReceiverActivity : AppCompatActivity() {

    private lateinit var textInfo: TextView
    private lateinit var btnSend: Button
    private lateinit var btnCancel: Button
    private lateinit var spinnerPeers: Spinner

    private var sharedUri: Uri? = null
    private var peerMap: Map<String, String> = emptyMap()
    private var selectedPeerId: String? = null

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
                selectedPeerId = peerMap[name]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedPeerId = null
            }
        }

        btnSend.setOnClickListener {
            if (sharedUri != null && selectedPeerId != null) {
                val success = TaildropManager.sendFileToPeer(this, sharedUri!!, selectedPeerId!!)
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

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null && !type.startsWith("text/")) {
            sharedUri = intent.getParcelableExtra(Intent.EXTRA_STREAM)
            textInfo.text = "即将分享的文件：\n\n" + sharedUri?.toString()
        } else {
            textInfo.text = "只支持文件分享"
            btnSend.isEnabled = false
        }
    }

    private fun loadPeers() {
        val status = DaemonManager.getStatusJson()
        val peers = mutableMapOf<String, String>()

        try {
            val json = JSONObject(status.output)
            val peerList = json.getJSONArray("Peer")
            for (i in 0 until peerList.length()) {
                val peer = peerList.getJSONObject(i)
                val hostName = peer.optString("HostName")
                val taildropId = peer.optString("ID")
                if (hostName.isNotBlank() && taildropId.isNotBlank()) {
                    peers[hostName] = taildropId
                }
            }
        } catch (e: Exception) {
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

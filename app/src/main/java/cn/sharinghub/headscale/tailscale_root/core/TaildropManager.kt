package cn.sharinghub.headscale.tailscale_root.core

import android.content.Context
import android.net.Uri
import cn.sharinghub.headscale.tailscale_root.util.LogCollector
import java.io.File
import java.io.FileOutputStream

object TaildropManager {

    fun sendFileToPeer(context: Context, uri: Uri, peerId: String): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                LogCollector.log("taildrop", "无法打开文件 URI：$uri")
                return false
            }

            val tempFile = File.createTempFile("upload_", null, context.cacheDir)
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }

            val destPath = "$peerId:/${tempFile.name}"
            val cmd = listOf(
                BinaryInstaller.getTailscalePath(),
                " --socket=${DaemonManager.SOCKET_PATH}",
                " file",
                " cp",
                tempFile.absolutePath,
                destPath
            )

            LogCollector.log("发送文件命令：${cmd.joinToString(" ")}")
            val result = RootShell.exec(cmd)

            LogCollector.log("tailscale file cp 输出：${result.output}")
            result.success
        } catch (e: Exception) {
            LogCollector.log("发送文件失败：${e.message}")
            e.printStackTrace()
            false
        }
    }
}

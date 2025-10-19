package cn.sharinghub.headscale.tailscale_root.core

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import cn.sharinghub.headscale.tailscale_root.util.LogCollector
import java.io.File
import java.io.FileOutputStream

object TaildropManager {
    fun getFileNameFromUri(context: Context, uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) return cursor.getString(index)
            }
        }
        return null
    }
    fun sendFilesToPeer(context: Context, uris: List<Uri>, hostname: String): Boolean {
        return try {
            // 目标目录
            val dropTempDir = File("/data/local/temp/tailscale/droptemp")
            if (!dropTempDir.exists()) {
                dropTempDir.mkdirs()
                RootShell.exec(listOf("chmod -R 777 ${dropTempDir.absolutePath}"))
            }

            val tempFiles = uris.map { uri ->
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("无法打开 $uri")

                val fileName = getFileNameFromUri(context, uri) ?: "upload_${System.currentTimeMillis()}"
                val targetFile = File(dropTempDir, fileName)

                FileOutputStream(targetFile).use { output ->
                    inputStream.copyTo(output)
                }
                targetFile
            }

            val destPath = "$hostname:"
            val cmd = buildString {
                append(BinaryInstaller.getTailscalePath())
                append(" --socket=${DaemonManager.SOCKET_PATH} file cp ")
                append(tempFiles.joinToString(" ") { "\"${it.absolutePath}\"" })
                append(" ")
                append(destPath)
            }

            LogCollector.log("发送文件命令：$cmd")
            val result = RootShell.exec(listOf(cmd))
            result.success
        } catch (e: Exception) {
            LogCollector.log("发送文件失败：${e.message}")
            false
        }
    }

}

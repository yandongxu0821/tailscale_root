package cn.sharinghub.headscale.tailscale_root.core

import android.annotation.SuppressLint
import android.content.Context
import cn.sharinghub.headscale.tailscale_root.util.LogCollector
import java.io.File
import java.io.FileOutputStream

object BinaryInstaller {

    private const val INSTALL_PATH = "/data/local/temp/tailscale/"

    @SuppressLint("SetWorldReadable")
    private fun extractAndCopyBinary(context: Context, assetName: String, outputName: String): Boolean {
        return try {
            // 1. 写入到 app 私有目录
            val tempFile = File(context.filesDir, outputName)
            context.assets.open(assetName).use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile.setReadable(true, false)
            tempFile.setExecutable(true, false)
            LogCollector.log("已写入临时文件: ${tempFile.absolutePath}")

            // 2. 使用 root 权限复制到目标目录
            val dstPath = INSTALL_PATH + outputName
            val copyCmd = "cp ${tempFile.absolutePath} $dstPath && chmod 755 $dstPath"
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", copyCmd))
            val code = process.waitFor()
            LogCollector.log("复制到 $dstPath 并赋权，返回码: $code")

            code == 0
        } catch (e: Exception) {
            LogCollector.log("安装 $assetName 失败: ${e.message}")
            false
        }
    }

    fun installAllBinaries(context: Context): Boolean {
        return try {
            // 创建目标目录
            val mkdirCmd = "mkdir -p $INSTALL_PATH"
            val result = Runtime.getRuntime().exec(arrayOf("su", "-c", mkdirCmd)).waitFor()
            LogCollector.log("创建安装目录 $INSTALL_PATH 返回码: $result")

            val ok1 = extractAndCopyBinary(context, "tailscaled", "tailscaled")
            val ok2 = extractAndCopyBinary(context, "tailscale", "tailscale")

            ok1 && ok2
        } catch (e: Exception) {
            LogCollector.log("安装失败: ${e.message}")
            false
        }
    }

    fun getTailscaledPath(): String = INSTALL_PATH + "tailscaled"
    fun getTailscalePath(): String = INSTALL_PATH + "tailscale"
    fun getTailscaleSockPath(): String = INSTALL_PATH + "tailscaled.sock"
    // fun getInstallPath(): String = INSTALL_PATH
}

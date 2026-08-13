package cn.sharinghub.headscale.tailscale_root.core

import android.content.Context
import cn.sharinghub.headscale.tailscale_root.util.LogCollector
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {

    private const val VERSION_ENDPOINT = "https://pkgs.tailscale.com/stable/?mode=json"
    private const val DOWNLOAD_TEMPLATE = "https://pkgs.tailscale.com/stable/tailscale_%s_arm.tgz"

    data class Result(val success: Boolean, val message: String)

    fun checkRemoteVersion(): String? {
        return try {
            val conn = URL(VERSION_ENDPOINT).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000

            if (conn.responseCode != 200) {
                LogCollector.log("获取版本号失败，HTTP ${conn.responseCode}")
                return null
            }

            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val json = org.json.JSONObject(text)
            json.optString("TarballsVersion", null)
        } catch (e: Exception) {
            LogCollector.log("检查远端版本失败：${e.message}")
            null
        }
    }

    fun getLocalVersion(): String? {
        return try {
            val cmd = "${BinaryInstaller.getTailscalePath()} --socket ${BinaryInstaller.getTailscaleSockPath()} version"
            val res = RootShell.exec(cmd)
            if (!res.success) return null
            val firstLine = res.output.lineSequence().firstOrNull()?.trim()
            firstLine
        } catch (e: Exception) {
            LogCollector.log("读取本地版本失败：${e.message}")
            null
        }
    }

    fun downloadTgz(context: Context, version: String): File? {
        val url = String.format(DOWNLOAD_TEMPLATE, version)
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000

            if (conn.responseCode != 200) {
                LogCollector.log("下载失败，HTTP ${conn.responseCode}")
                return null
            }

            val tempFile = File(context.cacheDir, "tailscale_update_${version}.tgz")
            conn.inputStream.use { input ->
                FileOutputStream(tempFile).use { out ->
                    input.copyTo(out)
                }
            }
            LogCollector.log("已下载到 ${tempFile.absolutePath}")
            tempFile
        } catch (e: Exception) {
            LogCollector.log("下载失败：${e.message}")
            null
        }
    }

    private fun extractTgz(tgz: File): File? {
        return try {
            val parent = tgz.parentFile ?: return null
            val tgzPath = tgz.absolutePath
            val destPath = parent.absolutePath

            // 使用系统 tar 命令解压到目标目录（需要 root 权限）
            val cmd = listOf("tar -xzf '$tgzPath' -C '$destPath'")
            val res = RootShell.exec(cmd)
            if (!res.success) {
                LogCollector.log("解压失败：${res.error}")
                return null
            }
            parent
        } catch (e: Exception) {
            LogCollector.log("解压失败：${e.message}")
            null
        }
    }

    private fun replaceBinaries(extractedDir: File, version: String): Boolean {
        try {
            val root = extractedDir
            // 查找包含 tailscale 和 tailscaled 的文件
            val tailscaleFile = root.walkTopDown().firstOrNull { it.name == "tailscale" }
            val tailscaledFile = root.walkTopDown().firstOrNull { it.name == "tailscaled" }

            if (tailscaleFile == null || tailscaledFile == null) {
                LogCollector.log("未在解压目录找到二进制文件")
                return false
            }

            val dstTailscale = BinaryInstaller.getTailscalePath()
            val dstTailscaled = BinaryInstaller.getTailscaledPath()

            val cmds = listOf(
                "cp ${tailscaleFile.absolutePath} $dstTailscale",
                "chmod 755 $dstTailscale",
                "cp ${tailscaledFile.absolutePath} $dstTailscaled",
                "chmod 755 $dstTailscaled"
            )

            val res = RootShell.exec(cmds)
            if (!res.success) {
                LogCollector.log("替换二进制失败：${res.error}")
                return false
            }

            LogCollector.log("已替换为 $version")
            return true
        } catch (e: Exception) {
            LogCollector.log("替换失败：${e.message}")
            return false
        }
    }

    fun performUpdate(context: Context): Result {
        var outcome: Result
        try {
            val remote = checkRemoteVersion() ?: return Result(false, "无法获取远端版本")
            val local = getLocalVersion()
            LogCollector.log("远端版本=$remote 本地版本=$local")

            if (local != null && local.startsWith(remote)) {
                outcome = Result(true, "已是最新版本")
                return outcome
            }

            val tgz = downloadTgz(context, remote) ?: return Result(false, "下载失败")
            val extracted = extractTgz(tgz) ?: return Result(false, "解压失败")

            // 停止 daemon
            val stopOk = DaemonManager.stopDaemon()
            if (!stopOk) {
                outcome = Result(false, "停止 tailscaled 失败")
                return outcome
            }

            val replaced = replaceBinaries(extracted, remote)
            if (!replaced) {
                outcome = Result(false, "替换二进制失败")
                return outcome
            }

            val startOk = DaemonManager.startDaemon()
            if (!startOk) {
                outcome = Result(false, "重启 tailscaled 失败")
                return outcome
            }

            outcome = Result(true, "更新成功：$remote")
            return outcome
        } catch (e: Exception) {
            LogCollector.log("更新失败：${e.message}")
            outcome = Result(false, "更新异常：${e.message}")
            return outcome
        } finally {
            // 无论如何，保证守护进程处于运行状态
            try {
                val running = DaemonManager.isRunning()
                if (!running) {
                    LogCollector.log("更新流程结束，守护进程未运行，尝试启动...")
                    val started = DaemonManager.startDaemon()
                    LogCollector.log("尝试启动守护进程结果: $started")
                } else {
                    LogCollector.log("更新流程结束，守护进程已在运行")
                }
            } catch (ex: Exception) {
                LogCollector.log("确保守护进程运行时发生异常：${ex.message}")
            }
        }
    }
}
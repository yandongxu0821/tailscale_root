package cn.sharinghub.headscale.tailscale_root.core

import android.content.Context
import cn.sharinghub.headscale.tailscale_root.util.LogCollector

object DaemonManager {

    private const val STATE_PATH = "/data/local/temp/tailscale/state.json"
    const val SOCKET_PATH = "/data/local/temp/tailscale/tailscaled.sock"
    private const val PORT = 41641

    /**
     * 启动 tailscaled 守护进程
     */
    private var tailscaledProcess: Process? = null

    fun startDaemon(context: Context): Boolean {
        LogCollector.log("正在启动 tailscaled（捕获日志）...")

        try {
            // 启动前准备
            RootShell.exec(listOf(
                "setenforce 0",
                "mkdir -p /dev/net",
                "ln -sf /dev/tun /dev/net/tun",
                "chmod 755 ${BinaryInstaller.getTailscaledPath()}"
            ))

            // 构造命令行
            val cmd = listOf(
                "su", "-c",
                "${BinaryInstaller.getTailscaledPath()} " +
                        "--tun=tun " +
                        "--state=$STATE_PATH " +
                        "--socket=$SOCKET_PATH " +
                        "--port=$PORT"
            )

            val builder = ProcessBuilder(cmd)
            builder.redirectErrorStream(true) // stderr 合并到 stdout

            tailscaledProcess = builder.start()

            // 异步读取日志
            Thread {
                val reader = tailscaledProcess!!.inputStream.bufferedReader()
                reader.forEachLine { line ->
                    LogCollector.log(line, tag = "daemon")
                }
            }.start()

            LogCollector.log("tailscaled 启动成功，同时开始捕获日志")
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            LogCollector.log("启动 tailscaled 失败：${e.message}")
            return false
        }
    }

    /**
     * 停止 tailscaled 守护进程（简单暴力）
     */
    fun stopDaemon(): Boolean {
        val result1 = RootShell.exec("pkill tailscaled")
        val result2 = RootShell.exec("rm -rf $SOCKET_PATH")
        return result1.success && result2.success
    }

    /**
     * 启动 tailscale 网络连接（需要登录或 authkey）
     * 已被弃用，并升级为支持全部参数的选项页
     */
//    fun tailscaleUp(authKey: String? = null): RootShell.CommandResult {
//        val baseCmd = "${BinaryInstaller.getTailscalePath()} --socket=$SOCKET_PATH up"
//        val fullCmd = if (!authKey.isNullOrBlank()) "$baseCmd --authkey=$authKey" else baseCmd
//        return RootShell.exec(fullCmd)
//    }

    /**
     * 停止 tailscale 网络连接
     */
    fun tailscaleDown(): RootShell.CommandResult {
        val cmd = "${BinaryInstaller.getTailscalePath()} --socket=$SOCKET_PATH down"
        return RootShell.exec(cmd)
    }

    /**
     * 获取当前连接状态（tailscale status）
     */
    fun getStatus(): RootShell.CommandResult {
        val cmd = "${BinaryInstaller.getTailscalePath()} --socket=$SOCKET_PATH status"
        return RootShell.exec(cmd)
    }
    /**
     * 获取当前连接状态（tailscale status --json）
     */
    fun getStatusJson(): RootShell.CommandResult {
        val cmd = "${BinaryInstaller.getTailscalePath()} --socket=$SOCKET_PATH status --json"
        return RootShell.exec(cmd)
    }


    /**
     * 获取当前分配的 Tailscale IP 地址（提取自 status）
     */
    fun getTailscaleIP(): String? {
        val result = getStatus()
        if (!result.success) return null
        val regex = Regex("""^100\.\d+\.\d+\.\d+""", RegexOption.MULTILINE)
        return regex.find(result.output)?.value
    }

    /**
     * 从 Taildrop 获取接收到的文件
     */
    fun recvFileFromTaildrop(): RootShell.CommandResult {
        val cmd = "${BinaryInstaller.getTailscalePath()} --socket=$SOCKET_PATH file get /sdcard/Download/"
        return RootShell.exec(cmd)
    }

    /**
     * 检查 tailscaled 是否已启动（根据 socket 是否存在）
     */
    fun isRunning(): Boolean {
        val result = RootShell.exec("[ -S $SOCKET_PATH ] && echo RUNNING || echo STOPPED")
        return result.output.trim() == "RUNNING"
    }
}

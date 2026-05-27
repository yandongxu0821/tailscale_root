package cn.sharinghub.headscale.tailscale_root.core

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object RootShell {

    data class CommandResult(
        val success: Boolean,
        val output: String,
        val error: String
    )

    /**
     * 执行单条 root 命令
     * 转发到 exec(commands: List<String>) 执行
     * @param cmd: String
     */
    fun exec(cmd: String): CommandResult {
        return exec(listOf(cmd))
    }

    /**
     * 执行多条 root 命令
     * @param commands: List<String>
     */
    fun exec(commands: List<String>): CommandResult {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            val stdout = StringBuilder()
            val stderr = StringBuilder()

            for (cmd in commands) {
                os.writeBytes("$cmd\n")
            }
            os.writeBytes("exit\n")
            os.flush()

            val outThread = Thread {
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach {
                        stdout.appendLine(it)
                    }
                }
            }

            val errThread = Thread {
                process.errorStream.bufferedReader().useLines { lines ->
                    lines.forEach {
                        stderr.appendLine(it)
                    }
                }
            }

            outThread.start()
            errThread.start()

            val exitCode = process.waitFor()

            outThread.join()
            errThread.join()

            CommandResult(
                success = exitCode == 0,
                output = stdout.toString().trim(),
                error = stderr.toString().trim()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            CommandResult(false, "", e.message ?: "Exception")
        }
    }

    /**
     * 快捷判断命令是否成功
     */
    fun isSuccess(cmd: String): Boolean {
        return exec(cmd).success
    }
}

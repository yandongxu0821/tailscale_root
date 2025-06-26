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
     */
    fun exec(cmd: String): CommandResult {
        return exec(listOf(cmd))
    }

    /**
     * 执行多条 root 命令
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

            // 读取标准输出
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                stdout.appendLine(line)
            }

            // 读取标准错误
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            while (errorReader.readLine().also { line = it } != null) {
                stderr.appendLine(line)
            }

            val exitCode = process.waitFor()
            CommandResult(
                success = (exitCode == 0),
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

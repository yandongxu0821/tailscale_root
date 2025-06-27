package cn.sharinghub.headscale.tailscale_root.util

import java.text.SimpleDateFormat
import java.util.*

object LogCollector {
    private const val MAX_LOG_SIZE = 500

    private val logMap = mutableMapOf<String, MutableList<String>>()

    /**
     * 添加日志
     * @param msg 日志内容
     * @param tag 日志类别（如 "general", "daemon"）
     */
    fun log(msg: String, tag: String = "general") {
        val timestamp = "[" + SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()) + "] "
        val entry = timestamp + msg

        val list = logMap.getOrPut(tag) { mutableListOf() }
        list.add(entry)
        if (list.size > MAX_LOG_SIZE) list.removeAt(0)
    }

    /**
     * 获取指定 tag 的日志拼接文本
     */
    fun getLogs(tag: String = "general"): String {
        return logMap[tag]?.joinToString("\n") ?: ""
    }

    /**
     * 清空指定 tag 的日志，默认清空全部
     */
    fun clear(tag: String = "all") {
        if (tag == "all") {
            logMap.clear()
        } else {
            logMap.remove(tag)
        }
    }

    /**
     * 获取所有 tag 名称（用于 UI 切换）
     */
    fun getTags(): Set<String> = logMap.keys
}

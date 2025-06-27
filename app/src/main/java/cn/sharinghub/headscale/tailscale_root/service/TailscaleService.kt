package cn.sharinghub.headscale.tailscale_root.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import cn.sharinghub.headscale.tailscale_root.R
import cn.sharinghub.headscale.tailscale_root.core.DaemonManager
import cn.sharinghub.headscale.tailscale_root.MainActivity

class TailscaleService : Service() {

    companion object {
        const val CHANNEL_ID = "tailscale_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SERVICE = "cn.sharinghub.headscale.tailscale_root.STOP_SERVICE"
    }

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Tailscaled 启动中..."))
        startTailscaled()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Tailscale 守护进程",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于保持 tailscaled 持续运行的通知"
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(content: String): Notification {
        // 点击通知跳转 MainActivity
        val mainIntent = Intent(this, MainActivity::class.java)
        val pendingMainIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 添加“停止服务”按钮
        val stopIntent = Intent(this, TailscaleService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val pendingStopIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tailscale Rooted")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingMainIntent)
            .addAction(R.drawable.ic_notification, "停止服务", pendingStopIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = buildNotification(content)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun startTailscaled() {
        Thread {
            val success = DaemonManager.startDaemon(this)
            updateNotification(
                if (success) "tailscaled 正在运行"
                else "tailscaled 启动失败，请检查 root 权限或配置"
            )
        }.start()
    }
}

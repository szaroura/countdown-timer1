package com.example.countdowntimer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    private val binder = LocalBinder()

    private val _remainingMillis = MutableStateFlow(0L)
    val remainingMillis: StateFlow<Long> = _remainingMillis.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _justFinished = MutableStateFlow(0L)
    val justFinished: StateFlow<Long> = _justFinished.asStateFlow()

    private var countDownTimer: CountDownTimer? = null
    private var toneGenerator: ToneGenerator? = null
    private var totalMillis: Long = 0L

    override fun onCreate() {
        super.onCreate()
        toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME)
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun start(totalMs: Long, remainingMs: Long) {
        totalMillis = totalMs
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(remainingMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _remainingMillis.value = millisUntilFinished
                updateNotification(millisUntilFinished)
            }

            override fun onFinish() {
                _remainingMillis.value = 0L
                _isRunning.value = false
                playAlarm()
                showFinishedNotification()
                _justFinished.value = System.currentTimeMillis()
            }
        }.start()
        _isRunning.value = true
        startForeground(NOTIF_ID, buildNotification(remainingMs))
    }

    fun pause() {
        countDownTimer?.cancel()
        _isRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    fun reset() {
        countDownTimer?.cancel()
        _isRunning.value = false
        _remainingMillis.value = 0L
        totalMillis = 0L
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    fun setRemaining(ms: Long) {
        _remainingMillis.value = ms
    }

    private fun playAlarm() {
        Thread {
            repeat(6) {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300)
                Thread.sleep(500)
            }
        }.start()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows the running countdown"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(this, 0, intent, flags)
    }

    private fun buildNotification(remainingMs: Long): Notification {
        val mm = (remainingMs / 1000) / 60
        val ss = (remainingMs / 1000) % 60
        val text = String.format("%02d:%02d remaining", mm, ss)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Timer running")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent())
            .build()
    }

    private fun updateNotification(remainingMs: Long) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, buildNotification(remainingMs))
    }

    private fun showFinishedNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Time's up")
            .setContentText("Your countdown has finished.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setAutoCancel(true)
            .setContentIntent(contentIntent())
            .build()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, notification)
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        toneGenerator?.release()
        toneGenerator = null
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "timer_channel"
        const val NOTIF_ID = 1
    }
}

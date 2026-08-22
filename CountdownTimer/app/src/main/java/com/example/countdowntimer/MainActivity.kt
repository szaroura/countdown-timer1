package com.example.countdowntimer

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private var timerService: TimerService? = null
    private var bound by mutableStateOf(false)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.LocalBinder
            timerService = binder.getService()
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            bound = false
        }
    }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (bound) {
                        TimerScreen(service = timerService!!)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, TimerService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }
}

@Composable
fun TimerScreen(service: TimerService) {
    var minutesText by remember { mutableStateOf("5") }
    var secondsText by remember { mutableStateOf("0") }
    var totalMillis by remember { mutableStateOf(5 * 60_000L) }

    val remainingMillis by service.remainingMillis.collectAsState()
    val isRunning by service.isRunning.collectAsState()

    LaunchedEffect(Unit) {
        if (remainingMillis == 0L && !isRunning) {
            service.setRemaining(totalMillis)
        }
    }

    fun currentInputMillis(): Long {
        val mins = minutesText.toLongOrNull() ?: 0L
        val secs = secondsText.toLongOrNull() ?: 0L
        return (mins * 60 + secs) * 1000
    }

    fun startTimer() {
        val remaining = if (remainingMillis > 0L) remainingMillis else currentInputMillis()
        if (remaining <= 0L) return
        if (remainingMillis <= 0L || totalMillis <= 0L) {
            totalMillis = currentInputMillis()
        }
        service.start(totalMillis, remaining)
    }

    fun pauseTimer() {
        service.pause()
    }

    fun resetTimer() {
        service.reset()
        totalMillis = currentInputMillis()
        service.setRemaining(totalMillis)
    }

    val displaySeconds = remainingMillis / 1000
    val mm = displaySeconds / 60
    val ss = displaySeconds % 60
    val display = String.format("%02d:%02d", mm, ss)
    val progress = if (totalMillis > 0) remainingMillis.toFloat() / totalMillis.toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
            CircularProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape),
                strokeWidth = 10.dp,
                color = if (displaySeconds in 1..10) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
            )
            Text(text = display, fontSize = 44.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = minutesText,
                onValueChange = {
                    if (it.length <= 3) minutesText = it.filter { c -> c.isDigit() }
                },
                label = { Text("min") },
                enabled = !isRunning,
                modifier = Modifier.width(90.dp)
            )
            Text(":", fontSize = 20.sp)
            OutlinedTextField(
                value = secondsText,
                onValueChange = {
                    if (it.length <= 2) secondsText = it.filter { c -> c.isDigit() }
                },
                label = { Text("sec") },
                enabled = !isRunning,
                modifier = Modifier.width(90.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = {
                if (isRunning) pauseTimer() else startTimer()
            }) {
                Text(
                    if (isRunning) "Pause"
                    else if (remainingMillis in 1 until totalMillis) "Resume"
                    else "Start"
                )
            }
            OutlinedButton(onClick = { resetTimer() }) {
                Text("Reset")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Keeps counting down in the background too — check the notification.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

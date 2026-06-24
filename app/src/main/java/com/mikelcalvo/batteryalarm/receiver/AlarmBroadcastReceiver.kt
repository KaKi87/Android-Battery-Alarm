package com.mikelcalvo.batteryalarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.mikelcalvo.batteryalarm.R
import com.mikelcalvo.batteryalarm.model.AlarmState
import com.mikelcalvo.batteryalarm.model.AlarmType
import com.mikelcalvo.batteryalarm.model.SILENT_NOTIFICATION_SOUND_URI

class AlarmBroadcastReceiver : BroadcastReceiver() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onReceive(context: Context, intent: Intent) {
        val defaultAlarmSound = Uri.parse("android.resource://" + context.packageName + "/" + R.raw.default_alarm)
        val selectedAlarmSound = intent.getStringExtra("alarmSound")
        val repeatingTimes = intent.getIntExtra("repeatingTimes", 1)
        val alarmType = intent.getStringExtra("alarmType")

        if (selectedAlarmSound == SILENT_NOTIFICATION_SOUND_URI) {
            vibrator = getVibrator(context)
            vibrator?.vibrate(createVibrationEffect(repeatingTimes))
            observeAlarmStop(alarmType)
            return
        }

        Log.d("AlarmBroadcastReceiver", "Alarm sound: $selectedAlarmSound")

        val alarmSound: Uri = Uri.parse(selectedAlarmSound ?: defaultAlarmSound.toString())

        if (isUriValid(context, alarmSound)) {
            var repeatedTimes = 0

            mediaPlayer = MediaPlayer.create(context, alarmSound).apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = false
                setOnCompletionListener {
                    val preferences = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)
                    repeatedTimes++

                    if (repeatedTimes < repeatingTimes) {
                        if(alarmType == AlarmType.BATTERY_LOW.name && preferences.getBoolean("enabled_${AlarmType.BATTERY_LOW.name}", false)){
                            start()
                        }
                        if(alarmType == AlarmType.BATTERY_FULL.name && preferences.getBoolean("enabled_${AlarmType.BATTERY_FULL.name}", false)){
                            start()
                        }
                    }
                }
                start()
            }

            observeAlarmStop(alarmType)
        }
    }

    private fun isUriValid(context: Context, uri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun observeAlarmStop(alarmType: String?) {
        AlarmState.shouldStopLowBatteryAlarm.observeForever { shouldStop ->
            if(alarmType == AlarmType.BATTERY_LOW.name && shouldStop) {
                mediaPlayer?.stop()
                vibrator?.cancel()
            }
        }

        AlarmState.shouldStopFullChargeAlarm.observeForever { shouldStop ->
            if(alarmType == AlarmType.BATTERY_FULL.name && shouldStop) {
                mediaPlayer?.stop()
                vibrator?.cancel()
            }
        }
    }

    private fun createVibrationEffect(repeatingTimes: Int): VibrationEffect {
        if (repeatingTimes == Int.MAX_VALUE) {
            return VibrationEffect.createWaveform(longArrayOf(0, 1_000, 1_000), 0)
        }

        val safeRepeatTimes = repeatingTimes.coerceAtLeast(1)
        val pattern = LongArray((safeRepeatTimes * 2) + 1).apply {
            this[0] = 0
            for (index in 0 until safeRepeatTimes) {
                this[(index * 2) + 1] = 1_000
                this[(index * 2) + 2] = 1_000
            }
        }

        return VibrationEffect.createWaveform(pattern, -1)
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
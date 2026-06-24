package com.mikelcalvo.batteryalarm.model

import android.net.Uri

const val SILENT_NOTIFICATION_SOUND_URI = "silent"

data class NotificationSound(
    val title: String,
    val uri: Uri?
)
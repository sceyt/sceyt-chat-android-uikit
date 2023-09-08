package com.sceyt.sceytchatuikit.extensions

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlin.random.Random

@SuppressLint("UnspecifiedImmutableFlag")
fun Context.initPendingIntent(intent: Intent): PendingIntent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.getActivity(this, Random.nextInt(), intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    } else
        PendingIntent.getActivity(this, Random.nextInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT)
}

@SuppressLint("UnspecifiedImmutableFlag")
fun Context.getBroadcastPendingIntent(intent: Intent): PendingIntent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.getBroadcast(this, Random.nextInt(), intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    } else
        PendingIntent.getBroadcast(this, Random.nextInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT)
}

fun TaskStackBuilder.getPendingIntent(): PendingIntent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE)
    } else
        getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
}
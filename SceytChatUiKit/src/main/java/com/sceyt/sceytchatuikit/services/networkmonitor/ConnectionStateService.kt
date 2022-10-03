package com.sceyt.sceytchatuikit.services.networkmonitor

import android.net.ConnectivityManager
import android.net.Network
import com.hadilq.liveevent.LiveEvent

interface ConnectionStateService {
    fun addAvailableCallback(key: String, callback: (Boolean) -> Unit)
    fun addNetworkCallback(networkCallback: ConnectivityManager.NetworkCallback)
    fun unRegisterCallback(callBack: ConnectivityManager.NetworkCallback)
    fun getOnAvailableLiveData(): LiveEvent<Network>
}
package com.sceyt.sceytchatuikit.services.networkmonitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import com.hadilq.liveevent.LiveEvent
import com.sceyt.sceytchatuikit.extensions.checkActiveInternetConnection


class ConnectionStateServiceImpl(private val context: Context) : ConnectionStateService {
    private val mOnAvailableLiveData by lazy { LiveEvent<Network>() }
    private val mOnUnavailableLiveData by lazy { LiveEvent<Boolean>() }
    private val mOnLostLiveData by lazy { LiveEvent<Network>() }
    private val mAvailableCallbackList by lazy { HashMap<String, (Boolean) -> Unit>() }

    private val networkRequest: NetworkRequest = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .build()

    private val mConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private lateinit var mNetworkCallback: ConnectivityManager.NetworkCallback

    init {
        registerCallback()
    }

    private fun registerCallback() {
        mNetworkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                super.onLost(network)
                mOnLostLiveData.postValue(network)
            }

            override fun onUnavailable() {
                super.onUnavailable()
                mOnUnavailableLiveData.postValue(true)
            }

            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                if (context.checkActiveInternetConnection(5000)) {
                    mOnAvailableLiveData.postValue(network)
                    mAvailableCallbackList.values.forEach {
                        it.invoke(true)
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mConnectivityManager.registerDefaultNetworkCallback(mNetworkCallback)
        } else
            mConnectivityManager.registerNetworkCallback(networkRequest, mNetworkCallback)
    }

    override fun addAvailableCallback(key: String, callback: (Boolean) -> Unit) {
        mAvailableCallbackList[key] = callback
    }

    override fun addNetworkCallback(networkCallback: ConnectivityManager.NetworkCallback) {
        mConnectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    override fun unRegisterCallback(callBack: ConnectivityManager.NetworkCallback) {
        mConnectivityManager.unregisterNetworkCallback(callBack)
    }

    override fun getOnAvailableLiveData() = mOnAvailableLiveData
}
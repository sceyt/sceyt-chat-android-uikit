package com.sceyt.push.huawei

import android.content.Context
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.api.ConnectionResult.SUCCESS
import com.huawei.hms.api.HuaweiApiAvailability
import com.sceyt.chatuikit.push.PushDevice
import com.sceyt.chatuikit.push.providers.PushDeviceProvider
import com.sceyt.chatuikit.push.PushServiceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HuaweiPushServiceProvider(
        context: Context,
        private val appId: String,
) : PushDeviceProvider {
    private val hmsInstanceId: HmsInstanceId = HmsInstanceId.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun isSupported(context: Context): Boolean {
        return HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(context) == SUCCESS
    }

    override fun generatePushDeviceAsync(onDeviceGenerated: (pushDevice: PushDevice) -> Unit) {
        scope.launch {
            val result = runCatching { hmsInstanceId.getToken(appId, "HCM") }
            if (result.isSuccess) {
                val token = result.getOrNull() ?: return@launch
                onDeviceGenerated(
                    PushDevice(
                        token = token,
                        service = PushServiceType.Hms
                    ))
            }
        }
    }
}

package com.sceyt.chat.demo.call.ui.dialpad

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.call.R
import com.sceyt.chat.demo.call.manager.CallManager
import com.sceyt.chat.demo.call.ui.CallActivity
import com.sceyt.chat.demo.call.ui.theme.CallColors
import com.sceyt.chat.demo.call.ui.theme.CallTheme
import com.sceyt.chat.models.signal.MediaFlow
import com.sceyt.tonemanager.audio.dtmf.DtmfToneConfig
import com.sceyt.tonemanager.manager.ToneManagerFactory
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class DialPadActivity : ComponentActivity() {
    private val callManager: CallManager by inject()
    private val toneManager by lazy { ToneManagerFactory.getInstance(this) }
    private var lastNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            CallTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DialPadScreen(
                        onBack = ::finish,
                        onToneClick = ::playDialPadTone,
                        onCallClick = { phoneNumber ->
                            makeCall(phoneNumber)
                        }
                    )
                }
            }
        }
    }

    private fun playDialPadTone(tone: Char) {
        lifecycleScope.launch {
            toneManager.playDtmfTone(
                tone = tone,
                durationMs = DIAL_PAD_TONE_DURATION_MS,
                volume = DIAL_PAD_TONE_VOLUME,
                config = DtmfToneConfig.media()
            ).onFailure {
                Log.w(TAG, "Failed to play dial pad tone", it)
            }
        }
    }

    private fun makeCall(number: String) {
        lifecycleScope.launch {
            lastNumber = number
            val missingPermissions = getMissingPermissions()

            if (missingPermissions.isEmpty()) {
                if (number.isBlank()) {
                    Toast.makeText(
                        this@DialPadActivity, "Please wire the number", Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                callManager.startOutgoingCall(
                    userId = number,
                    mediaFlow = MediaFlow.S2W,
                    isVideo = false,
                    isCallAgain = false
                ) {
                    CallActivity.launchOngoing(context = this@DialPadActivity)
                }
            } else {
                permissionLauncher.launch(missingPermissions.toTypedArray())
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            makeCall(lastNumber)
        } else {
            Toast.makeText(
                this,
                "Permissions required making a call",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getMissingPermissions(): List<String> {
        val required = mutableListOf(Manifest.permission.RECORD_AUDIO)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            required.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        return required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private const val TAG = "DialPadActivity"
        private const val DIAL_PAD_TONE_DURATION_MS = 100
        private const val DIAL_PAD_TONE_VOLUME = 50

        fun createIntent(context: Context): Intent {
            return Intent(context, DialPadActivity::class.java)
        }
    }
}

@Composable
private fun DialPadScreen(
    onBack: () -> Unit,
    onToneClick: (Char) -> Unit,
    onCallClick: (String) -> Unit,
    initialPhoneNumber: String = "",
) {
    val context = LocalContext.current
    val repository = remember(context) { LastCalledNumberRepository(context.applicationContext) }
    val colors = dialPadColors()
    var phoneNumber by rememberSaveable(initialPhoneNumber) { mutableStateOf(initialPhoneNumber) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp)
    ) {
        DialPadToolbar(
            colors = colors,
            onBack = onBack
        )

        PhoneNumberDisplay(
            modifier = Modifier.weight(1f),
            phoneNumber = phoneNumber,
            colors = colors,
            onBackspaceClick = {
                phoneNumber = phoneNumber.dropLast(1)
            },
            onClearClick = {
                phoneNumber = ""
            }
        )

        Spacer(modifier = Modifier.height(36.dp))

        DialPad(
            colors = colors,
            onDigitClick = { tone, value ->
                onToneClick(tone)
                phoneNumber += value
            },
        )

        Spacer(modifier = Modifier.height(20.dp))

        DialPadCallButton(
            colors = colors,
            onCallClick = {
                val number = phoneNumber.trim()
                if (number.isEmpty()) {
                    phoneNumber = repository.getLastCalledNumber().orEmpty()
                } else {
                    repository.saveLastCalledNumber(number)
                    onCallClick(number)
                }
            }
        )

        Spacer(
            modifier = Modifier
                .heightIn(min = 32.dp)
                .padding(bottom = 12.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        )
    }
}

@Composable
private fun PhoneNumberDisplay(
    modifier: Modifier = Modifier,
    phoneNumber: String,
    colors: DialPadColors,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = phoneNumber.ifEmpty { " " },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            textAlign = TextAlign.Center,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 24.sp,
                maxFontSize = 32.sp,
                stepSize = 1.sp
            ),
            fontSize = 32.sp,
            fontWeight = FontWeight.Medium,
            color = colors.primaryText,
            overflow = TextOverflow.StartEllipsis,
            softWrap = false,
            maxLines = 1
        )

        if (phoneNumber.isNotEmpty()) {
            DialPadClearButton(
                onClick = onBackspaceClick,
                onLongClick = onClearClick,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
private fun DialPadToolbar(
    colors: DialPadColors,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "",
                tint = colors.toolbarIcon
            )
        }
    }
}

@Composable
private fun DialPad(
    colors: DialPadColors,
    onDigitClick: (Char, String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DialPadKeys.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { key ->
                    DialPadKey(
                        key = key,
                        colors = colors,
                        onClick = { onDigitClick(key.tone, key.tone.toString()) },
                        onLongClick = if (key.tone == '0') {
                            { onDigitClick(key.tone, "+") }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.DialPadKey(
    key: DialPadKey,
    colors: DialPadColors,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hapticFeedback = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .weight(1f)
            .height(88.dp)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                hapticFeedbackEnabled = false,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                    onClick()
                },
                onLongClick = onLongClick?.let { longClick ->
                    {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        longClick()
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    color = colors.keyBackground,
                    shape = CircleShape
                )
                .indication(interactionSource, LocalIndication.current),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = key.tone.toString(),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.primaryText,
                    lineHeight = 30.sp,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
                if (key.letters.isNotEmpty()) {
                    Text(
                        text = key.letters,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.secondaryText,
                        lineHeight = 10.sp,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }
            }
        }
    }
}

@Composable
private fun DialPadCallButton(
    colors: DialPadColors,
    onCallClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hapticFeedback = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    color = colors.callButton,
                    shape = CircleShape
                )
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    hapticFeedbackEnabled = false,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                        onCallClick()
                    }
                )
                .indication(interactionSource, LocalIndication.current),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Call,
                contentDescription = stringResource(R.string.call),
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun DialPadClearButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hapticFeedback = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .size(32.dp)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                hapticFeedbackEnabled = false,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                    onClick()
                },
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {

        Icon(
            painter = painterResource(R.drawable.ic_eraser_themed),
            contentDescription = "Clear digit",
            tint = Color.Unspecified,
            modifier = Modifier.size(width = 32.dp, height = 24.dp)
        )
    }
}

@Composable
private fun dialPadColors(): DialPadColors {
    return if (isSystemInDarkTheme()) {
        DialPadColors(
            background = CallColors.BackgroundDark,
            keyBackground = Color(0xFF2C2C2E),
            primaryText = Color.White,
            secondaryText = Color(0xFF8A8A8E),
            toolbarIcon = Color.White,
            clearButtonBackground = Color(0xFF343437),
            clearButtonIcon = Color.White.copy(alpha = 0.72f),
            callButton = CallColors.CallAgainGreen
        )
    } else {
        DialPadColors(
            background = Color.White,
            keyBackground = Color(0xFFF3F4F8),
            primaryText = Color(0xFF111113),
            secondaryText = Color(0xFF6D7280),
            toolbarIcon = Color(0xFF111113),
            clearButtonBackground = Color(0xFFECEEF3),
            clearButtonIcon = Color(0xFF6D7280),
            callButton = CallColors.CallAgainGreen
        )
    }
}

private data class DialPadColors(
    val background: Color,
    val keyBackground: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val toolbarIcon: Color,
    val clearButtonBackground: Color,
    val clearButtonIcon: Color,
    val callButton: Color,
)

private data class DialPadKey(
    val tone: Char,
    val letters: String = "",
)

private val DialPadKeys = listOf(
    DialPadKey('1'),
    DialPadKey('2', "ABC"),
    DialPadKey('3', "DEF"),
    DialPadKey('4', "GHI"),
    DialPadKey('5', "JKL"),
    DialPadKey('6', "MNO"),
    DialPadKey('7', "PQRS"),
    DialPadKey('8', "TUV"),
    DialPadKey('9', "WXYZ"),
    DialPadKey('*'),
    DialPadKey('0', "+"),
    DialPadKey('#')
)

@Preview(
    name = "Dial Pad Light",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 393,
    heightDp = 852
)
@Composable
private fun DialPadScreenLightPreview() {
    CallTheme(darkTheme = false) {
        DialPadScreenPreviewContent()
    }
}

@Preview(
    name = "Dial Pad Dark",
    showBackground = true,
    backgroundColor = 0xFF19191B,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = 393,
    heightDp = 852
)
@Composable
private fun DialPadScreenDarkPreview() {
    CallTheme(darkTheme = true) {
        DialPadScreenPreviewContent()
    }
}

@Composable
private fun DialPadScreenPreviewContent() {
    DialPadScreen(
        initialPhoneNumber = "989-568",
        onBack = {},
        onToneClick = {},
        onCallClick = {}
    )
}

private class LastCalledNumberRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getLastCalledNumber(): String? {
        return preferences.getString(KEY_LAST_CALLED_NUMBER, null)
    }

    fun saveLastCalledNumber(number: String) {
        preferences.edit {
            putString(KEY_LAST_CALLED_NUMBER, number)
        }
    }

    private companion object {
        private const val PREF_NAME = "sceyt_call_kit_preferences"
        private const val KEY_LAST_CALLED_NUMBER = "last_called_number"
    }
}

package com.sceyt.chat.demo.call.ui.dialpad

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class DialPadActivity : ComponentActivity() {
    private val callManager: CallManager by inject()
    private var lastNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            CallTheme(darkTheme = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DialPadScreen(
                        onBack = ::finish,
                        onCallClick = { phoneNumber ->
                            makeCall(phoneNumber)
                        }
                    )
                }
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
        fun createIntent(context: Context): Intent {
            return Intent(context, DialPadActivity::class.java)
        }
    }
}

@Composable
private fun DialPadScreen(
    onBack: () -> Unit,
    onCallClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { LastCalledNumberRepository(context.applicationContext) }
    var phoneNumber by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp)
    ) {
        DialPadToolbar(onBack = onBack)

        Spacer(modifier = Modifier.weight(1f))

        PhoneNumberDisplay(phoneNumber = phoneNumber)

        Spacer(modifier = Modifier.height(40.dp))

        DialPad(
            onDigitClick = { digit ->
                phoneNumber += digit
            },
            onBackspaceClick = {
                phoneNumber = phoneNumber.dropLast(1)
            },
            onClearClick = {
                phoneNumber = ""
            },
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
                .height(32.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        )
    }
}

@Composable
private fun PhoneNumberDisplay(
    phoneNumber: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = phoneNumber,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 24.sp,
                maxFontSize = 42.sp,
                stepSize = 1.sp
            ),
            fontSize = 42.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.StartEllipsis,
            softWrap = false,
            maxLines = 1
        )
    }
}

@Composable
private fun DialPadToolbar(
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
                contentDescription = ""
            )
        }
    }
}

@Composable
private fun DialPad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit,
    onCallClick: () -> Unit,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("*", "0", "#")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { digit ->
                    DialPadKey(
                        label = digit,
                        secondaryLabel = if (digit == "0") "+" else null,
                        onClick = { onDigitClick(digit) },
                        onLongClick = if (digit == "0") {
                            { onDigitClick("+") }
                        } else null
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DialPadEmptyKey()
            DialPadCallButton(onClick = onCallClick)
            DialPadBackspaceKey(
                onClick = onBackspaceClick,
                onLongClick = onClearClick
            )
        }
    }
}

@Composable
private fun RowScope.DialPadKey(
    label: String,
    secondaryLabel: String? = null,
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
                    color = MaterialTheme.colorScheme.surfaceVariant,
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
                    text = label,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 30.sp,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
                if (secondaryLabel != null) {
                    Text(
                        text = secondaryLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.DialPadCallButton(
    onClick: () -> Unit,
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
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    color = CallColors.AccentGreen,
                    shape = CircleShape
                )
                .indication(interactionSource, LocalIndication.current),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Call,
                contentDescription = stringResource(R.string.call),
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun RowScope.DialPadEmptyKey() {
    Spacer(
        modifier = Modifier
            .weight(1f)
            .height(88.dp)
    )
}

@Composable
private fun RowScope.DialPadBackspaceKey(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
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
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .indication(interactionSource, LocalIndication.current),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp)
            )
        }
    }
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

package com.sceyt.chat.demo.call.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sceyt.chat.call.R

private val DtmfLetterColor = Color(0xFF757D8B)
private val DtmfPanelBackground = Color(0xFF353536).copy(alpha = 0.40f)
private val DtmfDividerColor = Color.White.copy(alpha = 0.10f)

private data class DtmfKey(
    val tone: Char,
    val letters: String = ""
)

private val DtmfKeys = listOf(
    DtmfKey('1'),
    DtmfKey('2', "ABC"),
    DtmfKey('3', "DEF"),
    DtmfKey('4', "GHI"),
    DtmfKey('5', "JKL"),
    DtmfKey('6', "MNO"),
    DtmfKey('7', "PQRS"),
    DtmfKey('8', "TUV"),
    DtmfKey('9', "WXYZ"),
    DtmfKey('*'),
    DtmfKey('0', "+"),
    DtmfKey('#')
)

@Composable
internal fun DtmfPanel(
    isDtmfEnabled: Boolean,
    onToneClick: (Char) -> Unit,
    modifier: Modifier = Modifier,
    digits: String = "",
) {
    var digits by rememberSaveable { mutableStateOf(digits) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DtmfPanelBackground, RoundedCornerShape(16.dp))
            .padding(top = 16.dp)
            .padding(bottom = 24.dp)
            .padding(horizontal = 18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                Text(
                    text = digits.ifEmpty { " " },
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 36.sp,
                    maxLines = 1,
                    overflow = TextOverflow.StartEllipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 64.dp)
                )

                if (digits.isNotEmpty()) {
                    DtmfClearButton(
                        onClick = { digits = digits.dropLast(1) },
                        onLongClick = { digits = "" },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DtmfDividerColor)
            )

            Spacer(modifier = Modifier.height(17.dp))

            DtmfKeypad(
                enabled = isDtmfEnabled,
                onKeyClick = { tone ->
                    digits += tone
                    onToneClick(tone)
                }
            )
        }
    }
}

@Composable
private fun DtmfKeypad(
    enabled: Boolean,
    onKeyClick: (Char) -> Unit
) {
    Column(
        modifier = Modifier
            .width(264.dp)
            .height(280.dp)
    ) {
        DtmfKeys.chunked(3).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
            ) {
                row.forEach { key ->
                    DtmfKeyButton(
                        key = key,
                        enabled = enabled,
                        onClick = { onKeyClick(key.tone) },
                        modifier = Modifier
                            .weight(1f)
                            .height(70.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DtmfKeyButton(
    key: DtmfKey,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .size(64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = key.tone.toString(),
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 32.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = key.letters,
                color = if (enabled) DtmfLetterColor else DtmfLetterColor.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DtmfClearButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_eraser),
            contentDescription = "Clear digit",
            tint = Color.Unspecified,
            modifier = Modifier.size(width = 32.dp, height = 24.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF19191B)
@Composable
private fun DtmfPanelPreview() {
    DtmfPanel(
        isDtmfEnabled = true,
        digits = "213",
        onToneClick = {}
    )
}

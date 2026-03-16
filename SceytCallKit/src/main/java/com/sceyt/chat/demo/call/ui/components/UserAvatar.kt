package com.sceyt.chat.demo.call.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * User avatar with fallback to initials.
 */
@Composable
fun UserAvatar(
    modifier: Modifier = Modifier,
    avatarUrl: String? = "",
    name: String = "John Doe",
) {
    if (!avatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = "Avatar of $name",
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Image(
            painter = painterResource(com.sceyt.chatuikit.R.drawable.sceyt_ic_default_avatar),
            contentDescription = "Default avatar",
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
@Preview
fun UserAvatarWithOuter(
    avatarUrl: String? = "",
    name: String = "John Doe",
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth(0.5f)
            .aspectRatio(1f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(0.12f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )
        UserAvatar(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            avatarUrl = avatarUrl,
            name = name,
        )
    }
}
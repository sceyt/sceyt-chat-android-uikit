package com.sceyt.chat.connection

import com.google.gson.JsonParser
import kotlin.io.encoding.Base64

internal fun String.jwtExpirationEpochSeconds(): Long? = runCatching {
    val parts = split('.')
    if (parts.size != JWT_PART_COUNT) return null

    val payload = Base64.UrlSafe
        .withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
        .decode(parts[1])
        .toString(Charsets.UTF_8)
    val expiration = JsonParser.parseString(payload)
        .asJsonObject
        .get(EXPIRATION_CLAIM)
        ?.asLong

    expiration?.takeIf { it > 0L }
}.getOrNull()

private const val JWT_PART_COUNT = 3
private const val EXPIRATION_CLAIM = "exp"

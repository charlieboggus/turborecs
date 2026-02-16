package com.github.charlieboggus.turborecs.error

class ClaudeApiException(
    message: String,
    val statusCode: Int?,
    val responseBody: String?
) : RuntimeException(message)
package com.github.charlieboggus.turborecs.error

data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String?
)
package com.github.charlieboggus.turborecs.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("turborecs.auth")
data class AuthProperties(
    val enabled: Boolean,
    val username: String,
    val passwordHash: String
)
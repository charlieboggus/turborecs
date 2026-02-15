package com.github.charlieboggus.turborecs.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "turborecs.security")
data class SecurityProperties(
    val internalNetworkAuthToken: String
)
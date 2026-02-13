package com.github.charlieboggus.turborecs.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.Resource

@ConfigurationProperties(prefix = "turborecs.import.letterboxd")
data class ImportProperties(
    val enabled: Boolean = false,
    val path: Resource,
    val enrich: Boolean = true,
    val tag: Boolean = false,
    val limit: Int = 10_000,
    val dryRun: Boolean = false
)
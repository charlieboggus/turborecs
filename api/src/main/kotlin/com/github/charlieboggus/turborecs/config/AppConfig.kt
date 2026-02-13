package com.github.charlieboggus.turborecs.config

import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import com.github.charlieboggus.turborecs.config.properties.ImportProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(
    ClaudeProperties::class,
    ImportProperties::class
)
class AppConfig
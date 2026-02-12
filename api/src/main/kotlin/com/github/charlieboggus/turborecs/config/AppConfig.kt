package com.github.charlieboggus.turborecs.config

import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(ClaudeProperties::class)
class AppConfig
package com.github.charlieboggus.turborecs.config

import com.github.charlieboggus.turborecs.config.properties.AuthProperties
import com.github.charlieboggus.turborecs.config.properties.ClaudeProperties
import com.github.charlieboggus.turborecs.config.properties.OpenLibraryProperties
import com.github.charlieboggus.turborecs.config.properties.TmdbProperties

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(
    AuthProperties::class,
    ClaudeProperties::class,
    TmdbProperties::class,
    OpenLibraryProperties::class
)
class AppConfig
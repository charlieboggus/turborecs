package com.github.charlieboggus.turborecs.controller

import com.github.charlieboggus.turborecs.config.properties.AuthProperties
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Base64

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authProperties: AuthProperties
) {

    private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()

    @GetMapping("/verify")
    fun verifyAuthentication(
        @RequestHeader("Authorization") authHeader: String?
    ): ResponseEntity<Void> {
        if (!authProperties.enabled) {
            return ResponseEntity.ok().build()
        }
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
        return try {
            val decoded = String(Base64.getDecoder().decode(authHeader.removePrefix("Basic ")))
            val (username, password) = decoded.split(":", limit = 2)
            if (username == authProperties.username &&
                passwordEncoder.matches(password, authProperties.passwordHash)) {
                ResponseEntity.ok().build()
            }
            else {
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
            }
        }
        catch (_: Exception) {
            ResponseEntity.badRequest().build()
        }
    }
}
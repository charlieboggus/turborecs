package com.github.charlieboggus.turborecs.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class InternalAuthFilter(
    @Value("\${turborecs.security.auth-enabled}") private val authEnabled: Boolean,
    @Value("\${turborecs.security.internal-auth-token}") private val internalAuthToken: String
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return !authEnabled
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (!authEnabled) {
            return
        }
        val path = request.requestURI ?: ""
        if (path.startsWith("/api/")) {
            if (internalAuthToken.isBlank()) {
                response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_AUTH_TOKEN not configured")
                return
            }
            val provided = request.getHeader("X-Internal-Auth")?.trim().orEmpty()
            if (provided != internalAuthToken) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing/invalid internal auth")
                return
            }
        }
        filterChain.doFilter(request, response)
    }
}
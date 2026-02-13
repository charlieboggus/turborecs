package com.github.charlieboggus.turborecs.web.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class InternalAuthFilter(
    @Value("\${turborecs.security.internal-token:}") private val internalToken: String,
    @Value("\${turborecs.security.admin-token:}") private val adminToken: String,
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI ?: ""
        // Allow basic health checks if you want
        return path == "/actuator/health"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI ?: ""
        if (path.startsWith("/api/")) {
            if (internalToken.isBlank()) {
                response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_AUTH_TOKEN not configured")
                return
            }
            val provided = request.getHeader("X-Internal-Auth")?.trim().orEmpty()
            if (provided != internalToken) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing/invalid internal auth")
                return
            }
            // Extra lock for expensive/admin routes
            if (path.startsWith("/api/tagging/") || path.startsWith("/api/enrich/")) {
                if (adminToken.isBlank()) {
                    response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "ADMIN_AUTH_TOKEN not configured")
                    return
                }
                val adminProvided = request.getHeader("X-Admin-Auth")?.trim().orEmpty()
                if (adminProvided != adminToken) {
                    response.sendError(HttpStatus.FORBIDDEN.value(), "Missing/invalid admin auth")
                    return
                }
            }
        }
        filterChain.doFilter(request, response)
    }
}
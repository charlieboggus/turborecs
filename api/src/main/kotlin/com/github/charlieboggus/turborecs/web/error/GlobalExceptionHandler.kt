package com.github.charlieboggus.turborecs.web.error

import com.github.charlieboggus.turborecs.service.ClaudeApiException
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(ex: NoSuchElementException, request: HttpServletRequest): ResponseEntity<ApiError> =
        buildError(HttpStatus.NOT_FOUND, ex.message, request)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException, request: HttpServletRequest): ResponseEntity<ApiError> =
        buildError(HttpStatus.BAD_REQUEST, ex.message, request)

    /**
     * Bean validation on @RequestBody DTOs
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException, request: HttpServletRequest): ResponseEntity<ApiError> {
        val message = ex.bindingResult
            .fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
            .ifBlank { "Validation failed" }

        return buildError(HttpStatus.BAD_REQUEST, message, request)
    }

    /**
     * Bean validation on @RequestParam / @PathVariable (when you use @Validated on controller)
     */
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException, request: HttpServletRequest): ResponseEntity<ApiError> {
        val message = ex.constraintViolations
            .joinToString("; ") { v ->
                val field = v.propertyPath?.toString()?.substringAfterLast('.') ?: "param"
                "$field: ${v.message}"
            }
            .ifBlank { "Validation failed" }

        return buildError(HttpStatus.BAD_REQUEST, message, request)
    }

    /**
     * Query param enum parsing failures, UUID parsing, etc.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException, request: HttpServletRequest): ResponseEntity<ApiError> {
        val msg = "Invalid value for '${ex.name}': '${ex.value}'"
        return buildError(HttpStatus.BAD_REQUEST, msg, request)
    }

    /**
     * Unique constraint violations, FK issues, etc.
     */
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleConflict(ex: DataIntegrityViolationException, request: HttpServletRequest): ResponseEntity<ApiError> {
        // Don't leak DB internals; log details server-side.
        log.warn("DataIntegrityViolation at {}: {}", request.requestURI, ex.message)
        return buildError(HttpStatus.CONFLICT, "Data integrity violation", request)
    }

    /**
     * Upstream Claude failures => 502 Bad Gateway
     */
    @ExceptionHandler(ClaudeApiException::class)
    fun handleClaude(ex: ClaudeApiException, request: HttpServletRequest): ResponseEntity<ApiError> {
        log.warn("ClaudeApiException at {}: {}", request.requestURI, ex.message)
        return buildError(HttpStatus.BAD_GATEWAY, ex.message ?: "Upstream dependency error", request)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception, request: HttpServletRequest): ResponseEntity<ApiError> {
        log.error("Unhandled exception at ${request.requestURI}", ex)
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request)
    }

    private fun buildError(
        status: HttpStatus,
        message: String?,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        val error = ApiError(
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = request.requestURI
        )
        return ResponseEntity.status(status).body(error)
    }
}
package com.github.charlieboggus.turborecs.web.error

import jakarta.servlet.http.HttpServletRequest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.*

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(
        ex: NoSuchElementException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return buildError(HttpStatus.NOT_FOUND, ex.message, request)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(
        ex: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return buildError(HttpStatus.BAD_REQUEST, ex.message, request)
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleConflict(
        ex: DataIntegrityViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return buildError(HttpStatus.CONFLICT, "Data integrity violation", request)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {

        val validationErrors = ex.bindingResult
            .allErrors
            .mapNotNull { error ->
                if (error is FieldError) {
                    "${error.field}: ${error.defaultMessage}"
                } else {
                    error.defaultMessage
                }
            }
            .joinToString("; ")

        return buildError(HttpStatus.BAD_REQUEST, validationErrors, request)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        return buildError(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred",
            request
        )
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
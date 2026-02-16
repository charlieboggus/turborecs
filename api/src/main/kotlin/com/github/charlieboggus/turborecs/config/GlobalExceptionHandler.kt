package com.github.charlieboggus.turborecs.config

import com.github.charlieboggus.turborecs.error.ClaudeApiException
import com.github.charlieboggus.turborecs.error.ErrorResponse
import com.github.charlieboggus.turborecs.error.ItemAlreadyExistsException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(e: NoSuchElementException) = respond(HttpStatus.NOT_FOUND, e.message)

    @ExceptionHandler(ItemAlreadyExistsException::class)
    fun handleConflict(e: ItemAlreadyExistsException) = respond(HttpStatus.CONFLICT, e.message)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException) = respond(HttpStatus.BAD_REQUEST, e.message)

    @ExceptionHandler(ClaudeApiException::class)
    fun handleClaudeError(e: ClaudeApiException): ResponseEntity<ErrorResponse> {
        log.error("Claude API error: {}", e.message)
        return respond(HttpStatus.BAD_GATEWAY, "AI service error")
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception", e)
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong")
    }

    private fun respond(status: HttpStatus, message: String?) =
        ResponseEntity.status(status).body(
            ErrorResponse(
                status = status.value(),
                error = status.reasonPhrase,
                message = message
            )
        )
}
package com.github.charlieboggus.turborecs.web.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class RateItemRequest(
    @field:NotNull(message = "rating is required")
    @field:Min(1, message = "rating must be between 1 and 5")
    @field:Max(5, message = "rating must be between 1 and 5")
    val rating: Int
)
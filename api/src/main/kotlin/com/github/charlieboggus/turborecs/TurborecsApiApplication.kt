package com.github.charlieboggus.turborecs

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class TurborecsApiApplication

fun main(args: Array<String>) {
	runApplication<TurborecsApiApplication>(*args)
}

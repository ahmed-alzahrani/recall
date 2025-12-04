package com.recall.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RecallBackendApplication

fun main(args: Array<String>) {
	runApplication<RecallBackendApplication>(*args)
}

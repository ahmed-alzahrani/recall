package com.recall.backend.annotations

import com.recall.backend.config.FlywayConfig
import org.junit.jupiter.api.Assertions.*
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace
import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(FlywayConfig::class)
annotation class DatabaseTest

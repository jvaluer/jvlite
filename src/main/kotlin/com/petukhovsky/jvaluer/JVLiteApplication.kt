package com.petukhovsky.jvaluer

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.crypto.password.StandardPasswordEncoder

@SpringBootApplication
open class JVLiteApplication

fun main(args: Array<String>) {
    SpringApplication.run(JVLiteApplication::class.java, *args)
}

@Bean
fun passwordEncoder(): PasswordEncoder {
    return StandardPasswordEncoder()
}
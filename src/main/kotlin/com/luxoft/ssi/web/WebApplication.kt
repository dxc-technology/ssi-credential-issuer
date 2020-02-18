package com.luxoft.ssi.web

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class WebApplication

fun main(args: Array<String>) {
    SpringApplication.run(WebApplication::class.java, *args)
}

package com.serco.dias.streamsMaster

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ImportResource

@SpringBootApplication
@ImportResource("classpath:general-route.xml")
open class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}


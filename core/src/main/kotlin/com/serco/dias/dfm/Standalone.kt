package com.serco.dias.dfm

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource

@SpringBootApplication
@ImportResource("classpath:general-route.xml", "classpath:standalone.xml")
@ComponentScan(basePackages = ["com.serco.dias.dfm"])
class Standalone

fun main(args: Array<String>) {
    runApplication<Standalone>(*args)
}


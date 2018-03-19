package com.serco.dias.dfm.download

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.Output
import org.springframework.messaging.MessageChannel

@SpringBootApplication @EnableBinding(ListChannels::class)
class ListApplication

fun main(args: Array<String>) {
    runApplication<ListApplication>(*args)
}

interface ListChannels {
    @Input("list") fun list(): MessageChannel
    @Output("downloadQueue") fun completed(): MessageChannel
}

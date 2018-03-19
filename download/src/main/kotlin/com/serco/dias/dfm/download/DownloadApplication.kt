package com.serco.dias.dfm.download

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Input
import org.springframework.cloud.stream.annotation.Output
import org.springframework.messaging.MessageChannel

@SpringBootApplication @EnableBinding(DownloadChannels::class)
class DownloadApplication

fun main(args: Array<String>) {
    runApplication<DownloadApplication>(*args)
}

interface DownloadChannels {
    @Input("downloadQueue") fun list(): MessageChannel
    @Output("completed") fun completed(): MessageChannel
}

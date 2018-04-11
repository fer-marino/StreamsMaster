package com.serco.dias.dfm.download

import com.serco.dias.dfm.model.Product
import org.reactivestreams.Publisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Output
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Processor
import org.springframework.cloud.stream.reactive.StreamEmitter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ImportResource
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.GenericMessage

@SpringBootApplication
@EnableBinding(Processor::class)
@ImportResource("classpath:general-route.xml", "classpath:download.xml")
@ComponentScan(basePackages = ["com.serco.dias.dfm"])
class DownloadApplication {
    @Autowired
    @Qualifier("downloadQueue")
    lateinit var downloadQueue: MessageChannel

    @StreamListener(Processor.INPUT)
    fun forwardInput(m: Product) {
        downloadQueue.send(GenericMessage(m))
    }

    @StreamEmitter
    @Output(Processor.OUTPUT)
    @Bean
    fun forwardOutput(): Publisher<Message<Product>> = IntegrationFlows.from("completed").bridge().toReactivePublisher<Product>()


}

fun main(args: Array<String>) {
    runApplication<DownloadApplication>(*args)
}


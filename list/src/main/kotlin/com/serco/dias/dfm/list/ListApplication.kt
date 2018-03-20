package com.serco.dias.dfm.list

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
import org.springframework.integration.annotation.BridgeFrom
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.GenericMessage

@SpringBootApplication @EnableBinding(Processor::class) @ImportResource("classpath:general-route.xml", "classpath:list.xml")
@ComponentScan(basePackages = ["com.serco.dias.dfm"])
class ListApplication {
    @Autowired @Qualifier("list")
    lateinit var listChannel: MessageChannel

    @StreamListener(Processor.INPUT)
    fun forwardInput(m: Any) {
        listChannel.send(GenericMessage(m))
    }

    @StreamEmitter @Output(Processor.OUTPUT) @Bean
    fun forwardOutput(): Publisher<Message<Product>> = IntegrationFlows.from("listResult").bridge().toReactivePublisher<Product>()

}

fun main(args: Array<String>) {
    runApplication<ListApplication>(*args)
}


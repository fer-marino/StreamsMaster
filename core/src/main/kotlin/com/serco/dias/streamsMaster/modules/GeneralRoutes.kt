package com.serco.dias.streamsMaster.modules

import com.serco.dias.streamsMaster.config.Config
import com.serco.dias.streamsMaster.model.Center
import com.serco.dias.streamsMaster.model.Product
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.task.TaskExecutor
import org.springframework.integration.redis.metadata.RedisMetadataStore
import org.springframework.messaging.MessageHandlingException
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.handler.annotation.Headers
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.time.LocalDateTime
import java.time.ZoneOffset


@Component
class GeneralRoutes {
    @Autowired private lateinit var modules: Map<String, StreamModule>
    @Autowired private lateinit var metadataStore: RedisMetadataStore
    @Autowired private lateinit var config: Config

    fun list(): Flux<Product> = modules[config.center.type + "Module"]!!.list(config.center)

    fun download(product: Product) = modules[product.downloadCenter.type + "Module"]!!.download(product)

    fun finalize(product: Product) {
        modules[product.downloadCenter.type + "Module"]!!.finalize(product)
    }

    fun storeRedis(payload: Product, headers: MessageHeaders): Product {
        metadataStore.put(payload.name, LocalDateTime.now().plusDays(60).toEpochSecond(ZoneOffset.UTC).toString())
        return payload
    }

    fun errorHandler(@Payload payload: MessageHandlingException, @Headers headers: MessageHeaders): Product {
        println(payload.cause?.message)
        return payload.failedMessage!!.payload as Product
    }
}
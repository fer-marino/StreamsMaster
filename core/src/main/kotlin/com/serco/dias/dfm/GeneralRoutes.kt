package com.serco.dias.dfm

import com.serco.dias.dfm.model.Center
import com.serco.dias.dfm.model.Product
import com.serco.dias.dfm.modules.AbstractModule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.integration.redis.metadata.RedisMetadataStore
import org.springframework.messaging.MessageHandlingException
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.handler.annotation.Headers
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.io.IOException
import java.util.logging.Logger


@Component
class GeneralRoutes {
    val log get() = Logger.getLogger(this.javaClass.canonicalName)!!
    @Autowired
    private lateinit var modules: Map<String, AbstractModule>
    @Autowired
    private lateinit var metadataStore: RedisMetadataStore
    @Autowired
    private lateinit var redisSerializer: RedisSerializer<Product>

    fun list(center: Center): Flux<Product> = modules[center.type.toLowerCase() + "Module"]!!.list(center)

    fun download(product: Product) {
        log.info("Download started for ${product.name}")
        modules[product.downloadCenter.type.toLowerCase() + "Module"]!!.download(product)
        log.info("Download complete for ${product.name} in ${product.downloadStop!!.time - product.downloadStart!!.time} msec")
    }

    fun finalize(product: Product): Product = modules[product.downloadCenter.type + "Module"]!!.finalize(product)

    fun storeRedis(payload: Product, headers: MessageHeaders): Product {
        metadataStore.put(payload.name, redisSerializer.serialize(payload).toString())
        return payload
    }

    //    @Scheduled(fixedDelay = 6000000)
    fun metadataReaper() {
        TODO("Implement cron that delete older metadata from redis")
    }

    fun errorHandler(@Payload payload: MessageHandlingException, @Headers headers: MessageHeaders): Product {
        headers["reattempt"] = payload.mostSpecificCause is IOException
        payload.printStackTrace()
        log.warning(payload.message)
        log.warning(payload.cause?.message)
        return payload.failedMessage!!.payload as Product
    }

}
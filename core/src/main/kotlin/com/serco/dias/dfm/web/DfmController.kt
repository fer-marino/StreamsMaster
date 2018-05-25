package com.serco.dias.dfm.web

import com.serco.dias.dfm.GeneralRoutes
import com.serco.dias.dfm.config.Config
import com.serco.dias.dfm.model.DownloadTransaction
import com.serco.dias.dfm.model.Product
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.integration.redis.channel.SubscribableRedisChannel
import org.springframework.integration.redis.metadata.RedisMetadataStore
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@Profile("embeddedRedis")
class DfmController {
    @Autowired
    lateinit var generalRoutes: GeneralRoutes
    @Autowired
    lateinit var config: Config
    @Autowired
    lateinit var ltaService: LtaService
    @Autowired
    lateinit var metadataStore: RedisMetadataStore
    @Autowired
    lateinit var redisSerializer: RedisSerializer<Product>

    @Autowired
    @Qualifier("downloadQueue")
    lateinit var downloadQueue: SubscribableRedisChannel

    @RequestMapping("/queueSize", method = [RequestMethod.GET])
    fun getDownloadQueueSize() = ResponseEntity(downloadQueue.sendCount, HttpStatus.OK)

    @RequestMapping("/{centerId}/{productName}/download", method = [RequestMethod.POST, RequestMethod.GET])
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun downloadProduct(@PathVariable("productName") productName: String, @PathVariable("centerId") centerId: String): ResponseEntity<String> {
        val center = config.center.find { it.name == centerId }
        return if (center == null) {
            ResponseEntity("Center with id $centerId not found", HttpStatus.NOT_FOUND)
        } else {
            generalRoutes.download(Product(name = productName, downloadCenter = center, enqueued = Date()))

            ResponseEntity("Product $productName added to the queue", HttpStatus.ACCEPTED)
        }
    }

    @RequestMapping("/{centerId}/list", method = [RequestMethod.GET, RequestMethod.POST])
    @ResponseBody
    fun runList(@PathVariable("centerId") centerId: String): ResponseEntity<List<Product>> {
        val center = config.center.find { it.name == centerId }
        return if (center == null)
            ResponseEntity(listOf(), HttpStatus.NOT_FOUND)
        else
            ResponseEntity(generalRoutes.list(center).collectList().block(), HttpStatus.OK)
    }

    @RequestMapping("/{centerId}/{productName}", method = [RequestMethod.POST, RequestMethod.GET])
    fun status(@PathVariable("productName") productName: String, @PathVariable("centerId") centerId: String): ResponseEntity<DownloadTransaction> =
            if (metadataStore[productName] != null) {
                ResponseEntity(DownloadTransaction(redisSerializer.deserialize(metadataStore[productName].toByteArray())!!), HttpStatus.OK)
            } else {
                val ltaTransaction = ltaService.transactions.find { it.product == productName }
                if (ltaTransaction != null) {
                    ltaService.status(ltaTransaction)

                    ResponseEntity(ltaTransaction, HttpStatus.OK)
                } else
                    ResponseEntity(HttpStatus.NOT_FOUND)
            }


    @RequestMapping("/{centerId}/{productName}/center/download", method = [RequestMethod.POST, RequestMethod.GET])
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun downloadProductLta(@PathVariable("productName") productName: String, @PathVariable("centerId") centerId: String): ResponseEntity<DownloadTransaction> {
        val center = config.center.find { it.name == centerId }

        return if (center?.lta == null)
            ResponseEntity(HttpStatus.NOT_FOUND)
        else
            ResponseEntity(ltaService.retrieve(center.lta!!, productName), HttpStatus.OK)
    }
}
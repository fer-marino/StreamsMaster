package com.serco.dias.dfm.modules

import com.serco.dias.dfm.model.Center
import com.serco.dias.dfm.model.Product
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.toFlux
import java.util.*

@Component
class Sentinel1Module : NGeoModule() {
    @Autowired
    private lateinit var restBuilder: RestTemplateBuilder

    override fun list(center: Center): Flux<Product> {
        log.info("Starting Sentinel 1 list for ${center.name}")
        val restTemplate = restBuilder.basicAuthorization(center.options!!["username"], center.options!!["password"])
                .setConnectTimeout(center.options!!["connectionTimeout"]!!.toInt())
                .setReadTimeout(center.options!!["readTimeout"]!!.toInt())
                .build()
        val tdacListUrl = "/tdac/service/products"
        val headers = HttpHeaders().apply {
            accept = Collections.singletonList(MediaType.APPLICATION_JSON)
        }
        val entity = HttpEntity("parameters", headers)
        val ris = restTemplate.exchange("http://" + center.address + tdacListUrl, HttpMethod.GET, entity, Map::class.java)
        val products = ris.body!!["products"] as List<Map<String, Any>>
        log.info("Found ${products.size} new products from ${center.name}")
        return products.map { Product(it["product_name"].toString(), Date(), center) }.toFlux()
    }

}
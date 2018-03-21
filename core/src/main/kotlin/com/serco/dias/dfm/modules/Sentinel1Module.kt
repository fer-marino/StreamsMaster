package com.serco.dias.dfm.modules

import com.serco.dias.dfm.model.Center
import com.serco.dias.dfm.model.Product
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.nio.file.Path
import com.oracle.util.Checksums.update
import jdk.nashorn.internal.objects.NativeArray.forEach
import org.springframework.http.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.*
import java.util.Collections.singletonList
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.toFlux
import java.nio.file.Paths


@Component
class Sentinel1Module: NGeoModule() {
    @Autowired private lateinit var restBuilder: RestTemplateBuilder

    override fun list(center: Center): Flux<Product> {
        val restTemplate = restBuilder.basicAuthorization(center.options!!["username"], center.options!!["password"]).build()
        val tdacListUrl = "/tdac/service/products"
        val headers = HttpHeaders().apply {
            accept = Collections.singletonList(MediaType.APPLICATION_JSON)
        }
        val entity = HttpEntity("parameters", headers)
        val ris = restTemplate.exchange("http://" + center.address + tdacListUrl, HttpMethod.GET, entity, Map::class.java)
        val products = ris.body!!["products"] as List<Map<String, Any>>

        return products.map { Product(it["product_name"].toString(), Date(), center) }.toFlux()
    }

}
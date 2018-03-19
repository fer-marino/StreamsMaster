package com.serco.dias.streamsMaster.modules

import com.serco.dias.streamsMaster.model.Center
import com.serco.dias.streamsMaster.model.Product
import reactor.core.publisher.Flux
import java.util.logging.Logger

interface StreamModule {
    val log get() = Logger.getLogger(this.javaClass.canonicalName)!!

    fun list(center: Center): Flux<Product>
    fun download(product: Product): Product
    fun finalize(product: Product): Product

}
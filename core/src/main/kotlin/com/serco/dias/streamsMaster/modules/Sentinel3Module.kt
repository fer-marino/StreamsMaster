package com.serco.dias.streamsMaster.modules

import com.serco.dias.streamsMaster.model.Center
import com.serco.dias.streamsMaster.model.Product
import reactor.core.publisher.Flux

class Sentinel3Module: StreamModule {
    override fun list(center: Center): Flux<Product> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun download(product: Product): Product {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun finalize(product: Product): Product {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
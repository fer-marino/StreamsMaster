package com.serco.dias.dfm.modules

import com.serco.dias.dfm.config.Config
import com.serco.dias.dfm.model.Center
import com.serco.dias.dfm.model.Product
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.nio.file.Path
import org.apache.commons.io.FileUtils
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.toFlux
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.io.IOException




@Component
class PupModule: StreamModule() {

    override fun list(center: Center): Flux<Product> =
         Files.walk(Paths.get(center.address), 1)
                .filter({ f -> Files.isDirectory(f) && f != Paths.get(center.address) && !f.fileName.startsWith(".") })
                .map { Product(it.fileName.toString(), Date(), center, size = FileUtils.sizeOfDirectory(it.toFile())) }
                .toFlux()

    override fun download(product: Product): Product {
        val source = Paths.get(product.downloadCenter.address, product.name)
        product.downloadStart = Date()
        try {
            Files.createSymbolicLink(Paths.get(config.tmpDir, product.name), source)
        } catch (e: IOException) {
            log.warning("Fall back to copy as symlink is not supported: " + e.message)
            FileUtils.copyDirectory(source.toFile(), Paths.get(config.tmpDir, product.name).toFile())
        }
        product.downloadStop = Date()
        return product
    }

}
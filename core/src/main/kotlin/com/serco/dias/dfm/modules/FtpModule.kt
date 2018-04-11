package com.serco.dias.dfm.modules

import com.serco.dias.dfm.model.Center
import com.serco.dias.dfm.model.Product
import org.apache.commons.io.FileUtils
import org.apache.commons.io.input.CountingInputStream
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.toFlux
import java.io.IOException
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Callable

@Component
class FtpModule : StreamModule() {
    override fun list(center: Center): Flux<Product> =
            Flux.using(FtpListSupplier(center), FtpListSourceSupplier(), FtpSupplierCleanup())

    override fun download(product: Product): Product {
        val f = FTPClient()
        val center = product.downloadCenter
        try {
            f.connect(center.address, center.options!!["port"].toString().toInt())
            if (center.options!!["passiveMode"]!!.toBoolean())
                f.enterLocalPassiveMode()
            val username = center.options!!["username"].toString()
            val password = center.options!!["password"].toString()
            f.login(username, password)
            f.setFileType(FTP.BINARY_FILE_TYPE)
            f.changeWorkingDirectory("/${center.options!!["baseDir"]}")

            val ins = CountingInputStream(f.retrieveFileStream(product.metadata!!["path"].toString()))
            product.downloadStart = Date()
            meterUtils.monitorDownload(ins, center,
                    { FileUtils.copyInputStreamToFile(ins, Paths.get(config.tmpDir, "${product.name}.zip").toFile()) })
            product.downloadStop = Date()
            return product
        } catch (e: IOException) {
            throw IOException("Error occurred during product list for center ${center.name}: ${e.message}", e)
        } finally {
            f.logout()
        }
    }

    private class FtpListSourceSupplier : java.util.function.Function<Iterator<Product>, Publisher<Product>> {
        override fun apply(t: Iterator<Product>): Publisher<Product> = t.toFlux()
    }

    private class FtpListSupplier(var center: Center) : Callable<Iterator<Product>> {

        override fun call(): Iterator<Product> {

            try {


                return object : Iterator<Product> {
                    var started = false
                    val f = FTPClient()
                    fun init() {
                        f.connect(center.address, center.options!!["port"].toString().toInt())
                        if (center.options!!["passiveMode"]!!.toBoolean())
                            f.enterLocalPassiveMode()
                        val username = center.options!!["username"].toString()
                        val password = center.options!!["password"].toString()
                        f.login(username, password)
                        f.changeWorkingDirectory("/${center.options!!["baseDir"]}")

                        queue.addAll(f.listNames().toMutableList())
                    }

                    val queue = mutableListOf<String>()

                    override fun hasNext(): Boolean {
                        if (!started) init()
                        return if (queue.isNotEmpty()) {
                            true
                        } else {
                            f.logout()
                            false
                        }
                    }

                    override fun next(): Product = next("/${center.options!!["baseDir"]}")

                    private fun next(baseDir: String): Product {
                        if (!started) init()

                        if (queue.isEmpty()) throw NoSuchElementException("No more products")

                        var dept = -1
                        baseDir.forEach { if (it == '/') dept++ }

                        val elem = queue.removeAt(0)
                        if (elem.matches(Regex(center.filter!!)))
                            return Product(elem, enqueued = Date(), downloadCenter = center, metadata = mutableMapOf("path" to "$baseDir/$elem"))

                        if (dept <= center.options!!["maxDept"]!!.toInt()) {
                            val tmp = f.listNames("$baseDir/$elem")
                            if (tmp != null)
                                queue.addAll(tmp)
                        }

                        return next("$baseDir/$elem/")
                    }
                }

            } catch (e: IOException) {
                throw IOException("Error occurred during product list for center ${center.name}: ${e.message}", e)
            }
        }

    }

    private class FtpSupplierCleanup : java.util.function.Consumer<Iterator<Product>> {
        override fun accept(t: Iterator<Product>) {

        }
    }
}
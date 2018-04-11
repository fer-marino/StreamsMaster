package com.serco.dias.dfm.modules

import com.fasterxml.jackson.annotation.JsonProperty
import com.serco.dias.dfm.model.Center
import com.serco.dias.dfm.model.Product
import org.apache.commons.io.FileUtils
import org.apache.commons.io.input.CountingInputStream
import org.reactivestreams.Publisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.Base64Utils
import org.springframework.web.client.HttpStatusCodeException
import reactor.core.publisher.Flux
import reactor.core.publisher.toFlux
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Callable
import java.util.logging.Logger
import java.util.zip.ZipInputStream

@Component
class DhusModule : StreamModule() {
    @Autowired
    lateinit var restTemplateBuilder: RestTemplateBuilder

    override fun list(center: Center): Flux<Product> = Flux.using(DhusListSupplier(center, restTemplateBuilder), DhusListSourceSupplier(), DhusSupplierCleanup())

    override fun download(product: Product): Product {
        product.downloadStart = Date()
        downloadProduct(product.metadata!!["uuid"].toString(), product.name, product.downloadCenter, config.tmpDir)
        unzip(product.name, config.tmpDir, product.downloadCenter.destination)
        FileUtils.deleteDirectory(File(config.tmpDir + File.separator + product.name))
        product.downloadStop = Date()
        return product
    }

    private fun downloadProduct(id: String, productName: String, center: Center, destination: String) {
        val uc: HttpURLConnection = URL("${center.address}/odata/v1/Products('$id')/\$value").openConnection() as HttpURLConnection
        val basicAuth = "Basic " + Base64Utils.encodeToString("${center.options!!["username"]}:${center.options!!["password"]}".toByteArray())
        uc.setRequestProperty("Authorization", basicAuth)
        try {
            val stream = CountingInputStream(uc.inputStream)

            meterUtils.monitorDownload(stream, center,
                    { FileUtils.copyInputStreamToFile(stream, Paths.get(destination, "$productName.zip").toFile()) }
            )
        } catch (e: IOException) {
            val os = ByteArrayOutputStream()
            uc.errorStream.copyTo(os)
            if (os.toString().isNotBlank())
                throw IOException(os.toString().substringBefore("</message>").substringAfter("<message xml:lang=\"en\">") + " for product $productName")
            else
                throw e
        }
    }

    private fun unzip(product: String, source: String, destination: String, notDelete: Boolean = false) {
        log.info("Unzipping $product...")
        val buffer = ByteArray(1024)
        val zis = ZipInputStream(FileInputStream("$source/$product.zip"))
        var zipEntry = zis.nextEntry
        while (zipEntry != null) {
            if (zipEntry.isDirectory && !Files.exists(Paths.get("$destination/${zipEntry.name}"))) {
                Files.createDirectories(Paths.get("$destination/${zipEntry.name}"))
            } else {
                val fos = FileOutputStream(File("$destination/${zipEntry.name}"))
                var len: Int = zis.read(buffer)
                while (len > 0) {
                    fos.write(buffer, 0, len)
                    len = zis.read(buffer)
                }
                fos.close()
            }
            zipEntry = zis.nextEntry
        }
        zis.closeEntry()
        zis.close()

        if (!notDelete)
            Files.delete(Paths.get("$source/$product.zip"))

    }

    private class DhusSupplierCleanup : java.util.function.Consumer<Iterator<Product>> {
        override fun accept(t: Iterator<Product>) {

        }
    }

    private class DhusListSourceSupplier : java.util.function.Function<Iterator<Product>, Publisher<Product>> {
        override fun apply(t: Iterator<Product>): Publisher<Product> = t.toFlux()
    }

    private class DhusListSupplier(val center: Center, restTemplateBuilder: RestTemplateBuilder) : Callable<Iterator<Product>> {
        private var skip = 0
        private val pageSize = 100

        private var log = Logger.getLogger(DhusListSupplier::class.qualifiedName)
        private val tpl = restTemplateBuilder.basicAuthorization(center.options!!["username"].toString(), center.options!!["password"].toString()).build()

        // TODO lastBatch and hasNext shall be thread safe?
        private val lastBatch = mutableListOf<Product>()

        private var hasNext = true

        override fun call(): Iterator<Product> {
            if (lastBatch.isEmpty() && hasNext)
                nextBatch()

            return object : Iterator<Product> {
                var index = 0

                override fun hasNext(): Boolean = hasNext

                override fun next(): Product {
                    if (hasNext && index >= lastBatch.size) {
                        nextBatch()
                        index = 0
                    }

                    return lastBatch[index++]
                }
            }


        }

        private fun nextBatch() {
            try {
                lastBatch.clear()
                val start = System.currentTimeMillis()
                val headers = HttpHeaders()
                headers.accept = listOf(MediaType.APPLICATION_JSON)
                val entity = HttpEntity("parameters", headers)
                val filter = center.options!!["filter"].toString().replace("#ingestionDate", center.options!!["lastIngestionDate"].toString())
                val query = "${center.address}/odata/v1/Products?\$filter=$filter" +
                        "&\$orderby=${center.options!!["orderBy"]}&\$skip=$skip&\$top=$pageSize"

                log.info("Running query $query")
                val response = tpl.exchange(query, HttpMethod.GET, entity, ODataRoot::class.java)

                log.info("Returned ${response.body!!.d.results.size} products in ${System.currentTimeMillis() - start}msec")
                var skipCount = 0
                for (entry in response.body!!.d.results) {
                    if (!Files.exists(Paths.get("uncompressed", entry.name))) {
                        if (skipCount != 0) {
                            log.info(" * Skipped $skipCount products as already downloaded")
                            skipCount = 0
                        }

                        lastBatch.add(Product(entry.name, Date(), center, metadata = mutableMapOf("uuid" to entry.id, "ingestionDate" to entry.ingestionDate)))

                        center.options!!["lastIngestionDate"] = entry.ingestionDate.toString()
                    } else
                        skipCount++
                }

                if (skipCount != 0) log.info(" * Skipped $skipCount products as already downloaded")

                skip += pageSize

                hasNext = response.body!!.d.results.size == pageSize
            } catch (e: HttpStatusCodeException) {
                log.severe("HTTP Error (${e.statusCode}): ${e.message} ${e.responseBodyAsString}")
            }
        }
    }

    // helper classes to decode odata query
    private data class ODataEntry(
            @JsonProperty("Id")
            var id: String,
            @JsonProperty("Name")
            var name: String,
            @JsonProperty("IngestionDate")
            var ingestionDate: String?
    )

    private data class ODataRoot(var d: ODataResults)

    private data class ODataResults(var results: List<ODataEntry>)

}


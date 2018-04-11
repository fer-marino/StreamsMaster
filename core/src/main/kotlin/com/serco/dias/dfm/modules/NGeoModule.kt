package com.serco.dias.dfm.modules

import com.serco.dias.dfm.model.Center
import com.serco.dias.dfm.model.Product
import com.serco.dias.dfm.utils.SlowInputStream
import org.apache.commons.io.FileUtils
import org.apache.commons.io.input.CountingInputStream
import org.w3c.dom.NodeList
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.function.BiPredicate
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

abstract class NGeoModule : StreamModule() {
    override fun download(product: Product): Product {
        product.downloadStart = Date()
        val center = product.downloadCenter
        val username = center.options!!["username"].toString()
        val password = center.options!!["password"].toString()
        var address = center.options!!["address"].toString()

        val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val tdac = product.downloadCenter.type == "Sentinel1"
        // start of ngeo interface
        // fetch metalink
        val basicAuth = "Basic " + String(org.apache.commons.net.util.Base64.encodeBase64(("$username:$password").toByteArray()))

        address = address.replaceFirst("^http[s]?://", "")
        val metalink = URL("http://" + address + ":80/oda/rest/ngEO/" + product.name + if (tdac) "?ID_TYPE=PHYSICAL" else "")
        var connection = metalink.openConnection()
        if (tdac) connection.setRequestProperty("Authorization", basicAuth)
        val doc = db.parse(connection.getInputStream())

        val xPathfactory = XPathFactory.newInstance()
        val responseCode = xPathfactory.newXPath().compile("/ProductDownloadResponse/ResponseCode/text()").evaluate(doc, XPathConstants.NODESET) as NodeList

        if (responseCode.length > 0) {
            // lta forwarded request
            val code = responseCode.item(0).textContent
            throw IOException("Product rolled from ODA. Will be fetched from LTA. Response code $code")
        }

        val urls = xPathfactory.newXPath().compile("//file/resources/url/text()").evaluate(doc, XPathConstants.NODESET) as NodeList
        val prods = xPathfactory.newXPath().compile("//file/@name").evaluate(doc, XPathConstants.NODESET) as NodeList

        // download each file
        if (product.name.matches(Regex(center.noDataFilter)))
            log.warning("Product ${product.name} match no-data (pattern ${center.noDataFilter})")

        for (i in 0 until urls.length) {
            val url = urls.item(i).textContent
            val filename = prods.item(i).textContent

            if (!filename.contains("manifest") && !filename.contains("measurement") && product.name.matches(Regex(center.noDataFilter))) continue

            val destination = File(config.tmpDir + File.separator + filename)
            if (!destination.parentFile.exists())
                destination.parentFile.mkdirs()
            val source = URL(url)
            val newURL = URL(source.protocol, address, source.port, source.file)
            connection = newURL.openConnection().apply {
                connectTimeout = 100000
                readTimeout = 100000
            }


            if (tdac) connection.setRequestProperty("Authorization", basicAuth)

            val input = CountingInputStream(if (config.bandwidth > 0)
                SlowInputStream(connection.getInputStream(), config.bandwidth / config.parallelDownloads)
            else
                connection.getInputStream())
            meterUtils.monitorDownload(input, center, { FileUtils.copyInputStreamToFile(input, destination) })
        }

        // quality validation
        val manifest = Files.find(Paths.get(config.tmpDir, product.name), 2,
                BiPredicate { path, _ -> path.toString().toLowerCase().contains("manifest") },
                FileVisitOption.FOLLOW_LINKS).findFirst()

        when {
            manifest.isPresent -> {
                //  check md5
                if (!product.name.matches(Regex(center.noDataFilter)) && center.validation != Center.ValidationMode.NONE)
                    validate(product, manifest.get())
            }
            product.name.matches(Regex(center.skipManifestCheck)) -> log.warning("Manifest missing, validation not possible. Is this a valid product?")
            else -> throw IllegalArgumentException("Manifest is missing")
        }

        product.size = FileUtils.sizeOfDirectory(File(config.tmpDir + File.separator + product.name))

        if (center.destination.isNotBlank()) {
            val dest = Paths.get(center.destination, product.name)
            if (Files.exists(dest))
                FileUtils.deleteDirectory(dest.toFile())

            if (Files.isSymbolicLink(Paths.get(config.tmpDir, product.name))) {
                Files.move(Paths.get(config.tmpDir, product.name), dest)
            } else
                FileUtils.moveDirectory(Paths.get(config.tmpDir, product.name).toFile(), dest.toFile())
        }

        product.downloadStop = Date()

        return product
    }
}
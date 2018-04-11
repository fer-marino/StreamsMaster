package com.serco.dias.dfm.modules

import com.serco.dias.dfm.model.Center
import com.serco.dias.dfm.model.Product
import org.apache.commons.net.ftp.FTPClient
import org.springframework.stereotype.Component
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import reactor.core.publisher.Flux
import reactor.core.publisher.toFlux
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory


@Component
class Sentinel3Module : NGeoModule() {

    override fun list(center: Center): Flux<Product> {
        val f = FTPClient()
        try {
            f.connect(center.address, center.options!!["port"].toString().toInt())
            f.enterLocalPassiveMode()
            val username = center.options!!["username"].toString()
            val password = center.options!!["password"].toString()
            f.login(username, password)
            f.changeWorkingDirectory("/${center.options!!["dataset"]}")

            // listNames is nlst ftp command. It returns only the file names without any extra info (timestamps and size)
            return f.listNames().map { Product(it, Date(), center) }.toFlux()
        } catch (e: IOException) {
            throw IOException("Error occurred during product list for center ${center.name}: ${e.message}", e)
        } finally {
            f.logout()
        }
    }

    override fun validate(product: Product, manifest: Path): Boolean {
        validate(Paths.get(config.tmpDir, product.name), manifest, product.downloadCenter.validation)

        var rank: Double
        val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = db.parse(Files.newInputStream(manifest))
        when (product.name.substring(4, 15)) {
            "OL_1_EFR___", "OL_1_ERR___" -> {
                rank = olciL1QualityRank(doc)
                val qualityOlci = product.downloadCenter.options!!["qualityThresholdOlci"]!!.toDouble()
                log.info("Quality rank for ${product.name} is $rank/$qualityOlci")
                if (rank > qualityOlci)
                    throw QualityInvalidException("Rank $rank, threshold $qualityOlci")
                rank = slstrL1QualityRank(doc)
                log.info("Quality rank for ${product.name} is $rank/$qualityOlci")
                if (rank > qualityOlci)
                    throw QualityInvalidException("Rank $rank, threshold $qualityOlci")
            }
            "SL_1_RBT___" -> {
                rank = slstrL1QualityRank(doc)
                val qualitySlstr = product.downloadCenter.options!!["qualityThresholdSlstr"]!!.toDouble()

                log.info("Quality rank for ${product.name} is $rank/$qualitySlstr")
                if (rank > qualitySlstr)
                    throw QualityInvalidException("Rank $rank, threshold $qualitySlstr")
            }
        }

        return true
    }

    private fun olciL1QualityRank(manifest: Document): Double {
        val xpath = XPathFactory.newInstance().newXPath()
        return xpath.compile("/XFDU/metadataSection/metadataObject/metadataWrap/xmlData/" +
                "olciProductInformation/pixelQualitySummary/invalidPixels/@percentage").evaluate(manifest).toDouble()
    }

    private fun slstrL1QualityRank(manifest: Document): Double {
        val xPathfactory = XPathFactory.newInstance()
        val xpath = xPathfactory.newXPath()
        val missing = xpath.compile("/XFDU/metadataSection/metadataObject/metadataWrap/xmlData" +
                "/slstrProductInformation/missingElements/globalInfo").evaluate(manifest, XPathConstants.NODESET) as NodeList

        var max = 0.0
        for (i in 0 until missing.length) {
            val c = missing.item(i)

            val current = xpath.compile("@percentage").evaluate(c).toDouble()
            max = Math.max(max, current)
        }

        return max
    }

    private inner class QualityInvalidException(s: String) : Throwable(s)

}
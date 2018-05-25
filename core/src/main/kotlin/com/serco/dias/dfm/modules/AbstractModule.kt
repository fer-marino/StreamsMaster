package com.serco.dias.dfm.modules

import com.serco.dias.dfm.config.Config
import com.serco.dias.dfm.model.Center
import com.serco.dias.dfm.model.Product
import com.serco.dias.dfm.utils.DownloaderHealth
import com.serco.dias.dfm.utils.Utils
import org.springframework.beans.factory.annotation.Autowired
import org.w3c.dom.NodeList
import reactor.core.publisher.Flux
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.logging.Logger
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

abstract class AbstractModule {
    val log get() = Logger.getLogger(this.javaClass.canonicalName)!!
    @Autowired
    protected lateinit var config: Config
    @Autowired
    lateinit var meterUtils: DownloaderHealth

    abstract fun list(center: Center): Flux<Product>
    abstract fun download(product: Product): Product

    open fun finalize(product: Product): Product = product

    protected fun validate(product: Path, manifest: Path, validation: Center.ValidationMode) {
        log.info("Validation started for ${product.fileName}")
        val start = System.currentTimeMillis()
        // extract md5 from manifest
        val md5Map = mutableMapOf<String, String>()
        val sizeMap = mutableMapOf<String, Long>()

        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = builder.parse(Files.newInputStream(manifest))
        val xPathfactory = XPathFactory.newInstance()
        val xpath = xPathfactory.newXPath()
        var nl = xpath.compile("/XFDU/dataObjectSection/dataObject/byteStream").evaluate(doc, XPathConstants.NODESET) as NodeList

        for (i in 0 until nl.length) {
            val node = nl.item(i)
            val file = xpath.compile("fileLocation/@href").evaluate(node).replaceFirst("./", "")
            val checksum = xpath.compile("checksum/text()").evaluate(node)
            val checkType = xpath.compile("checksum/@checksumName").evaluate(node)
            val s = xpath.compile("@size").evaluate(node).toLong()
            if (checkType.toLowerCase() == "md5")
                md5Map[file] = checksum
            sizeMap[file] = s
        }

        // validate
        if (validation == Center.ValidationMode.SIZE || validation == Center.ValidationMode.MD5)
            sizeMap.forEach { file, size ->
                if (Files.size(product.resolve(file)) != size)
                    throw  IOException("Size does not match for file $file")
            }

        if (validation == Center.ValidationMode.MD5)
            md5Map.forEach { file, md5 ->
                if (Utils.md5sum(product.resolve(file)).toLowerCase() != md5)
                    throw IOException("Md5 does not match for file $file")
            }

        // check completeness for metadata
        nl = xpath.compile("/XFDU/metadataSection/metadataObject/metadataReference/@href").evaluate(doc, XPathConstants.NODESET) as NodeList
        for (i in 0 until nl.length)
            if (!Files.exists(product.resolve(nl.item(i).nodeValue)))
                throw IOException("Product incomplete. Missing " + nl.item(i).nodeValue)


        log.info("Validation completed for ${product.fileName} in ${System.currentTimeMillis() - start} msec")
    }

    open fun validate(product: Product, manifest: Path): Boolean {
        validate(Paths.get(config.tmpDir, product.name), manifest, product.downloadCenter.validation)

        return true
    }
}
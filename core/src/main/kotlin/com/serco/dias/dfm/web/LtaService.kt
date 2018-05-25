package com.serco.dias.dfm.web

import com.serco.dias.dfm.config.Config
import com.serco.dias.dfm.model.DownloadTransaction
import com.serco.dias.dfm.model.Lta
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.w3c.dom.NodeList
import org.xml.sax.SAXException
import java.io.ByteArrayInputStream
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

@Service
class LtaService {
    @Autowired
    lateinit var config: Config

    // TODO circular list to avoid memory leak
    val transactions = mutableListOf<DownloadTransaction>()

    private val responseHandler: ResponseHandler<String> by lazy {
        ResponseHandler<String> {
            val status = it.statusLine.statusCode
            if (status in 200..299)
                if (it.entity == null) "" else EntityUtils.toString(it.entity)
            else
                throw ClientProtocolException("Unexpected response status: $status")
        }
    }

    fun list(lta: Lta, regex: String?, from: Date?, to: Date?): List<QueryResponse> = try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        HttpClients.createDefault().use { httpclient ->
            val request = HttpPost(String.format("http://%1s:%2d/%3s", lta.ip, lta.port, lta.queryUrl).trim { it <= ' ' })
            val body = StringEntity("<queryRequest> <clientID>${config.instanceName}</clientID><productRegEx>$regex</productRegEx>" +
                    "<timeRange><startDate>${sdf.format(from)}</startDate><stopDate>${sdf.format(to)}</stopDate></timeRange>" +
                    "</queryRequest>", ContentType.TEXT_XML)
            request.entity = body

            val responseBody = httpclient.execute(request, responseHandler)
            /* sample
                <?xml version="1.0" encoding="UTF-8"?><queryResponse><requestOutcome>OK</requestOutcome><reason></reason><productsListOutcome><product><productID status="NOT_FOUND">S3A_SR_0_SRA____20150101T102500_20150101T114000_4500_030_215______ESR_O_NT_____00.YY
                YY</productID><statusTimestamp>2016-06-29T07:32:04.478Z</statusTimestamp><statusReason>NOT_FOUND on 20160629T073204</statusReason></product><product><productID status="NOT_FOUND">S3A_SL_1_RBT____20150101T102500_20150101T114000_4500_030_215______ESR_O_NT_____01.YY
                YY</productID><statusTimestamp>2016-06-29T07:32:04.478Z</statusTimestamp><statusReason>NOT_FOUND on 20160629T073204</statusReason></product><product><productID status="NOT_FOUND">S3A_SL_2_LST____20150101T102500_20150101T114000_4500_030_215______ESR_F_NT_TST_01.YY
                YY</productID><statusTimestamp>2016-06-29T07:32:04.478Z</statusTimestamp><statusReason>NOT_FOUND on 20160629T073204</statusReason></product></productsListOutcome></queryResponse>
             */

            val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val doc = db.parse(ByteArrayInputStream(responseBody.toByteArray(charset("UTF-8"))))
            val xPathfactory = XPathFactory.newInstance()
            val requestOutcome = xPathfactory.newXPath().compile("//requestOutcome/text()").evaluate(doc, XPathConstants.STRING).toString()
            val requestReason = xPathfactory.newXPath().compile("/queryResponse/reason/text()").evaluate(doc, XPathConstants.STRING).toString()

            if (requestOutcome.toLowerCase() == "ok") {
                val out = ArrayList<QueryResponse>()
                val products = xPathfactory.newXPath().compile("//product").evaluate(doc, XPathConstants.NODESET) as NodeList
                for (i in 0 until products.length) {
                    val status = xPathfactory.newXPath().compile("./productID/@status").evaluate(products.item(i), XPathConstants.STRING).toString()
                    val name = xPathfactory.newXPath().compile("./productID/text()").evaluate(products.item(i), XPathConstants.STRING).toString()
                    val timestamp = xPathfactory.newXPath().compile("./statusTimestamp/text()").evaluate(products.item(i), XPathConstants.STRING).toString().replace("\\.[0-9]*Z?$".toRegex(), "")
                    val reason = xPathfactory.newXPath().compile("./statusReason/text()").evaluate(products.item(i), XPathConstants.STRING).toString()

                    out.add(QueryResponse(lta, name, status, sdf.parse(timestamp), reason))
                }
                return out
            } else
                throw IllegalStateException("Query refused because $requestReason")
        }
    } catch (e: IOException) {
        throw IllegalStateException("Unable to submit request to ${lta.id}. ${e.message}", e)
    } catch (e: ParseException) {
        throw IllegalStateException("Unable to parse date " + e.message, e)
    } catch (e: ParserConfigurationException) {
        throw IllegalStateException("Unable to parse response " + e.message, e)
    } catch (e: SAXException) {
        throw IllegalStateException("Unable to parse response " + e.message, e)
    } catch (e: XPathExpressionException) {
        throw IllegalStateException("Unable to parse response " + e.message, e)
    }

    fun retrieve(lta: Lta, product: String): DownloadTransaction = try {
        HttpClients.createDefault().use { httpclient ->
            val request = HttpPost(String.format("http://%1s:%2d/%3s", lta.ip.trim({ it <= ' ' }), lta.port, lta.retrieveUrl).trim { it <= ' ' })
            request.entity = StringEntity("<retrieveRequest><clientID>${config.instanceName}</clientID><productID>$product</productID>" +
                    "<url>${lta.targetUrl}</url></retrieveRequest>", ContentType.APPLICATION_XML)

            val responseBody = httpclient.execute(request, responseHandler)
            // sample <?xml version="1.0" encoding="UTF-8"?><retrieveResponse><transactionID>508367004</transactionID><requestOutcome>OK</requestOutcome><fileSize>171845042</fileSize></retrieveResponse>
            val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val doc = db.parse(ByteArrayInputStream(responseBody.toByteArray(charset("UTF-8"))))
            val xPathfactory = XPathFactory.newInstance()
            val transactionId = java.lang.Long.valueOf(xPathfactory.newXPath().compile("//transactionID/text()").evaluate(doc, XPathConstants.STRING).toString())
            val outcome = xPathfactory.newXPath().compile("//requestOutcome/text()").evaluate(doc, XPathConstants.STRING).toString()
            val fileSize = java.lang.Long.valueOf(xPathfactory.newXPath().compile("//fileSize/text()").evaluate(doc, XPathConstants.STRING).toString())
            val t = DownloadTransaction(transactionId, lta.center, outcome, fileSize, product, Date())
            transactions.add(t)

            if (transactions.size > 10000) {
                val i = transactions.indexOfFirst { it.status == "COMPLETED" } // TODO check status value
                transactions.removeAt(i)
            }

            return t
        }
    } catch (e: IOException) {
        throw IllegalStateException("Unable to submit retrieve request to " + lta.id, e)
    } catch (e: ParserConfigurationException) {
        throw IllegalStateException("Unable to parse response for " + lta.id, e)
    } catch (e: XPathExpressionException) {
        throw IllegalStateException("Unable to parse response for " + lta.id, e)
    } catch (e: SAXException) {
        throw IllegalStateException("Unable to parse response for " + lta.id, e)
    }

    fun status(transaction: DownloadTransaction) = try {
        HttpClients.createDefault().use { httpclient ->
            val request = HttpGet(String.format("http://%1s:%2d/%3s?clientID=%4s&transactionID=%5s",
                    transaction.center.lta!!.ip, transaction.center.lta!!.port, transaction.center.lta!!.checkTransactionUrl, config.instanceName, transaction.id))
            val responseBody = httpclient.execute(request, responseHandler)

            // sample <transactionStatusResponse><requestOutcome>OK</requestOutcome><transactionStatus>FINISHED</transactionStatus><retryAfter>5 </retryAfter></transactionStatusResponse>
            val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val doc = db.parse(ByteArrayInputStream(responseBody.toByteArray(charset("UTF-8"))))
            val xPathfactory = XPathFactory.newInstance()
            val transactionStatus = xPathfactory.newXPath().compile("//transactionStatus/text()").evaluate(doc, XPathConstants.STRING).toString()
            val reason = xPathfactory.newXPath().compile("//reason/text()").evaluate(doc, XPathConstants.STRING).toString()

            transaction.reason = reason
            transaction.status = transactionStatus
            transaction.lastUpdate = Date()
        }
    } catch (e: IOException) {
        val message = "Unable to send status request for ${transaction.product} due to ${e.message}"
    } catch (e: ParserConfigurationException) {
        val message = "Unable to parse response for ${transaction.product} due to ${e.message}"
    } catch (e: SAXException) {
        val message = "Unable to parse response for ${transaction.product} due to ${e.message}"
    } catch (e: XPathExpressionException) {
        val message = "Unable to parse response for ${transaction.product} due to ${e.message}"
    }

    data class QueryResponse constructor(val lta: Lta, val product: String, val status: String, val timestamp: Date, val statusReason: String)
}
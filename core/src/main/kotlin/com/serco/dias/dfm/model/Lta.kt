package com.serco.dias.dfm.model

import java.io.Serializable
import java.util.*


data class Lta(
        var id: String,
        var ip: String,
        var port: Int,
        var checkTransactionUrl: String,
        var queryUrl: String,
        var abortUrl: String,
        var retrieveUrl: String,
        var center: Center,
        var targetUrl: String
) : Serializable

data class DownloadTransaction(
        val id: Long,
        val center: Center,
        val outcome: String,
        val filesize: Long?,
        val product: String,
        val start: Date,
        var lastUpdate: Date? = null,

        var status: String? = null,
        var reason: String? = null
) {
    constructor(product: Product) : this(0, product.downloadCenter, "COMPLETED", product.size, product.name, product.downloadStart!!, product.downloadStop)
}

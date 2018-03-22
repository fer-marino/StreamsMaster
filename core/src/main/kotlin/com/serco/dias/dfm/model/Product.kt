package com.serco.dias.dfm.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.io.Serializable
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
class Product(var name: String = "",
              var enqueued: Date = Date(),
              var downloadCenter: Center = Center(),
              var downloadStart: Date? = null,
              var downloadStop: Date? = null,
              var size: Long? = null,
              var metadata: MutableMap<String, Any?>? = null) : Serializable {

    override fun toString(): String {
        return "Product(name='$name', enqueued=$enqueued, downloadCenter=$downloadCenter, downloadStart=$downloadStart, downloadStop=$downloadStop)"
    }

}

package com.serco.dias.dfm.model

import java.io.Serializable

class Center : Serializable {
    var id: String = ""
    var name: String = ""
    var type: String = ""
    var address: String = ""
    var destination: String = ""
    var filter: String? = ""
    var noDataFilter: String = "-"
    var validation: ValidationMode = ValidationMode.MD5
    var skipManifestCheck: String = "-"
    var options: MutableMap<String, String>? = null
    var lta: Lta? = null

    enum class ValidationMode {
        NONE, SIZE, MD5
    }

}

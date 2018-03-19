package com.serco.dias.streamsMaster.model

import java.io.Serializable

class Center: Serializable {
    var id: String = ""
    var name: String = ""
    var type: String = ""
    var address: String = ""
    var destination: String = ""
    var parallelDownloads: Int? = 2
    var filter: String? = ""
    var options: MutableMap<String, Any>? = null
}


//interface CenterRepository : CrudRepository<Center, String>
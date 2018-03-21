package com.serco.dias.dfm.config

import com.serco.dias.dfm.model.Center
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix="streams-master")
open class Config {
    var listInterval: Int = 60000
    var productPerPoll: Int = 100
    var tmpDir: String = System.getProperty("java.io.tmpdir")
    var parallelDownloads: Int = 2
    var bandwidth: Int = -1
    @NestedConfigurationProperty
    var center: Center = Center()
}
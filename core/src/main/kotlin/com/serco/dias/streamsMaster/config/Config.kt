package com.serco.dias.streamsMaster.config

import com.serco.dias.streamsMaster.model.Center
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
    @NestedConfigurationProperty
    var center: Center = Center()
}
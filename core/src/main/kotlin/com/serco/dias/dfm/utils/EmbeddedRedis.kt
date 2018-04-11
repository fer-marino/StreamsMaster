package com.serco.dias.dfm.utils

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import redis.embedded.RedisServer
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy


@Component
@Profile("embeddedRedis")
class EmbeddedRedis {

    @Value("\${spring.redis.port}")
    private val redisPort: Int = 0

    private lateinit var redisServer: RedisServer

    @PostConstruct
    @Throws(IOException::class)
    fun startRedis() {
        val dataDir = Paths.get("data").toAbsolutePath()
        if (Files.notExists(dataDir)) Files.createDirectories(dataDir)

        val redisServer = RedisServer.builder()
                .port(redisPort)
                .setting("bind 127.0.0.1") // good for local development on Windows to prevent security popups
                .setting("daemonize no")
                .setting("appendonly no")
                .setting("maxmemory 64M")
                .setting("dir \"${dataDir.toString().replace("""\""", """\\""")}\"")
                .build()
        redisServer.start()
    }

    @PreDestroy
    fun stopRedis() {
        redisServer.stop()
    }
}
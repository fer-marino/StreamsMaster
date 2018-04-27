package com.serco.dias.dfm.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.serco.dias.dfm.model.Product
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.integration.redis.metadata.RedisMetadataStore
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.text.DecimalFormat
import javax.xml.bind.DatatypeConverter


@Component
class Utils {
    companion object {

        fun readableFileSize(size: Long): String {
            if (size <= 0) return "0"
            val units = arrayOf("B", "kB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
        }

        fun md5sum(file: Path): String {
            if (!Files.exists(file))
                throw IllegalArgumentException("File $file does not exists")

            Files.newInputStream(file).use({ stream ->
                val md = MessageDigest.getInstance("MD5")

                val dataBytes = ByteArray(1024)
                var nread = stream.read(dataBytes)
                while (nread != -1) {
                    md.update(dataBytes, 0, nread)
                    nread = stream.read(dataBytes)
                }

                val digest = md.digest()

                return DatatypeConverter.printHexBinary(digest).toLowerCase()
            })
        }

    }

    @Bean(name = ["redisProductSerializer"])
    fun getRedisProductMessageStore(@Autowired om: ObjectMapper): RedisSerializer<Product> = Jackson2JsonRedisSerializer<Product>(Product::class.java).apply { setObjectMapper(om) }

    @Bean(name = ["redisSerializer"])
    fun getRedisMessageSerializer(@Autowired objectMapper: ObjectMapper) = GenericJackson2JsonRedisSerializer(objectMapper)

    @Bean(name = ["metadataStore"])
    fun getMetadataStore(@Autowired rcf: RedisConnectionFactory) = RedisMetadataStore(rcf)


}
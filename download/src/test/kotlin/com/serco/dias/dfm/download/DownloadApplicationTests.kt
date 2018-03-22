package com.serco.dias.dfm.download

import com.serco.dias.dfm.config.Config
import com.serco.dias.dfm.model.Product
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.stream.messaging.Processor
import org.springframework.cloud.stream.test.binder.MessageCollector
import org.springframework.messaging.support.GenericMessage
import org.springframework.test.context.junit4.SpringRunner
import java.util.*
import java.util.concurrent.TimeUnit

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
class DownloadApplicationTests {

	@Autowired
	private lateinit var processor: Processor
	@Autowired lateinit var config: Config

	@Autowired
	lateinit var messageCollector: MessageCollector

	@Test @SuppressWarnings("unchecked")
	fun testDhus() {
		val message = GenericMessage(
				Product("S3A_SR_2_LAN____20180320T064932_20180320T065931_20180320T090109_0599_029_120______SVL_O_NR_003",
                        Date(), config.center, metadata = mutableMapOf("uuid" to "94457d61-7caa-4e98-bfe9-2c8cf74eb137")))
		processor.input().send(message)

		val received = messageCollector.forChannel(processor.output()).poll(5, TimeUnit.MINUTES)

	}

}

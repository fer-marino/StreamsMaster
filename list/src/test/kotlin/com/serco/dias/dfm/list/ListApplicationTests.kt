package com.serco.dias.dfm.list

import com.serco.dias.dfm.model.Center
import com.serco.dias.dfm.model.Product
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.stream.messaging.Processor
import org.springframework.cloud.stream.test.binder.MessageCollector
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.GenericMessage
import org.springframework.test.context.junit4.SpringRunner
import java.util.concurrent.TimeUnit

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
class ListApplicationTests {

	@Autowired
	private lateinit var processor: Processor

	@Autowired
	lateinit var messageCollector: MessageCollector

	@Test @SuppressWarnings("unchecked")
	fun testWiring() {
		val message = GenericMessage(Center())
		processor.input().send(message)

		val received = messageCollector.forChannel(processor.output()).poll() as Message<Product>

		assert(received != null)
	}

}

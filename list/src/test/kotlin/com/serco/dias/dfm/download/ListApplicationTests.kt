package com.serco.dias.dfm.download

import com.serco.dias.streamsMaster.model.Center
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.stream.test.binder.MessageCollector
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.GenericMessage
import org.springframework.test.context.junit4.SpringRunner
import java.util.concurrent.TimeUnit

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
class ListApplicationTests {

	@Autowired
	@Qualifier("list")
	private lateinit var list: MessageChannel
	@Autowired
	@Qualifier("completed")
	private lateinit var completed: MessageChannel

	@Autowired
	lateinit var messageCollector: MessageCollector

	@Test @SuppressWarnings("unchecked")
	fun testWiring() {
		val message = GenericMessage(Center())
		list.send(message)
		val received = messageCollector.forChannel(completed).poll(2, TimeUnit.MINUTES)
		println(received)

//        val received = messageCollector.forChannel(download).poll() as Message<String>
		assert(received != null)
	}
}

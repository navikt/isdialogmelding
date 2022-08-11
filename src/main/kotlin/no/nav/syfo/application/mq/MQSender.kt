package no.nav.syfo.application.mq

import com.ibm.msg.client.wmq.common.CommonConstants.*
import no.nav.syfo.application.Environment
import no.nav.syfo.metric.COUNT_SEND_MESSAGE_EMOTTAK_MQ
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.jms.JMSContext

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.application.mq")

class MQSender(private val env: Environment) {

    private val jmsContext: JMSContext = connectionFactory(env).createContext()

    protected fun finalize() {
        try {
            jmsContext.close()
        } catch (exc: Exception) {
            log.warn("Got exception when closing MQ-connection", exc)
        }
    }

    fun sendMessageToEmottak(payload: String) {
        val queueName = env.emottakQueuename
        jmsContext.createContext(AUTO_ACKNOWLEDGE).use { context ->
            val destination = context.createQueue("queue:///$queueName")
            val message = context.createTextMessage(payload)
            context.createProducer().send(destination, message)
        }
        COUNT_SEND_MESSAGE_EMOTTAK_MQ.increment()
    }
}

package no.nav.syfo.application.mq

import com.ibm.mq.constants.CMQC.MQENC_NATIVE
import com.ibm.msg.client.jms.JmsConstants.JMS_IBM_CHARACTER_SET
import com.ibm.msg.client.jms.JmsConstants.JMS_IBM_ENCODING
import com.ibm.msg.client.jms.JmsFactoryFactory
import com.ibm.msg.client.wmq.common.CommonConstants.*
import no.nav.syfo.application.Environment
import no.nav.syfo.metric.COUNT_SEND_MESSAGE_EMOTTAK_MQ
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.jms.ConnectionFactory
import javax.jms.JMSContext

private const val UTF_8_WITH_PUA = 1208
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

    private fun connectionFactory(env: Environment): ConnectionFactory {
        return JmsFactoryFactory.getInstance(WMQ_PROVIDER).createConnectionFactory().apply {
            setIntProperty(WMQ_CONNECTION_MODE, WMQ_CM_CLIENT)
            setStringProperty(WMQ_QUEUE_MANAGER, env.mqQueueManager)
            setStringProperty(WMQ_HOST_NAME, env.mqHostname)
            setStringProperty(WMQ_APPLICATIONNAME, env.mqApplicationName)
            setIntProperty(WMQ_PORT, env.mqPort)
            setStringProperty(WMQ_CHANNEL, env.mqChannelName)
            setIntProperty(WMQ_CCSID, UTF_8_WITH_PUA)
            setIntProperty(JMS_IBM_ENCODING, MQENC_NATIVE)
            setIntProperty(JMS_IBM_CHARACTER_SET, UTF_8_WITH_PUA)
            setBooleanProperty(USER_AUTHENTICATION_MQCSP, true)
            setStringProperty(USERID, env.serviceuserUsername)
            setStringProperty(PASSWORD, env.serviceuserPassword)
        }
    }
}

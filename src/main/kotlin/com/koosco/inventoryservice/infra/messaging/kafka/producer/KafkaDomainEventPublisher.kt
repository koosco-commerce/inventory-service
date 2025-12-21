package com.koosco.inventoryservice.infra.messaging.kafka.producer

import com.koosco.common.core.event.CloudEvent
import com.koosco.common.core.event.DomainEvent
import com.koosco.inventoryservice.application.event.DomainEventPublisher
import com.koosco.inventoryservice.infra.messaging.DomainTopicResolver
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/**
 * fileName       : KafkaDomainEventPublisher
 * author         : koo
 * date           : 2025. 12. 19. 오후 1:18
 * description    :
 */
@Component
class KafkaDomainEventPublisher(
    private val topicResolver: DomainTopicResolver,
    private val kafkaTemplate: KafkaTemplate<String, CloudEvent<*>>,

    @Value("\${spring.application.name}")
    private val source: String,
) : DomainEventPublisher {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun publish(event: DomainEvent) {
        val cloudEvent = event.toCloudEvent(source)

        val topic = topicResolver.resolve(event)
        val key = event.getAggregateId()

        kafkaTemplate.send(topic, key, cloudEvent)
            .whenComplete { _, ex ->
                if (ex == null) {
                    logger.info(
                        "DomainEvent published: type=${event.getEventType()}, aggregateId=$key, topic=$topic",
                    )
                } else {
                    logger.error(
                        "DomainEvent publish failed: type=${event.getEventType()}, aggregateId=$key, topic=$topic",
                        ex,
                    )
                }
            }
    }

    override fun publishAll(events: List<DomainEvent>) {
        events.forEach { publish(it) }
    }
}

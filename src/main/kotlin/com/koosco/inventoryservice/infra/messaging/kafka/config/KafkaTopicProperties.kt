package com.koosco.inventoryservice.infra.messaging.kafka.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * fileName       : KafkaTopicProperties
 * author         : koo
 * date           : 2025. 12. 19. 오후 3:00
 * description    :
 */
@Component
@ConfigurationProperties(prefix = "inventory.topic.domain")
class KafkaTopicProperties {

    /**
     * key   = DomainEvent.getEventType()
     * value = Kafka topic name
     */
    lateinit var mappings: Map<String, String>

    /**
     * fallback topic
     */
    lateinit var default: String
}

@Component
@ConfigurationProperties(prefix = "inventory.topic.integration")
class KafkaIntegrationProperties {

    lateinit var mappings: Map<String, String>

    /**
     * fallback topic
     */
    lateinit var default: String
}

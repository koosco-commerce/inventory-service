package com.koosco.inventoryservice.infra.event.kafka.producer

import com.koosco.common.core.event.DomainEvent
import com.koosco.inventoryservice.infra.event.DomainTopicResolver
import com.koosco.inventoryservice.infra.event.IntegrationTopicResolver
import com.koosco.inventoryservice.infra.event.kafka.event.InventoryIntegrationEvent
import org.springframework.stereotype.Component

/**
 * fileName       : TopicResolver
 * author         : koo
 * date           : 2025. 12. 19. 오후 1:24
 * description    : domain event와 topic mapping
 */
@Component
class InventoryDomainTopicResolver(private val topicProperties: KafkaTopicProperties) : DomainTopicResolver {
    override fun resolve(event: DomainEvent): String = topicProperties.mappings[event.getEventType()]
        ?: topicProperties.default
}

@Component
class InventoryIntegrationTopicResolver(private val props: KafkaIntegrationProperties) : IntegrationTopicResolver {

    override fun resolve(event: InventoryIntegrationEvent): String = props.mappings[event.getEventType()]
        ?: props.default
}

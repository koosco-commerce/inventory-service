package com.koosco.inventoryservice.application.event

import com.koosco.inventoryservice.infra.messaging.kafka.message.InventoryIntegrationEvent

/**
 * fileName       : IntegrationEventPublisher
 * author         : koo
 * date           : 2025. 12. 19. 오후 1:45
 * description    :
 */
interface IntegrationEventPublisher {
    fun publish(event: InventoryIntegrationEvent)
}

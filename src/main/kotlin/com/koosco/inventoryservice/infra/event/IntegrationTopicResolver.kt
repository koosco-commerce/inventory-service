package com.koosco.inventoryservice.infra.event

import com.koosco.inventoryservice.infra.event.kafka.event.InventoryIntegrationEvent

/**
 * fileName       : IntegrationTopicResolver
 * author         : koo
 * date           : 2025. 12. 19. 오후 3:00
 * description    :
 */
interface IntegrationTopicResolver {
    fun resolve(event: InventoryIntegrationEvent): String
}

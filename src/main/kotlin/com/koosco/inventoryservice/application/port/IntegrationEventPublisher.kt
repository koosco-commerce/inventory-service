package com.koosco.inventoryservice.application.port

import com.koosco.inventoryservice.application.contract.InventoryIntegrationEvent

/**
 * fileName       : IntegrationEventPublisher
 * author         : koo
 * date           : 2025. 12. 19. 오후 1:45
 * description    :
 */
interface IntegrationEventPublisher {
    fun publish(event: InventoryIntegrationEvent)
}

package com.koosco.inventoryservice.application.event

import com.koosco.common.core.event.DomainEvent

/**
 * fileName       : DomainEventPublisher
 * author         : koo
 * date           : 2025. 12. 19. 오후 1:14
 * description    :
 */
interface DomainEventPublisher {

    fun publish(event: DomainEvent)

    fun publishAll(event: List<DomainEvent>)
}

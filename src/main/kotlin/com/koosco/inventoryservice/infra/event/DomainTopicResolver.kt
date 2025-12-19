package com.koosco.inventoryservice.infra.event

import com.koosco.common.core.event.DomainEvent

/**
 * fileName       : TopicResolver
 * author         : koo
 * date           : 2025. 12. 19. 오후 1:23
 * description    :
 */
interface DomainTopicResolver {
    fun resolve(event: DomainEvent): String
}

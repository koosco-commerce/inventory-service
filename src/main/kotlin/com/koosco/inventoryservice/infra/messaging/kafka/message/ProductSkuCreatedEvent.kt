package com.koosco.inventoryservice.infra.messaging.kafka.message

import java.time.LocalDateTime

/**
 * fileName       : ProductEvents
 * author         : koo
 * date           : 2025. 12. 19. 오후 3:16
 * description    :
 */
data class ProductSkuCreatedEvent(
    val skuId: String,
    val productId: Long,
    val productCode: String,
    val price: Long,
    val optionValues: String,
    val initialQuantity: Int = 0,
    val createdAt: LocalDateTime,
)

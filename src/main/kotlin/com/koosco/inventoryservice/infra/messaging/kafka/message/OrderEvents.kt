package com.koosco.inventoryservice.infra.messaging.kafka.message

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/**
 * fileName       : OrderEvents
 * author         : koo
 * date           : 2025. 12. 19. 오후 12:41
 * description    :
 */
/**
 * Order created
 * → inventory should reserve stock
 */
data class OrderPlacedEvent(
    @field:NotNull
    val orderId: Long,

    @field:NotNull
    val skuId: String,

    @field:Positive
    val reservedAmount: Int,
)

/**
 * Order completed (payment success)
 * → Inventory should confirm reserved stock
 */
data class OrderCompleted(

    @field:NotNull
    val orderId: Long,

    @field:NotNull
    val skuId: String,

    @field:Positive
    val confirmedAmount: Int,
)

/**
 * Order failed (payment failed / canceled)
 * → Inventory should cancel reservation
 */
data class OrderCanceled(

    @field:NotNull
    val orderId: Long,

    @field:NotNull
    val skuId: String,

    @field:Positive
    val canceledAmount: Int,

    /**
     * 실패 유형 (선택)
     * INVENTORY는 로직에 사용하지 않음
     */
    val reason: String? = null,
)

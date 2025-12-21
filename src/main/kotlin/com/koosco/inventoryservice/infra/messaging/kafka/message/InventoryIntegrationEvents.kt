package com.koosco.inventoryservice.infra.messaging.kafka.message

import com.koosco.common.core.event.CloudEvent

/**
 * fileName       : InventoryIntegrationEvents
 * author         : koo
 * date           : 2025. 12. 19. 오후 2:38
 * description    :
 */

/**
 * Base marker interface for inventory integration events.
 *
 * Used for:
 * - IntegrationEventPublisher
 * - Topic resolution
 * - Logging / filtering
 */
sealed interface InventoryIntegrationEvent {
    val orderId: Long
    val skuId: String

    /**
     * CloudEvent type
     * 예: stock.reserve.failed
     */
    fun getEventType(): String

    /**
     * Kafka partition key
     */
    fun getPartitionKey(): String = orderId.toString()

    /**
     * CloudEvent subject (선택)
     */
    fun getSubject(): String = "order/$orderId"

    /**
     * CloudEvent 변환 (공통)
     */
    fun toCloudEvent(source: String): CloudEvent<InventoryIntegrationEvent> = CloudEvent.of(
        source = source,
        type = getEventType(),
        subject = getSubject(),
        data = this,
    )
}

/**
 * 재고 예약 실패
 * OrderCreated → Inventory.reserve 실패 시 발행
 */
data class StockReservationFailedEvent(override val orderId: Long, override val skuId: String, val reason: String) :
    InventoryIntegrationEvent {
    override fun getEventType(): String = "stock.reserve.failed"
}

/**
 * 재고 확정 실패
 * OrderCompleted → Inventory.confirm 실패 시 발행
 */
data class StockConfirmFailedEvent(override val orderId: Long, override val skuId: String, val reason: String) :
    InventoryIntegrationEvent {
    override fun getEventType(): String = "stock.confirm.failed"
}

/**
 * 재고 예약 취소 실패
 * OrderFailed / OrderCanceled → Inventory.cancelReservation 실패 시 발행
 */
data class StockReservationCancelFailedEvent(
    override val orderId: Long,
    override val skuId: String,
    val reason: String,
) : InventoryIntegrationEvent {
    override fun getEventType(): String = "stock.reservation.cancel.failed"
}

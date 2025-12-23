package com.koosco.inventoryservice.infra.messaging.kafka.consumer

import com.koosco.common.core.event.CloudEvent
import com.koosco.inventoryservice.application.command.CancelStockCommand
import com.koosco.inventoryservice.application.contract.inbound.order.OrderCancelledEvent
import com.koosco.inventoryservice.application.usecase.ReleaseStockUseCase
import com.koosco.inventoryservice.common.MessageContext
import com.koosco.inventoryservice.domain.enums.StockCancelReason.Companion.mapCancelReason
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

/**
 * fileName       : KafkaOrderCancelledEventConsumer
 * author         : koo
 * date           : 2025. 12. 19. 오후 3:47
 * description    :
 */
@Component
@Validated
class KafkaOrderCancelledEventConsumer(private val releaseStockUseCase: ReleaseStockUseCase) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["\${inventory.topic.integration.mappings.order.canceled}"],
        groupId = "\${spring.kafka.consumer.group-id}",
    )
    fun onOrderCancelled(@Valid event: CloudEvent<OrderCancelledEvent>, ack: Acknowledgment) {
        val data = event.data
            ?: run {
                logger.error("OrderCanceled is null: eventId=${event.id}")
                ack.acknowledge()
                return
            }

        logger.info(
            "Received OrderCanceled: eventId=${event.id}, orderId=${data.orderId}, items=${data.items}, reason=${data.reason}",
        )

        val context = MessageContext(
            correlationId = data.correlationId,
            causationId = event.id,
        )

        val command = CancelStockCommand(
            orderId = data.orderId,
            items = data.items.map { item ->
                CancelStockCommand.CancelledSku(
                    skuId = item.skuId,
                    quantity = item.quantity,
                )
            },
            reason = mapCancelReason(data.reason),
        )

        try {
            releaseStockUseCase.execute(command, context)

            ack.acknowledge()
            logger.info(
                "Stock reservation cancelled: eventId=${event.id}, orderId=${data.orderId}, items=${data.items}",
            )
        } catch (e: Exception) {
            logger.error(
                "Failed to process OrderCanceled event: eventId=${event.id}, orderId=${data.orderId}",
                e,
            )
            // TODO: 주문 취소에 따른 재고 증가는 반드시 일어나야하므로 재시도 후 DLQ 처리
            throw e
        }
    }
}

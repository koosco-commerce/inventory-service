package com.koosco.inventoryservice.infra.messaging.kafka.consumer

import com.koosco.common.core.event.CloudEvent
import com.koosco.inventoryservice.application.command.CancelStockCommand
import com.koosco.inventoryservice.application.usecase.ReserveStockUseCase
import com.koosco.inventoryservice.infra.messaging.kafka.message.OrderCanceled
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

/**
 * fileName       : KafkaOrderCanceledEventListener
 * author         : koo
 * date           : 2025. 12. 19. 오후 3:47
 * description    :
 */
@Component
@Validated
class KafkaOrderCanceledConsumer(private val reserveStockUseCase: ReserveStockUseCase) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["\${inventory.topic.integration.mappings.order.canceled}"],
        groupId = "\${spring.kafka.consumer.group-id}",
    )
    fun onOrderCanceled(@Valid event: CloudEvent<OrderCanceled>, ack: Acknowledgment) {
        val data = event.data
            ?: run {
                logger.error("OrderCanceled is null: eventId=${event.id}")
                ack.acknowledge()
                return
            }

        logger.info(
            "Received OrderCanceled: eventId=${event.id}, orderId=${data.orderId}, skuId=${data.skuId}, confirmedAmount=${data.canceledAmount}",
        )

        try {
            // 재고 예약 취소
            reserveStockUseCase.cancel(
                CancelStockCommand(
                    orderId = data.orderId,
                    skuId = data.skuId,
                    quantity = data.canceledAmount,
                ),
            )
            ack.acknowledge()
            logger.info(
                "Stock reservation cancelled: eventId=${event.id}, skuId=${data.skuId}, quantity=${data.canceledAmount}, orderId=${data.orderId}",
            )
        } catch (e: Exception) {
            logger.error(
                "Failed to process order canceled event: ${event.id}, " +
                    "orderId=${data.orderId}, skuId=${data.skuId}",
                e,
            )
            e
            // TODO: 주문 취소에 따른 재고 증가는 반드시 일어나야하므로 재시도 후 DLQ 처리
        }
    }
}

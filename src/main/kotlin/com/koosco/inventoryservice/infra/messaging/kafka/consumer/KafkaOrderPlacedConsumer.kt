package com.koosco.inventoryservice.infra.messaging.kafka.consumer

import com.koosco.common.core.event.CloudEvent
import com.koosco.inventoryservice.application.command.ReserveStockCommand
import com.koosco.inventoryservice.application.usecase.ReserveStockUseCase
import com.koosco.inventoryservice.domain.exception.NotEnoughStockException
import com.koosco.inventoryservice.infra.messaging.kafka.message.OrderPlacedEvent
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

/**
 * fileName       : KafkaOrderCreatedEventListener
 * author         : koo
 * date           : 2025. 12. 19. 오후 12:38
 * description    : OrderCreatedEvent 처리 리스너
 */
@Component
class KafkaOrderPlacedConsumer(private val reserveStockUseCase: ReserveStockUseCase) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["\${inventory.topic.integration.mappings.order.placed}"],
        groupId = "\${spring.kafka.consumer.group-id}",
    )
    fun onOrderCreated(@Valid event: CloudEvent<OrderPlacedEvent>, ack: Acknowledgment) {
        val data = event.data
            ?: run {
                logger.error("OrderPlacedEvent data is null: eventId=${event.id}")
                ack.acknowledge()
                return
            }

        logger.info(
            "Received OrderPlacedEvent: eventId=${event.id}, " +
                "orderId=${data.orderId}, skuId=${data.skuId}, quantity=${data.reservedAmount}",
        )

        try {
            reserveStockUseCase.reserve(
                ReserveStockCommand(
                    orderId = data.orderId,
                    skuId = data.skuId,
                    quantity = data.reservedAmount,
                ),
            )

            ack.acknowledge()
            logger.info(
                "Successfully reserve stock for ORDER: eventId=${event.id}, orderId=${data.orderId}, " +
                    "skuId=${data.skuId}, quantity=${data.reservedAmount}",
            )
        } catch (e: NotEnoughStockException) {
            logger.warn(
                "Stock reservation failed: eventId=${event.id}, orderId=${data.orderId}",
            )
            ack.acknowledge()
        } catch (e: Exception) {
            logger.error(
                "Failed to process OrderPlacedEvent: eventId=${event.id}, orderId=${data.orderId}",
                e,
            )
            // ❗ ack 안 함 → retry / DLQ
            throw e
        }
    }
}

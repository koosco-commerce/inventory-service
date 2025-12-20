package com.koosco.inventoryservice.infra.event.kafka.consumer

import com.koosco.common.core.event.CloudEvent
import com.koosco.inventoryservice.application.command.ReserveStockCommand
import com.koosco.inventoryservice.application.event.DomainEventPublisher
import com.koosco.inventoryservice.application.event.IntegrationEventPublisher
import com.koosco.inventoryservice.application.usecase.ReserveStockUseCase
import com.koosco.inventoryservice.domain.exception.NotEnoughStockException
import com.koosco.inventoryservice.infra.event.kafka.event.OrderCreatedEvent
import com.koosco.inventoryservice.infra.event.kafka.event.StockReservationFailedEvent
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
class KafkaOrderCreatedEventListener(
    private val reserveStockUseCase: ReserveStockUseCase,
    private val domainEventPublisher: DomainEventPublisher,
    private val integrationEventPublisher: IntegrationEventPublisher,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["\${kafka.topics.order-created}"],
        groupId = "\${spring.kafka.consumer.group-id}",
    )
    fun onOrderCreated(@Valid event: CloudEvent<OrderCreatedEvent>, ack: Acknowledgment) {
        val data = event.data
            ?: run {
                logger.error("OrderCreatedEvent data is null: eventId=${event.id}")
                ack.acknowledge()
                return
            }

        logger.info(
            "Received OrderCreatedEvent: eventId=${event.id}, " +
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
            integrationEventPublisher.publish(
                StockReservationFailedEvent(
                    orderId = data.orderId,
                    skuId = data.skuId,
                    reason = "NOT_ENOUGH_STOCK",
                ),
            )

            ack.acknowledge()
        } catch (e: Exception) {
            logger.error(
                "Failed to process order created event: ${event.id}, " +
                    "orderId=${data.orderId}, skuId=${data.skuId}",
                e,
            )
            throw e
        }
    }
}

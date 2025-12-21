package com.koosco.inventoryservice.infra.messaging.kafka.consumer

import com.koosco.common.core.event.CloudEvent
import com.koosco.inventoryservice.application.command.ConfirmStockCommand
import com.koosco.inventoryservice.application.event.DomainEventPublisher
import com.koosco.inventoryservice.application.event.IntegrationEventPublisher
import com.koosco.inventoryservice.application.usecase.ReserveStockUseCase
import com.koosco.inventoryservice.domain.exception.NotEnoughStockException
import com.koosco.inventoryservice.infra.messaging.kafka.message.OrderCompleted
import com.koosco.inventoryservice.infra.messaging.kafka.message.StockConfirmFailedEvent
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

/**
 * fileName       : KafkaOrderConfirmedEventListener
 * author         : koo
 * date           : 2025. 12. 19. 오후 2:27
 * description    :
 */
@Component
@Validated
class KafkaOrderConfirmedEventListener(
    private val reserveStockUseCase: ReserveStockUseCase,
    private val domainEventPublisher: DomainEventPublisher,
    private val integrationEventPublisher: IntegrationEventPublisher,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["\${kafka.topics.order-confirmed}"],
        groupId = "\${spring.kafka.consumer.group-id}",
    )
    fun onOrderCompleted(@Valid event: CloudEvent<OrderCompleted>, ack: Acknowledgment) {
        val data = event.data
            ?: run {
                logger.error("OrderCompleted is null: eventId=${event.id}")
                ack.acknowledge()
                return
            }

        logger.info(
            "Received OrderCompleted: eventId=${event.id}, orderId=${data.orderId}, skuId=${data.skuId}, confirmedAmount=${data.confirmedAmount}",
        )

        try {
            // 재고 확정
            reserveStockUseCase.confirm(
                ConfirmStockCommand(
                    orderId = data.orderId,
                    skuId = data.skuId,
                    quantity = data.confirmedAmount,
                ),
            )

            ack.acknowledge()
            logger.info(
                "Successfully reserve stock for ORDER: eventId=${event.id}, orderId=${data.orderId}, " +
                    "skuId=${data.skuId}, quantity=${data.confirmedAmount}",
            )
        } catch (e: NotEnoughStockException) {
            // 재고가 부족하면 재시도
            // 재시도 실패하면 실패 이벤트를 발행하여 롤백 시도
            integrationEventPublisher.publish(
                StockConfirmFailedEvent(
                    orderId = data.orderId,
                    skuId = data.skuId,
                    reason = "NOT_ENOUGH_STOCK",
                ),
            )
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

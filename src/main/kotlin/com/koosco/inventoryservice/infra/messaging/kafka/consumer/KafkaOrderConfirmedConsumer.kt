package com.koosco.inventoryservice.infra.messaging.kafka.consumer

import com.koosco.common.core.event.CloudEvent
import com.koosco.inventoryservice.application.command.ConfirmStockCommand
import com.koosco.inventoryservice.application.usecase.ReserveStockUseCase
import com.koosco.inventoryservice.domain.exception.NotEnoughStockException
import com.koosco.inventoryservice.infra.messaging.kafka.message.OrderCompleted
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
class KafkaOrderConfirmedConsumer(private val reserveStockUseCase: ReserveStockUseCase) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["\${inventory.topic.integration.mappings.order.confirmed}"],
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
                "Successfully confirm stock for ORDER: eventId=${event.id}, orderId=${data.orderId}, " +
                    "skuId=${data.skuId}, quantity=${data.confirmedAmount}",
            )
        } catch (e: NotEnoughStockException) {
            logger.warn(
                "Stock confirmed failed: eventId=${event.id}, orderId=${data.orderId}",
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

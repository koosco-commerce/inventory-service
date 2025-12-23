package com.koosco.inventoryservice.infra.messaging.kafka.consumer

import com.koosco.common.core.event.CloudEvent
import com.koosco.inventoryservice.application.command.ConfirmStockCommand
import com.koosco.inventoryservice.application.contract.inbound.order.OrderConfirmedEvent
import com.koosco.inventoryservice.application.usecase.ConfirmStockUseCase
import com.koosco.inventoryservice.common.MessageContext
import com.koosco.inventoryservice.domain.exception.NotEnoughStockException
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

/**
 * fileName       : KafkaOrderConfirmedEventConsumer
 * author         : koo
 * date           : 2025. 12. 19. 오후 2:27
 * description    :
 */
@Component
@Validated
class KafkaOrderConfirmedEventConsumer(private val confirmStockUseCase: ConfirmStockUseCase) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["\${inventory.topic.integration.mappings.order.confirmed}"],
        groupId = "\${spring.kafka.consumer.group-id}",
    )
    fun onOrderConfirmed(@Valid event: CloudEvent<OrderConfirmedEvent>, ack: Acknowledgment) {
        val data = event.data
            ?: run {
                logger.error("OrderCompleted is null: eventId=${event.id}")
                ack.acknowledge()
                return
            }

        logger.info(
            "Received OrderCompleted: eventId=${event.id}, orderId=${data.orderId}, items=${data.items}",
        )

        val context = MessageContext(
            correlationId = data.correlationId,
            causationId = event.id,
        )

        val command = ConfirmStockCommand(
            orderId = data.orderId,
            items = data.items.map { item ->
                ConfirmStockCommand.ConfirmedSku(
                    skuId = item.skuId,
                    quantity = item.quantity,
                )
            },
        )

        try {
            confirmStockUseCase.execute(command, context)

            ack.acknowledge()
            logger.info(
                "Successfully confirmed stock for ORDER: eventId=${event.id}, orderId=${data.orderId}, items=${data.items}",
            )
        } catch (_: NotEnoughStockException) {
            logger.warn(
                "Stock confirmation failed: eventId=${event.id}, orderId=${data.orderId}",
            )
            ack.acknowledge()
        } catch (e: Exception) {
            logger.error(
                "Failed to process OrderCompleted event: eventId=${event.id}, orderId=${data.orderId}",
                e,
            )
            throw e
        }
    }
}

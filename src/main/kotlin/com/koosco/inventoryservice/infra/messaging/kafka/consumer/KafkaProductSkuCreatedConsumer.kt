package com.koosco.inventoryservice.infra.messaging.kafka.consumer

import com.koosco.common.core.event.CloudEvent
import com.koosco.inventoryservice.application.command.InitStockCommand
import com.koosco.inventoryservice.application.usecase.InitializeStockUseCase
import com.koosco.inventoryservice.infra.messaging.kafka.message.ProductSkuCreatedEvent
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

/**
 * Catalog Service에서 개별 발행되는 SkuCreatedEvent를 처리하여 재고를 초기
 * 각 SKU마다 개별적으로 이벤트가 발행
 */
@Component
@Validated
class KafkaProductSkuCreatedConsumer(private val initializeStockUseCase: InitializeStockUseCase) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * SKU 생성 이벤트 리스너
     * 각 SKU가 생성될 때마다 개별적으로 재고를 초기화합니다.
     */
    @KafkaListener(
        topics = ["\${inventory.topic.integration.mappings.product.sku.created}"],
        groupId = "inventory-service",
    )
    fun onProductSkuCreated(@Valid event: CloudEvent<ProductSkuCreatedEvent>, ack: Acknowledgment) {
        // CloudEvent의 data 필드가 LinkedHashMap으로 역직렬화되는 경우 처리
        val data = event.data
            ?: run {
                logger.error("ProductSkuCreated is null: eventId=${event.id}")
                ack.acknowledge()
                return
            }

        logger.info(
            "Received ProductSkuCreated: eventId=${event.id}, " +
                "skuId=${data.skuId}, productId=${data.productId}, initialQuantity=${data.initialQuantity}",
        )

        try {
            // 재고 초기화
            initializeStockUseCase.initialize(
                InitStockCommand(
                    data.skuId,
                    data.initialQuantity,
                ),
            )
            ack.acknowledge()

            logger.info(
                "Successfully initialized stock for ProductSku: eventId=${event.id}, skuId=${data.skuId}, " +
                    "productId=${data.productId}, initialQuantity=${data.initialQuantity}",
            )
        } catch (e: Exception) {
            logger.error(
                "Failed to process SKU created event: ${event.id}, " +
                    "skuId=${data.skuId}, productId=${data.productId}",
                e,
            )
            // TODO: 이 SKU에 대해 재고가 ‘최소 1번’은 초기화되어 있어야 한다 -> 재시도 후 DLQ 처리
        }
    }
}

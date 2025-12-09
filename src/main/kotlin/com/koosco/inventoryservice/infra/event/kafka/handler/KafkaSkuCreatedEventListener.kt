package com.koosco.inventoryservice.infra.event.kafka.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.koosco.common.core.event.CloudEvent
import com.koosco.inventoryservice.application.usecase.InitializeStockUseCase
import com.koosco.inventoryservice.infra.event.kafka.event.SkuCreatedEvent
import com.koosco.inventoryservice.infra.event.kafka.event.toDto
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

/**
 * SKU 생성 이벤트 리스너
 * Catalog Service에서 개별 발행되는 SkuCreatedEvent를 처리하여 재고를 초기화합니다.
 * 각 SKU마다 개별적으로 이벤트가 발행되므로, 부분 실패 처리가 용이하고 향후 Outbox 패턴 도입에 유리합니다.
 */
@Component
@Validated
class KafkaSkuCreatedEventListener(
    private val initializeStockUseCase: InitializeStockUseCase,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * SKU 생성 이벤트 리스너
     * 각 SKU가 생성될 때마다 개별적으로 재고를 초기화합니다.
     */
    @KafkaListener(
        topics = ["\${kafka.topics.sku-created}"],
        groupId = "inventory-service",
    )
    fun onSkuCreated(@Valid event: CloudEvent<*>, ack: Acknowledgment) {
        // CloudEvent의 data 필드가 LinkedHashMap으로 역직렬화되는 경우 처리
        val data = event.data?.let { eventData ->
            when (eventData) {
                is SkuCreatedEvent -> eventData
                else -> {
                    // LinkedHashMap 등을 SkuCreatedEvent로 변환
                    logger.debug("Converting data from ${eventData.javaClass.simpleName} to SkuCreatedEvent")
                    objectMapper.convertValue(eventData, SkuCreatedEvent::class.java)
                }
            }
        } ?: run {
            logger.error("Received event with null data: id=${event.id}")
            return
        }

        logger.info(
            "Received SKU created event: id=${event.id}, " +
                "skuId=${data.skuId}, productId=${data.productId}, initialQuantity=${data.initialQuantity}",
        )

        try {
            // 재고 초기화
            initializeStockUseCase.initialize(data.toDto())

            ack.acknowledge()
            logger.info(
                "Successfully initialized stock for SKU: skuId=${data.skuId}, " +
                    "productId=${data.productId}, initialQuantity=${data.initialQuantity}",
            )
        } catch (e: Exception) {
            logger.error(
                "Failed to process SKU created event: ${event.id}, " +
                    "skuId=${data.skuId}, productId=${data.productId}",
                e,
            )
            // TODO: 보상 트랜잭션 또는 DLQ 처리
        }
    }
}

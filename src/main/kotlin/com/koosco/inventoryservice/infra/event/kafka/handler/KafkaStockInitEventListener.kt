package com.koosco.inventoryservice.infra.event.kafka.handler

import com.koosco.common.core.event.CloudEvent
import com.koosco.inventoryservice.application.usecase.InitializeStockUseCase
import com.koosco.inventoryservice.infra.event.kafka.event.StockBulkInitEvent
import com.koosco.inventoryservice.infra.event.kafka.event.StockInitEvent
import com.koosco.inventoryservice.infra.event.kafka.event.toDto
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

/**
 * 상품이 새롭게 생성되었을 때 재고 초기화 이벤트 리스너
 */
@Component
@Validated
class KafkaStockInitEventListener(private val initializeStockUseCase: InitializeStockUseCase) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 재고 초기화 요청 이벤트 리스너
     */
    @KafkaListener(
        topics = ["stock.init.requested"],
        groupId = "inventory-service",
    )
    fun onStockInitRequested(@Valid event: CloudEvent<StockInitEvent>, ack: Acknowledgment) {
        val data = event.data ?: run {
            logger.error("Received event with null data: id=${event.id}")
            return
        }

        logger.info(
            "Received stock init requested event: id=${event.id}, skuId=${data.skuId}, initialQuantity=${data.initialQuantity}",
        )

        try {
            initializeStockUseCase.initialize(data.toDto())
            ack.acknowledge()
        } catch (e: Exception) {
            logger.error("Failed to process stock init requested event: ${event.id}", e)
            // TODO: 보상 트랜잭션 또는 DLQ 처리
        }
    }

    /**
     * 재고 일괄 초기화 요청 이벤트 리스너
     */
    @KafkaListener(
        topics = ["stock.bulk.init.requested"],
        groupId = "inventory-service",
    )
    fun onStockBulkInitRequested(@Valid event: CloudEvent<StockBulkInitEvent>, ack: Acknowledgment) {
        val data = event.data ?: run {
            logger.error("Received event with null data: id=${event.id}")
            return
        }

        logger.info("Received stock bulk init requested event: id=${event.id}, itemCount=${data.items.size}")

        try {
            initializeStockUseCase.bulkInitialize(data.toDto())
            ack.acknowledge()
        } catch (e: Exception) {
            logger.error("Failed to process stock bulk init requested event: ${event.id}", e)
            // TODO: 보상 트랜잭션 또는 DLQ 처리
        }
    }
}

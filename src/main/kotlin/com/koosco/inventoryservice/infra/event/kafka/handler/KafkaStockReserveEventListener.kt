package com.koosco.inventoryservice.infra.event.kafka.handler

import com.koosco.common.core.event.CloudEvent
import com.koosco.inventoryservice.application.usecase.ReserveStockUseCase
import com.koosco.inventoryservice.infra.event.kafka.event.StockReserveCancelEvent
import com.koosco.inventoryservice.infra.event.kafka.event.StockReserveConfirmEvent
import com.koosco.inventoryservice.infra.event.kafka.event.StockReserveEvent
import com.koosco.inventoryservice.infra.event.kafka.event.toDto
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

/**
 * 재고 예약, 예약 확정, 예약 취소 이벤트 처리
 */
@Component
@Validated
class KafkaStockReserveEventListener(private val reserveStockUseCase: ReserveStockUseCase) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 재고 예약 요청 이벤트 리스너
     */
    @KafkaListener(
        topics = ["\${kafka.topics.stock-reserve}"],
        groupId = "inventory-service",
    )
    fun onStockReserveRequested(@Valid event: CloudEvent<StockReserveEvent>, ack: Acknowledgment) {
        val data = event.data ?: run {
            logger.error("Received event with null data: id=${event.id}")
            return
        }

        logger.info("Received stock reserve requested event: id=${event.id}, orderId=${data.orderId}")

        try {
            reserveStockUseCase.reserve(data.toDto())
            ack.acknowledge()
        } catch (e: Exception) {
            logger.error("Failed to process stock reserve requested event: ${event.id}", e)
            // TODO: 보상 트랜잭션 또는 DLQ 처리
        }
    }

    /**
     * 재고 예약 확정 이벤트 리스너 (결제 성공)
     */
    @KafkaListener(
        topics = ["\${kafka.topics.stock-confirm}"],
        groupId = "inventory-service",
    )
    fun onStockReserveConfirmed(@Valid event: CloudEvent<StockReserveConfirmEvent>, ack: Acknowledgment) {
        val data = event.data ?: run {
            logger.error("Received event with null data: id=${event.id}")
            return
        }

        logger.info("Received stock reserve confirmed event: id=${event.id}, orderId=${data.orderId}")

        try {
            reserveStockUseCase.confirm(data.toDto())
            ack.acknowledge()
        } catch (e: Exception) {
            logger.error("Failed to process stock reserve confirmed event: ${event.id}", e)
            // TODO: 보상 트랜잭션 또는 DLQ 처리
        }
    }

    /**
     * 재고 예약 취소 이벤트 리스너 (결제 실패/주문 취소)
     */
    @KafkaListener(
        topics = ["\${kafka.topics.stock-cancel}"],
        groupId = "inventory-service",
    )
    fun onStockReserveCancelled(@Valid event: CloudEvent<StockReserveCancelEvent>, ack: Acknowledgment) {
        val data = event.data ?: run {
            logger.error("Received event with null data: id=${event.id}")
            return
        }

        logger.info("Received stock reserve cancelled event: id=${event.id}, orderId=${data.orderId}")

        try {
            reserveStockUseCase.cancel(data.toDto())
            ack.acknowledge()
        } catch (e: Exception) {
            logger.error("Failed to process stock reserve cancelled event: ${event.id}", e)
            // TODO: 보상 트랜잭션 또는 DLQ 처리
        }
    }
}

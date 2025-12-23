package com.koosco.inventoryservice.application.usecase

import com.koosco.common.core.annotation.UseCase
import com.koosco.common.core.exception.NotFoundException
import com.koosco.inventoryservice.application.command.ConfirmStockCommand
import com.koosco.inventoryservice.application.contract.outbound.inventory.StockConfirmFailedEvent
import com.koosco.inventoryservice.application.contract.outbound.inventory.StockConfirmedEvent
import com.koosco.inventoryservice.application.port.IntegrationEventPublisher
import com.koosco.inventoryservice.application.port.InventoryRepository
import com.koosco.inventoryservice.common.InventoryErrorCode
import com.koosco.inventoryservice.common.MessageContext
import com.koosco.inventoryservice.domain.exception.NotEnoughStockException
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : ConfirmStockUseCase
 * author         : koo
 * date           : 2025. 12. 22. 오전 6:33
 * description    : 예약된 재고 확정 Usecase
 */
@UseCase
class ConfirmStockUseCase(
    private val inventoryRepository: InventoryRepository,
    private val integrationEventPublisher: IntegrationEventPublisher,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 예약된 재고 확정 처리 (결제 성공 시)
     */
    @Transactional
    fun execute(command: ConfirmStockCommand, context: MessageContext) {
        // === Phase 1: Lock 획득 및 검증 (상태 변경 없음) ===

        // 1-1. 데드락 방지를 위해 skuId 순으로 정렬
        val sortedItems = command.items.sortedBy { it.skuId }
        val skuIds = sortedItems.map { it.skuId }

        // 1-2. Pessimistic lock을 사용하여 모든 재고 조회
        val inventories = inventoryRepository.findAllBySkuIdInWithLock(skuIds)

        // 1-3. 존재하지 않는 SKU 확인
        val foundSkuIds = inventories.map { it.skuId }.toSet()
        val notFoundSkuIds = skuIds.filterNot { it in foundSkuIds }

        if (notFoundSkuIds.isNotEmpty()) {
            integrationEventPublisher.publish(
                StockConfirmFailedEvent(
                    orderId = command.orderId,
                    reservationId = command.reservationId,
                    reason = "INVENTORY_NOT_FOUND: $notFoundSkuIds",
                    correlationId = context.correlationId,
                    causationId = context.causationId,
                ),
            )

            logger.warn(
                "Stock confirmation failed - inventory not found: orderId=${command.orderId}, notFound=$notFoundSkuIds",
            )
            throw NotFoundException(
                InventoryErrorCode.INVENTORY_NOT_FOUND,
                "Inventory not found for SKUs: $notFoundSkuIds",
            )
        }

        // 1-4. 예약 재고 충분성 검증 (상태 변경 없음)
        val inventoryMap = inventories.associateBy { it.skuId }
        val insufficientReserved = mutableListOf<String>()

        sortedItems.forEach { item ->
            val inventory = inventoryMap[item.skuId]!!
            val reserved = inventory.stock.reserved

            if (reserved < item.quantity) {
                insufficientReserved.add("${item.skuId}(requested=${item.quantity}, reserved=$reserved)")
            }
        }

        // 1-5. 검증 실패 시 실패 이벤트 발행 및 예외 던지기
        if (insufficientReserved.isNotEmpty()) {
            integrationEventPublisher.publish(
                StockConfirmFailedEvent(
                    orderId = command.orderId,
                    reservationId = command.reservationId,
                    reason = "NOT_ENOUGH_RESERVED: ${insufficientReserved.joinToString(", ")}",
                    correlationId = context.correlationId,
                    causationId = context.causationId,
                ),
            )

            logger.warn("Stock confirmation failed: orderId=${command.orderId}, insufficient=$insufficientReserved")
            throw NotEnoughStockException(
                message = "Not enough reserved stock: $insufficientReserved",
            )
        }

        // === Phase 2: 모든 검증 통과 시 상태 변경 ===

        sortedItems.forEach { item ->
            val inventory = inventoryMap[item.skuId]!!
            inventory.confirm(item.quantity)
            inventoryRepository.save(inventory)
        }

        // 2-1. 성공 이벤트 발행
        val confirmedItems = sortedItems.map { item ->
            StockConfirmedEvent.ConfirmedItem(item.skuId, item.quantity)
        }

        integrationEventPublisher.publish(
            StockConfirmedEvent(
                orderId = command.orderId,
                reservationId = command.reservationId,
                items = confirmedItems,
                correlationId = context.correlationId,
                causationId = context.causationId,
            ),
        )

        logger.info("Stock confirmed successfully: orderId=${command.orderId}, items=${confirmedItems.size}")
    }
}

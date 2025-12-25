package com.koosco.inventoryservice.application.usecase

import com.koosco.common.core.annotation.UseCase
import com.koosco.common.core.event.DomainEvent
import com.koosco.common.core.exception.NotFoundException
import com.koosco.inventoryservice.application.command.ReserveStockCommand
import com.koosco.inventoryservice.application.contract.outbound.inventory.StockReservationFailedEvent
import com.koosco.inventoryservice.application.contract.outbound.inventory.StockReservedEvent
import com.koosco.inventoryservice.application.contract.outbound.inventory.StockReservedEvent.Item
import com.koosco.inventoryservice.application.port.IntegrationEventPublisher
import com.koosco.inventoryservice.application.port.InventoryRepositoryPort
import com.koosco.inventoryservice.common.InventoryErrorCode
import com.koosco.inventoryservice.common.MessageContext
import com.koosco.inventoryservice.domain.event.StockReserved
import com.koosco.inventoryservice.domain.exception.NotEnoughStockException
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : ReserveStockUseCase
 * author         : koo
 * date           : 2025. 12. 22. 오전 6:33
 * description    : 재고 예약 Usecase
 */
@UseCase
class ReserveStockUseCase(
    private val inventoryRepository: InventoryRepositoryPort,
    private val integrationEventPublisher: IntegrationEventPublisher,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun execute(command: ReserveStockCommand, context: MessageContext) {
        // === Phase 1: Lock 획득 및 검증 (상태 변경 없음) ===

        // 1-1. 데드락 방지를 위해 skuId 순으로 정렬
        val sortedItems = command.items.sortedBy { it.skuId }
        val skuIds = sortedItems.map { it.skuId }

        // 1-2. Pessimistic lock을 사용하여 모든 재고 조회
        val inventories = inventoryRepository.findAllBySkuIdInWithLock(skuIds)

        // 1-3. 존재하지 않는 SKU 확인
        val foundSkuIds = inventories.map { it.skuId }.toSet()
        val notFoundSkuIds = skuIds.filterNot { it in foundSkuIds }

        logger.info("foundSkuIds=${foundSkuIds.size}, notFoundSkuIds=${notFoundSkuIds.size}")

        if (notFoundSkuIds.isNotEmpty()) {
            val failedItems = notFoundSkuIds.map { skuId ->
                val item = sortedItems.first { it.skuId == skuId }
                StockReservationFailedEvent.FailedItem(
                    skuId = skuId,
                    requestedQuantity = item.quantity,
                    availableQuantity = null,
                )
            }

            integrationEventPublisher.publish(
                StockReservationFailedEvent(
                    orderId = command.orderId,
                    reason = "INVENTORY_NOT_FOUND",
                    failedItems = failedItems,
                    correlationId = context.correlationId,
                    causationId = context.causationId,
                ),
            )

            logger.warn(
                "Stock reservation failed - inventory not found: orderId=${command.orderId}, notFound=$notFoundSkuIds",
            )
            throw NotFoundException(
                InventoryErrorCode.INVENTORY_NOT_FOUND,
                "Inventory not found for SKUs: $notFoundSkuIds",
            )
        }

        // 1-4. 재고 가용성 검증 (상태 변경 없음)
        val inventoryMap = inventories.associateBy { it.skuId }
        val failedItems = mutableListOf<StockReservationFailedEvent.FailedItem>()

        sortedItems.forEach { item ->
            val inventory = inventoryMap[item.skuId]!!
            val available = inventory.stock.available

            if (available < item.quantity) {
                failedItems.add(
                    StockReservationFailedEvent.FailedItem(
                        skuId = item.skuId,
                        requestedQuantity = item.quantity,
                        availableQuantity = available,
                    ),
                )
            }
        }

        // 1-5. 검증 실패 시 실패 이벤트 발행 및 예외 던지기
        if (failedItems.isNotEmpty()) {
            integrationEventPublisher.publish(
                StockReservationFailedEvent(
                    orderId = command.orderId,
                    reason = "NOT_ENOUGH_STOCK",
                    failedItems = failedItems,
                    correlationId = context.correlationId,
                    causationId = context.causationId,
                ),
            )

            logger.warn("Stock reservation failed: orderId=${command.orderId}, failedItems=${failedItems.size}")
            throw NotEnoughStockException(
                message = "Stock reservation failed for ${failedItems.size} item(s): ${failedItems.map { it.skuId }}",
            )
        }

        // === Phase 2: 모든 검증 통과 시 상태 변경 ===

        sortedItems.forEach { item ->
            val inventory = inventoryMap[item.skuId]!!
            inventory.reserve(item.quantity)
            inventoryRepository.save(inventory)
        }

        // 2-1. 도메인 이벤트 수집 및 Integration Event 발행
        val domainEvents: List<DomainEvent> = inventories.flatMap { it.pullDomainEvents() }
        logger.info(
            "domainEvents=${domainEvents.size}, domainEvents=${domainEvents.joinToString {
                it::class.simpleName ?: "null"
            }}",
        )

        val items = domainEvents
            .filterIsInstance<StockReserved>()
            .map { Item(it.skuId, it.quantity) }

        integrationEventPublisher.publish(
            StockReservedEvent(
                orderId = command.orderId,
                items = items,
                correlationId = context.correlationId,
                causationId = context.causationId,
            ),
        )

        logger.info("Stock reserved successfully: orderId=${command.orderId}, items=${items.size}")
    }
}

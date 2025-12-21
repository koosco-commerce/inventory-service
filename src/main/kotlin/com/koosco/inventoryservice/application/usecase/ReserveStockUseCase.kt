package com.koosco.inventoryservice.application.usecase

import com.koosco.common.core.annotation.UseCase
import com.koosco.common.core.exception.NotFoundException
import com.koosco.inventoryservice.application.command.CancelStockCommand
import com.koosco.inventoryservice.application.command.ConfirmStockCommand
import com.koosco.inventoryservice.application.command.ReserveStockCommand
import com.koosco.inventoryservice.application.event.DomainEventPublisher
import com.koosco.inventoryservice.application.event.IntegrationEventPublisher
import com.koosco.inventoryservice.application.repository.InventoryRepository
import com.koosco.inventoryservice.common.InventoryErrorCode
import com.koosco.inventoryservice.domain.exception.NotEnoughStockException
import com.koosco.inventoryservice.infra.messaging.kafka.message.StockConfirmFailedEvent
import com.koosco.inventoryservice.infra.messaging.kafka.message.StockReservationFailedEvent
import org.springframework.transaction.annotation.Transactional

@UseCase
class ReserveStockUseCase(
    private val inventoryRepository: InventoryRepository,
    private val domainEventPublisher: DomainEventPublisher,
    private val integrationEventPublisher: IntegrationEventPublisher,
) {

    /**
     * 재고 예약 처리
     */
    @Transactional
    fun reserve(command: ReserveStockCommand) {
        try {
            val inventory = inventoryRepository.findBySkuIdOrNull(command.skuId)
                ?: throw NotFoundException(
                    InventoryErrorCode.INVENTORY_NOT_FOUND,
                    "Inventory not found. skuId=${command.skuId}",
                )

            inventory.reserve(command.quantity)

            // 재고 예약 성공에 대한 이벤트 발행
            domainEventPublisher.publishAll(inventory.pullDomainEvents())
        } catch (e: NotEnoughStockException) {
            integrationEventPublisher.publish(
                StockReservationFailedEvent(
                    orderId = command.orderId,
                    skuId = command.skuId,
                    reason = "NOT_ENOUGH_STOCK",
                ),
            )

            throw e
        }
    }

    /**
     * 예약된 재고 확정 처리 (결제 성공 시)
     */
    @Transactional
    fun confirm(command: ConfirmStockCommand) {
        try {
            val inventory = inventoryRepository.findBySkuIdOrNull(command.skuId)
                ?: throw NotFoundException(
                    InventoryErrorCode.INVENTORY_NOT_FOUND,
                    "Inventory not found. skuId=${command.skuId}",
                )

            inventory.confirm(command.quantity)

            inventoryRepository.save(inventory)

            domainEventPublisher.publishAll(inventory.pullDomainEvents())
        } catch (e: NotEnoughStockException) {
            integrationEventPublisher.publish(
                StockConfirmFailedEvent(
                    orderId = command.orderId,
                    skuId = command.skuId,
                    reason = "NOT_ENOUGH_STOCK",
                ),
            )

            throw e
        }
    }

    /**
     * 예약 취소 처리 (결제 실패/주문 취소)
     */
    @Transactional
    fun cancel(command: CancelStockCommand) {
        val inventory = inventoryRepository.findBySkuIdOrNull(command.skuId)
            ?: return // 해당 재고가 이미 없다면 성공 처리

        inventory.cancelReservation(command.quantity)

        inventoryRepository.save(inventory)
    }
}

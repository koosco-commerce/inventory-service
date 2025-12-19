package com.koosco.inventoryservice.application.usecase

import com.koosco.common.core.annotation.UseCase
import com.koosco.common.core.exception.NotFoundException
import com.koosco.inventoryservice.application.command.StockCancelCommand
import com.koosco.inventoryservice.application.command.StockConfirmCommand
import com.koosco.inventoryservice.application.command.StockReserveCommand
import com.koosco.inventoryservice.application.event.DomainEventPublisher
import com.koosco.inventoryservice.application.repository.InventoryRepository
import com.koosco.inventoryservice.common.InventoryErrorCode
import org.springframework.transaction.annotation.Transactional

@UseCase
class ReserveStockUseCase(
    private val inventoryRepository: InventoryRepository,
    private val domainEventPublisher: DomainEventPublisher,
) {

    /**
     * 재고 예약 처리
     */
    @Transactional
    fun reserve(command: StockReserveCommand) {
        val inventory = inventoryRepository.findBySkuIdOrNull(command.skuId)
            ?: throw NotFoundException(
                InventoryErrorCode.INVENTORY_NOT_FOUND,
                "Inventory not found. skuId=${command.skuId}",
            )

        inventory.reserve(command.quantity)

        // 재고 예약 성공에 대한 이벤트 발행
        domainEventPublisher.publishAll(inventory.pullDomainEvents())
    }

    /**
     * 예약된 재고 확정 처리 (결제 성공 시)
     */
    @Transactional
    fun confirm(command: StockConfirmCommand) {
        val inventory = inventoryRepository.findBySkuIdOrNull(command.skuId)
            ?: throw NotFoundException(
                InventoryErrorCode.INVENTORY_NOT_FOUND,
                "Inventory not found. skuId=${command.skuId}",
            )

        inventory.confirm(command.quantity)

        inventoryRepository.save(inventory)

        domainEventPublisher.publishAll(inventory.pullDomainEvents())
    }

    /**
     * 예약 취소 처리 (결제 실패/주문 취소)
     */
    @Transactional
    fun cancel(command: StockCancelCommand) {
        val inventory = inventoryRepository.findBySkuIdOrNull(command.skuId)
            ?: return // 해당 재고가 이미 없다면 성공 처리

        inventory.cancelReservation(command.quantity)

        inventoryRepository.save(inventory)
    }
}

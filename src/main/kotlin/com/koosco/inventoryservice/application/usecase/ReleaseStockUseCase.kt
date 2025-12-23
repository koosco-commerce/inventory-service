package com.koosco.inventoryservice.application.usecase

import com.koosco.common.core.annotation.UseCase
import com.koosco.inventoryservice.application.command.CancelStockCommand
import com.koosco.inventoryservice.application.port.IntegrationEventPublisher
import com.koosco.inventoryservice.application.port.InventoryRepository
import com.koosco.inventoryservice.common.MessageContext
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : CancelStockUseCase
 * author         : koo
 * date           : 2025. 12. 22. 오전 6:34
 * description    : 예약된 재고 취소 Usecase
 */
@UseCase
class ReleaseStockUseCase(
    private val inventoryRepository: InventoryRepository,
    private val integrationEventPublisher: IntegrationEventPublisher,
) {
    /**
     * 예약 취소 처리 (결제 실패/주문 취소)
     */
    @Transactional
    fun execute(command: CancelStockCommand, context: MessageContext) {
        // 각 상품별 재고 취소
        command.items.forEach { item ->
            val inventory = inventoryRepository.findBySkuIdOrNull(item.skuId)
                ?: return@forEach // 해당 재고가 이미 없다면 건너뜀

            inventory.cancelReservation(item.quantity)

            inventoryRepository.save(inventory)
        }
    }
}

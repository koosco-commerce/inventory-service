package com.koosco.inventoryservice.application.usecase

import com.koosco.common.core.exception.NotFoundException
import com.koosco.inventoryservice.application.dto.StockReserveCancelDto
import com.koosco.inventoryservice.application.dto.StockReserveConfirmDto
import com.koosco.inventoryservice.application.dto.StockReserveRequestDto
import com.koosco.inventoryservice.application.repository.InventoryRepository
import com.koosco.inventoryservice.common.InventoryErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReserveStockUseCase(private val inventoryRepository: InventoryRepository) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 재고 예약 처리
     */
    @Transactional
    fun reserve(dto: StockReserveRequestDto) {
        val inventory = inventoryRepository.findBySkuIdOrNull(dto.skuId)
            ?: throw NotFoundException(
                InventoryErrorCode.INVENTORY_NOT_FOUND,
                "해당하는 재고를 찾을 수 없습니다. skuId: ${dto.skuId}",
            )

        try {
            inventory.reserve(dto.quantity)
            inventoryRepository.save(inventory)

            logger.info(
                "Stock reserved successfully: skuId=${dto.skuId}, quantity=${dto.quantity}, orderId=${dto.orderId}",
            )
        } catch (e: Exception) {
            logger.error(
                "Failed to reserve stock: skuId=${dto.skuId}, quantity=${dto.quantity}, orderId=${dto.orderId}",
                e,
            )
            throw e
        }
    }

    /**
     * 예약 확정 처리 (결제 성공 시)
     */
    @Transactional
    fun confirm(dto: StockReserveConfirmDto) {
        val inventory = inventoryRepository.findBySkuIdOrNull(dto.skuId)
            ?: throw NotFoundException(
                InventoryErrorCode.INVENTORY_NOT_FOUND,
                "해당하는 재고를 찾을 수 없습니다. skuId: ${dto.skuId}",
            )

        try {
            inventory.confirm(dto.quantity)
            inventoryRepository.save(inventory)

            logger.info(
                "Stock reservation confirmed: skuId=${dto.skuId}, quantity=${dto.quantity}, orderId=${dto.orderId}",
            )
        } catch (e: Exception) {
            logger.error(
                "Failed to confirm stock reservation: skuId=${dto.skuId}, quantity=${dto.quantity}, orderId=${dto.orderId}\"",
                e,
            )
            throw e
        }
    }

    /**
     * 예약 취소 처리 (결제 실패/주문 취소)
     */
    @Transactional
    fun cancel(dto: StockReserveCancelDto) {
        val inventory = inventoryRepository.findBySkuIdOrNull(dto.skuId)
            ?: throw NotFoundException(
                InventoryErrorCode.INVENTORY_NOT_FOUND,
                "해당하는 재고를 찾을 수 없습니다. skuId: ${dto.skuId}",
            )

        try {
            inventory.cancelReservation(dto.quantity)
            inventoryRepository.save(inventory)

            logger.info(
                "Stock reservation cancelled: skuId=${dto.skuId}, quantity=${dto.quantity}, orderId=${dto.orderId}",
            )
        } catch (e: Exception) {
            logger.error(
                "Failed to cancel stock reservation: skuId=${dto.skuId}, quantity=${dto.quantity}, orderId=${dto.orderId}",
                e,
            )
            throw e
        }
    }
}

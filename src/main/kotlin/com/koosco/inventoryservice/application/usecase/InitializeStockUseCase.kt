package com.koosco.inventoryservice.application.usecase

import com.koosco.common.core.exception.BadRequestException
import com.koosco.inventoryservice.application.dto.StockInitDto
import com.koosco.inventoryservice.application.repository.InventoryRepository
import com.koosco.inventoryservice.common.InventoryErrorCode
import com.koosco.inventoryservice.domain.entity.Inventory
import com.koosco.inventoryservice.domain.vo.Stock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 재고 초기화 유스케이스
 * 새로운 상품이 생성되었을 때 재고를 초기화합니다.
 */
@Service
class InitializeStockUseCase(private val inventoryRepository: InventoryRepository) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 재고 초기화 처리
     *
     * @param dto 재고 초기화 정보 (skuId, initialQuantity)
     * @throws BadRequestException 이미 재고가 존재하는 경우 또는 초기 수량이 유효하지 않은 경우
     */
    @Transactional
    fun initialize(dto: StockInitDto) {
        // 이미 재고가 존재하는지 확인
        if (inventoryRepository.existsBySkuId(dto.skuId)) {
            logger.warn("Inventory already exists for skuId: ${dto.skuId}")
            throw BadRequestException(
                InventoryErrorCode.INVENTORY_ALREADY_EXISTS,
                "이미 재고가 존재합니다. skuId: ${dto.skuId}",
            )
        }

        try {
            // 새로운 재고 생성 (total = initialQuantity, reserved = 0)
            val newInventory = Inventory(
                skuId = dto.skuId,
                stock = Stock(total = dto.initialQuantity),
            )

            inventoryRepository.save(newInventory)

            logger.info("Stock initialized successfully: skuId=${dto.skuId}, initialQuantity=${dto.initialQuantity}")
        } catch (e: Exception) {
            logger.error(
                "Failed to initialize stock: skuId=${dto.skuId}, initialQuantity=${dto.initialQuantity}",
                e,
            )
            throw e
        }
    }
}

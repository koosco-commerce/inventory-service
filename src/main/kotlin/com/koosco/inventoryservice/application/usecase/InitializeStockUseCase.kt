package com.koosco.inventoryservice.application.usecase

import com.koosco.common.core.annotation.UseCase
import com.koosco.common.core.exception.BadRequestException
import com.koosco.inventoryservice.application.command.InitStockCommand
import com.koosco.inventoryservice.application.repository.InventoryRepository
import com.koosco.inventoryservice.domain.entity.Inventory
import com.koosco.inventoryservice.domain.exception.InventoryAlreadyInitialized
import com.koosco.inventoryservice.domain.vo.Stock
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional

/**
 * 재고 초기화 유스케이스
 * 새로운 상품이 생성되었을 때 재고를 초기화합니다.
 */
@UseCase
class InitializeStockUseCase(private val inventoryRepository: InventoryRepository) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 재고 초기화 처리
     *
     * @param command 재고 초기화 정보 (skuId, initialQuantity)
     * @throws BadRequestException 이미 재고가 존재하는 경우 또는 초기 수량이 유효하지 않은 경우
     */
    @Transactional
    fun initialize(command: InitStockCommand) {
        // 이미 재고가 존재하는지 확인
        if (inventoryRepository.existsBySkuId(command.skuId)) {
            logger.warn("Inventory already exists for skuId: ${command.skuId}")
            throw InventoryAlreadyInitialized(
                "Inventory already exists. skuId: ${command.skuId}",
            )
        }

        val newInventory = Inventory(
            skuId = command.skuId,
            stock = Stock(total = command.quantity),
        )

        inventoryRepository.save(newInventory)
    }
}

package com.koosco.inventoryservice.application.usecase

import com.koosco.common.core.annotation.UseCase
import com.koosco.inventoryservice.application.dto.AdjustStockDto
import com.koosco.inventoryservice.application.repository.InventoryRepository
import org.springframework.transaction.annotation.Transactional

@UseCase
class AdjustStockUseCase(private val inventoryRepository: InventoryRepository) {

    @Transactional
    fun adjustSingle(dto: AdjustStockDto) {
        val inventory = inventoryRepository.findBySkuIdOrNull(dto.skuId)
            ?: throw IllegalArgumentException("Inventory not found for skuId: ${dto.skuId}")

        inventory.updateStock(dto.quantity)
    }

    @Transactional
    fun adjustBulk(dtos: List<AdjustStockDto>) {
        // 1. 모든 SKU ID 수집
        val skuIds = dtos.map { it.skuId }

        // 2. 한 번의 쿼리로 모든 Inventory 조회
        val inventories = inventoryRepository.findAllBySkuIdIn(skuIds)
        val inventoryMap = inventories.associateBy { it.skuId }

        // 3. 사전 검증: 존재하지 않는 SKU 확인
        val missingSkuIds = skuIds - inventoryMap.keys
        if (missingSkuIds.isNotEmpty()) {
            throw com.koosco.common.core.exception.BadRequestException(
                com.koosco.inventoryservice.common.InventoryErrorCode.INVENTORY_NOT_FOUND,
                "다음 SKU의 재고 정보를 찾을 수 없습니다: ${missingSkuIds.joinToString()}",
            )
        }

        // 4. 모든 SKU가 존재하면 처리
        dtos.forEach { dto ->
            val inventory = inventoryMap[dto.skuId]!! // 사전 검증으로 null일 수 없음
            inventory.updateStock(dto.quantity)
        }
    }
}

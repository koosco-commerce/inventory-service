package com.koosco.inventoryservice.application.usecase

import com.koosco.common.core.annotation.UseCase
import com.koosco.inventoryservice.application.dto.GetInventoriesDto
import com.koosco.inventoryservice.application.dto.GetInventoryDto
import com.koosco.inventoryservice.application.dto.InventoryDto
import com.koosco.inventoryservice.application.repository.InventoryRepository
import org.springframework.transaction.annotation.Transactional

@UseCase
class GetInventoryUseCase(private val inventoryRepository: InventoryRepository) {

    @Transactional(readOnly = true)
    fun getInventoryBySkuId(dto: GetInventoryDto): InventoryDto {
        val inventory = (
            inventoryRepository.findBySkuIdOrNull(dto.skuId)
                ?: throw IllegalArgumentException("Inventory with SKU ID ${dto.skuId} not found")
            )

        return InventoryDto(
            skuId = inventory.skuId,
            totalStock = inventory.stock.total,
            reservedStock = inventory.stock.reserved,
            availableStock = inventory.stock.available,
        )
    }

    @Transactional(readOnly = true)
    fun getInventoriesBySkuIds(dto: GetInventoriesDto): List<InventoryDto> {
        val inventories = inventoryRepository.findAllBySkuIdIn(dto.skuIds)

        return inventories.map {
            InventoryDto(
                skuId = it.skuId,
                totalStock = it.stock.total,
                reservedStock = it.stock.reserved,
                availableStock = it.stock.available,
            )
        }
    }
}

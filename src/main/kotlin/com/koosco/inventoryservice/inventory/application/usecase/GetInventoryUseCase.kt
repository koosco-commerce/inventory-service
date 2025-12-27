package com.koosco.inventoryservice.inventory.application.usecase

import com.koosco.common.core.annotation.UseCase
import com.koosco.inventoryservice.inventory.application.command.GetInventoriesCommand
import com.koosco.inventoryservice.inventory.application.command.GetInventoryCommand
import com.koosco.inventoryservice.inventory.application.port.InventoryRepositoryPort
import com.koosco.inventoryservice.inventory.application.result.GetInventoryResult
import org.springframework.transaction.annotation.Transactional

@UseCase
class GetInventoryUseCase(private val inventoryRepository: InventoryRepositoryPort) {

    @Transactional(readOnly = true)
    fun execute(command: GetInventoryCommand): GetInventoryResult {
        val inventory = (
            inventoryRepository.findBySkuIdOrNull(command.skuId)
                ?: throw IllegalArgumentException("Inventory with SKU ID ${command.skuId} not found")
            )

        return GetInventoryResult(
            skuId = inventory.skuId,
            totalStock = inventory.stock.total,
            reservedStock = inventory.stock.reserved,
            availableStock = inventory.stock.available,
        )
    }

    @Transactional(readOnly = true)
    fun execute(command: GetInventoriesCommand): List<GetInventoryResult> {
        val inventories = inventoryRepository.findAllBySkuIdIn(command.skuIds)

        return inventories.map {
            GetInventoryResult(
                skuId = it.skuId,
                totalStock = it.stock.total,
                reservedStock = it.stock.reserved,
                availableStock = it.stock.available,
            )
        }
    }
}

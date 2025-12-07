package com.koosco.inventoryservice.application.repository

import com.koosco.inventoryservice.domain.entity.Inventory

interface InventoryRepository {

    fun save(inventory: Inventory)

    fun saveAll(inventories: List<Inventory>)

    fun findBySkuIdOrNull(skuId: String): Inventory?

    fun findAllBySkuIdIn(skuIds: List<String>): List<Inventory>

    fun existsBySkuId(skuId: String): Boolean
}

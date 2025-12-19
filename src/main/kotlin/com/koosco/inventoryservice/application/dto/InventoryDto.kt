package com.koosco.inventoryservice.application.dto

data class InventoryDto(val skuId: String, val totalStock: Int, val reservedStock: Int, val availableStock: Int)

data class GetInventoryCommand(val skuId: String)

data class GetInventoriesCommand(val skuIds: List<String>)

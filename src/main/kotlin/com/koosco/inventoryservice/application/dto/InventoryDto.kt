package com.koosco.inventoryservice.application.dto

data class InventoryDto(val skuId: String, val totalStock: Int, val reservedStock: Int, val availableStock: Int)

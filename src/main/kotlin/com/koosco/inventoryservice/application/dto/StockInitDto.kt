package com.koosco.inventoryservice.application.dto

data class StockInitDto(val skuId: String, val initialQuantity: Int)

data class StockBulkInitDto(val items: List<StockInitItemDto>) {
    data class StockInitItemDto(val skuId: String, val initialQuantity: Int)
}

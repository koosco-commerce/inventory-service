package com.koosco.inventoryservice.application.dto

data class AdjustStockDto(val skuId: String, val quantity: Int)

data class AddStockDto(val skuId: String, val addingQuantity: Int)

data class ReduceStockDto(val skuId: String, val reducingQuantity: Int)

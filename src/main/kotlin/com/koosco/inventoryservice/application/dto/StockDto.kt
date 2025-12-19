package com.koosco.inventoryservice.application.dto

data class AdjustStockCommand(val skuId: String, val quantity: Int)

data class AddStockCommand(val skuId: String, val addingQuantity: Int)

data class ReduceStockCommand(val skuId: String, val reducingQuantity: Int)

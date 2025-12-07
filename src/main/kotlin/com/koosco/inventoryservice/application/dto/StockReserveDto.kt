package com.koosco.inventoryservice.application.dto

data class StockReserveRequestDto(val orderId: Long, val skuId: String, val quantity: Int)

data class StockReserveConfirmDto(val orderId: Long, val skuId: String, val quantity: Int)

data class StockReserveCancelDto(val orderId: Long, val skuId: String, val quantity: Int)

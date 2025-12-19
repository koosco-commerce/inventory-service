package com.koosco.inventoryservice.application.command

/**
 * fileName       : StockCommand
 * author         : koo
 * date           : 2025. 12. 19. 오후 12:46
 * description    :
 */
data class StockInitCommand(val skuId: String, val quantity: Int)

data class StockReserveCommand(val orderId: Long, val skuId: String, val quantity: Int)

data class StockConfirmCommand(val orderId: Long, val skuId: String, val quantity: Int)

data class StockCancelCommand(val orderId: Long, val skuId: String, val quantity: Int)

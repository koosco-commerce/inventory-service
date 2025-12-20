package com.koosco.inventoryservice.application.command

/**
 * fileName       : StockCommand
 * author         : koo
 * date           : 2025. 12. 19. 오후 12:46
 * description    :
 */
data class InitStockCommand(val skuId: String, val quantity: Int)

data class ReserveStockCommand(val orderId: Long, val skuId: String, val quantity: Int)

data class ConfirmStockCommand(val orderId: Long, val skuId: String, val quantity: Int)

data class CancelStockCommand(val orderId: Long, val skuId: String, val quantity: Int)

data class AdjustStockCommand(val skuId: String, val quantity: Int)

data class AddStockCommand(val skuId: String, val addingQuantity: Int)

data class ReduceStockCommand(val skuId: String, val reducingQuantity: Int)

data class GetInventoryCommand(val skuId: String)

data class GetInventoriesCommand(val skuIds: List<String>)

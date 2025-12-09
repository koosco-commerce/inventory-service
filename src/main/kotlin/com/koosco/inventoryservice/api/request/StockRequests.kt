package com.koosco.inventoryservice.api.request

import com.koosco.inventoryservice.application.dto.AddStockDto
import com.koosco.inventoryservice.application.dto.AdjustStockDto
import com.koosco.inventoryservice.application.dto.ReduceStockDto

/**
 * 재고 수량 조정 요청 DTO
 */
data class AdjustStockRequest(val quantity: Int)

fun AdjustStockRequest.toDto(skuId: String): AdjustStockDto = AdjustStockDto(skuId = skuId, quantity = quantity)

/**
 * 대량 재고 수량 조정 요청 DTO
 */
data class BulkAdjustStockRequest(val adjustments: List<BulkAdjustStock>)

data class BulkAdjustStock(val skuId: String, val quantity: Int)

fun BulkAdjustStock.toDto(): AdjustStockDto = AdjustStockDto(skuId = skuId, quantity = quantity)

/**
 * 재고 추가 요청 DTO
 */
data class AddStockRequest(val addingQuantity: Int)

fun AddStockRequest.toDto(skuId: String) = AddStockDto(skuId = skuId, addingQuantity = addingQuantity)

/**
 * 대량 재고 추가 요청 DTO
 */
data class BulkAddStockRequest(val items: List<BulkAddStock>)

data class BulkAddStock(val skuId: String, val quantity: Int)

fun BulkAddStock.toDto(): AddStockDto = AddStockDto(skuId = skuId, addingQuantity = quantity)

/**
 * 재고 감소 요청 DTO
 */
data class ReduceStockRequest(val reducingQuantity: Int)

fun ReduceStockRequest.toDto(skuId: String) = ReduceStockDto(skuId = skuId, reducingQuantity = reducingQuantity)

/**
 * 대량 재고 감소 요청 DTO
 */
data class BulkReduceStockRequest(val items: List<BulkReduceStock>)

data class BulkReduceStock(val skuId: String, val quantity: Int)

fun BulkReduceStock.toDto(): ReduceStockDto = ReduceStockDto(skuId = skuId, reducingQuantity = quantity)

package com.koosco.inventoryservice.api.response

import com.koosco.inventoryservice.application.dto.InventoryDto

/**
 * 단일 재고 조회 응답 DTO
 */
data class GetInventoryResponse(
    val skuId: String,
    val totalStock: Int,
    val reservedStock: Int,
    val availableStock: Int,
) {
    companion object {
        fun toResponse(dto: InventoryDto): GetInventoryResponse = GetInventoryResponse(
            skuId = dto.skuId,
            totalStock = dto.totalStock,
            reservedStock = dto.reservedStock,
            availableStock = dto.availableStock,
        )
    }
}

/**
 * 대량 재고 조회 응답 DTO
 */
data class GetInventoriesResponse(val inventories: List<InventoryDto>) {
    companion object {
        fun toResponse(dtos: List<InventoryDto>): GetInventoriesResponse = GetInventoriesResponse(
            inventories = dtos,
        )
    }
}

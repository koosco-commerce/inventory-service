package com.koosco.inventoryservice.infra.event.kafka.event

import com.koosco.inventoryservice.application.dto.StockBulkInitDto
import com.koosco.inventoryservice.application.dto.StockInitDto
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

/**
 * 재고 초기화 이벤트 DTO (상품 생성 시)
 */
data class StockInitEvent(
    @field:NotNull
    val skuId: String,
    val initialQuantity: Int,
)

fun StockInitEvent.toDto() = StockInitDto(
    skuId = skuId,
    initialQuantity = initialQuantity,
)

/**
 * 재고 일괄 초기화 이벤트 DTO (여러 상품 생성 시)
 */
data class StockBulkInitEvent(
    @field:NotEmpty
    val items: List<StockInitItem>,
) {
    data class StockInitItem(val skuId: String, val initialQuantity: Int)
}

fun StockBulkInitEvent.toDto() = StockBulkInitDto(
    items = items.map {
        StockBulkInitDto.StockInitItemDto(
            skuId = it.skuId,
            initialQuantity = it.initialQuantity,
        )
    },
)

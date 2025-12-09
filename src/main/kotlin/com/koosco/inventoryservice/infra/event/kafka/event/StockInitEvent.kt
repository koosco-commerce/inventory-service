package com.koosco.inventoryservice.infra.event.kafka.event

import com.koosco.inventoryservice.application.dto.StockInitDto
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

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
 * SKU 정보 (재고 초기화용)
 */
data class SkuInfo(
    @field:NotNull
    val skuId: String,
    val price: Long,
    val optionValues: String,
    val initialQuantity: Int = 0,
)

fun SkuInfo.toDto() = StockInitDto(
    skuId = skuId,
    initialQuantity = initialQuantity,
)

/**
 * SKU 생성 이벤트 (Catalog Service에서 개별 발행)
 * 각 SKU마다 개별적으로 발행되어 Inventory Service에서 재고를 초기화합니다.
 */
data class SkuCreatedEvent(
    @field:NotNull
    val skuId: String,
    @field:NotNull
    val productId: Long,
    val productCode: String,
    val price: Long,
    val optionValues: String,
    val initialQuantity: Int = 0,
    val createdAt: LocalDateTime,
)

fun SkuCreatedEvent.toDto() = StockInitDto(
    skuId = skuId,
    initialQuantity = initialQuantity,
)

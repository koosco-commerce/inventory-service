package com.koosco.inventoryservice.infra.event.kafka.event

import com.koosco.inventoryservice.application.dto.StockReserveCancelDto
import com.koosco.inventoryservice.application.dto.StockReserveConfirmDto
import com.koosco.inventoryservice.application.dto.StockReserveRequestDto
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/**
 * 재고 예약 이벤트 DTO
 */
data class StockReserveEvent(
    @field:NotNull
    val orderId: Long,

    @field:NotNull
    val skuId: String,

    @field:Positive
    val quantity: Int,
)

fun StockReserveEvent.toDto() = StockReserveRequestDto(orderId, skuId, quantity)

/**
 * 재고 예약 확인 이벤트 DTO
 */
data class StockReserveConfirmEvent(
    @field:NotNull
    val orderId: Long,

    @field:NotNull
    val skuId: String,

    @field:Positive
    val quantity: Int,
)

fun StockReserveConfirmEvent.toDto() = StockReserveConfirmDto(orderId, skuId, quantity)

/**
 * 재고 예약 취소 이벤트 DTO
 */
data class StockReserveCancelEvent(
    @field:NotNull
    val orderId: Long,

    @field:NotNull
    val skuId: String,

    @field:Positive
    val quantity: Int,
)

fun StockReserveCancelEvent.toDto() = StockReserveCancelDto(orderId, skuId, quantity)

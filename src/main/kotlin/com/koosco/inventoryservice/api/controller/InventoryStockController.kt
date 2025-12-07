package com.koosco.inventoryservice.api.controller

import com.koosco.common.core.response.ApiResponse
import com.koosco.inventoryservice.api.request.*
import com.koosco.inventoryservice.application.usecase.AddStockUseCase
import com.koosco.inventoryservice.application.usecase.AdjustStockUseCase
import com.koosco.inventoryservice.application.usecase.ReduceStockUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Inventory Stock Controller", description = "재고 관리 API")
@RestController
@RequestMapping("/api/inventory")
class InventoryStockController(
    private val adjustStockUseCase: AdjustStockUseCase,
    private val addStockUseCase: AddStockUseCase,
    private val reduceStockUseCase: ReduceStockUseCase,
) {

    @Operation(
        summary = "재고 조정",
        description = "SKU ID로 재고를 조정합니다. 예약된 재고 이하로 조정할 수 없습니다.",
    )
    @PatchMapping("/adjust/{skuId}")
    fun increaseInventory(
        @PathVariable skuId: String,
        @RequestBody request: AdjustStockRequest
    ): ApiResponse<Any> {
        adjustStockUseCase.adjustSingle(request.toDto(skuId))

        return ApiResponse.success()
    }

    @Operation(
        summary = "대량 재고 조정",
        description = "여러 SKU ID로 재고를 대량 조정합니다.",
    )
    @PatchMapping("/adjust")
    fun adjustBulkInventory(@RequestBody request: BulkAdjustStockRequest): ApiResponse<Any> {
        adjustStockUseCase.adjustBulk(request.adjustments.map { it.toDto() })

        return ApiResponse.success()
    }

    @Operation(
        summary = "재고 추가",
        description = "SKU ID로 재고를 추가합니다.",
    )
    @PostMapping("/add/{skuId}")
    fun addInventory(
        @PathVariable skuId: String,
        @RequestBody request: AddStockRequest
    ): ApiResponse<Any> {
        addStockUseCase.addSingle(request.toDto(skuId))

        return ApiResponse.success()
    }

    @Operation(
        summary = "대량 재고 추가",
        description = "여러 SKU ID로 재고를 대량 추가합니다."
    )
    @PostMapping("/add")
    fun addBulkInventories(@RequestBody request: BulkAddStockRequest): ApiResponse<Any> {
        addStockUseCase.addBulk(request.items.map { it.toDto() })

        return ApiResponse.success()
    }

    @Operation(
        summary = "재고 감소",
        description = "SKU ID로 재고를 감소합니다."
    )
    @PostMapping("/remove/{skuId}")
    fun reduceInventory(
        @PathVariable skuId: String,
        @RequestBody request: ReduceStockRequest
    ): ApiResponse<Any> {
        reduceStockUseCase.reduceSingle(request.toDto(skuId))

        return ApiResponse.success()
    }

    @Operation(
        summary = "대량 재고 감소",
        description = "SKU ID로 재고를 대량으로 감소합니다."
    )
    @PostMapping("/remove")
    fun reduceBulkInventories(@RequestBody request: BulkReduceStockRequest): ApiResponse<Any> {
        reduceStockUseCase.reduceBulk(request.items.map { it.toDto() })

        return ApiResponse.success()
    }
}

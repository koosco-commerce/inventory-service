package com.koosco.inventoryservice.api.controller

import com.koosco.common.core.response.ApiResponse
import com.koosco.inventoryservice.api.request.GetInventoriesRequest
import com.koosco.inventoryservice.api.request.toDto
import com.koosco.inventoryservice.api.response.GetInventoriesResponse
import com.koosco.inventoryservice.api.response.GetInventoryResponse
import com.koosco.inventoryservice.application.dto.GetInventoryCommand
import com.koosco.inventoryservice.application.usecase.GetInventoryUseCase
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/inventory")
class QueryInventoryController(private val getInventoryUseCase: GetInventoryUseCase) {

    @Operation(
        summary = "재고 조회",
        description = "SKU ID로 재고 정보를 조회합니다.",
    )
    @GetMapping("/{skuId}")
    fun getInventoryBySkuId(@PathVariable skuId: String): ApiResponse<GetInventoryResponse> {
        val dto = getInventoryUseCase.getInventoryBySkuId(GetInventoryCommand(skuId = skuId))

        return ApiResponse.success(GetInventoryResponse.toResponse(dto))
    }

    @Operation(
        summary = "대량 재고 조회",
        description = "여러 SKU ID로 재고 정보를 대량 조회합니다.",
    )
    @PostMapping("/bulk")
    fun getInventoryBySkuIds(@RequestBody request: GetInventoriesRequest): ApiResponse<GetInventoriesResponse> {
        val dto = getInventoryUseCase.getInventoriesBySkuIds(request.toDto())

        return ApiResponse.success(GetInventoriesResponse.toResponse(dto))
    }
}

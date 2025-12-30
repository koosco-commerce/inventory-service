package com.koosco.inventoryservice.inventory.api.controller

import com.koosco.common.core.response.ApiResponse
import com.koosco.inventoryservice.inventory.api.request.AddStockRequest
import com.koosco.inventoryservice.inventory.api.request.BulkAddStockRequest
import com.koosco.inventoryservice.inventory.api.request.BulkReduceStockRequest
import com.koosco.inventoryservice.inventory.api.request.ReduceStockRequest
import com.koosco.inventoryservice.inventory.application.command.BulkDecreaseStockCommand
import com.koosco.inventoryservice.inventory.application.command.BulkIncreaseStockCommand
import com.koosco.inventoryservice.inventory.application.command.DecreaseStockCommand
import com.koosco.inventoryservice.inventory.application.command.IncreaseStockCommand
import com.koosco.inventoryservice.inventory.application.usecase.DecreaseStockUseCase
import com.koosco.inventoryservice.inventory.application.usecase.IncreaseStockUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Inventory Stock Controller", description = "재고 관리 API")
@RestController
@RequestMapping("/api/inventories")
class InventoryStockController(
    private val increaseStockUseCase: IncreaseStockUseCase,
    private val decreaseStockUseCase: DecreaseStockUseCase,
) {

    @Operation(
        summary = "재고 추가",
        description = "SKU ID로 재고를 추가합니다.",
    )
    @PostMapping("/{skuId}/increase")
    fun addInventory(@PathVariable skuId: String, @RequestBody body: AddStockRequest): ApiResponse<Any> {
        increaseStockUseCase.execute(
            IncreaseStockCommand(
                skuId = skuId,
                addingQuantity = body.quantity,
            ),
        )

        return ApiResponse.success()
    }

    @Operation(
        summary = "대량 재고 추가",
        description = "여러 SKU ID로 재고를 대량 추가합니다.",
    )
    @PostMapping("/increase")
    fun addBulkInventories(@RequestBody body: BulkAddStockRequest): ApiResponse<Any> {
        increaseStockUseCase.execute(
            BulkIncreaseStockCommand(
                items = body.items.map {
                    BulkIncreaseStockCommand.AddingStockInfo(
                        it.skuId,
                        it.quantity,
                    )
                },
            ),
        )

        return ApiResponse.success()
    }

    @Operation(
        summary = "재고 감소",
        description = "SKU ID로 재고를 감소합니다.",
    )
    @PostMapping("/{skuId}/decrease")
    fun reduceInventory(@PathVariable skuId: String, @RequestBody body: ReduceStockRequest): ApiResponse<Any> {
        decreaseStockUseCase.execute(
            DecreaseStockCommand(
                skuId = skuId,
                reducingQuantity = body.quantity,
            ),
        )

        return ApiResponse.success()
    }

    @Operation(
        summary = "대량 재고 감소",
        description = "SKU ID로 재고를 대량으로 감소합니다.",
    )
    @PostMapping("/decrease")
    fun reduceBulkInventories(@RequestBody body: BulkReduceStockRequest): ApiResponse<Any> {
        decreaseStockUseCase.execute(
            BulkDecreaseStockCommand(
                body.items.map {
                    BulkDecreaseStockCommand.ReducingStockInfo(
                        it.skuId,
                        it.quantity,
                    )
                },
            ),
        )

        return ApiResponse.success()
    }
}

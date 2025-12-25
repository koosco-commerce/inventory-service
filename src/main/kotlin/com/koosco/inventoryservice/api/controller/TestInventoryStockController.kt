package com.koosco.inventoryservice.api.controller

import com.koosco.inventoryservice.application.usecase.InventorySeedUseCase
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : TestInventoryStockController
 * author         : koo
 * date           : 2025. 12. 26. 오전 4:58
 * description    : 테스트용 재고 초기화 컨트롤러 (local 환경에서만 사용)
 */
@Profile("local")
@RestController
@RequestMapping("/api/inventory/test")
class TestInventoryStockController(private val inventorySeedUseCase: InventorySeedUseCase) {

    @PostMapping("/init")
    fun initStock() {
        inventorySeedUseCase.execute()
    }

    @PostMapping("/clear")
    fun clearStock() {
        inventorySeedUseCase.clear()
    }
}

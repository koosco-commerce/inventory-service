package com.koosco.inventoryservice.api.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/inventory")
class AdminController {

    @Operation(
        summary = "기간내 재고 변경 로그 조회",
        description = "관리자용으로 기간내 재고 변경 로그를 조회합니다.",
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun getInventoryChangeLogs(): String {
        // 기간내 재고 변경 로그 조회 로직 구현
        return "Inventory change logs"
    }

    @Operation(
        summary = "특정 SKU 재고 일괄 csv 업로드",
        description = "관리자용으로 특정 SKU 재고를 일괄 csv 업로드",
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun uploadInventoryCsv(): String {
        // 특정 SKU 재고 일괄 csv 업로드 로직 구현
        return "Inventory CSV uploaded"
    }
}

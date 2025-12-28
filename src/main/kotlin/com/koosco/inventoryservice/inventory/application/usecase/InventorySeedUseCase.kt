package com.koosco.inventoryservice.inventory.application.usecase

import com.koosco.common.core.annotation.UseCase
import com.koosco.inventoryservice.catalog.TestProductService
import com.koosco.inventoryservice.inventory.application.port.InventorySeedPort
import com.koosco.inventoryservice.order.TestOrderService
import org.springframework.context.annotation.Profile
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : InventorySeedUseCase
 * author         : koo
 * date           : 2025. 12. 26. 오전 4:59
 * description    : 더미 데이터 초기화 usecase
 */
@Profile("local")
@UseCase
class InventorySeedUseCase(private val inventorySeedPort: InventorySeedPort) {

    @Transactional
    fun execute() {
        inventorySeedPort.init(TestOrderService.FIRST_SKU_ID, TestOrderService.INITIAL_STOCK)
        inventorySeedPort.init(TestOrderService.SECOND_SKU_ID, TestOrderService.INITIAL_STOCK)
    }

    @Transactional
    fun clear() {
        inventorySeedPort.init(TestOrderService.FIRST_SKU_ID, 0)
        inventorySeedPort.init(TestOrderService.SECOND_SKU_ID, 0)
        TestProductService.PRODUCTS.forEach { inventorySeedPort.deleteById(it.skuId) }
    }
}

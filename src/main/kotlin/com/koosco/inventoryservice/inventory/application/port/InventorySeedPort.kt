package com.koosco.inventoryservice.inventory.application.port

/**
 * fileName       : InventorySeedPort
 * author         : koo
 * date           : 2025. 12. 26. 오전 4:55
 * description    : 더미 데이터 영속성 처리를 위한 인터페이스
 */
interface InventorySeedPort {

    fun init(skuId: String, initialQuantity: Int)

    fun deleteById(skuId: String)
}

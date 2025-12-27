package com.koosco.inventoryservice.inventory.application.port

/**
 * fileName       : InventorySeedPort
 * author         : koo
 * date           : 2025. 12. 26. 오전 4:55
 * description    :
 */
interface InventorySeedPort {

    fun init(skuId: String, initialQuantity: Int)
}

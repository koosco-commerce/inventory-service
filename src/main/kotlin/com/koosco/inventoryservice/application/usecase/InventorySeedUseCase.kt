package com.koosco.inventoryservice.application.usecase

import com.koosco.common.core.annotation.UseCase
import com.koosco.inventoryservice.application.port.InventorySeedPort
import org.springframework.context.annotation.Profile
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : InventorySeedUseCase
 * author         : koo
 * date           : 2025. 12. 26. 오전 4:59
 * description    :
 */
@Profile("local")
@UseCase
class InventorySeedUseCase(private val inventorySeedPort: InventorySeedPort) {

    companion object {
        const val INITIAL_STOCK = 10000
        const val FIRST_SKU_ID = "00001f4c-a36c-4a70-9347-413ce52d5d61"
        const val SECOND_SKU_ID = "0000298f-0c73-4df1-8576-ac232687c290"
    }

    @Transactional
    fun execute() {
        inventorySeedPort.init(FIRST_SKU_ID, INITIAL_STOCK)
        inventorySeedPort.init(SECOND_SKU_ID, INITIAL_STOCK)
    }

    @Transactional
    fun clear() {
        inventorySeedPort.init(FIRST_SKU_ID, 0)
        inventorySeedPort.init(SECOND_SKU_ID, 0)
    }
}

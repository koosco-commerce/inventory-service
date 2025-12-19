package com.koosco.inventoryservice.domain.exception

import com.koosco.inventoryservice.common.InventoryErrorCode

/**
 * fileName       : NotEnoughStockException
 * author         : koo
 * date           : 2025. 12. 19. 오후 1:54
 * description    :
 */
class NotEnoughStockException(message: String = "Not enough stock available") :
    BusinessException(
        InventoryErrorCode.NOT_ENOUGH_STOCK,
        message,
    )

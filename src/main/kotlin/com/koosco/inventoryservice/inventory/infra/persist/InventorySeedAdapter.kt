package com.koosco.inventoryservice.inventory.infra.persist

import com.koosco.inventoryservice.inventory.application.port.InventorySeedPort
import jakarta.persistence.EntityManager
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : InventorySeedAdapter
 * author         : koo
 * date           : 2025. 12. 26. 오전 4:57
 * description    :
 */
@Component
@Profile("local")
class InventorySeedAdapter(private val entityManager: EntityManager) : InventorySeedPort {

    @Transactional
    override fun init(skuId: String, initialQuantity: Int) {
        entityManager.createQuery(
            """
            UPDATE Inventory i
            SET i.stock.total = :quantity,
                i.stock.reserved = 0
            WHERE i.skuId = :skuId
        """,
        )
            .setParameter("skuId", skuId)
            .setParameter("quantity", initialQuantity)
            .executeUpdate()
    }
}

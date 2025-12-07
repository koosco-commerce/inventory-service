package com.koosco.inventoryservice.infra.persist

import com.koosco.inventoryservice.domain.entity.Inventory
import org.springframework.data.jpa.repository.JpaRepository

interface JpaInventoryRepository : JpaRepository<Inventory, String>

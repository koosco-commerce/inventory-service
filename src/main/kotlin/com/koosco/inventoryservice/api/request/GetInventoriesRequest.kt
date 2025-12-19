package com.koosco.inventoryservice.api.request

import com.koosco.inventoryservice.application.dto.GetInventoriesCommand

data class GetInventoriesRequest(val skuIds: List<String>)

fun GetInventoriesRequest.toDto() = GetInventoriesCommand(skuIds)

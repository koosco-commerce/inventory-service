package com.koosco.inventoryservice.api.request

import com.koosco.inventoryservice.application.dto.GetInventoriesDto

data class GetInventoriesRequest(val skuIds: List<String>)

fun GetInventoriesRequest.toDto() = GetInventoriesDto(skuIds)

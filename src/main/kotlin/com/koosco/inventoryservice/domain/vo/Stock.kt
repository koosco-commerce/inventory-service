package com.koosco.inventoryservice.domain.vo

import com.koosco.common.core.exception.BadRequestException
import com.koosco.inventoryservice.common.InventoryErrorCode
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Transient

@Embeddable
data class Stock(
    @Column(name = "total_stock", nullable = false)
    val total: Int,

    @Column(name = "reserved_stock", nullable = false)
    val reserved: Int = 0,
) {
    @Transient
    val available: Int = total - reserved

    init {
        if (total < 0) {
            throw BadRequestException(
                InventoryErrorCode.INVALID_QUANTITY,
                "총 재고는 0 이상이어야 합니다. (현재: $total)",
            )
        }
        if (reserved < 0) {
            throw BadRequestException(
                InventoryErrorCode.INVALID_QUANTITY,
                "예약 재고는 0 이상이어야 합니다. (현재: $reserved)",
            )
        }
        if (total < reserved) {
            throw BadRequestException(
                InventoryErrorCode.STOCK_ADJUST_NOT_ALLOWED,
                "총 재고($total)는 예약 재고($reserved)보다 작을 수 없습니다.",
            )
        }
    }

    fun adjust(q: Int): Stock {
        if (reserved > q) {
            throw BadRequestException(
                InventoryErrorCode.STOCK_ADJUST_NOT_ALLOWED,
                "조정 수량($q)은 예약 재고($reserved)보다 작을 수 없습니다.",
            )
        }
        return copy(total = q)
    }

    fun increase(q: Int): Stock {
        if (q <= 0) {
            throw BadRequestException(
                InventoryErrorCode.INVALID_QUANTITY,
                "증가 수량은 양수여야 합니다. (현재: $q)",
            )
        }
        return copy(total = total + q)
    }

    fun decrease(q: Int): Stock {
        if (q <= 0) {
            throw BadRequestException(
                InventoryErrorCode.INVALID_QUANTITY,
                "감소 수량은 양수여야 합니다. (현재: $q)",
            )
        }
        val newTotal = total - q
        if (newTotal < reserved) {
            throw BadRequestException(
                InventoryErrorCode.INSUFFICIENT_STOCK,
                "재고 감소 후($newTotal) 예약 재고($reserved)보다 작아질 수 없습니다.",
            )
        }
        return copy(total = newTotal)
    }

    fun reserve(q: Int): Stock {
        if (q <= 0) {
            throw BadRequestException(
                InventoryErrorCode.INVALID_QUANTITY,
                "예약 수량은 양수여야 합니다. (현재: $q)",
            )
        }
        if (available < q) {
            throw BadRequestException(
                InventoryErrorCode.INSUFFICIENT_STOCK,
                "사용 가능한 재고가 부족합니다. (요청: $q, 가능: $available)",
            )
        }
        return copy(reserved = reserved + q)
    }

    fun confirm(q: Int): Stock {
        if (q <= 0) {
            throw BadRequestException(
                InventoryErrorCode.INVALID_QUANTITY,
                "확정 수량은 양수여야 합니다. (현재: $q)",
            )
        }
        if (reserved < q) {
            throw BadRequestException(
                InventoryErrorCode.INSUFFICIENT_STOCK,
                "예약 재고가 부족합니다. (요청: $q, 예약: $reserved)",
            )
        }
        if (total < q) {
            throw BadRequestException(
                InventoryErrorCode.INSUFFICIENT_STOCK,
                "총 재고가 부족합니다. (요청: $q, 총: $total)",
            )
        }
        return copy(
            total = total - q,
            reserved = reserved - q,
        )
    }

    fun cancelReservation(q: Int): Stock {
        if (q <= 0) {
            throw BadRequestException(
                InventoryErrorCode.INVALID_QUANTITY,
                "취소 수량은 양수여야 합니다. (현재: $q)",
            )
        }
        if (reserved < q) {
            throw BadRequestException(
                InventoryErrorCode.INSUFFICIENT_STOCK,
                "예약 재고가 부족합니다. (요청: $q, 예약: $reserved)",
            )
        }
        return copy(reserved = reserved - q)
    }
}

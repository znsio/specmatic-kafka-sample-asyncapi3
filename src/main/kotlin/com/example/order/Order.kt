package com.example.order

import java.math.BigDecimal
import java.time.LocalDateTime.now

data class Order(
    val id: Int,
    val orderItems: List<OrderItem> = emptyList(),
    val lastUpdatedDate: String = now().toString(),
    val status: OrderStatus
) {
    fun totalAmount(): BigDecimal {
        return orderItems.sumOf { it.price * BigDecimal(it.quantity) }
    }
}

data class OrderId(
    val id: Int
)
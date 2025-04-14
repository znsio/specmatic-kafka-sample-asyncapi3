package com.example.order

import org.springframework.stereotype.Repository

@Repository
class OrderDeliveryRepository {
    private val initiatedDeliveries = mutableListOf<OrderDelivery>()

    fun save(orderDelivery: OrderDelivery) {
        if(orderDelivery.deliveryStatus == DeliveryStatus.INITIATED) {
            initiatedDeliveries.add(orderDelivery)
        }
    }

    fun findById(orderId: Int): OrderDelivery {
        return initiatedDeliveries.find {
            it.orderId == orderId && it.deliveryStatus == DeliveryStatus.INITIATED
        } ?: throw OrderDeliveryNotFoundException("No delivery was initiated with orderId '$orderId'")
    }
}

class OrderDeliveryNotFoundException(
    override val message: String
) : Exception(message)
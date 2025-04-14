package com.example.order

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/order")
class OrderDeliveryController(
    private val orderDeliveryService: OrderDeliveryService
) {

    @GetMapping("/initiated-delivery/{id}")
    fun getInitiatedDeliveryWith(
        @PathVariable id: Int
    ): ResponseEntity<OrderDelivery> {
        println("[OrderDeliveryController] Received request to find initiated delivery with id '$id'")
        return ResponseEntity.ok(orderDeliveryService.findById(id))
    }
}
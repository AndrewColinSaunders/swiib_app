package com.example.warehousetet

data class DeliveryOrders(
    val id: Int,
    val orderName: String,
    val deliveryDate: String,
    val sourceDocument: String,
    val productDetails: List<DeliveryProduct>
)






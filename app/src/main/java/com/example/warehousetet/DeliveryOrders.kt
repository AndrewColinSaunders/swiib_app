package com.example.warehousetet

data class DeliveryOrders(
    val id: Int,
    val name: String,
    val date: String,
    val origin: String,
    var locationId: String?, // Nullable integer to accommodate cases where the location ID is not provided
    var locationDestId: String? // Similarly, nullable for the destination location ID
)

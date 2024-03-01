package com.example.warehousetet

data class InternalTransfers(
    val id: Int,
    val transferName: String,
    val transferDate: String,
    val productDetails: List<Product> // Holds Product objects instead of a String
)

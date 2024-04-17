package com.example.warehousetet

data class MoveLine(
    val id: Int,
    val productId: Int,
    val productName: String,
    val lotId: Int?,
    val lotName: String,
    val quantity: Double,
    val locationId: Int,
    val locationName: String,
    val locationDestId: Int,
    val locationDestName: String,
    val barcode: String? = null,
)
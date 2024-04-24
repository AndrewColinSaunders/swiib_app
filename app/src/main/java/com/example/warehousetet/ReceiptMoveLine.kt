package com.example.warehousetet

data class ReceiptMoveLine(
    val id: Int,
    val productId: Int,
    val productName: String,
    val lotName: String,
    val quantity: Double,
    val expectedQuantity: Double,
    val totalQuantity: Double,
    val locationDestId: Int,
    val locationDestName: String,
    val expirationDate: String,
    val barcode: String? = null,
    val trackingType: String = "none",
    val useExpirationDate: Boolean? = null
)


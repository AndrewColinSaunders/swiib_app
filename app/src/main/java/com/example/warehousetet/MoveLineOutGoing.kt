package com.example.warehousetet

data class MoveLineOutGoing(
    val lineId: Int,
    val productId: Int,
    val productName: String,
    val lotId: Int?,
    val lotName: String,
    val quantity: Double,
    val locationDestId: Int,
    val locationDestName: String,
    val resultPackageId : Int?,
    val resultPackageName: String
)

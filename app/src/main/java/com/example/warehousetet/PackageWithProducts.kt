package com.example.warehousetet

data class PackageWithProducts(
    val packageName: String,
    val products: List<MoveLineOutgoing>
)


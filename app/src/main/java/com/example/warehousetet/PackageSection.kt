package com.example.warehousetet

data class PackageSection(
    val packageName: String,
    val packageId: Int?,
    val moveLines: MutableList<MoveLine>
)

package com.example.warehousetet

data class PackageSection(
    val packageName: String,
    val packageId: Int?,
    val moveLineOutGoings: MutableList<MoveLineOutGoing>
)

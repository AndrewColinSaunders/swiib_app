package com.example.warehousetet

data class InternalTransfers(
    val id: Int,
    val name: String,
    val date: String,
    val origin: String,
    var locationId: String?,
    var locationDestId: String?
)

package com.example.warehousetet

import IntTransferProducts

data class InternalTransfers(
    val id: Int,
    val transferName: String,
    val transferDate: String,
    val sourceDocument: String,
    val productDetails: List<IntTransferProducts>
)

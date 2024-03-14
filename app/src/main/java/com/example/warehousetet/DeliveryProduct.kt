package com.example.warehousetet

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DeliveryProduct(
    val name: String,
    val quantity: Double,
    val barcode: String?,
) : Parcelable

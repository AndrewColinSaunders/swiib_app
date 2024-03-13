package com.example.warehousetet

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    val id: Int,
    val name: String,
    val quantity: Double,
    val barcode: String? = null,
    val trackingType: String? = null,
    val useExpirationDate: Boolean? = null
    // Add other fields as necessary
) : Parcelable




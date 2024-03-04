package com.example.warehousetet

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    val name: String,
    val quantity: Double,
    // Add other fields as necessary
) : Parcelable






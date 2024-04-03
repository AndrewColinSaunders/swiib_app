package com.example.warehousetet

data class Pick(
    val id: Int,
    val name: String,
    val date: String, // Ensure this matches the format you expect from 'scheduled_date'
    val origin: String, // Additional details about the origin if applicable
    // You might want to include more fields here depending on what information is relevant for delivery orders
)

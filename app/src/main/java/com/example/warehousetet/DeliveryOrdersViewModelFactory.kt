package com.example.warehousetet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class DeliveryOrdersViewModelFactory(
    private val odooXmlRpcClient: OdooXmlRpcClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeliveryOrdersViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeliveryOrdersViewModel(odooXmlRpcClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

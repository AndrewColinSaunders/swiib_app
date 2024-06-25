package com.example.warehousetet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ReceiptsViewModelFactory(
    private val odooXmlRpcClient: OdooXmlRpcClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReceiptsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReceiptsViewModel(odooXmlRpcClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.example.warehousetet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PackViewModelFactory(
    private val odooXmlRpcClient: OdooXmlRpcClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PackViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PackViewModel(odooXmlRpcClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

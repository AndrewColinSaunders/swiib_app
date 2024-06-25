package com.example.warehousetet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PickViewModelFactory(
    private val odooXmlRpcClient: OdooXmlRpcClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PickViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PickViewModel(odooXmlRpcClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

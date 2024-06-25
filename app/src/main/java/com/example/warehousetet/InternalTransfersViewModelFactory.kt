package com.example.warehousetet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class InternalTransfersViewModelFactory(
    private val odooXmlRpcClient: OdooXmlRpcClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InternalTransfersViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InternalTransfersViewModel(odooXmlRpcClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

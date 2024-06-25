package com.example.warehousetet

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*

class InternalTransfersViewModel(
    private val odooXmlRpcClient: OdooXmlRpcClient
) : ViewModel() {

    private var refreshJob: Job? = null

    fun fetchTransfersAndDisplay(onResult: (List<InternalTransfers>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val transfers = odooXmlRpcClient.fetchIntTransfers() // This method needs to be implemented
                withContext(Dispatchers.Main) {
                    onResult(transfers)
                }
            } catch (e: Exception) {
                // Log error or handle it appropriately
                Log.e("InternalTransfersViewModel", "Error fetching transfers: ${e.localizedMessage}", e)
            }
        }
    }

    fun fetchAndLogBuyerDetails(transferName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            odooXmlRpcClient.fetchAndLogBuyerDetails(transferName)
        }
    }

    fun startPeriodicRefresh(onRefresh: () -> Unit) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                onRefresh()
                delay(5000) // Refresh every 5 seconds
            }
        }
    }

    fun stopPeriodicRefresh() {
        refreshJob?.cancel()
    }

    fun clear() {
        refreshJob?.cancel()
    }
}

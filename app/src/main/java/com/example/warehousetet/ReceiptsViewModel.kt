
package com.example.warehousetet

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*

class ReceiptsViewModel(
    private val odooXmlRpcClient: OdooXmlRpcClient
) : ViewModel() {

    private var refreshJob: Job? = null

    fun fetchReceiptsAndDisplay(onResult: (List<Receipt>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val receipts = odooXmlRpcClient.fetchReceipts()
                withContext(Dispatchers.Main) {
                    onResult(receipts)
                }
            } catch (e: Exception) {
                // Log error or handle it appropriately
                Log.e("ReceiptsViewModel", "Error fetching receipts: ${e.localizedMessage}", e)
            }
        }
    }

    fun fetchAndLogBuyerDetails(receiptName: String) {
        viewModelScope.launch {
            odooXmlRpcClient.fetchAndLogBuyerDetails(receiptName)
        }
    }

    fun startPeriodicRefresh(onRefresh: () -> Unit) {
        refreshJob?.cancel() // Cancel any existing job to avoid duplicates
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

package com.example.warehousetet

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*

class DeliveryOrdersViewModel(
    private val odooXmlRpcClient: OdooXmlRpcClient
) : ViewModel() {

    private var refreshJob: Job? = null

    fun fetchDeliveryOrdersAndDisplay(onResult: (List<DeliveryOrders>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val deliveryOrders = odooXmlRpcClient.fetchDeliveryOrders()
                withContext(Dispatchers.Main) {
                    onResult(deliveryOrders)
                }
            } catch (e: Exception) {
                // Log error or handle it appropriately
                Log.e("DeliveryOrdersViewModel", "Error fetching delivery orders: ${e.localizedMessage}", e)
            }
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


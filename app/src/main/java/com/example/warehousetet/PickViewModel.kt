package com.example.warehousetet

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*

class PickViewModel(
    private val odooXmlRpcClient: OdooXmlRpcClient
) : ViewModel() {

    private var refreshJob: Job? = null

    fun fetchPicksAndDisplay(onResult: (List<Pick>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val picks = odooXmlRpcClient.fetchPicks()
                withContext(Dispatchers.Main) {
                    onResult(picks)
                }
            } catch (e: Exception) {
                // Log error or handle it appropriately
                Log.e("PickViewModel", "Error fetching picks: ${e.localizedMessage}", e)
            }
        }
    }

    fun fetchAndLogBuyerDetails(pickName: String) {
        viewModelScope.launch {
            odooXmlRpcClient.fetchAndLogBuyerDetails(pickName)
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


package com.example.warehousetet

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*

class PackViewModel(
    private val odooXmlRpcClient: OdooXmlRpcClient
) : ViewModel() {

    private var refreshJob: Job? = null

    fun fetchPacksAndDisplay(onResult: (List<Pack>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val packs = odooXmlRpcClient.fetchPacks()
                withContext(Dispatchers.Main) {
                    onResult(packs)
                }
            } catch (e: Exception) {
                // Log error or handle it appropriately
                Log.e("PackViewModel", "Error fetching packs: ${e.localizedMessage}", e)
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

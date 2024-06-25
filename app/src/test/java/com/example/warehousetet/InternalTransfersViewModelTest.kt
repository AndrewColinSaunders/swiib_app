//package com.example.warehousetet
//
//import io.mockk.coEvery
//import io.mockk.coVerify
//import io.mockk.mockk
//import kotlinx.coroutines.*
//import kotlinx.coroutines.test.*
//import org.junit.After
//import org.junit.Before
//import org.junit.Test
//import kotlin.test.assertEquals
//
//@OptIn(ExperimentalCoroutinesApi::class)
//class InternalTransfersViewModelTest {
//
//    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
//    private lateinit var viewModel: InternalTransfersViewModel
//    private val testDispatcher = StandardTestDispatcher()
//    private val testScope = TestScope(testDispatcher)
//
//    @Before
//    fun setup() {
//        Dispatchers.setMain(testDispatcher)
//        odooXmlRpcClient = mockk(relaxed = true)
//        viewModel = InternalTransfersViewModel(odooXmlRpcClient, testScope)
//    }
//
//    @After
//    fun tearDown() {
//        Dispatchers.resetMain()
//        testScope.cancel()
//    }
//
//    @Test
//    fun fetchTransfersAndDisplay_success() = testScope.runTest {
//        val transfers = listOf(
//            InternalTransfers(id = 1, name = "Transfer 1", date = "2024-01-01", origin = "Origin 1", locationId = "1", locationDestId = "2"),
//            InternalTransfers(id = 2, name = "Transfer 2", date = "2024-01-02", origin = "Origin 2", locationId = "3", locationDestId = "4")
//        )
//        coEvery { odooXmlRpcClient.fetchIntTransfers() } returns transfers
//
//        var result: List<InternalTransfers>? = null
//        viewModel.fetchTransfersAndDisplay { result = it }
//
//        advanceUntilIdle()
//        assertEquals(transfers, result)
//        coVerify { odooXmlRpcClient.fetchIntTransfers() }
//    }
//
//    @Test
//    fun fetchAndLogBuyerDetails_success() = testScope.runTest {
//        val buyerDetails = BuyerDetails(
//            id = 123,
//            name = "John Doe",
//            login = "john.doe@example.com"
//        )
//        coEvery { odooXmlRpcClient.fetchAndLogBuyerDetails(any()) } returns buyerDetails
//
//        viewModel.fetchAndLogBuyerDetails("Transfer 1")
//
//        advanceUntilIdle()
//        coVerify { odooXmlRpcClient.fetchAndLogBuyerDetails("Transfer 1") }
//    }
//
//    @Test
//    fun startPeriodicRefresh_callsOnRefreshRepeatedly() = testScope.runTest {
//        var callCount = 0
//        viewModel.startPeriodicRefresh { callCount++ }
//
//        advanceTimeBy(5000)
//        assertEquals(1, callCount)
//
//        advanceTimeBy(10000)
//        assertEquals(3, callCount)
//
//        viewModel.stopPeriodicRefresh()
//    }
//
//    @Test
//    fun stopPeriodicRefresh_stopsCallingOnRefresh() = testScope.runTest {
//        var callCount = 0
//        viewModel.startPeriodicRefresh { callCount++ }
//
//        advanceTimeBy(10000)
//        viewModel.stopPeriodicRefresh()
//
//        advanceTimeBy(5000)
//        assertEquals(2, callCount)
//    }
//}



package com.example.warehousetet

import androidx.lifecycle.viewModelScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class InternalTransfersViewModelTest {

    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var viewModel: InternalTransfersViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher) // Set the main dispatcher to the test dispatcher
        odooXmlRpcClient = mockk(relaxed = true)
        viewModel = InternalTransfersViewModel(odooXmlRpcClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testScope.cancel()  // Ensure all coroutines are cancelled and cleaned up
    }

    @Test
    fun fetchTransfersAndDisplay_success() = testScope.runTest {
        val transfers = listOf(
            InternalTransfers(id = 1, name = "Transfer 1", date = "2024-01-01", origin = "Origin 1", locationId = "1", locationDestId = "2"),
            InternalTransfers(id = 2, name = "Transfer 2", date = "2024-01-02", origin = "Origin 2", locationId = "3", locationDestId = "4")
        )
        coEvery { odooXmlRpcClient.fetchIntTransfers() } returns transfers

        // Create a CompletableDeferred to capture the result
        val resultDeferred = CompletableDeferred<List<InternalTransfers>?>()

        // Trigger the ViewModel to fetch transfers and capture the result
        viewModel.fetchTransfersAndDisplay {
            resultDeferred.complete(it)
        }

        // Await the result to ensure the coroutine completes before making assertions
        val result = resultDeferred.await()

        // Assertions
        assertNotNull(result, "The result should not be null.")
        assertEquals(transfers, result, "The fetched transfers should match the expected list.")
        coVerify { odooXmlRpcClient.fetchIntTransfers() }
    }

    @Test
    fun startPeriodicRefresh_callsOnRefreshRepeatedly() = testScope.runTest {
        var callCount = 0
        viewModel.startPeriodicRefresh { callCount++ }

        advanceTimeBy(5000)  // Forward time by 5 seconds
        assertEquals(1, callCount, "One refresh should have been triggered.")

        advanceTimeBy(10000) // Forward time by another 10 seconds
        assertEquals(3, callCount, "Three refreshes should have been triggered.")

        viewModel.stopPeriodicRefresh()
    }

    @Test
    fun stopPeriodicRefresh_stopsCallingOnRefresh() = testScope.runTest {
        var callCount = 0
        viewModel.startPeriodicRefresh { callCount++ }

        advanceTimeBy(10000) // 10 seconds pass
        viewModel.stopPeriodicRefresh() // Stop refreshing

        advanceTimeBy(5000)  // Forward time by 5 more seconds
        assertEquals(2, callCount, "Should only have two calls after stopping the refresh.")
    }
}

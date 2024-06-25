//package com.example.warehousetet
//
//import io.mockk.coEvery
//import io.mockk.coJustRun
//import io.mockk.coVerify
//import io.mockk.mockk
//import kotlinx.coroutines.*
//import kotlinx.coroutines.test.*
//import org.junit.After
//import org.junit.Before
//import org.junit.Test
//import kotlin.test.assertEquals
//
//
//@OptIn(ExperimentalCoroutinesApi::class)
//class ReceiptsViewModelTest {
//
//    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
//    private lateinit var viewModel: ReceiptsViewModel
//    private val testDispatcher = StandardTestDispatcher()
//    private val testScope = TestScope(testDispatcher)
//
//    @Before
//    fun setup() {
//        Dispatchers.setMain(testDispatcher)
//        odooXmlRpcClient = mockk(relaxed = true)  // relaxed = true helps to avoid having to stub every call
//        viewModel = ReceiptsViewModel(odooXmlRpcClient, testScope)
//    }
//
//    @After
//    fun tearDown() {
//        Dispatchers.resetMain()
//        testScope.cancel()  // Ensure all coroutines are cancelled and cleaned up
//    }
//
//    @Test
//    fun fetchReceiptsAndDisplay_success() = testScope.runTest {
//        val receipts = listOf(
//            Receipt(id = 1, name = "Receipt 1", date = "2024-01-01", origin = "Origin 1"),
//            Receipt(id = 2, name = "Receipt 2", date = "2024-01-02", origin = "Origin 2")
//        )
//        coEvery { odooXmlRpcClient.fetchReceipts() } returns receipts
//
//        var result: List<Receipt>? = null
//        viewModel.fetchReceiptsAndDisplay { result = it }
//
//        advanceUntilIdle()
//        assertEquals(receipts, result)
//        coVerify { odooXmlRpcClient.fetchReceipts() }
//    }
//
//    @Test
//    fun fetchAndLogBuyerDetails_success() = testScope.runTest {
//        coJustRun { odooXmlRpcClient.fetchAndLogBuyerDetails(any()) }
//
//        viewModel.fetchAndLogBuyerDetails("Receipt 1")
//
//        advanceUntilIdle()
//        coVerify { odooXmlRpcClient.fetchAndLogBuyerDetails("Receipt 1") }
//    }
//
//    @Test
//    fun startPeriodicRefresh_callsOnRefreshRepeatedly() = testScope.runTest {
//        var callCount = 0
//        viewModel.startPeriodicRefresh { callCount++ }
//
//        advanceTimeBy(5000)  // Forward time by 5 seconds
//        assertEquals(1, callCount)
//
//        advanceTimeBy(10000) // Forward time by another 10 seconds
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
//        advanceTimeBy(5000)  // Forward time by 5 more seconds
//        assertEquals(2, callCount)  // Confirm only two refresh calls were made
//    }
//}



package com.example.warehousetet

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptsViewModelTest {

    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var viewModel: ReceiptsViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        odooXmlRpcClient = mockk(relaxed = true)  // relaxed = true helps to avoid having to stub every call
        viewModel = ReceiptsViewModel(odooXmlRpcClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testScope.cancel()  // Ensure all coroutines are cancelled and cleaned up
    }

    @Test
    fun fetchReceiptsAndDisplay_success() = testScope.runTest {
        val receipts = listOf(
            Receipt(id = 1, name = "Receipt 1", date = "2024-01-01", origin = "Origin 1"),
            Receipt(id = 2, name = "Receipt 2", date = "2024-01-02", origin = "Origin 2")
        )
        coEvery { odooXmlRpcClient.fetchReceipts() } returns receipts

        // Create a CompletableDeferred to capture the result
        val resultDeferred = CompletableDeferred<List<Receipt>?>()

        // Trigger the ViewModel to fetch receipts and capture the result
        viewModel.fetchReceiptsAndDisplay {
            resultDeferred.complete(it)
        }

        // Await the result to ensure the coroutine completes before making assertions
        val result = resultDeferred.await()

        // Assertions
        assertEquals(receipts, result)
        coVerify { odooXmlRpcClient.fetchReceipts() }
    }

    @Test
    fun fetchAndLogBuyerDetails_success() = testScope.runTest {
        coJustRun { odooXmlRpcClient.fetchAndLogBuyerDetails(any()) }

        viewModel.fetchAndLogBuyerDetails("Receipt 1")

        advanceUntilIdle()
        coVerify { odooXmlRpcClient.fetchAndLogBuyerDetails("Receipt 1") }
    }

    @Test
    fun startPeriodicRefresh_callsOnRefreshRepeatedly() = testScope.runTest {
        var callCount = 0
        viewModel.startPeriodicRefresh { callCount++ }

        advanceTimeBy(5000)  // Forward time by 5 seconds
        assertEquals(1, callCount)

        advanceTimeBy(10000) // Forward time by another 10 seconds
        assertEquals(3, callCount)

        viewModel.stopPeriodicRefresh()
    }

    @Test
    fun stopPeriodicRefresh_stopsCallingOnRefresh() = testScope.runTest {
        var callCount = 0
        viewModel.startPeriodicRefresh { callCount++ }

        advanceTimeBy(10000)
        viewModel.stopPeriodicRefresh()

        advanceTimeBy(5000)  // Forward time by 5 more seconds
        assertEquals(2, callCount)  // Confirm only two refresh calls were made
    }
}

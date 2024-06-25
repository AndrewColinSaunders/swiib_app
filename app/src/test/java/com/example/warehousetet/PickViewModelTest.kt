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
//class PickViewModelTest {
//
//    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
//    private lateinit var viewModel: PickViewModel
//    private val testDispatcher = StandardTestDispatcher()
//    private val testScope = TestScope(testDispatcher)
//
//    @Before
//    fun setup() {
//        Dispatchers.setMain(testDispatcher)
//        odooXmlRpcClient = mockk(relaxed = true)
//        viewModel = PickViewModel(odooXmlRpcClient, testScope)
//    }
//
//    @After
//    fun tearDown() {
//        Dispatchers.resetMain()
//        testScope.cancel()
//    }
//
//    @Test
//    fun fetchPicksAndDisplay_success() = testScope.runTest {
//        val picks = listOf(
//            Pick(id = 1, name = "Pick 1", date = "2024-01-01", origin = "Origin 1", locationId = "1", locationDestId = "2"),
//            Pick(id = 2, name = "Pick 2", date = "2024-01-02", origin = "Origin 2", locationId = "3", locationDestId = "4")
//        )
//        coEvery { odooXmlRpcClient.fetchPicks() } returns picks
//
//        var result: List<Pick>? = null
//        viewModel.fetchPicksAndDisplay { result = it }
//
//        advanceUntilIdle()
//        assertEquals(picks, result)
//        coVerify { odooXmlRpcClient.fetchPicks() }
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
//        viewModel.fetchAndLogBuyerDetails("Pick 1")
//
//        advanceUntilIdle()
//        coVerify { odooXmlRpcClient.fetchAndLogBuyerDetails("Pick 1") }
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
class PickViewModelTest {

    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var viewModel: PickViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        odooXmlRpcClient = mockk(relaxed = true)
        viewModel = PickViewModel(odooXmlRpcClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testScope.cancel()
    }

    @Test
    fun fetchPicksAndDisplay_success() = testScope.runTest {
        val picks = listOf(
            Pick(id = 1, name = "Pick 1", date = "2024-01-01", origin = "Origin 1", locationId = "1", locationDestId = "2"),
            Pick(id = 2, name = "Pick 2", date = "2024-01-02", origin = "Origin 2", locationId = "3", locationDestId = "4")
        )
        coEvery { odooXmlRpcClient.fetchPicks() } returns picks

        // Create a CompletableDeferred to capture the result
        val resultDeferred = CompletableDeferred<List<Pick>?>()

        // Trigger the ViewModel to fetch picks and capture the result
        viewModel.fetchPicksAndDisplay {
            resultDeferred.complete(it)
        }

        // Await the result to ensure the coroutine completes before making assertions
        val result = resultDeferred.await()

        // Assertions
        assertNotNull(result, "The result should not be null.")
        assertEquals(picks, result, "The fetched picks should match the expected list.")
        coVerify { odooXmlRpcClient.fetchPicks() }
    }

    @Test
    fun fetchAndLogBuyerDetails_success() = testScope.runTest {
        val buyerDetails = BuyerDetails(
            id = 123,
            name = "John Doe",
            login = "john.doe@example.com"
        )
        coEvery { odooXmlRpcClient.fetchAndLogBuyerDetails(any()) } returns buyerDetails

        viewModel.fetchAndLogBuyerDetails("Pick 1")

        advanceUntilIdle()
        coVerify { odooXmlRpcClient.fetchAndLogBuyerDetails("Pick 1") }
    }

    @Test
    fun startPeriodicRefresh_callsOnRefreshRepeatedly() = testScope.runTest {
        var callCount = 0
        viewModel.startPeriodicRefresh { callCount++ }

        advanceTimeBy(5000)
        assertEquals(1, callCount)

        advanceTimeBy(10000)
        assertEquals(3, callCount)

        viewModel.stopPeriodicRefresh()
    }

    @Test
    fun stopPeriodicRefresh_stopsCallingOnRefresh() = testScope.runTest {
        var callCount = 0
        viewModel.startPeriodicRefresh { callCount++ }

        advanceTimeBy(10000)
        viewModel.stopPeriodicRefresh()

        advanceTimeBy(5000)
        assertEquals(2, callCount)
    }
}

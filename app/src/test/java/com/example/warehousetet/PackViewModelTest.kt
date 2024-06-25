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
//class PackViewModelTest {
//
//    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
//    private lateinit var viewModel: PackViewModel
//    private val testDispatcher = StandardTestDispatcher()
//    private val testScope = TestScope(testDispatcher)
//
//    @Before
//    fun setup() {
//        Dispatchers.setMain(testDispatcher)
//        odooXmlRpcClient = mockk(relaxed = true)
//        viewModel = PackViewModel(odooXmlRpcClient, testScope)
//    }
//
//    @After
//    fun tearDown() {
//        Dispatchers.resetMain()
//        testScope.cancel()
//    }
//
//    @Test
//    fun fetchPacksAndDisplay_success() = testScope.runTest {
//        val packs = listOf(
//            Pack(id = 1, name = "Pack 1", date = "2024-01-01", origin = "Origin 1", locationId = "1", locationDestId = "2"),
//            Pack(id = 2, name = "Pack 2", date = "2024-01-02", origin = "Origin 2", locationId = "3", locationDestId = "4")
//        )
//        coEvery { odooXmlRpcClient.fetchPacks() } returns packs
//
//        var result: List<Pack>? = null
//        viewModel.fetchPacksAndDisplay { result = it }
//
//        advanceUntilIdle()
//        assertEquals(packs, result)
//        coVerify { odooXmlRpcClient.fetchPacks() }
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
class PackViewModelTest {

    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
    private lateinit var viewModel: PackViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher) // Set the main dispatcher to the test dispatcher
        odooXmlRpcClient = mockk(relaxed = true)
        viewModel = PackViewModel(odooXmlRpcClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testScope.cancel()  // Ensure all coroutines are cancelled and cleaned up
    }

    @Test
    fun fetchPacksAndDisplay_success() = testScope.runTest {
        val packs = listOf(
            Pack(id = 1, name = "Pack 1", date = "2024-01-01", origin = "Origin 1", locationId = "1", locationDestId = "2"),
            Pack(id = 2, name = "Pack 2", date = "2024-01-02", origin = "Origin 2", locationId = "3", locationDestId = "4")
        )
        coEvery { odooXmlRpcClient.fetchPacks() } returns packs

        // Create a CompletableDeferred to capture the result
        val resultDeferred = CompletableDeferred<List<Pack>?>()

        // Trigger the ViewModel to fetch packs and capture the result
        viewModel.fetchPacksAndDisplay {
            resultDeferred.complete(it)
        }

        // Await the result to ensure the coroutine completes before making assertions
        val result = resultDeferred.await()

        // Assertions
        assertNotNull(result, "The result should not be null.")
        assertEquals(packs, result, "The fetched packs should match the expected list.")
        coVerify { odooXmlRpcClient.fetchPacks() }
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

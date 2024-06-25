//package com.example.warehousetet
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import androidx.test.ext.junit.runners.AndroidJUnit4
//import androidx.test.platform.app.InstrumentationRegistry
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.test.resetMain
//import kotlinx.coroutines.test.setMain
//import org.junit.*
//import org.junit.runner.RunWith
//import org.mockito.Mock
//import org.mockito.Mockito
//import org.mockito.MockitoAnnotations
//
//@RunWith(AndroidJUnit4::class)
//class ReceiptsAdapterTest {
//
//    private lateinit var receiptsAdapter: ReceiptsAdapter
//    private val receipts = listOf(
//        Receipt(1, "Receipt 1", "2023-01-01", "Origin 1"),
//        Receipt(2, "Receipt 2", "2023-01-02", "Origin 2")
//    )
//
//    @Mock
//    private lateinit var onReceiptClicked: (Receipt) -> Unit
//
//    @Before
//    fun setUp() {
//        Dispatchers.setMain(Dispatchers.Unconfined)
//        MockitoAnnotations.openMocks(this)
//        receiptsAdapter = ReceiptsAdapter(receipts, onReceiptClicked)
//    }
//
//    @After
//    fun tearDown() {
//        Dispatchers.resetMain()
//    }
//
//    @Test
//    fun testGetItemCount() {
//        Assert.assertEquals(2, receiptsAdapter.itemCount)
//    }
//
//    @Test
//    fun testOnBindViewHolder() {
//        // Create a mock ViewGroup
//        val parent = Mockito.mock(ViewGroup::class.java)
//        Mockito.`when`(parent.context).thenReturn(InstrumentationRegistry.getInstrumentation().targetContext)
//        val inflater = LayoutInflater.from(InstrumentationRegistry.getInstrumentation().targetContext)
//        val view = inflater.inflate(R.layout.receipt_item, parent, false)
//
//        // Now create the viewHolder with the mocked parent
//        val viewHolder = receiptsAdapter.ViewHolder(view, onReceiptClicked)
//        receiptsAdapter.onBindViewHolder(viewHolder, 0)
//
//        Assert.assertEquals("Receipt 1", viewHolder.itemView.findViewById<TextView>(R.id.receiptNameTextView).text)
//        Assert.assertEquals("2023-01-01", viewHolder.itemView.findViewById<TextView>(R.id.receiptDateTextView).text)
//        Assert.assertEquals("Origin 1", viewHolder.itemView.findViewById<TextView>(R.id.receiptOriginTextView).text)
//    }
//
//    @Test
//    fun testFilter() {
//        receiptsAdapter.filter("Receipt 1")
//
//        Assert.assertEquals(1, receiptsAdapter.itemCount)
//        Assert.assertEquals("Receipt 1", receiptsAdapter.filteredReceipts[0].name)
//    }
//
//    @Test
//    fun testUpdateReceipts() {
//        val newReceipts = listOf(
//            Receipt(3, "Receipt 3", "2023-01-03", "Origin 3")
//        )
//        receiptsAdapter.updateReceipts(newReceipts)
//
//        Assert.assertEquals(1, receiptsAdapter.itemCount)
//        Assert.assertEquals("Receipt 3", receiptsAdapter.filteredReceipts[0].name)
//    }
//}

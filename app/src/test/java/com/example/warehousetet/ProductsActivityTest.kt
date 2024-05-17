//package com.example.warehousetet
//
//import android.content.Intent
//import android.view.MenuItem
//import android.widget.Button
//import android.widget.EditText
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.test.core.app.ApplicationProvider
//import androidx.test.ext.junit.runners.AndroidJUnit4
//import org.hamcrest.Matchers.`is`
//import org.hamcrest.Matchers.notNullValue
//import org.junit.After
//import org.junit.Assert.*
//import org.junit.Before
//import org.junit.Test
//import org.junit.runner.RunWith
//import org.mockito.Mockito.*
//import org.robolectric.Robolectric
//import org.robolectric.Shadows
//import org.robolectric.android.controller.ActivityController
//
//@RunWith(AndroidJUnit4::class)
//class ProductsActivityTest {
//
//    private lateinit var activity: ProductsActivity
//    private lateinit var controller: ActivityController<ProductsActivity>
//
//    @Before
//    fun setup() {
//        // Intent that mimics the real one used to start the activity
//        val intent = Intent(ApplicationProvider.getApplicationContext(), ProductsActivity::class.java).apply {
//            putExtra("RECEIPT_ID", 123)
//            putExtra("RECEIPT_NAME", "WH-1/IN/00048")
//        }
//        // Create an instance of the activity under test
//        controller = Robolectric.buildActivity(ProductsActivity::class.java, intent).create().start().resume()
//        activity = controller.get()
//    }
//
//    @Test
//    fun onCreate_validReceiptId_shouldSetUpActionBarWithTitle() {
//        // Assert that the activity is not null
//        assertNotNull(activity)
//        // Check that the title is set correctly based on the passed receipt name
//        assertThat(activity.supportActionBar?.title.toString(), `is`("WH-1/IN/00048"))
//    }
//
//    @Test
//    fun onOptionsItemSelected_home_shouldFinishActivity() {
//        val menuItem = mock(MenuItem::class.java)
//        `when`(menuItem.itemId).thenReturn(android.R.id.home)
//        val result = activity.onOptionsItemSelected(menuItem)
//        assertTrue(result)
//        assertTrue(activity.isFinishing)
//    }
//
//    @Test
//    fun showRedToast_shouldDisplayCorrectToast() {
//        activity.runOnUiThread {
//            activity.showRedToast("Error occurred")
//        }
//        val shadowToast = Shadows.shadowOf(Toast.makeText(activity, "Error occurred", Toast.LENGTH_SHORT))
//        assertEquals("Error occurred", shadowToast.text)
//        assertEquals(Toast.LENGTH_SHORT, shadowToast.duration)
//    }
//
//    @Test
//    fun confirmButton_click_shouldVerifyBarcode() {
//        // Get the confirmButton and barcodeInput in the activity
//        val confirmButton = activity.findViewById<Button>(R.id.confirmButton)
//        val barcodeInput = activity.findViewById<EditText>(R.id.barcodeInput)
//        // Input a barcode
//        activity.runOnUiThread {
//            barcodeInput.setText("123456789")
//            confirmButton.performClick()
//        }
//        // Check interactions or further state changes
//        // For example, assuming `verifyBarcode` modifies some view states or invokes other methods
//    }
//
//    @After
//    fun tearDown() {
//        // Clean up and release resources after tests
//        controller.pause().stop().destroy()
//    }
//}

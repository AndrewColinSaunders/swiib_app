//package com.example.warehousetet
//
//import android.app.Activity
//import android.content.Context
//import android.content.Intent
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.view.KeyEvent
//import android.view.inputmethod.EditorInfo
//import android.widget.Button
//import android.widget.EditText
//import androidx.activity.result.ActivityResult
//import androidx.appcompat.app.ActionBar
//import androidx.core.content.ContextCompat
//import androidx.lifecycle.Lifecycle
//import androidx.test.core.app.ActivityScenario
//import androidx.test.espresso.matcher.ViewMatchers.assertThat
//import androidx.test.ext.junit.runners.AndroidJUnit4
//import io.mockk.mockk
//import kotlinx.coroutines.runBlocking
//import org.hamcrest.CoreMatchers.equalTo
//import org.junit.Assert.assertEquals
//import org.junit.Assert.assertTrue
//import org.junit.Before
//import org.junit.Test
//import org.junit.runner.RunWith
//import java.io.ByteArrayOutputStream
//import org.junit.Assert.assertNotNull
//
//@RunWith(AndroidJUnit4::class)
//class ProductsActivityTest {
//
//    private lateinit var scenario: ActivityScenario<ProductsActivity>
//    private lateinit var activity: ProductsActivity
//    private lateinit var barcodeInput: EditText
//    private lateinit var confirmButton: Button
//
//    @Before
//    fun setUp() {
//        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<Context>()
//        val intent = Intent(context, ProductsActivity::class.java).apply {
//            putExtra("RECEIPT_ID", 1)
//            putExtra("RECEIPT_NAME", "Test Receipt")
//        }
//        scenario = ActivityScenario.launch(intent)
//        scenario.onActivity {
//            activity = it
//            barcodeInput = activity.findViewById(R.id.barcodeInput)
//            confirmButton = activity.findViewById(R.id.confirmButton)
//        }
//    }
//
//    @Test
//    fun testSetupActionBar() {
//        scenario.onActivity { activity ->
//            activity.receiptName = "Test Receipt"
//            activity.setupActionBar()
//
//            val actionBar: ActionBar? = activity.supportActionBar
//            assertEquals("Test Receipt", actionBar?.title)
//        }
//    }
//
//    @Test
//    fun testInitializeFields() {
//        scenario.onActivity { activity ->
//            activity.initializeFields()
//
//            assertEquals(1, activity.receiptId)
//            assertEquals("Test Receipt", activity.receiptName)
//            assert(activity.barcodeInput is EditText)
//            assert(activity.confirmButton is Button)
//        }
//    }
//
//    @Test
//    fun testHandleCameraResult_success() {
//        scenario.moveToState(Lifecycle.State.CREATED).onActivity { activity ->
//            val bitmap = BitmapFactory.decodeResource(activity.resources, R.drawable.test_image)
//            val byteArrayOutputStream = ByteArrayOutputStream()
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
//            val byteArray = byteArrayOutputStream.toByteArray()
//            val intent = Intent().apply {
//                putExtra("data", byteArray)
//            }
//            val result = ActivityResult(Activity.RESULT_OK, intent)
//
//            runBlocking {
//                activity.handleCameraResult(result)
//            }
//        }
//    }
//
//    @Test
//    fun testProcessImage() {
//        scenario.onActivity { activity ->
//            val bitmap = BitmapFactory.decodeResource(activity.resources, R.drawable.test_image)
//            val encodedImage = activity.processImage(bitmap)
//
//            // Assert that the encoded image is not empty
//            assert(encodedImage.isNotEmpty())
//        }
//    }
//
//    @Test
//    fun testOnResume() {
//        scenario.onActivity { activity ->
//            val validateButton: Button = activity.findViewById(R.id.pickValidateButton)
//            val sharedPref = activity.getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
//            sharedPref.edit().putBoolean("ValidateButtonVisible_1", true).apply()
//            activity.onResume()
//            assertEquals(Button.VISIBLE, validateButton.visibility)
//        }
//    }
//
//    @Test
//    fun testOnCreateOptionsMenu() {
//        scenario.onActivity { activity ->
//            val menu = androidx.appcompat.view.menu.MenuBuilder(activity)
//            val inflater = activity.menuInflater
//            inflater.inflate(R.menu.menu_products_activity, menu)
//            activity.onCreateOptionsMenu(menu)
//            val menuItem = menu.findItem(R.id.action_flag_receipt)
//            assertEquals("Flag", menuItem.title.toString())
//        }
//    }
//
//    @Test
//    fun testOnPrepareOptionsMenu() {
//        scenario.onActivity { activity ->
//            val menu = androidx.appcompat.view.menu.MenuBuilder(activity)
//            val inflater = activity.menuInflater
//            inflater.inflate(R.menu.menu_products_activity, menu)
//            activity.onPrepareOptionsMenu(menu)
//            val menuItem = menu.findItem(R.id.action_flag_receipt)
//            assertEquals("Flag", menuItem.title.toString())
//            val spanString = menuItem.title as android.text.SpannableString
//            val spans = spanString.getSpans(0, spanString.length, android.text.style.ForegroundColorSpan::class.java)
//            assert(spans.isNotEmpty())
//            assertThat(spans[0].foregroundColor, equalTo(ContextCompat.getColor(activity, R.color.danger_red)))
//        }
//    }
//
//    @Test
//    fun testSetupBarcodeVerification() {
//        scenario.onActivity { activity ->
//            activity.setupBarcodeVerification(1)
//
//            // Simulate pressing the Enter key
//            val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
//            val editorActionResult = activity.handleEditorActions(EditorInfo.IME_ACTION_DONE, event, 1)
//            assertTrue(editorActionResult)
//
//            // Simulate clicking the confirm button
//            confirmButton.performClick()
//            // Assert that the barcode input is cleared after clicking the confirm button
//            assertEquals("", barcodeInput.text.toString())
//        }
//    }
//
//    @Test
//    fun testHandleEditorActions() {
//        scenario.onActivity { activity ->
//            activity.setupBarcodeVerification(1)
//
//            // Simulate pressing the Enter key
//            val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
//            val result = activity.handleEditorActions(EditorInfo.IME_ACTION_DONE, event, 1)
//
//            assertTrue(result)
//        }
//    }
//
//    @Test
//    fun testPerformBarcodeVerification() {
//        scenario.onActivity { activity ->
//            activity.setupBarcodeVerification(1)
//
//            // Set a test barcode
//            barcodeInput.setText("testBarcode")
//
//            // Simulate clicking the confirm button
//            confirmButton.performClick()
//
//            // Assert that the barcode input is cleared
//            assertEquals("", barcodeInput.text.toString())
//        }
//    }
//}
//





package com.example.warehousetet

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.ActionBar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class ProductsActivityTest {

    private lateinit var scenario: ActivityScenario<ProductsActivity>
    private lateinit var activity: ProductsActivity
    private lateinit var barcodeInput: EditText
    private lateinit var confirmButton: Button

    @MockK
    lateinit var mockContext: Context

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, ProductsActivity::class.java).apply {
            putExtra("RECEIPT_ID", 1)
            putExtra("RECEIPT_NAME", "Test Receipt")
        }
        scenario = ActivityScenario.launch(intent)
        scenario.onActivity {
            activity = it
            barcodeInput = activity.findViewById(R.id.barcodeInput)
            confirmButton = activity.findViewById(R.id.confirmButton)
        }
    }

    @Test
    fun testSetupActionBar() {
        scenario.onActivity { activity ->
            activity.receiptName = "Test Receipt"
            activity.setupActionBar()

            val actionBar: ActionBar? = activity.supportActionBar
            assertEquals("Test Receipt", actionBar?.title)
        }
    }

    @Test
    fun testInitializeFields() {
        scenario.onActivity { activity ->
            activity.initializeFields()

            assertEquals(1, activity.receiptId)
            assertEquals("Test Receipt", activity.receiptName)
            assert(activity.barcodeInput is EditText)
            assert(activity.confirmButton is Button)
        }
    }

    @Test
    fun testHandleCameraResult_success() {
        scenario.moveToState(Lifecycle.State.CREATED).onActivity { activity ->
            val bitmap = BitmapFactory.decodeResource(activity.resources, R.drawable.test_image)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            val intent = Intent().apply {
                putExtra("data", byteArray)
            }
            val result = ActivityResult(Activity.RESULT_OK, intent)

            runBlocking {
                activity.handleCameraResult(result)
            }
        }
    }

    @Test
    fun testProcessImage() {
        scenario.onActivity { activity ->
            val bitmap = BitmapFactory.decodeResource(activity.resources, R.drawable.test_image)
            val encodedImage = activity.processImage(bitmap)

            // Assert that the encoded image is not empty
            assert(encodedImage.isNotEmpty())
        }
    }

    @Test
    fun testOnResume() {
        scenario.onActivity { activity ->
            val validateButton: Button = activity.findViewById(R.id.pickValidateButton)
            val sharedPref = activity.getSharedPreferences("ProductMatchStates", Context.MODE_PRIVATE)
            sharedPref.edit().putBoolean("ValidateButtonVisible_1", true).apply()
            activity.onResume()
            assertEquals(Button.VISIBLE, validateButton.visibility)
        }
    }

    @Test
    fun testOnCreateOptionsMenu() {
        scenario.onActivity { activity ->
            val menu = androidx.appcompat.view.menu.MenuBuilder(activity)
            val inflater = activity.menuInflater
            inflater.inflate(R.menu.menu_products_activity, menu)
            activity.onCreateOptionsMenu(menu)
            val menuItem = menu.findItem(R.id.action_flag_receipt)
            assertEquals("Flag", menuItem.title.toString())
        }
    }

    @Test
    fun testOnPrepareOptionsMenu() {
        scenario.onActivity { activity ->
            val menu = androidx.appcompat.view.menu.MenuBuilder(activity)
            val inflater = activity.menuInflater
            inflater.inflate(R.menu.menu_products_activity, menu)
            activity.onPrepareOptionsMenu(menu)
            val menuItem = menu.findItem(R.id.action_flag_receipt)
            assertEquals("Flag", menuItem.title.toString())
            val spanString = menuItem.title as android.text.SpannableString
            val spans = spanString.getSpans(0, spanString.length, android.text.style.ForegroundColorSpan::class.java)
            assert(spans.isNotEmpty())
            assertThat(spans[0].foregroundColor, equalTo(ContextCompat.getColor(activity, R.color.danger_red)))
        }
    }

    @Test
    fun testSetupBarcodeVerification() {
        scenario.onActivity { activity ->
            activity.setupBarcodeVerification(1)

            // Simulate pressing the Enter key
            val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
            val editorActionResult = activity.handleEditorActions(EditorInfo.IME_ACTION_DONE, event, 1)
            assertTrue(editorActionResult)

            // Simulate clicking the confirm button
            confirmButton.performClick()
            // Assert that the barcode input is cleared after clicking the confirm button
            assertEquals("", barcodeInput.text.toString())
        }
    }

    @Test
    fun testHandleEditorActions() {
        scenario.onActivity { activity ->
            activity.setupBarcodeVerification(1)

            // Simulate pressing the Enter key
            val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
            val result = activity.handleEditorActions(EditorInfo.IME_ACTION_DONE, event, 1)

            assertTrue(result)
        }
    }

    @Test
    fun testPerformBarcodeVerification() {
        scenario.onActivity { activity ->
            activity.setupBarcodeVerification(1)

            // Set a test barcode
            barcodeInput.setText("testBarcode")

            // Simulate clicking the confirm button
            confirmButton.performClick()

            // Assert that the barcode input is cleared
            assertEquals("", barcodeInput.text.toString())
        }
    }
}















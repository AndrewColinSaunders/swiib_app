package com.example.warehousetet

import android.content.Intent
import androidx.appcompat.app.ActionBar
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductsActivityTest {

    private lateinit var scenario: ActivityScenario<ProductsActivity>

    @Before
    fun setUp() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, ProductsActivity::class.java).apply {
            putExtra("RECEIPT_ID", 1)
            putExtra("RECEIPT_NAME", "Test Receipt")
        }
        scenario = ActivityScenario.launch(intent)
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
}

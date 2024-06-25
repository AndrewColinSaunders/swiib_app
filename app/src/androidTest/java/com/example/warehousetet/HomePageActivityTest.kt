//package com.example.warehousetet
//
//import android.content.Intent
//import androidx.test.core.app.ActivityScenario
//import androidx.test.core.app.ApplicationProvider
//import androidx.test.espresso.Espresso.onView
//import androidx.test.espresso.action.ViewActions.click
//import androidx.test.espresso.intent.Intents
//import androidx.test.espresso.intent.matcher.IntentMatchers
//import androidx.test.ext.junit.runners.AndroidJUnit4
//import com.example.warehousetet.HomePageActivity
//import com.example.warehousetet.InternalTransfersActivity
//import com.example.warehousetet.PickActivity
//import com.example.warehousetet.ReceiptsActivity
//import org.junit.runner.RunWith
//import org.junit.Before
//import org.junit.Test
//import org.junit.After
//import androidx.test.espresso.matcher.ViewMatchers.withId
//import com.example.warehousetet.R
//
//@RunWith(AndroidJUnit4::class)
//class HomePageActivityTest {
//
//    private lateinit var scenario: ActivityScenario<HomePageActivity>
//
//    @Before
//    fun setup() {
//        Intents.init()
//        val intent = Intent(ApplicationProvider.getApplicationContext(), HomePageActivity::class.java)
//        intent.putExtra("USER_ID", 6) // Ensure USER_ID is what HomePageActivity expects
//        scenario = ActivityScenario.launch(intent)
//        // Remove the explicit moveToState to let the activity lifecycle proceed naturally
//    }
//
//    @After
//    fun tearDown() {
//        scenario.close()
//        Intents.release()
//    }
//
//    @Test
//    fun clickingReceiptButton_shouldStartReceiptActivity() {
//        onView(withId(R.id.btnReceipt)).perform(click())
//        Intents.intended(IntentMatchers.hasComponent(ReceiptsActivity::class.java.name))
//    }
//
//    @Test
//    fun clickingInternalTransfersButton_shouldStartInternalTransfersActivity() {
//        onView(withId(R.id.btnInternalTransfers)).perform(click())
//        Intents.intended(IntentMatchers.hasComponent(InternalTransfersActivity::class.java.name))
//    }
//
//    @Test
//    fun clickingPickButton_shouldStartPickActivity() {
//        onView(withId(R.id.btnPick)).perform(click())
//        Intents.intended(IntentMatchers.hasComponent(PickActivity::class.java.name))
//    }
//}
//

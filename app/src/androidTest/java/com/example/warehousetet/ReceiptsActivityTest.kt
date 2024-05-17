import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.example.warehousetet.ProductsActivity
import com.example.warehousetet.R
import com.example.warehousetet.ReceiptsActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.hamcrest.Matchers.* // Make sure it's imported if you're using it directly
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.After
import org.junit.Before


@RunWith(AndroidJUnit4::class)
class ReceiptsActivityTest {

    @get:Rule
    var activityRule: ActivityTestRule<ReceiptsActivity> = ActivityTestRule(ReceiptsActivity::class.java, true, true)

    @Before
    fun setUp() {
        Intents.init()
    }
    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun testSearchReceipts() {
        // First, click to ensure the SearchView is expanded and ready
        onView(withId(R.id.action_widget_button)).perform(click())

        // Now, wait for the SearchView to be ready to receive input.
        // This is a placeholder; use an IdlingResource if possible.
        Thread.sleep(1000)  // Not recommended for production code.

        // Perform the text input into the SearchView.
        onView(isAssignableFrom(SearchView::class.java))
            .perform(click())  // Ensure the SearchView has focus.
            .perform(typeText("WH-1/IN/00048"), pressImeActionButton())

        // Check if RecyclerView updates based on the search query.
        onView(withId(R.id.receiptsRecyclerView))
            .check(matches(hasDescendant(withText("WH-1/IN/00048"))))
    }



    @Test
    fun testNavigationToProductsActivity() {
        // Simulate clicking on a receipt
        onView(withId(R.id.receiptsRecyclerView)).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // Check if the intent to start ProductsActivity was created with the correct extras
        val expectedIntent = IntentMatchers.hasComponent(ProductsActivity::class.java.name)
        Intents.intended(expectedIntent)
    }

    @Test
    fun testRecyclerViewVisibility() {
        // Check if the RecyclerView is displayed when activity starts
        onView(withId(R.id.receiptsRecyclerView)).check(matches(isDisplayed()))
    }

    @Test
    fun testPeriodicRefresh() {
        // Ensure that the periodic refresh is functioning (this may require mocking or a different approach to verify)
    }
}




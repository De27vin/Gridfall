package com.example.gridfall.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gridfall.network.*
import com.example.gridfall.ui.theme.GridfallTheme
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.delay
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CommunityStarRatingDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private class Gateway(initial: Int? = null) : CommunityUiGateway(communityUiFixture(2)) {
        var saved = initial
        var fail = false
        val submitted = mutableListOf<Int>()
        override suspend fun fetch(code: String) = map.copy(yourRating = saved)
        override suspend fun rate(code: String, stars: Int, token: String): CommunityPublication {
            submitted.add(stars); delay(200)
            if (fail) throw IOException("Local rating failure")
            saved = stars
            return map.copy(yourRating = stars)
        }
    }
    private fun open(gateway: Gateway, reopenable: Boolean = false) {
        compose.setContent { GridfallTheme { CommunityMapsTheme {
            var opened by remember { mutableStateOf(true) }
            if (opened) CommunityMapDialog(null, gateway, true, { "local-fixture" }, { _, _ -> }, { opened = false }, initialCode = gateway.map.code)
            else if (reopenable) androidx.compose.material3.Button(onClick = { opened = true }) { androidx.compose.material3.Text("Reopen fixture") }
        } } }
        ready()
    }
    private fun ready() = compose.waitUntil(5_000) { compose.onAllNodesWithTag("community-rating-row").fetchSemanticsNodes().isNotEmpty() }
    private fun assertFilled(stars: Int) {
        for (i in 1..5) {
            val node = compose.onNodeWithTag("community-rating-$i")
            if (i <= stars) node.assertIsSelected() else node.assertIsNotSelected()
        }
    }
    private fun select(gateway: Gateway, stars: Int) {
        compose.onNodeWithTag("community-rating-row").performScrollTo()
        compose.onNodeWithTag("community-rating-$stars").performClick()
        compose.waitUntil(5_000) { gateway.saved == stars && runCatching { compose.onNodeWithTag("community-rating-5").assertIsEnabled() }.isSuccess }
        assertFilled(stars)
    }
    @Test fun unratedAndCumulativeFourTwoFiveWithoutVisibleNumbers() {
        val gateway = Gateway(); open(gateway); assertFilled(0)
        compose.onNodeWithTag("community-rating-row").onChildren().assertCountEquals(5)
        (1..5).forEach { assertFalse(compose.onNodeWithTag("community-rating-$it").fetchSemanticsNode().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Text)) }
        select(gateway, 4); select(gateway, 2); select(gateway, 5)
        assertEquals(listOf(4, 2, 5), gateway.submitted)
    }
    @Test fun existingPersonalRatingAndReopenUseSavedVoteRatherThanAverage() {
        val gateway = Gateway(4); open(gateway, true); assertFilled(4)
        select(gateway, 2)
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Reopen fixture").performClick(); ready(); assertFilled(2)
    }
    @Test fun failedRequestRestoresLastAcknowledgedRatingAndShowsError() {
        val gateway = Gateway(2); open(gateway); assertFilled(2); gateway.fail = true
        compose.onNodeWithTag("community-rating-row").performScrollTo()
        compose.onNodeWithTag("community-rating-4").performTouchInput { click(); click() }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("community-rating-error").fetchSemanticsNodes().isNotEmpty() }
        assertFilled(2); assertEquals(2, gateway.saved); assertEquals(listOf(4), gateway.submitted)
        compose.onNodeWithTag("community-rating-error").performScrollTo().assertTextEquals("Local rating failure")
        gateway.fail = false; select(gateway, 5)
        compose.onNodeWithTag("community-rating-error").assertDoesNotExist()
    }
    @Test fun successfulLocalRatingsAreScopedByAccountAndSurviveStoreRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val account = "star-review-${UUID.randomUUID()}"; val code = "GF-A7K9P23456"
        val store = CommunityRatingStore(context)
        assertNull(store.load(account, code))
        store.save(account, code, 4)
        assertEquals(4, CommunityRatingStore(context).load(account, code))
        assertNull(store.load("another-$account", code))
        assertNull(store.load(account, "GF-B7K9P23456"))
    }
}

package com.example.gridfall.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.gridfall.game.*
import com.example.gridfall.network.*
import com.example.gridfall.ui.theme.GridfallTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CommunityVisibilityDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val saved = SavedCustomMap("local-visibility-fixture", "Visibility fixture", communityUiFixture(1).document.design)
    private val public = "Public · marketplace"
    private val unlisted = "Unlisted · code only"
    private class Gateway : CommunityUiGateway(communityUiFixture(1)) {
        var submittedVisibility: String? = null
        var submittedDocument: String? = null
        var pending: PendingPublication? = null
        override suspend fun pendingPublication(token: String, mapId: String) = pending
        override suspend fun publish(token: String, document: String, visibility: String, requestId: String): CommunityPublication {
            submittedVisibility = visibility; submittedDocument = document
            return map.copy(document = CommunityMapFormat.decode(document), visibility = visibility)
        }
    }
    private fun open(gateway: Gateway, restoration: StateRestorationTester? = null) {
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            GridfallTheme { CommunityMapsTheme { CommunityMapDialog(saved, gateway, true, { "local-fixture-token" }, { _, _ -> }, {}) } }
        }
        if (restoration == null) compose.setContent(content) else restoration.setContent(content)
        compose.waitUntil(5_000) { runCatching { compose.onNodeWithText("Publish Map").assertIsEnabled() }.isSuccess }
    }
    @Test fun freshDialogShowsPublicFirstAndPublishesOnlyAfterConfirmation() {
        val gateway = Gateway(); open(gateway)
        compose.onNodeWithText(public).assertIsSelected()
        compose.onNodeWithText(unlisted).assertIsNotSelected()
        assertTrue(compose.onNodeWithText(public).fetchSemanticsNode().boundsInRoot.top <
            compose.onNodeWithText(unlisted).fetchSemanticsNode().boundsInRoot.top)
        assertNull(gateway.submittedVisibility)
        compose.onNodeWithText("Publish Map").performScrollTo().performClick()
        compose.waitUntil(5_000) { gateway.submittedVisibility != null }
        assertEquals("public", gateway.submittedVisibility)
    }
    @Test fun explicitUnlistedChoiceIsSubmitted() {
        val gateway = Gateway(); open(gateway)
        compose.onNodeWithText(unlisted).performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Publish Map").performScrollTo().performClick()
        compose.waitUntil(5_000) { gateway.submittedVisibility != null }
        assertEquals("unlisted", gateway.submittedVisibility)
    }
    @Test fun explicitChoiceSurvivesSavedStateRestoration() {
        val gateway = Gateway(); val restoration = StateRestorationTester(compose); open(gateway, restoration)
        compose.onNodeWithText(unlisted).performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText(unlisted).assertIsSelected()
        assertNull(gateway.submittedVisibility)
        compose.onNodeWithText("Publish Map").performScrollTo().performClick()
        compose.waitUntil(5_000) { gateway.submittedVisibility != null }
        assertEquals("unlisted", gateway.submittedVisibility)
    }
    @Test fun uncertainUnlistedSnapshotKeepsItsExistingVisibility() {
        val document = CommunityMapFormat.export("Previous snapshot", "Original description", saved.design)
        val gateway = Gateway().also { it.pending = PendingPublication(document, "unlisted") }; open(gateway)
        compose.onNodeWithText(unlisted).assertIsSelected().assertIsNotEnabled()
        compose.onNodeWithText(public).assertIsNotSelected().assertIsNotEnabled()
        compose.onNodeWithText("Publish Map").performScrollTo().performClick()
        compose.waitUntil(5_000) { gateway.submittedVisibility != null }
        assertEquals("unlisted", gateway.submittedVisibility)
        assertEquals(document, gateway.submittedDocument)
    }
}

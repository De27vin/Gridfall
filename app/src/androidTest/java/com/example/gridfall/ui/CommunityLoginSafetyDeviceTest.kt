package com.example.gridfall.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.gridfall.ui.theme.GridfallTheme
import com.google.firebase.auth.FirebaseAuth
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** No login submitted, no credentials inspected, no network fixtures. */
class CommunityLoginSafetyDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun qaLoginCanBeDismissedWithoutCreatingOrSwitchingIdentities() {
        val before = FirebaseAuth.getInstance().currentUser?.uid
        compose.setContent { GridfallTheme {
            GameScreen(autoCreateGuest = false, startInLogin = true)
        } }
        compose.onNodeWithText("Email").assertExists()
        compose.onNodeWithText("Password").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithTag("gameplay").assertIsDisplayed()
        assertTrue("Opening QA login must preserve the existing Firebase session",
            before == FirebaseAuth.getInstance().currentUser?.uid)
    }
}

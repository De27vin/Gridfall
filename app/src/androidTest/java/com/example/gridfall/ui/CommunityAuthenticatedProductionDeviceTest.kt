package com.example.gridfall.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gridfall.auth.GridfallAuthManager
import com.example.gridfall.auth.AuthPromptStore
import com.example.gridfall.game.*
import com.example.gridfall.network.*
import com.example.gridfall.ui.theme.GridfallTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.security.MessageDigest
import java.util.UUID

/** Explicit, manually phased live QA. Never logs identities/tokens or touches pre-existing maps.
 * Run ONLY this class with productionAuthenticated=accountA|accountB|cleanup after manual login.
 * Run metadata survives app restarts; cleanup requires the same owner and exact document.
 */
class CommunityAuthenticatedProductionDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs get() = context.getSharedPreferences("authenticated_community_qa_v1", 0)
    private fun identity(): String {
        val user = FirebaseAuth.getInstance().currentUser ?: error("Manual login required; no account created by test")
        check(!user.isAnonymous) { "Existing named Account A/B required" }
        return MessageDigest.getInstance("SHA-256").digest(user.uid.toByteArray()).joinToString("") { "%02x".format(it) }
    }
    private suspend fun expectStatus(status: Int, action: suspend () -> Any?) {
        val failure = runCatching { action() }.exceptionOrNull()
        assertTrue("Expected API status $status", failure is CommunityApiException)
        assertEquals(status, (failure as CommunityApiException).status)
    }
    @Test fun manuallyAuthorizedProductionPhase(): Unit = runBlocking {
        val phase = InstrumentationRegistry.getArguments().getString("productionAuthenticated")
        assumeTrue(phase in listOf("accountA", "accountB", "cleanup"))
        assertEquals("https://api.gridfall.site", ApiConfig.baseUrl)
        val owner = identity()
        val token = GridfallAuthManager().getFreshIdToken() // In-memory only; never emit it.
        val me = GridfallApiClient().getMe(token)
        check(!me.isAnonymous && !me.username.isNullOrBlank()) { "Existing completed profile required" }
        val client = CommunityMapsClient()
        when (phase) {
            "accountA" -> {
                check(!prefs.getBoolean("cleaned", false)) { "Previous QA finished; explicitly clear only QA metadata before another run" }
                val previous = prefs.getString("owner", null)
                check(previous == null || previous == owner) { "Account A changed; refusing publication" }
                val id = prefs.getString("request", null) ?: UUID.randomUUID().toString()
                val document = prefs.getString("document", null) ?: CommunityMapFormat.export(
                    "QA-" + id.take(8), "Temporary owner-authorized production E2E test; will be unpublished.",
                    CustomMapDesign(5, 4, setOf(Cell(0, 0)), listOf(
                        MapBlockDefinition("one", "Single", setOf(Cell(0, 0)), 1000, 2))))
                check(prefs.edit().putString("owner", owner).putString("request", id).putString("document", document).commit())
                val map = client.publish(token, document, "public", id)
                check(prefs.edit().putString("code", map.code).commit())
                assertEquals(map.code, CommunityMapsClient().publish(token, document, "public", id).code)
                assertEquals(map.document, client.fetch(map.code.lowercase()).document)
                assertTrue(client.browse("", "newest", null, token).items.any { it.code == map.code })
                expectStatus(403) { client.rate(map.code, 5, token) }
                check(prefs.edit().putBoolean("accountACompleted", true).commit())
            }
            "accountB" -> {
                check(prefs.getBoolean("accountACompleted", false)) { "Complete Account A first" }
                check(owner != prefs.getString("owner", null)) { "Sign in as a different Account B" }
                val previous = prefs.getString("player", null)
                check(previous == null || previous == owner) { "Account B changed" }
                check(prefs.edit().putString("player", owner).commit())
                val code = checkNotNull(prefs.getString("code", null))
                val map = client.fetch(code)
                assertEquals(prefs.getString("document", null), CommunityMapFormat.export(map.document.name, map.document.description, map.document.design))
                assertTrue(client.browse(map.document.name, "newest", null).items.any { it.code == code })
                assertFalse(client.browse("", "newest", null, token).items.any { it.code == code })
                expectStatus(404) { client.unpublish(code, token) }
                val id = prefs.getString("import", null) ?: UUID.randomUUID().toString()
                check(prefs.edit().putString("import", id).commit())
                val baselineDownloads = prefs.getInt("downloadBefore", map.downloads)
                check(prefs.edit().putInt("downloadBefore", baselineDownloads).commit())
                val downloaded = client.importMap(code, id, token)
                assertEquals(baselineDownloads + 1, downloaded.downloads)
                assertEquals(downloaded.downloads, client.importMap(code, id, token).downloads)
                assertEquals(downloaded.downloads, client.importMap(code, UUID.randomUUID().toString(), token).downloads)
                val rated = client.rate(code, 4, token)
                val updated = client.rate(code, 5, token)
                assertEquals(rated.ratingCount, updated.ratingCount)
                assertEquals(5.0, updated.ratingAverage, 0.0)
                client.report(code, "other", "Owner-authorized temporary E2E QA report; not real inappropriate content.", token)
                // Actual GameScreen, production gateway, real import; no injected network responses.
                val runStore = InProgressRunStore(context)
                val priorRun = runStore.load()
                val priorGrid = GridLayoutPreferenceStore.load(context)
                val priorPrompt = AuthPromptStore.isSavePromptDismissed(context)
                val mounted = androidx.compose.runtime.mutableStateOf(true)
                try {
                    AuthPromptStore.setSavePromptDismissed(context, true)
                    runStore.clear()
                    compose.setContent { if (mounted.value) GridfallTheme { GameScreen(autoCreateGuest = false, connectExistingAccount = true) } }
                    compose.onNodeWithText("Settings", ignoreCase = true).performClick()
                    compose.waitUntil(30_000) { compose.onAllNodesWithText("Username: ${me.username}").fetchSemanticsNodes().isNotEmpty() }
                    compose.onNodeWithText("Enter Map Code").performScrollTo().performClick()
                    compose.onNodeWithText("Map code").performTextInput(code)
                    compose.onNodeWithText("Preview").performScrollTo().performClick()
                    compose.waitUntil(30_000) { compose.onAllNodesWithText("Import & Play").fetchSemanticsNodes().isNotEmpty() }
                    compose.onNodeWithText("Import & Play").assertIsDisplayed().performClick()
                    compose.waitUntil(30_000) { compose.onAllNodesWithTag("gameplay").fetchSemanticsNodes().isNotEmpty() }
                    compose.onNodeWithTag("gameplay").assertIsDisplayed()
                    compose.onNodeWithText("Return to Game").assertDoesNotExist()
                    compose.onNodeWithTag("community-maps").assertDoesNotExist()
                    assertEquals(map.document.design, SavedCustomMapStore(context).load().first { it.id == "community_$code" }.design)
                } finally {
                    // Tear down before restoring persistence to avoid lifecycle writes racing restoration.
                    compose.runOnIdle { mounted.value = false }
                    compose.waitForIdle()
                    priorRun?.let { runStore.save(it) } ?: runStore.clear()
                    GridLayoutPreferenceStore.save(context, priorGrid)
                    AuthPromptStore.setSavePromptDismissed(context, priorPrompt)
                }
                check(prefs.edit().putBoolean("accountBCompleted", true).commit())
            }
            "cleanup" -> {
                check(owner == prefs.getString("owner", null)) { "Original Account A required" }
                val code = checkNotNull(prefs.getString("code", null))
                val fetched = runCatching { client.fetch(code) }
                if (fetched.isSuccess) {
                    val map = fetched.getOrThrow()
                    assertEquals(prefs.getString("document", null), CommunityMapFormat.export(map.document.name, map.document.description, map.document.design))
                    client.unpublish(code, token)
                } else {
                    assertEquals(404, (fetched.exceptionOrNull() as CommunityApiException).status)
                }
                expectStatus(404) { client.fetch(code) }
                expectStatus(404) { client.importMap(code, UUID.randomUUID().toString(), token) }
                // Retain only this run's local import to verify offline persistence; no unrelated map deletion.
                check(prefs.edit().putBoolean("cleaned", true).commit())
            }
        }
        Unit
    }
}

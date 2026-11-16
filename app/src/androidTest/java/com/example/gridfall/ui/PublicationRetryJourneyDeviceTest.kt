package com.example.gridfall.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.example.gridfall.game.*
import com.example.gridfall.network.*
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.IOException
import java.util.UUID

class PublicationRetryJourneyDeviceTest {
    @Test fun lostSuccessfulResponseAndClientRecreationDoNotDuplicatePublication(): Unit = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("communityTestServer") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val phase = InstrumentationRegistry.getArguments().getString("publicationRestartPhase")
        val metadata = context.getSharedPreferences("publication_restart_test", 0)
        val name = if (phase == "recover") requireNotNull(metadata.getString("name", null))
            else "Retry${UUID.randomUUID().toString().take(8)}"
        if (phase == "prepare") check(metadata.edit().putString("name", name).commit())
        val document = CommunityMapFormat.export(name, "Original pending description", CustomMapDesign(4, 4, emptySet()))
        val endpoint: (String) -> String = { "http://127.0.0.1:55440$it" }
        val lossy = OkHttpClient.Builder().addInterceptor { chain ->
            val response = chain.proceed(chain.request())
            check(response.isSuccessful)
            response.close()
            throw IOException("Simulated loss after server committed")
        }.build()
        val first = CommunityMapsClient(endpoint = endpoint, journal = PublicationJournal(context), identityOfToken = { it }, http = lossy)
        if (phase != "recover") {
            assertTrue(runCatching { first.publishSavedMap("device-owner", name, document, "public", UUID.randomUUID().toString()) }.isFailure)
        }
        if (phase == "prepare") return@runBlocking
        val recreated = CommunityMapsClient(endpoint = endpoint, journal = PublicationJournal(context), identityOfToken = { it })
        val pending = requireNotNull(recreated.pendingPublication("device-owner", name))
        assertEquals("public", pending.visibility)
        assertEquals(document, pending.document)
        assertNull(recreated.pendingPublication("device-player", name))
        // Reopened dialog defaults or locally edited content must not replace the uncertain operation.
        val recovered = recreated.publishSavedMap("device-owner", name, CommunityMapFormat.export("Changed", design = CustomMapDesign(5, 5, emptySet())), "unlisted", UUID.randomUUID().toString())
        assertEquals(name, recovered.document.name)
        assertEquals("public", recovered.visibility)
        assertEquals("Original pending description", recovered.document.description)
        assertEquals(4, recovered.document.design.rows)
        val matches = recreated.browse(name, "newest", null, "device-owner").items
        assertEquals(1, matches.size)
        assertEquals(matches.single().code, recovered.code)
        recreated.unpublish(recovered.code, "device-owner")
        recreated.finishPublication(recovered.code)
        assertNull(recreated.pendingPublication("device-owner", name))
        if (phase == "recover") check(metadata.edit().clear().commit())
    }
}

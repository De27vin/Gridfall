package com.example.gridfall.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.example.gridfall.network.PublicationJournal
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID
import com.example.gridfall.network.CommunityMapsClient
import com.example.gridfall.network.CommunityApiException
import com.example.gridfall.game.CommunityMapFormat
import com.example.gridfall.game.CustomMapDesign
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class PublicationJournalDeviceTest {
    @Test fun operationSurvivesRecreationAndUncertainOutcomeWithoutCrossAccountReplay() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val owner = UUID.randomUUID().toString()
        val first = PublicationJournal(context).requestId(owner, "canonical-document", "unlisted")
        // Reconstruct all in-memory state, as after process death before receiving a response.
        val recreated = PublicationJournal(context)
        assertEquals(first, recreated.requestId(owner, "canonical-document", "unlisted"))
        assertNotEquals(first, recreated.requestId("other-$owner", "canonical-document", "unlisted"))
        recreated.received(owner, "canonical-document", "unlisted", "test-$owner")
        assertEquals(first, PublicationJournal(context).requestId(owner, "canonical-document", "unlisted"))
        PublicationJournal(context).finish("test-$owner")
        assertNotEquals(first, PublicationJournal(context).requestId(owner, "canonical-document", "unlisted"))
    }
    @Test fun definitiveModerationRejectionReleasesDraftButUncertainFailuresDoNot(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val journal = PublicationJournal(context)
        val owner = UUID.randomUUID().toString()
        val mapId = UUID.randomUUID().toString()
        val document = CommunityMapFormat.export("Draft", design = CustomMapDesign(4, 4, emptySet()))
        var status = 422
        val http = OkHttpClient.Builder().addInterceptor { chain -> Response.Builder()
            .request(chain.request()).protocol(Protocol.HTTP_1_1).code(status).message("Test failure")
            .body("{}".toResponseBody()).build() }.build()
        val client = CommunityMapsClient(endpoint = { "http://127.0.0.1:1$it" }, journal = journal,
            identityOfToken = { it }, http = http)
        val rejected = runCatching { client.publishSavedMap(owner, mapId, document, "public", UUID.randomUUID().toString()) }.exceptionOrNull()
        assertEquals(422, (rejected as CommunityApiException).status)
        assertNull(PublicationJournal(context).pending(owner, mapId))
        status = 503
        val uncertain = runCatching { client.publishSavedMap(owner, mapId, document, "public", UUID.randomUUID().toString()) }.exceptionOrNull()
        assertEquals(503, (uncertain as CommunityApiException).status)
        assertNotNull(PublicationJournal(context).pending(owner, mapId))
        journal.rejectDraft(owner, mapId)
    }

}

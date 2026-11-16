package com.example.gridfall.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.example.gridfall.network.PublicationJournal
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

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
}

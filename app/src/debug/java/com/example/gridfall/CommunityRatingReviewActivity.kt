package com.example.gridfall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gridfall.game.*
import com.example.gridfall.network.*
import com.example.gridfall.ui.*
import com.example.gridfall.ui.theme.GridfallTheme
import kotlinx.coroutines.delay
import java.io.IOException

/** Debug-only interactive fixture. No HTTP client, Firebase login or production mutations. */
class CommunityRatingReviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferences = getSharedPreferences("local_star_review", MODE_PRIVATE)
        val fixture = CommunityPublication("GF-A7K9P23456", "Local review fixture",
            CommunityMapDocument("LOCAL REVIEW · Star rating", "Local fixture only. Close and reopen to verify your saved stars.",
                CustomMapDesign(7, 7, setOf(Cell(0,0), Cell(6,6)), listOf(
                    MapBlockDefinition("single", "Single", setOf(Cell(0,0)), 600, 2),
                    MapBlockDefinition("corner", "Corner", setOf(Cell(0,0),Cell(1,0),Cell(1,1)), 400, 1)))), true, "public", 12, 4.2, 8)
        var failNext = false
        val gateway = object : CommunityMapsGateway {
            override suspend fun fetch(code: String) = fixture.copy(yourRating = preferences.getInt("rating", 0).takeIf { it in 1..5 })
            override suspend fun browse(query: String, sort: String, cursor: String?, ownerToken: String?) = CommunityPage(listOf(fixture), null)
            override suspend fun publish(token: String, document: String, visibility: String, requestId: String): CommunityPublication = error("Not part of this review")
            override suspend fun importMap(code: String, requestId: String, token: String?) = fixture
            override suspend fun rate(code: String, stars: Int, token: String): CommunityPublication {
                delay(400)
                if (failNext) { failNext = false; throw IOException("Local test: rating request failed. Your saved rating was restored.") }
                preferences.edit().putInt("rating", stars).apply()
                return fixture.copy(yourRating = stars)
            }
            override suspend fun unpublish(code: String, token: String) = Unit
            override suspend fun report(code: String, reason: String, notes: String, token: String) = Unit
        }
        setContent { GridfallTheme { CommunityMapsTheme {
            var opened by remember { mutableStateOf(true) }
            var generation by remember { mutableIntStateOf(0) }
            var message by remember { mutableStateOf("No network or production accounts are used.") }
            Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("LOCAL REVIEW — Five-star rating", style = MaterialTheme.typography.titleLarge)
                    Text(message)
                    Button(onClick = { generation++; opened = true }) { Text("Reopen map") }
                    OutlinedButton(onClick = { failNext = true; message = "The next rating will fail once. Reopen the map and tap a star." }) { Text("Reject next rating") }
                    OutlinedButton(onClick = { preferences.edit().remove("rating").apply(); generation++; opened = true }) { Text("Reset to unrated") }
                }
            }
            if (opened) key(generation) { CommunityMapDialog(null, gateway, true, { "local-fixture-only" },
                { _, _ -> opened = false; message = "Import actions are outside this rating-only review. Use the Gridfall launcher to play." }, { opened = false }, initialCode = fixture.code) }
        } } }
    }
}

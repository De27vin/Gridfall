package com.example.gridfall.ui

import android.content.Context
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.example.gridfall.audio.GridfallSoundManager
import com.example.gridfall.audio.SoundPreferenceStore
import com.example.gridfall.audio.ThemeSoundEvent
import com.example.gridfall.auth.AuthPromptStore
import com.example.gridfall.auth.GridfallAuthManager
import com.example.gridfall.auth.GridfallAuthSession
import com.example.gridfall.game.Board
import com.example.gridfall.game.ClearResult
import com.example.gridfall.game.GameEngine
import com.example.gridfall.game.GameState
import com.example.gridfall.game.JokerType
import com.example.gridfall.game.LevelSystem
import com.example.gridfall.game.Piece
import com.example.gridfall.game.PieceEffect
import com.example.gridfall.game.RiskSpinMemorySession
import com.example.gridfall.game.ScoreSystem
import com.example.gridfall.network.AccountConnectionState
import com.example.gridfall.network.GridfallApiClient
import com.example.gridfall.network.dto.MeResponse
import com.example.gridfall.network.dto.RunSubmissionRequest
import com.example.gridfall.sync.PendingRunSubmission
import com.example.gridfall.sync.PendingRunSubmissionStore
import com.example.gridfall.sync.RunSubmissionPolicy
import com.example.gridfall.sync.RunSubmissionRegistry
import com.example.gridfall.sync.RunSyncState
import com.example.gridfall.sync.RunSyncStatus
import com.example.gridfall.ui.auth.AuthDialog
import com.example.gridfall.ui.auth.AuthDialogMode
import com.example.gridfall.ui.auth.SaveProgressPrompt
import com.example.gridfall.ui.leaderboard.LeaderboardDialog
import com.example.gridfall.ui.leaderboard.LeaderboardUiState
import com.example.gridfall.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun GameScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val view = LocalView.current
    val lifecycleOwner = view.findViewTreeLifecycleOwner()
    var gameState by remember { mutableStateOf(GameEngine.createInitialState()) }
    var dragState by remember { mutableStateOf(DragState()) }
    var boardLayoutInfo by remember { mutableStateOf<BoardLayoutInfo?>(null) }
    var highScore by remember { mutableStateOf(HighScoreStore.load(context)) }
    var isNewBestThisGame by remember { mutableStateOf(false) }
    var lineClearFeedback by remember { mutableStateOf<LineClearFeedback?>(null) }
    var lineClearFeedbackToken by remember { mutableStateOf(0) }
    var bombPulseFeedback by remember { mutableStateOf<BombPulseFeedback?>(null) }
    var bombPulseFeedbackToken by remember { mutableStateOf(0) }
    var scoreEventFeedback by remember { mutableStateOf<ScoreEventFeedback?>(null) }
    var scoreEventFeedbackToken by remember { mutableStateOf(0) }
    var showRestartConfirmDialog by remember { mutableStateOf(false) }
    var showRiskSpinOverlay by remember { mutableStateOf(false) }
    var showRiskSpinOptions by remember { mutableStateOf(false) }
    var riskSpinMemorySession by remember { mutableStateOf<RiskSpinMemorySession?>(null) }
    var riskSpinPaidCost by remember { mutableStateOf<Int?>(null) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var selectedThemeMode by remember { mutableStateOf(ThemePreferenceStore.load(context)) }
    var soundEffectsVolume by remember { mutableStateOf(SoundPreferenceStore.loadSoundEffectsVolume(context)) }
    var backgroundMusicVolume by remember { mutableStateOf(SoundPreferenceStore.loadBackgroundMusicVolume(context)) }
    var isAppInForeground by remember { mutableStateOf(true) }
    var accountConnectionState by remember { mutableStateOf(AccountConnectionState()) }
    var savePromptDismissed by remember { mutableStateOf(AuthPromptStore.isSavePromptDismissed(context)) }
    var showSaveProgressPrompt by remember { mutableStateOf(false) }
    var authDialogMode by remember { mutableStateOf<AuthDialogMode?>(null) }
    var isAuthActionLoading by remember { mutableStateOf(false) }
    var authUiError by remember { mutableStateOf<String?>(null) }
    var authUiMessage by remember { mutableStateOf<String?>(null) }
    var runSyncState by remember { mutableStateOf(RunSyncState()) }
    var runSubmissionRegistry by remember { mutableStateOf(RunSubmissionRegistry()) }
    var showLeaderboardDialog by remember { mutableStateOf(false) }
    var leaderboardUiState by remember { mutableStateOf(LeaderboardUiState()) }
    val soundManager = remember { GridfallSoundManager(context) }
    val authManager = remember { GridfallAuthManager() }
    val apiClient = remember { GridfallApiClient() }
    val pendingRunStore = remember { PendingRunSubmissionStore(context) }
    val coroutineScope = rememberCoroutineScope()
    val activeThemeColors = colorsForThemeMode(selectedThemeMode)
    val currentLevel = LevelSystem.levelForScore(gameState.score)
    val nextLevelScore = LevelSystem.nextLevelScore(currentLevel)
    val density = LocalDensity.current
    
    // Updated drag offset for better UX (above finger)
    val verticalShiftPx = with(density) { -64.dp.toPx() }
    val dragVisualOffset = boardLayoutInfo?.let { layoutInfo ->
        dragState.piece?.let { piece ->
            calculateDragVisualOffset(
                piece = piece,
                boardLayoutInfo = layoutInfo
            ).let { offset ->
                Offset(offset.x, offset.y + verticalShiftPx)
            }
        } ?: Offset(0f, verticalShiftPx)
    } ?: Offset(0f, verticalShiftPx)

    val placementPreview = createPlacementPreview(
        dragState = dragState,
        placementResolution = dragState.placementResolution
    )

    fun markSavePromptDismissed() {
        savePromptDismissed = true
        showSaveProgressPrompt = false
        AuthPromptStore.setSavePromptDismissed(context)
    }

    fun offerSavePromptIfNeeded() {
        if (!savePromptDismissed && accountConnectionState.isAnonymous) {
            showSaveProgressPrompt = true
        }
    }

    fun openAuthDialog(mode: AuthDialogMode) {
        authDialogMode = mode
        authUiError = null
        authUiMessage = null
        showSaveProgressPrompt = false
    }

    fun syncHighScoreFromBackend(backendUser: MeResponse) {
        val mergedHighScore = HighScoreStore.mergedBestScore(
            localBestScore = highScore,
            backendBestScore = backendUser.profile.bestScore
        )
        if (mergedHighScore != highScore) {
            highScore = mergedHighScore
            HighScoreStore.save(context, mergedHighScore)
        }
    }

    suspend fun retryPendingRuns(firebaseIdToken: String): Int {
        var syncedCount = 0
        pendingRunStore.load().forEach { pendingSubmission ->
            try {
                apiClient.submitRun(firebaseIdToken, pendingSubmission.request)
                pendingRunStore.remove(pendingSubmission.runId)
                syncedCount += 1
            } catch (error: Exception) {
                Log.w(ACCOUNT_LOG_TAG, "Pending run retry failed: ${error.message}")
                return syncedCount
            }
        }
        return syncedCount
    }

    suspend fun refreshAccountState(
        authSession: GridfallAuthSession,
        successMessage: String? = null
    ) {
        accountConnectionState = AccountConnectionState(
            isLoading = false,
            firebaseUid = authSession.firebaseUid,
            isAnonymous = authSession.isAnonymous
        )

        try {
            val backendUser = apiClient.getMe(authSession.idToken)
            Log.i(
                ACCOUNT_LOG_TAG,
                "Backend /me connected user=${backendUser.id.take(8)}..."
            )
            accountConnectionState = AccountConnectionState(
                isLoading = false,
                firebaseUid = authSession.firebaseUid,
                isAnonymous = authSession.isAnonymous,
                backendUser = backendUser
            )
            syncHighScoreFromBackend(backendUser)
            val retriedCount = retryPendingRuns(authSession.idToken)
            if (retriedCount > 0) {
                runSyncState = RunSyncState(
                    status = RunSyncStatus.Synced,
                    message = "Pending runs synced"
                )
                runCatching { apiClient.getMe(authSession.idToken) }.getOrNull()?.let { refreshedUser ->
                    accountConnectionState = accountConnectionState.copy(
                        backendUser = refreshedUser,
                        isAnonymous = refreshedUser.isAnonymous,
                        backendError = null
                    )
                    syncHighScoreFromBackend(refreshedUser)
                }
            }
            authUiMessage = successMessage
        } catch (error: Exception) {
            accountConnectionState = AccountConnectionState(
                isLoading = false,
                firebaseUid = authSession.firebaseUid,
                isAnonymous = authSession.isAnonymous,
                backendError = error.message ?: "Backend unavailable"
            )
            authUiError = error.message ?: "Backend unavailable"
            Log.w(ACCOUNT_LOG_TAG, "Backend /me unavailable: ${error.message}")
        }
    }

    fun registerAccount(
        email: String,
        password: String,
        username: String
    ) {
        coroutineScope.launch {
            isAuthActionLoading = true
            authUiError = null
            authUiMessage = null
            try {
                val authSession = authManager.registerWithEmailPasswordAndLinkAnonymous(email, password)
                val token = authManager.getFreshIdToken()
                val backendUser = apiClient.getMe(token)
                accountConnectionState = AccountConnectionState(
                    isLoading = false,
                    firebaseUid = authSession.firebaseUid,
                    isAnonymous = authSession.isAnonymous,
                    backendUser = backendUser
                )
                syncHighScoreFromBackend(backendUser)

                val updatedUser = apiClient.setUsername(token, username)
                val retriedCount = retryPendingRuns(token)
                accountConnectionState = accountConnectionState.copy(
                    backendUser = updatedUser,
                    backendError = null
                )
                syncHighScoreFromBackend(updatedUser)
                if (retriedCount > 0) {
                    runSyncState = RunSyncState(
                        status = RunSyncStatus.Synced,
                        message = "Pending runs synced"
                    )
                }
                markSavePromptDismissed()
                authDialogMode = null
            } catch (error: Exception) {
                if (!accountConnectionState.isAnonymous) {
                    authDialogMode = AuthDialogMode.Username
                }
                authUiError = error.message ?: "Registration failed."
            } finally {
                isAuthActionLoading = false
            }
        }
    }

    fun loginAccount(
        email: String,
        password: String
    ) {
        coroutineScope.launch {
            isAuthActionLoading = true
            authUiError = null
            authUiMessage = null
            var mergeToken: String? = null

            try {
                if (accountConnectionState.isAnonymous) {
                    mergeToken = runCatching {
                        apiClient.createGuestMergeToken(authManager.getFreshIdToken())
                    }.getOrNull()
                }

                val authSession = authManager.loginWithEmailPassword(email, password)
                var token = authManager.getFreshIdToken()
                var backendUser = apiClient.getMe(token)

                if (mergeToken != null) {
                    val merged = runCatching {
                        apiClient.mergeGuest(token, mergeToken)
                    }.getOrDefault(false)
                    if (!merged) {
                        authUiMessage = "Logged in. Guest progress merge was unavailable."
                    }
                    token = authManager.getFreshIdToken()
                    backendUser = apiClient.getMe(token)
                }

                val retriedCount = retryPendingRuns(token)
                accountConnectionState = AccountConnectionState(
                    isLoading = false,
                    firebaseUid = authSession.firebaseUid,
                    isAnonymous = authSession.isAnonymous,
                    backendUser = backendUser
                )
                syncHighScoreFromBackend(backendUser)
                if (retriedCount > 0) {
                    runSyncState = RunSyncState(
                        status = RunSyncStatus.Synced,
                        message = "Pending runs synced"
                    )
                    backendUser = apiClient.getMe(token)
                    accountConnectionState = accountConnectionState.copy(backendUser = backendUser)
                    syncHighScoreFromBackend(backendUser)
                }
                markSavePromptDismissed()
                authDialogMode = if (backendUser.username == null) AuthDialogMode.Username else null
            } catch (error: Exception) {
                authUiError = error.message ?: "Login failed."
            } finally {
                isAuthActionLoading = false
            }
        }
    }

    fun saveUsername(username: String) {
        coroutineScope.launch {
            isAuthActionLoading = true
            authUiError = null
            authUiMessage = null
            try {
                val token = authManager.getFreshIdToken()
                val updatedUser = apiClient.setUsername(token, username)
                val retriedCount = retryPendingRuns(token)
                accountConnectionState = accountConnectionState.copy(
                    backendUser = updatedUser,
                    isAnonymous = updatedUser.isAnonymous,
                    backendError = null
                )
                syncHighScoreFromBackend(updatedUser)
                if (retriedCount > 0) {
                    runSyncState = RunSyncState(
                        status = RunSyncStatus.Synced,
                        message = "Pending runs synced"
                    )
                }
                markSavePromptDismissed()
                authDialogMode = null
            } catch (error: Exception) {
                authUiError = error.message ?: "Username could not be saved."
            } finally {
                isAuthActionLoading = false
            }
        }
    }

    fun logoutToGuest() {
        coroutineScope.launch {
            isAuthActionLoading = true
            authUiError = null
            try {
                val authSession = authManager.logoutToAnonymous()
                refreshAccountState(authSession)
            } catch (error: Exception) {
                authUiError = error.message ?: "Logout failed."
            } finally {
                isAuthActionLoading = false
            }
        }
    }

    fun runSubmissionRequest(endedState: GameState): RunSubmissionRequest {
        return RunSubmissionRequest(
            score = endedState.score,
            level = LevelSystem.levelForScore(endedState.score),
            linesCleared = endedState.runStats.linesCleared,
            contractsCompleted = endedState.runStats.contractsCompleted,
            bombsUsed = endedState.runStats.bombsUsed,
            megaBombsUsed = endedState.runStats.megaBombsUsed,
            durationSeconds = endedState.runStats.durationSeconds(),
            appVersion = appVersionName(context)
        )
    }

    fun submitEndedRunOnce(endedState: GameState) {
        val runId = endedState.runStats.runId
        val decision = runSubmissionRegistry.markSubmittedOnce(runId)
        runSubmissionRegistry = decision.registry
        if (!decision.shouldSubmit) return

        runSyncState = RunSyncState(
            runId = runId,
            status = RunSyncStatus.Submitting,
            message = "Submitting..."
        )

        coroutineScope.launch {
            val request = runSubmissionRequest(endedState)
            try {
                val token = authManager.getFreshIdToken()
                apiClient.submitRun(token, request)
                pendingRunStore.remove(runId)
                val retriedCount = retryPendingRuns(token)
                runSyncState = RunSyncState(
                    runId = runId,
                    status = RunSyncStatus.Synced,
                    message = when {
                        retriedCount > 0 -> "Pending runs synced"
                        accountConnectionState.isAnonymous -> "Guest run saved"
                        else -> "Run synced"
                    }
                )

                runCatching { apiClient.getMe(token) }.getOrNull()?.let { backendUser ->
                    accountConnectionState = accountConnectionState.copy(
                        isLoading = false,
                        firebaseUid = backendUser.firebaseUid,
                        isAnonymous = backendUser.isAnonymous,
                        backendUser = backendUser,
                        backendError = null
                    )
                    syncHighScoreFromBackend(backendUser)
                }
            } catch (error: Exception) {
                pendingRunStore.savePending(
                    PendingRunSubmission(
                        runId = runId,
                        request = request
                    )
                )
                runSyncState = RunSyncState(
                    runId = runId,
                    status = RunSyncStatus.Failed,
                    message = "Sync pending"
                )
                Log.w(ACCOUNT_LOG_TAG, "Run submission failed: ${error.message}")
            }
        }
    }

    fun loadLeaderboard() {
        leaderboardUiState = LeaderboardUiState(isLoading = true)
        coroutineScope.launch {
            leaderboardUiState = try {
                LeaderboardUiState(entries = apiClient.getLeaderboard(limit = 50).entries)
            } catch (error: Exception) {
                LeaderboardUiState(error = error.message ?: "Leaderboard unavailable.")
            }
        }
    }

    fun openLeaderboard() {
        showLeaderboardDialog = true
        loadLeaderboard()
    }

    LaunchedEffect(Unit) {
        accountConnectionState = AccountConnectionState(isLoading = true)

        try {
            val authSession = authManager.ensureAnonymousUser()
            Log.i(
                ACCOUNT_LOG_TAG,
                "Firebase anonymous auth ready uid=${authSession.firebaseUid.take(8)}..."
            )
            refreshAccountState(authSession)
        } catch (error: Exception) {
            accountConnectionState = AccountConnectionState(
                isLoading = false,
                authError = error.message ?: "Firebase Auth unavailable"
            )
            Log.w(ACCOUNT_LOG_TAG, "Firebase anonymous auth unavailable: ${error.message}")
        }
    }

    LaunchedEffect(gameState.isGameOver, gameState.runStats.runId) {
        if (RunSubmissionPolicy.shouldSubmitGameOverRun(gameState)) {
            submitEndedRunOnce(gameState)
        }
    }

    DisposableEffect(soundManager) {
        onDispose {
            soundManager.release()
        }
    }

    DisposableEffect(lifecycleOwner, soundManager) {
        val owner = lifecycleOwner
        if (owner == null) {
            onDispose { soundManager.stopBackgroundMusic() }
        } else {
            isAppInForeground = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> isAppInForeground = true
                    Lifecycle.Event.ON_PAUSE,
                    Lifecycle.Event.ON_STOP,
                    Lifecycle.Event.ON_DESTROY -> {
                        isAppInForeground = false
                        soundManager.stopBackgroundMusic()
                    }
                    else -> Unit
                }
            }
            owner.lifecycle.addObserver(observer)
            onDispose {
                owner.lifecycle.removeObserver(observer)
                soundManager.stopBackgroundMusic()
            }
        }
    }

    LaunchedEffect(selectedThemeMode, showSettingsScreen, backgroundMusicVolume, isAppInForeground) {
        if (isAppInForeground) {
            soundManager.playBackgroundMusic(
                themeMode = selectedThemeMode,
                isMenu = showSettingsScreen,
                musicVolume = backgroundMusicVolume
            )
        } else {
            soundManager.stopBackgroundMusic()
        }
    }

    LaunchedEffect(lineClearFeedback?.token) {
        if (lineClearFeedback != null) {
            delay(650)
            lineClearFeedback = null
        }
    }

    LaunchedEffect(scoreEventFeedback?.token) {
        if (scoreEventFeedback != null) {
            delay(900)
            scoreEventFeedback = null
        }
    }

    LaunchedEffect(bombPulseFeedback?.token) {
        if (bombPulseFeedback != null) {
            delay(560)
            bombPulseFeedback = null
        }
    }

    var showContractResult by remember { mutableStateOf(false) }
    LaunchedEffect(
        gameState.contractState.resolvedContract,
        gameState.contractState.isCompleted,
        gameState.contractState.isFailed
    ) {
        if (
            gameState.contractState.resolvedContract != null &&
            (gameState.contractState.isCompleted || gameState.contractState.isFailed)
        ) {
            if (gameState.contractState.isCompleted) {
                soundManager.playContractSuccess(soundEffectsVolume)
            } else {
                soundManager.playContractFailed(soundEffectsVolume)
            }
            showContractResult = true
            delay(1200)
            showContractResult = false
        }
    }

    val contractOfferSoundKey = gameState.contractState.offeredContract?.let { contract ->
        "${gameState.contractState.completedBatchCount}:${contract.id}:${contract.rewardPoints}"
    }
    LaunchedEffect(contractOfferSoundKey) {
        if (contractOfferSoundKey != null) {
            soundManager.playContractPopup(soundEffectsVolume)
        }
    }

    fun restartGame() {
        showRestartConfirmDialog = false
        showRiskSpinOverlay = false
        showRiskSpinOptions = false
        riskSpinMemorySession = null
        riskSpinPaidCost = null
        showSettingsScreen = false
        gameState = GameEngine.createInitialState()
        dragState = DragState()
        lineClearFeedback = null
        bombPulseFeedback = null
        scoreEventFeedback = null
        isNewBestThisGame = false
    }

    fun finishDrag() {
        val pieceIndex = dragState.pieceIndex
        val jokerType = dragState.jokerType
        val piece = dragState.piece
        val resolution = dragState.placementResolution
        val target = resolution?.target

        if (piece != null && target != null && resolution.isValid && (pieceIndex != null || jokerType != null)) {
            val clearResult = previewClearResult(
                board = gameState.board,
                piece = piece,
                startRow = target.startRow,
                startCol = target.startCol
            )
            val nextState = if (jokerType != null) {
                GameEngine.placeJoker(
                    state = gameState,
                    jokerType = jokerType,
                    startRow = target.startRow,
                    startCol = target.startCol
                )
            } else {
                GameEngine.placePiece(
                    state = gameState,
                    pieceIndex = pieceIndex ?: return,
                    startRow = target.startRow,
                    startCol = target.startCol
                )
            }

            if (nextState != gameState) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                val nextScoreEvent = createScoreEventFeedback(
                    board = gameState.board,
                    piece = piece,
                    startRow = target.startRow,
                    startCol = target.startCol,
                    clearResult = clearResult,
                    nextState = nextState
                )
                gameState = nextState

                if (nextState.score > highScore) {
                    highScore = nextState.score
                    HighScoreStore.save(context, nextState.score)
                    isNewBestThisGame = true
                }

                if (clearResult != null && clearResult.clearedLineCount > 0) {
                    lineClearFeedbackToken += 1
                    lineClearFeedback = LineClearFeedback(
                        clearedRows = clearResult.clearedRows,
                        clearedColumns = clearResult.clearedColumns,
                        token = lineClearFeedbackToken
                    )
                }

                createBombPulseFeedback(
                    piece = piece,
                    startRow = target.startRow,
                    startCol = target.startCol
                )?.let { feedback ->
                    bombPulseFeedbackToken += 1
                    bombPulseFeedback = feedback.copy(token = bombPulseFeedbackToken)
                }

                if (nextScoreEvent != null) {
                    scoreEventFeedbackToken += 1
                    scoreEventFeedback = nextScoreEvent.copy(token = scoreEventFeedbackToken)
                }

                playMoveSound(
                    soundManager = soundManager,
                    themeMode = selectedThemeMode,
                    effectsVolume = soundEffectsVolume,
                    piece = piece,
                    clearedLineCount = clearResult?.clearedLineCount ?: 0
                )

                if (nextState.isGameOver) {
                    soundManager.playThemeEvent(
                        themeMode = selectedThemeMode,
                        event = ThemeSoundEvent.GameOver,
                        effectsVolume = soundEffectsVolume
                    )
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        } else if (pieceIndex != null || jokerType != null) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }

        dragState = DragState()
    }

    CompositionLocalProvider(LocalGridfallColors provides activeThemeColors) {
        if (showSettingsScreen) {
            SettingsScreen(
                selectedThemeMode = selectedThemeMode,
                soundEffectsVolume = soundEffectsVolume,
                backgroundMusicVolume = backgroundMusicVolume,
                onThemeSelected = { theme ->
                    selectedThemeMode = theme
                    ThemePreferenceStore.save(context, theme)
                },
                onSoundEffectsVolumeChange = { volume ->
                    soundEffectsVolume = volume
                    SoundPreferenceStore.saveSoundEffectsVolume(context, volume)
                },
                onBackgroundMusicVolumeChange = { volume ->
                    backgroundMusicVolume = volume
                    SoundPreferenceStore.saveBackgroundMusicVolume(context, volume)
                },
                accountConnectionState = accountConnectionState,
                runSyncMessage = runSyncState.message,
                onRegisterClick = {
                    openAuthDialog(AuthDialogMode.Register)
                },
                onLoginClick = {
                    openAuthDialog(AuthDialogMode.Login)
                },
                onChooseUsernameClick = {
                    openAuthDialog(AuthDialogMode.Username)
                },
                onLeaderboardClick = {
                    openLeaderboard()
                },
                onLogoutClick = {
                    logoutToGuest()
                },
                onReturnToGame = {
                    showSettingsScreen = false
                },
                modifier = modifier
            )
        } else {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(activeThemeColors.backgroundTop, activeThemeColors.backgroundBottom)
                        )
                    )
                    .infernoAppTexture(activeThemeColors)
                    .retroAppTexture(activeThemeColors)
            ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            GameTopBar(
                score = gameState.score,
                highScore = highScore,
                level = currentLevel,
                nextLevelScore = nextLevelScore,
                combo = gameState.combo,
                onSettingsClick = {
                    showRestartConfirmDialog = false
                    showSettingsScreen = true
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            BoardCanvas(
                board = gameState.board,
                placementPreview = placementPreview,
                lineClearFeedback = lineClearFeedback,
                bombPulseFeedback = bombPulseFeedback,
                contractWarningCells = contractWarningCells(gameState.contractState),
                onBoardLayoutChanged = { layoutInfo ->
                    boardLayoutInfo = layoutInfo
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            PieceTray(
                pieces = gameState.currentPieces,
                usedPieceIndices = gameState.usedPieceIndices,
                draggingPieceIndex = dragState.pieceIndex,
                onPieceDragStarted = { pieceIndex, piece, position, startOffset ->
                    val resolution = calculateCurrentDragPlacementResolution(
                        piece = piece,
                        dragPosition = position,
                        boardLayoutInfo = boardLayoutInfo,
                        board = gameState.board,
                        verticalShiftPx = verticalShiftPx
                    )
                    dragState = DragState(
                        pieceIndex = pieceIndex,
                        piece = piece,
                        dragPosition = position,
                        dragStartOffset = startOffset,
                        placementResolution = resolution,
                        isDragging = true
                    )
                },
                onPieceDragged = { dragAmount ->
                    val piece = dragState.piece
                    val nextPosition = dragState.dragPosition + dragAmount
                    val resolution = if (piece != null) {
                        calculateCurrentDragPlacementResolution(
                            piece = piece,
                            dragPosition = nextPosition,
                            boardLayoutInfo = boardLayoutInfo,
                            board = gameState.board,
                            verticalShiftPx = verticalShiftPx
                        )
                    } else {
                        null
                    }

                    dragState = dragState.copy(
                        dragPosition = nextPosition,
                        placementResolution = resolution
                    )
                },
                onPieceDragEnded = ::finishDrag,
                onPieceDragCancelled = {
                    dragState = DragState()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            val restartShape = RoundedCornerShape(retroCorner(activeThemeColors, infernoCorner(activeThemeColors, 16.dp)))
            val riskSpinAvailability = GameEngine.riskSpinAvailability(gameState)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        showRestartConfirmDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeThemeColors.button,
                        contentColor = activeThemeColors.textPrimary
                    ),
                    shape = restartShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .infernoPanelTexture(activeThemeColors)
                        .retroPanelTexture(activeThemeColors)
                        .border(
                            width = if (activeThemeColors.isRetroTheme() || activeThemeColors.isInfernoTheme()) 1.dp else 0.dp,
                            color = activeThemeColors.panelBorder.copy(alpha = if (activeThemeColors.isRetroTheme() || activeThemeColors.isInfernoTheme()) 0.82f else 0f),
                            shape = restartShape
                        )
                ) {
                    Text(
                        text = if (activeThemeColors.isRetroTheme()) "RESTART" else "Restart",
                        style = MaterialTheme.typography.labelLarge.retroText(activeThemeColors)
                    )
                }

                Button(
                    onClick = {
                        showRiskSpinOverlay = true
                        showRiskSpinOptions = true
                        riskSpinMemorySession = null
                        riskSpinPaidCost = null
                    },
                    enabled = riskSpinAvailability.isAvailable,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeThemeColors.button,
                        contentColor = activeThemeColors.textPrimary,
                        disabledContainerColor = activeThemeColors.chipBackground.copy(alpha = 0.48f),
                        disabledContentColor = activeThemeColors.textMuted
                    ),
                    shape = restartShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .infernoPanelTexture(activeThemeColors)
                        .retroPanelTexture(activeThemeColors)
                        .border(
                            width = if (activeThemeColors.isRetroTheme() || activeThemeColors.isInfernoTheme()) 1.dp else 0.dp,
                            color = activeThemeColors.accentStrong.copy(
                                alpha = if (riskSpinAvailability.isAvailable) {
                                    if (activeThemeColors.isRetroTheme() || activeThemeColors.isInfernoTheme()) 0.82f else 0f
                                } else {
                                    0.20f
                                }
                            ),
                            shape = restartShape
                        )
                ) {
                    Text(
                        text = if (activeThemeColors.isRetroTheme()) "RISK SPIN" else "Risk Spin",
                        style = MaterialTheme.typography.labelLarge.retroText(activeThemeColors)
                    )
                }
            }

            if (!riskSpinAvailability.isAvailable && riskSpinAvailability.reason != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = riskSpinAvailability.reason,
                    color = activeThemeColors.textMuted,
                    style = MaterialTheme.typography.labelSmall.retroText(activeThemeColors)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            JokerInventoryCarousel(
                inventory = gameState.riskSpinState.inventory,
                draggingJokerInventoryIndex = dragState.jokerInventoryIndex,
                canUseRevert = gameState.riskSpinState.previousMoveSnapshot != null &&
                    JokerType.Revert in gameState.riskSpinState.inventory,
                onJokerDragStarted = { inventoryIndex, selectedJokerType, jokerPiece, position, startOffset ->
                    val resolution = calculateCurrentDragPlacementResolution(
                        piece = jokerPiece,
                        dragPosition = position,
                        boardLayoutInfo = boardLayoutInfo,
                        board = gameState.board,
                        verticalShiftPx = verticalShiftPx
                    )
                    dragState = DragState(
                        jokerInventoryIndex = inventoryIndex,
                        jokerType = selectedJokerType,
                        piece = jokerPiece,
                        dragPosition = position,
                        dragStartOffset = startOffset,
                        placementResolution = resolution,
                        isDragging = true
                    )
                },
                onJokerDragged = { dragAmount ->
                    val piece = dragState.piece
                    val nextPosition = dragState.dragPosition + dragAmount
                    val resolution = if (piece != null) {
                        calculateCurrentDragPlacementResolution(
                            piece = piece,
                            dragPosition = nextPosition,
                            boardLayoutInfo = boardLayoutInfo,
                            board = gameState.board,
                            verticalShiftPx = verticalShiftPx
                        )
                    } else {
                        null
                    }

                    dragState = dragState.copy(
                        dragPosition = nextPosition,
                        placementResolution = resolution
                    )
                },
                onJokerDragEnded = ::finishDrag,
                onJokerDragCancelled = {
                    dragState = DragState()
                },
                onRevertClicked = {
                    val nextState = GameEngine.useRevertJoker(gameState)
                    if (nextState != gameState) {
                        gameState = nextState
                        dragState = DragState()
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        val draggedPiece = dragState.piece
        val currentBoardLayoutInfo = boardLayoutInfo
        if (dragState.isDragging && draggedPiece != null && currentBoardLayoutInfo != null) {
            val draggedPiecePosition = dragState.dragPosition + dragVisualOffset
            DraggedPiece(
                piece = draggedPiece,
                cellSizePx = currentBoardLayoutInfo.cellSizePx,
                spacingPx = currentBoardLayoutInfo.spacingPx,
                modifier = Modifier.offset {
                    IntOffset(
                        x = draggedPiecePosition.x.roundToInt(),
                        y = draggedPiecePosition.y.roundToInt()
                    )
                }
            )
        }

        if (showContractResult && gameState.contractState.resolvedContract != null) {
            ContractResultChip(
                contractState = gameState.contractState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .systemBarsPadding()
                    .padding(top = 112.dp)
            )
        } else if (gameState.contractState.activeContract != null) {
            ContractActiveChip(
                contractState = gameState.contractState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .systemBarsPadding()
                    .padding(top = 112.dp, start = 20.dp, end = 20.dp)
            )
        }

        scoreEventFeedback?.let { feedback ->
            ScoreEventChip(
                feedback = feedback,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .systemBarsPadding()
                    .padding(top = 160.dp)
            )
        }

        val offeredContract = gameState.contractState.offeredContract
        if (offeredContract != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(activeThemeColors.backgroundTop.copy(alpha = 0.84f))
                    .infernoAppTexture(activeThemeColors)
                    .retroAppTexture(activeThemeColors)
                    .padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                ContractOfferPopup(
                    contract = offeredContract,
                    onAccept = {
                        gameState = GameEngine.acceptContract(gameState)
                    },
                    onSkip = {
                        gameState = GameEngine.skipContract(gameState)
                    }
                )
            }
        }

        if (showRiskSpinOverlay) {
            RiskSpinMemoryOverlay(
                session = riskSpinMemorySession,
                paidCost = riskSpinPaidCost,
                inventoryCount = gameState.riskSpinState.inventory.size,
                isGridActive = !showRiskSpinOptions,
                onFieldClick = { fieldId ->
                    val session = riskSpinMemorySession ?: return@RiskSpinMemoryOverlay
                    val result = GameEngine.revealRiskSpinMemoryField(
                        state = gameState,
                        session = session,
                        fieldId = fieldId
                    )
                    if (result != null) {
                        gameState = result.state
                        riskSpinMemorySession = result.session
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                },
                onClose = {
                    showRiskSpinOverlay = false
                    showRiskSpinOptions = false
                    riskSpinMemorySession = null
                    riskSpinPaidCost = null
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }

        if (gameState.isGameOver) {
            LaunchedEffect(gameState.isGameOver, savePromptDismissed, accountConnectionState.isAnonymous) {
                offerSavePromptIfNeeded()
            }
            GameOverDialog(
                finalScore = gameState.score,
                highScore = highScore,
                isNewBest = isNewBestThisGame,
                runSyncMessage = runSyncState.message,
                onLeaderboard = {
                    openLeaderboard()
                },
                onRestart = ::restartGame
        )
    } else if (showRiskSpinOverlay && showRiskSpinOptions) {
        RiskSpinDialog(
            gameState = gameState,
            onOptionSelected = { option ->
                val result = GameEngine.startRiskSpinMemorySession(gameState, option)
                if (result != null) {
                    gameState = result.state
                    riskSpinMemorySession = result.session
                    riskSpinPaidCost = result.cost
                    showRiskSpinOptions = false
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                }
            },
            onDismiss = {
                showRiskSpinOverlay = false
                showRiskSpinOptions = false
                riskSpinMemorySession = null
                riskSpinPaidCost = null
            }
        )
        } else if (showRestartConfirmDialog) {
            RestartConfirmDialog(
                onCancel = {
                    showRestartConfirmDialog = false
                },
                onConfirmRestart = {
                    if (RunSubmissionPolicy.shouldSubmitConfirmedRestartRun(gameState)) {
                        submitEndedRunOnce(gameState)
                    }
                    restartGame()
                    offerSavePromptIfNeeded()
                }
            )
        }

        if (showSaveProgressPrompt) {
            SaveProgressPrompt(
                onRegister = {
                    markSavePromptDismissed()
                    openAuthDialog(AuthDialogMode.Register)
                },
                onLogin = {
                    markSavePromptDismissed()
                    openAuthDialog(AuthDialogMode.Login)
                },
                onSkip = {
                    markSavePromptDismissed()
                }
            )
        }

        if (showLeaderboardDialog) {
            LeaderboardDialog(
                state = leaderboardUiState,
                accountConnectionState = accountConnectionState,
                onRefresh = {
                    loadLeaderboard()
                },
                onDismiss = {
                    showLeaderboardDialog = false
                }
            )
        }

        authDialogMode?.let { mode ->
            AuthDialog(
                mode = mode,
                isLoading = isAuthActionLoading,
                message = authUiMessage,
                error = authUiError,
                initialUsername = accountConnectionState.backendUser?.username,
                initialEmail = accountConnectionState.backendUser?.email,
                onDismiss = {
                    if (!isAuthActionLoading) {
                        authDialogMode = null
                        authUiError = null
                        authUiMessage = null
                    }
                },
                onSwitchMode = { nextMode ->
                    authDialogMode = nextMode
                    authUiError = null
                    authUiMessage = null
                },
                onRegister = ::registerAccount,
                onLogin = ::loginAccount,
                onSaveUsername = ::saveUsername
            )
        }
        }
    }
}

private const val ACCOUNT_LOG_TAG = "GridfallAccount"

@Suppress("DEPRECATION")
private fun appVersionName(context: Context): String {
    return runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
    }.getOrDefault("1.0")
}

@Composable
private fun DraggedPiece(
    piece: Piece,
    cellSizePx: Float,
    spacingPx: Float,
    modifier: Modifier = Modifier
) {
    val bounds = piece.bounds()
    val widthPx = visualPieceWidth(
        piece = piece,
        cellSizePx = cellSizePx,
        spacingPx = spacingPx
    )
    val heightPx = visualPieceHeight(
        piece = piece,
        cellSizePx = cellSizePx,
        spacingPx = spacingPx
    )
    val density = LocalDensity.current
    val widthDp = with(density) { widthPx.toDp() }
    val heightDp = with(density) { heightPx.toDp() }
    val theme = LocalGridfallColors.current

    Canvas(
        modifier = modifier
            .size(widthDp, heightDp)
    ) {
        piece.cells.forEach { cell ->
            val row = cell.row - bounds.minRow
            val col = cell.col - bounds.minCol
            val topLeft = Offset(
                x = col * (cellSizePx + spacingPx),
                y = row * (cellSizePx + spacingPx)
            )

            drawDraggedCell(topLeft, cellSizePx, piece.colorVariant, theme)
        }
    }
}

private fun DrawScope.drawDraggedCell(
    topLeft: Offset,
    cellSize: Float,
    variant: Int,
    colors: com.example.gridfall.ui.theme.GridfallColors
) {
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.24f),
        topLeft = topLeft + Offset(cellSize * 0.08f, cellSize * 0.10f),
        size = Size(cellSize, cellSize),
        cornerRadius = if (colors.isInfernoTheme()) {
            CornerRadius(cellSize * 0.055f, cellSize * 0.055f)
        } else {
            CornerRadius(8.dp.toPx(), 8.dp.toPx())
        }
    )
    drawTacticalBlock(
        topLeft = topLeft,
        cellSize = cellSize,
        variant = variant,
        colors = colors
    )
}

private fun playMoveSound(
    soundManager: GridfallSoundManager,
    themeMode: GridfallThemeMode,
    effectsVolume: Float,
    piece: Piece,
    clearedLineCount: Int
) {
    val event = when {
        piece.effect == PieceEffect.Bomb || piece.effect == PieceEffect.MegaBomb -> ThemeSoundEvent.Bomb
        clearedLineCount >= 2 -> ThemeSoundEvent.MultiLineClear
        clearedLineCount == 1 -> ThemeSoundEvent.LineClear
        else -> ThemeSoundEvent.Place
    }

    soundManager.playThemeEvent(
        themeMode = themeMode,
        event = event,
        effectsVolume = effectsVolume
    )
}

private fun createPlacementPreview(
    dragState: DragState,
    placementResolution: DragPlacementResolution?
): PlacementPreview? {
    val piece = dragState.piece ?: return null
    if (!dragState.isDragging) return null
    val target = placementResolution?.target ?: return null

    return PlacementPreview(
        piece = piece,
        originRow = target.startRow,
        originCol = target.startCol,
        isValid = placementResolution.isValid
    )
}

private fun calculateCurrentDragPlacementResolution(
    piece: Piece,
    dragPosition: Offset,
    boardLayoutInfo: BoardLayoutInfo?,
    board: Board,
    verticalShiftPx: Float
): DragPlacementResolution? {
    val layoutInfo = boardLayoutInfo ?: return null
    
    val currentDragVisualOffset = calculateDragVisualOffset(
        piece = piece,
        boardLayoutInfo = layoutInfo
    )
    
    return calculateDragPlacementResolution(
        piece = piece,
        visualPieceTopLeft = dragPosition + currentDragVisualOffset + Offset(0f, verticalShiftPx), 
        boardLayoutInfo = layoutInfo,
        board = board
    )
}

private fun previewClearResult(
    board: Board,
    piece: Piece,
    startRow: Int,
    startCol: Int
): ClearResult? {
    if (!GameEngine.canPlace(board, piece, startRow, startCol)) return null
    if (piece.effect == PieceEffect.Bomb || piece.effect == PieceEffect.MegaBomb) return null

    val placedBoard = piece.cells.fold(board) { currentBoard, cell ->
        currentBoard.fill(
            row = startRow + cell.row,
            col = startCol + cell.col,
            value = piece.colorVariant
        )
    }

    return GameEngine.clearLines(placedBoard)
}

private fun createScoreEventFeedback(
    board: Board,
    piece: Piece,
    startRow: Int,
    startCol: Int,
    clearResult: ClearResult?,
    nextState: com.example.gridfall.game.GameState
): ScoreEventFeedback? {
    if (piece.effect == PieceEffect.Bomb || piece.effect == PieceEffect.MegaBomb) {
        val clearedCells = previewBombClearedCellCount(
            board = board,
            piece = piece,
            startRow = startRow,
            startCol = startCol
        )
        if (clearedCells > 0) {
            return ScoreEventFeedback(
                text = "Bomb Clear +${clearedCells * ScoreSystem.BOMB_CLEAR_POINTS_PER_CELL}",
                tone = FeedbackTone.Bomb,
                token = 0
            )
        }
    }

    val lineCount = clearResult?.clearedLineCount ?: 0
    if (lineCount > 0) {
        val feedbackParts = mutableListOf<String>()
        val multiLineBonus = ScoreSystem.calculateMultiLineBonus(lineCount)
        val comboBonus = ScoreSystem.calculateComboBonus(nextState.combo)

        if (nextState.board.isEmpty()) {
            feedbackParts += "Perfect Clear +${ScoreSystem.PERFECT_CLEAR_BONUS}"
        }

        if (multiLineBonus > 0 && clearResult != null) {
            val label = if (
                clearResult.clearedRows.isNotEmpty() &&
                clearResult.clearedColumns.isNotEmpty()
            ) {
                "Cross Clear"
            } else {
                "Multi Clear"
            }
            feedbackParts += "$label +$multiLineBonus"
        }

        if (comboBonus > 0) {
            feedbackParts += "Combo x${nextState.combo} +$comboBonus"
        }

        if (feedbackParts.isNotEmpty()) {
            return ScoreEventFeedback(
                text = feedbackParts.joinToString("  "),
                tone = if (nextState.board.isEmpty()) FeedbackTone.Success else FeedbackTone.Accent,
                token = 0
            )
        }
    }

    return null
}

private fun createBombPulseFeedback(
    piece: Piece,
    startRow: Int,
    startCol: Int
): BombPulseFeedback? {
    if (piece.effect != PieceEffect.Bomb && piece.effect != PieceEffect.MegaBomb) return null
    val bombCell = piece.cells.singleOrNull() ?: return null

    return BombPulseFeedback(
        centerRow = startRow + bombCell.row,
        centerCol = startCol + bombCell.col,
        isMega = piece.effect == PieceEffect.MegaBomb,
        token = 0
    )
}

private fun previewBombClearedCellCount(
    board: Board,
    piece: Piece,
    startRow: Int,
    startCol: Int
): Int {
    if (piece.effect != PieceEffect.Bomb && piece.effect != PieceEffect.MegaBomb) return 0
    val bombCell = piece.cells.singleOrNull() ?: return 0
    val centerRow = startRow + bombCell.row
    val centerCol = startCol + bombCell.col
    var clearedCells = 0

    val affectedCells = if (piece.effect == PieceEffect.MegaBomb) {
        GameEngine.megaBombAffectedCells(centerRow, centerCol)
    } else {
        (centerRow - 1..centerRow + 1).flatMap { row ->
            (centerCol - 1..centerCol + 1).map { col ->
                com.example.gridfall.game.Cell(row, col)
            }
        }
    }

    affectedCells.forEach { cell ->
        if (board.isInside(cell.row, cell.col) && board.get(cell.row, cell.col) != 0) {
            clearedCells += 1
        }
    }

    return clearedCells
}

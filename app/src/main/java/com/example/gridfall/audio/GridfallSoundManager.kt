package com.example.gridfall.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.example.gridfall.ui.theme.GridfallThemeMode
import kotlin.random.Random

class GridfallSoundManager(context: Context) {
    private val appContext = context.applicationContext
    private val loadedSoundIds = mutableSetOf<Int>()
    private var musicPlayer: MediaPlayer? = null
    private var currentMusicTrack: MusicTrack? = null
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
        .also { pool ->
            pool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) loadedSoundIds += sampleId
            }
        }

    private val sounds = mapOf(
        SoundKey(GridfallThemeMode.PremiumTactical, ThemeSoundEvent.Place) to loadSfx("premium_place.ogg"),
        SoundKey(GridfallThemeMode.PremiumTactical, ThemeSoundEvent.Bomb) to loadSfx("premium_bomb.ogg"),
        SoundKey(GridfallThemeMode.PremiumTactical, ThemeSoundEvent.LineClear) to loadSfx("premium_line_clear.ogg"),
        SoundKey(GridfallThemeMode.PremiumTactical, ThemeSoundEvent.MultiLineClear) to loadSfx("premium_multi_line_clear.ogg"),
        SoundKey(GridfallThemeMode.PremiumTactical, ThemeSoundEvent.GameOver) to loadSfx("premium_game_over.ogg"),
        SoundKey(GridfallThemeMode.InfernoCore, ThemeSoundEvent.Place) to loadSfx("inferno_place.ogg"),
        SoundKey(GridfallThemeMode.InfernoCore, ThemeSoundEvent.Bomb) to loadSfx("inferno_bomb.ogg"),
        SoundKey(GridfallThemeMode.InfernoCore, ThemeSoundEvent.LineClear) to loadSfx("inferno_line_clear.ogg"),
        SoundKey(GridfallThemeMode.InfernoCore, ThemeSoundEvent.MultiLineClear) to loadSfx("inferno_multi_line_clear.ogg"),
        SoundKey(GridfallThemeMode.InfernoCore, ThemeSoundEvent.GameOver) to loadSfx("inferno_game_over.ogg"),
        SoundKey(GridfallThemeMode.RetroArcade, ThemeSoundEvent.Place) to loadSfx("retro_place.ogg"),
        SoundKey(GridfallThemeMode.RetroArcade, ThemeSoundEvent.Bomb) to loadSfx("retro_bomb.ogg"),
        SoundKey(GridfallThemeMode.RetroArcade, ThemeSoundEvent.LineClear) to loadSfx("retro_line_clear.ogg"),
        SoundKey(GridfallThemeMode.RetroArcade, ThemeSoundEvent.MultiLineClear) to loadSfx("retro_multi_line_clear.ogg"),
        SoundKey(GridfallThemeMode.RetroArcade, ThemeSoundEvent.GameOver) to loadSfx("retro_game_over.ogg")
    )

    private val contractPopup = loadSfx("contract_popup.ogg")
    private val contractSuccess = loadSfx("contract_success.ogg")
    private val contractFailed = listOf(
        loadSfx("contract_failed_1.ogg"),
        loadSfx("contract_failed_2.ogg")
    )

    fun playBackgroundMusic(
        themeMode: GridfallThemeMode,
        isMenu: Boolean,
        soundEnabled: Boolean
    ) {
        if (!soundEnabled) {
            stopBackgroundMusic()
            return
        }

        val targetTrack = if (isMenu) {
            MusicTrack.Menu
        } else {
            MusicTrack.forTheme(themeMode)
        }

        val player = musicPlayer
        if (currentMusicTrack == targetTrack && player != null) {
            if (!player.isPlaying) {
                runCatching { player.start() }
            }
            return
        }

        stopBackgroundMusic()
        currentMusicTrack = targetTrack
        musicPlayer = createMusicPlayer(targetTrack)
    }

    fun playThemeEvent(
        themeMode: GridfallThemeMode,
        event: ThemeSoundEvent,
        soundEnabled: Boolean
    ) {
        if (!soundEnabled) return
        sounds[SoundKey(themeMode, event)]?.let(::play)
    }

    fun playContractPopup(soundEnabled: Boolean) {
        if (!soundEnabled) return
        play(contractPopup)
    }

    fun playContractSuccess(soundEnabled: Boolean) {
        if (!soundEnabled) return
        play(contractSuccess)
    }

    fun playContractFailed(soundEnabled: Boolean) {
        if (!soundEnabled) return
        play(contractFailed.random(Random.Default))
    }

    fun release() {
        stopBackgroundMusic()
        soundPool.release()
    }

    fun stopBackgroundMusic() {
        musicPlayer?.let { player ->
            runCatching {
                if (player.isPlaying) player.stop()
            }
            player.release()
        }
        musicPlayer = null
        currentMusicTrack = null
    }

    private fun createMusicPlayer(track: MusicTrack): MediaPlayer? {
        return runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                appContext.assets.openFd(track.assetPath).use { descriptor ->
                    setDataSource(
                        descriptor.fileDescriptor,
                        descriptor.startOffset,
                        descriptor.length
                    )
                }
                isLooping = true
                val volume = if (track == MusicTrack.Menu) 0.38f else 0.42f
                setVolume(volume, volume)
                setOnPreparedListener { preparedPlayer ->
                    if (musicPlayer === preparedPlayer) {
                        preparedPlayer.start()
                    }
                }
                setOnErrorListener { erroredPlayer, _, _ ->
                    if (musicPlayer === erroredPlayer) {
                        musicPlayer = null
                        currentMusicTrack = null
                    }
                    erroredPlayer.release()
                    true
                }
                prepareAsync()
            }
        }.getOrNull()
    }

    private fun loadSfx(fileName: String): Int {
        return runCatching {
            appContext.assets.openFd("sounds/sfx/$fileName").use { descriptor ->
                soundPool.load(descriptor, 1)
            }
        }.getOrDefault(0)
    }

    private fun play(soundId: Int) {
        if (soundId !in loadedSoundIds) return
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    private data class SoundKey(
        val themeMode: GridfallThemeMode,
        val event: ThemeSoundEvent
    )

    private enum class MusicTrack(val assetPath: String) {
        Menu("sounds/music/menu-background-music.ogg"),
        Premium("sounds/music/background-music.ogg"),
        Inferno("sounds/music/inferno-background-music.ogg"),
        Retro("sounds/music/retro-background-music.ogg");

        companion object {
            fun forTheme(themeMode: GridfallThemeMode): MusicTrack {
                return when (themeMode) {
                    GridfallThemeMode.PremiumTactical -> Premium
                    GridfallThemeMode.InfernoCore -> Inferno
                    GridfallThemeMode.RetroArcade -> Retro
                }
            }
        }
    }
}

enum class ThemeSoundEvent {
    Place,
    Bomb,
    LineClear,
    MultiLineClear,
    GameOver
}

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
    private var currentMusicVolume: Float = 0f
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
        SoundKey(GridfallThemeMode.PremiumTactical, ThemeSoundEvent.Place) to loadSfx("premium_tactical/premium_place.ogg"),
        SoundKey(GridfallThemeMode.PremiumTactical, ThemeSoundEvent.Bomb) to loadSfx("premium_tactical/premium_bomb.ogg"),
        SoundKey(GridfallThemeMode.PremiumTactical, ThemeSoundEvent.LineClear) to loadSfx("premium_tactical/premium_line_clear.ogg"),
        SoundKey(GridfallThemeMode.PremiumTactical, ThemeSoundEvent.MultiLineClear) to loadSfx("premium_tactical/premium_multi_line_clear.ogg"),
        SoundKey(GridfallThemeMode.PremiumTactical, ThemeSoundEvent.GameOver) to loadSfx("premium_tactical/premium_game_over.ogg"),
        SoundKey(GridfallThemeMode.InfernoCore, ThemeSoundEvent.Place) to loadSfx("inferno_core/inferno_place.ogg"),
        SoundKey(GridfallThemeMode.InfernoCore, ThemeSoundEvent.Bomb) to loadSfx("inferno_core/inferno_bomb.ogg"),
        SoundKey(GridfallThemeMode.InfernoCore, ThemeSoundEvent.LineClear) to loadSfx("inferno_core/inferno_line_clear.ogg"),
        SoundKey(GridfallThemeMode.InfernoCore, ThemeSoundEvent.MultiLineClear) to loadSfx("inferno_core/inferno_multi_line_clear.ogg"),
        SoundKey(GridfallThemeMode.InfernoCore, ThemeSoundEvent.GameOver) to loadSfx("inferno_core/inferno_game_over.ogg"),
        SoundKey(GridfallThemeMode.RetroArcade, ThemeSoundEvent.Place) to loadSfx("retro_arcade/retro_place.ogg"),
        SoundKey(GridfallThemeMode.RetroArcade, ThemeSoundEvent.Bomb) to loadSfx("retro_arcade/retro_bomb.ogg"),
        SoundKey(GridfallThemeMode.RetroArcade, ThemeSoundEvent.LineClear) to loadSfx("retro_arcade/retro_line_clear.ogg"),
        SoundKey(GridfallThemeMode.RetroArcade, ThemeSoundEvent.MultiLineClear) to loadSfx("retro_arcade/retro_multi_line_clear.ogg"),
        SoundKey(GridfallThemeMode.RetroArcade, ThemeSoundEvent.GameOver) to loadSfx("retro_arcade/retro_game_over.ogg"),
        SoundKey(GridfallThemeMode.Blockworld, ThemeSoundEvent.Place) to loadSfx("blockworld/blockworld_place.ogg"),
        SoundKey(GridfallThemeMode.Blockworld, ThemeSoundEvent.Bomb) to loadSfx("blockworld/blockworld_bomb.ogg"),
        SoundKey(GridfallThemeMode.Blockworld, ThemeSoundEvent.LineClear) to loadSfx("blockworld/blockworld_clear_line.ogg"),
        SoundKey(GridfallThemeMode.Blockworld, ThemeSoundEvent.MultiLineClear) to loadSfx("blockworld/blockworld_multi_line_clear.ogg"),
        SoundKey(GridfallThemeMode.Blockworld, ThemeSoundEvent.GameOver) to loadSfx("blockworld/blockworld-game-over.ogg"),
        SoundKey(GridfallThemeMode.FrutigerAero, ThemeSoundEvent.Place) to loadSfx("frutiger_aero/frutiger_aero_place.ogg"),
        SoundKey(GridfallThemeMode.FrutigerAero, ThemeSoundEvent.Bomb) to loadSfx("frutiger_aero/frutiger_aero_bomb.ogg"),
        SoundKey(GridfallThemeMode.FrutigerAero, ThemeSoundEvent.LineClear) to loadSfx("frutiger_aero/frutiger_aero_clear_line.ogg"),
        SoundKey(GridfallThemeMode.FrutigerAero, ThemeSoundEvent.MultiLineClear) to loadSfx("frutiger_aero/frutiger_aero_multiple_line_clear.ogg"),
        SoundKey(GridfallThemeMode.FrutigerAero, ThemeSoundEvent.GameOver) to loadSfx("frutiger_aero/frutiger_aero_game_over.ogg")
    )

    private val contractPopup = loadSfx("contract_popup.ogg")
    private val riskSpinAvailable = loadSfx("risk_spin_available.ogg")
    private val riskSpinAccepted = loadSfx("risk_spin_accepted.ogg")
    private val riskSpinFieldChosen = loadSfx("risk_spin_field_chosen.ogg")
    private val contractSuccess = loadSfx("contract_success.ogg")
    private val contractFailed = listOf(
        loadSfx("contract_failed_1.ogg"),
        loadSfx("contract_failed_2.ogg")
    )

    fun playBackgroundMusic(
        themeMode: GridfallThemeMode,
        isMenu: Boolean,
        musicVolume: Float
    ) {
        val targetVolume = musicVolume.coerceIn(0f, 1f)
        if (targetVolume <= 0f) {
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
            currentMusicVolume = targetVolume
            player.setVolume(targetVolume, targetVolume)
            if (!player.isPlaying) {
                runCatching { player.start() }
            }
            return
        }

        stopBackgroundMusic()
        currentMusicTrack = targetTrack
        currentMusicVolume = targetVolume
        musicPlayer = createMusicPlayer(targetTrack)
    }

    fun playThemeEvent(
        themeMode: GridfallThemeMode,
        event: ThemeSoundEvent,
        effectsVolume: Float
    ) {
        val soundId = sounds[SoundKey(themeMode, event)]
            ?: sounds[SoundKey(GridfallThemeMode.PremiumTactical, event)]
        if (soundId != null) {
            play(soundId, effectsVolume)
        }
    }

fun playRiskSpinAvailable(effectsVolume: Float) {
        play(riskSpinAvailable, effectsVolume)
    }

    fun playRiskSpinAccepted(effectsVolume: Float) {
        play(riskSpinAccepted, effectsVolume)
    }

    fun playRiskSpinFieldChosen(effectsVolume: Float) {
        play(riskSpinFieldChosen, effectsVolume)
    }

    fun playContractPopup(effectsVolume: Float) {
        play(contractPopup, effectsVolume)
    }

    fun playContractSuccess(effectsVolume: Float) {
        play(contractSuccess, effectsVolume)
    }

    fun playContractFailed(effectsVolume: Float) {
        play(contractFailed.random(Random.Default), effectsVolume)
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
        currentMusicVolume = 0f
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
                setVolume(currentMusicVolume, currentMusicVolume)
                setOnPreparedListener { preparedPlayer ->
                    if (musicPlayer === preparedPlayer) {
                        preparedPlayer.setVolume(currentMusicVolume, currentMusicVolume)
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

    private fun play(soundId: Int, effectsVolume: Float) {
        val targetVolume = effectsVolume.coerceIn(0f, 1f)
        if (targetVolume <= 0f) return
        if (soundId !in loadedSoundIds) return
        soundPool.play(soundId, targetVolume, targetVolume, 1, 0, 1f)
    }

    private data class SoundKey(
        val themeMode: GridfallThemeMode,
        val event: ThemeSoundEvent
    )

    private enum class MusicTrack(val assetPath: String) {
        Menu("sounds/music/menu-background-music.ogg"),
        Premium("sounds/music/background-music.ogg"),
        Inferno("sounds/music/inferno-background-music.ogg"),
        Retro("sounds/music/retro-background-music.ogg"),
        Blockworld("sounds/music/blockworld-background-music.ogg"),
        FrutigerAero("sounds/music/frutiger_aero-background-music.ogg");

        companion object {
            fun forTheme(themeMode: GridfallThemeMode): MusicTrack {
                return when (themeMode) {
                    GridfallThemeMode.PremiumTactical -> Premium
                    GridfallThemeMode.InfernoCore -> Inferno
                    GridfallThemeMode.RetroArcade -> Retro
                    GridfallThemeMode.Blockworld -> Blockworld
                    GridfallThemeMode.FrutigerAero -> FrutigerAero
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

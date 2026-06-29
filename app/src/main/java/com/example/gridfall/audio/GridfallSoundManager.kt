package com.example.gridfall.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.gridfall.R
import com.example.gridfall.ui.theme.GridfallThemeMode
import kotlin.random.Random

class GridfallSoundManager(context: Context) {
    private val appContext = context.applicationContext
    private val loadedSoundIds = mutableSetOf<Int>()
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
        SoundKey(GridfallThemeMode.PremiumTactical, ThemeSoundEvent.Place) to load(R.raw.premium_place),
        SoundKey(GridfallThemeMode.PremiumTactical, ThemeSoundEvent.Bomb) to load(R.raw.premium_bomb),
        SoundKey(GridfallThemeMode.PremiumTactical, ThemeSoundEvent.LineClear) to load(R.raw.premium_line_clear),
        SoundKey(GridfallThemeMode.PremiumTactical, ThemeSoundEvent.MultiLineClear) to load(R.raw.premium_multi_line_clear),
        SoundKey(GridfallThemeMode.PremiumTactical, ThemeSoundEvent.GameOver) to load(R.raw.premium_game_over),
        SoundKey(GridfallThemeMode.InfernoCore, ThemeSoundEvent.Place) to load(R.raw.inferno_place),
        SoundKey(GridfallThemeMode.InfernoCore, ThemeSoundEvent.Bomb) to load(R.raw.inferno_bomb),
        SoundKey(GridfallThemeMode.InfernoCore, ThemeSoundEvent.LineClear) to load(R.raw.inferno_line_clear),
        SoundKey(GridfallThemeMode.InfernoCore, ThemeSoundEvent.MultiLineClear) to load(R.raw.inferno_multi_line_clear),
        SoundKey(GridfallThemeMode.InfernoCore, ThemeSoundEvent.GameOver) to load(R.raw.inferno_game_over),
        SoundKey(GridfallThemeMode.RetroArcade, ThemeSoundEvent.Place) to load(R.raw.retro_place),
        SoundKey(GridfallThemeMode.RetroArcade, ThemeSoundEvent.Bomb) to load(R.raw.retro_bomb),
        SoundKey(GridfallThemeMode.RetroArcade, ThemeSoundEvent.LineClear) to load(R.raw.retro_line_clear),
        SoundKey(GridfallThemeMode.RetroArcade, ThemeSoundEvent.MultiLineClear) to load(R.raw.retro_multi_line_clear),
        SoundKey(GridfallThemeMode.RetroArcade, ThemeSoundEvent.GameOver) to load(R.raw.retro_game_over)
    )

    private val contractPopup = load(R.raw.contract_popup)
    private val contractSuccess = load(R.raw.contract_success)
    private val contractFailed = listOf(
        load(R.raw.contract_failed_1),
        load(R.raw.contract_failed_2)
    )

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
        soundPool.release()
    }

    private fun load(rawResourceId: Int): Int {
        return soundPool.load(appContext, rawResourceId, 1)
    }

    private fun play(soundId: Int) {
        if (soundId !in loadedSoundIds) return
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    private data class SoundKey(
        val themeMode: GridfallThemeMode,
        val event: ThemeSoundEvent
    )
}

enum class ThemeSoundEvent {
    Place,
    Bomb,
    LineClear,
    MultiLineClear,
    GameOver
}

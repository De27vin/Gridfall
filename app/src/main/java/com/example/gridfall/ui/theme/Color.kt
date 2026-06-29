package com.example.gridfall.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Core Surfaces
val MidnightNavy = Color(0xFF070E18)
val DeepGraphite = Color(0xFF10141C)
val DarkGlass = Color(0xCC0B1320)
val TacticalFrame = Color(0xFF0D1726)
val DeepBoardNavy = Color(0xFF111C2B)
val MutedCellNavy = Color(0xFF1A2738)
val NearBlackNavy = Color(0xFF08111C)

// Blocks
val ArcCyan = Color(0xFF35D6FF)
val TacticalViolet = Color(0xFF8E6CFF)
val SignalAmber = Color(0xFFFFB84A)
val RewardMint = Color(0xFF43F2A1)

val ArcCyanTop = Color(0xFF65E6FF)
val ArcCyanBottom = Color(0xFF168FB0)

val TacticalVioletTop = Color(0xFFAA91FF)
val TacticalVioletBottom = Color(0xFF5940B8)

val SignalAmberTop = Color(0xFFFFD37A)
val SignalAmberBottom = Color(0xFFB97318)

val RewardMintTop = Color(0xFF78FFC0)
val RewardMintBottom = Color(0xFF159B6B)

// Bomb Colors
val BombMagenta = Color(0xFFFF4FD8)
val HotCore = Color(0xFFFFE66D)
val WarningOrange = Color(0xFFFF8A3D)
val BombGlow = Color(0x66FF4FD8)

// Placement States
val MintGhost = Color(0x4D43F2A1)
val MintBorder = Color(0xFF43F2A1)
val CoralGhost = Color(0x59FF5A6A)
val CoralBorder = Color(0xFFFF5A6A)

// Text
val IceWhite = Color(0xFFF3F8FF)
val SoftIce = Color(0xFFDCE8F7)
val LevelCyan = Color(0xFF4DE3FF)
val ComboAmber = Color(0xFFFFC857)
val BlueGray = Color(0xFF8EA0B8)
val MutedSlate = Color(0xFF5F7086)

// Contracts / Dialogs / Buttons
val DeepContractGlass = Color(0xF2151D2A)
val ContractChipNavy = Color(0xFF19283A)
val SlateButton = Color(0xFF223044)
val PressedSlate = Color(0xFF172334)
val ActionCyan = Color(0xFF2BD4F7)
val DeepCyan = Color(0xFF159AB8)
val DialogNavy = Color(0xFF151D2A)
val SoftCyanBorder = Color(0xFF31536A)
val CoralWarning = Color(0xFFFF5A6A)
val PremiumGold = Color(0xFFFFD166)

enum class GridfallThemeMode(val id: String, val label: String) {
    PremiumTactical("premium_tactical", "Premium Tactical"),
    InfernoCore("inferno_core", "Inferno Core"),
    RetroArcade("retro_arcade", "Retro Arcade");

    companion object {
        fun fromId(id: String?): GridfallThemeMode {
            return entries.firstOrNull { it.id == id } ?: PremiumTactical
        }
    }
}

data class GridfallColors(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val darkGlass: Color,
    val tacticalFrame: Color,
    val boardInner: Color,
    val emptyCell: Color,
    val emptyCellBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentStrong: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val button: Color,
    val buttonPressed: Color,
    val dialogBackground: Color,
    val chipBackground: Color,
    val panelBackground: Color,
    val panelBorder: Color,
    val validPreviewFill: Color,
    val validPreviewBorder: Color,
    val invalidPreviewFill: Color,
    val invalidPreviewBorder: Color,
    val contractWarningFill: Color,
    val contractWarningBorder: Color,
    val block1Top: Color,
    val block1Mid: Color,
    val block1Bottom: Color,
    val block2Top: Color,
    val block2Mid: Color,
    val block2Bottom: Color,
    val block3Top: Color,
    val block3Mid: Color,
    val block3Bottom: Color,
    val block4Top: Color,
    val block4Mid: Color,
    val block4Bottom: Color,
    val bombOuter: Color,
    val bombCore: Color,
    val bombInner: Color,
    val bombGlow: Color
)

val PremiumTacticalColors = GridfallColors(
    backgroundTop = MidnightNavy,
    backgroundBottom = DeepGraphite,
    darkGlass = DarkGlass,
    tacticalFrame = TacticalFrame,
    boardInner = DeepBoardNavy,
    emptyCell = MutedCellNavy,
    emptyCellBorder = SoftCyanBorder,
    textPrimary = IceWhite,
    textSecondary = SoftIce,
    textMuted = BlueGray,
    accent = LevelCyan,
    accentStrong = ActionCyan,
    success = RewardMint,
    warning = PremiumGold,
    danger = CoralWarning,
    button = SlateButton,
    buttonPressed = PressedSlate,
    dialogBackground = DialogNavy,
    chipBackground = ContractChipNavy,
    panelBackground = DeepContractGlass,
    panelBorder = SoftCyanBorder,
    validPreviewFill = MintGhost,
    validPreviewBorder = MintBorder,
    invalidPreviewFill = CoralGhost,
    invalidPreviewBorder = CoralBorder,
    contractWarningFill = CoralWarning.copy(alpha = 0.14f),
    contractWarningBorder = CoralWarning.copy(alpha = 0.30f),
    block1Top = ArcCyanTop,
    block1Mid = ArcCyan,
    block1Bottom = ArcCyanBottom,
    block2Top = TacticalVioletTop,
    block2Mid = TacticalViolet,
    block2Bottom = TacticalVioletBottom,
    block3Top = SignalAmberTop,
    block3Mid = SignalAmber,
    block3Bottom = SignalAmberBottom,
    block4Top = RewardMintTop,
    block4Mid = RewardMint,
    block4Bottom = RewardMintBottom,
    bombOuter = BombMagenta,
    bombCore = HotCore,
    bombInner = WarningOrange,
    bombGlow = BombGlow
)

val InfernoCoreColors = GridfallColors(
    backgroundTop = Color(0xFF0D0504),
    backgroundBottom = Color(0xFF1A0704),
    darkGlass = Color(0xE0160805),
    tacticalFrame = Color(0xFF2F1209),
    boardInner = Color(0xFF110604),
    emptyCell = Color(0xFF251009),
    emptyCellBorder = Color(0xFF4A1D0E),
    textPrimary = Color(0xFFFFE6B3),
    textSecondary = Color(0xFFFFD9A0),
    textMuted = Color(0xFFB98A6A),
    accent = Color(0xFFFF8A00),
    accentStrong = Color(0xFFFFD166),
    success = Color(0xFFFFD166),
    warning = Color(0xFFFFB347),
    danger = Color(0xFFFF1744),
    button = Color(0xFF873412),
    buttonPressed = Color(0xFFB64616),
    dialogBackground = Color(0xFF1A0905),
    chipBackground = Color(0xFF2D1209),
    panelBackground = Color(0xF2220D07),
    panelBorder = Color(0xFF823313),
    validPreviewFill = Color(0x4DFFB347),
    validPreviewBorder = Color(0xFFFFB347),
    invalidPreviewFill = Color(0x66FF1744),
    invalidPreviewBorder = Color(0xFFFF1744),
    contractWarningFill = Color(0x33A50F1F),
    contractWarningBorder = Color(0x99FF1744),
    block1Top = Color(0xFFFFC25B),
    block1Mid = Color(0xFFFF6A1A),
    block1Bottom = Color(0xFF8E2409),
    block2Top = Color(0xFFFFD980),
    block2Mid = Color(0xFFFF8A24),
    block2Bottom = Color(0xFFA93209),
    block3Top = Color(0xFFFFE69A),
    block3Mid = Color(0xFFFFBE32),
    block3Bottom = Color(0xFF9F5A0B),
    block4Top = Color(0xFFFF9B6D),
    block4Mid = Color(0xFFFF3D00),
    block4Bottom = Color(0xFF7C1206),
    bombOuter = Color(0xFFFF3D00),
    bombCore = Color(0xFFFFD166),
    bombInner = Color(0xFFFF8A00),
    bombGlow = Color(0x77FF8A00)
)

val RetroArcadeColors = GridfallColors(
    backgroundTop = Color(0xFF070014),
    backgroundBottom = Color(0xFF120026),
    darkGlass = Color(0xE8120026),
    tacticalFrame = Color(0xFF100020),
    boardInner = Color(0xFF05000D),
    emptyCell = Color(0xFF140026),
    emptyCellBorder = Color(0xFF402060),
    textPrimary = Color(0xFFFFE600),
    textSecondary = Color(0xFF00F5FF),
    textMuted = Color(0xFFA875FF),
    accent = Color(0xFF00F5FF),
    accentStrong = Color(0xFFFF2BD6),
    success = Color(0xFF7CFF00),
    warning = Color(0xFFFFE600),
    danger = Color(0xFFFF1744),
    button = Color(0xFF24004D),
    buttonPressed = Color(0xFF3A007A),
    dialogBackground = Color(0xFF100020),
    chipBackground = Color(0xFF1B0038),
    panelBackground = Color(0xF218002F),
    panelBorder = Color(0xFFFF2BD6),
    validPreviewFill = Color(0x4D7CFF00),
    validPreviewBorder = Color(0xFF7CFF00),
    invalidPreviewFill = Color(0x70FF1744),
    invalidPreviewBorder = Color(0xFFFF1744),
    contractWarningFill = Color(0x335A1AFF),
    contractWarningBorder = Color(0xCCFF2BD6),
    block1Top = Color(0xFF82FFFF),
    block1Mid = Color(0xFF00F5FF),
    block1Bottom = Color(0xFF0088A8),
    block2Top = Color(0xFFFF7BEA),
    block2Mid = Color(0xFFFF2BD6),
    block2Bottom = Color(0xFF9E007F),
    block3Top = Color(0xFFFFFF7A),
    block3Mid = Color(0xFFFFE600),
    block3Bottom = Color(0xFFB99500),
    block4Top = Color(0xFFB6FF66),
    block4Mid = Color(0xFF7CFF00),
    block4Bottom = Color(0xFF2F9E00),
    bombOuter = Color(0xFFFF2BD6),
    bombCore = Color(0xFFFFE600),
    bombInner = Color(0xFF00F5FF),
    bombGlow = Color(0x7700F5FF)
)

val LocalGridfallColors = staticCompositionLocalOf { PremiumTacticalColors }

fun colorsForThemeMode(mode: GridfallThemeMode): GridfallColors {
    return when (mode) {
        GridfallThemeMode.PremiumTactical -> PremiumTacticalColors
        GridfallThemeMode.InfernoCore -> InfernoCoreColors
        GridfallThemeMode.RetroArcade -> RetroArcadeColors
    }
}

package com.example.gridfall.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.gridfall.ui.theme.FrutigerEaroColors
import com.example.gridfall.ui.theme.BlockworldColors
import com.example.gridfall.ui.theme.GridfallColors
import com.example.gridfall.ui.theme.InfernoCoreColors
import com.example.gridfall.ui.theme.PremiumTacticalColors
import com.example.gridfall.ui.theme.RetroArcadeColors

internal enum class ThemeIconKind(val filePrefix: String) {
    Reload("reload"),
    Undo("undo")
}

@Composable
internal fun ThemeIconAsset(
    kind: ThemeIconKind,
    colors: GridfallColors,
    contentDescription: String?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    fallback: @Composable (Modifier) -> Unit
) {
    val context = LocalContext.current
    val assetPath = remember(kind, colors) {
        "icons/${kind.filePrefix}_${colors.assetThemeSuffix()}.png"
    }
    val imageBitmap = remember(assetPath) {
        runCatching {
            context.assets.open(assetPath).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = modifier.alpha(if (enabled) 1f else 0.45f)
        )
    } else {
        fallback(modifier)
    }
}

private fun GridfallColors.assetThemeSuffix(): String {
    return when (this) {
        PremiumTacticalColors -> "premium"
        InfernoCoreColors -> "inferno"
        RetroArcadeColors -> "retro"
        FrutigerEaroColors -> "premium"
        else -> "premium"
    }
}

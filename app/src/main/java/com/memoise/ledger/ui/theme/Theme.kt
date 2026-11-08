package com.memoise.ledger.ui.theme

import android.annotation.SuppressLint
import android.os.Build
import android.view.accessibility.AccessibilityManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.memoise.ledger.data.preferences.ThemePreference

private val LightColors = lightColorScheme(
    primary = Olive,
    onPrimary = Paper,
    secondary = Terracotta,
    onSecondary = Paper,
    tertiary = Terracotta,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    onSurfaceVariant = MutedInk,
    outlineVariant = Rule,
)

private val DarkColors = darkColorScheme(
    primary = NightOlive,
    secondary = NightTerracotta,
    tertiary = NightTerracotta,
    background = NightPaper,
    onBackground = NightInk,
    surface = NightPaper,
    onSurface = NightInk,
    onSurfaceVariant = NightMutedInk,
    outlineVariant = NightRule,
)

private val HighContrastLightColors = LightColors.copy(
    primary = HighContrastOlive,
    secondary = HighContrastTerracotta,
    tertiary = HighContrastTerracotta,
)

@Composable
fun MemoiseTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    val highContrastTextEnabled = rememberSystemHighContrastTextEnabled()
    val typography = MaterialTheme.typography.copy(
        headlineLarge = TextStyle(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Normal,
            fontSize = 34.sp,
            lineHeight = 40.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Normal,
            fontSize = 23.sp,
            lineHeight = 29.sp,
        ),
    )

    MaterialTheme(
        colorScheme = when {
            darkTheme -> DarkColors
            highContrastTextEnabled -> HighContrastLightColors
            else -> LightColors
        },
        typography = typography,
        content = content,
    )
}

@Composable
@SuppressLint("NewApi")
private fun rememberSystemHighContrastTextEnabled(): Boolean {
    val context = LocalContext.current
    val accessibilityManager = remember(context) {
        context.getSystemService(AccessibilityManager::class.java)
    }
    var enabled by remember(accessibilityManager) {
        mutableStateOf(
            Build.VERSION.SDK_INT >= 36 && accessibilityManager?.isHighContrastTextEnabled == true,
        )
    }

    if (Build.VERSION.SDK_INT >= 36) {
        DisposableEffect(accessibilityManager) {
            val manager = accessibilityManager
            if (manager != null) {
                val listener = AccessibilityManager.HighContrastTextStateChangeListener {
                    enabled = it
                }
                manager.addHighContrastTextStateChangeListener(context.mainExecutor, listener)
                enabled = manager.isHighContrastTextEnabled
                onDispose { manager.removeHighContrastTextStateChangeListener(listener) }
            } else {
                onDispose { }
            }
        }
    }
    return enabled
}

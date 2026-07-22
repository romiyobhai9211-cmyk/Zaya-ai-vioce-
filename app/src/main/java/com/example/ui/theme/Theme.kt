package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ZoyaColorScheme = darkColorScheme(
  primary = NeonMagenta,
  onPrimary = Color.White,
  primaryContainer = ObsidianCard,
  onPrimaryContainer = NeonCyan,
  secondary = NeonCyan,
  onSecondary = Color.Black,
  secondaryContainer = ObsidianCardBorder,
  onSecondaryContainer = TextPrimary,
  tertiary = NeonViolet,
  onTertiary = Color.White,
  background = ObsidianDark,
  onBackground = TextPrimary,
  surface = ObsidianSurface,
  onSurface = TextPrimary,
  surfaceVariant = ObsidianCard,
  onSurfaceVariant = TextSecondary,
  outline = ObsidianCardBorder,
  outlineVariant = TextMuted
)

@Composable
fun ZoyaTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = ZoyaColorScheme,
    typography = Typography,
    content = content
  )
}


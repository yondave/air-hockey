package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ArcadeColorScheme = darkColorScheme(
  primary = NeonCyan,
  onPrimary = ArenaDark,
  primaryContainer = ArenaSurfaceVariant,
  onPrimaryContainer = NeonCyan,
  secondary = NeonMagenta,
  onSecondary = ArenaDark,
  secondaryContainer = ArenaSurfaceVariant,
  onSecondaryContainer = NeonMagenta,
  tertiary = NeonGold,
  onTertiary = ArenaDark,
  background = ArenaDark,
  onBackground = TextPrimary,
  surface = ArenaSurface,
  onSurface = TextPrimary,
  surfaceVariant = ArenaSurfaceVariant,
  onSurfaceVariant = TextSecondary,
  outline = ArenaBorder
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = ArcadeColorScheme,
    typography = Typography,
    content = content
  )
}

package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsHockey
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.BotDifficulty
import com.example.physics.MapLayout
import com.example.ui.rendering.PerspectiveRenderer
import com.example.ui.theme.ArenaBorder
import com.example.ui.theme.ArenaDark
import com.example.ui.theme.ArenaSurface
import com.example.ui.theme.ArenaSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanGlow
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonMagentaGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.isActive

@Composable
fun GameScreen(
  viewModel: GameViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val renderer = remember { PerspectiveRenderer() }
  var renderTick by remember { mutableLongStateOf(0L) }

  // Continuous VSYNC display frame loop driving physics & rendering
  // Only runs when status is PLAYING or GOAL_CELEBRATION — NEVER when GAME_OVER, PAUSED, SETTINGS, or MENU
  LaunchedEffect(uiState.status) {
    if (uiState.status == MatchStatus.PLAYING || uiState.status == MatchStatus.GOAL_CELEBRATION) {
      var lastNano = System.nanoTime()
      while (isActive) {
        withFrameNanos { nowNano ->
          val dt = ((nowNano - lastNano) / 1_000_000_000f).coerceIn(0.001f, 0.033f)
          lastNano = nowNano
          viewModel.updateGame(dt)
          renderTick = nowNano
        }
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(ArenaDark)
      .testTag("game_screen_container")
  ) {
    renderer.isFlat = uiState.flatPerspective

    // 1. Interactive 3D/Flat Arena Canvas with Multi-Touch Support
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(uiState.flatPerspective) {
          awaitPointerEventScope {
            while (true) {
              val event = awaitPointerEvent()
              for (change in event.changes) {
                if (change.pressed) {
                  val virtual = renderer.toVirtual(change.position.x, change.position.y)
                  // Bottom half: Player 1; Top half: Player 2
                  if (virtual.y >= renderer.virtualHeight * 0.5f) {
                    viewModel.onPlayerTouch(1, virtual)
                  } else {
                    viewModel.onPlayerTouch(2, virtual)
                  }
                  change.consume()
                }
              }
            }
          }
        }
    ) {
      val currentFrame = renderTick

      renderer.drawArena(
        drawScope = this,
        physics = viewModel.physics,
        particles = viewModel.particles,
        floatingScores = viewModel.floatingScores,
        screenShakeX = viewModel.screenShakeX,
        screenShakeY = viewModel.screenShakeY,
        flatPerspective = uiState.flatPerspective
      )
    }

    // 2. In-Game HUD (Visible during PLAYING, CELEBRATION, and PAUSED)
    if (uiState.status != MatchStatus.MENU) {
      if (uiState.mode == GameMode.SOLO_VS_AI) {
        // Unified Top Cyber Scoreboard for Solo vs AI:
        // Positioned at top header, leaving the bottom goal area and player 1's crease 100% UNCLUTTERED!
        Surface(
          shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
          color = ArenaSurface.copy(alpha = 0.92f),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, ArenaBorder.copy(alpha = 0.7f)),
          modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(top = 4.dp, start = 12.dp, end = 60.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Player 1 (Cyan)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "${uiState.p1Goals}",
                color = NeonCyan,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
              )
              Column {
                Text(
                  text = "PLAYER 1",
                  color = NeonCyan,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.ExtraBold,
                  letterSpacing = 0.8.sp
                )
                Text(
                  text = "${uiState.p1PinballScore} PTS" + if (uiState.p1Combo > 1) " (${uiState.p1Combo}x)" else "",
                  color = NeonGold,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            // Match Target & Layout Pill
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "TARGET: ${uiState.targetGoals}",
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = uiState.mapLayout.title,
                color = NeonGreen,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold
              )
            }

            // Cyber Bot (Magenta)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "CYBER BOT",
                  color = NeonMagenta,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.ExtraBold,
                  letterSpacing = 0.8.sp
                )
                Text(
                  text = "${uiState.p2PinballScore} PTS" + if (uiState.p2Combo > 1) " (${uiState.p2Combo}x)" else "",
                  color = NeonGold,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
              Text(
                text = "${uiState.p2Goals}",
                color = NeonMagenta,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
              )
            }
          }
        }
      } else {
        // Two-Player Local Mode:
        // Position score badges at the CORNERS so they NEVER overlap the center goals!
        // Player 2 Corner Badge (Top-Start, rotated 180° for Player 2):
        Box(
          modifier = Modifier
            .align(Alignment.TopStart)
            .statusBarsPadding()
            .padding(top = 4.dp, start = 12.dp)
            .rotate(180f)
        ) {
          PlayerCornerChip(
            playerName = "PLAYER 2",
            goals = uiState.p2Goals,
            pinballScore = uiState.p2PinballScore,
            combo = uiState.p2Combo,
            accentColor = NeonMagenta
          )
        }

        // Player 1 Corner Badge (Bottom-Start corner, leaves center goal mouth 100% open):
        Box(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .navigationBarsPadding()
            .padding(bottom = 6.dp, start = 12.dp)
        ) {
          PlayerCornerChip(
            playerName = "PLAYER 1",
            goals = uiState.p1Goals,
            pinballScore = uiState.p1PinballScore,
            combo = uiState.p1Combo,
            accentColor = NeonCyan
          )
        }
      }

      // Pause Button: cleanly positioned in top-right corner, off the playing ice
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .statusBarsPadding()
          .padding(top = 4.dp, end = 10.dp)
      ) {
        IconButton(
          onClick = { viewModel.pauseGame() },
          modifier = Modifier
            .size(44.dp)
            .background(ArenaSurface.copy(alpha = 0.90f), CircleShape)
            .border(1.5.dp, NeonCyan.copy(alpha = 0.7f), CircleShape)
            .testTag("pause_button")
        ) {
          Icon(
            imageVector = Icons.Default.Pause,
            contentDescription = "Pause Game",
            tint = NeonCyan,
            modifier = Modifier.size(22.dp)
          )
        }
      }
    }

    // 3. Main Menu Screen with Map Layout Selection
    AnimatedVisibility(
      visible = uiState.status == MatchStatus.MENU,
      enter = fadeIn() + scaleIn(initialScale = 0.92f),
      exit = fadeOut() + scaleOut(targetScale = 0.92f),
      modifier = Modifier.align(Alignment.Center)
    ) {
      MainMenuScreen(
        uiState = uiState,
        onStartMatch = { mode, layout -> viewModel.startMatch(mode, layout) },
        onSelectLayout = { viewModel.setMapLayout(it) },
        onSelectMode = { viewModel.setGameMode(it) },
        onTargetGoalsChange = { viewModel.setTargetGoals(it) },
        onDifficultyChange = { viewModel.setBotDifficulty(it) },
        onToggleSound = { viewModel.toggleSound() },
        onToggleHaptics = { viewModel.toggleHaptics() }
      )
    }

    // 4. Goal Celebration Animated Banner
    AnimatedVisibility(
      visible = uiState.status == MatchStatus.GOAL_CELEBRATION,
      enter = scaleIn() + fadeIn(),
      exit = scaleOut() + fadeOut(),
      modifier = Modifier.align(Alignment.Center)
    ) {
      val scorerColor = if (uiState.lastScorer == 1) NeonCyan else NeonMagenta
      val scorerText = if (uiState.lastScorer == 1) "PLAYER 1 GOAL!" else if (uiState.mode == GameMode.SOLO_VS_AI) "CYBER BOT GOAL!" else "PLAYER 2 GOAL!"

      Surface(
        shape = RoundedCornerShape(20.dp),
        color = ArenaDark.copy(alpha = 0.94f),
        border = androidx.compose.foundation.BorderStroke(2.dp, scorerColor),
        modifier = Modifier.padding(32.dp)
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "GOAL!",
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            color = scorerColor,
            letterSpacing = 3.sp
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = scorerText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "NEXT SERVE IN ${(uiState.celebrationCountdown).coerceAtLeast(0.1f).toInt() + 1}...",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
          )
        }
      }
    }

    // 5. Pause Menu Drawer (slides in from the right)
    AnimatedVisibility(
      visible = uiState.status == MatchStatus.PAUSED,
      enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
      exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
      modifier = Modifier.align(Alignment.CenterEnd)
    ) {
      PauseMenuDrawer(
        uiState = uiState,
        onResume = { viewModel.resumeGame() },
        onRestart = { viewModel.resetMatch() },
        onOpenSettings = { viewModel.openSettings() },
        onHome = { viewModel.returnToMenu() },
        onToggleSound = { viewModel.toggleSound() },
        onToggleHaptics = { viewModel.toggleHaptics() },
        onTogglePerspective = { viewModel.togglePerspective() }
      )
    }

    // 6. Settings Modal Overlay
    AnimatedVisibility(
      visible = uiState.status == MatchStatus.SETTINGS,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.Center)
    ) {
      SettingsOverlay(
        uiState = uiState,
        onClose = { viewModel.closeSettings() },
        onSelectLayout = { viewModel.setMapLayout(it) },
        onTargetGoalsChange = { viewModel.setTargetGoals(it) },
        onDifficultyChange = { viewModel.setBotDifficulty(it) },
        onToggleSound = { viewModel.toggleSound() },
        onToggleHaptics = { viewModel.toggleHaptics() },
        onTogglePerspective = { viewModel.togglePerspective() },
        onResetMatch = { viewModel.resetMatch() },
        onReturnToMenu = { viewModel.returnToMenu() }
      )
    }

    // 6. Victory / Game Over Overlay
    AnimatedVisibility(
      visible = uiState.status == MatchStatus.GAME_OVER,
      enter = scaleIn() + fadeIn(),
      exit = scaleOut() + fadeOut(),
      modifier = Modifier.align(Alignment.Center)
    ) {
      GameOverOverlay(
        uiState = uiState,
        onRematch = { viewModel.resetMatch() },
        onMenu = { viewModel.returnToMenu() }
      )
    }
  }
}

@Composable
fun MainMenuScreen(
  uiState: GameUiState,
  onStartMatch: (GameMode, MapLayout) -> Unit,
  onSelectLayout: (MapLayout) -> Unit,
  onSelectMode: (GameMode) -> Unit,
  onTargetGoalsChange: (Int) -> Unit,
  onDifficultyChange: (BotDifficulty) -> Unit,
  onToggleSound: () -> Unit,
  onToggleHaptics: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(ArenaDark.copy(alpha = 0.92f))
      .padding(horizontal = 20.dp, vertical = 24.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = ArenaSurface),
      border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.7f)),
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .testTag("main_menu_card")
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // App Title
        Text(
          text = "CYBERPUCK 3D",
          fontSize = 26.sp,
          fontWeight = FontWeight.Black,
          color = NeonCyan,
          letterSpacing = 2.sp
        )
        Text(
          text = "AIR HOCKEY & PINBALL ARENA",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = NeonGold,
          letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Game Mode Selector
        Text(
          text = "CHOOSE GAME MODE",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          GameMode.values().forEach { mode ->
            val isSelected = uiState.mode == mode
            val color = if (mode == GameMode.TWO_PLAYER) NeonCyan else NeonMagenta
            OutlinedButton(
              onClick = { onSelectMode(mode) },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isSelected) color.copy(alpha = 0.25f) else Color.Transparent,
                contentColor = if (isSelected) color else TextSecondary
              ),
              border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isSelected) color else ArenaBorder
              ),
              modifier = Modifier.weight(1f).height(44.dp)
            ) {
              Text(
                text = mode.label,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = if (uiState.flatPerspective) "VIEW: FLAT 2D (SYMMETRIC LOCAL 2P)" else "VIEW: 3D TILTED ARCADE",
          color = if (uiState.flatPerspective) NeonGreen else NeonCyan,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Map Layout Selection
        Text(
          text = "SELECT MAP LAYOUT",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          MapLayout.values().forEach { layout ->
            val isSelected = uiState.mapLayout == layout
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) ArenaSurfaceVariant else ArenaDark.copy(alpha = 0.6f),
              border = androidx.compose.foundation.BorderStroke(
                if (isSelected) 1.5.dp else 1.dp,
                if (isSelected) NeonGold else ArenaBorder
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectLayout(layout) }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = layout.title,
                    color = if (isSelected) NeonGold else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = layout.subtitle,
                    color = TextMuted,
                    fontSize = 10.sp
                  )
                }
                if (isSelected) {
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonGold.copy(alpha = 0.2f)
                  ) {
                    Text(
                      text = "ACTIVE",
                      color = NeonGold,
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Black,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Target Goals
        Text(
          text = "GOALS TO WIN",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          listOf(3, 5, 7, 10).forEach { goals ->
            val isSelected = uiState.targetGoals == goals
            OutlinedButton(
              onClick = { onTargetGoalsChange(goals) },
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(0.dp),
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.Transparent,
                contentColor = if (isSelected) NeonCyan else TextSecondary
              ),
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isSelected) NeonCyan else ArenaBorder
              ),
              modifier = Modifier.size(56.dp, 38.dp)
            ) {
              Text(
                text = "$goals",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
            }
          }
        }

        // 4. AI Difficulty (if Solo mode)
        if (uiState.mode == GameMode.SOLO_VS_AI) {
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = "AI OPPONENT DIFFICULTY",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            BotDifficulty.values().forEach { diff ->
              val isSelected = uiState.botDifficulty == diff
              OutlinedButton(
                onClick = { onDifficultyChange(diff) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                  containerColor = if (isSelected) NeonMagenta.copy(alpha = 0.2f) else Color.Transparent,
                  contentColor = if (isSelected) NeonMagenta else TextSecondary
                ),
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (isSelected) NeonMagenta else ArenaBorder
                ),
                modifier = Modifier.weight(1f).height(36.dp)
              ) {
                Text(
                  text = diff.name,
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.sp,
                  maxLines = 1
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 5. Sound & Haptics Quick Toggles
        Row(
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = onToggleSound,
            modifier = Modifier
              .background(ArenaSurfaceVariant, CircleShape)
              .size(40.dp)
          ) {
            Icon(
              imageVector = if (uiState.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
              contentDescription = "Toggle Audio",
              tint = if (uiState.soundEnabled) NeonCyan else TextMuted,
              modifier = Modifier.size(18.dp)
            )
          }

          IconButton(
            onClick = onToggleHaptics,
            modifier = Modifier
              .background(ArenaSurfaceVariant, CircleShape)
              .size(40.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Vibration,
              contentDescription = "Toggle Haptics",
              tint = if (uiState.hapticsEnabled) NeonGreen else TextMuted,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Big Primary Launch Button
        Button(
          onClick = { onStartMatch(uiState.mode, uiState.mapLayout) },
          colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ArenaDark),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("launch_match_button")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = null,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "LAUNCH MATCH",
              fontWeight = FontWeight.Black,
              fontSize = 15.sp,
              letterSpacing = 1.sp
            )
          }
        }
      }
    }
  }
}

@Composable
fun PlayerCornerChip(
  playerName: String,
  goals: Int,
  pinballScore: Int,
  combo: Int,
  accentColor: Color
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = ArenaSurface.copy(alpha = 0.88f),
    border = androidx.compose.foundation.BorderStroke(1.2.dp, accentColor.copy(alpha = 0.6f)),
    modifier = Modifier.widthIn(max = 130.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(
        text = "$goals",
        color = TextPrimary,
        fontSize = 20.sp,
        fontWeight = FontWeight.Black
      )
      Column {
        Text(
          text = playerName,
          color = accentColor,
          fontSize = 9.sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 0.5.sp
        )
        Text(
          text = "$pinballScore PTS" + if (combo > 1) " (${combo}x)" else "",
          color = NeonGold,
          fontSize = 8.sp,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}

@Composable
fun PlayerScoreBar(
  playerName: String,
  goals: Int,
  pinballScore: Int,
  combo: Int,
  accentColor: Color,
  glowColor: Color
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = ArenaSurface.copy(alpha = 0.88f),
    border = androidx.compose.foundation.BorderStroke(1.2.dp, accentColor.copy(alpha = 0.6f))
  ) {
    Row(
      modifier = Modifier
        .padding(horizontal = 12.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "$goals",
        color = TextPrimary,
        fontSize = 24.sp,
        fontWeight = FontWeight.Black
      )
      Column {
        Text(
          text = playerName,
          color = accentColor,
          fontSize = 11.sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 0.8.sp
        )
        Text(
          text = "PINBALL: $pinballScore PTS" + if (combo > 1) " (${combo}x)" else "",
          color = NeonGold,
          fontSize = 9.sp,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}

@Composable
fun SettingsOverlay(
  uiState: GameUiState,
  onClose: () -> Unit,
  onSelectLayout: (MapLayout) -> Unit,
  onTargetGoalsChange: (Int) -> Unit,
  onDifficultyChange: (BotDifficulty) -> Unit,
  onToggleSound: () -> Unit,
  onToggleHaptics: () -> Unit,
  onTogglePerspective: () -> Unit,
  onResetMatch: () -> Unit,
  onReturnToMenu: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(ArenaDark.copy(alpha = 0.90f))
      .padding(20.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = ArenaSurface),
      border = androidx.compose.foundation.BorderStroke(1.5.dp, ArenaBorder),
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "ARENA SETTINGS",
          fontSize = 20.sp,
          fontWeight = FontWeight.Black,
          color = NeonCyan,
          letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Camera Perspective Mode
        Text(
          text = "CAMERA PERSPECTIVE",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = { if (!uiState.flatPerspective) onTogglePerspective() },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = if (uiState.flatPerspective) NeonGreen.copy(alpha = 0.25f) else Color.Transparent,
              contentColor = if (uiState.flatPerspective) NeonGreen else TextSecondary
            ),
            border = androidx.compose.foundation.BorderStroke(
              1.2.dp,
              if (uiState.flatPerspective) NeonGreen else ArenaBorder
            ),
            modifier = Modifier.weight(1f).height(38.dp)
          ) {
            Text("FLAT 2D (FAIR 2P)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
          }

          OutlinedButton(
            onClick = { if (uiState.flatPerspective) onTogglePerspective() },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = if (!uiState.flatPerspective) NeonCyan.copy(alpha = 0.25f) else Color.Transparent,
              contentColor = if (!uiState.flatPerspective) NeonCyan else TextSecondary
            ),
            border = androidx.compose.foundation.BorderStroke(
              1.2.dp,
              if (!uiState.flatPerspective) NeonCyan else ArenaBorder
            ),
            modifier = Modifier.weight(1f).height(38.dp)
          ) {
            Text("3D TILTED", fontWeight = FontWeight.Bold, fontSize = 11.sp)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Change Map Layout
        Text(
          text = "SWITCH MAP LAYOUT",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          MapLayout.values().forEach { layout ->
            val isSelected = uiState.mapLayout == layout
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isSelected) ArenaSurfaceVariant else Color.Transparent,
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isSelected) NeonGold else ArenaBorder
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectLayout(layout) }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = layout.title,
                  color = if (isSelected) NeonGold else TextPrimary,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
                if (isSelected) {
                  Text("ACTIVE", color = NeonGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Target Goals Option
        Text(
          text = "TARGET GOALS TO WIN",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          listOf(3, 5, 7, 10).forEach { goals ->
            val isSelected = uiState.targetGoals == goals
            OutlinedButton(
              onClick = { onTargetGoalsChange(goals) },
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(0.dp),
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.Transparent,
                contentColor = if (isSelected) NeonCyan else TextSecondary
              ),
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isSelected) NeonCyan else ArenaBorder
              ),
              modifier = Modifier.size(56.dp, 38.dp)
            ) {
              Text(
                text = "$goals",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sound & Haptics Toggles
        Row(
          horizontalArrangement = Arrangement.spacedBy(20.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = onToggleSound,
            modifier = Modifier
              .background(ArenaSurfaceVariant, CircleShape)
              .size(42.dp)
          ) {
            Icon(
              imageVector = if (uiState.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
              contentDescription = "Toggle Audio",
              tint = if (uiState.soundEnabled) NeonCyan else TextMuted,
              modifier = Modifier.size(18.dp)
            )
          }

          IconButton(
            onClick = onToggleHaptics,
            modifier = Modifier
              .background(ArenaSurfaceVariant, CircleShape)
              .size(42.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Vibration,
              contentDescription = "Toggle Haptics",
              tint = if (uiState.hapticsEnabled) NeonGreen else TextMuted,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
          onClick = {
            onResetMatch()
            onClose()
          },
          colors = ButtonDefaults.buttonColors(containerColor = NeonGold, contentColor = ArenaDark),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
        ) {
          Text("RESET ROUND", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
          onClick = onReturnToMenu,
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
          border = androidx.compose.foundation.BorderStroke(1.dp, ArenaBorder),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
        ) {
          Text("EXIT TO MAIN MENU", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
          onClick = onClose,
          colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ArenaDark),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
        ) {
          Text("RESUME PLAY", fontWeight = FontWeight.Black)
        }
      }
    }
  }
}

@Composable
fun GameOverOverlay(
  uiState: GameUiState,
  onRematch: () -> Unit,
  onMenu: () -> Unit
) {
  val isP1Winner = uiState.winnerPlayer == 1
  val winnerColor = if (isP1Winner) NeonCyan else NeonMagenta
  val winnerName = if (isP1Winner) "PLAYER 1" else if (uiState.mode == GameMode.SOLO_VS_AI) "CYBER BOT" else "PLAYER 2"

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(ArenaDark.copy(alpha = 0.92f))
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = ArenaSurface),
      border = androidx.compose.foundation.BorderStroke(2.dp, winnerColor),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("game_over_card")
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "VICTORY!",
          fontSize = 34.sp,
          fontWeight = FontWeight.Black,
          color = winnerColor,
          letterSpacing = 2.sp
        )
        Text(
          text = "$winnerName CLAIMS THE MATCH",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )

        Spacer(modifier = Modifier.height(18.dp))

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = ArenaSurfaceVariant,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text("PLAYER 1", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              Text("${uiState.p1Goals}", color = TextPrimary, fontSize = 34.sp, fontWeight = FontWeight.Black)
              Text("${uiState.p1PinballScore} pts", color = NeonGold, fontSize = 11.sp)
            }

            Text("VS", color = TextMuted, fontSize = 16.sp, fontWeight = FontWeight.Black)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                if (uiState.mode == GameMode.SOLO_VS_AI) "CYBER BOT" else "PLAYER 2",
                color = NeonMagenta,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
              Text("${uiState.p2Goals}", color = TextPrimary, fontSize = 34.sp, fontWeight = FontWeight.Black)
              Text("${uiState.p2PinballScore} pts", color = NeonGold, fontSize = 11.sp)
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = onRematch,
          colors = ButtonDefaults.buttonColors(containerColor = winnerColor, contentColor = ArenaDark),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("rematch_button")
        ) {
          Text("PLAY AGAIN", fontWeight = FontWeight.Black, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
          onClick = onMenu,
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
          border = androidx.compose.foundation.BorderStroke(1.dp, ArenaBorder),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("menu_button")
        ) {
          Text("MAIN MENU", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun PauseMenuDrawer(
  uiState: GameUiState,
  onResume: () -> Unit,
  onRestart: () -> Unit,
  onOpenSettings: () -> Unit,
  onHome: () -> Unit,
  onToggleSound: () -> Unit,
  onToggleHaptics: () -> Unit,
  onTogglePerspective: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.65f))
      .clickable(onClick = onResume),
    contentAlignment = Alignment.CenterEnd
  ) {
    Surface(
      shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
      color = ArenaSurface,
      border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.5f)),
      modifier = Modifier
        .widthIn(min = 280.dp, max = 340.dp)
        .padding(vertical = 36.dp)
        .clickable(enabled = false) {}
        .testTag("pause_menu_drawer")
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Header with close button
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "MATCH PAUSED",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary
          )
          IconButton(
            onClick = onResume,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Current Arena Name Display
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = ArenaSurfaceVariant,
          border = androidx.compose.foundation.BorderStroke(1.dp, NeonGold.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "CURRENT ARENA",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = TextMuted,
              letterSpacing = 1.sp
            )
            Text(
              text = uiState.mapLayout.title.uppercase(),
              fontSize = 15.sp,
              fontWeight = FontWeight.Black,
              color = NeonGold
            )
            Text(
              text = uiState.mapLayout.subtitle,
              fontSize = 10.sp,
              color = TextSecondary
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Resume Button
        Button(
          onClick = onResume,
          colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ArenaDark),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag("resume_button")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("RESUME MATCH", fontWeight = FontWeight.Black)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Restart Round Button
        OutlinedButton(
          onClick = onRestart,
          colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGold),
          border = androidx.compose.foundation.BorderStroke(1.dp, NeonGold.copy(alpha = 0.5f)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("restart_button")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("RESTART ROUND", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Perspective Quick Toggle Button (Flat vs 3D)
        OutlinedButton(
          onClick = onTogglePerspective,
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (uiState.flatPerspective) NeonGreen else NeonCyan,
            containerColor = if (uiState.flatPerspective) NeonGreen.copy(alpha = 0.12f) else NeonCyan.copy(alpha = 0.12f)
          ),
          border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (uiState.flatPerspective) NeonGreen.copy(alpha = 0.6f) else NeonCyan.copy(alpha = 0.6f)
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("pause_perspective_button")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SportsHockey, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (uiState.flatPerspective) "VIEW: FLAT 2D (FAIR 2P)" else "VIEW: 3D PERSPECTIVE",
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Arena Settings Button
        OutlinedButton(
          onClick = onOpenSettings,
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
          border = androidx.compose.foundation.BorderStroke(1.dp, ArenaBorder),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("pause_settings_button")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("SETTINGS & MAPS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Home Button
        OutlinedButton(
          onClick = onHome,
          colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonMagenta),
          border = androidx.compose.foundation.BorderStroke(1.dp, NeonMagenta.copy(alpha = 0.5f)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("pause_home_button")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("MAIN MENU", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Audio & Vibration quick toggles
        Row(
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = onToggleSound,
            modifier = Modifier.background(ArenaSurfaceVariant, CircleShape).size(38.dp)
          ) {
            Icon(
              imageVector = if (uiState.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
              contentDescription = "Toggle Audio",
              tint = if (uiState.soundEnabled) NeonCyan else TextMuted,
              modifier = Modifier.size(18.dp)
            )
          }

          IconButton(
            onClick = onToggleHaptics,
            modifier = Modifier.background(ArenaSurfaceVariant, CircleShape).size(38.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Vibration,
              contentDescription = "Toggle Haptics",
              tint = if (uiState.hapticsEnabled) NeonGreen else TextMuted,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }
  }
}

package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.ai.BotController
import com.example.ai.BotDifficulty
import com.example.audio.ArcadeSoundPlayer
import com.example.haptics.HapticManager
import com.example.physics.FloatingScore
import com.example.physics.GameEvent
import com.example.physics.MapLayout
import com.example.physics.Particle
import com.example.physics.PhysicsEngine
import com.example.physics.Vector2D
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class GameMode(val label: String) {
  TWO_PLAYER("2P Head-to-Head"),
  SOLO_VS_AI("Solo vs AI")
}

enum class MatchStatus {
  MENU,
  PLAYING,
  GOAL_CELEBRATION,
  PAUSED,
  SETTINGS,
  GAME_OVER
}

data class GameUiState(
  val mode: GameMode = GameMode.TWO_PLAYER,
  val mapLayout: MapLayout = MapLayout.PURE_HOCKEY,
  val status: MatchStatus = MatchStatus.MENU,
  val p1Goals: Int = 0,
  val p2Goals: Int = 0,
  val p1PinballScore: Int = 0,
  val p2PinballScore: Int = 0,
  val p1Combo: Int = 0,
  val p2Combo: Int = 0,
  val targetGoals: Int = 5,
  val botDifficulty: BotDifficulty = BotDifficulty.MEDIUM,
  val soundEnabled: Boolean = true,
  val hapticsEnabled: Boolean = true,
  val flatPerspective: Boolean = true,
  val lastScorer: Int = 0,
  val celebrationCountdown: Float = 0f,
  val winnerPlayer: Int = 0,
  val maxPuckSpeedRecorded: Float = 0f
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
  val physics = PhysicsEngine()
  val soundPlayer = ArcadeSoundPlayer(application)
  val haptics = HapticManager(application)
  val bot = BotController()

  private val _uiState = MutableStateFlow(GameUiState())
  val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

  val particles = mutableListOf<Particle>()
  val floatingScores = mutableListOf<FloatingScore>()

  var screenShakeX = 0f
  var screenShakeY = 0f
  private var shakeIntensity = 0f
  private var maxSpeedRecorded = 0f

  init {
    bot.difficulty = _uiState.value.botDifficulty
    physics.setupArenaLayout(_uiState.value.mapLayout)
    physics.resetPuck(servingPlayer = 1)
    physics.resetStrikers()
    bot.reset(physics)
  }

  fun startMatch(mode: GameMode = _uiState.value.mode, layout: MapLayout = _uiState.value.mapLayout) {
    maxSpeedRecorded = 0f
    physics.setupArenaLayout(layout)
    physics.resetPuck(servingPlayer = 1)
    physics.resetStrikers()
    bot.reset(physics)
    particles.clear()
    floatingScores.clear()

    val shouldBeFlat = (mode == GameMode.TWO_PLAYER)
    _uiState.value = _uiState.value.copy(
      mode = mode,
      mapLayout = layout,
      flatPerspective = shouldBeFlat,
      status = MatchStatus.PLAYING,
      p1Goals = 0,
      p2Goals = 0,
      p1PinballScore = 0,
      p2PinballScore = 0,
      p1Combo = 0,
      p2Combo = 0,
      lastScorer = 0,
      winnerPlayer = 0,
      maxPuckSpeedRecorded = 0f,
      celebrationCountdown = 0f
    )
  }

  fun resetMatch(
    mode: GameMode = _uiState.value.mode,
    layout: MapLayout = _uiState.value.mapLayout
  ) {
    startMatch(mode, layout)
  }

  fun setMapLayout(layout: MapLayout) {
    physics.setupArenaLayout(layout)
    _uiState.value = _uiState.value.copy(mapLayout = layout)
    physics.resetPuck(servingPlayer = 1)
    physics.resetStrikers()
    bot.reset(physics)
  }

  fun setGameMode(mode: GameMode) {
    val shouldBeFlat = (mode == GameMode.TWO_PLAYER)
    _uiState.value = _uiState.value.copy(
      mode = mode,
      flatPerspective = shouldBeFlat
    )
  }

  fun togglePerspective() {
    _uiState.value = _uiState.value.copy(flatPerspective = !_uiState.value.flatPerspective)
  }

  fun setFlatPerspective(flat: Boolean) {
    _uiState.value = _uiState.value.copy(flatPerspective = flat)
  }

  fun returnToMenu() {
    _uiState.value = _uiState.value.copy(status = MatchStatus.MENU)
  }

  fun openSettings() {
    if (_uiState.value.status == MatchStatus.PLAYING) {
      _uiState.value = _uiState.value.copy(status = MatchStatus.SETTINGS)
    }
  }

  fun closeSettings() {
    if (_uiState.value.status == MatchStatus.SETTINGS) {
      _uiState.value = _uiState.value.copy(status = MatchStatus.PLAYING)
    }
  }

  fun pauseGame() {
    if (_uiState.value.status == MatchStatus.PLAYING) {
      _uiState.value = _uiState.value.copy(status = MatchStatus.PAUSED)
    }
  }

  fun resumeGame() {
    if (_uiState.value.status == MatchStatus.PAUSED || _uiState.value.status == MatchStatus.SETTINGS) {
      _uiState.value = _uiState.value.copy(status = MatchStatus.PLAYING)
    }
  }

  fun setTargetGoals(goals: Int) {
    _uiState.value = _uiState.value.copy(targetGoals = goals)
  }

  fun setBotDifficulty(difficulty: BotDifficulty) {
    bot.difficulty = difficulty
    _uiState.value = _uiState.value.copy(botDifficulty = difficulty)
  }

  fun toggleSound() {
    val newState = !_uiState.value.soundEnabled
    soundPlayer.isSoundEnabled = newState
    _uiState.value = _uiState.value.copy(soundEnabled = newState)
  }

  fun toggleHaptics() {
    val newState = !_uiState.value.hapticsEnabled
    haptics.isHapticsEnabled = newState
    _uiState.value = _uiState.value.copy(hapticsEnabled = newState)
  }

  fun onPlayerTouch(player: Int, virtualPos: Vector2D) {
    if (_uiState.value.status == MatchStatus.PLAYING || _uiState.value.status == MatchStatus.GOAL_CELEBRATION) {
      if (player == 1) {
        physics.setStrikerTarget(1, virtualPos)
      } else if (player == 2 && _uiState.value.mode == GameMode.TWO_PLAYER) {
        physics.setStrikerTarget(2, virtualPos)
      }
    }
  }

  fun updateGame(dt: Float) {
    val currentState = _uiState.value
    // Immediately stop game simulation if paused, in settings, at menu, or if match has ended (GAME_OVER)
    if (currentState.status != MatchStatus.PLAYING && currentState.status != MatchStatus.GOAL_CELEBRATION) {
      return
    }

    // Screen Shake decay
    if (shakeIntensity > 0f) {
      screenShakeX = (Random.nextFloat() - 0.5f) * shakeIntensity * 14f
      screenShakeY = (Random.nextFloat() - 0.5f) * shakeIntensity * 14f
      shakeIntensity = (shakeIntensity - dt * 4.5f).coerceAtLeast(0f)
    } else {
      screenShakeX = 0f
      screenShakeY = 0f
    }

    // Update Particles
    val pIter = particles.iterator()
    while (pIter.hasNext()) {
      val p = pIter.next()
      p.position += p.velocity * dt
      p.life -= dt
      if (p.life <= 0f) {
        pIter.remove()
      }
    }

    // Update Floating Scores
    val fsIter = floatingScores.iterator()
    while (fsIter.hasNext()) {
      val fs = fsIter.next()
      fs.position += fs.velocity * dt
      fs.life -= dt
      if (fs.life <= 0f) {
        fsIter.remove()
      }
    }

    // If celebrating a goal
    if (currentState.status == MatchStatus.GOAL_CELEBRATION) {
      val newCountdown = currentState.celebrationCountdown - dt
      if (newCountdown <= 0f) {
        // Reset puck and resume
        val nextServer = if (currentState.lastScorer == 1) 2 else 1
        physics.resetPuck(servingPlayer = nextServer)
        physics.resetStrikers()
        bot.reset(physics)
        _uiState.value = currentState.copy(
          status = MatchStatus.PLAYING,
          celebrationCountdown = 0f
        )
      } else {
        _uiState.value = currentState.copy(celebrationCountdown = newCountdown)
      }
      return
    }

    // AI Bot Update in Solo Mode
    if (currentState.mode == GameMode.SOLO_VS_AI) {
      bot.update(dt, physics)
    }

    // Physics Simulation Step
    val events = mutableListOf<GameEvent>()
    physics.update(dt, events)

    // Track max puck speed without triggering recomposition
    val curSpeed = physics.puck.velocity.length()
    if (curSpeed > maxSpeedRecorded) {
      maxSpeedRecorded = curSpeed
    }

    // Process Simulation Events
    for (event in events) {
      when (event) {
        is GameEvent.PuckStrikerHit -> {
          soundPlayer.playStrikerHit(event.intensity)
          haptics.vibrateLight()
          spawnImpactSparks(event.position, if (event.strikerPlayer == 1) NeonCyan else NeonMagenta, 6)
        }
        is GameEvent.PuckWallHit -> {
          soundPlayer.playWallHit()
          haptics.vibrateLight()
        }
        is GameEvent.PuckBumperHit -> {
          soundPlayer.playBumperHit()
          haptics.vibrateMedium()
          triggerShake(0.6f)
          awardPinballPoints(event.scoredByPlayer, event.bumper.points)
          spawnImpactSparks(event.position, NeonGreen, 12)
          floatingScores.add(FloatingScore("+${event.bumper.points}", event.position, color = NeonGreen))
        }
        is GameEvent.PuckSlingshotHit -> {
          soundPlayer.playSlingshotHit()
          haptics.vibrateMedium()
          triggerShake(0.4f)
          awardPinballPoints(event.scoredByPlayer, event.slingshot.points)
          spawnImpactSparks(event.position, NeonGreen, 8)
          floatingScores.add(FloatingScore("+${event.slingshot.points}", event.position, color = NeonGreen))
        }
        is GameEvent.PuckTargetHit -> {
          soundPlayer.playTargetHit()
          haptics.vibrateMedium()
          awardPinballPoints(event.scoredByPlayer, event.target.points)
          spawnImpactSparks(event.position, NeonGold, 10)
          floatingScores.add(FloatingScore("+${event.target.points}", event.position, color = NeonGold))
        }
        is GameEvent.GoalScored -> {
          handleGoalScored(event.scoringPlayer)
          return
        }
      }
    }
  }

  private fun awardPinballPoints(player: Int, points: Int) {
    if (player == 1) {
      val newScore = _uiState.value.p1PinballScore + points
      val newCombo = _uiState.value.p1Combo + 1
      _uiState.value = _uiState.value.copy(p1PinballScore = newScore, p1Combo = newCombo)
    } else {
      val newScore = _uiState.value.p2PinballScore + points
      val newCombo = _uiState.value.p2Combo + 1
      _uiState.value = _uiState.value.copy(p2PinballScore = newScore, p2Combo = newCombo)
    }
  }

  private fun handleGoalScored(scorer: Int) {
    soundPlayer.playGoal()
    haptics.vibrateGoal()
    triggerShake(1.2f)

    val newP1Goals = if (scorer == 1) _uiState.value.p1Goals + 1 else _uiState.value.p1Goals
    val newP2Goals = if (scorer == 2) _uiState.value.p2Goals + 1 else _uiState.value.p2Goals

    // Goal firework explosion
    val goalPos = if (scorer == 1) Vector2D(physics.tableWidth / 2f, 20f) else Vector2D(physics.tableWidth / 2f, physics.tableHeight - 20f)
    val color = if (scorer == 1) NeonCyan else NeonMagenta
    spawnGoalFireworks(goalPos, color, 35)

    if (newP1Goals >= _uiState.value.targetGoals || newP2Goals >= _uiState.value.targetGoals) {
      // Game Over
      val winner = if (newP1Goals >= _uiState.value.targetGoals) 1 else 2
      _uiState.value = _uiState.value.copy(
        p1Goals = newP1Goals,
        p2Goals = newP2Goals,
        status = MatchStatus.GAME_OVER,
        winnerPlayer = winner,
        lastScorer = scorer,
        maxPuckSpeedRecorded = maxSpeedRecorded
      )
    } else {
      // Goal celebration countdown (1.8s)
      _uiState.value = _uiState.value.copy(
        p1Goals = newP1Goals,
        p2Goals = newP2Goals,
        status = MatchStatus.GOAL_CELEBRATION,
        lastScorer = scorer,
        celebrationCountdown = 1.8f
      )
    }
  }

  private fun triggerShake(intensity: Float) {
    shakeIntensity = intensity.coerceAtLeast(shakeIntensity)
  }

  private fun spawnImpactSparks(pos: Vector2D, color: androidx.compose.ui.graphics.Color, count: Int) {
    for (i in 0 until count) {
      val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
      val speed = Random.nextFloat() * 320f + 120f
      val vel = Vector2D(cos(angle) * speed, sin(angle) * speed)
      particles.add(
        Particle(
          position = pos,
          velocity = vel,
          color = color,
          life = Random.nextFloat() * 0.28f + 0.15f,
          maxLife = 0.4f,
          initialRadius = Random.nextFloat() * 4.5f + 2.5f
        )
      )
    }
  }

  private fun spawnGoalFireworks(pos: Vector2D, color: androidx.compose.ui.graphics.Color, count: Int) {
    for (i in 0 until count) {
      val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
      val speed = Random.nextFloat() * 550f + 150f
      val vel = Vector2D(cos(angle) * speed, sin(angle) * speed)
      particles.add(
        Particle(
          position = pos,
          velocity = vel,
          color = if (Random.nextBoolean()) color else NeonGold,
          life = Random.nextFloat() * 0.6f + 0.4f,
          maxLife = 1.0f,
          initialRadius = Random.nextFloat() * 6f + 3f
        )
      )
    }
  }
}

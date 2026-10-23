package com.example.physics

import androidx.compose.ui.graphics.Color

data class Puck(
  var position: Vector2D = Vector2D(300f, 500f),
  var velocity: Vector2D = Vector2D.ZERO,
  val radius: Float = 22f,
  val mass: Float = 1.0f,
  var lastHitByPlayer: Int = 1,
  val trail: MutableList<Vector2D> = mutableListOf()
) {
  fun addTrailPoint() {
    trail.add(0, position)
    if (trail.size > 14) {
      trail.removeAt(trail.size - 1)
    }
  }

  fun clearTrail() {
    trail.clear()
  }
}

data class Striker(
  val player: Int, // 1 = Bottom (Cyan), 2 = Top (Magenta)
  var position: Vector2D,
  var prevPosition: Vector2D = position,
  var targetPosition: Vector2D = position,
  var velocity: Vector2D = Vector2D.ZERO,
  val radius: Float = 36f,
  val mass: Float = 5.0f
)

data class PinballBumper(
  val id: Int,
  val position: Vector2D,
  val radius: Float = 32f,
  val impulsePower: Float = 620f,
  val points: Int = 100,
  var flashProgress: Float = 0f, // 1f on hit, fades to 0f
  var ringExpansion: Float = 0f
)

data class Slingshot(
  val id: Int,
  val p1: Vector2D,
  val p2: Vector2D,
  val normal: Vector2D, // points into the playfield
  val boostMultiplier: Float = 1.35f,
  val points: Int = 50,
  var flashProgress: Float = 0f
)

data class ScoreTarget(
  val id: Int,
  val position: Vector2D,
  val radius: Float = 16f,
  val points: Int = 250,
  var isLit: Boolean = false,
  var flashProgress: Float = 0f
)

data class Particle(
  var position: Vector2D,
  var velocity: Vector2D,
  val color: Color,
  var life: Float, // 1f down to 0f
  val maxLife: Float,
  val initialRadius: Float
) {
  val currentRadius: Float get() = initialRadius * (life / maxLife)
}

data class FloatingScore(
  val text: String,
  var position: Vector2D,
  var velocity: Vector2D = Vector2D(0f, -60f),
  val color: Color,
  var life: Float = 1.0f,
  val maxLife: Float = 1.0f
)

sealed class GameEvent {
  data class PuckWallHit(val position: Vector2D, val intensity: Float) : GameEvent()
  data class PuckStrikerHit(val strikerPlayer: Int, val position: Vector2D, val intensity: Float) : GameEvent()
  data class PuckBumperHit(val bumper: PinballBumper, val position: Vector2D, val scoredByPlayer: Int) : GameEvent()
  data class PuckSlingshotHit(val slingshot: Slingshot, val position: Vector2D, val scoredByPlayer: Int) : GameEvent()
  data class PuckTargetHit(val target: ScoreTarget, val position: Vector2D, val scoredByPlayer: Int) : GameEvent()
  data class GoalScored(val scoringPlayer: Int, val puckSpeed: Float) : GameEvent()
}

enum class MapLayout(val title: String, val subtitle: String) {
  PURE_HOCKEY("Pure Air Hockey", "Classic open rink, pure skill & physics"),
  FRENZY("Pinball Frenzy", "5 Kinetic Bumpers, 4 Slingshots"),
  CLASSIC("Classic Duel", "Solo Power Bumper & Bank Rails"),
  TWIN_CORES("Twin Cores", "Dual Power Cores with Goal Deflectors"),
  SLINGSHOT_MAZE("Slingshot Maze", "6 Kinetic Rebound Rails & Targets")
}

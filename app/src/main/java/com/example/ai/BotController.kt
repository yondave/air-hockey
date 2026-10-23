package com.example.ai

import com.example.physics.PhysicsEngine
import com.example.physics.Vector2D
import kotlin.math.min

enum class BotDifficulty(val label: String, val speed: Float, val reactionDelay: Float) {
  EASY("Rookie AI", 750f, 0.12f),
  MEDIUM("Cyber Pro", 1250f, 0.05f),
  HARD("Master Core", 1850f, 0.01f)
}

class BotController(
  var difficulty: BotDifficulty = BotDifficulty.MEDIUM
) {
  private var targetPos = Vector2D(300f, 160f)
  private var timer = 0f

  fun reset(physics: PhysicsEngine) {
    targetPos = Vector2D(physics.tableWidth / 2f, 160f)
    timer = 0f
  }

  fun update(dt: Float, physics: PhysicsEngine) {
    timer += dt
    val puck = physics.puck
    val striker2 = physics.striker2

    // Desired defensive home position in front of Bot's goal (y = 0)
    val homeY = 150f
    val homeX = (physics.tableWidth / 2f + (puck.position.x - physics.tableWidth / 2f) * 0.6f)
      .coerceIn(striker2.radius + 20f, physics.tableWidth - striker2.radius - 20f)

    if (puck.position.y > physics.tableHeight * 0.50f || puck.velocity.y > 80f) {
      // Puck is in Player 1's territory OR already flying towards Player 1:
      // Hold solid defensive stance guarding the top goal
      targetPos = Vector2D(homeX, homeY)
    } else {
      // Puck is in Bot's half and needs to be addressed!
      if (puck.position.y > striker2.position.y + 12f) {
        // Puck is safely in FRONT of the bot (between striker2 and Player 1):
        // Attack down towards Player 1's goal!
        // To avoid hitting center bumpers dead-on on kickoff, angle shot towards left or right goal corner
        val targetGoalX = if (puck.position.x < physics.tableWidth / 2f) {
          physics.tableWidth * 0.32f
        } else {
          physics.tableWidth * 0.68f
        }
        val goalTarget = Vector2D(targetGoalX, physics.tableHeight)
        val aimDir = (goalTarget - puck.position).normalized()

        val attackLead = when (difficulty) {
          BotDifficulty.EASY -> 0f
          BotDifficulty.MEDIUM -> 10f
          BotDifficulty.HARD -> 20f
        }

        // IMPORTANT: Never allow targetPos.y to cross below the puck into player 1's territory
        // Clamped so striker strikes through puck but never gets trapped south of the puck!
        val strikeY = (puck.position.y + aimDir.y * attackLead).coerceAtMost(physics.tableHeight * 0.44f)
        val strikeX = (puck.position.x + aimDir.x * attackLead).coerceIn(
          striker2.radius + 15f,
          physics.tableWidth - striker2.radius - 15f
        )
        targetPos = Vector2D(strikeX, strikeY)
      } else {
        // Puck is level with or BEHIND the bot (dangerously close to bot's goal):
        // CRITICAL: NEVER drive straight up towards y = 100f because that smacks the puck into our own goal!
        // Instead, steer WIDE around the puck to the flank wall, then retreat to defensive guard!
        val flankX = if (puck.position.x < physics.tableWidth / 2f) {
          (physics.tableWidth - striker2.radius - 25f) // skirt via right wall
        } else {
          (striker2.radius + 25f) // skirt via left wall
        }

        if (kotlin.math.abs(puck.position.x - striker2.position.x) < striker2.radius + puck.radius + 30f) {
          // Puck is directly in our vertical path: move horizontally away from puck to clear the lane
          targetPos = Vector2D(flankX, striker2.position.y)
        } else {
          // Lane is clear: safely return to home position
          targetPos = Vector2D(flankX, homeY)
        }
      }
    }

    // Move smoothly towards target based on difficulty speed
    val delta = targetPos - striker2.targetPosition
    val dist = delta.length()
    if (dist > 1f) {
      val step = min(dist, difficulty.speed * dt)
      val newPos = striker2.targetPosition + delta.normalized() * step
      physics.setStrikerTarget(2, newPos)
    }
  }
}

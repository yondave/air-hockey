package com.example

import com.example.physics.GameEvent
import com.example.physics.PhysicsEngine
import com.example.physics.Vector2D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhysicsEngineTest {

  @Test
  fun testVectorMath() {
    val v1 = Vector2D(3f, 4f)
    assertEquals(5f, v1.length(), 0.001f)
    assertEquals(25f, v1.lengthSquared(), 0.001f)

    val normalized = v1.normalized()
    assertEquals(0.6f, normalized.x, 0.001f)
    assertEquals(0.8f, normalized.y, 0.001f)

    val v2 = Vector2D(1f, 0f)
    assertEquals(3f, v1.dot(v2), 0.001f)
  }

  @Test
  fun testBumperCollisionBoost() {
    val engine = PhysicsEngine()
    val bumper = engine.bumpers.first()

    // Position puck directly next to bumper moving into it
    engine.puck.position = bumper.position + Vector2D(bumper.radius + engine.puck.radius - 2f, 0f)
    engine.puck.velocity = Vector2D(-300f, 0f)

    val events = mutableListOf<GameEvent>()
    engine.update(0.016f, events)

    // Verify bumper hit was registered and puck was propelled away positively along X
    assertTrue(events.any { it is GameEvent.PuckBumperHit })
    assertTrue(engine.puck.velocity.x > 0f)
    assertTrue(engine.puck.velocity.length() >= bumper.impulsePower * 0.9f)
  }

  @Test
  fun testStrikerMomentumTransfer() {
    val engine = PhysicsEngine()
    // Position puck in an open lane away from center bumper (x = 100f, y = 700f)
    engine.puck.position = Vector2D(100f, 700f)
    engine.puck.velocity = Vector2D.ZERO

    // Position striker 1 just below puck and strike upward
    val puckPos = engine.puck.position
    engine.striker1.position = puckPos + Vector2D(0f, engine.striker1.radius + engine.puck.radius - 2f)
    engine.striker1.targetPosition = puckPos + Vector2D(0f, -50f)

    val events = mutableListOf<GameEvent>()
    engine.update(0.016f, events)

    assertTrue(events.any { it is GameEvent.PuckStrikerHit })
    // Puck should be propelled upward (negative Y velocity)
    assertTrue(engine.puck.velocity.y < -100f)
  }

  @Test
  fun testGoalScored() {
    val engine = PhysicsEngine()
    // Position puck right at top goal mouth moving upward fast
    engine.puck.position = Vector2D(engine.tableWidth / 2f, 5f)
    engine.puck.velocity = Vector2D(0f, -800f)

    val events = mutableListOf<GameEvent>()
    engine.update(0.035f, events)

    assertTrue(events.any { it is GameEvent.GoalScored && it.scoringPlayer == 1 })
  }

  @Test
  fun testPureAirHockeyLayout() {
    val engine = PhysicsEngine()
    engine.setupArenaLayout(com.example.physics.MapLayout.PURE_HOCKEY)

    // Pure air hockey must have zero pinball obstacles
    assertTrue(engine.bumpers.isEmpty())
    assertTrue(engine.slingshots.isEmpty())
    assertTrue(engine.scoreTargets.isEmpty())
  }

  @Test
  fun testRealTimeStrikerInstantTracking() {
    val engine = PhysicsEngine()
    val startPos = engine.striker1.position
    val targetPos = startPos + Vector2D(50f, -30f)

    engine.setStrikerTarget(1, targetPos)
    val events = mutableListOf<GameEvent>()
    engine.update(0.016f, events)

    // Striker must reach exact target position by end of frame with zero lag
    assertEquals(engine.striker1.targetPosition.x, engine.striker1.position.x, 0.001f)
    assertEquals(engine.striker1.targetPosition.y, engine.striker1.position.y, 0.001f)
  }

  @Test
  fun testKickoffResetSafePosition() {
    val engine = PhysicsEngine()
    engine.resetPuck(servingPlayer = 2)

    // Puck must be in AI half (y < tableHeight / 2)
    assertTrue(engine.puck.position.y < engine.tableHeight * 0.5f)
    // Puck must be offset from dead-center x=300 to avoid direct center bumper rebounds
    assertTrue(kotlin.math.abs(engine.puck.position.x - engine.tableWidth / 2f) > 20f)
  }

  @Test
  fun testBotKickoffStrikeDownward() {
    val engine = PhysicsEngine()
    val bot = com.example.ai.BotController(com.example.ai.BotDifficulty.HARD)

    engine.resetPuck(servingPlayer = 2)
    engine.resetStrikers()
    bot.reset(engine)

    // Run 30 simulation frames (~0.5s) to allow bot to strike kickoff puck
    val events = mutableListOf<GameEvent>()
    var selfGoalDetected = false
    for (i in 0 until 40) {
      bot.update(0.016f, engine)
      engine.update(0.016f, events)
      if (events.any { it is GameEvent.GoalScored && it.scoringPlayer == 1 }) {
        selfGoalDetected = true
        break
      }
    }

    // AI must NOT score into its own top goal on kickoff
    assertTrue("AI should not score on its own goal on kickoff", !selfGoalDetected)
  }
}


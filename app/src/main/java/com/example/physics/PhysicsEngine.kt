package com.example.physics

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class PhysicsEngine(
  val tableWidth: Float = 600f,
  val tableHeight: Float = 1000f,
  val goalWidth: Float = 220f
) {
  val goalLeft: Float = (tableWidth - goalWidth) / 2f
  val goalRight: Float = (tableWidth + goalWidth) / 2f

  val puck = Puck(
    position = Vector2D(tableWidth / 2f, tableHeight / 2f),
    velocity = Vector2D.ZERO,
    radius = 20f,
    mass = 1.0f
  )

  val striker1 = Striker(
    player = 1,
    position = Vector2D(tableWidth / 2f, tableHeight * 0.82f),
    radius = 35f,
    mass = 4.5f
  )

  val striker2 = Striker(
    player = 2,
    position = Vector2D(tableWidth / 2f, tableHeight * 0.18f),
    radius = 35f,
    mass = 4.5f
  )

  val bumpers = mutableListOf<PinballBumper>()
  val slingshots = mutableListOf<Slingshot>()
  val scoreTargets = mutableListOf<ScoreTarget>()

  var currentLayout: MapLayout = MapLayout.FRENZY

  init {
    setupArenaLayout()
  }

  fun setupArenaLayout(layout: MapLayout = currentLayout) {
    currentLayout = layout
    bumpers.clear()
    slingshots.clear()
    scoreTargets.clear()

    when (layout) {
      MapLayout.PURE_HOCKEY -> {
        // Pure classic air hockey: NO pinball bumpers, NO slingshots, NO targets
        // Clean high-speed rink, bank shots, pure striker duel
      }
      MapLayout.FRENZY -> {
        // 5 Kinetic Mushroom Bumpers in strategic tactical layout
        bumpers.add(PinballBumper(1, Vector2D(tableWidth / 2f, tableHeight / 2f), radius = 32f, impulsePower = 640f, points = 100))
        bumpers.add(PinballBumper(2, Vector2D(tableWidth * 0.28f, tableHeight * 0.36f), radius = 28f, impulsePower = 600f, points = 150))
        bumpers.add(PinballBumper(3, Vector2D(tableWidth * 0.72f, tableHeight * 0.36f), radius = 28f, impulsePower = 600f, points = 150))
        bumpers.add(PinballBumper(4, Vector2D(tableWidth * 0.28f, tableHeight * 0.64f), radius = 28f, impulsePower = 600f, points = 150))
        bumpers.add(PinballBumper(5, Vector2D(tableWidth * 0.72f, tableHeight * 0.64f), radius = 28f, impulsePower = 600f, points = 150))

        addStandardSlingshots()
        addStandardTargets()
      }
      MapLayout.CLASSIC -> {
        // 1 Big Center Power Bumper
        bumpers.add(PinballBumper(1, Vector2D(tableWidth / 2f, tableHeight / 2f), radius = 38f, impulsePower = 720f, points = 200))

        // 2 Side Bank Slingshots right around mid-court
        slingshots.add(Slingshot(
          id = 1,
          p1 = Vector2D(12f, tableHeight * 0.44f),
          p2 = Vector2D(55f, tableHeight * 0.56f),
          normal = Vector2D(0.95f, -0.1f).normalized(),
          boostMultiplier = 1.4f,
          points = 75
        ))
        slingshots.add(Slingshot(
          id = 2,
          p1 = Vector2D(tableWidth - 12f, tableHeight * 0.44f),
          p2 = Vector2D(tableWidth - 55f, tableHeight * 0.56f),
          normal = Vector2D(-0.95f, -0.1f).normalized(),
          boostMultiplier = 1.4f,
          points = 75
        ))

        scoreTargets.add(ScoreTarget(1, Vector2D(20f, tableHeight * 0.28f), radius = 16f, points = 300))
        scoreTargets.add(ScoreTarget(2, Vector2D(tableWidth - 20f, tableHeight * 0.28f), radius = 16f, points = 300))
        scoreTargets.add(ScoreTarget(3, Vector2D(20f, tableHeight * 0.72f), radius = 16f, points = 300))
        scoreTargets.add(ScoreTarget(4, Vector2D(tableWidth - 20f, tableHeight * 0.72f), radius = 16f, points = 300))
      }
      MapLayout.TWIN_CORES -> {
        // 2 Center Twin Core Bumpers creating a narrow center slot and fast side lanes
        bumpers.add(PinballBumper(1, Vector2D(tableWidth * 0.33f, tableHeight / 2f), radius = 30f, impulsePower = 650f, points = 120))
        bumpers.add(PinballBumper(2, Vector2D(tableWidth * 0.67f, tableHeight / 2f), radius = 30f, impulsePower = 650f, points = 120))

        // 2 Goalie Deflector Bumpers
        bumpers.add(PinballBumper(3, Vector2D(tableWidth / 2f, tableHeight * 0.26f), radius = 26f, impulsePower = 580f, points = 150))
        bumpers.add(PinballBumper(4, Vector2D(tableWidth / 2f, tableHeight * 0.74f), radius = 26f, impulsePower = 580f, points = 150))

        addStandardSlingshots()
        addStandardTargets()
      }
      MapLayout.SLINGSHOT_MAZE -> {
        // 2 Center Mushroom Bumpers
        bumpers.add(PinballBumper(1, Vector2D(tableWidth * 0.32f, tableHeight * 0.5f), radius = 27f, impulsePower = 620f, points = 100))
        bumpers.add(PinballBumper(2, Vector2D(tableWidth * 0.68f, tableHeight * 0.5f), radius = 27f, impulsePower = 620f, points = 100))

        // 6 Slingshots creating zig-zag pinball rails
        slingshots.add(Slingshot(1, Vector2D(12f, tableHeight * 0.20f), Vector2D(65f, tableHeight * 0.28f), Vector2D(0.85f, -0.52f).normalized(), 1.4f, 60))
        slingshots.add(Slingshot(2, Vector2D(tableWidth - 12f, tableHeight * 0.20f), Vector2D(tableWidth - 65f, tableHeight * 0.28f), Vector2D(-0.85f, -0.52f).normalized(), 1.4f, 60))

        slingshots.add(Slingshot(3, Vector2D(55f, tableHeight * 0.44f), Vector2D(12f, tableHeight * 0.52f), Vector2D(0.9f, 0.4f).normalized(), 1.35f, 50))
        slingshots.add(Slingshot(4, Vector2D(tableWidth - 55f, tableHeight * 0.44f), Vector2D(tableWidth - 12f, tableHeight * 0.52f), Vector2D(-0.9f, 0.4f).normalized(), 1.35f, 50))

        slingshots.add(Slingshot(5, Vector2D(65f, tableHeight * 0.72f), Vector2D(12f, tableHeight * 0.80f), Vector2D(0.85f, 0.52f).normalized(), 1.4f, 60))
        slingshots.add(Slingshot(6, Vector2D(tableWidth - 65f, tableHeight * 0.72f), Vector2D(tableWidth - 12f, tableHeight * 0.80f), Vector2D(-0.85f, 0.52f).normalized(), 1.4f, 60))

        addStandardTargets()
      }
    }
  }

  private fun addStandardSlingshots() {
    slingshots.add(Slingshot(
      id = 1,
      p1 = Vector2D(12f, tableHeight * 0.24f),
      p2 = Vector2D(65f, tableHeight * 0.33f),
      normal = Vector2D(0.85f, -0.52f).normalized(),
      boostMultiplier = 1.35f,
      points = 50
    ))
    slingshots.add(Slingshot(
      id = 2,
      p1 = Vector2D(tableWidth - 12f, tableHeight * 0.24f),
      p2 = Vector2D(tableWidth - 65f, tableHeight * 0.33f),
      normal = Vector2D(-0.85f, -0.52f).normalized(),
      boostMultiplier = 1.35f,
      points = 50
    ))
    slingshots.add(Slingshot(
      id = 3,
      p1 = Vector2D(65f, tableHeight * 0.67f),
      p2 = Vector2D(12f, tableHeight * 0.76f),
      normal = Vector2D(0.85f, 0.52f).normalized(),
      boostMultiplier = 1.35f,
      points = 50
    ))
    slingshots.add(Slingshot(
      id = 4,
      p1 = Vector2D(tableWidth - 65f, tableHeight * 0.67f),
      p2 = Vector2D(tableWidth - 12f, tableHeight * 0.76f),
      normal = Vector2D(-0.85f, 0.52f).normalized(),
      boostMultiplier = 1.35f,
      points = 50
    ))
  }

  private fun addStandardTargets() {
    scoreTargets.add(ScoreTarget(1, Vector2D(20f, tableHeight * 0.45f), radius = 15f, points = 250))
    scoreTargets.add(ScoreTarget(2, Vector2D(tableWidth - 20f, tableHeight * 0.45f), radius = 15f, points = 250))
    scoreTargets.add(ScoreTarget(3, Vector2D(20f, tableHeight * 0.55f), radius = 15f, points = 250))
    scoreTargets.add(ScoreTarget(4, Vector2D(tableWidth - 20f, tableHeight * 0.55f), radius = 15f, points = 250))
  }

  fun resetPuck(servingPlayer: Int = 1) {
    puck.velocity = Vector2D.ZERO
    puck.clearTrail()
    val yPos = if (servingPlayer == 1) tableHeight * 0.62f else tableHeight * 0.38f
    // Offset serve slightly away from center so puck never sits directly in line with center obstacles
    val xOffset = if (servingPlayer == 1) 50f else -50f
    puck.position = Vector2D(tableWidth / 2f + xOffset, yPos)
    puck.lastHitByPlayer = servingPlayer
  }

  fun resetStrikers() {
    striker1.position = Vector2D(tableWidth / 2f, tableHeight * 0.82f)
    striker1.prevPosition = striker1.position
    striker1.targetPosition = striker1.position
    striker1.velocity = Vector2D.ZERO

    striker2.position = Vector2D(tableWidth / 2f, tableHeight * 0.18f)
    striker2.prevPosition = striker2.position
    striker2.targetPosition = striker2.position
    striker2.velocity = Vector2D.ZERO
  }

  fun setStrikerTarget(player: Int, target: Vector2D) {
    if (player == 1) {
      // Clamped to Player 1 zone: bottom half
      val clampedX = target.x.coerceIn(striker1.radius + 6f, tableWidth - striker1.radius - 6f)
      val clampedY = target.y.coerceIn(tableHeight * 0.52f, tableHeight - striker1.radius - 6f)
      striker1.targetPosition = Vector2D(clampedX, clampedY)
    } else {
      // Clamped to Player 2 zone: top half
      val clampedX = target.x.coerceIn(striker2.radius + 6f, tableWidth - striker2.radius - 6f)
      val clampedY = target.y.coerceIn(striker2.radius + 6f, tableHeight * 0.48f)
      striker2.targetPosition = Vector2D(clampedX, clampedY)
    }
  }

  fun update(dt: Float, events: MutableList<GameEvent>) {
    // Fade bumper and obstacle animations
    for (bumper in bumpers) {
      if (bumper.flashProgress > 0f) {
        bumper.flashProgress = max(0f, bumper.flashProgress - dt * 2.8f)
      }
      if (bumper.ringExpansion > 0f) {
        bumper.ringExpansion = max(0f, bumper.ringExpansion - dt * 3.2f)
      }
    }
    for (slingshot in slingshots) {
      if (slingshot.flashProgress > 0f) {
        slingshot.flashProgress = max(0f, slingshot.flashProgress - dt * 2.8f)
      }
    }
    for (target in scoreTargets) {
      if (target.flashProgress > 0f) {
        target.flashProgress = max(0f, target.flashProgress - dt * 2.0f)
      }
    }

    // Sub-stepping for accurate, high-velocity collisions without tunneling
    val subSteps = 4
    val subDt = dt / subSteps

    // Real-time instantaneous striker movement:
    // Distribute movement evenly across substeps so Continuous Collision Detection (CCD) is seamless.
    // Velocity accurately reflects the instantaneous swat/stroke speed.
    val s1Move = (striker1.targetPosition - striker1.position) / subSteps.toFloat()
    val s1Vel = (striker1.targetPosition - striker1.position) / dt.coerceAtLeast(0.0005f)

    val s2Move = (striker2.targetPosition - striker2.position) / subSteps.toFloat()
    val s2Vel = (striker2.targetPosition - striker2.position) / dt.coerceAtLeast(0.0005f)

    for (step in 0 until subSteps) {
      // 1. Advance striker along finger path for this substep
      striker1.prevPosition = striker1.position
      striker1.position += s1Move
      striker1.velocity = s1Vel

      striker2.prevPosition = striker2.position
      striker2.position += s2Move
      striker2.velocity = s2Vel

      // 2. Striker vs Puck collisions
      checkStrikerPuckCollision(striker1, events)
      checkStrikerPuckCollision(striker2, events)

      // 3. Puck vs Pinball Obstacles
      checkPuckBumpers(events)
      checkPuckSlingshots(events)
      checkPuckTargets(events)

      // 4. Puck vs Walls and Goal checking
      checkPuckWallsAndGoals(subDt, events)

      // 5. Integrate puck motion
      puck.position += puck.velocity * subDt

      // Slight air hockey table damping
      puck.velocity = puck.velocity * 0.9992f
    }

    // Lock striker exactly to finger target at end of frame with zero latency
    striker1.position = striker1.targetPosition
    striker2.position = striker2.targetPosition

    // Record trail point periodically
    puck.addTrailPoint()
  }

  private fun checkStrikerPuckCollision(striker: Striker, events: MutableList<GameEvent>) {
    val delta = puck.position - striker.position
    val dist = delta.length()
    val minDist = striker.radius + puck.radius

    if (dist < minDist && dist > 0.0001f) {
      val normal = delta / dist
      val overlap = minDist - dist

      // Positional correction to resolve penetration
      puck.position = puck.position + normal * overlap

      // Relative velocity
      val relVel = puck.velocity - striker.velocity
      val velAlongNormal = relVel.dot(normal)

      // Reflect if bodies are moving towards each other or striker is actively swatting forward
      val forwardDrive = striker.velocity.dot(normal)
      if (velAlongNormal < 0f || forwardDrive > 30f) {
        val restitution = 0.96f
        val effectiveNormalVel = if (velAlongNormal < 0f) velAlongNormal else -forwardDrive
        val impulseMag = -(1f + restitution) * effectiveNormalVel / (1f / puck.mass + 1f / striker.mass)
        val impulse = normal * (impulseMag / puck.mass)

        // Striker batting power boost: impart striker's forward momentum
        val driveBoost = if (forwardDrive > 0f) normal * (forwardDrive * 1.30f) else Vector2D.ZERO

        puck.velocity = puck.velocity + impulse + driveBoost

        // Clamp max puck speed
        puck.velocity = puck.velocity.clampLength(1800f)
        puck.lastHitByPlayer = striker.player

        events.add(GameEvent.PuckStrikerHit(striker.player, puck.position, (puck.velocity.length() / 800f).coerceIn(0.2f, 1.5f)))
      }
    }
  }

  private fun checkPuckBumpers(events: MutableList<GameEvent>) {
    for (bumper in bumpers) {
      val delta = puck.position - bumper.position
      val dist = delta.length()
      val minDist = bumper.radius + puck.radius

      if (dist < minDist && dist > 0.0001f) {
        val normal = delta / dist
        val overlap = minDist - dist

        // Push puck outside bumper immediately
        puck.position = puck.position + normal * (overlap + 2f)

        // Reflect velocity along normal
        val reflected = puck.velocity.reflect(normal)
        val curSpeed = reflected.length()

        // Energetic explosive pinball impulse
        val newSpeed = max(curSpeed * 1.15f, bumper.impulsePower)
        puck.velocity = normal * newSpeed

        bumper.flashProgress = 1.0f
        bumper.ringExpansion = 1.0f

        events.add(GameEvent.PuckBumperHit(bumper, puck.position, puck.lastHitByPlayer))
      }
    }
  }

  private fun checkPuckSlingshots(events: MutableList<GameEvent>) {
    for (slingshot in slingshots) {
      // Point-to-segment distance
      val seg = slingshot.p2 - slingshot.p1
      val segLenSq = seg.lengthSquared()
      if (segLenSq == 0f) continue

      val t = ((puck.position - slingshot.p1).dot(seg) / segLenSq).coerceIn(0f, 1f)
      val closestPoint = slingshot.p1 + seg * t
      val delta = puck.position - closestPoint
      val dist = delta.length()

      if (dist < puck.radius + 6f && dist > 0.0001f) {
        val normal = slingshot.normal
        // Push puck away from slingshot
        puck.position = closestPoint + normal * (puck.radius + 8f)

        // Kick puck with slingshot boost
        val reflected = puck.velocity.reflect(normal)
        val boostedSpeed = max(reflected.length() * slingshot.boostMultiplier, 550f)
        puck.velocity = (normal * 0.7f + reflected.normalized() * 0.3f).normalized() * boostedSpeed

        slingshot.flashProgress = 1.0f
        events.add(GameEvent.PuckSlingshotHit(slingshot, closestPoint, puck.lastHitByPlayer))
      }
    }
  }

  private fun checkPuckTargets(events: MutableList<GameEvent>) {
    for (target in scoreTargets) {
      val dist = (puck.position - target.position).length()
      if (dist < target.radius + puck.radius && target.flashProgress <= 0.1f) {
        target.isLit = !target.isLit
        target.flashProgress = 1.0f
        events.add(GameEvent.PuckTargetHit(target, target.position, puck.lastHitByPlayer))
      }
    }
  }

  private fun checkPuckWallsAndGoals(subDt: Float, events: MutableList<GameEvent>) {
    val r = puck.radius

    // Left Wall
    if (puck.position.x - r < 8f) {
      puck.position = Vector2D(8f + r, puck.position.y)
      if (puck.velocity.x < 0f) {
        puck.velocity = Vector2D(-puck.velocity.x * 0.96f, puck.velocity.y)
        events.add(GameEvent.PuckWallHit(puck.position, puck.velocity.length() / 800f))
      }
    }

    // Right Wall
    if (puck.position.x + r > tableWidth - 8f) {
      puck.position = Vector2D(tableWidth - 8f - r, puck.position.y)
      if (puck.velocity.x > 0f) {
        puck.velocity = Vector2D(-puck.velocity.x * 0.96f, puck.velocity.y)
        events.add(GameEvent.PuckWallHit(puck.position, puck.velocity.length() / 800f))
      }
    }

    // Top Boundary / Goal
    if (puck.position.y - r < 8f) {
      val inGoalMouth = puck.position.x in (goalLeft + 6f)..(goalRight - 6f)
      if (inGoalMouth) {
        // Puck entered top goal!
        if (puck.position.y < -15f) {
          // Goal for Player 1 (Bottom player scored in top goal)
          events.add(GameEvent.GoalScored(1, puck.velocity.length()))
        }
      } else {
        // Wall bounce
        puck.position = Vector2D(puck.position.x, 8f + r)
        if (puck.velocity.y < 0f) {
          puck.velocity = Vector2D(puck.velocity.x, -puck.velocity.y * 0.96f)
          events.add(GameEvent.PuckWallHit(puck.position, puck.velocity.length() / 800f))
        }
      }
    }

    // Bottom Boundary / Goal
    if (puck.position.y + r > tableHeight - 8f) {
      val inGoalMouth = puck.position.x in (goalLeft + 6f)..(goalRight - 6f)
      if (inGoalMouth) {
        // Puck entered bottom goal!
        if (puck.position.y > tableHeight + 15f) {
          // Goal for Player 2 (Top player scored in bottom goal)
          events.add(GameEvent.GoalScored(2, puck.velocity.length()))
        }
      } else {
        // Wall bounce
        puck.position = Vector2D(puck.position.x, tableHeight - 8f - r)
        if (puck.velocity.y > 0f) {
          puck.velocity = Vector2D(puck.velocity.x, -puck.velocity.y * 0.96f)
          events.add(GameEvent.PuckWallHit(puck.position, puck.velocity.length() / 800f))
        }
      }
    }
  }
}

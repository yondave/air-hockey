package com.example.ui.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.physics.FloatingScore
import com.example.physics.Particle
import com.example.physics.PhysicsEngine
import com.example.physics.Vector2D
import com.example.ui.theme.ArenaBorder
import com.example.ui.theme.ArenaDark
import com.example.ui.theme.ArenaGrid
import com.example.ui.theme.ArenaSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanGlow
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonMagentaGlow
import kotlin.math.cos
import kotlin.math.sin

class PerspectiveRenderer(
  val virtualWidth: Float = 600f,
  val virtualHeight: Float = 1000f
) {
  var isFlat: Boolean = false

  // Projection parameters computed per draw call based on available DrawScope size
  var arenaLeft = 0f
    private set
  var arenaTop = 0f
    private set
  var arenaWidth = 0f
    private set
  var arenaHeight = 0f
    private set
  var centerX = 0f
    private set

  private val floorPath = Path()
  private var cachedFloorBrush: Brush? = null
  private var lastCenterX = -1f
  private var lastArenaTop = -1f
  private var lastArenaH = -1f

  fun toScreen(vX: Float, vY: Float, heightOffset: Float = 0f): Offset {
    if (isFlat) {
      val normY = (vY / virtualHeight).coerceIn(0f, 1f)
      val sY = arenaTop + normY * arenaHeight
      val sX = arenaLeft + (vX / virtualWidth) * arenaWidth
      return Offset(sX, sY)
    }
    val normY = (vY / virtualHeight).coerceIn(0f, 1f)
    // Tilted depth foreshortening: top is further away (smaller scale), bottom is closer (larger scale)
    val depthFactor = 1.0f + (1.0f - normY) * 0.42f
    val scale = 1.0f / depthFactor

    val sY = arenaTop + (normY * 0.95f + normY * normY * 0.05f) * arenaHeight - (heightOffset * scale)
    val sX = centerX + (vX - virtualWidth / 2f) * (arenaWidth / virtualWidth) * scale
    return Offset(sX, sY)
  }

  fun toScreen(vec: Vector2D, heightOffset: Float = 0f): Offset = toScreen(vec.x, vec.y, heightOffset)

  fun toVirtual(screenX: Float, screenY: Float): Vector2D {
    if (arenaHeight <= 0.001f || arenaWidth <= 0.001f) {
      return Vector2D(virtualWidth / 2f, virtualHeight * 0.75f)
    }
    if (isFlat) {
      val normY = ((screenY - arenaTop) / arenaHeight).coerceIn(0f, 1f)
      val normX = ((screenX - arenaLeft) / arenaWidth).coerceIn(0f, 1f)
      val vX = normX * virtualWidth
      val vY = normY * virtualHeight
      return Vector2D(vX.coerceIn(0f, virtualWidth), vY.coerceIn(0f, virtualHeight))
    }
    // Inverse transformation for touch input mapping
    val normY = ((screenY - arenaTop) / arenaHeight).coerceIn(0f, 1f)
    val depthFactor = 1.0f + (1.0f - normY) * 0.42f
    val scale = 1.0f / depthFactor
    val safeWidthScale = ((arenaWidth / virtualWidth) * scale).coerceAtLeast(0.0001f)
    val vX = (screenX - centerX) / safeWidthScale + virtualWidth / 2f
    val vY = normY * virtualHeight
    return Vector2D(vX.coerceIn(0f, virtualWidth), vY.coerceIn(0f, virtualHeight))
  }

  fun scaleRadius(vRadius: Float, vY: Float): Float {
    if (isFlat) {
      return vRadius * (arenaWidth / virtualWidth)
    }
    val normY = (vY / virtualHeight).coerceIn(0f, 1f)
    val depthFactor = 1.0f + (1.0f - normY) * 0.42f
    val scale = 1.0f / depthFactor
    return vRadius * (arenaWidth / virtualWidth) * scale
  }

  fun drawArena(
    drawScope: DrawScope,
    physics: PhysicsEngine,
    particles: List<Particle>,
    floatingScores: List<FloatingScore>,
    screenShakeX: Float = 0f,
    screenShakeY: Float = 0f,
    flatPerspective: Boolean = isFlat
  ) {
    this.isFlat = flatPerspective
    val w = drawScope.size.width
    val h = drawScope.size.height

    // Maximize playfield size across whole device screen
    val maxArenaW = w * 0.96f
    val maxArenaH = h * 0.88f
    val aspect = virtualHeight / virtualWidth

    if (maxArenaW * aspect <= maxArenaH) {
      arenaWidth = maxArenaW
      arenaHeight = maxArenaW * aspect
    } else {
      arenaHeight = maxArenaH
      arenaWidth = maxArenaH / aspect
    }
    arenaLeft = (w - arenaWidth) / 2f + screenShakeX
    arenaTop = (h - arenaHeight) / 2f + screenShakeY
    centerX = arenaLeft + arenaWidth / 2f

    with(drawScope) {
      // 1. Draw 3D Tilted Table Base / Floor Polygon
      val pTopLeft = toScreen(0f, 0f)
      val pTopRight = toScreen(virtualWidth, 0f)
      val pBottomRight = toScreen(virtualWidth, virtualHeight)
      val pBottomLeft = toScreen(0f, virtualHeight)

      floorPath.reset()
      floorPath.moveTo(pTopLeft.x, pTopLeft.y)
      floorPath.lineTo(pTopRight.x, pTopRight.y)
      floorPath.lineTo(pBottomRight.x, pBottomRight.y)
      floorPath.lineTo(pBottomLeft.x, pBottomLeft.y)
      floorPath.close()

      if (cachedFloorBrush == null || lastCenterX != centerX || lastArenaTop != arenaTop || lastArenaH != arenaHeight) {
        lastCenterX = centerX
        lastArenaTop = arenaTop
        lastArenaH = arenaHeight
        cachedFloorBrush = Brush.radialGradient(
          colors = listOf(ArenaSurface, ArenaDark, Color(0xFF04050A)),
          center = Offset(centerX, arenaTop + arenaHeight * 0.5f),
          radius = arenaHeight * 0.70f
        )
      }

      drawPath(
        path = floorPath,
        brush = cachedFloorBrush!!
      )

      // 2. Neon Perspective Grid Lines
      // Transverse lines
      val gridSteps = 10
      for (i in 1 until gridSteps) {
        val gy = virtualHeight * (i.toFloat() / gridSteps)
        val left = toScreen(0f, gy)
        val right = toScreen(virtualWidth, gy)
        drawLine(
          color = if (i == 5) NeonCyan.copy(alpha = 0.5f) else ArenaGrid.copy(alpha = 0.45f),
          start = left,
          end = right,
          strokeWidth = if (i == 5) 2.5f else 1.2f
        )
      }

      // Longitudinal lines
      val vCols = 6
      for (i in 1 until vCols) {
        val gx = virtualWidth * (i.toFloat() / vCols)
        val top = toScreen(gx, 0f)
        val bottom = toScreen(gx, virtualHeight)
        drawLine(
          color = ArenaGrid.copy(alpha = 0.45f),
          start = top,
          end = bottom,
          strokeWidth = 1.2f
        )
      }

      // Center Faceoff Circle & Line
      val centerPos = toScreen(virtualWidth / 2f, virtualHeight / 2f)
      val centerRadius = scaleRadius(75f, virtualHeight / 2f)
      drawCircle(
        color = NeonCyanGlow.copy(alpha = 0.15f),
        radius = centerRadius,
        center = centerPos,
        style = Stroke(width = 3f)
      )
      drawCircle(
        color = NeonMagentaGlow.copy(alpha = 0.35f),
        radius = centerRadius * 0.4f,
        center = centerPos,
        style = Stroke(width = 2f)
      )

      // Center Divider Neon Line
      val centerL = toScreen(0f, virtualHeight / 2f)
      val centerR = toScreen(virtualWidth, virtualHeight / 2f)
      drawLine(
        brush = Brush.horizontalGradient(listOf(NeonMagenta, NeonCyan, NeonMagenta)),
        start = centerL,
        end = centerR,
        strokeWidth = 3f
      )

      // Classic Air Hockey Creases (Semicircles in front of each goal)
      val p2Crease = toScreen(virtualWidth / 2f, 0f)
      val p2CreaseR = scaleRadius(95f, 0f)
      drawArc(
        color = NeonMagentaGlow.copy(alpha = 0.25f),
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(p2Crease.x - p2CreaseR, p2Crease.y - p2CreaseR),
        size = Size(p2CreaseR * 2, p2CreaseR * 2),
        style = Stroke(width = 2.5f)
      )

      val p1Crease = toScreen(virtualWidth / 2f, virtualHeight)
      val p1CreaseR = scaleRadius(95f, virtualHeight)
      drawArc(
        color = NeonCyanGlow.copy(alpha = 0.25f),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(p1Crease.x - p1CreaseR, p1Crease.y - p1CreaseR),
        size = Size(p1CreaseR * 2, p1CreaseR * 2),
        style = Stroke(width = 2.5f)
      )

      // 4 Air Hockey Corner Faceoff Spots
      val faceoffSpots = listOf(
        Pair(0.24f, 0.22f),
        Pair(0.76f, 0.22f),
        Pair(0.24f, 0.78f),
        Pair(0.76f, 0.78f)
      )
      for ((fx, fy) in faceoffSpots) {
        val spotPos = toScreen(virtualWidth * fx, virtualHeight * fy)
        val spotR = scaleRadius(16f, virtualHeight * fy)
        val color = if (fy < 0.5f) NeonMagentaGlow else NeonCyanGlow
        drawCircle(
          color = color.copy(alpha = 0.2f),
          radius = spotR,
          center = spotPos,
          style = Stroke(width = 1.8f)
        )
        drawCircle(
          color = color.copy(alpha = 0.45f),
          radius = 3.5f,
          center = spotPos
        )
      }

      // 3. Goal Zones
      val p1GoalL = toScreen(physics.goalLeft, virtualHeight)
      val p1GoalR = toScreen(physics.goalRight, virtualHeight)
      val p2GoalL = toScreen(physics.goalLeft, 0f)
      val p2GoalR = toScreen(physics.goalRight, 0f)

      // Player 1 Goal (Bottom - Cyan)
      drawLine(
        brush = Brush.horizontalGradient(listOf(NeonCyan, Color.White, NeonCyan)),
        start = p1GoalL,
        end = p1GoalR,
        strokeWidth = 7f
      )

      // Player 2 Goal (Top - Magenta)
      drawLine(
        brush = Brush.horizontalGradient(listOf(NeonMagenta, Color.White, NeonMagenta)),
        start = p2GoalL,
        end = p2GoalR,
        strokeWidth = 6f
      )

      // 4. Draw 3D Boundary Rails
      draw3DRails(this, pTopLeft, pTopRight, pBottomRight, pBottomLeft, p1GoalL, p1GoalR, p2GoalL, p2GoalR)

      // 5. Draw Slingshots
      for (slingshot in physics.slingshots) {
        drawSlingshot(this, slingshot)
      }

      // 6. Draw Score Targets
      for (target in physics.scoreTargets) {
        drawScoreTarget(this, target)
      }

      // 7. Draw Pinball Mushroom Bumpers
      for (bumper in physics.bumpers) {
        drawBumper(this, bumper)
      }

      // 8. Draw Puck Trail & 3D Puck
      drawPuck(this, physics.puck)

      // 9. Draw 3D Strikers
      drawStriker(this, physics.striker1, NeonCyan, NeonCyanGlow)
      drawStriker(this, physics.striker2, NeonMagenta, NeonMagentaGlow)

      // 10. Draw Particle Systems (sparks, ring explosions)
      for (p in particles) {
        val pScr = toScreen(p.position)
        val pRad = scaleRadius(p.currentRadius, p.position.y).coerceAtLeast(1.5f)
        drawCircle(
          color = p.color.copy(alpha = (p.life / p.maxLife).coerceIn(0f, 1f)),
          radius = pRad,
          center = pScr
        )
      }

      // 11. Draw Floating Score Popups
      for (fs in floatingScores) {
        val sPos = toScreen(fs.position, heightOffset = 25f)
        val alpha = (fs.life / fs.maxLife).coerceIn(0f, 1f)
        // Score indicator dot / halo
        drawCircle(
          color = fs.color.copy(alpha = alpha * 0.4f),
          radius = 16f,
          center = sPos
        )
      }
    }
  }

  private fun draw3DRails(
    drawScope: DrawScope,
    topLeft: Offset,
    topRight: Offset,
    bottomRight: Offset,
    bottomLeft: Offset,
    p1GoalL: Offset,
    p1GoalR: Offset,
    p2GoalL: Offset,
    p2GoalR: Offset
  ) {
    with(drawScope) {
      val railColor = ArenaBorder
      val railGlow = NeonCyan.copy(alpha = 0.25f)

      // Left Rail
      drawLine(railGlow, topLeft, bottomLeft, strokeWidth = 9f)
      drawLine(railColor, topLeft, bottomLeft, strokeWidth = 4f)

      // Right Rail
      drawLine(railGlow, topRight, bottomRight, strokeWidth = 9f)
      drawLine(railColor, topRight, bottomRight, strokeWidth = 4f)

      // Top Rails (Left of goal & Right of goal)
      drawLine(railColor, topLeft, p2GoalL, strokeWidth = 5f)
      drawLine(railColor, p2GoalR, topRight, strokeWidth = 5f)

      // Bottom Rails (Left of goal & Right of goal)
      drawLine(railColor, bottomLeft, p1GoalL, strokeWidth = 5f)
      drawLine(railColor, p1GoalR, bottomRight, strokeWidth = 5f)
    }
  }

  private fun drawSlingshot(drawScope: DrawScope, slingshot: com.example.physics.Slingshot) {
    with(drawScope) {
      val heightOff = if (isFlat) 0f else 6f
      val p1Scr = toScreen(slingshot.p1, heightOffset = heightOff)
      val p2Scr = toScreen(slingshot.p2, heightOffset = heightOff)
      val isHit = slingshot.flashProgress > 0f

      val lineColor = if (isHit) Color.White else NeonGreen
      val glowColor = if (isHit) NeonGreen.copy(alpha = 0.9f) else NeonGreen.copy(alpha = 0.35f)

      // Glowing spring rail
      drawLine(glowColor, p1Scr, p2Scr, strokeWidth = if (isHit) 12f else 7f)
      drawLine(lineColor, p1Scr, p2Scr, strokeWidth = if (isHit) 5f else 3f)

      // End Posts
      val r1 = scaleRadius(6f, slingshot.p1.y)
      val r2 = scaleRadius(6f, slingshot.p2.y)
      drawCircle(Color(0xFFE0E0E0), radius = r1, center = p1Scr)
      drawCircle(Color(0xFFE0E0E0), radius = r2, center = p2Scr)
    }
  }

  private fun drawScoreTarget(drawScope: DrawScope, target: com.example.physics.ScoreTarget) {
    with(drawScope) {
      val pos = toScreen(target.position, heightOffset = if (isFlat) 0f else 4f)
      val r = scaleRadius(target.radius, target.position.y)
      val isLit = target.isLit || target.flashProgress > 0f
      val color = if (isLit) NeonGold else Color(0xFF6B5800)

      if (isLit) {
        drawCircle(color.copy(alpha = 0.4f), radius = r * 1.8f, center = pos)
      }
      drawCircle(color, radius = r, center = pos)
      drawCircle(Color.White, radius = r * 0.45f, center = pos)
    }
  }

  private fun drawBumper(drawScope: DrawScope, bumper: com.example.physics.PinballBumper) {
    with(drawScope) {
      val basePos = toScreen(bumper.position, heightOffset = 0f)
      val topPos = if (isFlat) basePos else toScreen(bumper.position, heightOffset = 14f)
      val rBase = scaleRadius(bumper.radius, bumper.position.y)
      val rTop = scaleRadius(bumper.radius * 0.95f, bumper.position.y)
      val isHit = bumper.flashProgress > 0f

      // Expanding kinetic shockwave ring on bumper hit
      if (bumper.ringExpansion > 0f) {
        val ringR = rBase * (1.0f + (1f - bumper.ringExpansion) * 1.5f)
        val alpha = bumper.ringExpansion.coerceIn(0f, 1f)
        drawCircle(
          color = NeonGreen.copy(alpha = alpha * 0.7f),
          radius = ringR,
          center = basePos,
          style = Stroke(width = 4f * alpha)
        )
      }

      // Base shadow
      drawCircle(
        color = Color.Black.copy(alpha = 0.5f),
        radius = rBase * 1.15f,
        center = basePos
      )

      // Cylinder Body
      val sideColor = if (isHit) Color(0xFF00C853) else Color(0xFF004D25)
      drawCircle(
        color = sideColor,
        radius = rBase,
        center = basePos
      )

      // Raised Bumper Mushroom Cap
      val capColor = if (isHit) Color.White else NeonGreen
      val innerGlow = if (isHit) NeonGreen else Color(0xFFB9F6CA)
      drawCircle(
        color = capColor,
        radius = rTop,
        center = topPos
      )
      drawCircle(
        color = innerGlow,
        radius = rTop * 0.65f,
        center = topPos
      )
      drawCircle(
        color = if (isHit) NeonGold else NeonCyan,
        radius = rTop * 0.35f,
        center = topPos
      )
      // Metallic outer rim
      drawCircle(
        color = Color(0xFFE0E0E0),
        radius = rTop,
        center = topPos,
        style = Stroke(width = 2.5f)
      )
    }
  }

  private fun drawPuck(drawScope: DrawScope, puck: com.example.physics.Puck) {
    with(drawScope) {
      // 1. Draw glowing wake trail
      if (puck.trail.isNotEmpty()) {
        for (i in 0 until puck.trail.size - 1) {
          val p1 = toScreen(puck.trail[i])
          val p2 = toScreen(puck.trail[i + 1])
          val alpha = (1.0f - (i.toFloat() / puck.trail.size)).coerceIn(0f, 1f) * 0.4f
          val stroke = scaleRadius(puck.radius * (1f - i.toFloat() / puck.trail.size), puck.trail[i].y)
          drawLine(
            color = NeonGold.copy(alpha = alpha),
            start = p1,
            end = p2,
            strokeWidth = stroke
          )
        }
      }

      val r = scaleRadius(puck.radius, puck.position.y)

      if (isFlat) {
        val center = toScreen(puck.position)
        // Concentric drop shadow
        drawCircle(
          color = Color.Black.copy(alpha = 0.45f),
          radius = r * 1.10f,
          center = center
        )
        // Outer rim
        drawCircle(
          color = Color(0xFFCC7A00),
          radius = r,
          center = center
        )
        // Radiant halo
        drawCircle(
          color = NeonGold.copy(alpha = 0.40f),
          radius = r * 1.45f,
          center = center
        )
        // Puck Face
        drawCircle(
          color = NeonGold,
          radius = r * 0.92f,
          center = center
        )
        // Brilliant white core
        drawCircle(
          color = Color.White,
          radius = r * 0.50f,
          center = center
        )
        // Outer metallic edge
        drawCircle(
          color = Color(0xFFFFF176),
          radius = r * 0.92f,
          center = center,
          style = Stroke(width = 2f)
        )
      } else {
        // 2. Puck Shadow
        val shadowPos = toScreen(puck.position, heightOffset = 0f)
        drawCircle(
          color = Color.Black.copy(alpha = 0.6f),
          radius = r * 1.1f,
          center = shadowPos + Offset(0f, 3f)
        )

        // 3. 3D Puck Cylinder (Top face offset by 8px)
        val topPos = toScreen(puck.position, heightOffset = 8f)
        // Side wall
        drawCircle(
          color = Color(0xFFCC7A00),
          radius = r,
          center = shadowPos
        )
        // Radiant Halo
        drawCircle(
          color = NeonGold.copy(alpha = 0.45f),
          radius = r * 1.6f,
          center = topPos
        )
        // Top face
        drawCircle(
          color = NeonGold,
          radius = r,
          center = topPos
        )
        // Brilliant white core
        drawCircle(
          color = Color.White,
          radius = r * 0.55f,
          center = topPos
        )
        // Puck Rim
        drawCircle(
          color = Color(0xFFFFF176),
          radius = r,
          center = topPos,
          style = Stroke(width = 2f)
        )
      }
    }
  }

  private fun drawStriker(
    drawScope: DrawScope,
    striker: com.example.physics.Striker,
    primaryColor: Color,
    glowColor: Color
  ) {
    with(drawScope) {
      val r = scaleRadius(striker.radius, striker.position.y)

      if (isFlat) {
        // Pure symmetric concentric top-down rendering for head-to-head 2P fairness
        val center = toScreen(striker.position)

        // 1. Base shadow
        drawCircle(
          color = Color.Black.copy(alpha = 0.5f),
          radius = r * 1.15f,
          center = center
        )

        // 2. Base Glow Ring
        drawCircle(
          color = glowColor.copy(alpha = 0.35f),
          radius = r * 1.35f,
          center = center
        )

        // 3. Lower Outer Beveled Ring
        drawCircle(
          color = Color(0xFF0F152B),
          radius = r,
          center = center
        )

        // 4. Primary Color Bevel
        drawCircle(
          color = primaryColor,
          radius = r * 0.94f,
          center = center
        )

        // 5. Recessed Inner Ring
        drawCircle(
          color = Color(0xFF131A33),
          radius = r * 0.78f,
          center = center
        )

        // 6. Handle Knob Base
        val handleR = r * 0.44f
        drawCircle(
          color = Color(0xFF0C1021),
          radius = handleR,
          center = center
        )

        // 7. Handle Knob Top & White Core Dot
        drawCircle(
          color = primaryColor,
          radius = handleR * 0.88f,
          center = center
        )
        drawCircle(
          color = Color.White.copy(alpha = 0.9f),
          radius = handleR * 0.42f,
          center = center
        )
      } else {
        // 3D Tilted Perspective with Height-Offset Knob & Rims
        val basePos = toScreen(striker.position, heightOffset = 0f)
        val rimPos = toScreen(striker.position, heightOffset = 10f)
        val handlePos = toScreen(striker.position, heightOffset = 24f)

        // 1. Cast Floor Shadow
        drawCircle(
          color = Color.Black.copy(alpha = 0.55f),
          radius = r * 1.2f,
          center = basePos + Offset(0f, 5f)
        )

        // 2. Base Glow Ring
        drawCircle(
          color = glowColor.copy(alpha = 0.35f),
          radius = r * 1.4f,
          center = basePos
        )

        // 3. Lower Rim Cylinder
        drawCircle(
          color = Color(0xFF0F152B),
          radius = r,
          center = basePos
        )
        drawCircle(
          color = primaryColor,
          radius = r,
          center = rimPos
        )
        drawCircle(
          color = Color(0xFF131A33),
          radius = r * 0.82f,
          center = rimPos
        )

        // 4. Center Handle Knob
        val handleR = r * 0.45f
        drawCircle(
          color = Color(0xFF0C1021),
          radius = handleR,
          center = rimPos
        )
        drawCircle(
          color = primaryColor,
          radius = handleR,
          center = handlePos
        )
        drawCircle(
          color = Color.White.copy(alpha = 0.85f),
          radius = handleR * 0.42f,
          center = handlePos
        )
      }
    }
  }
}

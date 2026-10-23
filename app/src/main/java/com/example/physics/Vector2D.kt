package com.example.physics

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vector2D(val x: Float = 0f, val y: Float = 0f) {
  operator fun plus(other: Vector2D) = Vector2D(x + other.x, y + other.y)
  operator fun minus(other: Vector2D) = Vector2D(x - other.x, y - other.y)
  operator fun times(scalar: Float) = Vector2D(x * scalar, y * scalar)
  operator fun div(scalar: Float) = if (scalar != 0f) Vector2D(x / scalar, y / scalar) else Vector2D.ZERO
  operator fun unaryMinus() = Vector2D(-x, -y)

  fun length(): Float = sqrt(x * x + y * y)
  fun lengthSquared(): Float = x * x + y * y

  fun normalized(): Vector2D {
    val len = length()
    return if (len > 0.0001f) Vector2D(x / len, y / len) else Vector2D.ZERO
  }

  fun distanceTo(other: Vector2D): Float = (this - other).length()
  fun distanceSquaredTo(other: Vector2D): Float = (this - other).lengthSquared()

  fun dot(other: Vector2D): Float = x * other.x + y * other.y
  fun cross(other: Vector2D): Float = x * other.y - y * other.x

  fun reflect(normal: Vector2D): Vector2D {
    val norm = normal.normalized()
    val d = this.dot(norm)
    return this - norm * (2f * d)
  }

  fun clampLength(maxLength: Float): Vector2D {
    val len = length()
    return if (len > maxLength && len > 0.0001f) {
      this * (maxLength / len)
    } else {
      this
    }
  }

  fun angle(): Float = atan2(y, x)

  companion object {
    val ZERO = Vector2D(0f, 0f)
    fun fromAngle(angleRad: Float, length: Float = 1f): Vector2D {
      return Vector2D(cos(angleRad) * length, sin(angleRad) * length)
    }
  }
}

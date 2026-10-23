package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

class ArcadeSoundPlayer(context: Context) {
  var isSoundEnabled: Boolean = true
  private val sampleRate = 22050

  private val soundPool: SoundPool = SoundPool.Builder()
    .setMaxStreams(8)
    .setAudioAttributes(
      AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    )
    .build()

  private var soundWallId = 0
  private var soundStrikerId = 0
  private var soundBumperId = 0
  private var soundSlingshotId = 0
  private var soundTargetId = 0
  private var soundGoalId = 0

  init {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val cacheDir = context.cacheDir
        soundWallId = loadGeneratedSound(cacheDir, "wall.wav", 300f, 150f, 40, 0.5f)
        soundStrikerId = loadGeneratedSound(cacheDir, "striker.wav", 950f, 380f, 60, 0.7f)
        soundBumperId = loadGeneratedSound(cacheDir, "bumper.wav", 650f, 1400f, 110, 0.8f)
        soundSlingshotId = loadGeneratedSound(cacheDir, "slingshot.wav", 450f, 1050f, 95, 0.75f)
        soundTargetId = loadGeneratedSound(cacheDir, "target.wav", 1600f, 2200f, 80, 0.6f)
        soundGoalId = loadGeneratedGoalSound(cacheDir, "goal.wav")
      } catch (_: Exception) {}
    }
  }

  private fun loadGeneratedSound(
    dir: File,
    filename: String,
    startFreq: Float,
    endFreq: Float,
    durationMs: Int,
    volume: Float
  ): Int {
    val file = File(dir, filename)
    if (!file.exists()) {
      val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
      val buffer = ShortArray(numSamples)
      var phase = 0.0

      for (i in 0 until numSamples) {
        val progress = i.toFloat() / numSamples
        val currentFreq = startFreq + (endFreq - startFreq) * progress
        val phaseIncrement = 2.0 * Math.PI * currentFreq / sampleRate
        phase += phaseIncrement
        val envelope = (1.0f - progress) * (1.0f - progress)
        val sampleValue = (sin(phase) * Short.MAX_VALUE * volume * envelope).toInt()
        buffer[i] = sampleValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
      }
      writeWavFile(file, buffer, sampleRate)
    }
    return soundPool.load(file.absolutePath, 1)
  }

  private fun loadGeneratedGoalSound(dir: File, filename: String): Int {
    val file = File(dir, filename)
    if (!file.exists()) {
      val durationMs = 500
      val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
      val buffer = ShortArray(numSamples)
      var phase = 0.0

      for (i in 0 until numSamples) {
        val progress = i.toFloat() / numSamples
        val currentFreq = if (progress < 0.3f) 440f else if (progress < 0.6f) 660f else 880f
        val phaseIncrement = 2.0 * Math.PI * currentFreq / sampleRate
        phase += phaseIncrement
        val envelope = (1.0f - progress * 0.5f)
        val sampleValue = (sin(phase) * Short.MAX_VALUE * 0.85f * envelope).toInt()
        buffer[i] = sampleValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
      }
      writeWavFile(file, buffer, sampleRate)
    }
    return soundPool.load(file.absolutePath, 1)
  }

  private fun writeWavFile(file: File, samples: ShortArray, sRate: Int) {
    val dataSize = samples.size * 2
    val totalSize = 36 + dataSize
    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)

    header.put("RIFF".toByteArray())
    header.putInt(totalSize)
    header.put("WAVE".toByteArray())
    header.put("fmt ".toByteArray())
    header.putInt(16) // Subchunk1Size for PCM
    header.putShort(1) // AudioFormat 1 = PCM
    header.putShort(1) // NumChannels = 1 (Mono)
    header.putInt(sRate) // SampleRate
    header.putInt(sRate * 2) // ByteRate (SampleRate * NumChannels * BitsPerSample/8)
    header.putShort(2) // BlockAlign (NumChannels * BitsPerSample/8)
    header.putShort(16) // BitsPerSample
    header.put("data".toByteArray())
    header.putInt(dataSize)

    val pcmBytes = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
    for (s in samples) {
      pcmBytes.putShort(s)
    }

    FileOutputStream(file).use { fos ->
      fos.write(header.array())
      fos.write(pcmBytes.array())
    }
  }

  fun playWallHit() {
    if (isSoundEnabled && soundWallId != 0) {
      soundPool.play(soundWallId, 0.45f, 0.45f, 1, 0, 1.0f)
    }
  }

  fun playStrikerHit(intensity: Float) {
    if (isSoundEnabled && soundStrikerId != 0) {
      val pitch = (0.9f + intensity * 0.25f).coerceIn(0.7f, 1.4f)
      soundPool.play(soundStrikerId, 0.7f, 0.7f, 1, 0, pitch)
    }
  }

  fun playBumperHit() {
    if (isSoundEnabled && soundBumperId != 0) {
      soundPool.play(soundBumperId, 0.85f, 0.85f, 2, 0, 1.0f)
    }
  }

  fun playSlingshotHit() {
    if (isSoundEnabled && soundSlingshotId != 0) {
      soundPool.play(soundSlingshotId, 0.8f, 0.8f, 2, 0, 1.0f)
    }
  }

  fun playTargetHit() {
    if (isSoundEnabled && soundTargetId != 0) {
      soundPool.play(soundTargetId, 0.7f, 0.7f, 1, 0, 1.0f)
    }
  }

  fun playGoal() {
    if (isSoundEnabled && soundGoalId != 0) {
      soundPool.play(soundGoalId, 1.0f, 1.0f, 3, 0, 1.0f)
    }
  }
}

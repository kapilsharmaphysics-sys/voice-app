package com.app.voiceapp.data.local

import android.content.Context
import java.io.File
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

/**
 * Generates real playable WAV files on first launch so the mock feed actually plays audio, not dummy paths.
 * Uses 44100Hz mono 16-bit PCM with a 5% attack/release fade to avoid click artifacts at the edges.
 */
object SampleAudioGenerator {

    private val samples = listOf(
        Triple("sample_morning.wav", 349.23f, 8_000L),
        Triple("sample_podcast.wav", 261.63f, 14_000L),
        Triple("sample_vibes.wav", 523.25f, 6_000L),
        Triple("sample_thoughts.wav", 440.00f, 11_000L),
    )

    /** Creates missing sample files on first run and returns their absolute paths paired with durations. */
    fun ensureSamplesExist(context: Context): List<Pair<String, Long>> {
        return samples.map { (filename, frequency, durationMs) ->
            val file = File(context.filesDir, filename)
            if (!file.exists()) {
                generateWavFile(file, frequency, durationMs)
            }
            file.absolutePath to durationMs
        }
    }

    /** Writes a single-frequency sine wave to [file] at [frequencyHz] for [durationMs] with fade in/out. */
    private fun generateWavFile(file: File, frequencyHz: Float, durationMs: Long) {
        val sampleRate = 44100
        val totalSamples = (sampleRate * durationMs / 1000).toInt()
        val pcm = ShortArray(totalSamples) { i ->
            val envelope = when {
                i < sampleRate * 0.05 -> i / (sampleRate * 0.05)
                i > totalSamples - sampleRate * 0.05 -> (totalSamples - i) / (sampleRate * 0.05)
                else -> 1.0
            }
            (Short.MAX_VALUE * 0.4 * envelope * sin(2.0 * PI * frequencyHz * i / sampleRate)).toInt().toShort()
        }

        file.outputStream().buffered().use { out ->
            val dataBytes = totalSamples * 2
            writeWavHeader(out, dataBytes, sampleRate)
            val buffer = ByteBuffer.allocate(dataBytes).order(ByteOrder.LITTLE_ENDIAN)
            pcm.forEach { buffer.putShort(it) }
            out.write(buffer.array())
        }
    }

    /** Writes the 44-byte RIFF/WAVE/fmt/data header required by the WAV spec before the PCM payload. */
    private fun writeWavHeader(out: OutputStream, dataBytes: Int, sampleRate: Int) {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        out.write("RIFF".toByteArray())
        out.writeInt32Le(dataBytes + 36)
        out.write("WAVE".toByteArray())
        out.write("fmt ".toByteArray())
        out.writeInt32Le(16)
        out.writeInt16Le(1)
        out.writeInt16Le(channels)
        out.writeInt32Le(sampleRate)
        out.writeInt32Le(byteRate)
        out.writeInt16Le(blockAlign)
        out.writeInt16Le(bitsPerSample)
        out.write("data".toByteArray())
        out.writeInt32Le(dataBytes)
    }

    private fun OutputStream.writeInt32Le(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
        write((value shr 16) and 0xFF)
        write((value shr 24) and 0xFF)
    }

    private fun OutputStream.writeInt16Le(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
    }
}

package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class SoundEngine(private val context: Context) {
    private val prefs = context.getSharedPreferences("stop_rush_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var muted: Boolean = prefs.getBoolean("stop_game_muted", false)

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private enum class WaveType { SINE, TRIANGLE, SAWTOOTH }

    init {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val res = tts?.setLanguage(Locale("es", "ES"))
                    if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale.getDefault())
                    }
                    ttsReady = true
                }
            }
        } catch (_: Throwable) {
            ttsReady = false
        }
    }

    fun isMuted(): Boolean = muted

    fun setMuted(mute: Boolean) {
        muted = mute
        prefs.edit().putBoolean("stop_game_muted", mute).apply()
        if (mute) {
            try {
                tts?.stop()
            } catch (_: Throwable) {}
        }
    }

    fun toggleMute(): Boolean {
        setMuted(!muted)
        return muted
    }

    fun playLetterTick() {
        if (muted) return
        val freq = 450.0 + Random.nextDouble() * 80.0
        playToneAsync(freq, freq, 50, 0.14, WaveType.TRIANGLE)
    }

    fun playLetterReveal() {
        if (muted) return
        val notes = listOf(523.25, 659.25, 783.99, 1046.50)
        scope.launch {
            notes.forEachIndexed { idx, freq ->
                if (idx > 0) delay(70)
                playToneBlocking(freq, freq, 240, 0.18, WaveType.SINE)
            }
        }
    }

    fun playKeyType() {
        if (muted) return
        val freq = 800.0 + Random.nextDouble() * 200.0
        playToneAsync(freq, freq, 22, 0.05, WaveType.SINE)
    }

    fun playStopAlarm() {
        if (muted) return
        // Siren sweep + sub-bass impact gong
        playToneAsync(880.0, 220.0, 620, 0.28, WaveType.SAWTOOTH)
        playToneAsync(140.0, 45.0, 780, 0.35, WaveType.TRIANGLE)
    }

    fun playTimerAlert() {
        if (muted) return
        playToneAsync(750.0, 750.0, 85, 0.20, WaveType.SINE)
    }

    fun playScoreChime(points: Int) {
        if (muted) return
        val base = if (points >= 10) 587.33 else 440.0
        playToneAsync(base, base * 1.5, 190, 0.18, WaveType.SINE)
    }

    fun playVictoryFanfare() {
        if (muted) return
        val melody = listOf(
            523.25 to 120L,
            659.25 to 120L,
            783.99 to 140L,
            1046.50 to 340L
        )
        scope.launch {
            melody.forEach { (freq, dur) ->
                playToneBlocking(freq, freq, dur.toInt(), 0.22, WaveType.TRIANGLE)
                delay((dur * 0.85).toLong())
            }
        }
    }

    fun playGameShowIntro() {
        if (muted) return
        val fanfare = listOf(
            523.25 to 95L,
            659.25 to 95L,
            783.99 to 95L,
            1046.50 to 240L
        )
        scope.launch {
            fanfare.forEachIndexed { idx, (freq, dur) ->
                if (idx > 0) delay(75L)
                playToneBlocking(freq, freq * 1.01, dur.toInt(), 0.20, WaveType.TRIANGLE)
            }
        }
    }

    fun playStopBuzzerAndFanfare() {
        if (muted) return
        // Punchy contest buzzer + celebratory high bell arpeggio
        playToneAsync(220.0, 110.0, 220, 0.30, WaveType.SAWTOOTH)
        scope.launch {
            delay(140L)
            listOf(659.25, 783.99, 1046.50, 1318.51).forEachIndexed { idx, f ->
                if (idx > 0) delay(55L)
                playToneBlocking(f, f, 200, 0.20, WaveType.SINE)
            }
        }
    }

    fun playCrowdApplauseAndCheer(durationMs: Int = 2700) {
        if (muted) return
        scope.launch {
            playSynthesizedApplauseBlocking(durationMs)
        }
    }

    fun playCountdownBeep(step: Int) {
        if (muted) return
        val freq = when (step) {
            1 -> 659.0
            2 -> 554.0
            else -> 440.0
        }
        playToneAsync(freq, freq, 220, 0.24, WaveType.SINE)
    }

    fun playCountdownStop() {
        if (muted) return
        listOf(523.25, 659.25, 783.99, 1046.50).forEach { f ->
            playToneAsync(f, f * 0.95, 420, 0.12, WaveType.SAWTOOTH)
        }
        playToneAsync(150.0, 40.0, 500, 0.34, WaveType.TRIANGLE)
    }

    fun speakVoice(
        text: String,
        rate: Float = 1.24f,
        pitch: Float = 1.16f,
        flush: Boolean = true
    ) {
        if (muted || !ttsReady) return
        try {
            tts?.setSpeechRate(rate)
            tts?.setPitch(pitch)
            val mode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts?.speak(text, mode, null, "stop_tts_${System.currentTimeMillis()}")
        } catch (_: Throwable) {}
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Throwable) {}
    }

    private fun playToneAsync(
        startFreq: Double,
        endFreq: Double,
        durationMs: Int,
        volume: Double,
        wave: WaveType
    ) {
        scope.launch {
            playToneBlocking(startFreq, endFreq, durationMs, volume, wave)
        }
    }

    private fun playToneBlocking(
        startFreq: Double,
        endFreq: Double,
        durationMs: Int,
        volume: Double,
        wave: WaveType
    ) {
        try {
            val sampleRate = 22050
            val numSamples = (durationMs * sampleRate) / 1000
            if (numSamples <= 0) return

            val pcm = ShortArray(numSamples)
            var phase = 0.0
            for (i in 0 until numSamples) {
                val t = i.toDouble() / numSamples
                val freq = startFreq + (endFreq - startFreq) * t
                phase += 2.0 * PI * freq / sampleRate
                val raw = when (wave) {
                    WaveType.SINE -> sin(phase)
                    WaveType.TRIANGLE -> (2.0 / PI) * asin(sin(phase))
                    WaveType.SAWTOOTH -> {
                        val norm = (phase / (2.0 * PI)) % 1.0
                        2.0 * norm - 1.0
                    }
                }
                val env = exp(-4.5 * t)
                val sample = (raw * env * volume * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                pcm[i] = sample.toShort()
            }

            playPcmStatic(pcm, sampleRate, durationMs)
        } catch (_: Throwable) {
            // AudioTrack may fail in headless unit test/emulation edge cases
        }
    }

    private fun playSynthesizedApplauseBlocking(durationMs: Int) {
        try {
            val sampleRate = 22050
            val numSamples = (durationMs * sampleRate) / 1000
            if (numSamples <= 0) return

            val mix = DoubleArray(numSamples)
            val rnd = Random(System.currentTimeMillis())

            // Layer 1: Warm crowd cheer + happy audience roar swell
            var cheerLow = 0.0
            var cheerBand = 0.0
            for (i in 0 until numSamples) {
                val t = i.toDouble() / numSamples
                val fadeIn = (t / 0.07).coerceIn(0.0, 1.0)
                val fadeOut = ((1.0 - t) / 0.25).coerceIn(0.0, 1.0)
                val crowdSwell = fadeIn * fadeOut * (0.75 + 0.25 * sin(2.0 * PI * 2.2 * t))
                val white = rnd.nextDouble() * 2.0 - 1.0
                cheerLow = cheerLow * 0.86 + white * 0.14
                cheerBand = cheerBand * 0.55 + cheerLow * 0.45
                // Subtle joyful vocal-like crowd overtone + stadium chord shimmer
                val chord = (
                    sin(2.0 * PI * 523.25 * i / sampleRate) * 0.35 +
                    sin(2.0 * PI * 659.25 * i / sampleRate) * 0.30 +
                    sin(2.0 * PI * 783.99 * i / sampleRate) * 0.25 +
                    sin(2.0 * PI * 1046.50 * i / sampleRate) * 0.20
                ) * exp(-2.4 * t) * 0.14
                mix[i] += cheerBand * crowdSwell * 0.48 + chord
            }

            // Layer 2: Staggered individual handclaps from dozens of happy audience members
            val totalClaps = 110
            val activeEnd = (numSamples * 0.88).toInt().coerceAtLeast(1)
            for (c in 0 until totalClaps) {
                val startSample = rnd.nextInt(activeEnd)
                val clapLen = ((0.028 + rnd.nextDouble() * 0.032) * sampleRate).toInt()
                val clapAmp = 0.22 + rnd.nextDouble() * 0.28
                val decayRate = 5.0 + rnd.nextDouble() * 4.5
                var prev = 0.0
                for (j in 0 until clapLen) {
                    val idx = startSample + j
                    if (idx >= numSamples) break
                    val ct = j.toDouble() / clapLen
                    val white = rnd.nextDouble() * 2.0 - 1.0
                    // Simple band emphasis around mid frequencies for crisp handclap pop
                    val hp = white - prev * 0.65
                    prev = white
                    val env = exp(-decayRate * ct)
                    mix[idx] += hp * env * clapAmp
                }
            }

            // Normalize softly into PCM 16-bit
            val pcm = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / numSamples
                val masterFadeOut = ((1.0 - t) / 0.15).coerceIn(0.0, 1.0)
                val sampleVal = (mix[i] * 0.45 * masterFadeOut * Short.MAX_VALUE)
                    .toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                pcm[i] = sampleVal.toShort()
            }

            playPcmStatic(pcm, sampleRate, durationMs)
        } catch (_: Throwable) {
        }
    }

    private fun playPcmStatic(pcm: ShortArray, sampleRate: Int, durationMs: Int) {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()

        track.write(pcm, 0, pcm.size)
        track.play()
        Thread.sleep(durationMs.toLong() + 40L)
        track.stop()
        track.release()
    }
}

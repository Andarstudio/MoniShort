package app.monishort.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import app.monishort.automation.ActionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** Plays one short sound at a time (a new sound replaces the previous one). */
class SoundPlayer(private val context: Context) {
    private class Active(val mp: MediaPlayer, val onDone: () -> Unit)

    private val lock = Any()
    private var active: Active? = null

    /** Starts playback and returns immediately. @throws ActionException if the file cannot be played. */
    fun play(uri: Uri, volume: Float, onDone: () -> Unit = {}) {
        stop()
        val mp = MediaPlayer()
        val a = Active(mp, onDone)
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setDataSource(context, uri)
            val v = volume.coerceIn(0f, 1f)
            mp.setVolume(v, v)
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener { finish(a) }
            mp.setOnErrorListener { _, _, _ -> finish(a); true }
            synchronized(lock) { active = a }
            mp.prepareAsync()
        } catch (e: Exception) {
            synchronized(lock) { if (active === a) active = null }
            runCatching { mp.release() }
            throw ActionException("Cannot play sound: ${e.message ?: e::class.java.simpleName}")
        }
    }

    suspend fun playAndWait(uri: Uri, volume: Float) = suspendCancellableCoroutine<Unit> { cont ->
        try {
            play(uri, volume) { if (cont.isActive) cont.resume(Unit) }
        } catch (e: Exception) {
            cont.resumeWithException(e)
        }
        cont.invokeOnCancellation { stop() }
    }

    fun stop() {
        val a = synchronized(lock) { active.also { active = null } } ?: return
        runCatching { a.mp.stop() }
        finish(a)
    }

    private fun finish(a: Active) {
        synchronized(lock) { if (active === a) active = null }
        runCatching { a.mp.release() }
        a.onDone()
    }
}

object SoundFiles {
    fun displayName(context: Context, uri: Uri): String {
        val name: String? = try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
        return name ?: uri.lastPathSegment ?: "Selected sound"
    }
}

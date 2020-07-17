package tech.alvarez.filterable

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import java.io.IOException
import java.io.OutputStream
import kotlin.math.abs

fun Context.toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun Context.log(message: String) {
    if (BuildConfig.DEBUG) Log.d(this::class.java.simpleName, message)
}

fun Context.saveBitmap(bitmap: Bitmap, displayName: String) {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
    }
    val contentUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    var stream: OutputStream? = null
    var uri: Uri? = null
    try {
        uri = contentResolver.insert(contentUri, contentValues)
        if (uri == null) {
            throw IOException("Failed to create new MediaStore record.")
        }
        stream = contentResolver.openOutputStream(uri)
        if (stream == null) {
            throw IOException("Failed to get output stream.")
        }
        if (!bitmap.compress(CompressFormat.JPEG, 100, stream)) {
            throw IOException("Failed to save bitmap.")
        }
    } catch (e: IOException) {
        uri?.let { contentResolver.delete(it, null, null) }
        log("IOException ${e.message}")
    } finally {
        stream?.close()
    }
}

internal open class OnSwipeTouchListener(context: Context) : View.OnTouchListener {

    private val gestureDetector: GestureDetector

    companion object {
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }

    init {
        gestureDetector = GestureDetector(context, GestureListener())
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent) = true

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onClick()
            return true
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            try {
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        return if (diffX > 0) {
                            onSwipeRight()
                            true
                        } else {
                            onSwipeLeft()
                            true
                        }
                    }
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            return false
        }
    }

    open fun onClick() {}
    open fun onSwipeRight() {}
    open fun onSwipeLeft() {}
}
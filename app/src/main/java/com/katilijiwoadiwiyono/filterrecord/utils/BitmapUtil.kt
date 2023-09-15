package com.katilijiwoadiwiyono.filterrecord.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import android.opengl.GLException
import android.util.Log
import java.nio.IntBuffer
import javax.microedition.khronos.opengles.GL10


fun Bitmap.rotateBitmap(angle: Float): Bitmap? {
    return try {
        val matrix = Matrix()
        matrix.postRotate(angle)
        Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
    } catch (ex: Exception) {
        null
    }
}

fun createBitmapFromGLSurface(w: Int, h: Int, gl: GL10): Bitmap? {
    val bitmapBuffer = IntArray(w * h)
    val bitmapSource = IntArray(w * h)
    val intBuffer = IntBuffer.wrap(bitmapBuffer)
    intBuffer.position(0)
    try {
        gl.glReadPixels(0, 0, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, intBuffer)
        var offset1: Int
        var offset2: Int
        var texturePixel: Int
        var blue: Int
        var red: Int
        var pixel: Int
        for (i in 0 until h) {
            offset1 = i * w
            offset2 = (h - i - 1) * w
            for (j in 0 until w) {
                texturePixel = bitmapBuffer[offset1 + j]
                blue = texturePixel shr 16 and 0xff
                red = texturePixel shl 16 and 0x00ff0000
                pixel = texturePixel and -0xff0100 or red or blue
                bitmapSource[offset2 + j] = pixel
            }
        }
    } catch (e: GLException) {
        Log.e("CreateBitmap", "createBitmapFromGLSurface: " + e.message, e)
        return null
    }
    return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888)
}


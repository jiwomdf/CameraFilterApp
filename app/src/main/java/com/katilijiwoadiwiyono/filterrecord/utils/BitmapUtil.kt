package com.katilijiwoadiwiyono.filterrecord.utils

import android.graphics.Bitmap
import android.graphics.Matrix


fun Bitmap.rotateBitmap(angle: Float): Bitmap? {
    return try {
        val matrix = Matrix()
        matrix.postRotate(angle)
        Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
    } catch (ex: Exception) {
        null
    }
}

package com.katilijiwoadiwiyono.filterrecord.utils

import android.os.Environment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NameUtil {

    private const val FILENAME_FORMAT = "yyyyMM_dd-HHmmss"

    fun getVideoFileName(): String {
        return SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault()).format(Date()) + "filter_record.mp4"
    }

    fun getMoviesFolder(): String {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath
    }

    fun getImageName(): String {
        return SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault()).format(Date()) + "filter_record"
    }
}
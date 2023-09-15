package com.katilijiwoadiwiyono.filterrecord.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore

object VideoUtil {
    fun exportMp4ToGallery(context: Context, filePath: String, fileName: String) {
        val values = ContentValues(2)
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        values.put(MediaStore.Video.Media.RELATIVE_PATH, filePath)
        values.put(MediaStore.Video.Media.RELATIVE_PATH, fileName)
        context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        context.sendBroadcast(
            Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
            Uri.parse("file://$filePath/$fileName"))
        )
    }

    fun getVideoUri(filePath: String, fileName: String): Uri? {
        return Uri.parse("file://$filePath/$fileName")
    }
}
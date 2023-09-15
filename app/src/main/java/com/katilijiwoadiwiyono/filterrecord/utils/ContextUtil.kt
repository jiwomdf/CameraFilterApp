package com.katilijiwoadiwiyono.filterrecord.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object ContextUtil {
    fun Context.openInGallery(uri: Uri) {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.setDataAndType(uri, "image/*")
        this.startActivity(intent)
    }
}
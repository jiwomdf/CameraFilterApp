//package com.katilijiwoadiwiyono.filterrecord.utils
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.os.Build
//import android.os.Environment
//import android.util.Log
//import org.jcodec.api.awt.AWTSequenceEncoder
//import org.jcodec.common.AndroidUtil
//import org.jcodec.common.io.NIOUtils
//import org.jcodec.common.model.ColorSpace.RGB
//import org.jcodec.common.model.Picture
//import org.jcodec.common.model.Rational
//import java.io.File
//import java.lang.System.out
//
//private fun creteRootPath(context: Context): File? {
//    var file: File? = null
//    try {
//        file = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            File(context
//                .getExternalFilesDir("")
//                .toString() + File.separator + "UsamaSd.mp4")
//        } else {
//            File(
//                Environment
//                    .getExternalStorageDirectory()
//                    .absolutePath.toString()
//                        + File.separator + "UsamaSd.mp4")
//        }
//        if (file?.exists() == true) {
//            file?.mkdirs()
//        }
//    } catch (e: Exception) {
//        e.printStackTrace()
//        file // it will return null
//    }
//    return file
//}
//fun convertImagesToVideo(
//    context: Context,
//    arrBitmap: Array<Bitmap>,
//) {
//    try {//Rational(1, 1). SequenceEncoder
//        val output = creteRootPath(context)
//        val enc =
//
//            AWTSequenceEncoder.createWithFps(
//                NIOUtils.writableChannel(output),
//                Rational.R(2, 1))
//        for (bitmap in arrBitmap) {
//            enc.encodeNativeFrame(fromBitmaps(bitmap))
//        }
//        enc.finish()
//    } catch (ex: Exception) {
//        Log.e("jiwo", "convertImagesToVideo: $ex")
//    }
//    finally {
//        NIOUtils.closeQuietly(out);
//    }
//}
//
//private fun fromBitmaps(src: Bitmap): Picture {
//    val dst: Picture = Picture.create(src.width, src.height, RGB)
//    AndroidUtil.fromBitmap(src, dst)
//    return dst
//}

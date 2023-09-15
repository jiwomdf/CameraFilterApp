package com.katilijiwoadiwiyono.filterrecord.features.viewer

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.util.TypedValue
import android.widget.MediaController
import androidx.lifecycle.lifecycleScope
import com.katilijiwoadiwiyono.filterrecord.common.BaseActivity
import com.katilijiwoadiwiyono.filterrecord.databinding.ActivityMediaViewerBinding
import kotlinx.coroutines.launch
import java.lang.RuntimeException

class MediaViewerActivity : BaseActivity<ActivityMediaViewerBinding>() {

    companion object {
        fun newInstance(
            context: Context,
            uri: String
        ): Intent {
            val intent = Intent(context, MediaViewerActivity::class.java)
            intent.putExtra(VIDEO_URI_ARGS, uri)
            return intent
        }

        private const val VIDEO_URI_ARGS = "uri_args"
    }

    override fun getViewBinding(): ActivityMediaViewerBinding =
        ActivityMediaViewerBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setupView()
        setupListener()
    }

    private fun setupView() {
        val uri = Uri.parse(intent.getStringExtra(VIDEO_URI_ARGS))
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            showVideo(uri)
        } else {
            val path = getAbsolutePathFromUri(uri) ?: return
            MediaScannerConnection.scanFile(this@MediaViewerActivity,
                arrayOf(path), null) { _, uri ->
                if (uri != null) {
                    lifecycleScope.launch {
                        showVideo(uri)
                    }
                }
            }
        }
    }

    private fun setupListener() {
        with(binding) {
            backButton.setOnClickListener {
                finish()
            }
        }
    }

    private fun showVideo(uri : Uri) {
        with(binding) {
            val fileSize = getFileSizeFromUri(uri)
            if (fileSize == null || fileSize <= 0) {
                showToast("Failed to get recorded file size, could not be played!")
                return
            }

            val filePath = getAbsolutePathFromUri(uri) ?: return
            val fileInfo = "FileSize: $fileSize\n $filePath"
            videoViewerTips.text = fileInfo

            val mc = MediaController(this@MediaViewerActivity)
            videoViewer.apply {
                setVideoURI(uri)
                setMediaController(mc)
                requestFocus()
            }.start()
            mc.show(0)
        }
    }


    private fun getFileSizeFromUri(contentUri: Uri): Long? {
        val cursor = this
            .contentResolver
            .query(contentUri, null, null, null, null)
            ?: return null

        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        cursor.moveToFirst()
        return cursor.getLong(sizeIndex)
    }

    private fun getAbsolutePathFromUri(contentUri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = this
                .contentResolver
                .query(contentUri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)
            if (cursor == null) {
                return null
            }
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            cursor.getString(columnIndex)
        } catch (e: RuntimeException) {
            Log.e("VideoViewerFragment", String.format(
                "Failed in getting absolute path for Uri %s with Exception %s",
                contentUri.toString(), e.toString())
            )
            null
        } finally {
            cursor?.close()
        }
    }


}
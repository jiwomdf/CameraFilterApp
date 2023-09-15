package com.katilijiwoadiwiyono.filterrecord.features.camera


import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.isVisible
import com.daasuu.gpuv.camerarecorder.CameraRecordListener
import com.daasuu.gpuv.camerarecorder.GPUCameraRecorder
import com.daasuu.gpuv.camerarecorder.GPUCameraRecorderBuilder
import com.daasuu.gpuv.camerarecorder.LensFacing
import com.daasuu.gpuv.egl.filter.GlRGBFilter
import com.katilijiwoadiwiyono.filterrecord.common.BaseActivity
import com.katilijiwoadiwiyono.filterrecord.databinding.ActivityMainBinding
import com.katilijiwoadiwiyono.filterrecord.features.viewer.MediaViewerActivity
import com.katilijiwoadiwiyono.filterrecord.utils.ContextUtil.openInGallery
import com.katilijiwoadiwiyono.filterrecord.utils.NameUtil
import com.katilijiwoadiwiyono.filterrecord.utils.VideoUtil
import com.katilijiwoadiwiyono.filterrecord.utils.checkMultiplePermissions
import com.katilijiwoadiwiyono.filterrecord.utils.createBitmapFromGLSurface
import com.katilijiwoadiwiyono.filterrecord.utils.hasPermissions
import com.katilijiwoadiwiyono.filterrecord.utils.requestMultiplePermissionLauncher
import com.katilijiwoadiwiyono.filterrecord.utils.widget.SampleCameraGLView
import com.programmergabut.scopestorageutility.ScopeStorageUtility
import com.programmergabut.scopestorageutility.util.Extension
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.opengles.GL10


open class MainActivity: BaseActivity<ActivityMainBinding>() {

    private val permissionsLauncher = requestMultiplePermissionLauncher(
        onPermissionGranted = {
            setUpCamera()
        },
        onPermissionDenied = {
            showToast("Permission $it Required", Toast.LENGTH_SHORT)
        }
    )

    private var arrPermission = mutableListOf<String>()
    private var sampleGLView: SampleCameraGLView? = null
    private var gPUCameraRecorder: GPUCameraRecorder? = null
    private var lensFacing = LensFacing.BACK
    private var toggleClick = false
    private var videoWidth = 720
    private var videoHeight = 1280
    private var cameraWidth = 1280
    private var cameraHeight = 720
    private var videoFilepath: String = ""
    private var videoFileName: String = ""

    init {
        val isTiramisuAndAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val permission = if (isTiramisuAndAbove) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
        arrPermission = mutableListOf(permission, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    }

    override fun onResume() {
        super.onResume()
        hasPermissions(
            context = this@MainActivity,
            permissions = arrPermission.toTypedArray(),
            hasPermission = {
                setUpCamera()
            },
            notHasPermission = {
                showToast("Permissions required")
            }
        )
    }

    override fun onStop() {
        hasPermissions(
            context = this@MainActivity,
            permissions = arrPermission.toTypedArray(),
            hasPermission = {
                releaseCamera()
            },
            notHasPermission = {
                showToast("Permissions required")
            }
        )
        super.onStop()
    }

    override fun getViewBinding(): ActivityMainBinding =
        ActivityMainBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        checkMultiplePermissions(
            permissions = arrPermission.toTypedArray(),
            launcher = permissionsLauncher,
            onPermissionGranted = {
                setUpCamera()
            }
        )
        setListener()
    }

    private fun setListener() {
        with(binding) {
            switchButton.setOnClickListener {
                releaseCamera()
                lensFacing = if (lensFacing == LensFacing.BACK) {
                    LensFacing.FRONT
                } else {
                    LensFacing.BACK
                }
                toggleClick = true
            }

            btnRecord.setOnClickListener {
                videoFilepath = "${NameUtil.getMoviesFolder()}/${NameUtil.getVideoFileName()}"
                Log.e("jiwo", "setListener: $videoFilepath")
                gPUCameraRecorder?.start(videoFilepath)
                buttonState(true)
            }

            stopButton.setOnClickListener {
                gPUCameraRecorder?.stop()
                buttonState(false)
            }

            btnTakePhoto.setOnClickListener {
                captureBitmap {
                    it?.let {
                        val name = NameUtil.getImageName()
                        val dir = "FilterRecord/"
                        ScopeStorageUtility
                            .manage(this@MainActivity)
                            .isShareStorage(true)
                            .attribute(
                                fileName = name,
                                directory = dir,
                                env = Environment.DIRECTORY_DCIM,
                                extension = Extension.get(Extension.PNG),
                            )
                            .save(it, 100, {
                                runOnUiThread {
                                    showToast("Photo saved to gallery")
                                    getImageUri(name, dir)?.let {
                                        openInGallery(it)
                                    }
                                }
                            }, {
                                runOnUiThread {
                                    showToast("Something went wrong")
                                }
                            })
                    } ?: kotlin.run {
                        showToast("Something went wrong")
                    }
                }
            }

            cvRed.setOnClickListener {
                GlRGBFilter().apply {
                    setRed(1f)
                    setBlue(0f)
                    setGreen(0f)
                    gPUCameraRecorder?.setFilter(this)
                }
            }

            cvBlue.setOnClickListener {
                GlRGBFilter().apply {
                    setRed(0f)
                    setBlue(1f)
                    setGreen(0f)
                    gPUCameraRecorder?.setFilter(this)
                }
            }

            cvGreen.setOnClickListener {
                GlRGBFilter().apply {
                    setRed(0f)
                    setBlue(0f)
                    setGreen(1f)
                    gPUCameraRecorder?.setFilter(this)
                }
            }
        }
    }

    private fun buttonState(isRecord: Boolean) {
        with(binding) {
            val nonRecordView = arrayOf(btnTakePhoto, switchButton, btnRecord, cvGreen, cvBlue, cvRed)
            val recordView = arrayOf(stopButton)
            nonRecordView.forEach {
                it.isVisible = !isRecord
                it.isEnabled = !isRecord
            }
            recordView.forEach {
                it.isVisible = isRecord
                it.isEnabled = isRecord
            }
        }
    }

    private fun releaseCamera() {
        if (sampleGLView != null) {
            sampleGLView?.onPause()
        }
        if (gPUCameraRecorder != null) {
            gPUCameraRecorder?.stop()
            gPUCameraRecorder?.release()
            gPUCameraRecorder = null
        }
        if (sampleGLView != null) {
            (binding.previewView as FrameLayout).removeView(sampleGLView)
            sampleGLView = null
        }
    }

    private fun setUpCameraView() {
        runOnUiThread {
            with(binding) {
                previewView.removeAllViews()
                sampleGLView = null
                sampleGLView =
                    SampleCameraGLView(
                        applicationContext
                    )
                sampleGLView!!.setTouchListener { event, width, height ->
                    if (gPUCameraRecorder == null) return@setTouchListener
                    gPUCameraRecorder?.changeManualFocusPoint(event.x, event.y, width, height)
                }
                previewView.addView(sampleGLView)
            }
        }
    }

    private fun setUpCamera() {
        setUpCameraView()
        gPUCameraRecorder = GPUCameraRecorderBuilder(this, sampleGLView)
            .cameraRecordListener(object : CameraRecordListener {
                override fun onGetFlashSupport(flashSupport: Boolean) {}

                override fun onRecordComplete() {
                    runOnUiThread {
                        VideoUtil.exportMp4ToGallery(application, videoFilepath, videoFileName)
                        val uri = VideoUtil.getVideoUri(videoFilepath, videoFileName)
                        startActivity(MediaViewerActivity.newInstance(
                            this@MainActivity, uri.toString()))
                    }
                }

                override fun onRecordStart() {
                    runOnUiThread {
                        buttonState(true)
                    }
                }

                override fun onError(exception: java.lang.Exception) {
                    Log.e("GPUCameraRecorder", exception.toString())
                    buttonState(false)
                }

                override fun onCameraThreadFinish() {
                    if (toggleClick) {
                        runOnUiThread {
                            setUpCamera()
                        }
                    }
                    toggleClick = false
                }

                override fun onVideoFileReady() {}
            })
            .videoSize(videoWidth, videoHeight)
            .cameraSize(cameraWidth, cameraHeight)
            .lensFacing(lensFacing)
            .build()
    }

    private fun captureBitmap(bitmapReadyCallbacks: (Bitmap?) -> Unit) {
        sampleGLView?.queueEvent {
            val egl = EGLContext.getEGL() as EGL10
            val gl = egl.eglGetCurrentContext().gl as GL10
            val snapshotBitmap = createBitmapFromGLSurface(
                sampleGLView?.measuredWidth ?: 0,
                sampleGLView?.measuredHeight ?: 0,
                gl
            )
            runOnUiThread {
                bitmapReadyCallbacks(snapshotBitmap)
            }
        }
    }

    private fun getImageUri(name: String, dir: String): Uri? {
        return ScopeStorageUtility
            .manage(this@MainActivity)
            .isShareStorage(true)
            .attribute(
                fileName = name,
                directory = dir,
                env = Environment.DIRECTORY_DCIM,
                extension = Extension.get(Extension.PNG)
            )
            .loadSharedFileUri(this@MainActivity, "com.katilijiwoadiwiyono.filterrecord")
    }

}
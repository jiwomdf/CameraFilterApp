package com.katilijiwoadiwiyono.filterrecord

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.concurrent.futures.await
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import androidx.recyclerview.widget.LinearLayoutManager
import com.katilijiwoadiwiyono.filterrecord.adapter.GenericListAdapter
import com.katilijiwoadiwiyono.filterrecord.common.BaseActivity
import com.katilijiwoadiwiyono.filterrecord.data.model.CameraCapability
import com.katilijiwoadiwiyono.filterrecord.databinding.ActivityMainBinding
import com.katilijiwoadiwiyono.filterrecord.utils.UiState
import com.katilijiwoadiwiyono.filterrecord.utils.checkPermission
import com.katilijiwoadiwiyono.filterrecord.utils.getAspectRatio
import com.katilijiwoadiwiyono.filterrecord.utils.getAspectRatioString
import com.katilijiwoadiwiyono.filterrecord.utils.getNameString
import com.katilijiwoadiwiyono.filterrecord.utils.requestPermissionLauncher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : BaseActivity<ActivityMainBinding>() {

    private val storagePermissionLauncher = requestPermissionLauncher(
        onPermissionGranted = {
            showToast("storage granted", Toast.LENGTH_SHORT)
        },
        onPermissionDenied = {
            showToast("Permission is Required", Toast.LENGTH_SHORT)
        }
    )

    private val cameraPermissionLauncher = requestPermissionLauncher(
        onPermissionGranted = {
            initCamera()
        },
        onPermissionDenied = {
            showToast("Permission is Required", Toast.LENGTH_SHORT)
        }
    )

    private val captureLiveStatus = MutableLiveData<String>()
    private val cameraCapabilities = mutableListOf<CameraCapability>()

    private lateinit var videoCapture: VideoCapture<Recorder>
    private var currentRecording: Recording? = null
    private lateinit var recordingState: VideoRecordEvent

    private var cameraIndex = 0
    private var qualityIndex = DEFAULT_QUALITY_IDX
    private var audioEnabled = false

    private val mainThreadExecutor by lazy { ContextCompat.getMainExecutor(this) }
    private var enumerationDeferred: Deferred<Unit>? = null

    companion object {
        // default Quality selection if no input from UI
        const val TAG = "jiwo"
        const val DEFAULT_QUALITY_IDX = 0
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }


    override fun getViewBinding(): ActivityMainBinding =
        ActivityMainBinding.inflate(layoutInflater)

    /**
     * Query and cache this platform's camera capabilities, run only once.
     */
    init {
        enumerationDeferred = lifecycleScope.async {
            whenCreated {
                val provider = ProcessCameraProvider.getInstance(this@MainActivity).await()

                provider.unbindAll()
                for (camSelector in arrayOf(
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    CameraSelector.DEFAULT_FRONT_CAMERA
                )) {
                    try {
                        // just get the camera.cameraInfo to query capabilities
                        // we are not binding anything here.
                        if (provider.hasCamera(camSelector)) {
                            val camera = provider.bindToLifecycle(this@MainActivity, camSelector)
                            QualitySelector
                                .getSupportedQualities(camera.cameraInfo)
                                .filter { quality ->
                                    listOf(Quality.UHD, Quality.FHD, Quality.HD, Quality.SD)
                                        .contains(quality)
                                }.also {
                                    cameraCapabilities.add(CameraCapability(camSelector, it))
                                }
                        }
                    } catch (exc: java.lang.Exception) {
                        Log.e(TAG, "Camera Face $camSelector is not supported")
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //checkStoragePermission()
        checkCameraPermission()
    }

    /**
     * Camera Section
     */
    private suspend fun bindCaptureUseCase() {
        val cameraProvider = ProcessCameraProvider.getInstance(this).await()

        val cameraSelector = getCameraSelector(cameraIndex)

        // create the user required QualitySelector (video resolution): we know this is
        // supported, a valid qualitySelector will be created.
        val quality = cameraCapabilities[cameraIndex].qualities[qualityIndex]
        val qualitySelector = QualitySelector.from(quality)

        binding.previewView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            val orientation = resources.configuration.orientation
            dimensionRatio = quality.getAspectRatioString(quality,
                (orientation == Configuration.ORIENTATION_PORTRAIT))
        }

        val preview = Preview.Builder()
            .setTargetAspectRatio(quality.getAspectRatio(quality))
            .build().apply {
                setSurfaceProvider(binding.previewView.surfaceProvider)
            }

        // build a recorder, which can:
        //   - record video/audio to MediaStore(only shown here), File, ParcelFileDescriptor
        //   - be used create recording(s) (the recording performs recording)
        val recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                videoCapture,
                preview
            )
        } catch (exc: Exception) {
            // we are on main thread, let's reset the controls on the UI.
            Log.e(TAG, "Use case binding failed", exc)
            resetUIandState("bindToLifecycle failed: $exc")
        }
        enableUI(true)
    }

    /**
     * Retrieve the asked camera's type(lens facing type). In this sample, only 2 types:
     *   idx is even number:  CameraSelector.LENS_FACING_BACK
     *          odd number:   CameraSelector.LENS_FACING_FRONT
     */
    private fun getCameraSelector(idx: Int) : CameraSelector {
        if (cameraCapabilities.size == 0) {
            Log.i(TAG, "Error: This device does not have any camera, bailing out")
            finish()
        }
        return (cameraCapabilities[idx % cameraCapabilities.size].camSelector)
    }

    /**
     * initialize UI for recording:
     *  - at recording: hide audio, qualitySelection,change camera UI; enable stop button
     *  - otherwise: show all except the stop button
     */
    private fun showUI(state: UiState, status:String = "idle") {
        binding.let {
            when(state) {
                UiState.IDLE -> {
                    it.captureButton.setImageResource(R.drawable.ic_start)
                    it.stopButton.visibility = View.INVISIBLE

                    it.cameraButton.visibility= View.VISIBLE
                    it.audioSelection.visibility = View.VISIBLE
                    it.qualitySelection.visibility= View.VISIBLE
                }
                UiState.RECORDING -> {
                    it.cameraButton.visibility = View.INVISIBLE
                    it.audioSelection.visibility = View.INVISIBLE
                    it.qualitySelection.visibility = View.INVISIBLE

                    it.captureButton.setImageResource(R.drawable.ic_pause)
                    it.captureButton.isEnabled = true
                    it.stopButton.visibility = View.VISIBLE
                    it.stopButton.isEnabled = true
                }
                UiState.FINALIZED -> {
                    it.captureButton.setImageResource(R.drawable.ic_start)
                    it.stopButton.visibility = View.INVISIBLE
                }
                else -> {
                    val errorMsg = "Error: showUI($state) is not supported"
                    Log.e(TAG, errorMsg)
                    return
                }
            }
            it.captureStatus.text = status
        }
    }

    /**
     * ResetUI (restart):
     *    in case binding failed, let's give it another change for re-try. In future cases
     *    we might fail and user get notified on the status
     */
    private fun resetUIandState(reason: String) {
        enableUI(true)
        showUI(UiState.IDLE, reason)

        cameraIndex = 0
        qualityIndex = DEFAULT_QUALITY_IDX
        audioEnabled = false
        binding.audioSelection.isChecked = audioEnabled
        initializeQualitySectionsUI()
    }

    /**
     * Enable/disable UI:
     *    User could select the capture parameters when recording is not in session
     *    Once recording is started, need to disable able UI to avoid conflict.
     */
    private fun enableUI(enable: Boolean) {
        arrayOf(binding.cameraButton,
            binding.captureButton,
            binding.stopButton,
            binding.audioSelection,
            binding.qualitySelection).forEach {
            it.isEnabled = enable
        }
        // disable the camera button if no device to switch
        if (cameraCapabilities.size <= 1) {
            binding.cameraButton.isEnabled = false
        }
        // disable the resolution list if no resolution to switch
        if (cameraCapabilities[cameraIndex].qualities.size <= 1) {
            binding.qualitySelection.apply { isEnabled = false }
        }
    }

    /**
     *  initializeQualitySectionsUI():
     *    Populate a RecyclerView to display camera capabilities:
     *       - one front facing
     *       - one back facing
     *    User selection is saved to qualityIndex, will be used
     *    in the bindCaptureUsecase().
     */
    private fun initializeQualitySectionsUI() {
        val selectorStrings = cameraCapabilities[cameraIndex].qualities.map {
            it.getNameString()
        }
        // create the adapter to Quality selection RecyclerView
        binding.qualitySelection.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = GenericListAdapter(
                selectorStrings,
                itemLayoutId = R.layout.video_quality_item
            ) { holderView, qcString, position ->

                holderView.apply {
                    findViewById<TextView>(R.id.qualityTextView)?.text = qcString
                    // select the default quality selector
                    isSelected = (position == qualityIndex)
                }

                holderView.setOnClickListener { view ->
                    if (qualityIndex == position) return@setOnClickListener

                    let {
                        // deselect the previous selection on UI.
                        it.findViewHolderForAdapterPosition(qualityIndex)
                            ?.itemView
                            ?.isSelected = false
                    }
                    // turn on the new selection on UI.
                    view.isSelected = true
                    qualityIndex = position

                    // rebind the use cases to put the new QualitySelection in action.
                    enableUI(false)
                    lifecycleScope.launch {
                        bindCaptureUseCase()
                    }
                }
            }
            isEnabled = false
        }
    }


    /**
     * Permission Section
     */
    private fun checkStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
        checkPermission(
            permission = permission,
            launcher = storagePermissionLauncher,
            onPermissionGranted = {

            }
        )
    }

    private fun checkCameraPermission() {
        checkPermission(
            permission = Manifest.permission.CAMERA,
            launcher = cameraPermissionLauncher,
            onPermissionGranted = {
                initCamera()
            }
        )
    }

    /**
     * One time initialize for CameraFragment (as a part of fragment layout's creation process).
     * This function performs the following:
     *   - initialize but disable all UI controls except the Quality selection.
     *   - set up the Quality selection recycler view.
     *   - bind use cases to a lifecycle camera, enable UI controls.
     */
    private fun initCamera() {
        initializeUI()
        lifecycleScope.launch {
            if (enumerationDeferred != null) {
                enumerationDeferred!!.await()
                enumerationDeferred = null
            }
            initializeQualitySectionsUI()

            bindCaptureUseCase()
        }
    }

    /**
     * Initialize UI. Preview and Capture actions are configured in this function.
     * Note that preview and capture are both initialized either by UI or CameraX callbacks
     * (except the very 1st time upon entering to this fragment in onCreateView()
     */
    @SuppressLint("ClickableViewAccessibility", "MissingPermission")
    private fun initializeUI() {
        binding.cameraButton.apply {
            setOnClickListener {
                cameraIndex = (cameraIndex + 1) % cameraCapabilities.size
                // camera device change is in effect instantly:
                //   - reset quality selection
                //   - restart preview
                qualityIndex = DEFAULT_QUALITY_IDX
                initializeQualitySectionsUI()
                enableUI(false)
                lifecycleScope.launch {
                    bindCaptureUseCase()
                }
            }
            isEnabled = false
        }

        // audioEnabled by default is disabled.
        binding.audioSelection.isChecked = audioEnabled
        binding.audioSelection.setOnClickListener {
            audioEnabled = binding.audioSelection.isChecked
        }

        // React to user touching the capture button
        binding.captureButton.apply {
            setOnClickListener {
                if (!this@MainActivity::recordingState.isInitialized ||
                    recordingState is VideoRecordEvent.Finalize)
                {
                    enableUI(false)  // Our eventListener will turn on the Recording UI.
                    startRecording()
                } else {
                    when (recordingState) {
                        is VideoRecordEvent.Start -> {
                            currentRecording?.pause()
                            binding.stopButton.visibility = View.VISIBLE
                        }
                        is VideoRecordEvent.Pause -> currentRecording?.resume()
                        is VideoRecordEvent.Resume -> currentRecording?.pause()
                        else -> throw IllegalStateException("recordingState in unknown state")
                    }
                }
            }
            isEnabled = false
        }

        binding.stopButton.apply {
            setOnClickListener {
                // stopping: hide it after getting a click before we go to viewing fragment
                binding.stopButton.visibility = View.INVISIBLE
                if (currentRecording == null || recordingState is VideoRecordEvent.Finalize) {
                    return@setOnClickListener
                }

                val recording = currentRecording
                if (recording != null) {
                    recording.stop()
                    currentRecording = null
                }
                binding.captureButton.setImageResource(R.drawable.ic_start)
            }
            // ensure the stop button is initialized disabled & invisible
            visibility = View.INVISIBLE
            isEnabled = false
        }

        captureLiveStatus.observe(this) {
            binding.captureStatus.apply {
                post { text = it }
            }
        }
        captureLiveStatus.value = getString(R.string.Idle)
    }

    /**
     * CaptureEvent listener.
     */
    private val captureListener = Consumer<VideoRecordEvent> { event ->
        // cache the recording state
        if (event !is VideoRecordEvent.Status)
            recordingState = event

        updateUI(event)

        if (event is VideoRecordEvent.Finalize) {
            // display the captured video
            lifecycleScope.launch {
                //TODO:JIWO Navigate
//                navController.navigate(
//                    CaptureFragmentDirections.actionCaptureToVideoViewer(
//                        event.outputResults.outputUri
//                    )
//                )
            }
        }
    }

    /**
     * UpdateUI according to CameraX VideoRecordEvent type:
     *   - user starts capture.
     *   - this app disables all UI selections.
     *   - this app enables capture run-time UI (pause/resume/stop).
     *   - user controls recording with run-time UI, eventually tap "stop" to end.
     *   - this app informs CameraX recording to stop with recording.stop() (or recording.close()).
     *   - CameraX notify this app that the recording is indeed stopped, with the Finalize event.
     *   - this app starts VideoViewer fragment to view the captured result.
     */
    private fun updateUI(event: VideoRecordEvent) {
        val state = if (event is VideoRecordEvent.Status) recordingState.getNameString()
        else event.getNameString()
        when (event) {
            is VideoRecordEvent.Status -> {
                // placeholder: we update the UI with new status after this when() block,
                // nothing needs to do here.
            }
            is VideoRecordEvent.Start -> {
                showUI(UiState.RECORDING, event.getNameString())
            }
            is VideoRecordEvent.Finalize-> {
                showUI(UiState.FINALIZED, event.getNameString())
            }
            is VideoRecordEvent.Pause -> {
                binding.captureButton.setImageResource(R.drawable.ic_resume)
            }
            is VideoRecordEvent.Resume -> {
                binding.captureButton.setImageResource(R.drawable.ic_pause)
            }
        }

        val stats = event.recordingStats
        val size = stats.numBytesRecorded / 1000
        val time = java.util.concurrent.TimeUnit.NANOSECONDS.toSeconds(stats.recordedDurationNanos)
        var text = "${state}: recorded ${size}KB, in ${time}second"
        if(event is VideoRecordEvent.Finalize)
            text = "${text}\nFile saved to: ${event.outputResults.outputUri}"

        captureLiveStatus.value = text
        Log.i(TAG, "recording event: $text")
    }

    /**
     * Kick start the video recording
     *   - config Recorder to capture to MediaStoreOutput
     *   - register RecordEvent Listener
     *   - apply audio request from user
     *   - start recording!
     * After this function, user could start/pause/resume/stop recording and application listens
     * to VideoRecordEvent for the current recording status.
     */
    @SuppressLint("MissingPermission")
    private fun startRecording() {
        // create MediaStoreOutputOptions for our recorder: resulting our recording!
        val name = "CameraX-recording-" +
                SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                    .format(System.currentTimeMillis()) + ".mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
        }
        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            this.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        // configure Recorder and Start recording to the mediaStoreOutput.
        currentRecording = videoCapture.output
            .prepareRecording(this, mediaStoreOutput)
            .apply { if (audioEnabled) withAudioEnabled() }
            .start(mainThreadExecutor, captureListener)

        Log.i(TAG, "Recording started")
    }
}